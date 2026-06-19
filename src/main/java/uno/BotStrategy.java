package uno;

import java.util.ArrayList;

public class BotStrategy {

    /**
     * Selects the index of the card the bot wants to play, or -1 to draw.
     * Priority: DRAW_TWO > SKIP > REVERSE > NUMBER > Wild (last resort).
     */
    public static int selectCard(ArrayList<String> hand, String upCard, String calledColor) {
        for (int i = 0; i < hand.size(); i++) {
            if (Card.rank(hand.get(i)).equals("DRAW_TWO") && Rules.isLegal(hand.get(i), upCard, calledColor)) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            if (Card.rank(hand.get(i)).equals("SKIP") && Rules.isLegal(hand.get(i), upCard, calledColor)) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            if (Card.rank(hand.get(i)).equals("REVERSE") && Rules.isLegal(hand.get(i), upCard, calledColor)) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            if (Card.rank(hand.get(i)).equals("NUMBER") && Rules.isLegal(hand.get(i), upCard, calledColor)) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).startsWith("W")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Selects the color the bot will call after playing a wild card.
     * Picks the color it holds the most of; defaults to B on empty hand.
     */
    public static String selectColor(ArrayList<String> hand) {
        int r = 0, y = 0, g = 0, b = 0;
        for (int i = 0; i < hand.size(); i++) {
            String c = Card.color(hand.get(i));
            if (c.equals("R")) r++;
            else if (c.equals("Y")) y++;
            else if (c.equals("G")) g++;
            else if (c.equals("B")) b++;
        }
        if (r >= y && r >= g && r >= b) return "R";
        if (y >= r && y >= g && y >= b) return "Y";
        if (g >= r && g >= y && g >= b) return "G";
        return "B";
    }
}
