public class Rules {

    public static boolean isLegal(String card, String up, String calledColor) {
        if (card.startsWith("W")) return true;
        if (Card.color(card).equals(Card.color(up))) return true;
        if (!calledColor.equals("") && Card.color(card).equals(calledColor)) return true;
        if (Card.rank(card).equals(Card.rank(up)) && !Card.rank(card).equals("NUMBER")) return true;
        if (Card.rank(card).equals("NUMBER") && Card.rank(up).equals("NUMBER")
                && Card.number(card) == Card.number(up)) return true;
        return false;
    }
}
