package uno;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;

class BotStrategyTest {

    private ArrayList<String> hand(String... cards) {
        return new ArrayList<>(Arrays.asList(cards));
    }

    @Test void prefersNumberOverWild() {
        assertEquals(1, BotStrategy.selectCard(hand("B3", "R4", "W"), "R9", ""));
    }

    @Test void prefersDrawTwoOverNumber() {
        assertEquals(1, BotStrategy.selectCard(hand("R5", "R+2", "W"), "R9", ""));
    }

    @Test void prefersSkipOverNumber() {
        assertEquals(1, BotStrategy.selectCard(hand("R5", "RS"), "R9", ""));
    }

    @Test void prefersReverseOverNumber() {
        assertEquals(1, BotStrategy.selectCard(hand("R5", "RR"), "R9", ""));
    }

    @Test void prefersDrawTwoOverSkip() {
        assertEquals(0, BotStrategy.selectCard(hand("R+2", "RS"), "R9", ""));
    }

    @Test void prefersSkipOverReverse() {
        assertEquals(0, BotStrategy.selectCard(hand("RS", "RR"), "R9", ""));
    }

    @Test void playsWildWhenOnlyLegalOption() {
        assertEquals(1, BotStrategy.selectCard(hand("G3", "W"), "R9", ""));
    }

    @Test void drawsWhenNothingLegal() {
        assertEquals(-1, BotStrategy.selectCard(hand("G3", "B5"), "R9", ""));
    }

    @Test void colorPicksMostFrequent() {
        assertEquals("B", BotStrategy.selectColor(hand("B1", "B2", "R3")));
    }

    @Test void colorPicksRedWhenTie() {
        assertEquals("R", BotStrategy.selectColor(hand("R1", "Y1")));
    }

    @Test void colorDefaultsToRedWhenEmpty() {
        // empty hand: all counts are 0, first condition (r>=y&&r>=g&&r>=b) is true → returns R
        assertEquals("R", BotStrategy.selectColor(hand()));
    }
}
