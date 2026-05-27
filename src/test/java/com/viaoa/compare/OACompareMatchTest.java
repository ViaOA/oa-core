package com.viaoa.compare;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.viaoa.compare.match.OAMatch;
import com.viaoa.compare.match.OAMatchAny;
import com.viaoa.compare.match.OAMatchEmpty;
import com.viaoa.compare.match.OAMatchGreaterThanZero;
import com.viaoa.compare.match.OAMatchLessThanZero;
import com.viaoa.compare.match.OAMatchNotEmpty;
import com.viaoa.compare.match.OAMatchNotExist;
import com.viaoa.compare.match.OAMatchNotNull;
import com.viaoa.compare.match.OAMatchNull;
import com.viaoa.compare.match.OAMatchUnknown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OACompareMatchTest {

	@Nested
	@DisplayName("OAMatch direct predicate behavior")
	class DirectPredicateBehavior {

		@Test
		void anyMatchesEveryKnownValueButNotUnknown() {
			assertTrue(OAMatchAny.instance.matches(null, -1));
			assertTrue(OAMatchAny.instance.matches("x", -1));
			assertTrue(OAMatchAny.instance.matches(0, -1));
			assertFalse(OAMatchAny.instance.matches(OAMatchUnknown.instance, -1));
		}

		@Test
		void nullMatchesNullAndNullTokenOnly() {
			assertTrue(OAMatchNull.instance.matches(null, -1));
			assertTrue(OAMatchNull.instance.matches(OAMatchNull.instance, -1));
			assertFalse(OAMatchNull.instance.matches("", -1));
			assertFalse(OAMatchNull.instance.matches(OAMatchUnknown.instance, -1));
		}

		@Test
		void notNullMatchesNonNullKnownValuesOnly() {
			assertFalse(OAMatchNotNull.instance.matches(null, -1));
			assertTrue(OAMatchNotNull.instance.matches("", -1));
			assertTrue(OAMatchNotNull.instance.matches(0, -1));
			assertFalse(OAMatchNotNull.instance.matches(OAMatchUnknown.instance, -1));
		}

		@ParameterizedTest
		@MethodSource("com.viaoa.compare.OACompareMatchTest#emptyValues")
		void emptyMatchesOAEmptyValues(Object value) {
			assertTrue(OAMatchEmpty.instance.matches(value, -1));
			assertFalse(OAMatchNotEmpty.instance.matches(value, -1));
		}

		@ParameterizedTest
		@MethodSource("com.viaoa.compare.OACompareMatchTest#notEmptyValues")
		void notEmptyMatchesOANonEmptyValues(Object value) {
			assertFalse(OAMatchEmpty.instance.matches(value, -1));
			assertTrue(OAMatchNotEmpty.instance.matches(value, -1));
		}

		@Test
		void unknownIsProtectedFromBroadTokens() {
			assertFalse(OAMatchAny.instance.matches(OAMatchUnknown.instance, -1));
			assertFalse(OAMatchNotNull.instance.matches(OAMatchUnknown.instance, -1));
			assertFalse(OAMatchNotEmpty.instance.matches(OAMatchUnknown.instance, -1));
			assertFalse(OAMatchEmpty.instance.matches(OAMatchUnknown.instance, -1));
			assertFalse(OAMatchNotExist.instance.matches(OAMatchUnknown.instance, -1));
			assertFalse(OAMatchGreaterThanZero.instance.matches(OAMatchUnknown.instance, -1));
			assertFalse(OAMatchLessThanZero.instance.matches(OAMatchUnknown.instance, -1));
		}

		@Test
		void unknownMatchesOnlyUnknownToken() {
			assertTrue(OAMatchUnknown.instance.matches(OAMatchUnknown.instance, -1));
			assertFalse(OAMatchUnknown.instance.matches(null, -1));
			assertFalse(OAMatchUnknown.instance.matches("x", -1));
			assertFalse(OAMatchUnknown.instance.matches(OAMatchAny.instance, -1));
		}

		@Test
		void notExistMatchesNullAndNotExistButNotUnknown() {
			assertTrue(OAMatchNotExist.instance.matches(null, -1));
			assertTrue(OAMatchNotExist.instance.matches(OAMatchNotExist.instance, -1));
			assertFalse(OAMatchNotExist.instance.matches("x", -1));
			assertFalse(OAMatchNotExist.instance.matches(OAMatchUnknown.instance, -1));
		}
	}

	@Nested
	@DisplayName("Numeric match predicates")
	class NumericMatchPredicates {

		@Test
		void greaterThanZeroUsesOACompareNumericSemantics() {
			assertTrue(OAMatchGreaterThanZero.instance.matches(1, -1));
			assertTrue(OAMatchGreaterThanZero.instance.matches("1", -1));
			assertTrue(OAMatchGreaterThanZero.instance.matches(new BigDecimal("1e-400"), -1));

			assertFalse(OAMatchGreaterThanZero.instance.matches(0, -1));
			assertFalse(OAMatchGreaterThanZero.instance.matches(-1, -1));
			assertFalse(OAMatchGreaterThanZero.instance.matches("not-number", -1));
			assertFalse(OAMatchGreaterThanZero.instance.matches(null, -1));
		}

		@Test
		void lessThanZeroUsesOACompareNumericSemantics() {
			assertTrue(OAMatchLessThanZero.instance.matches(-1, -1));
			assertTrue(OAMatchLessThanZero.instance.matches("-1", -1));
			assertTrue(OAMatchLessThanZero.instance.matches(new BigDecimal("-1e-400"), -1));

			assertFalse(OAMatchLessThanZero.instance.matches(0, -1));
			assertFalse(OAMatchLessThanZero.instance.matches(1, -1));
			assertFalse(OAMatchLessThanZero.instance.matches("not-number", -1));
			assertFalse(OAMatchLessThanZero.instance.matches(null, -1));
		}

		@Test
		void greaterAndLessThanZeroRespectDecimalPlaces() {
			assertFalse(OAMatchGreaterThanZero.instance.matches("0.004", 2));
			assertTrue(OAMatchGreaterThanZero.instance.matches("0.005", 2));

			assertFalse(OAMatchLessThanZero.instance.matches("-0.004", 2));
			assertTrue(OAMatchLessThanZero.instance.matches("-0.005", 2));
		}
	}

	@Nested
	@DisplayName("OACompare integration with OAMatch")
	class CompareIntegration {

		@ParameterizedTest
		@MethodSource("com.viaoa.compare.OACompareMatchTest#matchingPairs")
		void compareReturnsZeroWhenMatchTokenMatchesValue(Object value, OAMatch token, int decimalPlaces) {
			assertEquals(0, OACompare.compare(value, token, decimalPlaces));
			assertEquals(0, OACompare.compare(token, value, decimalPlaces));
		}

		@ParameterizedTest
		@MethodSource("com.viaoa.compare.OACompareMatchTest#nonMatchingPairs")
		void compareReturnsDirectionalNonZeroWhenMatchTokenDoesNotMatchValue(Object value, OAMatch token, int decimalPlaces) {
			int x = OACompare.compare(value, token, decimalPlaces);
			assertTrue(x != 0);
			int x2 = OACompare.compare(token, value, decimalPlaces);
			assertTrue(x == -x2);
		}

		@Test
		void differentMatchTokensDoNotPredicateMatchEachOtherThroughCompare() {
			assertEquals(0, OACompare.compare(OAMatchAny.instance, OAMatchAny.instance));

			assertNotEquals(0, OACompare.compare(OAMatchAny.instance, OAMatchUnknown.instance));
			assertNotEquals(0, OACompare.compare(OAMatchUnknown.instance, OAMatchAny.instance));
			assertEquals(-Integer.signum(OACompare.compare(OAMatchAny.instance, OAMatchUnknown.instance)), Integer.signum(OACompare.compare(OAMatchUnknown.instance, OAMatchAny.instance)));

			assertNotEquals(0, OACompare.compare(OAMatchNotNull.instance, OAMatchUnknown.instance));
			assertNotEquals(0, OACompare.compare(OAMatchNotEmpty.instance, OAMatchUnknown.instance));
		}

		@Test
		void repeatedMatchComparisonsAreDeterministic() {
			int expected = OACompare.compare("0.005", OAMatchGreaterThanZero.instance, 2);
			for (int i = 0; i < 100; i++) {
				assertEquals(expected, OACompare.compare("0.005", OAMatchGreaterThanZero.instance, 2));
			}
		}
	}

	static Stream<Arguments> emptyValues() {
	    return Stream.of(
	        Arguments.of((Object) null),
	        Arguments.of(""),
	        Arguments.of(0),
	        Arguments.of(0L),
	        Arguments.of(0.0d),
	        Arguments.of(-0.0d),
	        Arguments.of(BigDecimal.ZERO),
	        Arguments.of(false),
	        Arguments.of((Object) new Object[0]),
	        Arguments.of(Collections.emptyList()),
	        Arguments.of(Collections.emptyMap()),
	        Arguments.of(OAMatchEmpty.instance)
	    );
	}
	static Stream<Object> notEmptyValues() {
		return Stream.of(" ", "x", 1, -1, 0.1d, new BigDecimal("1e-400"), true, new Object[] { "x" }, List.of("x"), Map.of("x", 1), OAMatchNotEmpty.instance);
	}

	static Stream<Arguments> matchingPairs() {
		return Stream.of(Arguments.of(null, OAMatchAny.instance, -1), Arguments.of("x", OAMatchAny.instance, -1), Arguments.of(null, OAMatchNull.instance, -1), Arguments.of("x", OAMatchNotNull.instance, -1), Arguments.of("", OAMatchEmpty.instance, -1), Arguments.of(0, OAMatchEmpty.instance, -1), Arguments.of("x", OAMatchNotEmpty.instance, -1), Arguments.of(1, OAMatchNotEmpty.instance, -1), Arguments.of(null, OAMatchNotExist.instance, -1), Arguments.of(1, OAMatchGreaterThanZero.instance, -1),
				Arguments.of("0.005", OAMatchGreaterThanZero.instance, 2), Arguments.of(-1, OAMatchLessThanZero.instance, -1), Arguments.of("-0.005", OAMatchLessThanZero.instance, 2));
	}

	static Stream<Arguments> nonMatchingPairs() {
		return Stream.of(Arguments.of(OAMatchUnknown.instance, OAMatchAny.instance, -1), Arguments.of(null, OAMatchNotNull.instance, -1), Arguments.of("x", OAMatchNull.instance, -1), Arguments.of("x", OAMatchEmpty.instance, -1), Arguments.of("", OAMatchNotEmpty.instance, -1), Arguments.of(OAMatchUnknown.instance, OAMatchNotExist.instance, -1), Arguments.of(0, OAMatchGreaterThanZero.instance, -1), Arguments.of("0.004", OAMatchGreaterThanZero.instance, 2), Arguments.of(0, OAMatchLessThanZero.instance, -1),
				Arguments.of("-0.004", OAMatchLessThanZero.instance, 2));
	}

}
