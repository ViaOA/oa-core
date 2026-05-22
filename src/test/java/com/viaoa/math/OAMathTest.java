package com.viaoa.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("OAMath")
class OAMathTest {

	private static final double EXACT = 0.0;

	@Nested
	@DisplayName("round")
	class RoundTest {
		@ParameterizedTest(name = "round({0}, {1}) == {2}")
		@CsvSource({ "1.005, 2, 1.01", "1.0049, 2, 1.0", "1.0051, 2, 1.01", "-1.005, 2, -1.01", "-1.0049, 2, -1.0", "-1.0051, 2, -1.01", "0.0, 2, 0.0", "-0.0, 2, 0.0", "123.444, 0, 123.0", "123.5, 0, 124.0", "-123.5, 0, -124.0", "0.0000000004, 9, 0.0", "0.0000000005, 9, 0.000000001", "-0.0000000005, 9, -0.000000001", "999999999999.995, 2, 1000000000000.0" })
		@DisplayName("uses HALF_UP by default")
		void roundUsesHalfUpByDefault(double value, int decimalPlaces, double expected) {
			assertEquals(expected, OAMath.round(value, decimalPlaces), EXACT);
		}

		@ParameterizedTest(name = "round({0}, {1}, {2}) == {3}")
		@CsvSource({ 
			"1.005, 2, 4, 1.01", "1.005, 2, 5, 1.0", "1.005, 2, 6, 1.0", "-1.005, 2, 4, -1.01", "-1.005, 2, 5, -1.0", "-1.005, 2, 6, -1.0", 
			"1.234, 2, 0, 1.24", "1.234, 2, 1, 1.23", "-1.234, 2, 0, -1.24", "-1.234, 2, 1, -1.23" 
			})
		@DisplayName("honors explicit BigDecimal rounding constants")
		void roundHonorsExplicitRoundType(double value, int decimalPlaces, int roundType, double expected) {
			assertEquals(expected, OAMath.round(value, decimalPlaces, roundType), EXACT);
		}

		@ParameterizedTest
		@ValueSource(doubles = { 1.005, -1.005, 123.456789, -987654321.123456 })
		@DisplayName("negative round type defaults to HALF_UP")
		void negativeRoundTypeDefaultsToHalfUp(double value) {
			assertEquals(OAMath.round(value, 2), OAMath.round(value, 2, -1), EXACT);
		}

		@ParameterizedTest
		@ValueSource(doubles = { 1.005, -1.005, 123.456789, -987654321.123456 })
		@DisplayName("negative decimal places leave the value unscaled")
		void negativeDecimalPlacesLeaveValueUnscaled(double value) {
			assertEquals(value, OAMath.round(value, -1), EXACT);
		}

		@Test
		@DisplayName("invalid rounding mode is rejected")
		void invalidRoundingModeIsRejected() {
			assertThrows(IllegalArgumentException.class, () -> OAMath.round(1.23, 2, BigDecimal.ROUND_UNNECESSARY + 1));
		}

		@ParameterizedTest
		@ValueSource(doubles = { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY })
		@DisplayName("non-finite values are rejected by BigDecimal-backed rounding")
		void nonFiniteValuesAreRejected(double value) {
			assertThrows(NumberFormatException.class, () -> OAMath.round(value, 2));
		}
	}

	@Nested
	@DisplayName("add")
	class AddTest {
		@ParameterizedTest(name = "{0} + {1}, scale {2} == {3}")
		@CsvSource({ "1.005, 2.005, 2, 3.01", "1.0049, 2.0049, 2, 3.01", "1.0051, 2.0051, 2, 3.01", "-1.005, -2.005, 2, -3.01", "-1.005, 2.005, 2, 1.0", "0.1, 0.2, 2, 0.3", "999999999999.99, 0.01, 2, 1000000000000.0", "0.0000000004, 0.0000000004, 9, 0.000000001", "0.0000000005, 0.0000000005, 9, 0.000000001" })
		@DisplayName("computes full sum first and rounds only the final result")
		void addComputesFirstAndRoundsFinalResult(double a, double b, int decimalPlaces, double expected) {
			assertEquals(expected, OAMath.add(a, b, decimalPlaces), EXACT);
			assertEquals(expected, OAMath.add(Double.valueOf(a), Double.valueOf(b), decimalPlaces), EXACT);
			assertEquals(expected, OAMath.add(a, Double.valueOf(b), decimalPlaces), EXACT);
			assertEquals(expected, OAMath.add(Double.valueOf(a), b, decimalPlaces), EXACT);
		}

		@ParameterizedTest
		@MethodSource("com.viaoa.math.OAMathTest#addPairs")
		@DisplayName("addition is commutative")
		void additionIsCommutative(double a, double b, int decimalPlaces) {
			assertEquals(OAMath.add(a, b, decimalPlaces), OAMath.add(b, a, decimalPlaces), EXACT);
		}

		@Test
		@DisplayName("null operands are treated as zero")
		void nullOperandsAreTreatedAsZero() {
			assertEquals(0.0, OAMath.add(null, null), EXACT);
			assertEquals(5.25, OAMath.add(null, BigDecimal.valueOf(5.25)), EXACT);
			assertEquals(5.25, OAMath.add(BigDecimal.valueOf(5.25), null), EXACT);
			assertEquals(5.25, OAMath.add(5.25, null), EXACT);
			assertEquals(5.25, OAMath.add(null, 5.25), EXACT);
		}
	}

	@Nested
	@DisplayName("subtract")
	class SubtractTest {
		@ParameterizedTest(name = "{0} - {1}, scale {2} == {3}")
		@CsvSource({ "10.005, 2.005, 2, 8.0", "1.005, 0.0049, 2, 1.0", "1.0049, 0.0049, 2, 1.0", "-1.005, -2.005, 2, 1.0", "-1.005, 2.005, 2, -3.01", "0.3, 0.2, 2, 0.1", "1000000000000.00, 0.01, 2, 999999999999.99", "0.0000000005, 0.0000000001, 9, 0.0" })
		@DisplayName("computes full difference first and rounds only the final result")
		void subtractComputesFirstAndRoundsFinalResult(double a, double b, int decimalPlaces, double expected) {
			assertEquals(expected, OAMath.subtract(a, b, decimalPlaces), EXACT);
			assertEquals(expected, OAMath.subtract(Double.valueOf(a), Double.valueOf(b), decimalPlaces), EXACT);
			assertEquals(expected, OAMath.subtract(a, Double.valueOf(b), decimalPlaces), EXACT);
			assertEquals(expected, OAMath.subtract(Double.valueOf(a), b, decimalPlaces), EXACT);
		}

		@ParameterizedTest
		@ValueSource(doubles = { 0.0, -0.0, 1.005, -1.005, 999999999999.99, 0.000000001 })
		@DisplayName("subtracting a value from itself is zero")
		void subtractingSelfIsZero(double value) {
			assertEquals(0.0, OAMath.subtract(value, value, 9), EXACT);
		}

		@ParameterizedTest
		@MethodSource("com.viaoa.math.OAMathTest#subtractPairs")
		@DisplayName("subtract sign is antisymmetric")
		void subtractSignIsAntisymmetric(double a, double b, int decimalPlaces) {
			int ab = Double.compare(OAMath.subtract(a, b, decimalPlaces), 0.0);
			int ba = Double.compare(OAMath.subtract(b, a, decimalPlaces), 0.0);

			assertEquals(-ab, ba);
		}

		@Test
		@DisplayName("null operands are treated as zero")
		void nullOperandsAreTreatedAsZero() {
			assertEquals(0.0, OAMath.subtract(null, null), EXACT);
			assertEquals(-5.25, OAMath.subtract(null, BigDecimal.valueOf(5.25)), EXACT);
			assertEquals(5.25, OAMath.subtract(BigDecimal.valueOf(5.25), null), EXACT);
			assertEquals(5.25, OAMath.subtract(5.25, null), EXACT);
			assertEquals(-5.25, OAMath.subtract(null, 5.25), EXACT);
		}
	}

	@Nested
	@DisplayName("multiply")
	class MultiplyTest {
		@ParameterizedTest(name = "{0} * {1}, scale {2} == {3}")
		@CsvSource({ "1.005, 2.005, 2, 2.02", "1.0049, 2.0049, 2, 2.01", "1.0051, 2.0051, 2, 2.02", "-1.005, 2.005, 2, -2.02", "-1.005, -2.005, 2, 2.02", "0.1, 0.2, 2, 0.02", "999999999.99, 1000.0, 2, 999999999990.0", "0.0000000005, 2.0, 9, 0.000000001" })
		@DisplayName("computes full product first and rounds only the final result")
		void multiplyComputesFirstAndRoundsFinalResult(double a, double b, int decimalPlaces, double expected) {
			assertEquals(expected, OAMath.multiply(a, b, decimalPlaces), EXACT);
			assertEquals(expected, OAMath.multiply(Double.valueOf(a), Double.valueOf(b), decimalPlaces), EXACT);
			assertEquals(expected, OAMath.multiply(a, Double.valueOf(b), decimalPlaces), EXACT);
			assertEquals(expected, OAMath.multiply(Double.valueOf(a), b, decimalPlaces), EXACT);
		}

		@ParameterizedTest
		@MethodSource("com.viaoa.math.OAMathTest#multiplyPairs")
		@DisplayName("multiplication is commutative")
		void multiplicationIsCommutative(double a, double b, int decimalPlaces) {
			assertEquals(OAMath.multiply(a, b, decimalPlaces), OAMath.multiply(b, a, decimalPlaces), EXACT);
		}

		@ParameterizedTest
		@ValueSource(doubles = { 0.0, -0.0, 1.005, -1.005, 999999999999.99, 0.000000001 })
		@DisplayName("multiplying by one preserves the final rounded value")
		void multiplyingByOnePreservesRoundedValue(double value) {
			assertEquals(OAMath.round(value, 9), OAMath.multiply(value, 1.0, 9), EXACT);
		}

		@Test
		@DisplayName("null operands are treated as zero")
		void nullOperandsAreTreatedAsZero() {
			assertEquals(0.0, OAMath.multiply(null, null), EXACT);
			assertEquals(0.0, OAMath.multiply(null, BigDecimal.valueOf(5.25)), EXACT);
			assertEquals(0.0, OAMath.multiply(BigDecimal.valueOf(5.25), null), EXACT);
			assertEquals(0.0, OAMath.multiply(5.25, null), EXACT);
			assertEquals(0.0, OAMath.multiply(null, 5.25), EXACT);
		}
	}

	@Nested
	@DisplayName("divide")
	class DivideTest {
		@ParameterizedTest(name = "{0} / {1}, scale {2} == {3}")
		@CsvSource({ "10.005, 2.005, 2, 4.99", "1.0, 3.0, 2, 0.33", "2.0, 3.0, 4, 0.6667", "1.0, 8.0, 3, 0.125", "-1.0, 3.0, 2, -0.33", "1.0, -3.0, 2, -0.33", "-1.0, -3.0, 2, 0.33", "0.0, 3.0, 2, 0.0", "-0.0, 3.0, 2, 0.0", "1000000000000.0, 4.0, 2, 250000000000.0" })
		@DisplayName("computes quotient first and rounds only the final result")
		void divideComputesFirstAndRoundsFinalResult(double a, double b, int decimalPlaces, double expected) {
			assertEquals(expected, OAMath.divide(a, b, decimalPlaces), EXACT);
			assertEquals(expected, OAMath.divide(Double.valueOf(a), Double.valueOf(b), decimalPlaces), EXACT);
			assertEquals(expected, OAMath.divide(a, Double.valueOf(b), decimalPlaces), EXACT);
			assertEquals(expected, OAMath.divide(Double.valueOf(a), b, decimalPlaces), EXACT);
		}

		@ParameterizedTest
		@CsvSource({ "1.0, 3.0, 0.3333333333333333", "2.0, 3.0, 0.6666666666666667", "10.0, 4.0, 2.5", "-1.0, 3.0, -0.3333333333333333" })
		@DisplayName("unscaled division uses OAMath's default repeating-decimal protection")
		void unscaledDivisionUsesDefaultRepeatingDecimalProtection(double a, double b, double expected) {
			assertEquals(expected, OAMath.divide(a, b), EXACT);
		}

		@ParameterizedTest
		@ValueSource(doubles = { 0.0, -0.0 })
		@DisplayName("division by zero returns NaN")
		void divisionByZeroReturnsNaN(double zero) {
			assertTrue(Double.isNaN(OAMath.divide(1.0, zero)));
			assertTrue(Double.isNaN(OAMath.divide(BigDecimal.ONE, BigDecimal.valueOf(zero))));
			assertTrue(Double.isNaN(OAMath.divide(1.0, BigDecimal.valueOf(zero), 2)));
			assertTrue(Double.isNaN(OAMath.divide(BigDecimal.ONE, zero, 2)));
		}

		@Test
		@DisplayName("null dividend is zero and null divisor is divide-by-zero")
		void nullHandlingForDivide() {
			assertEquals(0.0, OAMath.divide(null, BigDecimal.valueOf(5.0)), EXACT);
			assertEquals(0.0, OAMath.divide(null, 5.0), EXACT);
			assertTrue(Double.isNaN(OAMath.divide(BigDecimal.valueOf(5.0), null)));
			assertTrue(Double.isNaN(OAMath.divide(5.0, null)));
			assertTrue(Double.isNaN(OAMath.divide(null, null)));
		}
	}

	@Nested
	@DisplayName("performMathOp")
	class PerformMathOpTest {
		@ParameterizedTest
		@MethodSource("com.viaoa.math.OAMathTest#numberOperands")
		@DisplayName("accepts supported Number implementations")
		void acceptsSupportedNumberImplementations(Number a, Number b, double expectedSum, double expectedDifference, double expectedProduct) {
			assertEquals(expectedSum, OAMath.performMathOp(OAMath.MATH_OP_ADD, a, b, 2, BigDecimal.ROUND_HALF_UP), EXACT);
			assertEquals(expectedDifference, OAMath.performMathOp(OAMath.MATH_OP_SUBTRACT, a, b, 2, BigDecimal.ROUND_HALF_UP), EXACT);
			assertEquals(expectedProduct, OAMath.performMathOp(OAMath.MATH_OP_MULTIPLY, a, b, 2, BigDecimal.ROUND_HALF_UP), EXACT);
		}

		@Test
		@DisplayName("BigInteger operands are accepted")
		void bigIntegerOperandsAreAccepted() {
			BigInteger a = new BigInteger("12345678901234567890");
			BigInteger b = new BigInteger("10");

			assertEquals(1.2345678901234567E19, OAMath.add(a, b), EXACT);
			// assertEquals(1.2345678901234567E20, OAMath.multiply(a, b), EXACT);
		}

		@Test
		@DisplayName("invalid operation code is rejected")
		void invalidOperationCodeIsRejected() {
			assertThrows(IllegalArgumentException.class, () -> OAMath.performMathOp(999, BigDecimal.ONE, BigDecimal.ONE, 2, BigDecimal.ROUND_HALF_UP));
		}

		@Test
		@DisplayName("invalid rounding mode is rejected")
		void invalidRoundingModeIsRejected() {
			assertThrows(IllegalArgumentException.class, () -> OAMath.performMathOp(OAMath.MATH_OP_ADD, BigDecimal.ONE, BigDecimal.ONE, 2, BigDecimal.ROUND_UNNECESSARY + 1));
		}

		@Test
		@DisplayName("negative rounding mode defaults to HALF_UP")
		void negativeRoundTypeDefaultsToHalfUp() {
			assertEquals(OAMath.performMathOp(OAMath.MATH_OP_ADD, 1.005, 2.005, 2, BigDecimal.ROUND_HALF_UP), OAMath.performMathOp(OAMath.MATH_OP_ADD, 1.005, 2.005, 2, -1), EXACT);
		}
	}

	@Nested
	@DisplayName("deterministic runtime invariants")
	class DeterministicInvariantTest {
		@ParameterizedTest
		@MethodSource("com.viaoa.math.OAMathTest#allArithmeticPairs")
		@DisplayName("same inputs produce the same results repeatedly")
		void repeatedCalculationsAreDeterministic(double a, double b, int decimalPlaces) {
			double add = OAMath.add(a, b, decimalPlaces);
			double subtract = OAMath.subtract(a, b, decimalPlaces);
			double multiply = OAMath.multiply(a, b, decimalPlaces);
			double divide = OAMath.divide(a, b, decimalPlaces);

			for (int i = 0; i < 100; i++) {
				assertEquals(add, OAMath.add(a, b, decimalPlaces), EXACT);
				assertEquals(subtract, OAMath.subtract(a, b, decimalPlaces), EXACT);
				assertEquals(multiply, OAMath.multiply(a, b, decimalPlaces), EXACT);

				if (Double.isNaN(divide)) {
					assertTrue(Double.isNaN(OAMath.divide(a, b, decimalPlaces)));
				} else {
					assertEquals(divide, OAMath.divide(a, b, decimalPlaces), EXACT);
				}
			}
		}

		@Test
		@DisplayName("chained arithmetic is deterministic with explicit final-result scales")
		void chainedArithmeticIsDeterministic() {
			double first = OAMath.divide(OAMath.multiply(OAMath.add(1.005, 2.005, 2), 3.333, 4), 7.0, 6);
			double second = OAMath.divide(OAMath.multiply(OAMath.add(1.005, 2.005, 2), 3.333, 4), 7.0, 6);

			assertEquals(1.433186, first, EXACT);
			assertEquals(first, second, EXACT);
		}

		@ParameterizedTest
		@ValueSource(doubles = { 0.0, -0.0 })
		@DisplayName("positive and negative zero are semantically zero")
		void positiveAndNegativeZeroAreSemanticallyZero(double zero) {
			assertEquals(0.0, OAMath.add(zero, 0.0, 2), EXACT);
			assertEquals(0.0, OAMath.subtract(zero, 0.0, 2), EXACT);
			assertEquals(0.0, OAMath.multiply(zero, 999.99, 2), EXACT);
			assertEquals(0.0, OAMath.divide(zero, 999.99, 2), EXACT);
			assertEquals(0.0, OAMath.round(zero, 2), EXACT);
		}

		@Test
		@DisplayName("divide-by-zero NaN is current output but not accepted as later BigDecimal-backed input")
		void divideByZeroResultIsNotAcceptedAsLaterInput() {
			double value = OAMath.divide(1.0, 0.0);

			assertTrue(Double.isNaN(value));
			assertThrows(NumberFormatException.class, () -> OAMath.add(value, 1.0));
			assertThrows(NumberFormatException.class, () -> OAMath.round(value, 2));
		}
	}

	static Stream<Arguments> numberOperands() {
		return Stream.of(Arguments.of(Byte.valueOf((byte) 2), Short.valueOf((short) 3), 5.0, -1.0, 6.0), Arguments.of(Integer.valueOf(10), Long.valueOf(4L), 14.0, 6.0, 40.0), Arguments.of(Float.valueOf("1.25"), Double.valueOf("2.50"), 3.75, -1.25, 3.13), Arguments.of(BigDecimal.valueOf(1.005), BigDecimal.valueOf(2.005), 3.01, -1.0, 2.02), Arguments.of(new BigInteger("100"), new BigInteger("3"), 103.0, 97.0, 300.0));
	}

	static Stream<Arguments> addPairs() {
		return Stream.of(Arguments.of(1.005, 2.005, 2), Arguments.of(1.0049, 2.0049, 2), Arguments.of(-1.005, 2.005, 2), Arguments.of(999999.99, -0.01, 2), Arguments.of(0.000001, 0.000002, 9));
	}

	static Stream<Arguments> subtractPairs() {
		return Stream.of(Arguments.of(1.005, 2.005, 2), Arguments.of(1.0049, 2.0049, 2), Arguments.of(-1.005, 2.005, 2), Arguments.of(999999.99, -0.01, 2), Arguments.of(0.000001, 0.000002, 9));
	}

	static Stream<Arguments> multiplyPairs() {
		return Stream.of(Arguments.of(1.005, 2.005, 2), Arguments.of(1.0049, 2.0049, 2), Arguments.of(-1.005, 2.005, 2), Arguments.of(999999.99, -0.01, 2), Arguments.of(0.000001, 0.000002, 12));
	}

	static Stream<Arguments> allArithmeticPairs() {
		return Stream.of(Arguments.of(1.005, 2.005, 2), Arguments.of(1.0049, 2.0049, 2), Arguments.of(-1.005, 2.005, 2), Arguments.of(0.0, 1.0, 2), Arguments.of(-0.0, -1.0, 2), Arguments.of(999999999.99, 0.01, 2), Arguments.of(0.000000001, 0.000000002, 9), Arguments.of(1.0, 3.0, 8));
	}

}

/* CODEX


 Highest-value additions:

  1. BigDecimal scale preservation inputs
     Test values like new BigDecimal("1.2300"), new BigDecimal("0.000000000000000001"), and large exact decimal
     strings. OAMath.toBigDecimal preserves BigDecimal directly, so this catches precision assumptions better
     than double.
  2. All rounding mode constants
     Add a parameterized table for ROUND_UP, ROUND_DOWN, ROUND_CEILING, ROUND_FLOOR, ROUND_HALF_UP,
     ROUND_HALF_DOWN, ROUND_HALF_EVEN, and ROUND_UNNECESSARY. Include positive and negative values. Current
     coverage only samples a few.
  3. ROUND_UNNECESSARY behavior
     This should succeed when no rounding is required and throw ArithmeticException when rounding is required:

     OAMath.round(1.23, 2, BigDecimal.ROUND_UNNECESSARY) == 1.23
     OAMath.round(1.234, 2, BigDecimal.ROUND_UNNECESSARY) throws ArithmeticException

  4. Negative decimalPlaces for arithmetic
     You covered negative scale for round, but arithmetic should also verify decimalPlaces < 0 means “no final
     scale,” except divide still uses internal 16-place scale.
  5. Direct performMathOp coverage for all op constants
     The public helpers cover this indirectly, but since performMathOp is public, it should have explicit tests
     for add/subtract/multiply/divide, invalid op, invalid rounding mode, negative rounding mode.
  6. Non-finite arithmetic inputs
     Since BigDecimal.valueOf(Double.NaN) and infinities throw, test that add, subtract, multiply, and non-zero
     divide reject NaN/infinity consistently. Also preserve the current divide-by-zero NaN contract.
  7. Overflow-to-infinity result boundary
     BigDecimal can compute a result too large for doubleValue(), which returns infinity. If that’s acceptable
     current behavior, lock it:

     OAMath.multiply(new BigDecimal("1E400"), BigDecimal.TEN) == Double.POSITIVE_INFINITY

  8. Divide precision contract
     Add explicit tests that unscaled repeating division uses 16 places:

     OAMath.divide(1, 6) == 0.1666666666666667
     OAMath.divide(1, 7) == 0.1428571428571429

  9. Final-result-only rounding regression tests
     Add comments/DisplayNames that lock this exact semantic:

     add(1.005, 2.005, 2) == 3.01
     multiply(1.005, 2.005, 2) == 2.02
     divide(10.005, 2.005, 2) == 4.99

     These are important because this was the semantic correction.

  10. Null behavior with explicit scale and rounding mode
     Current null tests mostly hit default overloads. Add:

  OAMath.add(null, 1.005, 2, BigDecimal.ROUND_HALF_UP) == 1.01
  OAMath.subtract(null, 1.005, 2, BigDecimal.ROUND_HALF_UP) == -1.01

  One small correction to the generated test class: update the explicit rounding CSV for ROUND_UP/ROUND_DOWN as
  noted. After that, I’d add the above as another nested section or expand the existing ones.


*/

