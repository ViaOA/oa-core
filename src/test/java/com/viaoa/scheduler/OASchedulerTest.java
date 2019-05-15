package com.viaoa.scheduler;

import org.junit.Test;

import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

import static org.junit.Assert.*;

public class OASchedulerTest {

    @Test
    public void test() {
        OADateTime dt1 = new OADateTime();
        OADateTime dt2 = new OADateTime().addDays(3);

        OASchedule sch = new OASchedule<>();
        assertFalse(sch.isEndOfList());
        
        OADateTimeRange dtr = sch.next();
        assertNull(dtr);
        assertTrue(sch.isEndOfList());
        
        sch.add(dt1, dt2, null);
        assertTrue(sch.isEndOfList());
        
        sch.reset();
        assertFalse(sch.isEndOfList());
        
        dtr = sch.next();
        assertNotNull(dtr);
        assertEquals(dt1, dtr.getBegin());
        assertEquals(dt2, dtr.getEnd());
        assertFalse(sch.isEndOfList());

        dtr = sch.next();
        assertNull(dtr);
    }
    
    @Test
    public void testAdd() {
        OADateTime dt1 = new OADateTime();
        OADateTime dt2 = new OADateTime().addDays(3);

        OASchedule sch = new OASchedule<>();
        assertFalse(sch.isEndOfList());
        sch.add(dt1, dt2, null);
        
        assertEquals(1, sch.size());
        
        dt1 = new OADateTime().addDays(4);
        dt2 = new OADateTime().addDays(7);
        sch.add(dt1, dt2, null);
        assertEquals(2, sch.size());
    }
    
    @Test
    public void testAddJoinExtendA() {
        OADateTime dt1 = new OADateTime();
        OADateTime dt2 = new OADateTime().addDays(3);

        OASchedule sch = new OASchedule<>();
        assertFalse(sch.isEndOfList());
        sch.add(dt1, dt2, null);
        assertEquals(1, sch.size());
        
        OADateTime dt3 = dt2.addDays(1);
        sch.add(dt1, dt3, null);
        assertEquals(1, sch.size());
        
        OADateTimeRange dtr = sch.next();
        assertNotNull(dtr);
        assertFalse(sch.isEndOfList());
        
        assertEquals(dtr.getBegin(), dt1);
        assertEquals(dtr.getEnd(), dt3);
    }
    
    @Test
    public void testAddJoinExtendA2() {
        OADateTime dt1 = new OADateTime();
        OADateTime dt2 = new OADateTime().addDays(3);

        OASchedule sch = new OASchedule<>();
        sch.add(dt1, dt2, null);
        
        OADateTime dt3 = dt1.addDays(1);
        OADateTime dt4 = dt2.addDays(1);
        
        sch.add(dt3, dt4, null);
        assertEquals(1, sch.size());
        
        OADateTimeRange dtr = sch.next();
        assertNotNull(dtr);
        assertFalse(sch.isEndOfList());
        
        assertEquals(dtr.getBegin(), dt1);
        assertEquals(dtr.getEnd(), dt4);
    }

    @Test
    public void testAddJoinExtendB() {
        OADateTime dt1 = new OADateTime().addDays(1);
        OADateTime dt2 = new OADateTime().addDays(4);

        OASchedule sch = new OASchedule<>();
        sch.add(dt1, dt2, null);
        assertEquals(1, sch.size());
        
        OADateTime dt3 = new OADateTime();
        OADateTime dt4 = new OADateTime().addDays(2);
        
        sch.add(dt3, dt4, null);
        assertEquals(1, sch.size());
        
        OADateTimeRange dtr = sch.next();
        assertNotNull(dtr);
        assertFalse(sch.isEndOfList());
        
        assertEquals(dtr.getBegin(), dt3);
        assertEquals(dtr.getEnd(), dt2);
    }
    
    @Test
    public void testClear() {
        OADateTime dt1 = new OADateTime();
        OADateTime dt2 = new OADateTime().addDays(3);

        OASchedule sch = new OASchedule<>();
        assertFalse(sch.isEndOfList());
        sch.add(dt1, dt2, null);
        
        assertEquals(1, sch.size());
        
        sch.clear();
        assertEquals(0, sch.size());
    }


    
    @Test
    public void testAdd_NoDtr1OrDtr2( ) {
        OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        OADateTime dtA2 = new OADateTime("01/01/2019 10:00am");
        
        OASchedule<String> sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        
        assertEquals(1, sch.size());
        
        OADateTimeRange<String> dtr = sch.next();
        assertEquals("A", dtr.getReference());
    }
    
    @Test
    public void testAdd_NoDtr2_A() {
        OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        OADateTime dtA2 = new OADateTime("01/01/2019 10:00am");
        
        OASchedule<String> sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        assertEquals(1, sch.size());
        
        OADateTimeRange<String> dtr = sch.next();
        assertNotNull(dtr);
        assertEquals(dtr.getBegin(), dtA1);
        assertEquals(dtr.getEnd(), dtA2);
        assertEquals(dtr.getReference(), "A");

        OADateTime dtB1 = new OADateTime("01/01/2019 11:00am");
        OADateTime dtB2 = new OADateTime("01/01/2019 12:00pm");
        sch.add(dtB1, dtB2, "B");
        assertEquals(2, sch.size());

        sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        dtB1 = new OADateTime("01/01/2019 10:01am");
        dtB2 = new OADateTime("01/01/2019 12:00pm");
        sch.add(dtB1, dtB2, "B");
        assertEquals(2, sch.size());
    }

    @Test
    public void testAdd_NoDtr2_B() {
        final OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        final OADateTime dtA2 = new OADateTime("01/01/2019 10:00am");
        
        OASchedule<String> sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        assertEquals(1, sch.size());
        
        OADateTimeRange<String> dtr = sch.next();
        assertNotNull(dtr);
        assertEquals(dtr.getBegin(), dtA1);
        assertEquals(dtr.getEnd(), dtA2);
        assertEquals(dtr.getReference(), "A");

        // equal
        OADateTime dtB1 = new OADateTime("01/01/2019 6:00am");
        OADateTime dtB2 = new OADateTime("01/01/2019 10:00am");
        sch.add(dtB1, dtB2, "B");
        assertEquals(1, sch.size());
        assertEquals(1, dtr.getChildren().size());
        assertEquals("B", dtr.getChildren().get(0).getReference());

        // consume dtrNew
        sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        dtB1 = new OADateTime("01/01/2019 6:31am");
        dtB2 = new OADateTime("01/01/2019 9:00am");
        sch.add(dtB1, dtB2, "B");
        assertEquals(1, sch.size());
        dtr = sch.next();
        assertEquals("B", dtr.getChildren().get(0).getReference());
        
        // consume dtr1
        sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        dtB1 = new OADateTime("01/01/2019 6:00am");
        dtB2 = new OADateTime("01/01/2019 11:00am");
        sch.add(dtB1, dtB2, "B");
        assertEquals(1, sch.size());
        dtr = sch.next();
        assertEquals("B", dtr.getReference());
        assertEquals("A", dtr.getChildren().get(0).getReference());
        
        // overlap
        sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        dtB1 = new OADateTime("01/01/2019 10:00am");
        dtB2 = new OADateTime("01/01/2019 11:00am");
        sch.add(dtB1, dtB2, "B");
        assertEquals(1, sch.size());
        dtr = sch.next();
        assertNull(dtr.getReference());
        assertEquals(2, dtr.getChildren().size());
        dtr = sch.next();
        assertNull(dtr);
        
        // overlap
        sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        dtB1 = new OADateTime("01/01/2019 9:59am");
        dtB2 = new OADateTime("01/01/2019 10:00am");
        sch.add(dtB1, dtB2, "B");
        assertEquals(1, sch.size());
        dtr = sch.next();
        assertEquals("A", dtr.getReference());
        assertEquals(1, dtr.getChildren().size());
        assertEquals("B", dtr.getChildren().get(0).getReference());
        dtr = sch.next();
        assertNull(dtr);

        sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        dtB1 = new OADateTime("01/01/2019 9:59am");
        dtB2 = new OADateTime("01/01/2019 10:01am");
        sch.add(dtB1, dtB2, "B");
        assertEquals(1, sch.size());
        dtr = sch.next();
        assertNull(dtr.getReference());
        assertEquals(2, dtr.getChildren().size());
        assertEquals("A", dtr.getChildren().get(0).getReference());
        assertEquals("B", dtr.getChildren().get(1).getReference());
        dtr = sch.next();
        assertNull(dtr);
    }
    
    @Test
    public void testAdd_NoDtr1( ) {
        final OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        final OADateTime dtA2 = new OADateTime("01/01/2019 10:00am");
        OADateTime dtB1;
        OADateTime dtB2;
        OADateTimeRange<String> dtr;
        
        OASchedule<String> sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");

        // add
        dtB1 = new OADateTime("01/01/2019 4:00am");
        dtB2 = new OADateTime("01/01/2019 5:00am");
        sch.add(dtB1, dtB2, "B");
        assertEquals(2, sch.size());
        dtr = sch.next();
        
        // dtr2 consume dtrNew
        sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        dtB1 = new OADateTime("01/01/2019 6:00am");
        dtB2 = new OADateTime("01/01/2019 8:00am");
        sch.add(dtB1, dtB2, "B");
        assertEquals(1, sch.size());
        dtr = sch.next();
        assertEquals("A", dtr.getReference());
        assertEquals(1, dtr.getChildren().size());
        assertEquals("B", dtr.getChildren().get(0).getReference());

        // dtrNew consume dtr2
        sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        dtB1 = new OADateTime("01/01/2019 6:00am");
        dtB2 = new OADateTime("01/01/2019 8:00pm");
        sch.add(dtB1, dtB2, "B");
        assertEquals(1, sch.size());
        dtr = sch.next();
        assertEquals(1, dtr.getChildren().size());
        assertEquals("A", dtr.getChildren().get(0).getReference());

        // combines
        sch = new OASchedule<>();
        sch.add(dtA1, dtA2, "A");
        dtB1 = new OADateTime("01/01/2019 5:59am");
        dtB2 = new OADateTime("01/01/2019 8:00am");
        sch.add(dtB1, dtB2, "B");
        assertEquals(1, sch.size());
        dtr = sch.next();
        assertEquals(2, dtr.getChildren().size());
        assertEquals("B", dtr.getChildren().get(0).getReference());
        assertEquals("A", dtr.getChildren().get(1).getReference());
    }
    
    @Test
    public void testAdd_both1and2( ) {
        OASchedule<String> sch = new OASchedule<>();
        final OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        final OADateTime dtA2 = new OADateTime("01/01/2019 10:00am");

        final OADateTime dtB1 = new OADateTime("01/01/2019 11:00am");
        final OADateTime dtB2 = new OADateTime("01/01/2019 12:00pm");

        OADateTime dtC1;
        OADateTime dtC2;
        OADateTimeRange dtr;
        
        sch.add(dtA1, dtA2, "A");
        assertEquals(1, sch.size());
        
        sch.add(dtB1, dtB2, "B");
        assertEquals(2, sch.size());

        // combine into one
        dtC1 = new OADateTime("01/01/2019 10:00am");
        dtC2 = new OADateTime("01/01/2019 11:00am");
        sch.add(dtC1, dtC2, "C");
        assertEquals(1, sch.size());
        
        dtr = sch.next();
        assertEquals(dtA1, dtr.getBegin());
        assertEquals(dtB2, dtr.getEnd());
        
        dtr = sch.next();
        assertNull(dtr);
    }
    
    @Test
    public void testAdd_multiple_dtr( ) {
        OASchedule<String> sch = createSch();
        assertEquals(10, sch.getSize());
        
        OADateTime dt1 = new OADateTime().addDays(-1);
        OADateTime dt2 = dt1.addDays(15);
        sch.add(dt1, dt2, "Z");
        assertEquals(1, sch.getSize());
        
        sch = createSch();
        dt1 = new OADateTime().addDays(-2);
        dt2 = dt1.addDays(1);
        sch.add(dt1, dt2, "Z");
        assertEquals(11, sch.getSize());

        sch = createSch();
        dt1 = new OADateTime().addDays(12);
        dt2 = dt1.addDays(13);
        sch.add(dt1, dt2, "Z");
        assertEquals(11, sch.getSize());
        
        
        sch = createSch();
        dt1 = new OADateTime().addDays(2).addMinutes(3);
        dt2 = dt1.addDays(5);
        sch.add(dt1, dt2, "Z");
        assertTrue(sch.getSize() > 3);
    }
    private OASchedule<String> createSch() {
        OASchedule<String> sch = new OASchedule<>();
        for (int i=0; i<10; i++) {
            OADateTime dt1 = new OADateTime().addDays(i);
            OADateTime dt2 = dt1.addMinutes(5);
            sch.add(dt1, dt2, ('A'+i)+"");
        }
        return sch;
    }
    

    @Test
    public void nextEmptyTest( ) {
        OASchedule<String> sch = new OASchedule<>();
        final OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        final OADateTime dtA2 = new OADateTime("01/01/2019 10:00am");

        final OADateTime dtB1 = new OADateTime("01/01/2019 11:00am");
        final OADateTime dtB2 = new OADateTime("01/01/2019 12:00pm");

        sch.add(dtA1, dtA2, "a");
        sch.add(dtB1, dtB2, "b");
        
        OADateTimeRange dtr = sch.nextEmpty();
        assertNotNull(dtr);
        assertNull(dtr.getBegin());
        assertEquals(dtA1, dtr.getEnd());
        
        dtr = sch.nextEmpty();        
        assertNotNull(dtr);
        assertEquals(dtr.getBegin(), dtA2);
        assertEquals(dtr.getEnd(), dtB1);
        
        dtr = sch.nextEmpty();        
        assertNotNull(dtr);
        assertEquals(dtr.getBegin(), dtB2);
        assertEquals(dtr.getEnd(), null);
        
        dtr = sch.nextEmpty();        
        assertNull(dtr);
    }

    @Test
    public void clearTest() {
        OASchedule<String> sch = new OASchedule<>();
        final OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        final OADateTime dtA2 = new OADateTime("01/01/2019 10:00am");

        final OADateTime dtB1 = new OADateTime("01/01/2019 11:00am");
        final OADateTime dtB2 = new OADateTime("01/01/2019 12:00pm");

        sch.add(dtA1, dtA2, "a");
        sch.add(dtB1, dtB2, "b");

        final OADateTime dtC1 = new OADateTime("01/01/2019 9:30am");
        final OADateTime dtC2 = new OADateTime("01/01/2019 11:30am");

        sch.clear(dtC1, dtC2);
        
        assertEquals(2, sch.size());
        OADateTimeRange dtr = sch.next();
        assertNull(dtr.getReference());
        assertEquals(dtA1, dtr.getBegin());
        assertEquals(dtC1, dtr.getEnd());
        
        dtr = sch.next();
        assertNull(dtr.getReference());
        assertEquals(dtC2, dtr.getBegin());
        assertEquals(dtB2, dtr.getEnd());
        
        dtr = sch.next();
        assertNull(dtr);
    }
    
    @Test
    public void clearTest2() {
        OASchedule<String> sch = new OASchedule<>();
        final OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        final OADateTime dtA2 = new OADateTime("01/01/2019 11:00am");

        final OADateTime dtB1 = new OADateTime("01/01/2019 10:45am");
        final OADateTime dtB2 = new OADateTime("01/01/2019 12:00pm");

        sch.add(dtA1, dtA2, "a");
        sch.add(dtB1, dtB2, "b");
        assertEquals(1, sch.size());

        final OADateTime dtC1 = new OADateTime("01/01/2019 9:30am");
        final OADateTime dtC2 = new OADateTime("01/01/2019 11:30am");

        sch.clear(dtC1, dtC2);
        
        assertEquals(2, sch.size());
        OADateTimeRange dtr = sch.next();
        assertNull(dtr.getReference());
        assertEquals(dtA1, dtr.getBegin());
        assertEquals(dtC1, dtr.getEnd());
        
        dtr = sch.next();
        assertNull(dtr.getReference());
        assertEquals(dtC2, dtr.getBegin());
        assertEquals(dtB2, dtr.getEnd());
        
        dtr = sch.next();
        assertNull(dtr);
    }

    @Test
    public void clearTest3() {
        OASchedule<String> sch = new OASchedule<>();
        final OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        final OADateTime dtA2 = new OADateTime("01/01/2019 11:00am");

        final OADateTime dtB1 = new OADateTime("01/01/2019 10:45am");
        final OADateTime dtB2 = new OADateTime("01/01/2019 12:00pm");

        sch.add(dtA1, dtA2, "a");
        sch.add(dtB1, dtB2, "b");
        assertEquals(1, sch.size());

        final OADateTime dtC1 = new OADateTime("01/01/2019 5:30am");
        final OADateTime dtC2 = new OADateTime("01/01/2019 11:30am");

        sch.clear(dtC1, dtC2);
        
        assertEquals(1, sch.size());
        OADateTimeRange dtr = sch.next();
        assertNull(dtr.getReference());
        assertEquals(dtC2, dtr.getBegin());
        assertEquals(dtB2, dtr.getEnd());
        
        dtr = sch.next();
        assertNull(dtr);
    }

    @Test
    public void clearTest4() {
        OASchedule<String> sch = new OASchedule<>();
        final OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        final OADateTime dtA2 = new OADateTime("01/01/2019 11:00am");

        final OADateTime dtB1 = new OADateTime("01/01/2019 11:05am");
        final OADateTime dtB2 = new OADateTime("01/01/2019 12:00pm");

        sch.add(dtB1, dtB2, "b");
        sch.add(dtA1, dtA2, "a");
        assertEquals(2, sch.size());

        final OADateTime dtC1 = new OADateTime("01/01/2019 6:05am");
        final OADateTime dtC2 = new OADateTime("01/01/2019 11:03am");

        sch.clear(dtC1, dtC2);
        assertEquals(2, sch.size());

        OADateTimeRange dtr = sch.next();
        assertNull(dtr.getReference());
        assertEquals(dtA1, dtr.getBegin());
        assertEquals(dtC1, dtr.getEnd());

        dtr = sch.next();
        assertEquals(dtB1, dtr.getBegin());
        assertEquals(dtB2, dtr.getEnd());
        
        dtr = sch.next();
        assertNull(dtr);
    }

    @Test
    public void clearTest5() {
        OASchedule<String> sch = new OASchedule<>();
        final OADateTime dtA1 = new OADateTime("01/01/2019 6:00am");
        final OADateTime dtA2 = new OADateTime("01/01/2019 11:00am");

        final OADateTime dtB1 = new OADateTime("01/01/2019 11:05am");
        final OADateTime dtB2 = new OADateTime("01/01/2019 12:00pm");

        sch.add(dtB1, dtB2, "b");
        sch.add(dtA1, dtA2, "a");
        assertEquals(2, sch.size());

        final OADateTime dtC1 = new OADateTime("01/01/2019 5:05am");
        final OADateTime dtC2 = new OADateTime("01/01/2019 12:03pm");

        sch.clear(dtC1, dtC2);
        assertEquals(0, sch.size());

        OADateTimeRange dtr = sch.next();
        assertNull(dtr);
    }

    @Test
    public void allTest() {
        OASchedule<String> sch = new OASchedule();

        // mark day as used
        sch.add(new OADate("01/01/2019"), new OADate("01/02/2019"), "today");
        
        // open office hours
        final OADateTime dtA1 = new OADateTime("01/01/2019 8:00am");
        final OADateTime dtA2 = new OADateTime("01/01/2019 5:00pm");
        sch.clear(dtA1, dtA2);

        OADateTimeRange dtr = sch.next();
        assertNotNull(dtr);
        assertEquals(new OADate("01/01/2019"), dtr.getBegin());
        assertEquals(dtA1, dtr.getEnd());

        dtr = sch.next();
        assertNotNull(dtr);
        assertEquals(dtA2, dtr.getBegin());

        dtr = sch.next();
        assertNull(dtr);
        
        // employee
        final OADateTime dtB1 = new OADateTime("01/01/2019 9:00am");
        final OADateTime dtB2 = new OADateTime("01/01/2019 2:00pm");
        
        final OADateTime dtC1 = new OADateTime("01/01/2019 12:00pm");
        final OADateTime dtC2 = new OADateTime("01/01/2019 12:30pm");
        
        
        
        sch.add(new OADate("01/01/2019"), dtB1, "");
        sch.add(dtB2, new OADate("01/02/2019"), "");

        int xx = 4;
        xx++;
    }

}

