package uno;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all mutable state for one game session.
 * GameEngine reads and mutates this object; ConsoleView and Main read it for display.
 */
public class GameState {

    public final List<String> playerNames;
    public final List<Boolean> humanFlags;
    public final List<ArrayList<String>> hands;
    public final ArrayList<String> deck;
    public final ArrayList<String> discard;
    public final int[] scores;
    public final boolean[] unoCalled;
    public final int targetScore;

    public int currentPlayer;
    public int direction;
    public String upCard;
    public String calledColor;

    public GameState(List<String> names, List<Boolean> humans, int targetScore) {
        this.playerNames = new ArrayList<>(names);
        this.humanFlags = new ArrayList<>(humans);
        this.targetScore = targetScore;
        this.scores = new int[names.size()];
        this.unoCalled = new boolean[names.size()];
        this.hands = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            this.hands.add(new ArrayList<>());
        }
        this.deck = new ArrayList<>();
        this.discard = new ArrayList<>();
        this.currentPlayer = 0;
        this.direction = 1;
        this.upCard = "";
        this.calledColor = "";
    }

    public ArrayList<String> currentHand() {
        return hands.get(currentPlayer);
    }

    public String currentPlayerName() {
        return playerNames.get(currentPlayer);
    }

    public boolean currentIsHuman() {
        return humanFlags.get(currentPlayer);
    }

    public boolean isGameOver() {
        for (int s : scores) {
            if (s >= targetScore) return true;
        }
        return false;
    }

    /** Returns the index of the player whose score first reached targetScore, or -1 if none yet. */
    public int gameWinnerIndex() {
        int best = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] >= targetScore) {
                if (best == -1 || scores[i] > scores[best]) best = i;
            }
        }
        return best;
    }
}
