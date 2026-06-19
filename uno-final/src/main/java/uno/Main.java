package uno;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uno.persistence.JpaUtil;
import uno.persistence.entity.GameEntity;
import uno.persistence.repository.GameRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Main {

    static final Logger log = LoggerFactory.getLogger(Main.class);

    static final int DEFAULT_TARGET = 500;

    public static void main(String[] args) {
        int numBots = 3;
        boolean human = false;
        boolean quiet = false;
        boolean noPersist = false;
        boolean report = false;
        int targetScore = DEFAULT_TARGET;
        long seed = System.currentTimeMillis();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--bots") && i + 1 < args.length) {
                numBots = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--human")) {
                human = true;
            } else if (args[i].equals("--quiet")) {
                quiet = true;
            } else if (args[i].equals("--no-persist")) {
                noPersist = true;
            } else if (args[i].equals("--report")) {
                report = true;
            } else if (args[i].equals("--target") && i + 1 < args.length) {
                targetScore = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--seed") && i + 1 < args.length) {
                seed = Long.parseLong(args[++i]);
            } else if (args[i].equals("--help")) {
                printHelp();
                return;
            }
        }

        if (report) {
            runReport();
            return;
        }

        List<String> names = new ArrayList<>();
        List<Boolean> humans = new ArrayList<>();
        if (human) {
            names.add("You");
            humans.add(Boolean.TRUE);
        }
        for (int i = 1; i <= numBots; i++) {
            names.add("Bot" + i);
            humans.add(Boolean.FALSE);
        }

        if (names.size() < 2 || names.size() > 4) {
            System.out.println("UNO needs 2 to 4 players.");
            return;
        }

        Random rng = new Random(seed);
        ConsoleView view = new ConsoleView(quiet, new Scanner(System.in));
        GameState state = new GameState(names, humans, targetScore);

        log.info("Game session starting — players={} target={} seed={}", names, targetScore, seed);

        EntityManagerFactory emf = null;
        GameEntity gameEntity = null;
        GameRepository gameRepository = new GameRepository();

        if (!noPersist) {
            try {
                emf = JpaUtil.getFactory("uno-pu");
                EntityManager em = emf.createEntityManager();
                em.getTransaction().begin();
                gameEntity = gameRepository.startGame(em, names);
                em.getTransaction().commit();
                em.close();
                log.info("Persistence enabled — game id={}", gameEntity.getId());
            } catch (Exception e) {
                log.warn("Persistence unavailable, continuing without database: {}", e.getMessage());
                noPersist = true;
            }
        }

        view.showGameStart(names, targetScore);

        int roundNumber = 1;
        while (!state.isGameOver()) {
            view.showRoundHeader(roundNumber);
            RoundResult result = playRound(state, roundNumber, rng, view);

            view.showRoundScores(state.playerNames, state.scores);

            if (!noPersist && gameEntity != null) {
                try {
                    persistRound(emf, gameRepository, gameEntity.getId(), result, state);
                } catch (Exception e) {
                    log.warn("Failed to persist round {}: {}", roundNumber, e.getMessage());
                }
            }
            roundNumber++;
        }

        int winnerIdx = state.gameWinnerIndex();
        view.showGameWinner(state.playerNames.get(winnerIdx), state.scores[winnerIdx]);
        view.showFinalScores(state.playerNames, state.scores);

        log.info("Game over — winner={} score={}", state.playerNames.get(winnerIdx), state.scores[winnerIdx]);

        if (!noPersist && gameEntity != null) {
            try {
                EntityManager em = emf.createEntityManager();
                em.getTransaction().begin();
                GameEntity g = em.find(GameEntity.class, gameEntity.getId());
                gameRepository.finishGame(em, g, state.playerNames.get(winnerIdx), Instant.now());
                em.getTransaction().commit();
                em.close();
            } catch (Exception e) {
                log.warn("Failed to persist final game result: {}", e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Round loop
    // -------------------------------------------------------------------------

    static RoundResult playRound(GameState state, int roundNumber, Random rng, ConsoleView view) {
        GameEngine.setupRound(state, rng);
        Instant startedAt = Instant.now();

        log.info("Round {} started — upCard={} firstPlayer={}", roundNumber,
                state.upCard, state.currentPlayerName());

        int guard = 0;
        while (guard < 3000) {
            guard++;

            // Check if any other player missed UNO last turn
            for (int i = 0; i < state.playerNames.size(); i++) {
                if (i != state.currentPlayer
                        && state.hands.get(i).size() == 1
                        && !state.unoCalled[i]) {
                    GameEngine.applyUnoPenalty(state, i);
                    view.showUnoPenalty(state.playerNames.get(i));
                    log.info("UNO penalty — player={}", state.playerNames.get(i));
                }
            }

            String name = state.currentPlayerName();
            ArrayList<String> hand = state.currentHand();
            int cp = state.currentPlayer;

            log.debug("Turn {} — player={} handSize={} upCard={} calledColor={}",
                    guard, name, hand.size(), state.upCard,
                    state.calledColor.isEmpty() ? "none" : state.calledColor);

            view.showTurnState(state.upCard, state.calledColor, name, hand);

            // Get card choice
            int chosen;
            if (state.currentIsHuman()) {
                chosen = view.askHuman(hand, state.upCard, state.calledColor);
            } else {
                chosen = BotStrategy.selectCard(hand, state.upCard, state.calledColor);
            }

            // Handle draw
            if (chosen == -1) {
                String drawn = GameEngine.draw(state);
                hand.add(drawn);
                view.showDraw(name, drawn);
                log.info("Card drawn — player={} card={}", name, drawn);

                if (Rules.isLegal(drawn, state.upCard, state.calledColor)) {
                    if (state.currentIsHuman()) {
                        if (view.askPlayDrawn(drawn)) {
                            chosen = hand.size() - 1;
                        }
                    } else {
                        chosen = hand.size() - 1;
                    }
                }
            }

            if (chosen >= 0) {
                if (chosen >= hand.size()) {
                    view.showPenalty(name);
                    log.warn("Invalid index — player={} index={} handSize={}", name, chosen, hand.size());
                    hand.add(GameEngine.draw(state));
                    GameEngine.next(state);
                    continue;
                }

                String card = hand.get(chosen);
                if (!Rules.isLegal(card, state.upCard, state.calledColor)) {
                    view.showIllegalCard(name, card);
                    log.warn("Illegal card — player={} card={} upCard={}", name, card, state.upCard);
                    hand.add(GameEngine.draw(state));
                    GameEngine.next(state);
                    continue;
                }

                hand.remove(chosen);
                state.discard.add(state.upCard);
                state.upCard = card;
                state.calledColor = "";
                view.showPlay(name, card);
                log.info("Card played — player={} card={}", name, card);

                // Wild color selection
                if (card.equals("W") || card.equals("W4")) {
                    if (state.currentIsHuman()) {
                        state.calledColor = view.askColor();
                    } else {
                        state.calledColor = BotStrategy.selectColor(hand);
                    }
                    view.showCalledColor(name, state.calledColor);
                    log.info("Color called — player={} color={}", name, state.calledColor);
                }

                // UNO call detection
                if (hand.size() == 1) {
                    state.unoCalled[cp] = false;
                    if (state.currentIsHuman()) {
                        boolean called = view.askUno();
                        if (called) {
                            state.unoCalled[cp] = true;
                            view.showUno(name);
                            log.info("UNO called — player={}", name);
                        } else {
                            GameEngine.applyUnoPenalty(state, cp);
                            view.showUnoPenalty(name);
                            log.info("UNO penalty (declined) — player={}", name);
                        }
                    } else {
                        state.unoCalled[cp] = true;
                        view.showUno(name);
                        log.info("UNO called — player={}", name);
                    }
                }

                // Win check
                if (hand.isEmpty()) {
                    int points = GameEngine.scoreRound(state, cp);
                    view.showRoundWinner(name, points);
                    log.info("Round {} ended — winner={} points={} total={}",
                            roundNumber, name, points, state.scores[cp]);
                    return new RoundResult(roundNumber, name, points,
                            state.scores.clone(), startedAt, Instant.now());
                }

                // Apply card effect (Skip, Reverse, Draw Two, Wild Draw Four)
                String rank = Card.rank(card);
                if (rank.equals("SKIP")) {
                    view.showSkip(state.playerNames.get(
                            (cp + state.direction + state.playerNames.size()) % state.playerNames.size()));
                } else if (rank.equals("REVERSE")) {
                    view.showReverse(state.playerNames.size());
                }

                int drawVictim = GameEngine.applyEffect(state, card);
                if (drawVictim >= 0) {
                    if (rank.equals("DRAW_TWO")) {
                        view.showDrawTwo(state.playerNames.get(drawVictim));
                    } else if (rank.equals("WILD_DRAW_FOUR")) {
                        view.showDrawFour(state.playerNames.get(drawVictim));
                    }
                }

            } else {
                GameEngine.next(state);
            }
        }

        view.showSafetyLimit();
        log.warn("Safety limit reached after {} turns in round {}", 3000, roundNumber);
        return new RoundResult(roundNumber, null, 0, state.scores.clone(), startedAt, Instant.now());
    }

    // -------------------------------------------------------------------------
    // Persistence wiring
    // -------------------------------------------------------------------------

    static void persistRound(EntityManagerFactory emf, GameRepository repo,
                              Long gameId, RoundResult result, GameState state) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            GameEntity game = em.find(GameEntity.class, gameId);

            Map<String, Integer> pointsThisRound = new HashMap<>();
            Map<String, Integer> cumulative = new HashMap<>();
            for (int i = 0; i < state.playerNames.size(); i++) {
                String n = state.playerNames.get(i);
                cumulative.put(n, result.cumulativeScores[i]);
                pointsThisRound.put(n, n.equals(result.winnerName) ? result.pointsScoredThisRound : 0);
            }
            repo.recordRound(em, game, result.roundNumber, result.winnerName,
                    pointsThisRound, cumulative, result.endedAt);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    // -------------------------------------------------------------------------
    // Report mode
    // -------------------------------------------------------------------------

    static void runReport() {
        GameRepository repo = new GameRepository();
        EntityManagerFactory emf = JpaUtil.getFactory("uno-pu");
        EntityManager em = emf.createEntityManager();
        try {
            System.out.println("=== Recent Games ===");
            for (Object[] row : repo.findRecentGames(em, 10)) {
                String ended = row[2] == null ? "in progress" : row[2].toString();
                String winner = row[3] == null ? "n/a" : row[3].toString();
                System.out.println("Game #" + row[0] + "  started=" + row[1]
                        + "  ended=" + ended + "  winner=" + winner);
            }
            System.out.println("\n=== Player Win Counts ===");
            for (Object[] row : repo.findPlayerWinCounts(em)) {
                System.out.println(row[0] + "  wins=" + row[1]);
            }
            System.out.println("\n=== Highest Scores ===");
            for (Object[] row : repo.findHighestScores(em, 10)) {
                System.out.println(row[0] + "  score=" + row[1] + "  gameId=" + row[2]);
            }
        } finally {
            em.close();
        }
    }

    static void printHelp() {
        System.out.println("Usage: java -jar uno.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --bots N       Number of bot players (default 3; total players must be 2-4)");
        System.out.println("  --human        Add a human player");
        System.out.println("  --target N     Score target to win the game (default 500)");
        System.out.println("  --quiet        Only print final scores, no per-turn output");
        System.out.println("  --seed N       Fix random seed for reproducible games");
        System.out.println("  --no-persist   Skip database writes");
        System.out.println("  --report       Print game history instead of playing");
        System.out.println("  --help         Show this message");
        System.out.println();
        System.out.println("Card input (human mode):");
        System.out.println("  R5    red 5         YS   yellow skip");
        System.out.println("  BR    blue reverse   G+2  green draw two");
        System.out.println("  W     wild            W4   wild draw four");
        System.out.println("  draw  draw a card");
    }

    // -------------------------------------------------------------------------
    // RoundResult value object
    // -------------------------------------------------------------------------

    static class RoundResult {
        final int roundNumber;
        final String winnerName;
        final int pointsScoredThisRound;
        final int[] cumulativeScores;
        final Instant startedAt;
        final Instant endedAt;

        RoundResult(int roundNumber, String winnerName, int pointsScoredThisRound,
                    int[] cumulativeScores, Instant startedAt, Instant endedAt) {
            this.roundNumber = roundNumber;
            this.winnerName = winnerName;
            this.pointsScoredThisRound = pointsScoredThisRound;
            this.cumulativeScores = cumulativeScores;
            this.startedAt = startedAt;
            this.endedAt = endedAt;
        }
    }
}
