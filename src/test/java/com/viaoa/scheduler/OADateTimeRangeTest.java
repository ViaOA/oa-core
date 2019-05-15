package com.viaoa.scheduler;

import org.junit.Test;

import com.viaoa.util.OADateTime;

import static org.junit.Assert.*;

public class OADateTimeRangeTest {

    
    @Test
    public void testGetter() {
        OADateTime dt1 = new OADateTime();
        OADateTime dt2 = new OADateTime().addDays(3);
        
        OADateTimeRange dtr = new OADateTimeRange(dt1, dt2, null);
        assertEquals(dtr.getBegin(), dt1);
        assertEquals(dtr.getEnd(), dt2);
        assertNull(dtr.getReference());

        OADateTimeRange<String> dtr2 = new OADateTimeRange(dt1, dt2, "Hey");
        assertEquals(dtr2.getBegin(), dt1);
        assertEquals(dtr2.getEnd(), dt2);
        assertEquals("Hey", dtr2.getReference());
    }
    
    @Test
    public void testEquals() {
        OADateTime dt1 = new OADateTime();
        OADateTime dt2 = new OADateTime().addSeconds(3);
        
        OADateTimeRange dtr1 = new OADateTimeRange(dt1, dt2, null);
        OADateTimeRange dtr2 = new OADateTimeRange(dt1, dt2, null);
        assertEquals(dtr1, dtr2);
        
        dt2 = new OADateTime().addDays(1);;
        dtr2 = new OADateTimeRange(dt1, dt2, null);
        assertNotEquals(dtr1, dtr2);
    }

    @Test
    public void testCompareTo() {
        OADateTime dt1 = new OADateTime();
        OADateTime dt2 = new OADateTime();
        OADateTimeRange dtr1 = new OADateTimeRange(dt1, dt2, null);
        OADateTimeRange dtr2 = new OADateTimeRange(dt1, dt2, "hey");
        assertEquals(0, dtr1.compareTo(dtr2));

        OADateTime dt3 = new OADateTime().addSeconds(3);
        OADateTime dt4 = new OADateTime().addSeconds(4);
        dtr2 = new OADateTimeRange(dt3, dt4, "xx");
        assertTrue(dtr1.compareTo(dtr2) < 0);
        assertTrue(dtr2.compareTo(dtr1) > 0);
        assertTrue(dtr1.compareTo(dtr1) == 0);
        assertTrue(dtr2.compareTo(dtr2) == 0);
    }
    
    @Test
    public void testChildren() {
        OADateTime dt1 = new OADateTime();
        OADateTime dt2 = new OADateTime();
        OADateTimeRange dtr = new OADateTimeRange(dt1, dt2, null);
        
        for (int i=0; i<10; i++) {
            assertEquals(dtr.getChildren().size(), i);
            dtr.addChild(new OADateTimeRange(new OADateTime(), new OADateTime(), null));
        }
    }
    
}

