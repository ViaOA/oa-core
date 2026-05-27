package com.viaoa.compare;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class OACompareNumericTest {

	@Nested
	@DisplayName("OACompare.compare numeric behavior")
	class CompareNumericBehavior {

		@ParameterizedTest(name = "compare({0}, {1}, {2}) sign is {3}")
		@CsvSource({ "1, 1, -1, 0", "1, 2, -1, -1", "2, 1, -1, 1", "1.005, 1.0049, -1, 1", "1.004, 1.0044, 2, 0", "1.005, 1.0049, 2, 1", "-1.005, -1.0049, 2, -1", "-1.004, -1.0044, 2, 0", "0.0, -0.0, -1, 0" })
		void comparesDoubleValuesWithRequestedDecimalPlaces(double a, double b, int decimalPlaces, int expectedSign) {
			assertSign(expectedSign, OACompare.compare(a, b, decimalPlaces));
		}

		@ParameterizedTest(name = "compareNumbers({0}, {1}, {2}) sign is {3}")
		@CsvSource({ "1.004, 1.0044, 2, 0", "1.005, 1.0049, 2, 1", "10.005, 10.0049, 2, 1", "-10.005, -10.0049, 2, -1", "0.004, 0, 2, 0", "0.005, 0, 2, 1", "-0.004, 0, 2, 0", "-0.005, 0, 2, -1" })
		void compareNumbersRoundsScaleAwareWhenDecimalPlacesAreNonNegative(String a, String b, int decimalPlaces, int expectedSign) {
			assertSign(expectedSign, OACompare.compareNumbers(new BigDecimal(a), new BigDecimal(b), decimalPlaces));
		}

		@Test
		void decimalPlacesLessThanZeroUsesNativeExactNumericComparison() {
			assertTrue(OACompare.compare(1.004d, 1.0044d, -1) < 0);
			assertTrue(OACompare.compareNumbers(new BigDecimal("1.004"), new BigDecimal("1.0044"), -1) < 0);
			assertEquals(0, OACompare.compareNumbers(new BigDecimal("1.0"), new BigDecimal("1.00"), -1));
		}

		@Test
		void decimalPlacesZeroUsesWholeNumberScaleAwareComparison() {
			assertEquals(0, OACompare.compare(1.2d, 1.4d, 0));
			assertTrue(OACompare.compare(1.5d, 1.4d, 0) > 0);
			assertEquals(0, OACompare.compareNumbers(new BigDecimal("1.2"), new BigDecimal("1.4"), 0));
			assertTrue(OACompare.compareNumbers(new BigDecimal("1.5"), new BigDecimal("1.4"), 0) > 0);
		}

		@Test
		void plusZeroAndMinusZeroCompareEqualForDoubleAndFloatWrappers() {
			assertEquals(0, OACompare.compare(Double.valueOf(+0.0d), Double.valueOf(-0.0d)));
			assertEquals(0, OACompare.compare(Float.valueOf(+0.0f), Float.valueOf(-0.0f)));
			assertEquals(0, OACompare.compare(+0.0d, -0.0d, -1));
		}

		@Test
		void nanAndInfinityUseJavaFloatingPointOrderingBeforeBigDecimalConversion() {
			assertEquals(0, OACompare.compareNumbers(Double.NaN, Double.NaN, -1));
			assertTrue(OACompare.compareNumbers(Double.NaN, Double.POSITIVE_INFINITY, -1) > 0);
			assertTrue(OACompare.compareNumbers(Double.POSITIVE_INFINITY, 1.0d, -1) > 0);
			assertTrue(OACompare.compareNumbers(Double.NEGATIVE_INFINITY, -1.0d, -1) < 0);

			assertEquals(0, OACompare.compare(Double.NaN, Double.NaN, 2));
			assertTrue(OACompare.compare(Double.NaN, Double.POSITIVE_INFINITY, 2) > 0);
			assertTrue(OACompare.compare(Double.POSITIVE_INFINITY, 1.0d, 2) > 0);
			assertTrue(OACompare.compare(Double.NEGATIVE_INFINITY, -1.0d, 2) < 0);
		}

		@Test
		void bigDecimalAndBigIntegerMixedComparisonsPreserveLargeNumberPrecision() {
			BigInteger huge = new BigInteger("922337203685477580812345678901234567890");
			BigDecimal same = new BigDecimal("922337203685477580812345678901234567890");
			BigDecimal larger = new BigDecimal("922337203685477580812345678901234567890.1");

			assertEquals(0, OACompare.compareNumbers(huge, same, -1));
			assertTrue(OACompare.compareNumbers(larger, huge, -1) > 0);
			assertTrue(OACompare.compareNumbers(huge, larger, -1) < 0);
		}

		@Test
		void compareNumbersHandlesNullDeterministically() {
			assertEquals(0, OACompare.compareNumbers(null, null, -1));
			assertTrue(OACompare.compareNumbers(null, BigDecimal.ZERO, -1) < 0);
			assertTrue(OACompare.compareNumbers(BigDecimal.ZERO, null, -1) > 0);
		}

		@Test
		void explicitRoundingModeIsUsedByDoubleComparison() {
			assertEquals(0, OACompare.compare(1.24d, 1.25d, 1, RoundingMode.DOWN));
			assertTrue(OACompare.compare(1.24d, 1.25d, 1, RoundingMode.HALF_UP) < 0);
		}

		@ParameterizedTest
		@MethodSource("com.viaoa.compare.OACompareNumericTest#symmetricNumericPairs")
		void numericComparisonsAreSymmetricWhereExpected(Number a, Number b, int decimalPlaces) {
			int ab = OACompare.compare(a, b, decimalPlaces);
			int ba = OACompare.compare(b, a, decimalPlaces);
			assertEquals(-Integer.signum(ab), Integer.signum(ba), () -> "Expected antisymmetric signs for " + a + " and " + b);
		}
	}

	@Nested
	@DisplayName("Deterministic repeatability")
	class DeterministicRepeatability {

		@Test
		void repeatedNumericComparisonsReturnSameResult() {
			int expected = OACompare.compare(new BigDecimal("1.005"), new BigDecimal("1.0049"), 2);
			for (int i = 0; i < 100; i++) {
				assertEquals(expected, OACompare.compare(new BigDecimal("1.005"), new BigDecimal("1.0049"), 2));
			}
		}

		@Test
		void repeatedCompareNumbersWithLargeValuesReturnSameResult() {
			BigDecimal a = new BigDecimal("999999999999999999999999999999.9999");
			BigInteger b = new BigInteger("999999999999999999999999999999");

			int expected = OACompare.compareNumbers(a, b, -1);
			for (int i = 0; i < 100; i++) {
				assertEquals(expected, OACompare.compareNumbers(a, b, -1));
			}
		}
	}

	static Stream<Arguments> symmetricNumericPairs() {
		return Stream.of(Arguments.of(1, 2L, -1), Arguments.of(new BigDecimal("1.005"), new BigDecimal("1.0049"), 2), Arguments.of(new BigInteger("12345678901234567890"), new BigDecimal("12345678901234567890.01"), -1), Arguments.of(+0.0d, -0.0d, -1), Arguments.of(Double.POSITIVE_INFINITY, Double.MAX_VALUE, -1));
	}

	private static void assertSign(int expectedSign, int actual) {
		assertEquals(expectedSign, Integer.signum(actual), "Unexpected comparison sign");
	}

}
