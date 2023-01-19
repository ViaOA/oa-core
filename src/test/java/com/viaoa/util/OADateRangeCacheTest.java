package com.viaoa.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.util.OADateRangeCache.DateRange;

public class OADateRangeCacheTest extends OAUnitTest {

	protected class TestObj implements Comparable {
		OADate date;

		@Override
		public int compareTo(Object o) {
			if (!(o instanceof TestObj)) {
				return -1;
			}
			int x = this.date.compare(((TestObj) o).date);
			return x;
		}
	}

	protected void getSampleData(DateRange dr) {
		List<TestObj> al = new ArrayList<>();
		dr.setList(al);

		OADate d = dr.beginDate;
		for (;;) {
			TestObj tox = new TestObj();
			tox.date = d;
			al.add(tox);

			d = (OADate) d.addDay();
			if (d.compare(dr.endDate) > 0) {
				break;
			}
		}
	}

	@Test
	public void test() {
		OADateRangeCache<TestObj> c = new OADateRangeCache<TestObj>() {
			@Override
			protected OADate getDate(TestObj obj) {
				return obj.date;
			}
		};

		DateRange<TestObj> dr;
		List<TestObj> al;

		dr = new DateRange<TestObj>(new OADate("2022-09-01"), new OADate("2023-01-01"));
		for (DateRange<TestObj> drx : c.findMissingGaps(dr)) {
			getSampleData(drx);
			c.add(drx);
		}

		dr = new DateRange<TestObj>(new OADate("2022-07-01"), new OADate("2022-12-14"));
		for (DateRange<TestObj> drx : c.findMissingGaps(dr)) {
			getSampleData(drx);
			c.add(drx);
		}

		dr = new DateRange<TestObj>(new OADate("2022-07-29"), new OADate("2022-08-22"));
		for (DateRange<TestObj> drx : c.findMissingGaps(dr)) {
			getSampleData(drx);
			c.add(drx);
		}

		dr = new DateRange((OADate) (new OADate("2022-07-01")).subtractDay(), new OADate("2023-01-02"));
		for (DateRange<TestObj> drx : c.findMissingGaps(dr)) {
			getSampleData(drx);
			c.add(drx);
		}

		dr = new DateRange((OADate) (new OADate("2022-07-01")).subtractDay(), new OADate("2023-01-01"));
		assertEquals(0, c.findMissingGaps(dr).size());

		dr = new DateRange<TestObj>(new OADate("2022-07-03"), new OADate("2022-11-14"));
		al = c.getCacheItems(dr.beginDate, dr.endDate);
		Collections.sort(al);

		OADate d = dr.beginDate;

		int cnt = 0;
		for (TestObj obj : al) {
			// System.out.printf("%03d) %s   %s\n", cnt++, d, obj.date);
			if (!obj.date.equals(d)) {
				assertEquals(d, obj.date);
			}
			d = (OADate) d.addDay();
		}

		c.clearCache();
		al = c.getCacheItems(dr.beginDate, dr.endDate);
		assertEquals(0, al.size());
	}

	@Test
	public void testGaps() {
		OADateRangeCache<TestObj> c = new OADateRangeCache<TestObj>() {
			@Override
			protected OADate getDate(TestObj obj) {
				return obj.date;
			}
		};

		DateRange<TestObj> dr;
		List<TestObj> al;

		dr = new DateRange<TestObj>(new OADate("2022-01-01"), new OADate("2022-01-31"));
		c.add(dr);

		dr = new DateRange<TestObj>(new OADate("2022-11-01"), new OADate("2022-12-31"));
		c.add(dr);

		dr = new DateRange<TestObj>(new OADate("2022-01-01"), new OADate("2022-12-31"));
		List<DateRange<TestObj>> alt = c.findMissingGaps(dr);
		assertEquals(1, alt.size());

		DateRange<TestObj> drx = alt.get(0);
		assertEquals(new OADate("2022-02-01"), drx.beginDate);
		assertEquals(new OADate("2022-10-31"), drx.endDate);
	}

	public static void main(String[] args) throws Exception {
		OADateRangeCacheTest test = new OADateRangeCacheTest();
		test.test();
		test.testGaps();
	}
}
