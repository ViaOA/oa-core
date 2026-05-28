package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class OAQueryFilterCompoundInContractTest {

    public static class Line {
        private String orderId;
        private Integer itemId;

        public Line(String orderId, Integer itemId) {
            this.orderId = orderId;
            this.itemId = itemId;
        }

        public String getOrderId() {
            return orderId;
        }

        public Integer getItemId() {
            return itemId;
        }
    }

    @Test
    void compoundInLiteralTuplesMatchBothPropertiesIfSupported() {
        OAQueryFilter<Line> f = new OAQueryFilter<>(
            Line.class,
            "(orderId, itemId) IN (('A', 1), ('B', 2))"
        );

        assertTrue(f.isUsed(new Line("A", 1)));
        assertTrue(f.isUsed(new Line("B", 2)));
        assertFalse(f.isUsed(new Line("A", 2)));
        assertFalse(f.isUsed(new Line("C", 1)));
    }

    @Test
    void compoundInPlaceholderListCurrentContract() {
        List<Object[]> tuples = Arrays.asList(
            new Object[] { "A", 1 },
            new Object[] { "B", 2 }
        );

        OAQueryFilter<Line> f = new OAQueryFilter<>(
            Line.class,
            "(orderId, itemId) IN (?)",
            new Object[] { tuples }
        );

        assertTrue(f.isUsed(new Line("A", 1)));
        assertTrue(f.isUsed(new Line("B", 2)));
        assertFalse(f.isUsed(new Line("A", 2)));
    }

    @Test
    void compoundInMalformedTupleThrowsAtConstruction() {
        assertThrows(RuntimeException.class, () ->
            new OAQueryFilter<>(Line.class, "(orderId, itemId) IN (('A'), ('B', 2))")
        );
    }
}
