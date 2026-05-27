package com.viaoa.compare;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class OACompareCoercionTest {

	@Nested
	@DisplayName("String and number coercion")
	class StringNumberCoercion {

		@ParameterizedTest(name = "compare({0}, {1}, {2}) sign is {3}")
		@CsvSource({ "5, 5, -1, 0", "005, 5, -1, 0", "5.00, 5, -1, 0", "5.1, 5, -1, 1", "-5.1, -5, -1, -1", "1.004, 1.0044, 2, 0", "1.005, 1.0049, 2, 1" })
		void numericStringsCompareAgainstNumbers(String text, double number, int decimalPlaces, int expectedSign) {
			assertSign(expectedSign, OACompare.compare(text, number, decimalPlaces));
			assertSign(-expectedSign, OACompare.compare(number, text, decimalPlaces));
		}

		@Test
		void integerAndNumericStringUseNumericComparisonInsteadOfLexicalComparison() {
			assertEquals(0, OACompare.compare(10, "10"));
			assertTrue(OACompare.compare(2, "10") < 0);
			assertTrue(OACompare.compare("10", 2) > 0);
		}

		@Test
		void bigDecimalAndNumericStringCompareUsingCurrentCoercionPath() {
			assertEquals(0, OACompare.compare(new BigDecimal("123.45"), "123.45"));
			assertTrue(OACompare.compare(new BigDecimal("123.46"), "123.45") > 0);
		}
	}

	@Nested
	@DisplayName("Null comparison behavior")
	class NullComparisonBehavior {

		@Test
		void nullEqualsNullAndOrdersBeforeStrings() {
			assertEquals(0, OACompare.compare(null, null));
			assertTrue(OACompare.compare(null, "x") < 0);
			assertTrue(OACompare.compare("x", null) > 0);
		}

		@Test
		void currentContractNullCoercesToNumericAndBooleanDefaultsWhenComparedToWrappers() {
			assertEquals(0, OACompare.compare(null, 0));
			assertEquals(0, OACompare.compare(null, 0L));
			assertEquals(0, OACompare.compare(null, BigDecimal.ZERO));
			assertEquals(0, OACompare.compare(null, false));

			assertEquals(0, OACompare.compare(0, null));
			assertEquals(0, OACompare.compare(false, null));
		}

		@Test
		void nullDoesNotEqualNonDefaultWrapperValues() {
			assertTrue(OACompare.compare(null, 1) < 0);
			assertTrue(OACompare.compare(1, null) > 0);
			assertTrue(OACompare.compare(null, true) < 0);
			assertTrue(OACompare.compare(true, null) > 0);
		}
	}

	@Nested
	@DisplayName("Equality helpers")
	class EqualityHelpers {

		@ParameterizedTest
		@CsvSource({ "abc, abc, true", "abc, ABC, false", "5, 5, true", "5.00, 5, true" })
		void isEqualUsesCompareSemantics(String a, String b, boolean expected) {
			assertEquals(expected, OACompare.isEqual(a, b));
		}

		@Test
		void isEqualIgnoreCaseIgnoresStringCase() {
			assertTrue(OACompare.isEqualIgnoreCase("Alpha", "alpha"));
			assertTrue(OACompare.isEqual("Alpha", "alpha", true));
			assertFalse(OACompare.isEqual("Alpha", "alpha", false));
		}

		@Test
		void isEqualWithDecimalPlacesUsesScaleAwareNumericComparison() {
			assertTrue(OACompare.isEqual(new BigDecimal("1.004"), new BigDecimal("1.0044"), 2));
			assertFalse(OACompare.isEqual(new BigDecimal("1.005"), new BigDecimal("1.0049"), 2));
		}
	}

	@Nested
	@DisplayName("Ordering helpers")
	class OrderingHelpers {

		@Test
		void greaterLessAndBetweenHelpersUseCompareSemantics() {
			assertTrue(OACompare.isGreater("10", 2));
			assertTrue(OACompare.isLess("2", 10));
			assertTrue(OACompare.isEqualOrGreater("10", 10));
			assertTrue(OACompare.isEqualOrLess("10", 10));
			assertTrue(OACompare.isBetween(5, 1, 10));
			assertTrue(OACompare.isEqualOrBetween(10, 1, 10));
			assertTrue(OACompare.isBetweenOrEqual(1, 1, 10));
		}

		@Test
		void decimalPlaceAwareOrderingHelpersRoundBeforeTestingOrder() {
			assertFalse(OACompare.isGreater(new BigDecimal("1.004"), new BigDecimal("1.0044"), 2));
			assertTrue(OACompare.isGreater(new BigDecimal("1.005"), new BigDecimal("1.0049"), 2));
		}
	}

	@ParameterizedTest
	@MethodSource("symmetricCoercionPairs")
	void scalarComparisonsAreSymmetricWhereExpected(Object a, Object b, int decimalPlaces) {
		int ab = OACompare.compare(a, b, decimalPlaces);
		int ba = OACompare.compare(b, a, decimalPlaces);
		assertEquals(-Integer.signum(ab), Integer.signum(ba), () -> "Expected antisymmetric comparison signs for " + a + " and " + b);
	}

	static Stream<org.junit.jupiter.params.provider.Arguments> symmetricCoercionPairs() {
		return Stream.of(org.junit.jupiter.params.provider.Arguments.of("5", 5, -1), org.junit.jupiter.params.provider.Arguments.of("1.005", new BigDecimal("1.0049"), 2), org.junit.jupiter.params.provider.Arguments.of(new BigInteger("10"), "10", -1), org.junit.jupiter.params.provider.Arguments.of("true", true, -1), org.junit.jupiter.params.provider.Arguments.of("false", false, -1));
	}

	private static void assertSign(int expectedSign, int actual) {
		assertEquals(expectedSign, Integer.signum(actual), "Unexpected comparison sign");
	}

}
