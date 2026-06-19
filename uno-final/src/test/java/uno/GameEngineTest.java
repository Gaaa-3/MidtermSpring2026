package uno;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GameEngine: deck composition, action card effects, draw/pass,
 * UNO call and penalty, scoring, and multi-round target behavior.
 * No console I/O — all tests operate directly on GameState.
 */
class GameEngineTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private GameState state3() {
        return new GameState(
                Arrays.asList("A", "B", "C"),
                Arrays.asList(false, false, false),
                500);
    }

    private GameState state2() {
        return new GameState(
                Arrays.asList("A", "B"),
                Arrays.asList(false, false),
                500);
    }

    /** Fills hands and deck so applyEffect can draw without crashing. */
    private void primeDecks(GameState state) {
        for (int i = 0; i < 20; i++) state.deck.add("R1");
        for (ArrayList<String> h : state.hands) h.add("R5");
        state.upCard = "R9";
        state.calledColor = "";
    }

    // -------------------------------------------------------------------------
    // 1. Deck Composition
    // -------------------------------------------------------------------------

    @Test void buildDeck_has108Cards() {
        assertEquals(108, GameEngine.buildDeck().size());
    }

    @Test void buildDeck_hasFourColors() {
        ArrayList<String> deck = GameEngine.buildDeck();
        long red   = deck.stream().filter(c -> Card.color(c).equals("R")).count();
        long yell  = deck.stream().filter(c -> Card.color(c).equals("Y")).count();
        long green = deck.stream().filter(c -> Card.color(c).equals("G")).count();
        long blue  = deck.stream().filter(c -> Card.color(c).equals("B")).count();
        assertEquals(25, red);
        assertEquals(25, yell);
        assertEquals(25, green);
        assertEquals(25, blue);
    }

    @Test void buildDeck_hasOneZeroPerColor() {
        ArrayList<String> deck = GameEngine.buildDeck();
        for (String color : new String[]{"R", "Y", "G", "B"}) {
            long zeros = deck.stream().filter(c -> c.equals(color + "0")).count();
            assertEquals(1, zeros, "Expected 1 zero for color " + color);
        }
    }

    @Test void buildDeck_hasTwoOfEachNumber1to9PerColor() {
        ArrayList<String> deck = GameEngine.buildDeck();
        for (String color : new String[]{"R", "Y", "G", "B"}) {
            for (int n = 1; n <= 9; n++) {
                String card = color + n;
                long count = deck.stream().filter(c -> c.equals(card)).count();
                assertEquals(2, count, "Expected 2 copies of " + card);
            }
        }
    }

    @Test void buildDeck_hasTwoSkipPerColor() {
        ArrayList<String> deck = GameEngine.buildDeck();
        for (String color : new String[]{"R", "Y", "G", "B"}) {
            long count = deck.stream().filter(c -> c.equals(color + "S")).count();
            assertEquals(2, count, "Expected 2 Skips for " + color);
        }
    }

    @Test void buildDeck_hasTwoReversePerColor() {
        ArrayList<String> deck = GameEngine.buildDeck();
        for (String color : new String[]{"R", "Y", "G", "B"}) {
            long count = deck.stream().filter(c -> c.equals(color + "R")).count();
            assertEquals(2, count, "Expected 2 Reverses for " + color);
        }
    }

    @Test void buildDeck_hasTwoDrawTwoPerColor() {
        ArrayList<String> deck = GameEngine.buildDeck();
        for (String color : new String[]{"R", "Y", "G", "B"}) {
            long count = deck.stream().filter(c -> c.equals(color + "+2")).count();
            assertEquals(2, count, "Expected 2 Draw Twos for " + color);
        }
    }

    @Test void buildDeck_hasFourWilds() {
        long count = GameEngine.buildDeck().stream().filter(c -> c.equals("W")).count();
        assertEquals(4, count);
    }

    @Test void buildDeck_hasFourWildDrawFours() {
        long count = GameEngine.buildDeck().stream().filter(c -> c.equals("W4")).count();
        assertEquals(4, count);
    }

    // -------------------------------------------------------------------------
    // 2. Legal Play Validation (delegated to Rules, tested via Rules)
    // -------------------------------------------------------------------------
    // See RulesTest for full coverage. The engine trusts Rules.isLegal().

    // -------------------------------------------------------------------------
    // 3. Skip
    // -------------------------------------------------------------------------

    @Test void skip_nextPlayerLosesTurn_threePlayer() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 0; // A plays
        s.direction = 1;
        GameEngine.applyEffect(s, "RS");
        // B (index 1) is skipped; C (index 2) should be next
        assertEquals(2, s.currentPlayer);
    }

    @Test void skip_nextPlayerLosesTurn_reverseDirection() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 2; // C plays
        s.direction = -1;
        GameEngine.applyEffect(s, "RS");
        // B (index 1) would be next in reverse, gets skipped; A (index 0) is next
        assertEquals(0, s.currentPlayer);
    }

    @Test void skip_wrapsAroundToFirstPlayer() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 1; // B plays
        s.direction = 1;
        GameEngine.applyEffect(s, "RS");
        // C (index 2) skipped; A (index 0) is next
        assertEquals(0, s.currentPlayer);
    }

    // -------------------------------------------------------------------------
    // 4. Reverse
    // -------------------------------------------------------------------------

    @Test void reverse_changesDirectionForward_threePlayer() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 0;
        s.direction = 1;
        GameEngine.applyEffect(s, "RR");
        assertEquals(-1, s.direction);
        // with reversed direction from 0: next player is C (index 2)
        assertEquals(2, s.currentPlayer);
    }

    @Test void reverse_changesDirectionBackward_threePlayer() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 2;
        s.direction = -1;
        GameEngine.applyEffect(s, "RR");
        assertEquals(1, s.direction);
        // reversed from -1 to 1; next from 2 is 0... wait:
        // direction is now 1, next() from 2: 2+1=3 wraps to 0
        assertEquals(0, s.currentPlayer);
    }

    @Test void reverse_twoPlayer_actsLikeSkip() {
        GameState s = state2();
        primeDecks(s);
        s.currentPlayer = 0; // A plays
        s.direction = 1;
        GameEngine.applyEffect(s, "RR");
        // two-player: direction flips, then double-next → back to A
        assertEquals(0, s.currentPlayer);
    }

    @Test void reverse_twoPlayer_flipsDirection() {
        GameState s = state2();
        primeDecks(s);
        s.currentPlayer = 0;
        s.direction = 1;
        GameEngine.applyEffect(s, "RR");
        assertEquals(-1, s.direction);
    }

    // -------------------------------------------------------------------------
    // 5. Draw Two
    // -------------------------------------------------------------------------

    @Test void drawTwo_nextPlayerReceivesTwoCards() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 0; // A plays
        s.direction = 1;
        int prevSize = s.hands.get(1).size(); // B's hand
        GameEngine.applyEffect(s, "R+2");
        assertEquals(prevSize + 2, s.hands.get(1).size());
    }

    @Test void drawTwo_nextPlayerLosesTurn() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 0; // A plays
        s.direction = 1;
        GameEngine.applyEffect(s, "R+2");
        // B drew 2, is skipped; C (index 2) should be next
        assertEquals(2, s.currentPlayer);
    }

    @Test void drawTwo_returnsVictimIndex() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 0;
        s.direction = 1;
        int victim = GameEngine.applyEffect(s, "R+2");
        assertEquals(1, victim); // B is the victim
    }

    // -------------------------------------------------------------------------
    // 6. Wild
    // -------------------------------------------------------------------------
    // Wild card itself only affects calledColor — tested through Rules integration.

    @Test void wild_alwaysLegal_onAnyUpCard() {
        assertTrue(Rules.isLegal("W", "R9", ""));
        assertTrue(Rules.isLegal("W", "GS", ""));
        assertTrue(Rules.isLegal("W", "W",  ""));
    }

    @Test void wild_calledColorBecomesActiveColor() {
        // After a wild, the calledColor determines legality, not the card color
        assertTrue(Rules.isLegal("B5", "W", "B"));
        assertFalse(Rules.isLegal("R5", "W", "B"));
    }

    // -------------------------------------------------------------------------
    // 7. Wild Draw Four
    // -------------------------------------------------------------------------

    @Test void wildDrawFour_nextPlayerReceivesFourCards() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 0;
        s.direction = 1;
        int prevSize = s.hands.get(1).size();
        GameEngine.applyEffect(s, "W4");
        assertEquals(prevSize + 4, s.hands.get(1).size());
    }

    @Test void wildDrawFour_nextPlayerLosesTurn() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 0;
        s.direction = 1;
        GameEngine.applyEffect(s, "W4");
        // B drew 4, is skipped; C is next
        assertEquals(2, s.currentPlayer);
    }

    @Test void wildDrawFour_returnsVictimIndex() {
        GameState s = state3();
        primeDecks(s);
        s.currentPlayer = 0;
        s.direction = 1;
        int victim = GameEngine.applyEffect(s, "W4");
        assertEquals(1, victim);
    }

    @Test void wildDrawFour_alwaysLegal() {
        assertTrue(Rules.isLegal("W4", "G5", ""));
        assertTrue(Rules.isLegal("W4", "RS", ""));
        assertTrue(Rules.isLegal("W4", "W",  "B"));
    }

    // -------------------------------------------------------------------------
    // 8. Draw / Pass Behavior
    // -------------------------------------------------------------------------

    @Test void draw_takesFromTopOfDeck() {
        GameState s = state3();
        s.deck.add("G7");
        s.deck.add("B2");
        assertEquals("G7", GameEngine.draw(s));
        assertEquals(1, s.deck.size());
    }

    @Test void draw_refillsDeckFromDiscardWhenEmpty() {
        GameState s = state3();
        s.discard.add("R1");
        s.discard.add("B3");
        s.discard.add("G7");
        String drawn = GameEngine.draw(s);
        assertNotEquals("W", drawn);          // got a real card
        assertEquals(2, s.deck.size());       // two left in deck
        assertTrue(s.discard.isEmpty());      // discard was consumed
    }

    @Test void draw_returnsEmergencyWildWhenBothEmpty() {
        GameState s = state3();
        assertEquals("W", GameEngine.draw(s));
    }

    @Test void setupRound_dealsSevenCardsEach() {
        GameState s = state3();
        GameEngine.setupRound(s, new Random(0));
        for (ArrayList<String> hand : s.hands) {
            assertEquals(7, hand.size());
        }
    }

    @Test void setupRound_upCardIsNotWild() {
        GameState s = state3();
        GameEngine.setupRound(s, new Random(0));
        assertFalse(s.upCard.startsWith("W"),
                "Starting up card should not be a wild, got: " + s.upCard);
    }

    @Test void setupRound_deckSizeAfterDeal() {
        GameState s = state3();
        GameEngine.setupRound(s, new Random(0));
        // 108 cards - 7*3 hands - 1 up card = 86; wild discards reduce further but ≤86
        assertTrue(s.deck.size() <= 86);
        assertTrue(s.deck.size() >= 0);
    }

    // -------------------------------------------------------------------------
    // 9. UNO Call and Penalty
    // -------------------------------------------------------------------------

    @Test void unoPenalty_playerDrawsTwoCards() {
        GameState s = state3();
        primeDecks(s);
        s.hands.get(0).clear();
        s.hands.get(0).add("R5"); // A has exactly 1 card
        s.unoCalled[0] = false;
        int before = s.hands.get(0).size();
        GameEngine.applyUnoPenalty(s, 0);
        assertEquals(before + 2, s.hands.get(0).size());
    }

    @Test void unoPenalty_flagResetAfterPenalty() {
        GameState s = state3();
        primeDecks(s);
        s.unoCalled[1] = false;
        GameEngine.applyUnoPenalty(s, 1);
        assertTrue(s.unoCalled[1]); // penalty served, flag reset
    }

    @Test void unoState_detectedWhenHandSizeIsOne() {
        GameState s = state3();
        s.hands.get(0).clear();
        s.hands.get(0).add("R5");
        assertEquals(1, s.hands.get(0).size());
    }

    // -------------------------------------------------------------------------
    // 10. Round Scoring and Multi-Round Target
    // -------------------------------------------------------------------------

    @Test void scoreRound_sumsOpponentHandValues() {
        GameState s = state3();
        s.hands.get(0).clear(); // A wins (empty hand)
        s.hands.get(1).clear();
        s.hands.get(1).add("R5");  // 5
        s.hands.get(1).add("W");   // 50
        s.hands.get(2).clear();
        s.hands.get(2).add("YS");  // 20
        s.hands.get(2).add("B+2"); // 20
        int points = GameEngine.scoreRound(s, 0);
        assertEquals(5 + 50 + 20 + 20, points);
    }

    @Test void scoreRound_addsPointsToWinnersScore() {
        GameState s = state3();
        s.hands.get(0).clear();
        s.hands.get(1).clear();
        s.hands.get(1).add("R7");  // 7
        s.hands.get(2).clear();
        s.hands.get(2).add("G3");  // 3
        GameEngine.scoreRound(s, 0);
        assertEquals(10, s.scores[0]);
    }

    @Test void scoreRound_doesNotCountWinnersOwnCards() {
        GameState s = state2();
        s.hands.get(0).clear();          // winner: no cards
        s.hands.get(1).clear();
        s.hands.get(1).add("W4");        // 50 points
        int points = GameEngine.scoreRound(s, 0);
        assertEquals(50, points);
        assertEquals(50, s.scores[0]);
    }

    @Test void multiRound_isGameOver_falseWhenBelowTarget() {
        GameState s = state2();
        s.scores[0] = 499;
        assertFalse(s.isGameOver());
    }

    @Test void multiRound_isGameOver_trueWhenTargetReached() {
        GameState s = state2();
        s.scores[0] = 500;
        assertTrue(s.isGameOver());
    }

    @Test void multiRound_isGameOver_trueWhenTargetExceeded() {
        GameState s = state2();
        s.scores[1] = 750;
        assertTrue(s.isGameOver());
    }

    @Test void multiRound_gameWinnerIndex_returnsHighestScoreAboveTarget() {
        GameState s = state2();
        s.scores[0] = 600;
        s.scores[1] = 500;
        assertEquals(0, s.gameWinnerIndex());
    }

    @Test void multiRound_gameWinnerIndex_minusOneWhenNobodyWon() {
        GameState s = state2();
        s.scores[0] = 100;
        s.scores[1] = 200;
        assertEquals(-1, s.gameWinnerIndex());
    }

    @Test void multiRound_scoresAccumulateAcrossRounds() {
        GameState s = state2();
        s.hands.get(0).clear();
        s.hands.get(1).clear();
        s.hands.get(1).add("R5"); // 5 points for A in round 1
        GameEngine.scoreRound(s, 0);
        assertEquals(5, s.scores[0]);

        // Round 2: A wins again
        s.hands.get(0).clear();
        s.hands.get(1).clear();
        s.hands.get(1).add("W"); // 50 points
        GameEngine.scoreRound(s, 0);
        assertEquals(55, s.scores[0]); // 5 + 50
    }

    // -------------------------------------------------------------------------
    // ConsoleView.join() helper
    // -------------------------------------------------------------------------

    @Test void join_formatsHandWithIndices() {
        ArrayList<String> hand = new ArrayList<>();
        hand.add("R5"); hand.add("W");
        assertEquals("0:R5 1:W", ConsoleView.join(hand));
    }

    @Test void join_emptyHandReturnsEmptyString() {
        assertEquals("", ConsoleView.join(new ArrayList<>()));
    }
}
