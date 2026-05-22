  package com.viaoa.compare;

  import static org.junit.jupiter.api.Assertions.assertEquals;
  import static org.junit.jupiter.api.Assertions.assertFalse;
  import static org.junit.jupiter.api.Assertions.assertThrows;
  import static org.junit.jupiter.api.Assertions.assertTrue;

  import java.math.BigDecimal;
  import java.math.BigInteger;
  import java.util.Arrays;
  import java.util.Collections;
  import java.util.List;
  import java.util.stream.Stream;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Nested;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.params.ParameterizedTest;
  import org.junit.jupiter.params.provider.Arguments;
  import org.junit.jupiter.params.provider.CsvSource;
  import org.junit.jupiter.params.provider.MethodSource;
  import org.junit.jupiter.params.provider.NullAndEmptySource;
  import org.junit.jupiter.params.provider.ValueSource;

  import com.viaoa.datetime.OADate;
  import com.viaoa.datetime.OADateTime;
  import com.viaoa.datetime.OATime;

  @DisplayName("OACompare")
  class OACompareTest {

      @Nested
      @DisplayName("primitive compare")
      class PrimitiveCompareTest {
          @ParameterizedTest(name = "compare({0}, {1}) == {2}")
          @CsvSource({
                  "0, 0, 0",
                  "1, 1, 0",
                  "-1, -1, 0",
                  "1, 2, -1",
                  "2, 1, 1",
                  "-2, -1, -1",
                  "-1, -2, 1",
                  "2147483647, -2147483648, 1",
                  "-2147483648, 2147483647, -1"
          })
          @DisplayName("int comparison delegates to Integer.compare")
          void compareInts(int a, int b, int expected) {
              assertEquals(expected, Integer.signum(OACompare.compare(a, b)));
          }

          @ParameterizedTest(name = "compare({0}, {1}, {2}) == {3}")
          @CsvSource({
                  "1.005, 1.0049, 2, 0",
                  "1.005, 1.0049, 3, 1",
                  "-1.005, -1.0049, 2, 0",
                  "-1.005, -1.0049, 3, -1",
                  "0.0, -0.0, 2, 0",
                  "0.1, 0.1000000000001, 9, 0",
                  "0.1, 0.1000000000001, 12, 0",
                  "1.2345, 1.2344, 3, 0",
                  "1.2345, 1.2344, 4, 1"
          })
          @DisplayName("double comparison is scale-aware when decimal places are supplied")
          void compareDoublesWithScale(double a, double b, int decimalPlaces, int expectedSign) {
              assertEquals(expectedSign, Integer.signum(OACompare.compare(a, b, decimalPlaces)));
              assertEquals(-expectedSign, Integer.signum(OACompare.compare(b, a, decimalPlaces)));
          }

          @ParameterizedTest(name = "compare({0}, {1}, -1) == 0")
          @CsvSource({
                  "1.0, 1.0000000000001",
                  "1000.0, 1000.0000000001",
                  "-1000.0, -1000.0000000001"
          })
          @DisplayName("unscaled double comparison uses relative epsilon tolerance")
          void unscaledDoubleComparisonUsesEpsilon(double a, double b) {
              assertEquals(0, OACompare.compare(a, b, -1));
              assertTrue(OACompare.isEqual(a, b));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OACompareTest#nonFiniteDoubleComparisons")
          @DisplayName("NaN and infinity use Double.compare ordering")
          void nonFiniteDoubleComparison(double a, double b, int expectedSign) {
              assertEquals(expectedSign, Integer.signum(OACompare.compare(a, b, 2)));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OACompareTest#doubleEqualityCases")
          @DisplayName("double isEqual follows compare semantics")
          void doubleIsEqual(double a, double b, int decimalPlaces, boolean expected) {
              assertEquals(expected, OACompare.isEqual(a, b, decimalPlaces));
          }
      }

      @Nested
      @DisplayName("object compare")
      class ObjectCompareTest {
          @Test
          @DisplayName("null comparison is deterministic")
          void nullComparison() {
              assertEquals(0, OACompare.compare(null, null));
              assertTrue(OACompare.compare(null, "x") < 0);
              assertTrue(OACompare.compare("x", null) > 0);

              assertTrue(OACompare.isEqual(null, null));
              assertFalse(OACompare.isEqual(null, "x"));
              assertTrue(OACompare.isNotEqual(null, "x"));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OACompareTest#numericEqualityCases")
          @DisplayName("mixed numeric types compare consistently")
          void mixedNumericTypesCompareConsistently(Object a, Object b, int decimalPlaces, boolean expectedEqual) {
              assertEquals(expectedEqual, OACompare.isEqual(a, b, decimalPlaces));
              assertEquals(expectedEqual ? 0 : -Integer.signum(OACompare.compare(b, a, decimalPlaces)),
                      Integer.signum(OACompare.compare(a, b, decimalPlaces)));
          }

          @Test
          @DisplayName("BigDecimal scale does not affect equality for same numeric value")
          void bigDecimalScaleDoesNotAffectEquality() {
              assertEquals(0, OACompare.compare(new BigDecimal("1.0"), new BigDecimal("1.00")));
              assertTrue(OACompare.isEqual(new BigDecimal("1.0"), new BigDecimal("1.00")));
          }

          @Test
          @DisplayName("large BigInteger values compare without double conversion when both sides are BigInteger")
          void bigIntegerCompareUsesExactBigIntegerPath() {
              BigInteger a = new BigInteger("123456789012345678901234567890");
              BigInteger b = new BigInteger("123456789012345678901234567891");

              assertTrue(OACompare.compare(a, b) < 0);
              assertTrue(OACompare.compare(b, a) > 0);
              assertEquals(0, OACompare.compare(a, new BigInteger("123456789012345678901234567890")));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OACompareTest#stringCompareCases")
          @DisplayName("string comparison is case-sensitive by default")
          void stringCompareIsCaseSensitive(String a, String b, int expectedSign) {
              assertEquals(expectedSign, Integer.signum(OACompare.compare(a, b)));
          }

          @ParameterizedTest
          @CsvSource({
                  "abc, ABC, true",
                  "abc, AbC, true",
                  "abc, abd, false",
                  "'', '', true",
                  "' ', ' ', true",
                  "' ', '', false"
          })
          @DisplayName("explicit ignore-case equality lowercases string inputs")
          void stringIgnoreCaseEquality(String a, String b, boolean expected) {
              assertEquals(expected, OACompare.isEqual(a, b, true));
              assertEquals(expected, OACompare.isEqualIgnoreCase(a, b));
              assertEquals(!expected, OACompare.isNotEqual(a, b, true));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OACompareTest#booleanCompareCases")
          @DisplayName("boolean comparison uses Boolean.compare semantics")
          void booleanComparison(Boolean a, Boolean b, int expectedSign) {
              assertEquals(expectedSign, Integer.signum(OACompare.compare(a, b)));
              assertEquals(expectedSign == 0, OACompare.isEqual(a, b));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OACompareTest#dateTimeCompareCases")
          @DisplayName("OA date/time values compare through their Comparable contracts")
          void dateTimeComparison(Object a, Object b, int expectedSign) {
              assertEquals(expectedSign, Integer.signum(OACompare.compare(a, b)));
              assertEquals(-expectedSign, Integer.signum(OACompare.compare(b, a)));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OACompareTest#numericStringCases")
          @DisplayName("numeric strings can compare equal to numeric values")
          void numericStringsCompareToNumbers(Object a, Object b, int decimalPlaces, boolean expectedEqual) {
              assertEquals(expectedEqual, OACompare.isEqual(a, b, decimalPlaces));
          }

          @Test
          @DisplayName("incompatible non-comparable values fall back to toString ordering")
          void incompatibleNonComparableValuesUseToStringOrdering() {
              Object a = new PlainValue("a");
              Object b = new PlainValue("b");

              assertTrue(OACompare.compare(a, b) < 0);
              assertTrue(OACompare.compare(b, a) > 0);
              assertEquals(0, OACompare.compare(new PlainValue("same"), new PlainValue("same")));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OACompareTest#objectComparePairs")
          @DisplayName("object compare is deterministic and antisymmetric for supported comparable pairs")
          void compareIsDeterministicAndAntisymmetric(Object a, Object b, int decimalPlaces) {
              int ab = Integer.signum(OACompare.compare(a, b, decimalPlaces));
              int ba = Integer.signum(OACompare.compare(b, a, decimalPlaces));

              assertEquals(-ab, ba);
              for (int i = 0; i < 100; i++) {
                  assertEquals(ab, Integer.signum(OACompare.compare(a, b, decimalPlaces)));
                  assertEquals(ba, Integer.signum(OACompare.compare(b, a, decimalPlaces)));
              }
          }
      }

      @Nested
      @DisplayName("relational helpers")
      class RelationalHelperTest {
          @ParameterizedTest
          @CsvSource({
                  "5, 4, true",
                  "5, 5, false",
                  "4, 5, false",
                  "-1, -2, true",
                  "-2, -1, false"
          })
          @DisplayName("isGreater is strict")
          void isGreaterIsStrict(int value, int fromValue, boolean expected) {
              assertEquals(expected, OACompare.isGreater(value, fromValue));
          }

          @ParameterizedTest
          @CsvSource({
                  "5, 4, true",
                  "5, 5, true",
                  "4, 5, false",
                  "-1, -2, true",
                  "-2, -1, false"
          })
          @DisplayName("greater-or-equal aliases include equality")
          void greaterOrEqualAliasesIncludeEquality(int value, int fromValue, boolean expected) {
              assertEquals(expected, OACompare.isEqualOrGreater(value, fromValue));
              assertEquals(expected, OACompare.isGreaterOrEqual(value, fromValue));
          }

          @ParameterizedTest
          @CsvSource({
                  "4, 5, true",
                  "5, 5, false",
                  "5, 4, false",
                  "-2, -1, true",
                  "-1, -2, false"
          })
          @DisplayName("isLess is strict")
          void isLessIsStrict(int value, int fromValue, boolean expected) {
              assertEquals(expected, OACompare.isLess(value, fromValue));
          }

          @ParameterizedTest
          @CsvSource({
                  "4, 5, true",
                  "5, 5, true",
                  "5, 4, false",
                  "-2, -1, true",
                  "-1, -2, false"
          })
          @DisplayName("less-or-equal aliases include equality")
          void lessOrEqualAliasesIncludeEquality(int value, int fromValue, boolean expected) {
              assertEquals(expected, OACompare.isEqualOrLess(value, fromValue));
              assertEquals(expected, OACompare.isLessOrEqual(value, fromValue));
          }

          @ParameterizedTest
          @CsvSource({
                  "5, 1, 10, true",
                  "1, 1, 10, false",
                  "10, 1, 10, false",
                  "0, 1, 10, false",
                  "11, 1, 10, false"
          })
          @DisplayName("isBetween is exclusive")
          void isBetweenIsExclusive(int value, int fromValue, int toValue, boolean expected) {
              assertEquals(expected, OACompare.isBetween(value, fromValue, toValue));
          }

          @ParameterizedTest
          @CsvSource({
                  "5, 1, 10, true",
                  "1, 1, 10, true",
                  "10, 1, 10, true",
                  "0, 1, 10, false",
                  "11, 1, 10, false"
          })
          @DisplayName("equal-or-between is inclusive")
          void equalOrBetweenIsInclusive(int value, int fromValue, int toValue, boolean expected) {
              assertEquals(expected, OACompare.isEqualOrBetween(value, fromValue, toValue));
              assertEquals(expected, OACompare.isBetweenOrEqual(value, fromValue, toValue));
          }

          @Test
          @DisplayName("between helpers have deterministic null boundary behavior")
          void betweenNullBoundaryBehavior() {
              assertFalse(OACompare.isBetween(null, 1, 10));
              assertFalse(OACompare.isBetween(5, 1, null));
              assertTrue(OACompare.isEqualOrBetween(null, null, 10));
              assertFalse(OACompare.isEqualOrBetween(null, 1, 10));
              assertFalse(OACompare.isEqualOrBetween(5, 1, null));
          }

          @Test
          @DisplayName("scale-aware relational helpers use requested decimal places")
          void scaleAwareRelationalHelpers() {
              assertFalse(OACompare.isGreater(1.005, 1.0049, 2));
              assertTrue(OACompare.isEqualOrGreater(1.005, 1.0049, 2));
              assertTrue(OACompare.isGreater(1.005, 1.0049, 3));

              assertFalse(OACompare.isLess(1.0049, 1.005, 2));
              assertTrue(OACompare.isEqualOrLess(1.0049, 1.005, 2));
              assertTrue(OACompare.isLess(1.0049, 1.005, 3));
          }
      }

      @Nested
      @DisplayName("arrays and membership")
      class ArrayAndMembershipTest {
          @Test
          @DisplayName("isIn supports arrays and delegates element comparison through OACompare")
          void isInSupportsArrays() {
              Object[] values = { "a", "b", "3" };

              assertTrue(OACompare.isIn("a", values));
              assertTrue(OACompare.isIn(3, values));
              assertFalse(OACompare.isIn("c", values));
              assertTrue(OACompare.isEqualOrIn(3, values));
          }

          @Test
          @DisplayName("isIn returns false when object or match value is null")
          void isInNullBehavior() {
              assertFalse(OACompare.isIn(null, new Object[] { null }));
              assertFalse(OACompare.isIn("x", null));
              assertFalse(OACompare.isEqualOrIn(null, new Object[] { null }));
          }

          @Test
          @DisplayName("array comparison is lexicographic when both sides are arrays")
          void arrayComparisonIsLexicographic() {
              assertEquals(0, OACompare.compare(new int[] { 1, 2 }, new int[] { 1, 2 }));
              assertTrue(OACompare.compare(new int[] { 1, 2 }, new int[] { 1, 3 }) < 0);
              assertTrue(OACompare.compare(new int[] { 1, 3 }, new int[] { 1, 2 }) > 0);
              assertTrue(OACompare.compare(new int[] { 1, 2, 3 }, new int[] { 1, 2 }) > 0);
              assertTrue(OACompare.compare(new int[] { 1 }, new int[] { 1, 2 }) < 0);
          }

          @Test
          @DisplayName("single-element arrays compare as their single element against non-array values")
          void singleElementArrayComparesAsElement() {
              assertEquals(0, OACompare.compare(new String[] { "a" }, "a"));
              assertEquals(0, OACompare.compare("a", new String[] { "a" }));
              assertTrue(OACompare.compare(new String[] { "a", "b" }, "a") > 0);
              assertTrue(OACompare.compare("a", new String[] { "a", "b" }) < 0);
          }

          @Test
          @DisplayName("array versus Boolean tests empty/non-empty state")
          void arrayVersusBooleanTestsEmptiness() {
              assertEquals(0, OACompare.compare(new String[] { "x" }, Boolean.TRUE));
              assertTrue(OACompare.compare(new String[] {}, Boolean.TRUE) < 0);
              assertEquals(0, OACompare.compare(new String[] {}, Boolean.FALSE));
              assertTrue(OACompare.compare(new String[] { "x" }, Boolean.FALSE) > 0);

              assertEquals(0, OACompare.compare(Boolean.TRUE, new String[] { "x" }));
              assertTrue(OACompare.compare(Boolean.TRUE, new String[] {}) > 0);
              assertEquals(0, OACompare.compare(Boolean.FALSE, new String[] {}));
              assertTrue(OACompare.compare(Boolean.FALSE, new String[] { "x" }) < 0);
          }
      }

      @Nested
      @DisplayName("like")
      class LikeTest {
          @ParameterizedTest
          @CsvSource({
                  "John Smith, John Smith, true",
                  "John Smith, john smith, true",
                  "John Smith, John*, true",
                  "John Smith, john*, true",
                  "John Smith, '*Smith', true",
                  "John Smith, '*smiTH', true",
                  "John Smith, '*hn Sm*', true",
                  "John Smith, '%hn Sm%', true",
                  "John Smith, J*th, true",
                  "John Smith, J*n, false",
                  "John Smith, Smith*, false",
                  "John Smith, '', false",
                  "John Smith, '*', true",
                  "John Smith, '%', true"
          })
          @DisplayName("wildcard matching is case-insensitive")
          void wildcardMatchingIsCaseInsensitive(String value, String pattern, boolean expected) {
              assertEquals(expected, OACompare.isLike(value, pattern));
          }

          @Test
          @DisplayName("like returns false for nulls and non-string patterns unless direct compare succeeds")
          void likeNullAndNonStringPatternBehavior() {
              assertTrue(OACompare.isLike(null, null));
              assertFalse(OACompare.isLike(null, "x"));
              assertFalse(OACompare.isLike("x", null));
              assertTrue(OACompare.isLike(1, 1));
              assertFalse(OACompare.isLike(1, 2));
              assertFalse(OACompare.isLike("1", 2));
          }
      }

      @Nested
      @DisplayName("empty")
      class EmptyTest {
          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OACompareTest#emptyValues")
          @DisplayName("isEmpty identifies OA empty values")
          void isEmptyIdentifiesEmptyValues(Object value) {
              assertTrue(OACompare.isEmpty(value));
              assertFalse(OACompare.isNotEmpty(value));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OACompareTest#notEmptyValues")
          @DisplayName("isNotEmpty identifies OA non-empty values")
          void isNotEmptyIdentifiesNotEmptyValues(Object value) {
              assertFalse(OACompare.isEmpty(value));
              assertTrue(OACompare.isNotEmpty(value));
          }

          @ParameterizedTest
          @NullAndEmptySource
          @ValueSource(strings = { " ", "  ", "\t" })
          @DisplayName("trim option treats whitespace strings as empty")
          void trimOptionTreatsWhitespaceAsEmpty(String value) {
              assertTrue(OACompare.isEmpty(value, true));
              assertFalse(OACompare.isNotEmpty(value, true));
          }

          @ParameterizedTest
          @ValueSource(strings = { " ", "  ", "\t" })
          @DisplayName("without trim, whitespace strings are not empty")
          void withoutTrimWhitespaceIsNotEmpty(String value) {
              assertFalse(OACompare.isEmpty(value, false));
              assertTrue(OACompare.isNotEmpty(value, false));
          }
      }

      static Stream<Arguments> nonFiniteDoubleComparisons() {
          return Stream.of(
                  Arguments.of(Double.NaN, Double.NaN, 0),
                  Arguments.of(Double.NaN, 1.0, 1),
                  Arguments.of(1.0, Double.NaN, -1),
                  Arguments.of(Double.POSITIVE_INFINITY, 1.0, 1),
                  Arguments.of(1.0, Double.POSITIVE_INFINITY, -1),
                  Arguments.of(Double.NEGATIVE_INFINITY, 1.0, -1),
                  Arguments.of(1.0, Double.NEGATIVE_INFINITY, 1),
                  Arguments.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -1));
      }

      static Stream<Arguments> doubleEqualityCases() {
          return Stream.of(
                  Arguments.of(1.005, 1.0049, 2, true),
                  Arguments.of(1.005, 1.0049, 3, false),
                  Arguments.of(0.0, -0.0, 2, true),
                  Arguments.of(Double.NaN, Double.NaN, 2, true),
                  Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 2, true),
                  Arguments.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 2, false));
      }

      static Stream<Arguments> numericEqualityCases() {
          return Stream.of(
                  Arguments.of(1, 1L, -1, true),
                  Arguments.of(1, 1.0d, -1, true),
                  Arguments.of(1.0f, 1.0d, -1, true),
                  Arguments.of(new BigDecimal("1.0"), new BigDecimal("1.00"), -1, true),
                  Arguments.of(new BigDecimal("1.005"), new BigDecimal("1.0049"), 2, false),
                  Arguments.of(new BigDecimal("1.005"), new BigDecimal("1.0049"), 3, true),
                  Arguments.of(new BigInteger("100000000000000000000"), new BigInteger("100000000000000000000"), -1, true),
                  Arguments.of(Double.NaN, Double.NaN, -1, true),
                  Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, -1, true),
                  Arguments.of(0.0d, -0.0d, -1, true));
      }

      static Stream<Arguments> stringCompareCases() {
          return Stream.of(
                  Arguments.of("", "", 0),
                  Arguments.of("", " ", -1),
                  Arguments.of(" ", "", 1),
                  Arguments.of("ABC", "abc", -1),
                  Arguments.of("abc", "ABC", 1),
                  Arguments.of("abc", "abc", 0),
                  Arguments.of("abc", "abd", -1),
                  Arguments.of("abd", "abc", 1));
      }

      static Stream<Arguments> booleanCompareCases() {
          return Stream.of(
                  Arguments.of(Boolean.FALSE, Boolean.FALSE, 0),
                  Arguments.of(Boolean.TRUE, Boolean.TRUE, 0),
                  Arguments.of(Boolean.FALSE, Boolean.TRUE, -1),
                  Arguments.of(Boolean.TRUE, Boolean.FALSE, 1));
      }

      static Stream<Arguments> dateTimeCompareCases() {
          return Stream.of(
                  Arguments.of(new OADate(2024, 1, 1), new OADate(2024, 1, 1), 0),
                  Arguments.of(new OADate(2024, 1, 1), new OADate(2024, 1, 2), -1),
                  Arguments.of(new OADateTime(2024, 1, 1, 12, 0, 0), new OADateTime(2024, 1, 1, 12, 0, 1), -1),
                  Arguments.of(new OATime(12, 0, 0), new OATime(12, 0, 1), -1));
      }

      static Stream<Arguments> numericStringCases() {
          return Stream.of(
                  Arguments.of("1", 1, -1, true),
                  Arguments.of("1.00", 1.0d, -1, true),
                  Arguments.of("1.005", 1.0049d, 2, true),
                  Arguments.of("1.005", 1.0049d, 3, false),
                  Arguments.of("not-number", 1, -1, false));
      }

      static Stream<Arguments> objectComparePairs() {
          return Stream.of(
                  Arguments.of(null, null, -1),
                  Arguments.of(null, "x", -1),
                  Arguments.of("a", "b", -1),
                  Arguments.of(1, 2, -1),
                  Arguments.of(1.005, 1.0049, 2),
                  Arguments.of(Boolean.FALSE, Boolean.TRUE, -1),
                  Arguments.of(new BigDecimal("1.0"), new BigDecimal("1.00"), -1),
                  Arguments.of(new OADate(2024, 1, 1), new OADate(2024, 1, 2), -1),
                  Arguments.of(new PlainValue("a"), new PlainValue("b"), -1));
      }

      static Stream<Object> emptyValues() {
          return Stream.of(
                  null,
                  "",
                  Integer.valueOf(0),
                  Long.valueOf(0L),
                  Double.valueOf(0.0d),
                  Float.valueOf(0.0f),
                  Boolean.FALSE,
                  Character.valueOf('\0'),
                  // new Object[] {},
                  Collections.emptyList());
      }

      static Stream<Object> notEmptyValues() {
          return Stream.of(
                  " ",
                  "x",
                  Integer.valueOf(1),
                  Long.valueOf(-1L),
                  Double.valueOf(0.0001d),
                  Float.valueOf(-0.0001f),
                  Boolean.TRUE,
                  Character.valueOf('x'),
                  new Object[] { "x" },
                  new int[] { 0 },
                  Arrays.asList("x"));
      }

      private static final class PlainValue {
          private final String value;

          private PlainValue(String value) {
              this.value = value;
          }

          @Override
          public String toString() {
              return value;
          }
      }

  }

