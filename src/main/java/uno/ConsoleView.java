package uno;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    // Output methods
    // -------------------------------------------------------------------------

    public void showGameHeader(int gameNumber) {
        if (!quiet) System.out.println("\n=== Game " + gameNumber + " ===");
    }

    public void showTurnState(String upCard, String calledColor, String playerName, ArrayList<String> hand) {
        if (!quiet) {
            System.out.println("\nUp card: " + upCard
                    + (calledColor.equals("") ? "" : " called " + calledColor));
            System.out.println(playerName + " hand: " + join(hand));
        }
    }

    public void showDraw(String playerName, String card) {
        if (!quiet) System.out.println(playerName + " draws " + card);
    }

    public void showPenalty(String playerName) {
        if (!quiet) System.out.println(playerName + " selected an invalid index and draws a penalty card.");
    }

    public void showIllegalCard(String playerName, String card) {
        if (!quiet) System.out.println(playerName + " tried illegal card " + card + " and draws a penalty card.");
    }

    public void showPlay(String playerName, String card) {
        if (!quiet) System.out.println(playerName + " plays " + card);
    }

    public void showCalledColor(String playerName, String color) {
        if (!quiet) System.out.println(playerName + " calls " + color);
    }

    public void showUno(String playerName) {
        if (!quiet) System.out.println(playerName + " says UNO!");
    }

    public void showWin(String playerName, int points) {
        if (!quiet) System.out.println(playerName + " wins and scores " + points);
    }

    public void showDrawTwo(String playerName) {
        if (!quiet) System.out.println(playerName + " draws two.");
    }

    public void showDrawFour(String playerName) {
        if (!quiet) System.out.println(playerName + " draws four.");
    }

    public void showSafetyLimit() {
        if (!quiet) System.out.println("Game stopped at safety limit.");
    }

    public void showFinalScores(ArrayList<String> names, int[] scores) {
        System.out.println("\nFinal scores:");
        for (int i = 0; i < names.size(); i++) {
            System.out.println(names.get(i) + ": " + scores[i]);
        }
    }

    // -------------------------------------------------------------------------
    // Input methods
    // -------------------------------------------------------------------------

    /**
     * Asks a human player to choose a card.
     * Returns the chosen index, or -1 to draw.
     * Preserves original quirk: choosing by index skips the legality check here;
     * legality is enforced later in the game loop (penalty on illegal play).
     */
    public int askHuman(ArrayList<String> hand, String upCard, String calledColor) {
        while (true) {
            System.out.print("Choose card index/code or draw: ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("DRAW")) {
                return -1;
            }
            try {
                int index = Integer.parseInt(input);
                if (index >= 0 && index < hand.size()) {
                    return index;
                }
            } catch (Exception ignored) {
            }
            boolean matched = false;
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).equals(input)) {
                    matched = true;
                    if (Rules.isLegal(hand.get(i), upCard, calledColor)) {
                        return i;
                    }
                    System.out.println("That card is not legal.");
                    log.warn("Invalid input — card '{}' is not legal to play (upCard={} calledColor={})",
                            input, upCard, calledColor.isEmpty() ? "none" : calledColor);
                }
            }
            if (!matched) {
                System.out.println("Card not found.");
                log.warn("Invalid input — '{}' not recognized in hand", input);
            }
        }
    }

    /** Asks a human player to call a color after playing a wild. */
    public String askColor() {
        while (true) {
            System.out.print("Call color R/Y/G/B: ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("R")) return "R";
            if (input.equals("Y")) return "Y";
            if (input.equals("G")) return "G";
            if (input.equals("B")) return "B";
            System.out.println("Bad color.");
            log.warn("Invalid input — '{}' is not a valid color (expected R/Y/G/B)", input);
        }
    }

    /** Asks the human whether to play the card they just drew. */
    public boolean askPlayDrawn(String card) {
        System.out.print("Play drawn card " + card + "? y/n: ");
        String answer = scanner.nextLine();
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
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
