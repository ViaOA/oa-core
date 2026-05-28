package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.viaoa.annotation.OAProperty;
import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathFormatAndConversionBoundaryTest {

    public static class Root extends OAObject {
        private double amount = 1234.567;
        private int count = 7;
        private OADate date = new OADate(2026, 4, 27);
        private OADateTime dateTime = new OADateTime(2026, 4, 27, 8, 9, 10);
        private LocalDate localDate = LocalDate.of(2026, 5, 27);
        private LocalDateTime localDateTime = LocalDateTime.of(2026, 5, 27, 8, 9, 10);

        @OAProperty(decimalPlaces = 3)
        public double getAmount() {
            return amount;
        }

        @OAProperty(format = "000")
        public int getCount() {
            return count;
        }

        @OAProperty(format = "yyyy-MM-dd")
        public OADate getDate() {
            return date;
        }

        @OAProperty(format = "yyyy-MM-dd HH:mm:ss")
        public OADateTime getDateTime() {
            return dateTime;
        }

        @OAProperty(format = "yyyy-MM-dd")
        public LocalDate getLocalDate() {
            return localDate;
        }

        @OAProperty(format = "yyyy-MM-dd HH:mm:ss")
        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }
    }

    @Test
    void decimalPlacesFormatIsAppliedToValueAsString() {
        Root root = new Root();
        OAPath<Root> pp = new OAPath<>(Root.class, "amount");

        assertEquals("1234.567", pp.getValueAsString(root));
    }

    @Test
    void explicitFormatOverridesAnnotationFormatForNumber() {
        Root root = new Root();
        OAPath<Root> pp = new OAPath<>(Root.class, "amount");

        assertEquals("1234.6", pp.getValueAsString(null, root, "0.0"));
    }

    @Test
    void integerAnnotationFormatIsApplied() {
        Root root = new Root();
        OAPath<Root> pp = new OAPath<>(Root.class, "count");

        assertEquals("007", pp.getValueAsString(root));
    }

    @Test
    void dateAnnotationFormatIsApplied() {
        Root root = new Root();

        assertEquals("2026-05-27", new OAPath<Root>(Root.class, "date").getValueAsString(root));
        assertEquals("2026-05-27 08:09:10", new OAPath<Root>(Root.class, "dateTime").getValueAsString(root));
    }

    @Test
    void javaTimeAnnotationFormatIsAppliedIfSupported() {
        Root root = new Root();

        assertEquals("2026-05-27", new OAPath<Root>(Root.class, "localDate").getValueAsString(root));
        assertEquals("2026-05-27 08:09:10", new OAPath<Root>(Root.class, "localDateTime").getValueAsString(root));
    }

    @Test
    void nullFormattedValueReturnsEmptyStringOrNullByConverterContract() {
        class NullRoot extends OAObject {
            public String getName() { return null; }
        }

        OAPath<NullRoot> pp = new OAPath<>(NullRoot.class, "name");

        assertEquals("", pp.getValueAsString(new NullRoot()));
    }
}
