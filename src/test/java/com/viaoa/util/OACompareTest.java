package com.viaoa.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

import com.messagedesigner.model.oa.MessageGroup;
import com.messagedesigner.model.oa.MessageType;
import com.viaoa.OAUnitTest;
import com.viaoa.compare.OAAnyValueObject;
import com.viaoa.compare.OACompare;
import com.viaoa.compare.OAEmptyObject;
import com.viaoa.compare.OANotExist;
import com.viaoa.compare.OANotNullObject;
import com.viaoa.compare.OANullObject;
import com.viaoa.object.OAObjectKey;

public class OACompareTest extends OAUnitTest {

	@Test
	public void isLikeTest() {
		String s = "abcde";

		assertTrue(OACompare.isLike(s, "A*"));
		assertFalse(OACompare.isLike(s, "*A"));
		assertTrue(OACompare.isLike(s, "a*"));
		assertFalse(OACompare.isLike(s, "*a"));

		assertTrue(OACompare.isLike(s, "*Bc*"));
		assertTrue(OACompare.isLike(s, "*aBcde*"));
		assertTrue(OACompare.isLike(s, "*Bcd*"));

		assertTrue(OACompare.isLike(s, "*E"));
		assertFalse(OACompare.isLike(s, "E*"));
		assertTrue(OACompare.isLike(s, "*e"));
		assertFalse(OACompare.isLike(s, "e*"));

		assertTrue(OACompare.isLike(s, "A*E"));
		assertTrue(OACompare.isLike(s, "AB*E"));
		assertTrue(OACompare.isLike(s, "AB*DE"));
		assertTrue(OACompare.isLike(s, "ABc*E"));

		assertTrue(OACompare.isLike(s, "ABC*"));

		assertFalse(OACompare.isLike(null, "A*E"));
		assertFalse(OACompare.isLike(s, null));

	}

	@Test
	public void isEqualTest() {
		String s = "abcde";
		assertTrue(OACompare.isEqual(s, s));
		assertFalse(OACompare.isEqual(s, null));
		assertFalse(OACompare.isEqual(null, s));
		assertTrue(OACompare.isEqual(s, "ABcde", true));
		assertFalse(OACompare.isEqual(s, "ABcde", false));
		assertTrue(OACompare.isEqual(s, "abcde", false));

		assertTrue(OACompare.isEqual(null, null));
		assertTrue(OACompare.isEqual(null, 0));
		assertTrue(OACompare.isEqual(false, 0));

		assertTrue(OACompare.isEqual(0.0D, 0F));
		assertTrue(OACompare.isEqual(1.0D, 1F));
		assertTrue(OACompare.isEqual(0.01D, 0.01F, 2));
		assertTrue(OACompare.isEqual(0.01D, 0.009999F, 2));
		assertTrue(OACompare.isEqual(0.01D, ".010001", 2));
		assertTrue(OACompare.isEqual(0.01D, ".010001", 5));

		assertTrue(OACompare.isEqual(true, 1));
		assertTrue(OACompare.isEqual(true, -1));
		assertFalse(OACompare.isEqual(true, 0));
		assertTrue(OACompare.isEqual(false, 0));
		assertTrue(OACompare.isEqual(true, 't'));
		assertFalse(OACompare.isEqual(false, 'f'));
		assertTrue(OACompare.isEqual(true, "true"));
		assertTrue(OACompare.isEqual("true", true));
		assertTrue(OACompare.isEqual(false, "false"));
		assertTrue(OACompare.isEqual("false", false));
		assertTrue(OACompare.isEqual(true, "fx"));
		assertTrue(OACompare.isEqual("fx", true));
		assertTrue(OACompare.isEqual(false, ""));
		assertTrue(OACompare.isEqual(false, null));
	}

	@Test
	public void miscTest() {
		Object val1 = 222;
		Object val2 = "2*";

		assertTrue(OACompare.isLess(val2, val1));
		assertFalse(OACompare.isLess(val1, val2));

		assertTrue(OACompare.isLike(val1, val2));

		assertFalse(OACompare.isEqualOrLess(val1, val2));
		assertTrue(OACompare.isGreater(val1, val2));
		assertTrue(OACompare.isEqualOrGreater(val1, val2));

		assertFalse(OACompare.isEqualIgnoreCase(val1, val2));
		assertFalse(OACompare.isEqualIgnoreCase(val1, val2));
		assertFalse(OACompare.isEqual(val1, val2));

		val1 = 222;
		val2 = 222;
		assertTrue(OACompare.isEqualOrLess(val1, val2));
		assertFalse(OACompare.isLess(val1, val2));
		assertTrue(OACompare.isEqualOrGreater(val1, val2));
		assertFalse(OACompare.isGreater(val1, val2));

		val1 = 221;
		val2 = 222;
		assertTrue(OACompare.isEqualOrLess(val1, val2));
		assertTrue(OACompare.isLess(val1, val2));
		assertFalse(OACompare.isEqualOrGreater(val1, val2));
		assertFalse(OACompare.isGreater(val1, val2));
		assertTrue(OACompare.isGreater(val2, val1));

		assertTrue(OACompare.isBetween(val1, 0, 999));
		assertFalse(OACompare.isBetween(val1, 0, 5));
		assertFalse(OACompare.isBetween(val1, 999, 9999));
		assertFalse(OACompare.isBetween(val1, 221, 222));

		assertTrue(OACompare.isBetweenOrEqual(val1, 0, 221));
		assertFalse(OACompare.isBetweenOrEqual(val1, 0, 220));
		assertTrue(OACompare.isBetweenOrEqual(val1, 221, 999));
		assertFalse(OACompare.isBetweenOrEqual(val1, 222, 223));

		assertFalse(OACompare.isEmpty("a", true));
		assertTrue(OACompare.isEmpty("", true));
		assertTrue(OACompare.isEmpty(null, true));
		assertTrue(OACompare.isEmpty(0));
		assertFalse(OACompare.isEmpty(-1));
	}

	@Test
	public void testArray() {
		assertEquals(0, OACompare.compare(new String[] {}, false));
		assertEquals(-1, OACompare.compare(new String[] {}, true));
		assertEquals(1, OACompare.compare(new String[] { "z" }, false));
		assertEquals(0, OACompare.compare(new String[] { "z" }, true));

		assertEquals(0, OACompare.compare(false, new String[] {}));
		assertEquals(1, OACompare.compare(true, new String[] {}));
		assertEquals(-1, OACompare.compare(false, new String[] { "z" }));
		assertEquals(0, OACompare.compare(true, new String[] { "z" }));
	}

	@Test
	public void testOAObjectKey() {
		OAObjectKey ok = new OAObjectKey(123);
		assertEquals(0, ok.getGuid());
		assertTrue( Arrays.equals(ok.getObjectIds(), new Object[]{123}));
		
		MessageGroup mg = new MessageGroup();
//		assertTrue(mg.getObjectKey().getGuid() > 0L);
		
		MessageType mt = new MessageType();
//		assertTrue(mt.getObjectKey().getGuid() > 0L);
		mt.setId(11);
		ok = mt.getObjectKey();
		
//		OAObjectPropertyDelegate.setProperty(mg, mg.P_MessageType, ok);
		
		MessageType mtx = mg.getMessageType();
		
//		assertEquals(mt, mtx);
	}
	
	@Test
	public void testCompare() {
		int x;
		
		x = OACompare.compare("abc", "abc");
		assertTrue(x == 0);

		x = OACompare.compare("abc", "abc ");
		assertTrue(x != 0);

		x = OACompare.compare(true, true);
		assertTrue(x == 0);

		x = OACompare.compare(true, false);
		assertTrue(x != 0);

		x = OACompare.compare(true, Boolean.TRUE);
		assertTrue(x == 0);
		

		x = OACompare.compare(null, null);
		assertTrue(x == 0);
		
		x = OACompare.compare(null, "");
		assertTrue(x != 0);
		
		x = OACompare.compare("ab", "ab");
		assertTrue(x == 0);
		
		
		x = OACompare.compare(1,  1, 0);
		assertTrue(x == 0);
		x = OACompare.compare(1,  2, 0);
		assertTrue(x < 0);
		x = OACompare.compare(3,  2, 0);
		assertTrue(x > 0);

		x = OACompare.compare(3,  BigDecimal.valueOf(3));
		assertTrue(x == 0);

		x = OACompare.compare(3,  BigDecimal.valueOf(3));
		assertTrue(x == 0);
		
		x = OACompare.compare(3, 3.000001, 1);
		assertTrue(x == 0);
		
		x = OACompare.compare(3, 3L);
		assertTrue(x == 0);

		x = OACompare.compare((short)3, 3L);
		assertTrue(x == 0);

		double d = 12.345999;
		x = OACompare.compare(d, d);
		assertTrue(x == 0);
		x = OACompare.compare(d, 12.345999d);
		assertTrue(x == 0);
		
		x = OACompare.compare(1.2345,  1.2345111, 4);
		assertTrue(x == 0);
		x = OACompare.compare(1.2345,  1.2345111, 5);
		assertTrue(x < 0);
		x = OACompare.compare(1.23452,  1.23451119, 5);
		assertTrue(x > 0);
		x = OACompare.compare(1.23452,  1.23451119, 4);
		assertTrue(x == 0);
		x = OACompare.compare(1.2345,  1.2344999, 3);
		assertTrue(x > 0);

		x = OACompare.compare('a', OANotExist.instance);
		assertTrue(x != 0);
		x = OACompare.compare(null, OAAnyValueObject.instance);
		assertTrue(x == 0);
		x = OACompare.compare(null, OANullObject.instance);
		assertTrue(x == 0);
		x = OACompare.compare(null, OANotNullObject.instance);
		assertTrue(x != 0);

		x = OACompare.compare(null, OAEmptyObject.instance);
		assertTrue(x == 0);
		x = OACompare.compare("", OAEmptyObject.instance);
		assertTrue(x == 0);
		
		
		MessageType mt = new MessageType();
		mt.setId(777);
		OAObjectKey ok = mt.getObjectKey();
		
		x = OACompare.compare(mt, ok);
		assertTrue(x == 0);
		
		x = OACompare.compare(mt, mt.getId());
		assertTrue(x == 0);
		
		x = OACompare.compare(mt.getObjectKey(), mt.getId());
		assertTrue(x == 0);
		
		
		ok = new OAObjectKey((Object[]) null, mt.getGuid());
		x = OACompare.compare(mt, ok);
		assertTrue(x == 0);
		
		ok = new OAObjectKey(new Object[] { mt.getId() });
		x = OACompare.compare(mt, ok);
		assertTrue(x == 0);
		
		ok = new OAObjectKey(new Object[] { mt.getId() });
		x = OACompare.compare(new Object[] { mt.getId() }, ok);
		assertTrue(x == 0);
		

		UUID guid1 = UUID.randomUUID();
		ok = new OAObjectKey(new Object[] { 123, "abc" }, guid1);
		OAObjectKey ok2 = new OAObjectKey(new Object[] { 123, "abc" }, guid1);
		x = OACompare.compare(ok, ok2);
		assertTrue(x == 0);
		x = ok.compareTo(ok2);
		assertTrue(x == 0);
		assertTrue(ok.equals(ok2));

		ok = new OAObjectKey(new Object[] { 123, "abc" }, guid1);
		ok2 = new OAObjectKey(new Object[] { 678, "abcDEF" }, guid1);
		x = OACompare.compare(ok, ok2); // only checks guids
		assertTrue(x == 0);
		assertFalse(ok.equals(ok2));
		
		

		UUID guid2 = UUID.randomUUID();
		ok = new OAObjectKey(new Object[] { 123, "abc" });
		ok2 = new OAObjectKey(new Object[] { 123, "abc" }, guid2);
		x = OACompare.compare(ok, ok2);
		assertTrue(x == 0);
		x = ok.compareTo(ok2);
		assertTrue(x != 0);
		assertFalse(ok.equals(ok2));
		
		
		UUID guid3 = UUID.randomUUID();
		ok = new OAObjectKey(new Object[] { 123, "abc" }, guid1);
		ok2 = new OAObjectKey(new Object[] { 123, "abc" }, guid3);
		x = OACompare.compare(ok, ok2);
		assertTrue(x != 0);
		x = ok.compareTo(ok2);
		assertTrue(x != 0);
		
		
		x = OACompare.compare(new String[] {"a"}, new String[] {"a"});
		assertTrue(x == 0);
		x = OACompare.compare(new String[] {"a", "b"}, new String[] {"a"});
		assertTrue(x != 0);
		x = OACompare.compare(new String[] {"a"}, new String[] {"a", "b"});
		assertTrue(x != 0);
		
		x = OACompare.compare(new String[] {"a"}, true);
		assertTrue(x == 0);
		
		x = OACompare.compare(true, new String[] {"a"});
		assertTrue(x == 0);
		
		x = OACompare.compare(true, "true");
		assertTrue(x == 0);
		x = OACompare.compare("true", true);
		assertTrue(x == 0);
		
		x = OACompare.compare(123.455, "123.455", 3);
		assertTrue(x == 0);

		x = OACompare.compare(123.459, "123.46", 2);
		assertTrue(x == 0);
		
		x = OACompare.compare(mt, mt);
		assertTrue(x == 0);
		
		
		int xx = 4;
		xx++;
		
	}
/*
	
	public static void main(String[] args) {
		Object val1 = 222;
		Object val2 = "2*";

		boolean b;
		b = isEmpty(null);
		b = isEmpty("");
		b = isEmpty(new String[0]);
		b = isEmpty(false);
		b = isEmpty(true);
		b = isEmpty(0);
		b = isEmpty(0.0);
		b = isEmpty(0.0000001);
		b = isEmpty((char) 0);
		b = isEmpty('a');

		b = isLess(val1, val2);
		b = isLike(val1, val2);
		b = isLess(val1, val2);
		b = isEqualOrLess(val1, val2);
		b = isGreater(val1, val2);
		b = isEqualOrGreater(val1, val2);

		b = isEqualIgnoreCase(val1, val2);
		b = isEqual(val1, val2);

		int xx = 4;
		xx++;
	}
	
*/	
	
}
