/*
 * SHATU: SHA-256 Tutor
 *
 *  (C) Johanna & Richard Blumenthal, All rights reserved
 *
 *  Unauthorized use, duplication or distribution without the authors'
 *  permission is strictly prohibited.
 *
 *  Unless required by applicable law or agreed to in writing, this
 *  software is distributed on an "AS IS" basis without warranties
 *  or conditions of any kind, either expressed or implied.
 */
package edu.regis.shatu.objectives;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.regis.shatu.model.Account;
import edu.regis.shatu.model.Student;
import edu.regis.shatu.model.aol.BitOpExample;

/**
 * Unit tests for how {@link AddBits} grades an answer.
 *
 * The expected answer must be the sum of the two operands modulo 2^n, where n
 * is the width of the problem, written as exactly n binary digits.
 *
 * The method under test is private, so reflection is used to invoke it
 * directly rather than going through {@code completeStep(...)}, which has
 * database side-effects via {@code ServiceFactory.findStudentModelSvc()}.
 *
 * @see AddBits
 */
@DisplayName("AddBits grading tests")
public class AddBitsTest {

    private static AddBits addBits;
    private static Method expectedResult;

    @BeforeAll
    static void setUpReflection() throws Exception {
        addBits = new AddBits(new Student(new Account("test@regis.edu")));

        expectedResult = AddBits.class.getDeclaredMethod("expectedResult", BitOpExample.class);
        expectedResult.setAccessible(true);
    }

    /** Build an example the same way AddBits.example() does. */
    private static BitOpExample example(int width, long operand1, long operand2) {
        BitOpExample ex = new BitOpExample();
        ex.setPreSize(width);
        ex.setOperand1Val(operand1);
        ex.setOperand2Val(operand2);
        ex.setPostSize(width);
        return ex;
    }

    private static String expected(BitOpExample ex) throws Exception {
        return (String) expectedResult.invoke(addBits, ex);
    }

    /** (a + b) mod 2^width as exactly width binary digits, worked out with BigInteger. */
    private static String reference(int width, long operand1, long operand2) {
        BigInteger sum = BigInteger.valueOf(operand1).add(BigInteger.valueOf(operand2));
        String bits = sum.mod(BigInteger.TWO.pow(width)).toString(2);
        return "0".repeat(width - bits.length()) + bits;
    }

    @Test
    @DisplayName("4-bit example from issue #252: 1110 + 0011 = 0001")
    void fourBitExampleFromIssue() throws Exception {
        assertEquals("0001", expected(example(4, 0b1110, 0b0011)));
    }

    @Test
    @DisplayName("8-bit overflow wraps to zero")
    void eightBitOverflow() throws Exception {
        assertEquals("00000000", expected(example(8, 0xFF, 0x01)));
    }

    @Test
    @DisplayName("16-bit sum without overflow")
    void sixteenBitNoOverflow() throws Exception {
        assertEquals(reference(16, 0x1234, 0x0F0F), expected(example(16, 0x1234, 0x0F0F)));
    }

    @Test
    @DisplayName("32-bit overflow wraps to zero")
    void thirtyTwoBitOverflow() throws Exception {
        assertEquals("0".repeat(32), expected(example(32, 0xFFFFFFFFL, 1)));
    }

    @Test
    @DisplayName("every width from 1 to 32 matches BigInteger")
    void everyWidthMatchesBigInteger() throws Exception {
        Random rnd = new Random(252);

        for (int width = 1; width <= 32; width++) {
            long max = 1L << width;

            for (int i = 0; i < 50; i++) {
                long a = Math.floorMod(rnd.nextLong(), max);
                long b = Math.floorMod(rnd.nextLong(), max);

                assertEquals(reference(width, a, b), expected(example(width, a, b)),
                        width + "-bit: " + a + " + " + b);
            }
        }
    }
}
