package uno;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RulesTest {

    @Test void legal_sameColor()              { assertTrue(Rules.isLegal("R2",  "R9",  "")); }
    @Test void legal_sameNumber()             { assertTrue(Rules.isLegal("G9",  "R9",  "")); }
    @Test void legal_sameNumberAcrossColors() { assertTrue(Rules.isLegal("B9",  "R9",  "")); }
    @Test void legal_zeroMatchesZero()        { assertTrue(Rules.isLegal("G0",  "Y0",  "")); }
    @Test void legal_calledColorBlue()        { assertTrue(Rules.isLegal("B3",  "W",   "B")); }
    @Test void legal_calledColorGreen()       { assertTrue(Rules.isLegal("G3",  "W",   "G")); }
    @Test void legal_wildAlwaysPlayable()     { assertTrue(Rules.isLegal("W",   "R9",  "")); }
    @Test void legal_wildDrawFourAlways()     { assertTrue(Rules.isLegal("W4",  "G5",  "")); }
    @Test void legal_wildOnAnyCard()          { assertTrue(Rules.isLegal("W",   "R5",  "")); }
    @Test void legal_wildDrawFourOnAnyCard()  { assertTrue(Rules.isLegal("W4",  "G0",  "")); }
    @Test void legal_sameActionSkip()         { assertTrue(Rules.isLegal("RS",  "YS",  "")); }
    @Test void legal_skipAcrossColors()       { assertTrue(Rules.isLegal("GS",  "BS",  "")); }
    @Test void legal_sameActionReverse()      { assertTrue(Rules.isLegal("GR",  "BR",  "")); }
    @Test void legal_reverseAcrossColors()    { assertTrue(Rules.isLegal("RR",  "BR",  "")); }
    @Test void legal_sameActionDrawTwo()      { assertTrue(Rules.isLegal("R+2", "G+2", "")); }
    @Test void legal_drawTwoAcrossColors()    { assertTrue(Rules.isLegal("Y+2", "R+2", "")); }
    @Test void legal_matchesCalledColor()     { assertTrue(Rules.isLegal("G5",  "W",   "G")); }

    @Test void illegal_colorAndNumberMismatch() { assertFalse(Rules.isLegal("B3", "R9", "")); }
    @Test void illegal_wrongCalledColor()       { assertFalse(Rules.isLegal("R3", "W",  "G")); }
    @Test void illegal_wrongCalledColorNumber() { assertFalse(Rules.isLegal("R5", "W",  "G")); }
}
