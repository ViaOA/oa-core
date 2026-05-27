package com.viaoa.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;

class OAConverterFormatTest {

    private String dateFormat;
    private String timeFormat;
    private String dateTimeFormat;
    private String integerFormat;
    private String decimalFormat;
    private String bigDecimalFormat;
    private String moneyFormat;
    private String booleanFormat;

    @BeforeEach
    void rememberFormats() {
        dateFormat = OAConverter.getDateFormat();
        timeFormat = OAConverter.getTimeFormat();
        dateTimeFormat = OAConverter.getDateTimeFormat();
        integerFormat = OAConverter.getIntegerFormat();
        decimalFormat = OAConverter.getDecimalFormat();
        bigDecimalFormat = OAConverter.getBigDecimalFormat();
        moneyFormat = OAConverter.getMoneyFormat();
        booleanFormat = OAConverter.getBooleanFormat();
    }

    @AfterEach
    void restoreFormats() {
        OAConverter.setDateFormat(dateFormat);
        OAConverter.setTimeFormat(timeFormat);
        OAConverter.setDateTimeFormat(dateTimeFormat);
        OAConverter.setIntegerFormat(integerFormat);
        OAConverter.setDecimalFormat(decimalFormat);
        OAConverter.setBigDecimalFormat(bigDecimalFormat);
        OAConverter.setMoneyFormat(moneyFormat);
        OAConverter.setBooleanFormat(booleanFormat);
    }

    @Test
    void getFormatReturnsConfiguredDefaultsForKnownTypes() {
        OAConverter.setDateFormat("yyyy/MM/dd");
        OAConverter.setTimeFormat("HH-mm-ss");
        OAConverter.setDateTimeFormat("yyyy/MM/dd HH-mm-ss");
        OAConverter.setIntegerFormat("0000");
        OAConverter.setDecimalFormat("0.000");
        OAConverter.setBigDecimalFormat("0.0000");
        OAConverter.setBooleanFormat("yes;no;unknown");

        assertEquals("0.0000", OAConverter.getFormat(BigDecimal.class));
        assertEquals("0000", OAConverter.getFormat(Integer.class));
        assertEquals("0000", OAConverter.getFormat(int.class));
        assertEquals("0000", OAConverter.getFormat(Long.class));
        assertEquals("0000", OAConverter.getFormat(long.class));
        assertEquals("0.000", OAConverter.getFormat(Double.class));
        assertEquals("0.000", OAConverter.getFormat(double.class));
        assertEquals("0.000", OAConverter.getFormat(Float.class));
        assertEquals("yyyy/MM/dd", OAConverter.getFormat(java.util.Date.class));
        assertEquals("HH-mm-ss", OAConverter.getFormat(java.sql.Time.class));
        assertEquals("yyyy/MM/dd HH-mm-ss", OAConverter.getFormat(java.sql.Timestamp.class));
        assertEquals("yyyy/MM/dd", OAConverter.getFormat(OADate.class));
        assertEquals("HH-mm-ss", OAConverter.getFormat(OATime.class));
        assertEquals("yyyy/MM/dd HH-mm-ss", OAConverter.getFormat(OADateTime.class));
        assertEquals("yes;no;unknown", OAConverter.getFormat(Boolean.class));
        assertEquals("yes;no;unknown", OAConverter.getFormat(boolean.class));
        assertEquals("yyyy/MM/dd", OAConverter.getFormat(LocalDate.class));
        assertEquals("HH-mm-ss", OAConverter.getFormat(LocalTime.class));
        assertEquals("yyyy/MM/dd HH-mm-ss", OAConverter.getFormat(LocalDateTime.class));
        assertEquals("yyyy/MM/dd HH-mm-ss", OAConverter.getFormat(ZonedDateTime.class));
        assertEquals("yyyy/MM/dd HH-mm-ss", OAConverter.getFormat(Instant.class));
        assertNull(OAConverter.getFormat(ZoneId.class));
        assertNull(OAConverter.getFormat(String.class));
        assertNull(OAConverter.getFormat(null));
    }

    @Test
    void explicitFormatOverridesDefaultFormat() {
        OAConverter.setIntegerFormat("0000");

        assertEquals("0007", OAConverter.toString(7, true));
        assertEquals("7", OAConverter.toString(7, "#"));
    }

    @Test
    void toStringObjectTrueUsesConfiguredDefaultFormat() {
        OAConverter.setBigDecimalFormat("0.00");
        OAConverter.setBooleanFormat("Y;N;?");
        OAConverter.setDateFormat("yyyy/MM/dd");

        assertEquals("12.30", OAConverter.toString(new BigDecimal("12.3"), true));
        assertEquals("Y", OAConverter.toString(Boolean.TRUE, true));
        assertEquals("2024/05/06", OAConverter.toString(new OADate(2024, 4, 6), true));
    }

    @Test
    void formatGetterSetterRoundTrips() {
        OAConverter.setDateFormat("d1");
        OAConverter.setTimeFormat("t1");
        OAConverter.setDateTimeFormat("dt1");
        OAConverter.setIntegerFormat("i1");
        OAConverter.setDecimalFormat("dec1");
        OAConverter.setBigDecimalFormat("bd1");
        OAConverter.setMoneyFormat("m1");
        OAConverter.setBooleanFormat("b1;b2;b3");

        assertEquals("d1", OAConverter.getDateFormat());
        assertEquals("t1", OAConverter.getTimeFormat());
        assertEquals("dt1", OAConverter.getDateTimeFormat());
        assertEquals("i1", OAConverter.getIntegerFormat());
        assertEquals("dec1", OAConverter.getDecimalFormat());
        assertEquals("bd1", OAConverter.getBigDecimalFormat());
        assertEquals("m1", OAConverter.getMoneyFormat());
        assertEquals("m1", OAConverter.getCurrencyFormat());
        assertEquals("b1;b2;b3", OAConverter.getBooleanFormat());
    }

    @Test
    void nullFormatResetRestoresFallbackWhereSupported() {
        OAConverter.setIntegerFormat("0000");
        OAConverter.setDecimalFormat("0.000");
        OAConverter.setBigDecimalFormat("0.0000");
        OAConverter.setMoneyFormat("money");
        OAConverter.setBooleanFormat("Y;N;?");

        OAConverter.setIntegerFormat(null);
        OAConverter.setDecimalFormat(null);
        OAConverter.setBigDecimalFormat(null);
        OAConverter.setMoneyFormat(null);
        OAConverter.setBooleanFormat(null);

        assertNull(OAConverter.getIntegerFormat());
        assertNull(OAConverter.getDecimalFormat());
        assertNull(OAConverter.getBigDecimalFormat());
        assertNull(OAConverter.getMoneyFormat());
        assertNull(OAConverter.getCurrencyFormat());
        assertNull(OAConverter.getBooleanFormat());
    }
}
