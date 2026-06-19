package uno;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test void color_red()   { assertEquals("R", Card.color("R5")); }
    @Test void color_yellow(){ assertEquals("Y", Card.color("Y3")); }
    @Test void color_green() { assertEquals("G", Card.color("G+2")); }
    @Test void color_blue()  { assertEquals("B", Card.color("B0")); }
    @Test void color_wild_isEmpty()         { assertEquals("", Card.color("W")); }
    @Test void color_wildDrawFour_isEmpty() { assertEquals("", Card.color("W4")); }

    @Test void rank_drawTwo_green()  { assertEquals("DRAW_TWO",       Card.rank("G+2")); }
    @Test void rank_skip_red()       { assertEquals("SKIP",           Card.rank("RS")); }
    @Test void rank_skip_yellow()    { assertEquals("SKIP",           Card.rank("YS")); }
    @Test void rank_skip_blue()      { assertEquals("SKIP",           Card.rank("BS")); }
    @Test void rank_skip_green()     { assertEquals("SKIP",           Card.rank("GS")); }
    @Test void rank_reverse_blue()   { assertEquals("REVERSE",        Card.rank("BR")); }
    @Test void rank_reverse_red()    { assertEquals("REVERSE",        Card.rank("RR")); }
    @Test void rank_reverse_green()  { assertEquals("REVERSE",        Card.rank("GR")); }
    @Test void rank_drawTwo_red()    { assertEquals("DRAW_TWO",       Card.rank("R+2")); }
    @Test void rank_drawTwo_blue()   { assertEquals("DRAW_TWO",       Card.rank("B+2")); }
    @Test void rank_wild()           { assertEquals("WILD",           Card.rank("W")); }
    @Test void rank_wildDrawFour()   { assertEquals("WILD_DRAW_FOUR", Card.rank("W4")); }
    @Test void rank_number()         { assertEquals("NUMBER",         Card.rank("R5")); }

    @Test void points_wildDrawFour() { assertEquals(50, Card.points("W4")); }
    @Test void points_wild()         { assertEquals(50, Card.points("W")); }
    @Test void points_skip()         { assertEquals(20, Card.points("RS")); }
    @Test void points_reverse()      { assertEquals(20, Card.points("YR")); }
    @Test void points_drawTwo()      { assertEquals(20, Card.points("B+2")); }
    @Test void points_number7()      { assertEquals(7,  Card.points("R7")); }
    @Test void points_zero()         { assertEquals(0,  Card.points("G0")); }
}
