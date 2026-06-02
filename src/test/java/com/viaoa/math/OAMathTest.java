package com.viaoa.math;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

/**
 * Internal tests for OAMath.
 *
 * Strategy:
 * - One test method per public production method name.
 * - Overloads are tested inside the same methodNameTest().
 * - Comments explain what each assertion is checking.
 * - Tests characterize the current OAMath contract.
 */
public class OAMathTest {

    @Test
    public void roundTest() {
        // normal case: ROUND_HALF_UP is the default two-argument behavior
        assertEquals(1.24, OAMath.round(1.235, 2), 0.000000001);

        // explicit ROUND_DOWN truncates instead of rounding up
        assertEquals(1.23, OAMath.round(1.239, 2, BigDecimal.ROUND_DOWN), 0.000000001);

        // negative round type uses OA default ROUND_HALF_UP behavior
        assertEquals(1.24, OAMath.round(1.235, 2, -1), 0.000000001);

        // negative decimal places skip scale enforcement and return the input value
        assertEquals(1.235, OAMath.round(1.235, -1), 0.000000001);

        // zero decimal places rounds to whole number
        assertEquals(2.0, OAMath.round(1.5, 0), 0.000000001);

        // invalid positive round type fails visibly
        assertThrows(IllegalArgumentException.class, () -> OAMath.round(1.23, 2, BigDecimal.ROUND_UNNECESSARY + 1));

        // current non-finite behavior: BigDecimal.valueOf cannot accept NaN
        assertThrows(NumberFormatException.class, () -> OAMath.round(Double.NaN, 2));
    }

    @Test
    public void addTest() {
        // normal Number overload without scale
        assertEquals(3.3, OAMath.add(Double.valueOf(1.1), Double.valueOf(2.2)), 0.000000001);

        // primitive double overload without scale
        assertEquals(3.3, OAMath.add(1.1, 2.2), 0.000000001);

        // primitive + Number overload without scale
        assertEquals(3.3, OAMath.add(1.1, Double.valueOf(2.2)), 0.000000001);

        // Number + primitive overload without scale
        assertEquals(3.3, OAMath.add(Double.valueOf(1.1), 2.2), 0.000000001);

        // null operands are treated as zero
        assertEquals(5.0, OAMath.add(null, Integer.valueOf(5)), 0.000000001);
        assertEquals(5.0, OAMath.add(Integer.valueOf(5), null), 0.000000001);
        assertEquals(0.0, OAMath.add((Number) null, (Number) null), 0.000000001);

        // decimalPlaces applies final scale
        assertEquals(3.33, OAMath.add(1.111, 2.222, 2), 0.000000001);

        // explicit rounding mode is honored
        assertEquals(3.33, OAMath.add(1.115, 2.222, 2, BigDecimal.ROUND_DOWN), 0.000000001);

        // negative round type uses default ROUND_HALF_UP
        assertEquals(3.34, OAMath.add(1.115, 2.222, 2, -1), 0.000000001);

        // BigDecimal operands are used as decimal values
        assertEquals(0.3, OAMath.add(new BigDecimal("0.1"), new BigDecimal("0.2")), 0.000000001);

        // large integral values preserve unit difference before final double conversion for small result
        assertEquals(1.0, OAMath.add(BigInteger.ZERO, BigInteger.ONE), 0.000000001);

        // invalid positive round type fails visibly through performMathOp
        assertThrows(IllegalArgumentException.class,
                () -> OAMath.add(1.0, 2.0, 2, BigDecimal.ROUND_UNNECESSARY + 1));

        // current non-finite behavior: BigDecimal.valueOf cannot accept NaN
        assertThrows(NumberFormatException.class, () -> OAMath.add(Double.NaN, 1.0));
    }

    @Test
    public void subtractTest() {
        // normal Number overload without scale
        assertEquals(3.0, OAMath.subtract(Integer.valueOf(5), Integer.valueOf(2)), 0.000000001);

        // primitive double overload without scale
        assertEquals(3.0, OAMath.subtract(5.0, 2.0), 0.000000001);

        // primitive + Number overload without scale
        assertEquals(3.0, OAMath.subtract(5.0, Integer.valueOf(2)), 0.000000001);

        // Number + primitive overload without scale
        assertEquals(3.0, OAMath.subtract(Integer.valueOf(5), 2.0), 0.000000001);

        // null first operand is treated as zero
        assertEquals(-5.0, OAMath.subtract(null, Integer.valueOf(5)), 0.000000001);

        // null second operand is treated as zero
        assertEquals(5.0, OAMath.subtract(Integer.valueOf(5), null), 0.000000001);

        // decimalPlaces applies final scale
        assertEquals(1.11, OAMath.subtract(3.333, 2.222, 2), 0.000000001);

        // explicit rounding mode is honored
        assertEquals(1.11, OAMath.subtract(3.339, 2.222, 2, BigDecimal.ROUND_DOWN), 0.000000001);

        // negative round type uses default ROUND_HALF_UP
        assertEquals(1.12, OAMath.subtract(3.339, 2.222, 2, -1), 0.000000001);

        // BigInteger operands preserve exact integral difference
        assertEquals(1.0, OAMath.subtract(BigInteger.TEN, BigInteger.valueOf(9)), 0.000000001);

        // invalid positive round type fails visibly through performMathOp
        assertThrows(IllegalArgumentException.class,
                () -> OAMath.subtract(5.0, 2.0, 2, BigDecimal.ROUND_UNNECESSARY + 1));

        // current non-finite behavior: BigDecimal.valueOf cannot accept NaN
        assertThrows(NumberFormatException.class, () -> OAMath.subtract(Double.NaN, 1.0));
    }

    @Test
    public void multiplyTest() {
        // normal Number overload without scale
        assertEquals(6.0, OAMath.multiply(Integer.valueOf(2), Integer.valueOf(3)), 0.000000001);

        // primitive double overload without scale
        assertEquals(6.0, OAMath.multiply(2.0, 3.0), 0.000000001);

        // primitive + Number overload without scale
        assertEquals(6.0, OAMath.multiply(2.0, Integer.valueOf(3)), 0.000000001);

        // Number + primitive overload without scale
        assertEquals(6.0, OAMath.multiply(Integer.valueOf(2), 3.0), 0.000000001);

        // null first operand is treated as zero
        assertEquals(0.0, OAMath.multiply(null, Integer.valueOf(5)), 0.000000001);

        // null second operand is treated as zero
        assertEquals(0.0, OAMath.multiply(Integer.valueOf(5), null), 0.000000001);

        // decimalPlaces applies final scale
        assertEquals(3.33, OAMath.multiply(1.111, 3.0, 2), 0.000000001);

        // explicit rounding mode is honored
        double d = OAMath.multiply(1.119, 3.0, 2, BigDecimal.ROUND_DOWN);
        assertEquals(3.35, d, 0.000000001);

        // negative round type uses default ROUND_HALF_UP
        assertEquals(3.36, OAMath.multiply(1.119, 3.0, 2, -1), 0.000000001);

        // BigDecimal operands avoid primitive floating-point artifacts
        assertEquals(0.02, OAMath.multiply(new BigDecimal("0.10"), new BigDecimal("0.20")), 0.000000001);

        // invalid positive round type fails visibly through performMathOp
        assertThrows(IllegalArgumentException.class,
                () -> OAMath.multiply(2.0, 3.0, 2, BigDecimal.ROUND_UNNECESSARY + 1));

        // current non-finite behavior: BigDecimal.valueOf cannot accept NaN
        assertThrows(NumberFormatException.class, () -> OAMath.multiply(Double.NaN, 1.0));
    }

    @Test
    public void divideTest() {
        // normal Number overload without scale uses bounded internal division scale
        assertEquals(2.5, OAMath.divide(Integer.valueOf(5), Integer.valueOf(2)), 0.000000001);

        // primitive double overload without scale
        assertEquals(2.5, OAMath.divide(5.0, 2.0), 0.000000001);

        // primitive + Number overload without scale
        assertEquals(2.5, OAMath.divide(5.0, Integer.valueOf(2)), 0.000000001);

        // Number + primitive overload without scale
        assertEquals(2.5, OAMath.divide(Integer.valueOf(5), 2.0), 0.000000001);

        // decimalPlaces applies requested final scale
        assertEquals(0.33, OAMath.divide(1.0, 3.0, 2), 0.000000001);

        // explicit rounding mode is honored
        assertEquals(0.33, OAMath.divide(1.0, 3.0, 2, BigDecimal.ROUND_DOWN), 0.000000001);

        // negative round type uses default ROUND_HALF_UP
        assertEquals(0.33, OAMath.divide(1.0, 3.0, 2, -1), 0.000000001);

        // null numerator is treated as zero
        assertEquals(0.0, OAMath.divide(null, Integer.valueOf(5)), 0.000000001);

        // zero divisor returns NaN by contract
        assertTrue(Double.isNaN(OAMath.divide(1.0, 0.0)));

        // null divisor is treated as zero and returns NaN
        assertTrue(Double.isNaN(OAMath.divide(Integer.valueOf(1), null)));

        // repeating decimal without requested decimal places does not throw
        assertDoesNotThrow(() -> OAMath.divide(1.0, 3.0));

        // invalid positive round type fails visibly through performMathOp
        assertThrows(IllegalArgumentException.class,
                () -> OAMath.divide(1.0, 3.0, 2, BigDecimal.ROUND_UNNECESSARY + 1));

        // current non-finite behavior: BigDecimal.valueOf cannot accept NaN
        assertThrows(NumberFormatException.class, () -> OAMath.divide(Double.NaN, 1.0));
    }

    @Test
    public void performMathOpTest() {
        // operation dispatch: add
        assertEquals(5.0, OAMath.performMathOp(OAMath.MATH_OP_ADD, 2, 3, -1, -1), 0.000000001);

        // operation dispatch: subtract
        assertEquals(-1.0, OAMath.performMathOp(OAMath.MATH_OP_SUBTRACT, 2, 3, -1, -1), 0.000000001);

        // operation dispatch: multiply
        assertEquals(6.0, OAMath.performMathOp(OAMath.MATH_OP_MULTIPLY, 2, 3, -1, -1), 0.000000001);

        // operation dispatch: divide
        assertEquals(2.0, OAMath.performMathOp(OAMath.MATH_OP_DIVIDE, 6, 3, -1, -1), 0.000000001);

        // unknown operation code fails visibly
        assertThrows(IllegalArgumentException.class, () -> OAMath.performMathOp(999, 2, 3, -1, -1));

        // invalid positive round type fails visibly
        assertThrows(IllegalArgumentException.class,
                () -> OAMath.performMathOp(OAMath.MATH_OP_ADD, 2, 3, 2, BigDecimal.ROUND_UNNECESSARY + 1));

        // negative rounding mode uses default ROUND_HALF_UP
        assertEquals(3.34, OAMath.performMathOp(OAMath.MATH_OP_ADD, 1.115, 2.222, 2, -1), 0.000000001);

        // decimalPlaces applies final scale
        assertEquals(3.33, OAMath.performMathOp(OAMath.MATH_OP_ADD, 1.111, 2.222, 2, BigDecimal.ROUND_HALF_UP), 0.000000001);

        // null operands are treated as zero
        assertEquals(0.0, OAMath.performMathOp(OAMath.MATH_OP_ADD, null, null, -1, -1), 0.000000001);

        // divide by zero returns NaN
        assertTrue(Double.isNaN(OAMath.performMathOp(OAMath.MATH_OP_DIVIDE, 1, 0, -1, -1)));

        // BigDecimal operands are used directly
        assertEquals(0.3, OAMath.performMathOp(OAMath.MATH_OP_ADD,
                new BigDecimal("0.1"), new BigDecimal("0.2"), -1, -1), 0.000000001);

        // BigInteger operands preserve exact integral semantics before final double return
        assertEquals(1.0, OAMath.performMathOp(OAMath.MATH_OP_SUBTRACT,
                BigInteger.TEN, BigInteger.valueOf(9), -1, -1), 0.000000001);

        // repeated calls do not retain prior state
        assertEquals(2.0, OAMath.performMathOp(OAMath.MATH_OP_ADD, 1, 1, -1, -1), 0.000000001);
        assertEquals(10.0, OAMath.performMathOp(OAMath.MATH_OP_MULTIPLY, 2, 5, -1, -1), 0.000000001);
    }
}
