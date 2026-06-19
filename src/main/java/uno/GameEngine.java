package uno;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Pure game logic for UNO: deck building, round setup, turn mechanics,
 * action card effects, scoring, and UNO penalties.
 *
 * No console I/O lives here — all methods operate only on GameState.
 * This makes every rule testable without a Scanner or System.in.
 */
public class GameEngine {

    /**
     * Builds a standard 108-card UNO deck:
     *   4 colors × (1 zero + 2×numbers 1-9 + 2 Skip + 2 Reverse + 2 Draw Two)
     *   + 4 Wild + 4 Wild Draw Four
     */
    public static ArrayList<String> buildDeck() {
        ArrayList<String> deck = new ArrayList<>();
        String[] colors = {"R", "Y", "G", "B"};
        for (String c : colors) {
            deck.add(c + "0");
            for (int n = 1; n <= 9; n++) {
                deck.add(c + n);
                deck.add(c + n);
            }
            deck.add(c + "S");
            deck.add(c + "S");
            deck.add(c + "R");
            deck.add(c + "R");
            deck.add(c + "+2");
            deck.add(c + "+2");
        }
        for (int i = 0; i < 4; i++) {
            deck.add("W");
            deck.add("W4");
        }
        return deck;
    }

    /**
     * Resets hands, deals 7 cards each, places the first non-wild up card,
     * and picks a random starting player.
     */
    public static void setupRound(GameState state, Random rng) {
        state.deck.clear();
        state.deck.addAll(buildDeck());
        Collections.shuffle(state.deck, rng);
        state.discard.clear();
        for (int i = 0; i < state.hands.size(); i++) {
            state.hands.get(i).clear();
        }
        for (int i = 0; i < state.playerNames.size(); i++) {
            for (int j = 0; j < 7; j++) {
                state.hands.get(i).add(draw(state));
            }
        }
        state.upCard = draw(state);
        while (state.upCard.startsWith("W")) {
            state.discard.add(state.upCard);
            state.upCard = draw(state);
        }
        state.calledColor = "";
        state.direction = 1;
        state.currentPlayer = rng.nextInt(state.playerNames.size());
        for (int i = 0; i < state.unoCalled.length; i++) {
            state.unoCalled[i] = false;
        }
    }

    /**
     * Draws the top card from the deck.
     * If the deck is empty, the discard pile is reshuffled into it first.
     * Returns "W" as an emergency fallback when both piles are empty.
     */
    public static String draw(GameState state) {
        if (state.deck.isEmpty()) {
            state.deck.addAll(state.discard);
            state.discard.clear();
            Collections.shuffle(state.deck);
        }
        if (state.deck.isEmpty()) {
            return "W";
        }
        return state.deck.remove(0);
    }

    /** Advances currentPlayer by direction, wrapping around. */
    public static void next(GameState state) {
        state.currentPlayer += state.direction;
        int n = state.playerNames.size();
        if (state.currentPlayer >= n) state.currentPlayer = 0;
        if (state.currentPlayer < 0)  state.currentPlayer = n - 1;
    }

    /**
     * Applies the effect of the card just played and advances the turn.
     *
     * For DRAW_TWO and WILD_DRAW_FOUR, cards are added to the affected
     * player's hand here. Returns the index of the player who was forced
     * to draw cards, or -1 if no draw effect occurred.
     *
     * Two-player REVERSE: acts like SKIP (caller stays, opponent is skipped).
     */
    public static int applyEffect(GameState state, String card) {
        String rank = Card.rank(card);
        switch (rank) {
            case "SKIP":
                next(state);
                next(state);
                return -1;

            case "REVERSE":
                state.direction *= -1;
                if (state.playerNames.size() == 2) {
                    next(state);
                    next(state);
                } else {
                    next(state);
                }
                return -1;

            case "DRAW_TWO": {
                next(state);
                int victim = state.currentPlayer;
                state.hands.get(victim).add(draw(state));
                state.hands.get(victim).add(draw(state));
                next(state);
                return victim;
            }

            case "WILD_DRAW_FOUR": {
                next(state);
                int victim = state.currentPlayer;
                for (int i = 0; i < 4; i++) {
                    state.hands.get(victim).add(draw(state));
                }
                next(state);
                return victim;
            }

            default:
                next(state);
                return -1;
        }
    }

    /**
     * Sums card point values from all non-winner hands and adds them to
     * the winner's cumulative score. Returns the points earned this round.
     */
    public static int scoreRound(GameState state, int winnerIndex) {
        int points = 0;
        for (int i = 0; i < state.hands.size(); i++) {
            if (i != winnerIndex) {
                for (String c : state.hands.get(i)) {
                    points += Card.points(c);
                }
            }
        }
        state.scores[winnerIndex] += points;
        return points;
    }

    /**
     * Applies the missed-UNO penalty: the player at playerIndex draws 2 cards
     * and their unoCalled flag is reset to true (penalty already served).
     */
    public static void applyUnoPenalty(GameState state, int playerIndex) {
        state.hands.get(playerIndex).add(draw(state));
        state.hands.get(playerIndex).add(draw(state));
        state.unoCalled[playerIndex] = true;
    }
}
