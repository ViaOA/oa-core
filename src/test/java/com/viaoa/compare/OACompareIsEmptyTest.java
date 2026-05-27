  package com.viaoa.compare;

  import static org.junit.jupiter.api.Assertions.*;

  import java.math.BigDecimal;
  import java.util.Collections;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Nested;
  import org.junit.jupiter.api.Test;

  class OACompareIsEmptyTest {

      @Nested
      @DisplayName("isEmpty")
      class IsEmpty {

          @Test
          void nullAndEmptyStringsAreEmpty() {
              assertTrue(OACompare.isEmpty(null));
              assertTrue(OACompare.isEmpty(""));
              assertFalse(OACompare.isEmpty(" "));
              assertTrue(OACompare.isEmpty(" ", true));
              assertFalse(OACompare.isEmpty("x"));
          }

          @Test
          void emptyCollectionsMapsAndArraysAreEmpty() {
              assertTrue(OACompare.isEmpty(Collections.emptyList()));
              assertTrue(OACompare.isEmpty(Collections.emptyMap()));
              assertTrue(OACompare.isEmpty(new Object[0]));
              assertTrue(OACompare.isEmpty(new int[0]));

              assertFalse(OACompare.isEmpty(List.of("x")));
              assertFalse(OACompare.isEmpty(Map.of("x", 1)));
              assertFalse(OACompare.isEmpty(new Object[] { "x" }));
              assertFalse(OACompare.isEmpty(new int[] { 1 }));
          }

          @Test
          void primitiveWrapperDefaultsAreEmpty() {
              assertTrue(OACompare.isEmpty(0));
              assertTrue(OACompare.isEmpty(0L));
              assertTrue(OACompare.isEmpty(0.0d));
              assertTrue(OACompare.isEmpty(-0.0d));
              assertTrue(OACompare.isEmpty(false));
              assertTrue(OACompare.isEmpty(Character.valueOf('\0')));

              assertFalse(OACompare.isEmpty(1));
              assertFalse(OACompare.isEmpty(-1));
              assertFalse(OACompare.isEmpty(0.1d));
              assertFalse(OACompare.isEmpty(true));
              assertFalse(OACompare.isEmpty(Character.valueOf('x')));
          }

          @Test
          void currentContractVerySmallBigDecimalMayBeNonEmptyWhenDoubleValueIsNonZero() {
              assertFalse(OACompare.isEmpty(new BigDecimal("1e-10")));
          }

          @Test
          void verySmallBigDecimalThatWouldUnderflowToDoubleZeroIsNotEmpty() {
              assertFalse(OACompare.isEmpty(new BigDecimal("1e-400")));
          }
      }

      @Nested
      @DisplayName("isNotEmpty")
      class IsNotEmpty {

          @Test
          void isNotEmptyIsInverseOfIsEmptyForRepresentativeValues() {
              Object[] values = {
                      null,
                      "",
                      " ",
                      "x",
                      0,
                      1,
                      false,
                      true,
                      new Object[0],
                      new Object[] { "x" },
                      Collections.emptyList(),
                      List.of("x"),
                      Collections.emptyMap(),
                      Map.of("x", 1)
              };

              for (Object value : values) {
                  assertEquals(!OACompare.isEmpty(value), OACompare.isNotEmpty(value),
                          () -> "isNotEmpty should be inverse of isEmpty for " + value);
              }
          }

          @Test
          void trimAwareIsNotEmptyUsesTrimAwareIsEmpty() {
              assertFalse(OACompare.isNotEmpty(" ", true));
              assertTrue(OACompare.isNotEmpty(" ", false));
          }
      }

      @Test
      void mutableMapWithNullValueIsNotEmpty() {
          Map<String, Object> map = new HashMap<>();
          map.put("key", null);
          assertFalse(OACompare.isEmpty(map));
          assertTrue(OACompare.isNotEmpty(map));
      }

  }

