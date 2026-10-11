package edu.regis.shatu.objectives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.regis.shatu.model.Account;
import edu.regis.shatu.model.Student;

class ShiftBitsTest {
    private ShiftBits objective;

    @BeforeEach
    void setUp() {
        objective = new ShiftBits(new Student(new Account("student@regis.edu")));
    }

    @Test
    void rightShiftPadsWithZerosAtFourBits() {
        assertEquals("0101", objective.bitShiftFunction("1011", 1, true, 4));
    }

    @Test
    void rightShiftDoesNotTreatTheHighBitAsASignBit() {
        assertEquals("01000000000000000000000000000000",
                objective.bitShiftFunction("10000000000000000000000000000000", 1, true, 32));
    }

    @Test
    void leftShiftDropsOverflowBitsAtFourBits() {
        assertEquals("0110", objective.bitShiftFunction("1011", 1, false, 4));
    }

    @Test
    void leftShiftDropsOverflowBitsAtEightBits() {
        assertEquals("01100110", objective.bitShiftFunction("10110011", 1, false, 8));
    }

    @Test
    void leftShiftDropsOverflowBitsAtSixteenBits() {
        assertEquals("1111111111111110",
                objective.bitShiftFunction("1111111111111111", 1, false, 16));
    }

    @Test
    void leftShiftDropsOverflowBitsAtThirtyTwoBits() {
        assertEquals("11111111111111111111111111111110",
                objective.bitShiftFunction("11111111111111111111111111111111", 1, false, 32));
    }

    @Test
    void zeroShiftPreservesOperandAndWidth() {
        assertEquals("00001111", objective.bitShiftFunction("0000 1111", 0, false, 8));
    }

    @Test
    void shiftByWidthOrMoreClearsAllBits() {
        assertEquals("00000000", objective.bitShiftFunction("11111111", 8, true, 8));
        assertEquals("00000000", objective.bitShiftFunction("11111111", 64, false, 8));
    }

    @Test
    void rejectsUnsupportedWidths() {
        assertThrows(IllegalArgumentException.class,
                () -> objective.bitShiftFunction("101", 1, true, 3));
        assertThrows(IllegalArgumentException.class,
                () -> objective.bitShiftFunction("", 1, true, 0));
    }

    @Test
    void rejectsNegativeShiftCount() {
        assertThrows(IllegalArgumentException.class,
                () -> objective.bitShiftFunction("1011", -1, true, 4));
    }

    @Test
    void rejectsMalformedOperand() {
        assertThrows(IllegalArgumentException.class,
                () -> objective.bitShiftFunction("1012", 1, true, 4));
        assertThrows(IllegalArgumentException.class,
                () -> objective.bitShiftFunction("101", 1, true, 4));
        assertThrows(IllegalArgumentException.class,
                () -> objective.bitShiftFunction(null, 1, true, 4));
    }
}
