  package com.viaoa.compare;

  import static org.junit.jupiter.api.Assertions.*;

  import java.math.BigDecimal;
  import java.util.Arrays;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Nested;
  import org.junit.jupiter.api.Test;

  class OACompareIsInTest {

      @Nested
      @DisplayName("Array membership")
      class ArrayMembership {

          @Test
          void findsValuesInObjectArraysUsingOACompareSemantics() {
              Object[] values = { "1", "2", "3" };

              assertTrue(OACompare.isIn(2, values));
              assertTrue(OACompare.isIn("2", values));
              assertFalse(OACompare.isIn(4, values));
          }

          @Test
          void findsValuesInPrimitiveArraysUsingOACompareSemantics() {
              int[] values = { 1, 2, 3 };

              assertTrue(OACompare.isIn("2", values));
              assertTrue(OACompare.isIn(2L, values));
              assertFalse(OACompare.isIn(4, values));
          }

          @Test
          void nullCanMatchNullElementInArray() {
              Object[] values = { "x", null };

              assertTrue(OACompare.isIn(null, values));
              assertFalse(OACompare.isIn(null, new Object[] { "x" }));
          }
      }

      @Nested
      @DisplayName("Collection membership")
      class CollectionMembership {

          @Test
          void findsValuesInCollectionsUsingContainsOrOACompareSemantics() {
              List<Object> values = Arrays.asList("1", "2", new BigDecimal("3.00"));

              assertTrue(OACompare.isIn(2, values));
              assertTrue(OACompare.isIn("3.0", values));
              assertFalse(OACompare.isIn(4, values));
          }

          @Test
          void nullCanMatchNullElementInCollection() {
              List<Object> values = Arrays.asList("x", null);

              assertTrue(OACompare.isIn(null, values));
              assertFalse(OACompare.isIn(null, List.of("x")));
          }
      }

      @Nested
      @DisplayName("Map membership")
      class MapMembership {

          @Test
          void mapsMatchAgainstKeysNotValues() {
              Map<Object, Object> map = new HashMap<>();
              map.put("1", "not searched");
              map.put("2", null);

              assertTrue(OACompare.isIn(1, map));
              assertTrue(OACompare.isIn("2", map));
              assertFalse(OACompare.isIn("not searched", map));
          }

          @Test
          void mapContainsKeyAllowsNullMappedValuesToMatch() {
              Map<Object, Object> map = new HashMap<>();
              map.put("key", null);

              assertTrue(OACompare.isIn("key", map));
          }

          @Test
          void nullCanMatchNullKeyInMap() {
              Map<Object, Object> map = new HashMap<>();
              map.put(null, "value");

              assertTrue(OACompare.isIn(null, map));
              assertFalse(OACompare.isIn(null, Map.of("x", "value")));
          }
      }

      @Nested
      @DisplayName("Scalar fallback")
      class ScalarFallback {

          @Test
          void scalarMatchUsesOACompareEquality() {
              assertTrue(OACompare.isIn("5", 5));
              assertTrue(OACompare.isIn(5, "5"));
              assertFalse(OACompare.isIn(5, "6"));
          }

          @Test
          void nullScalarBehaviorIsDeterministic() {
              assertTrue(OACompare.isIn(null, null));
              assertFalse(OACompare.isIn("x", null));
              assertFalse(OACompare.isIn(null, "x"));
          }
      }

      @Test
      void isEqualOrInDelegatesToIsIn() {
          Object[] values = { "1", "2" };

          assertEquals(OACompare.isIn(2, values), OACompare.isEqualOrIn(2, values));
      }

  }

