package com.viaoa.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.viaoa.OAUnitTest;

public class OAMathTest extends OAUnitTest {
	
	@Test
	public void roundTest() {

		double d = 1.2345678;
		double d2;

		double dx = OAMath.round(d, 2);
		assertTrue(1.23 == dx);

		d = 1.23;
		dx = OAMath.round(d, 2);
		assertTrue(1.23 == dx);

		d = 1.239;
		dx = OAMath.round(d, 2);
		assertTrue(1.24 == dx);

		d = 1.235;
		dx = OAMath.round(d, 2);
		assertTrue(1.24 == dx);

		d = 1.2351;
		dx = OAMath.round(d, 2);
		assertTrue(1.24 == dx);

		d = 1.2349999;
		dx = OAMath.round(d, 2);
		assertTrue(1.23 == dx);

		d = .9999;
		dx = OAMath.round(d, 2);
		assertTrue(1.0 == dx);

		d = 1.2345;
		d2 = 1.23456666;

/*		
		assertTrue(OAMath.compare(d, d2, 0) == 0);
		assertTrue(OAMath.compare(d, d2, 3) == 0);
		assertTrue(OAMath.compare(d, d2, 4) < 0);
		d2 = 1.23454666;
		assertTrue(OAMath.compare(d, d2, 4) == 0);
*/
		d = 39.424;
		d = OAMath.round(d, 2);
		assertTrue(39.42 == d);

		d = 39.426;
		d = OAMath.round(d, 2);
		assertTrue(39.43 == d);

		d = 39.4251;
		d = OAMath.round(d, 2);
		assertTrue(39.43 == d);

		d = 39.425;
		d = OAMath.round(d, 2);
		assertTrue(39.43 == d);

		d = 48.5475;
		d = OAMath.round(d, 2);
		assertTrue(48.55 == d);

		d = 970.95 * .05;
		d = OAMath.round(d, 2);
		assertTrue(48.55 == d);

		d = 0.0;
		for (int i = 0; i < 10000; i++) {
			d += .01;
		}
		dx = OAMath.round(d, 2);
		assertTrue(100.0 == dx);
	}

	@Test
	public void roundTest2() {
		double d;
		double d2;

		double dx;

		// this is know to not work when using Math.round(d * 100)/100.0 ... result would be 1.02 instead of 1.03;
		d = 1.025;
		dx = OAMath.round(d, 2);
		assertTrue(1.03 == dx);

		dx = OAMath.round(d, 2);
		assertTrue(1.03 == dx);

		d = 1.024999999;
		dx = OAMath.round(d, 2);
		assertTrue(1.02 == dx);

		dx = OAMath.round(d, 3);
		assertTrue(1.025 == dx);

		dx = OAMath.round(d, 4);
		assertTrue(1.0250 == dx);
	}

	@Test
	public void addTest() {
		double d = 1.2345678;
		double d2, dx;

		assertEquals((int) OAMath.add(1, 5), 6);

		dx = OAMath.add(1.1, 5);
		assertTrue(dx == 6.1);
		assertFalse(dx == 6.100000001);

		dx = OAMath.add(1.1, 5, 0);
		assertTrue(dx == 6.0);

		dx = OAMath.add(1.1, 5, 1);
		assertTrue(dx == 6.1);

		dx = OAMath.add(1.1, 5, 2);
		assertTrue(dx == 6.1);

		dx = OAMath.add(1.100499, 5.005, 2);
		assertTrue(dx == 6.11);

		dx = OAMath.add(1.100499, 5.005, 3);
		assertTrue(dx == 6.105);
	}

	@Test
	public void subtractTest() {
		double d = 1.2345678;
		double d2, dx;

		dx = OAMath.subtract(5.0, 1.0, 3);
		assertTrue(dx == 4.0);

		dx = OAMath.subtract(5.999, 1.000, 2);
		assertTrue(dx == 5.0);

		dx = OAMath.subtract(5.499, 1.000, 1);
		assertTrue(dx == 4.5);
	}
	
/*	
	public static void main(String[] args) {

		double d = 0.0;
		for (int i = 0; i < 1000; i++) {
			d += .01;
			d = round(d, 2);
		}

		double dx = round(d, 2);

		dx = 256.025;
		/ *
		dx = round(dx, 3, 2);

		dx = 1.025;

		dx *= 100.0;
		dx /= 100.0;
		* /

		// .025 is a problem
		dx = .025;
		dx += 1.0; // 1.025
		dx *= 100.0; // 102.499999..
		dx = round(dx, 2, 0);

		dx = 1.024999999;
		dx = round(dx, 3, 2);

		dx = 1.024999999;
		dx = round(dx, 3, 2, BigDecimal.ROUND_HALF_UP);

		dx = 1.024999999;
		dx = round(dx, 3, 2); // <<<< this is wrong ... should be 1.3

		dx = 1.024999999;
		dx = round(dx, 2, 2);

		dx = 1.02500000001;
		dx = round(dx, 3, 2);

		dx = 1.02500000001;
		dx = round(dx, 2, 2);

		dx = 1.64999999;
		dx = round(dx, 4, 2, BigDecimal.ROUND_HALF_UP);

		double newNum = Math.floor(256.025 * 100 + 0.5) / 100;
		newNum = Math.rint(256.025 * 100) / 100;

		dx = 256.025;
		dx = Math.round(dx * 100) / 100.0d;

		dx = 2560.25;
		dx = Math.round(dx * 10) / 10.0d;

		Object objx = OAMath.convert(int.class, new OADateTime());

		objx = OAMath.convert(long.class, new OADateTime());

		objx = OAMath.convert(String.class, new OADateTime());

		objx = OAMath.convert(OADate.class, new OADateTime());

		objx = OAMath.convert(OADateTime.class, new OADateTime());

		objx = OAMath.convert(boolean.class, new OADateTime());

		// -4.1 => -5.0
		d = -4.5;

		d = OAMath.divide(1.0, 3.0, 4);

		System.out.println("Math.round " + d + " == " + Math.round(d));

		double d2 = round(d, 0, 0, BigDecimal.ROUND_HALF_UP);
		System.out.println("BigDecimal " + d + " == " + d2);

		d = OAConverter.round(d, 0);
		System.out.println("new round " + d + " == " + d);
	}
	
	
*/	
	
}
