package uno;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

public class Main {

    static final Logger log = LoggerFactory.getLogger(Main.class);

    static ArrayList<String> playerNames = new ArrayList<String>();
    static ArrayList<Boolean> humanPlayers = new ArrayList<Boolean>();
    static ArrayList<ArrayList<String>> hands = new ArrayList<ArrayList<String>>();
    static ArrayList<String> deck = new ArrayList<String>();
    static ArrayList<String> discard = new ArrayList<String>();
    static int[] scores = new int[10];
    static int currentPlayer = 0;
    static int direction = 1;
    static String upCard = "";
    static String calledColor = "";
    static boolean quiet = false;
    static Random random = new Random();
    static Scanner scanner = new Scanner(System.in);
    static ConsoleView view;

    public static void main(String[] args) {
        int bots = 3;
        int games = 1;
        boolean human = false;
        long seed = System.currentTimeMillis();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--bots") && i + 1 < args.length) {
                bots = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--games") && i + 1 < args.length) {
                games = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--human")) {
                human = true;
            } else if (args[i].equals("--quiet")) {
                quiet = true;
            } else if (args[i].equals("--seed") && i + 1 < args.length) {
                seed = Long.parseLong(args[++i]);
            } else if (args[i].equals("--self-test")) {
                selfTest();
                return;
            } else if (args[i].equals("--help")) {
                System.out.println("Usage: java -jar uno.jar [--bots N] [--games N] [--human] [--quiet] [--seed N]");
                return;
            }
        }

        random = new Random(seed);
        view = new ConsoleView(quiet, scanner);
        setupPlayers(bots, human);

        if (playerNames.size() < 2 || playerNames.size() > 4) {
            System.out.println("UNO needs 2 to 4 players.");
            return;
        }

        log.info("Game session starting — players={} bots={} human={} games={} seed={}",
                playerNames.size(), bots, human, games, seed);

        for (int g = 1; g <= games; g++) {
            view.showGameHeader(g);
            playGame();
        }

        view.showFinalScores(playerNames, scores);
        log.info("All games completed — final scores: {}", formatScores());
    }

    static void setupPlayers(int bots, boolean human) {
        playerNames.clear();
        humanPlayers.clear();
        hands.clear();
        if (human) {
            playerNames.add("You");
            humanPlayers.add(Boolean.TRUE);
            hands.add(new ArrayList<String>());
        }
        for (int i = 1; i <= bots; i++) {
            playerNames.add("Bot" + i);
            humanPlayers.add(Boolean.FALSE);
            hands.add(new ArrayList<String>());
        }
    }

    static void playGame() {
        deck.clear();
        String[] colors = {"R", "Y", "G", "B"};
        for (int c = 0; c < colors.length; c++) {
            deck.add(colors[c] + "0");
            for (int n = 1; n <= 9; n++) {
                deck.add(colors[c] + n);
                deck.add(colors[c] + n);
            }
            deck.add(colors[c] + "S");
            deck.add(colors[c] + "S");
            deck.add(colors[c] + "R");
            deck.add(colors[c] + "R");
            deck.add(colors[c] + "+2");
            deck.add(colors[c] + "+2");
        }
        for (int i = 0; i < 4; i++) {
            deck.add("W");
            deck.add("W4");
        }
        Collections.shuffle(deck, random);
        discard.clear();
        for (int i = 0; i < hands.size(); i++) {
            hands.get(i).clear();
        }
        for (int i = 0; i < playerNames.size(); i++) {
            for (int j = 0; j < 7; j++) {
                hands.get(i).add(draw());
            }
        }
        upCard = draw();
        while (upCard.startsWith("W")) {
            discard.add(upCard);
            upCard = draw();
        }
        calledColor = "";
        direction = 1;
        currentPlayer = random.nextInt(playerNames.size());

        log.info("Round started — upCard={} firstPlayer={} players={}",
                upCard, playerNames.get(currentPlayer), playerNames);

        int guard = 0;
        while (guard < 3000) {
            guard++;
            String name = playerNames.get(currentPlayer);
            ArrayList<String> hand = hands.get(currentPlayer);

            log.debug("Turn {} — player={} handSize={} upCard={} calledColor={}",
                    guard, name, hand.size(), upCard, calledColor.isEmpty() ? "none" : calledColor);

            view.showTurnState(upCard, calledColor, name, hand);

            int chosen = -1;
            if (humanPlayers.get(currentPlayer).booleanValue()) {
                chosen = view.askHuman(hand, upCard, calledColor);
            } else {
                chosen = BotStrategy.selectCard(hand, upCard, calledColor);
            }

            if (chosen == -1) {
                String drawn = draw();
                hand.add(drawn);
                view.showDraw(name, drawn);
                log.info("Card drawn — player={} card={}", name, drawn);

                if (Rules.isLegal(drawn, upCard, calledColor)) {
                    if (!humanPlayers.get(currentPlayer).booleanValue()) {
                        chosen = hand.size() - 1;
                    } else {
                        if (view.askPlayDrawn(drawn)) {
                            chosen = hand.size() - 1;
                        }
                    }
                }
            }

            if (chosen >= 0) {
                if (chosen >= hand.size()) {
                    view.showPenalty(name);
                    log.warn("Invalid index — player={} index={} handSize={}", name, chosen, hand.size());
                    hand.add(draw());
                    next();
                    continue;
                }

                String card = hand.get(chosen);
                boolean ok = Rules.isLegal(card, upCard, calledColor);

                if (!ok) {
                    view.showIllegalCard(name, card);
                    log.warn("Illegal card attempted — player={} card={} upCard={} calledColor={}",
                            name, card, upCard, calledColor.isEmpty() ? "none" : calledColor);
                    hand.add(draw());
                    next();
                    continue;
                }

                hand.remove(chosen);
                discard.add(upCard);
                upCard = card;
                calledColor = "";
                view.showPlay(name, card);
                log.info("Card played — player={} card={}", name, card);

                if (card.equals("W") || card.equals("W4")) {
                    if (humanPlayers.get(currentPlayer).booleanValue()) {
                        calledColor = view.askColor();
                    } else {
                        calledColor = BotStrategy.selectColor(hand);
                    }
                    view.showCalledColor(name, calledColor);
                    log.info("Wild color called — player={} color={}", name, calledColor);
                }

                if (hand.size() == 1) {
                    view.showUno(name);
                    log.info("UNO — player={}", name);
                }

                if (hand.size() == 0) {
                    int points = 0;
                    for (int i = 0; i < hands.size(); i++) {
                        if (i != currentPlayer) {
                            for (int j = 0; j < hands.get(i).size(); j++) {
                                points += Card.points(hands.get(i).get(j));
                            }
                        }
                    }
                    scores[currentPlayer] += points;
                    view.showWin(name, points);
                    log.info("Round ended — winner={} pointsScored={} runningTotal={}",
                            name, points, scores[currentPlayer]);
                    return;
                }

                applyEffect(card);
            } else {
                next();
            }
        }
        view.showSafetyLimit();
        log.warn("Safety limit reached after {} turns — game aborted", 3000);
    }

    static void applyEffect(String card) {
        String rank = Card.rank(card);
        if (rank.equals("SKIP")) {
            next();
            next();
        } else if (rank.equals("REVERSE")) {
            direction = direction * -1;
            if (playerNames.size() == 2) {
                next();
                next();
            } else {
                next();
            }
        } else if (rank.equals("DRAW_TWO")) {
            next();
            hands.get(currentPlayer).add(draw());
            hands.get(currentPlayer).add(draw());
            view.showDrawTwo(playerNames.get(currentPlayer));
            next();
        } else if (rank.equals("WILD_DRAW_FOUR")) {
            next();
            for (int i = 0; i < 4; i++) {
                hands.get(currentPlayer).add(draw());
            }
            view.showDrawFour(playerNames.get(currentPlayer));
            next();
        } else {
            next();
        }
    }

    static String draw() {
        if (deck.size() == 0) {
            deck.addAll(discard);
            discard.clear();
            Collections.shuffle(deck, random);
        }
        if (deck.size() == 0) {
            return "W";
        }
        return deck.remove(0);
    }

    static void next() {
        currentPlayer += direction;
        if (currentPlayer >= playerNames.size()) {
            currentPlayer = 0;
        }
        if (currentPlayer < 0) {
            currentPlayer = playerNames.size() - 1;
        }
    }

    // -------------------------------------------------------------------------
    // Legacy self-test (kept for backward compatibility with scripts/test.sh)
    // -------------------------------------------------------------------------

    static void selfTest() {
        int passed = 0;
        passed += testCardColor();
        passed += testCardRank();
        passed += testCardPoints();
        passed += testRulesIsLegal();
        passed += testBotSelectCard();
        passed += testBotSelectColor();
        passed += testNext();
        passed += testDrawMethod();
        passed += testScoring();
        passed += testConsoleViewJoin();
        System.out.println("Passed " + passed + " characterization checks.");
    }

    static int testCardColor() {
        int p = 0;
        if (Card.color("R5").equals("R"))  p++; else fail("color R5");
        if (Card.color("Y3").equals("Y"))  p++; else fail("color Y3");
        if (Card.color("G+2").equals("G")) p++; else fail("color G+2");
        if (Card.color("B0").equals("B"))  p++; else fail("color B0");
        if (Card.color("W").equals(""))    p++; else fail("color W has no color");
        if (Card.color("W4").equals(""))   p++; else fail("color W4 has no color");
        return p;
    }

    static int testCardRank() {
        int p = 0;
        if (Card.rank("G+2").equals("DRAW_TWO"))      p++; else fail("rank DRAW_TWO");
        if (Card.rank("RS").equals("SKIP"))            p++; else fail("rank SKIP");
        if (Card.rank("YS").equals("SKIP"))            p++; else fail("rank SKIP yellow");
        if (Card.rank("BS").equals("SKIP"))            p++; else fail("rank SKIP blue");
        if (Card.rank("GS").equals("SKIP"))            p++; else fail("rank SKIP green");
        if (Card.rank("BR").equals("REVERSE"))         p++; else fail("rank REVERSE");
        if (Card.rank("RR").equals("REVERSE"))         p++; else fail("rank REVERSE red");
        if (Card.rank("GR").equals("REVERSE"))         p++; else fail("rank REVERSE green");
        if (Card.rank("R+2").equals("DRAW_TWO"))       p++; else fail("rank DRAW_TWO red");
        if (Card.rank("B+2").equals("DRAW_TWO"))       p++; else fail("rank DRAW_TWO blue");
        if (Card.rank("W").equals("WILD"))             p++; else fail("rank WILD");
        if (Card.rank("W4").equals("WILD_DRAW_FOUR"))  p++; else fail("rank WILD_DRAW_FOUR");
        if (Card.rank("R5").equals("NUMBER"))          p++; else fail("rank NUMBER");
        return p;
    }

    static int testCardPoints() {
        int p = 0;
        if (Card.points("W4")  == 50) p++; else fail("points W4");
        if (Card.points("W")   == 50) p++; else fail("points W");
        if (Card.points("RS")  == 20) p++; else fail("points SKIP");
        if (Card.points("YR")  == 20) p++; else fail("points REVERSE");
        if (Card.points("B+2") == 20) p++; else fail("points DRAW_TWO");
        if (Card.points("R7")  ==  7) p++; else fail("points number 7");
        if (Card.points("G0")  ==  0) p++; else fail("points zero");
        return p;
    }

    static int testRulesIsLegal() {
        int p = 0;
        if (Rules.isLegal("R2",  "R9", ""))  p++; else fail("legal: same color");
        if (Rules.isLegal("G9",  "R9", ""))  p++; else fail("legal: same number");
        if (Rules.isLegal("B9",  "R9", ""))  p++; else fail("legal: same number across colors");
        if (Rules.isLegal("G0",  "Y0", ""))  p++; else fail("legal: zero matches zero");
        if (Rules.isLegal("B3",  "W",  "B")) p++; else fail("legal: called color");
        if (Rules.isLegal("G3",  "W",  "G")) p++; else fail("legal: called color G");
        if (!Rules.isLegal("B3", "R9", ""))  p++; else fail("illegal: color and number mismatch");
        if (!Rules.isLegal("R3", "W",  "G")) p++; else fail("illegal: wrong called color");
        if (!Rules.isLegal("R5", "W",  "G")) p++; else fail("illegal: wrong called color R5");
        if (Rules.isLegal("W",   "R9", ""))  p++; else fail("legal: wild always playable");
        if (Rules.isLegal("W4",  "G5", ""))  p++; else fail("legal: wild draw four always playable");
        if (Rules.isLegal("W",   "R5", ""))  p++; else fail("legal: wild on any up card");
        if (Rules.isLegal("W4",  "G0", ""))  p++; else fail("legal: W4 on any up card");
        if (Rules.isLegal("RS",  "YS", ""))  p++; else fail("legal: same action SKIP");
        if (Rules.isLegal("GS",  "BS", ""))  p++; else fail("legal: SKIP across colors");
        if (Rules.isLegal("GR",  "BR", ""))  p++; else fail("legal: same action REVERSE");
        if (Rules.isLegal("RR",  "BR", ""))  p++; else fail("legal: REVERSE across colors");
        if (Rules.isLegal("R+2", "G+2", "")) p++; else fail("legal: same action DRAW_TWO");
        if (Rules.isLegal("Y+2", "R+2", "")) p++; else fail("legal: DRAW_TWO across colors");
        if (Rules.isLegal("G5",  "W",  "G")) p++; else fail("legal: matches called color");
        return p;
    }

    static int testBotSelectCard() {
        int p = 0;
        ArrayList<String> h1 = new ArrayList<String>();
        h1.add("B3"); h1.add("R4"); h1.add("W");
        if (BotStrategy.selectCard(h1, "R9", "") == 1) p++; else fail("bot prefers number over wild");

        ArrayList<String> h2 = new ArrayList<String>();
        h2.add("R5"); h2.add("R+2"); h2.add("W");
        if (BotStrategy.selectCard(h2, "R9", "") == 1) p++; else fail("bot prefers DRAW_TWO");

        ArrayList<String> h3 = new ArrayList<String>();
        h3.add("R5"); h3.add("RS");
        if (BotStrategy.selectCard(h3, "R9", "") == 1) p++; else fail("bot prefers SKIP over NUMBER");

        ArrayList<String> h4 = new ArrayList<String>();
        h4.add("G3"); h4.add("W");
        if (BotStrategy.selectCard(h4, "R9", "") == 1) p++; else fail("bot plays wild when only legal option");

        ArrayList<String> h5 = new ArrayList<String>();
        h5.add("G3"); h5.add("B5");
        if (BotStrategy.selectCard(h5, "R9", "") == -1) p++; else fail("bot draws when nothing legal");
        return p;
    }

    static int testBotSelectColor() {
        int p = 0;
        ArrayList<String> h6 = new ArrayList<String>();
        h6.add("B1"); h6.add("B2"); h6.add("R3");
        if (BotStrategy.selectColor(h6).equals("B")) p++; else fail("bot color picks most frequent");
        return p;
    }

    static int testNext() {
        int p = 0;
        playerNames.clear();
        playerNames.add("A"); playerNames.add("B"); playerNames.add("C");

        currentPlayer = 0; direction = 1;
        next();
        if (currentPlayer == 1) p++; else fail("next() forward from 0 → 1");

        currentPlayer = 2; direction = 1;
        next();
        if (currentPlayer == 0) p++; else fail("next() wraps 2 → 0");

        currentPlayer = 1; direction = -1;
        next();
        if (currentPlayer == 0) p++; else fail("next() reverse from 1 → 0");

        currentPlayer = 0; direction = -1;
        next();
        if (currentPlayer == 2) p++; else fail("next() reverse wrap 0 → 2");

        playerNames.clear(); currentPlayer = 0; direction = 1;
        return p;
    }

    static int testDrawMethod() {
        int p = 0;
        deck.clear(); discard.clear();
        discard.add("R1"); discard.add("B3"); discard.add("G7");
        random = new Random(0);
        String drawn = draw();
        if (!drawn.equals("W") && deck.size() == 2) p++; else fail("draw() refills deck from discard");

        deck.clear(); discard.clear();
        if (draw().equals("W")) p++; else fail("draw() returns W when both empty");

        deck.clear(); discard.clear(); random = new Random();
        return p;
    }

    static int testScoring() {
        int p = 0;
        if (Card.points("R5")  ==  5) p++; else fail("scoring: number 5");
        if (Card.points("B0")  ==  0) p++; else fail("scoring: zero");
        if (Card.points("YS")  == 20) p++; else fail("scoring: SKIP = 20");
        if (Card.points("GR")  == 20) p++; else fail("scoring: REVERSE = 20");
        if (Card.points("R+2") == 20) p++; else fail("scoring: DRAW_TWO = 20");
        if (Card.points("W")   == 50) p++; else fail("scoring: WILD = 50");
        if (Card.points("W4")  == 50) p++; else fail("scoring: WILD_DRAW_FOUR = 50");
        return p;
    }

    static int testConsoleViewJoin() {
        int p = 0;
        ArrayList<String> jh = new ArrayList<String>();
        jh.add("R5"); jh.add("W");
        if (ConsoleView.join(jh).equals("0:R5 1:W")) p++; else fail("ConsoleView.join formats hand correctly");
        return p;
    }

    static void fail(String name) {
        throw new RuntimeException("Failed: " + name);
    }

    private static String formatScores() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < playerNames.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(playerNames.get(i)).append("=").append(scores[i]);
        }
        return sb.toString();
    }
}
