package uno;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleView {

    private static final Logger log = LoggerFactory.getLogger(ConsoleView.class);

    private final boolean quiet;
    private final Scanner scanner;

    public ConsoleView(boolean quiet, Scanner scanner) {
        this.quiet = quiet;
        this.scanner = scanner;
    }

    // -------------------------------------------------------------------------
    // Game / round headers
    // -------------------------------------------------------------------------

    public void showGameStart(List<String> playerNames, int targetScore) {
        if (!quiet) {
            System.out.println("\n=== UNO — first to " + targetScore + " points wins ===");
            System.out.println("Players: " + String.join(", ", playerNames));
        }
    }

    public void showRoundHeader(int roundNumber) {
        if (!quiet) System.out.println("\n--- Round " + roundNumber + " ---");
    }

    // -------------------------------------------------------------------------
    // Turn display
    // -------------------------------------------------------------------------

    public void showTurnState(String upCard, String calledColor, String playerName, ArrayList<String> hand) {
        if (!quiet) {
            System.out.println("\nUp card: " + upCard
                    + (calledColor.equals("") ? "" : " (called " + calledColor + ")"));
            System.out.println(playerName + "'s hand: " + join(hand));
        }
    }

    public void showDraw(String playerName, String card) {
        if (!quiet) System.out.println(playerName + " draws " + card);
    }

    public void showPlay(String playerName, String card) {
        if (!quiet) System.out.println(playerName + " plays " + card);
    }

    public void showCalledColor(String playerName, String color) {
        if (!quiet) System.out.println(playerName + " calls color " + color);
    }

    public void showDrawTwo(String playerName) {
        if (!quiet) System.out.println(playerName + " draws 2 and loses a turn.");
    }

    public void showDrawFour(String playerName) {
        if (!quiet) System.out.println(playerName + " draws 4 and loses a turn.");
    }

    public void showSkip(String playerName) {
        if (!quiet) System.out.println(playerName + " is skipped.");
    }

    public void showReverse(int playerCount) {
        if (!quiet) {
            if (playerCount == 2) System.out.println("Reverse! (acts like Skip in 2-player)");
            else System.out.println("Reverse! Turn order changed.");
        }
    }

    // -------------------------------------------------------------------------
    // UNO
    // -------------------------------------------------------------------------

    public void showUno(String playerName) {
        if (!quiet) System.out.println(playerName + " says UNO!");
    }

    public void showUnoPenalty(String playerName) {
        if (!quiet) System.out.println(playerName + " forgot to say UNO — draws 2 penalty cards.");
    }

    // -------------------------------------------------------------------------
    // Penalties and errors
    // -------------------------------------------------------------------------

    public void showPenalty(String playerName) {
        if (!quiet) System.out.println(playerName + " selected an invalid card — draws 1 penalty card.");
    }

    public void showIllegalCard(String playerName, String card) {
        if (!quiet) System.out.println(playerName + " tried illegal card " + card + " — draws 1 penalty card.");
    }

    public void showSafetyLimit() {
        if (!quiet) System.out.println("Round stopped at safety limit.");
    }

    // -------------------------------------------------------------------------
    // Round / game end
    // -------------------------------------------------------------------------

    public void showRoundWinner(String playerName, int points) {
        if (!quiet) System.out.println(playerName + " wins the round and scores " + points + " points!");
    }

    public void showRoundScores(List<String> names, int[] scores) {
        if (!quiet) {
            System.out.println("Scores after this round:");
            for (int i = 0; i < names.size(); i++) {
                System.out.println("  " + names.get(i) + ": " + scores[i]);
            }
        }
    }

    public void showGameWinner(String playerName, int finalScore) {
        System.out.println("\n*** " + playerName + " wins the game with " + finalScore + " points! ***");
    }

    public void showFinalScores(List<String> names, int[] scores) {
        System.out.println("\nFinal scores:");
        for (int i = 0; i < names.size(); i++) {
            System.out.println("  " + names.get(i) + ": " + scores[i]);
        }
    }

    // -------------------------------------------------------------------------
    // Human input
    // -------------------------------------------------------------------------

    /**
     * Prompts the human for a card to play or "draw".
     * Accepts a numeric index or a card code (e.g. "R5", "W4").
     * Returns the hand index, or -1 to draw.
     */
    public int askHuman(ArrayList<String> hand, String upCard, String calledColor) {
        while (true) {
            System.out.print("Play (index/code) or 'draw': ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("DRAW")) {
                return -1;
            }
            try {
                int index = Integer.parseInt(input);
                if (index >= 0 && index < hand.size()) {
                    return index;
                }
            } catch (NumberFormatException ignored) {
            }
            boolean found = false;
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).equals(input)) {
                    found = true;
                    if (Rules.isLegal(hand.get(i), upCard, calledColor)) {
                        return i;
                    }
                    System.out.println("That card is not legal to play now.");
                    log.warn("Illegal play attempt — card={} upCard={} calledColor={}", input, upCard,
                            calledColor.isEmpty() ? "none" : calledColor);
                }
            }
            if (!found) {
                System.out.println("Card '" + input + "' not found in hand.");
                log.warn("Unrecognized input — '{}'", input);
            }
        }
    }

    /** Asks the human whether to play the card they just drew. */
    public boolean askPlayDrawn(String card) {
        System.out.print("Play drawn card " + card + "? (y/n): ");
        String answer = scanner.nextLine().trim();
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    /** Asks the human to call a color after playing a wild. */
    public String askColor() {
        while (true) {
            System.out.print("Call color (R/Y/G/B): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("R") || input.equals("Y") || input.equals("G") || input.equals("B")) {
                return input;
            }
            System.out.println("Enter R, Y, G, or B.");
            log.warn("Bad color input — '{}'", input);
        }
    }

    /**
     * Prompts the human to say UNO after playing down to 1 card.
     * Returns true if they said UNO (saved), false if they skipped (penalty).
     */
    public boolean askUno() {
        System.out.print("You have 1 card! Say UNO? (y/n): ");
        String answer = scanner.nextLine().trim();
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("uno") || answer.equalsIgnoreCase("yes");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    public static String join(ArrayList<String> cards) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) out.append(" ");
            out.append(i).append(":").append(cards.get(i));
        }
        return out.toString();
    }
}
