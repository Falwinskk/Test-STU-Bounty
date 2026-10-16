package de.fabs.bounty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeldTest {

    @Test
    void parseNormaleBetraege() {
        assertEquals(10000, Geld.parseCent("100"));
        assertEquals(12345, Geld.parseCent("123.45"));
        assertEquals(12345, Geld.parseCent("123,45"));
        assertEquals(12345, Geld.parseCent("123.459"));
        assertEquals(50, Geld.parseCent("0.5"));
    }

    @Test
    void parseUngueltigeBetraege() {
        assertEquals(-1, Geld.parseCent("abc"));
        assertEquals(-1, Geld.parseCent(""));
        assertEquals(-1, Geld.parseCent(null));
        assertEquals(-1, Geld.parseCent("0"));
        assertEquals(-1, Geld.parseCent("-5"));
        assertEquals(-1, Geld.parseCent("NaN"));
        assertEquals(-1, Geld.parseCent("Infinity"));
        assertEquals(-1, Geld.parseCent("1e400"));
    }

    @Test
    void hinUndZurueck() {
        assertEquals(0.1, Geld.toDouble(10), 0.0);
        assertEquals(10, Geld.toCent(0.1));
        assertEquals(0, Geld.toCent(Double.NaN));
        assertEquals(0, Geld.toCent(-3));
        assertEquals(19999, Geld.toCent(199.999));
    }
}
