package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.annotation.OAClass;
import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;
import com.viaoa.lang.oa.VBoolean;
import com.viaoa.lang.oa.VDate;
import com.viaoa.lang.oa.VDateTime;
import com.viaoa.lang.oa.VDouble;
import com.viaoa.lang.oa.VEnum;
import com.viaoa.lang.oa.VInteger;
import com.viaoa.lang.oa.VLong;
import com.viaoa.lang.oa.VNameValue;
import com.viaoa.lang.oa.VString;
import com.viaoa.lang.oa.VTime;

class OALangValueTypesExtraTest {

    @Test
    void stringAndPrimitiveValueTypesRoundTripValues() {
        VString string = new VString("alpha");
        assertEquals("alpha", string.getValue());
        string.setValue(null);
        assertNull(string.getValue());

        VBoolean bool = new VBoolean(true);
        assertTrue(bool.getValue());
        bool.setValue(false);
        assertFalse(bool.getValue());

        VDouble dbl = new VDouble();
        dbl.setValue(1.25d);
        assertEquals(1.25d, dbl.getValue());

        VLong lng = new VLong();
        lng.setValue(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, lng.getValue());
    }

    @Test
    void integerArithmeticHelpersMutateValueAndCurrentlyWrapOnOverflow() {
        VInteger value = new VInteger(1);

        value.inc();
        assertEquals(2, value.getValue());
        value.add(5);
        assertEquals(7, value.getValue());
        value.sub(3);
        assertEquals(4, value.getValue());
        value.dec();
        assertEquals(3, value.getValue());

        value.setValue(Integer.MAX_VALUE);
        value.inc();
        assertEquals(Integer.MIN_VALUE, value.getValue());
    }

    @Test
    void dateAndTimeValueTypesRoundTripValuesAndAllowNull() {
        OADate date = new OADate("2026-05-27");
        OADateTime dateTime = new OADateTime("2026-05-27 08:30:00");
        OATime time = new OATime("08:30:00");

        VDate vDate = new VDate();
        vDate.setValue(date);
        assertSame(date, vDate.getValue());
        vDate.setValue(null);
        assertNull(vDate.getValue());

        VDateTime vDateTime = new VDateTime();
        vDateTime.setValue(dateTime);
        assertSame(dateTime, vDateTime.getValue());

        VTime vTime = new VTime();
        vTime.setValue(time);
        assertSame(time, vTime.getValue());
    }

    @Test
    void enumValueTypeRoundTripsNameDisplayAndValue() {
        VEnum value = new VEnum();

        value.setName("Ready");
        value.setDisplay("Ready to run");
        value.setValue(42);

        assertEquals("Ready", value.getName());
        assertEquals("Ready to run", value.getDisplay());
        assertEquals(42, value.getValue());
    }

    @Test
    void nameValueRoundTripsNameAndValueIncludingNulls() {
        VNameValue value = new VNameValue();

        value.setName("server");
        value.setValue("alpha");
        assertEquals("server", value.getName());
        assertEquals("alpha", value.getValue());

        value.setName(null);
        value.setValue(null);
        assertNull(value.getName());
        assertNull(value.getValue());
    }

    @Test
    void valueTypesCurrentlyUseIdentityEquality() {
        assertNotEquals(new VString("same"), new VString("same"));
        assertNotEquals(new VInteger(7), new VInteger(7));
    }

    @Test
    void vBooleanMetadataCurrentlyAdvertisesInteger() {
        OAClass annotation = VBoolean.class.getAnnotation(OAClass.class);

        assertNotNull(annotation);
        assertEquals("int", annotation.shortName());
        assertEquals("Integer", annotation.displayName());
    }

    @Test
    void valueTypesUseMovedLangOaPackage() {
        assertEquals("com.viaoa.lang.oa", VString.class.getPackageName());
        assertEquals("com.viaoa.lang.oa", VInteger.class.getPackageName());
        assertEquals("com.viaoa.lang.oa", VEnum.class.getPackageName());
    }
}
