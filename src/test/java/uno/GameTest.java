package uno;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Tests for game-loop internals: next(), draw(), scoring, and ConsoleView.join().
 * Uses @BeforeEach to isolate each test from Main's static state.
 */
class GameTest {

    @BeforeEach
    void resetState() {
        Main.playerNames.clear();
        Main.humanPlayers.clear();
        Main.hands.clear();
        Main.deck.clear();
        Main.discard.clear();
        Main.currentPlayer = 0;
        Main.direction = 1;
        Main.upCard = "";
        Main.calledColor = "";
        Main.random = new Random(42);
    }

    // --- next() forward movement ---

    @Test void next_forwardFrom0to1() {
        Main.playerNames.add("A"); Main.playerNames.add("B"); Main.playerNames.add("C");
        Main.currentPlayer = 0; Main.direction = 1;
        Main.next();
        assertEquals(1, Main.currentPlayer);
    }

    @Test void next_forwardWraps2to0() {
        Main.playerNames.add("A"); Main.playerNames.add("B"); Main.playerNames.add("C");
        Main.currentPlayer = 2; Main.direction = 1;
        Main.next();
        assertEquals(0, Main.currentPlayer);
    }

    // --- next() reverse movement ---

    @Test void next_reverseFrom1to0() {
        Main.playerNames.add("A"); Main.playerNames.add("B"); Main.playerNames.add("C");
        Main.currentPlayer = 1; Main.direction = -1;
        Main.next();
        assertEquals(0, Main.currentPlayer);
    }

    @Test void next_reverseWraps0to2() {
        Main.playerNames.add("A"); Main.playerNames.add("B"); Main.playerNames.add("C");
        Main.currentPlayer = 0; Main.direction = -1;
        Main.next();
        assertEquals(2, Main.currentPlayer);
    }

    // --- draw() refill from discard ---

    @Test void draw_refillsDeckFromDiscard() {
        // deck is empty; discard has 3 cards
        Main.discard.add("R1"); Main.discard.add("B3"); Main.discard.add("G7");
        Main.random = new Random(0);
        String drawn = Main.draw();
        // drew a real card (not the emergency wild), and 2 cards remain in deck
        assertNotEquals("W", drawn);
        assertEquals(2, Main.deck.size());
        assertTrue(Main.discard.isEmpty());
    }

    @Test void draw_returnsEmergencyWildWhenBothEmpty() {
        // both deck and discard are empty
        assertEquals("W", Main.draw());
    }

    @Test void draw_takesFromTopOfDeck() {
        Main.deck.add("R5");
        Main.deck.add("G3");
        // draws from index 0
        assertEquals("R5", Main.draw());
        assertEquals(1, Main.deck.size());
    }

    // --- Scoring via Card.points() ---

    @Test void scoring_numberCard5()   { assertEquals(5,  Card.points("R5")); }
    @Test void scoring_zero()          { assertEquals(0,  Card.points("B0")); }
    @Test void scoring_skip20()        { assertEquals(20, Card.points("YS")); }
    @Test void scoring_reverse20()     { assertEquals(20, Card.points("GR")); }
    @Test void scoring_drawTwo20()     { assertEquals(20, Card.points("R+2")); }
    @Test void scoring_wild50()        { assertEquals(50, Card.points("W")); }
    @Test void scoring_wildDrawFour50(){ assertEquals(50, Card.points("W4")); }

    // --- ConsoleView.join() ---

    @Test void consoleViewJoin_formatsCorrectly() {
        ArrayList<String> hand = new ArrayList<>();
        hand.add("R5"); hand.add("W");
        assertEquals("0:R5 1:W", ConsoleView.join(hand));
    }

    @Test void consoleViewJoin_emptyHandIsEmpty() {
        assertEquals("", ConsoleView.join(new ArrayList<>()));
    }

    @Test void consoleViewJoin_singleCard() {
        ArrayList<String> hand = new ArrayList<>();
        hand.add("G+2");
        assertEquals("0:G+2", ConsoleView.join(hand));
    }
}
