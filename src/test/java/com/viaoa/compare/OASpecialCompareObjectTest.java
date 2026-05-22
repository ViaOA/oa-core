  package com.viaoa.compare;

  import static org.junit.jupiter.api.Assertions.assertEquals;
  import static org.junit.jupiter.api.Assertions.assertFalse;
  import static org.junit.jupiter.api.Assertions.assertSame;
  import static org.junit.jupiter.api.Assertions.assertTrue;

  import java.math.BigDecimal;
  import java.math.BigInteger;
  import java.util.Collections;
  import java.util.stream.Stream;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Nested;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.params.ParameterizedTest;
  import org.junit.jupiter.params.provider.MethodSource;

  @DisplayName("OA special compare objects")
  class OASpecialCompareObjectTest {

      @Nested
      @DisplayName("singleton accessors")
      class SingletonAccessorTest {
          @Test
          @DisplayName("accessors return singleton instances")
          void accessorsReturnSingletonInstances() {
              assertSame(OANullObject.instance, OANullObject.instance.getNullObject());
              assertSame(OANotNullObject.instance, OANotNullObject.instance.getNotNullObject());
              assertSame(OAAnyValueObject.instance, OAAnyValueObject.instance.getNullObject());
              assertSame(OAEmptyObject.instance, OAEmptyObject.instance.getNotEmptyObject());
              assertSame(OANotEmptyObject.instance, OANotEmptyObject.instance.getNotEmptyObject());
              assertSame(OAGreaterThanZero.instance, OAGreaterThanZero.instance.getGreaterThanZeroObject());
              assertSame(OALessThanZero.instance, OALessThanZero.instance.getLessThanZeroObject());
              assertSame(OANotExist.instance, OANotExist.instance.getNotExistObject());
              assertSame(OAUnknownObject.instance, OAUnknownObject.instance.getUnknownObject());
          }

          @Test
          @DisplayName("all singleton tokens use stable constant hash code")
          void allSingletonTokensUseStableConstantHashCode() {
              assertEquals(1, OANullObject.instance.hashCode());
              assertEquals(1, OANotNullObject.instance.hashCode());
              assertEquals(1, OAAnyValueObject.instance.hashCode());
              assertEquals(1, OAEmptyObject.instance.hashCode());
              assertEquals(1, OANotEmptyObject.instance.hashCode());
              assertEquals(1, OAGreaterThanZero.instance.hashCode());
              assertEquals(1, OALessThanZero.instance.hashCode());
              assertEquals(1, OANotExist.instance.hashCode());
              assertEquals(1, OAUnknownObject.instance.hashCode());
          }
      }

      @Nested
      @DisplayName("equals semantics")
      class EqualsSemanticsTest {
          @Test
          @DisplayName("null token matches null and null tokens")
          void nullTokenMatchesNullAndNullTokens() {
              assertTrue(OANullObject.instance.equals(null));
              assertTrue(OANullObject.instance.equals(OANullObject.instance));
              assertFalse(OANullObject.instance.equals(""));
              assertFalse(OANullObject.instance.equals(0));
          }

          @Test
          @DisplayName("not-null token matches any non-null value")
          void notNullTokenMatchesAnyNonNullValue() {
              assertFalse(OANotNullObject.instance.equals(null));
              assertTrue(OANotNullObject.instance.equals(""));
              assertTrue(OANotNullObject.instance.equals(0));
              assertTrue(OANotNullObject.instance.equals(OANotNullObject.instance));
          }

          @Test
          @DisplayName("any-value token matches every value including null")
          void anyValueTokenMatchesEveryValue() {
              assertTrue(OAAnyValueObject.instance.equals(null));
              assertTrue(OAAnyValueObject.instance.equals(""));
              assertTrue(OAAnyValueObject.instance.equals(0));
              assertTrue(OAAnyValueObject.instance.equals(new Object()));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OASpecialCompareObjectTest#emptyValues")
          @DisplayName("empty token matches OA empty values")
          void emptyTokenMatchesEmptyValues(Object value) {
              assertTrue(OAEmptyObject.instance.equals(value));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OASpecialCompareObjectTest#notEmptyValues")
          @DisplayName("empty token rejects OA non-empty values")
          void emptyTokenRejectsNotEmptyValues(Object value) {
              assertFalse(OAEmptyObject.instance.equals(value));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OASpecialCompareObjectTest#notEmptyValues")
          @DisplayName("not-empty token matches OA non-empty values")
          void notEmptyTokenMatchesNotEmptyValues(Object value) {
              assertTrue(OANotEmptyObject.instance.equals(value));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OASpecialCompareObjectTest#emptyValues")
          @DisplayName("not-empty token rejects OA empty values")
          void notEmptyTokenRejectsEmptyValues(Object value) {
              assertFalse(OANotEmptyObject.instance.equals(value));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OASpecialCompareObjectTest#greaterThanZeroMatches")
          @DisplayName("greater-than-zero token matches values convertible to numbers greater than zero")
          void greaterThanZeroTokenMatchesPositiveValues(Object value) {
              assertTrue(OAGreaterThanZero.instance.equals(value));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OASpecialCompareObjectTest#greaterThanZeroRejects")
          @DisplayName("greater-than-zero token rejects non-positive or non-numeric values")
          void greaterThanZeroTokenRejectsNonPositiveValues(Object value) {
              assertFalse(OAGreaterThanZero.instance.equals(value));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OASpecialCompareObjectTest#lessThanZeroMatches")
          @DisplayName("less-than-zero token currently matches null and values convertible to numbers less than zero")
          void lessThanZeroTokenMatchesNegativeValuesAndNull(Object value) {
              assertTrue(OALessThanZero.instance.equals(value));
          }

          @ParameterizedTest
          @MethodSource("com.viaoa.compare.OASpecialCompareObjectTest#lessThanZeroRejects")
          @DisplayName("less-than-zero token rejects zero, positive, and non-numeric values")
          void lessThanZeroTokenRejectsNonNegativeValues(Object value) {
              assertFalse(OALessThanZero.instance.equals(value));
          }

          @Test
          @DisplayName("not-exist token matches null and itself")
          void notExistTokenMatchesNullAndItself() {
              assertTrue(OANotExist.instance.equals(null));
              assertTrue(OANotExist.instance.equals(OANotExist.instance));
              assertFalse(OANotExist.instance.equals(""));
              assertFalse(OANotExist.instance.equals(0));
          }

          @Test
          @DisplayName("unknown token matches only unknown token instances")
          void unknownTokenMatchesOnlyUnknownTokens() {
              assertTrue(OAUnknownObject.instance.equals(OAUnknownObject.instance));
              assertFalse(OAUnknownObject.instance.equals(null));
              assertFalse(OAUnknownObject.instance.equals(""));
              assertFalse(OAUnknownObject.instance.equals(0));
          }
      }

      @Nested
      @DisplayName("OACompare integration")
      class OACompareIntegrationTest {
          @Test
          @DisplayName("any-value token compares equal to everything")
          void anyValueTokenComparesEqualToEverything() {
              assertEquals(0, OACompare.compare(OAAnyValueObject.instance, null));
              assertEquals(0, OACompare.compare(null, OAAnyValueObject.instance));
              assertEquals(0, OACompare.compare(OAAnyValueObject.instance, "x"));
              assertEquals(0, OACompare.compare("x", OAAnyValueObject.instance));
          }

          @Test
          @DisplayName("null token compares equal to null and orders against non-null values")
          void nullTokenCompareBehavior() {
              assertEquals(0, OACompare.compare(OANullObject.instance, null));
              assertEquals(0, OACompare.compare(null, OANullObject.instance));
              assertTrue(OACompare.compare(OANullObject.instance, "x") > 0);
              assertTrue(OACompare.compare("x", OANullObject.instance) < 0);
          }

          @Test
          @DisplayName("not-null token compares equal to non-null values")
          void notNullTokenCompareBehavior() {
              assertEquals(0, OACompare.compare(OANotNullObject.instance, "x"));
              assertEquals(0, OACompare.compare("x", OANotNullObject.instance));
              assertTrue(OACompare.compare(OANotNullObject.instance, null) > 0);
              assertTrue(OACompare.compare(null, OANotNullObject.instance) < 0);
          }

          @Test
          @DisplayName("empty and not-empty tokens compare using current special-token semantics")
          void emptyAndNotEmptyTokenCompareBehavior() {
              assertEquals(0, OACompare.compare(OAEmptyObject.instance, null));
              assertEquals(0, OACompare.compare(OAEmptyObject.instance, ""));
              assertEquals(0, OACompare.compare(null, OAEmptyObject.instance));
              assertEquals(0, OACompare.compare("", OAEmptyObject.instance));
              assertTrue(OACompare.compare(OAEmptyObject.instance, "x") > 0);
              assertTrue(OACompare.compare("x", OAEmptyObject.instance) < 0);

              assertEquals(0, OACompare.compare(OANotEmptyObject.instance, "x"));
              assertEquals(0, OACompare.compare("x", OANotEmptyObject.instance));
              assertTrue(OACompare.compare(OANotEmptyObject.instance, null) < 0);
              assertTrue(OACompare.compare(null, OANotEmptyObject.instance) > 0);
          }

          @Test
          @DisplayName("unknown token sorts after concrete values when it is left value")
          void unknownTokenCompareBehavior() {
              assertEquals(0, OACompare.compare(OAUnknownObject.instance, OAUnknownObject.instance));
              assertTrue(OACompare.compare(OAUnknownObject.instance, "x") > 0);
              assertTrue(OACompare.compare("x", OAUnknownObject.instance) < 0);
          }
      }

      static Stream<Object> emptyValues() {
          return Stream.of(
                  null,
                  "",
                  " ",
                  "  ",
                  Integer.valueOf(0),
                  Long.valueOf(0L),
                  Double.valueOf(0.0d),
                  Float.valueOf(0.0f),
                  Boolean.FALSE,
                  Character.valueOf('\0'),
                  new Object[] {},
                  Collections.emptyList());
      }

      static Stream<Object> notEmptyValues() {
          return Stream.of(
                  "x",
                  Integer.valueOf(1),
                  Integer.valueOf(-1),
                  Long.valueOf(1L),
                  Double.valueOf(0.0001d),
                  Float.valueOf(-0.0001f),
                  Boolean.TRUE,
                  Character.valueOf('x'),
                  new Object[] { "x" },
                  Collections.singletonList("x"),
                  OANotEmptyObject.instance);
      }

      static Stream<Object> greaterThanZeroMatches() {
          return Stream.of(
                  Integer.valueOf(1),
                  Long.valueOf(1L),
                  Double.valueOf(0.0001d),
                  Float.valueOf(0.0001f),
                  BigDecimal.ONE,
                  BigInteger.ONE,
                  "1",
                  "0.0001");
      }

      static Stream<Object> greaterThanZeroRejects() {
          return Stream.of(
                  null,
                  Integer.valueOf(0),
                  Integer.valueOf(-1),
                  Double.valueOf(0.0d),
                  Double.valueOf(-0.0001d),
                  BigDecimal.ZERO,
                  BigDecimal.ONE.negate(),
                  "0",
                  "-1",
                  "not-number");
      }

      static Stream<Object> lessThanZeroMatches() {
          return Stream.of(
                  null,
                  Integer.valueOf(-1),
                  Long.valueOf(-1L),
                  Double.valueOf(-0.0001d),
                  Float.valueOf(-0.0001f),
                  BigDecimal.ONE.negate(),
                  BigInteger.ONE.negate(),
                  "-1",
                  "-0.0001");
      }

      static Stream<Object> lessThanZeroRejects() {
          return Stream.of(
                  Integer.valueOf(0),
                  Integer.valueOf(1),
                  Double.valueOf(0.0d),
                  Double.valueOf(0.0001d),
                  BigDecimal.ZERO,
                  BigDecimal.ONE,
                  "0",
                  "1",
                  "not-number");
      }

  }

