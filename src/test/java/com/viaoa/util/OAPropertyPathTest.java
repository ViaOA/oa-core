package com.viaoa.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.object.OALinkInfo;

import test.hifive.model.oa.Program;
import test.hifive.model.oa.propertypath.ProgramPP;

public class OAPropertyPathTest extends OAUnitTest {

	@Test
	public void test() {

	}

	@Test
	public void test1() {
		init();

		String spp = ProgramPP.locations().employees().pp;
		OAPropertyPath<Program> pp = new OAPropertyPath<>(Program.class, spp);

		OALinkInfo[] lis = pp.getRecursiveLinkInfos();
		assertEquals(2, lis.length);
		assertNotNull(lis[0]);
		assertNull(lis[1]);
		assertTrue(pp.getEndLinkInfo() != null);
	}

	@Test
	public void test2() {
		String spp = ProgramPP.locations().employees().lastName();
		OAPropertyPath<Program> pp = new OAPropertyPath<>(Program.class, spp);

		assertNull(pp.getEndLinkInfo());

	}

	@Test
	public void test3() {
		OAPropertyPath<Program> pp = new OAPropertyPath<>(Program.class, ".");
		Program p = new Program();
		Object px = pp.getValue(p);
		assertEquals(p, px);
		int xx = 4;
		xx++;

	}

}
