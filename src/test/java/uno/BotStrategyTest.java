package uno;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;

class BotStrategyTest {

    private ArrayList<String> hand(String... cards) {
        return new ArrayList<>(Arrays.asList(cards));
    }

    // --- selectCard() priority order ---

    @Test void prefersNumberOverWild() {
        // hand: B3(illegal), R4(legal number), W(wild)  — upCard R9
        assertEquals(1, BotStrategy.selectCard(hand("B3", "R4", "W"), "R9", ""));
    }

    @Test void prefersDrawTwoOverNumber() {
        // hand: R5(number), R+2(draw two), W  — upCard R9
        assertEquals(1, BotStrategy.selectCard(hand("R5", "R+2", "W"), "R9", ""));
    }

    @Test void prefersSkipOverNumber() {
        // hand: R5(number), RS(skip)  — upCard R9
        assertEquals(1, BotStrategy.selectCard(hand("R5", "RS"), "R9", ""));
    }

    @Test void playsWildWhenOnlyLegalOption() {
        // hand: G3(illegal on R9), W  — upCard R9
        assertEquals(1, BotStrategy.selectCard(hand("G3", "W"), "R9", ""));
    }

    @Test void drawsWhenNothingLegal() {
        // hand: G3, B5  — both illegal on R9 and no wilds
        assertEquals(-1, BotStrategy.selectCard(hand("G3", "B5"), "R9", ""));
    }

    // --- selectColor() ---

    @Test void colorPicksMostFrequent() {
        // B appears twice, R once → should pick B
        assertEquals("B", BotStrategy.selectColor(hand("B1", "B2", "R3")));
    }

    @Test void colorPicksRedWhenTie() {
        // R and Y both appear once — R wins due to >= logic
        String color = BotStrategy.selectColor(hand("R1", "Y1"));
        assertEquals("R", color);
    }

    @Test void colorDefaultsToBlueWhenEmpty() {
        // empty hand — falls through to return "B"
        assertEquals("B", BotStrategy.selectColor(hand()));
    }
}
