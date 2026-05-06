package com.viaoa.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.path.OAPath;

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
		OAPath<Program> pp = new OAPath<>(Program.class, spp);

		OALinkInfo[] lis = pp.getRecursiveLinkInfos();
		assertEquals(2, lis.length);
		assertNotNull(lis[0]);
		assertNull(lis[1]);
		assertTrue(pp.getEndLinkInfo() != null);
	}

	@Test
	public void test2() {
		String spp = ProgramPP.locations().employees().lastName();
		OAPath<Program> pp = new OAPath<>(Program.class, spp);

		assertNull(pp.getEndLinkInfo());

	}

	@Test
	public void test3() {
		OAPath<Program> pp = new OAPath<>(Program.class, ".");
		Program p = new Program();
		Object px = pp.getValue(p);
		assertEquals(p, px);
		int xx = 4;
		xx++;

	}

}

// use some of these examples for previous javadoc ??
/**
 * Utility used to parse a propertyPath, get methods, class information, and to be able to get the value by invoking on an object. A
 * PropertyPath String is separated by "." for each linkPropery, and each linkProperty can have a filter in the format ":filterName(a,b,n)"
 * Supports casting in property path, ex: from Emp, "dept.(manager)employee.name" ex: from OALeftJoin "(Location)A.name" Supports filters:
 * ex: "dept.employees:newHires(7).orders.orderItems:overDue(30)" Recursive: created 20120809
 *
 * @param <F> type of object that the property path is based on.
 * @see HubMerger which uses propertyPaths to create a Hub of all lastNode objects, and keeps it updated.
 * @see OAPropertyPathDelegate
 */
