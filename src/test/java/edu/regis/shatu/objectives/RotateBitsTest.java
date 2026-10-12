package edu.regis.shatu.objectives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import edu.regis.shatu.model.Account;
import edu.regis.shatu.model.Student;
import edu.regis.shatu.model.aol.RotateStep;

class RotateBitsTest {
    private static final Gson GSON = new Gson();

    @Test
    void rotatesSevenPlacesWithinSixteenBits() {
        assertEquals("0111110111010000",
                RotateBits.performBitRotation("1110 1000 0011 1110", 7, 16));
    }

    @Test
    void rotatesSixteenPlacesWithinThirtyTwoBits() {
        assertEquals("01010110011110000001001000110100",
                RotateBits.performBitRotation("00010010001101000101011001111000", 16, 32));
    }

    @Test
    void wrapsBitsFromTheRightBackToTheLeft() {
        assertEquals("11000001", RotateBits.performBitRotation("10000011", 1, 8));
    }

    @Test
    void zeroAndFullWidthRotationsPreserveTheValue() {
        assertEquals("10000011", RotateBits.performBitRotation("10000011", 0, 8));
        assertEquals("10000011", RotateBits.performBitRotation("10000011", 8, 8));
    }

    @Test
    void largerRotationCountsWrapByTheProblemWidth() {
        assertEquals("11100000", RotateBits.performBitRotation("10000011", 10, 8));
    }

    @Test
    void negativeRotationCountsWrapByTheProblemWidth() {
        assertEquals("00000111", RotateBits.performBitRotation("10000011", -1, 8));
    }

    @Test
    void rejectsEmptyInputAndInvalidWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> RotateBits.performBitRotation("", 7, 0));
        assertThrows(IllegalArgumentException.class,
                () -> RotateBits.performBitRotation(" ", 7, 8));
        assertThrows(IllegalArgumentException.class,
                () -> RotateBits.performBitRotation(null, 7, 8));
    }

    @Test
    void rejectsDataThatDoesNotMatchTheSelectedWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> RotateBits.performBitRotation("1010", 7, 16));
    }

    @Test
    void rejectsNonBinaryData() {
        assertThrows(IllegalArgumentException.class,
                () -> RotateBits.performBitRotation("10000012", 1, 8));
    }

    @Test
    void exampleRejectsUnsupportedProblemSizeBeforeCreatingATask() {
        RotateStep step = new RotateStep();
        step.setLength(0);
        RotateBits objective = new RotateBits(new Student(new Account("student@regis.edu")));

        assertThrows(IllegalArgumentException.class,
                () -> objective.example(null, GSON.toJson(step)));
    }

    @Test
    void exampleRejectsDataThatDoesNotMatchRequestedSize() {
        RotateStep step = new RotateStep();
        step.setLength(16);
        step.setData("1010");
        RotateBits objective = new RotateBits(new Student(new Account("student@regis.edu")));

        assertThrows(IllegalArgumentException.class,
                () -> objective.example(null, GSON.toJson(step)));
    }
}
