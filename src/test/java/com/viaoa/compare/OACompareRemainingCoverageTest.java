package com.viaoa.compare;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
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
import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OACompareRemainingCoverageTest {

    @Nested
    @DisplayName("isLike")
    class IsLike {
        @Test
        void isLikeHandlesWildcardPositionsAndInteriorWildcardOverlap() {
            assertTrue(OACompare.isLike("abcde", "*cde"));
            assertTrue(OACompare.isLike("abcde", "abc*"));
            assertTrue(OACompare.isLike("abcde", "*bcd*"));
            assertTrue(OACompare.isLike("abcde", "%bcd%"));
            assertTrue(OACompare.isLike("abcde", "*"));
            assertTrue(OACompare.isLike(12345, "12*"));

            assertFalse(OACompare.isLike("abcde", ""));
            assertFalse(OACompare.isLike(null, "*"));
            assertFalse(OACompare.isLike("abcde", null));

            assertTrue(OACompare.isLike("abcde", "ab*de"));
            assertTrue(OACompare.isLike("abc", "ab*bc"), "Current implementation allows start/end wildcard portions to overlap");
        }
    }

    @Nested
    @DisplayName("numeric string comparison")
    class NumericStringComparison {
        @Test
        void numericStringsCompareNumericallyNotLexically() {
            assertEquals(0, OACompare.compare("5", "5.00"));
            assertTrue(OACompare.compare("2", "10") < 0);
            assertEquals(0, OACompare.compare("1.004", "1.0044", 2));
            assertTrue(OACompare.compare("1.005", "1.0049", 2) > 0);

            assertTrue(OACompare.compare("b", "aa") > 0);
            assertFalse(OACompare.isEqual("true", "false"));
        }
    }

    @Nested
    @DisplayName("array comparison")
    class ArrayComparison {
        @Test
        void arrayComparisonPropagatesDecimalPlacesAndPreservesOrder() {
            assertEquals(0, OACompare.compare(new Object[] { new BigDecimal("1.004") }, new Object[] { new BigDecimal("1.0044") }, 2));
            assertTrue(OACompare.compare(new Object[] { "a", "b" }, new Object[] { "b", "a" }) < 0);
            assertEquals(0, OACompare.compare(new int[] { 1, 2 }, new Object[] { 1, 2 }));
            assertTrue(OACompare.compare(new int[] { 1, 2 }, new Object[] { 1, 3 }) < 0);
        }

        @Test
        void scalarAndEmptyArrayBehaviorIsDeterministic() {
            assertEquals(0, OACompare.compare("x", new String[] { "x" }));
            assertEquals(0, OACompare.compare(new String[] { "x" }, "x"));
            assertEquals(0, OACompare.compare(new String[0], null));
            assertEquals(0, OACompare.compare(null, new String[0]));
            assertTrue(OACompare.compare(new String[] { "x", "y" }, "x") > 0);
            assertTrue(OACompare.compare("x", new String[] { "x", "y" }) < 0);
        }
    }

    @Nested
    @DisplayName("boolean coercion")
    class BooleanCoercion {
        @Test
        void booleanCoercionDocumentsCurrentTruthinessRules() {
            assertTrue(OACompare.isEqual(true, 1));
            assertTrue(OACompare.isEqual(true, -1));
            assertTrue(OACompare.isEqual(true, "true"));
            assertTrue(OACompare.isEqual(true, "fx"));

            assertTrue(OACompare.isEqual(false, 0));
            assertTrue(OACompare.isEqual(false, ""));
            assertTrue(OACompare.isEqual(false, null));
            assertTrue(OACompare.isEqual(false, "f"));

            assertFalse(OACompare.isEqual("true", "false"));
            assertTrue(OACompare.compare("false", "true") < 0);
        }
    }

    @Nested
    @DisplayName("OA date/time coercion")
    class DateTimeCoercion {
        @Test
        void oaDateTimeStringComparisonIsSymmetric() {
            assertEquals(0, OACompare.compare(new OADate("2026-05-26"), "2026-05-26"));
            assertEquals(0, OACompare.compare("2026-05-26", new OADate("2026-05-26")));

            assertEquals(0, OACompare.compare(new OADateTime("2026-05-26 10:15:30"), "2026-05-26 10:15:30"));
            assertEquals(0, OACompare.compare("2026-05-26 10:15:30", new OADateTime("2026-05-26 10:15:30")));

            assertEquals(0, OACompare.compare(new OATime("10:15:30"), "10:15:30"));
            assertEquals(0, OACompare.compare("10:15:30", new OATime("10:15:30")));
        }
    }

    @Nested
    @DisplayName("public numeric helpers")
    class PublicNumericHelpers {
        @Test
        void publicNumericHelpersExposeDeterministicConversions() {
            assertNull(OACompare.toBigDecimal(null));

            BigDecimal bd = new BigDecimal("123.4500");
            assertSame(bd, OACompare.toBigDecimal(bd));
            assertEquals(new BigDecimal("123456789012345678901234567890"), OACompare.toBigDecimal(new BigInteger("123456789012345678901234567890")));
            assertEquals(new BigDecimal("123"), OACompare.toBigDecimal(Integer.valueOf(123)));
            assertEquals(new BigDecimal("123"), OACompare.toBigDecimal(Long.valueOf(123)));
            assertEquals(BigDecimal.valueOf(0.1d), OACompare.toBigDecimal(Double.valueOf(0.1d)));

            assertTrue(OACompare.isNonFinite(Double.NaN));
            assertTrue(OACompare.isNonFinite(Double.POSITIVE_INFINITY));
            assertTrue(OACompare.isNonFinite(Float.NaN));
            assertTrue(OACompare.isNonFinite(Float.NEGATIVE_INFINITY));

            assertFalse(OACompare.isNonFinite(BigDecimal.ONE));
            assertFalse(OACompare.isNonFinite(BigInteger.ONE));
            assertFalse(OACompare.isNonFinite(Integer.valueOf(1)));
            assertFalse(OACompare.isNonFinite(Double.valueOf(1.0d)));
        }
    }

    @Nested
    @DisplayName("match token ordering")
    class MatchTokenOrdering {
        @ParameterizedTest
        @MethodSource("com.viaoa.compare.OACompareRemainingCoverageTest#allMatchTokens")
        void sameMatchTokenComparesEqual(OAMatch token) {
            assertEquals(0, OACompare.compare(token, token));
        }

        @Test
        void allDifferentMatchTokensCompareNonZeroAndAntisymmetric() {
            OAMatch[] tokens = allMatchTokens().map(args -> (OAMatch) args.get()[0]).toArray(OAMatch[]::new);

            for (OAMatch left : tokens) {
                for (OAMatch right : tokens) {
                    if (left == right) continue;
                    int lr = OACompare.compare(left, right);
                    int rl = OACompare.compare(right, left);
                    assertNotEquals(0, lr, () -> left.getClass().getSimpleName() + " should not compare equal to " + right.getClass().getSimpleName());
                    assertEquals(-Integer.signum(lr), Integer.signum(rl));
                }
            }
        }

        @Test
        void unknownIsNotSwallowedByBroadMatchTokens() {
            assertNotEquals(0, OACompare.compare(OAMatchUnknown.instance, OAMatchAny.instance));
            assertNotEquals(0, OACompare.compare(OAMatchUnknown.instance, OAMatchNotNull.instance));
            assertNotEquals(0, OACompare.compare(OAMatchUnknown.instance, OAMatchNotEmpty.instance));
            assertTrue(OAMatchAny.instance.matches("ordinary", -1));
            assertFalse(OAMatchAny.instance.matches(OAMatchUnknown.instance, -1));
        }
    }

    @Nested
    @DisplayName("Hub comparison")
    class HubComparison {
        @Test
        void hubComparisonAndMembershipUseDocumentedHubRules() {
            TestObject a = new TestObject();
            TestObject b = new TestObject();

            Hub<TestObject> empty = new Hub<>(TestObject.class);
            Hub<TestObject> singleA = new Hub<>(TestObject.class);
            singleA.add(a);
            Hub<TestObject> singleA2 = new Hub<>(TestObject.class);
            singleA2.add(a);
            Hub<TestObject> multipleAB = new Hub<>(TestObject.class);
            multipleAB.add(a);
            multipleAB.add(b);

            assertEquals(0, OACompare.compare(empty, new Hub<>(TestObject.class)));
            assertNotEquals(0, OACompare.compare(singleA, a));
            assertNotEquals(0, OACompare.compare(a, singleA));
            assertEquals(0, OACompare.compare(singleA, singleA2));
            assertNotEquals(0, OACompare.compare(multipleAB, singleA));

            assertTrue(OACompare.isIn(a, multipleAB));
            assertFalse(OACompare.isIn("not an OAObject", multipleAB));
        }
    }

    static Stream<Arguments> allMatchTokens() {
        return Stream.of(
                Arguments.of(OAMatchAny.instance),
                Arguments.of(OAMatchEmpty.instance),
                Arguments.of(OAMatchNotEmpty.instance),
                Arguments.of(OAMatchNull.instance),
                Arguments.of(OAMatchNotNull.instance),
                Arguments.of(OAMatchNotExist.instance),
                Arguments.of(OAMatchUnknown.instance),
                Arguments.of(OAMatchGreaterThanZero.instance),
                Arguments.of(OAMatchLessThanZero.instance));
    }

    public static class TestObject extends OAObject {
        private static final long serialVersionUID = 1L;
    }
}
