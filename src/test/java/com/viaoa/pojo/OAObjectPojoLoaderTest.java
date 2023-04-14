package com.viaoa.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.auto.dev.reportercorp.model.oa.PypeReportMessage;
import com.auto.dev.reportercorp.model.oa.ReportInfo;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAPropertyInfo;

/**
 * Verify that OAObject POJO information is correctly loaded.
 * <p>
 * see com.viaoao.pojo.*
 */
public class OAObjectPojoLoaderTest {

	@Test
	public void reportInstanceProcessTest() throws Exception {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(ReportInstanceProcess.class);
		Pojo pojo = oi.getPojo();

		assertFalse(oi.getNoPojo());
		assertFalse(oi.getPojoSingleton());

		OAPropertyInfo pi = oi.getPropertyInfo(ReportInstanceProcess.P_Id);
		assertTrue(pi.getNoPojo());
		assertEquals(0, pi.getPojoKeyPos());

		assertNull(PojoDelegate.getPojoProperty(pojo, "id"));

		List<PojoProperty> al = PojoDelegate.getPojoPropertyKeys(pojo);
		assertEquals(1, al.size());
		PojoProperty pp = al.get(0);
		assertEquals(1, pp.getKeyPos());
		assertEquals("counter", pp.getName());

		assertFalse(PojoDelegate.hasCompoundKey(pojo));
		assertFalse(PojoDelegate.hasPkey(oi));
		assertFalse(PojoDelegate.hasImportMatchKey(oi));
		assertTrue(PojoDelegate.hasLinkUniqueKey(oi));

		PojoLinkOne plo = PojoLinkOneDelegate.getPojoLinkOne(pojo, ReportInstanceProcess.P_ThreadInfo);
		assertEquals(0, PojoLinkOneDelegate.getLinkFkeyPojoProperties(plo).size());
		assertEquals(0, PojoLinkOneDelegate.getImportMatchPojoProperties(plo).size());
		assertEquals(1, PojoLinkOneDelegate.getLinkUniquePojoProperties(plo).size());

		pp = PojoLinkOneDelegate.getLinkUniquePojoProperties(plo).get(0);
		assertEquals("threadInfoName", pp.getName());
		assertEquals("threadInfo.name", pp.getPropertyPath());

		plo = PojoLinkOneDelegate.getPojoLinkOne(pojo, ReportInstanceProcess.P_StoreInfo);
		assertEquals(0, PojoLinkOneDelegate.getLinkFkeyPojoProperties(plo).size());
		assertEquals(0, PojoLinkOneDelegate.getImportMatchPojoProperties(plo).size());
		assertEquals(1, PojoLinkOneDelegate.getLinkUniquePojoProperties(plo).size());

		pp = PojoLinkOneDelegate.getLinkUniquePojoProperties(plo).get(0);
		assertEquals("storeInfoStoreNumber", pp.getName());
		assertEquals("storeInfo.storeNumber", pp.getPropertyPath());

		plo = PojoLinkOneDelegate.getPojoLinkOne(pojo, ReportInstanceProcess.P_ReportInfo);
		assertEquals(0, PojoLinkOneDelegate.getLinkFkeyPojoProperties(plo).size());
		assertEquals(0, PojoLinkOneDelegate.getImportMatchPojoProperties(plo).size());
		assertEquals(1, PojoLinkOneDelegate.getLinkUniquePojoProperties(plo).size());

		pp = PojoLinkOneDelegate.getLinkUniquePojoProperties(plo).get(0);
		assertEquals("reportId", pp.getName());
		assertEquals("reportInfo.report.id", pp.getPropertyPath());

		plo = PojoLinkOneDelegate.getPojoLinkOne(pojo, ReportInstanceProcess.P_PypeReportMessage);
		assertEquals(1, PojoLinkOneDelegate.getLinkFkeyPojoProperties(plo).size());
		assertEquals(2, PojoLinkOneDelegate.getImportMatchPojoProperties(plo).size());
		assertEquals(0, PojoLinkOneDelegate.getLinkUniquePojoProperties(plo).size());

		pp = PojoLinkOneDelegate.getLinkFkeyPojoProperties(plo).get(0);
		assertEquals("pypeReportMessageId", pp.getName());
		assertEquals("pypeReportMessage.id", pp.getPropertyPath());

		plo = PojoLinkOneDelegate.getPojoLinkOne(pojo, ReportInstanceProcess.P_StatusInfo);
		assertEquals(0, PojoLinkOneDelegate.getLinkFkeyPojoProperties(plo).size());
		assertEquals(0, PojoLinkOneDelegate.getImportMatchPojoProperties(plo).size());
		assertEquals(0, PojoLinkOneDelegate.getLinkUniquePojoProperties(plo).size());
	}

	@Test
	public void reporterCorpTest() throws Exception {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(ReporterCorp.class);
		Pojo pojo = oi.getPojo();

		assertFalse(oi.getNoPojo());
		assertTrue(oi.getPojoSingleton());

		OAPropertyInfo pi = oi.getPropertyInfo(ReporterCorp.P_Id);
		assertTrue(pi.getNoPojo());
		assertEquals(0, pi.getPojoKeyPos());

		assertNull(PojoDelegate.getPojoProperty(pojo, "id"));

		List<PojoProperty> al = PojoDelegate.getPojoPropertyKeys(pojo);
		assertEquals(0, al.size());

		assertFalse(PojoDelegate.hasCompoundKey(pojo));
		assertFalse(PojoDelegate.hasPkey(oi));
		assertFalse(PojoDelegate.hasImportMatchKey(oi));
		assertFalse(PojoDelegate.hasLinkUniqueKey(oi));
	}

	@Test
	public void pypeReportMessageTest() throws Exception {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(PypeReportMessage.class);
		Pojo pojo = oi.getPojo();

		assertFalse(oi.getNoPojo());
		assertFalse(oi.getPojoSingleton());

		OAPropertyInfo pi = oi.getPropertyInfo(PypeReportMessage.P_Id);
		assertFalse(pi.getNoPojo());
		assertEquals(1, pi.getPojoKeyPos());

		assertNotNull(PojoDelegate.getPojoProperty(pojo, "id"));

		List<PojoProperty> al = PojoDelegate.getPojoPropertyKeys(pojo);
		assertEquals(1, al.size());
		PojoProperty pp = al.get(0);
		assertEquals(1, pp.getKeyPos());
		assertEquals("id", pp.getName());

		assertFalse(PojoDelegate.hasCompoundKey(pojo));
		assertTrue(PojoDelegate.hasPkey(oi));
		assertFalse(PojoDelegate.hasImportMatchKey(oi));
		assertFalse(PojoDelegate.hasLinkUniqueKey(oi));
	}

	@Test
	public void reportInfoTest() throws Exception {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(ReportInfo.class);
		Pojo pojo = oi.getPojo();

		assertFalse(oi.getNoPojo());
		assertFalse(oi.getPojoSingleton());

		OAPropertyInfo pi = oi.getPropertyInfo(PypeReportMessage.P_Id);
		assertTrue(pi.getNoPojo());
		assertEquals(0, pi.getPojoKeyPos());

		assertNull(PojoDelegate.getPojoProperty(pojo, "id"));

		PojoProperty pp = PojoDelegate.getPojoProperty(pojo, "reportId");

		assertEquals(1, pp.getKeyPos());
		assertEquals("report.id", pp.getPropertyPath());

		assertFalse(PojoDelegate.hasCompoundKey(pojo));
		assertFalse(PojoDelegate.hasPkey(oi));
		assertFalse(PojoDelegate.hasImportMatchKey(oi));
		assertTrue(PojoDelegate.hasLinkUniqueKey(oi));
	}
}
