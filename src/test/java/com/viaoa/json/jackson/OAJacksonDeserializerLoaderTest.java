package com.viaoa.json.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.auto.dev.reportercorp.model.oa.ProcessStep;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportInfo;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessorInfo;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.auto.dev.reportercorp.model.oa.StoreInfo;
import com.auto.dev.reportercorp.model.oa.ThreadInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viaoa.datasource.autonumber.OADataSourceAuto;
import com.viaoa.json.OAJson;
import com.viaoa.json.OAJson.StackItem;
import com.viaoa.json.jackson.OAJacksonDeserializerLoader.EqualQueryForObject;
import com.viaoa.json.jackson.OAJacksonDeserializerLoader.EqualQueryForReference;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.pojo.PojoLinkOneDelegate;

// see OAJacksonDeserializerLoaderTestPojo.java for list of test*.json files that were created from Pojo classes.

/**
 * Latest unit tests
 */
public class OAJacksonDeserializerLoaderTest {

	private OAJson oaj;
	private OADataSourceAuto dsAuto;

	@Before
	public void before() {
		oaj = new OAJson();
		OAObjectCacheDelegate.setUnitTestMode(true);

		dsAuto = new OADataSourceAuto();
		dsAuto.setAssignIdOnCreate(true);
	}

	@After
	public void after() {
		OAObjectCacheDelegate.setUnitTestMode(false);
	}

	private List<Report> alRpt = new ArrayList();

	protected void reset() throws Exception {
		oaj.setStackItem(null);
		oaj.setReadingPojo(true);
		// loader = new OAJacksonDeserializerLoader(oaj);
		OAObjectCacheDelegate.resetCache();

		dsAuto.getHub().clear();

		// List<Report> alRpt = new ArrayList();
		alRpt.clear();
		for (int id : Arrays.asList(1, 4)) {
			Report rpt = new Report();
			rpt.setId(id);
			rpt.save();
			alRpt.add(rpt);
		}

		ProcessStep pt = new ProcessStep();
		pt.setStep(2);
	}

	// test1.json   reporterCorp->reportInstanceProcessInfo->reportInstanceProcesses
	@Test
	public void test1() throws Exception {
		reset();

		ObjectMapper om = oaj.getObjectMapper();

		InputStream is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test1.json");
		JsonNode node = oaj.readTree(is);

		StackItem si = new StackItem();
		si.oi = OAObjectInfoDelegate.getOAObjectInfo(ReporterCorp.class);
		si.node = node;

		OAJacksonDeserializerLoader loader = new OAJacksonDeserializerLoader(oaj);
		ReporterCorp rc = loader.load(node, null, ReporterCorp.class);

		verifyAfterLoad(rc);
	}

	@Test
	public void test2() throws Exception {
		reset();

		ObjectMapper om = oaj.getObjectMapper();

		InputStream is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test2.json");
		JsonNode node = oaj.readTree(is);

		StackItem si = new StackItem();
		si.oi = OAObjectInfoDelegate.getOAObjectInfo(ReporterCorp.class);
		si.node = node;

		OAJacksonDeserializerLoader loader = new OAJacksonDeserializerLoader(oaj);
		ReporterCorp rc = loader.load(node, null, ReporterCorp.class);

		verifyAfterLoad(rc);
	}

	@Test
	public void test3() throws Exception {
		reset();

		ObjectMapper om = oaj.getObjectMapper();

		InputStream is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test3.json");
		JsonNode node = oaj.readTree(is);

		StackItem si = new StackItem();
		si.oi = OAObjectInfoDelegate.getOAObjectInfo(ReporterCorp.class);
		si.node = node;

		OAJacksonDeserializerLoader loader = new OAJacksonDeserializerLoader(oaj);
		ReporterCorp rc = loader.load(node, null, ReporterCorp.class);

		verifyAfterLoad(rc);
	}

	@Test
	public void test4() throws Exception {
		reset();

		ObjectMapper om = oaj.getObjectMapper();

		InputStream is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test4.json");
		JsonNode node = oaj.readTree(is);

		StackItem si = new StackItem();
		si.oi = OAObjectInfoDelegate.getOAObjectInfo(ReporterCorp.class);
		si.node = node;

		OAJacksonDeserializerLoader loader = new OAJacksonDeserializerLoader(oaj);
		ReporterCorp rc = loader.load(node, null, ReporterCorp.class);

		verifyAfterLoad(rc);
	}

	protected void verifyAfterLoad(final ReporterCorp rc) throws Exception {
		assertNotNull(rc);
		assertTrue(rc.getId() > 0);

		assertEquals(2, rc.getStoreInfos().size());
		assertEquals(2, rc.getThreadInfos().size());

		ReportInstanceProcessorInfo ripi = rc.getReportInstanceProcessorInfo();
		assertNotNull(ripi);
		assertEquals(3, ripi.getReportInstanceProcesses().size());

		assertEquals(2, ripi.getReportInfos().size());

		int cnt = 0;
		for (StoreInfo six : rc.getStoreInfos()) {
			cnt += six.getReportInstanceProcesses().size();
		}
		assertEquals(3, cnt);

		cnt = 0;
		for (ThreadInfo tix : rc.getThreadInfos()) {
			cnt += tix.getReportInstanceProcesses().size();
		}
		assertEquals(3, cnt);

		int x = rc.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getReportInstanceProcessSteps().get(0)
				.getProcessStep().getStep();
		assertEquals(2, x);

		cnt = 0;
		for (ReportInfo rix : ripi.getReportInfos()) {
			cnt += rix.getReportInstanceProcesses().size();
			assertNotNull(rix.getReport());
		}
		assertEquals(3, cnt);
	}

	//qqqqqqqqqqq need to create a list of unfound reference objects, that could not just use oaobjKey
	//qqqq need to retry at end and allow user access
	// qqqq might want an option to autocreate the references, which could cascade/recurse into hier of new objects,etc

	@Test
	public void test5() throws Exception {
		reset();

		ObjectMapper om = oaj.getObjectMapper();

		InputStream is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test5.json");
		ReporterCorp rcx = oaj.readObject(is, ReporterCorp.class, false);

		assertEquals(2, rcx.getReportInstanceProcessorInfo().getReportInfos().size());

		for (ReportInfo ri : rcx.getReportInstanceProcessorInfo().getReportInfos()) {
			assertNotNull(ri.getReport());
		}
	}

	@Test
	public void test6() throws Exception {
		reset();

		ObjectMapper om = oaj.getObjectMapper();

		InputStream is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		ReporterCorp rcx = oaj.readObject(is, ReporterCorp.class, false);

		is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		JsonNode nodeRoot = oaj.readTree(is);

		OAJacksonDeserializerLoader loader = new OAJacksonDeserializerLoader(oaj);
		rcx = loader.load(nodeRoot, null, ReporterCorp.class);

		// =========

		StackItem stackItem = new StackItem();
		final StackItem stackItemRoot = stackItem;

		stackItem.oi = OAObjectInfoDelegate.getOAObjectInfo(ReporterCorp.class);
		stackItem.node = nodeRoot;

		loader.findExistingObject(stackItem);
		assertNull(stackItem.obj);

		loader.createObject(stackItem);
		assertNotNull(stackItem.obj);
		loader.loadObjectIdProperties(stackItem);

		loader.loadObjectProperties(stackItem);

		StackItem stackItemChild = new StackItem();
		stackItemChild.parent = stackItem;
		stackItemChild.li = stackItem.oi.getLinkInfo(ReporterCorp.P_ReportInstanceProcessorInfo);
		stackItemChild.oi = OAObjectInfoDelegate.getOAObjectInfo(stackItemChild.li.getToClass());
		stackItemChild.node = stackItem.node.get(stackItemChild.li.getLowerName());
		assertNotNull(stackItemChild.node);

		loader.findExistingObject(stackItemChild);
		assertNotNull(stackItemChild.obj);

		stackItem = stackItemChild;

		// reportInstanceProcess
		stackItemChild = new StackItem();
		stackItemChild.parent = stackItem;
		stackItemChild.li = stackItem.oi.getLinkInfo(ReportInstanceProcessorInfo.P_ReportInstanceProcesses);
		stackItemChild.oi = OAObjectInfoDelegate.getOAObjectInfo(stackItemChild.li.getToClass());
		stackItemChild.node = stackItem.node.get(stackItemChild.li.getLowerName()).get(0);

		loader._findExistingObjectFromPojo(stackItemChild);
		assertNull(stackItemChild.obj);

		ReportInstanceProcess rip = new ReportInstanceProcess();
		rip.setCounter(0);

		// find it in storeInfo
		StoreInfo si = new StoreInfo();
		si.getReportInstanceProcesses().add(rip);
		((ReporterCorp) stackItemRoot.obj).getStoreInfos().add(si);

		loader._findExistingObjectFromPojo(stackItemChild);
		assertEquals(rip, stackItemChild.obj);

		// find it in threadInfo
		stackItemChild.obj = null;
		((ReporterCorp) stackItemRoot.obj).getStoreInfos().clear();
		loader._findExistingObjectFromPojo(stackItemChild);
		assertNull(stackItemChild.obj);

		ThreadInfo ti = new ThreadInfo();
		ti.getReportInstanceProcesses().add(rip);
		((ReporterCorp) stackItemRoot.obj).getThreadInfos().add(ti);

		loader._findExistingObjectFromPojo(stackItemChild);
		assertEquals(rip, stackItemChild.obj);

		// find in reportInfo
		stackItemChild.obj = null;
		((ReporterCorp) stackItemRoot.obj).getThreadInfos().clear();
		loader._findExistingObjectFromPojo(stackItemChild);
		assertNull(stackItemChild.obj);

		ReportInfo ri = new ReportInfo();
		ri.getReportInstanceProcesses().add(rip);
		((ReporterCorp) stackItemRoot.obj).getReportInstanceProcessorInfo().getReportInstanceProcesses().add(rip);
		loader._findExistingObjectFromPojo(stackItemChild);
		assertEquals(rip, stackItemChild.obj);
	}

	@Test
	public void test6b() throws Exception {
		reset();

		ObjectMapper om = oaj.getObjectMapper();

		InputStream is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		ReporterCorp rcx = oaj.readObject(is, ReporterCorp.class, false);

		assertEquals(1, rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().size());

		assertNull(rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getStoreInfo());
		assertNull(rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getThreadInfo());

		StoreInfo si = new StoreInfo();
		si.setStoreNumber(9000);
		rcx.getStoreInfos().add(si);

		is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		oaj.readIntoObject(is, rcx);
		assertNotNull(rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getStoreInfo());
		assertEquals(si, rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getStoreInfo());
		assertNull(rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getThreadInfo());

		ThreadInfo ti = new ThreadInfo();
		ti.setName("name.0");
		rcx.getThreadInfos().add(ti);

		is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		oaj.readIntoObject(is, rcx);
		assertNotNull(rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getThreadInfo());
		assertEquals(ti, rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getThreadInfo());
		assertNotNull(rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getStoreInfo());

		Report report = OAObjectCacheDelegate.get(Report.class, 1);

		ReportInfo ri = new ReportInfo();
		ri.setReport(report);
		rcx.getReportInstanceProcessorInfo().getReportInfos().add(ri);

		is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		oaj.readIntoObject(is, rcx);
		assertNotNull(rcx.getReportInstanceProcessorInfo().getReportInfos().get(0));

		assertEquals(ri, rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getReportInfo());
		assertEquals(si, rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getStoreInfo());
		assertEquals(ti, rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().get(0).getThreadInfo());

	}

	@Test
	public void getEqualQueryForObjectTest() throws Exception {
		reset();

		ObjectMapper om = oaj.getObjectMapper();

		InputStream is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		ReporterCorp rcx = oaj.readObject(is, ReporterCorp.class, false);

		is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		JsonNode nodeRoot = oaj.readTree(is);

		OAJacksonDeserializerLoader loader = new OAJacksonDeserializerLoader(oaj);
		rcx = loader.load(nodeRoot, null, ReporterCorp.class);

		// ReporterCorp.ReportInstanceProcessorInfo.ReportInstanceProcesses

		StackItem stackItem = new StackItem();
		final StackItem stackItemRoot = stackItem;
		stackItemRoot.obj = rcx;
		stackItem.oi = OAObjectInfoDelegate.getOAObjectInfo(ReporterCorp.class);
		stackItem.node = nodeRoot;

		StackItem stackItemChild = new StackItem();
		stackItemChild.parent = stackItem;
		stackItemChild.li = stackItem.oi.getLinkInfo(ReporterCorp.P_ReportInstanceProcessorInfo);
		stackItemChild.oi = OAObjectInfoDelegate.getOAObjectInfo(stackItemChild.li.getToClass());
		stackItemChild.node = stackItem.node.get(stackItemChild.li.getLowerName());
		stackItemChild.obj = rcx.getReportInstanceProcessorInfo();

		stackItem = stackItemChild;

		stackItemChild = new StackItem();
		stackItemChild.parent = stackItem;
		stackItemChild.li = stackItem.oi.getLinkInfo(ReportInstanceProcessorInfo.P_ReportInstanceProcesses);
		stackItemChild.oi = OAObjectInfoDelegate.getOAObjectInfo(stackItemChild.li.getToClass());
		stackItemChild.node = stackItem.node.get(stackItemChild.li.getLowerName()).get(0);

		EqualQueryForObject eq = loader.getEqualQueryForObject(stackItemChild);

		assertEquals(rcx, eq.value);
		assertEquals(3, eq.cntOrs);
		assertNotNull(eq.sqlOrs);
	}

	@Test
	public void getEqualQueryForObjectTest2() throws Exception {
		reset();

		ObjectMapper om = oaj.getObjectMapper();

		InputStream is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		ReporterCorp rcx = oaj.readObject(is, ReporterCorp.class, false);

		is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		JsonNode nodeRoot = oaj.readTree(is);

		OAJacksonDeserializerLoader loader = new OAJacksonDeserializerLoader(oaj);
		rcx = loader.load(nodeRoot, null, ReporterCorp.class);

		StackItem stackItem = new StackItem();
		final StackItem stackItemRoot = stackItem;
		stackItemRoot.obj = rcx;
		stackItem.oi = OAObjectInfoDelegate.getOAObjectInfo(ReporterCorp.class);
		stackItem.node = nodeRoot;

		StackItem stackItemChild = new StackItem();
		stackItemChild.parent = stackItem;
		stackItemChild.li = stackItem.oi.getLinkInfo(ReporterCorp.P_StoreInfos);
		stackItemChild.oi = OAObjectInfoDelegate.getOAObjectInfo(stackItemChild.li.getToClass());
		// stackItemChild.node = stackItem.node.get(stackItemChild.li.getLowerName()).get(0);

		EqualQueryForObject eq = loader.getEqualQueryForObject(stackItemChild);

		assertEquals(rcx, eq.value);
		assertEquals("ReporterCorp", eq.propPath);
		assertEquals(0, eq.cntOrs);
		assertNull(eq.sqlOrs);
	}

	@Test
	public void getEqualQueryForReferenceTest() throws Exception {
		reset();

		ObjectMapper om = oaj.getObjectMapper();

		InputStream is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		ReporterCorp rcx = oaj.readObject(is, ReporterCorp.class, false);

		is = OAJacksonDeserializerLoaderTest.class.getResourceAsStream("test6.json");
		JsonNode nodeRoot = oaj.readTree(is);

		OAJacksonDeserializerLoader loader = new OAJacksonDeserializerLoader(oaj);
		rcx = loader.load(nodeRoot, null, ReporterCorp.class);

		// ReporterCorp.ReportInstanceProcessorInfo.ReportInstanceProcesses

		StackItem stackItem = new StackItem();
		final StackItem stackItemRoot = stackItem;
		stackItemRoot.obj = rcx;
		stackItem.oi = OAObjectInfoDelegate.getOAObjectInfo(ReporterCorp.class);
		stackItem.node = nodeRoot;

		StackItem stackItemChild = new StackItem();
		stackItemChild.parent = stackItem;
		stackItemChild.li = stackItem.oi.getLinkInfo(ReporterCorp.P_ReportInstanceProcessorInfo);
		stackItemChild.oi = OAObjectInfoDelegate.getOAObjectInfo(stackItemChild.li.getToClass());
		stackItemChild.node = stackItem.node.get(stackItemChild.li.getLowerName());
		stackItemChild.obj = rcx.getReportInstanceProcessorInfo();

		stackItem = stackItemChild;

		stackItemChild = new StackItem();
		stackItemChild.parent = stackItem;
		stackItemChild.li = stackItem.oi.getLinkInfo(ReportInstanceProcessorInfo.P_ReportInstanceProcesses);
		stackItemChild.oi = OAObjectInfoDelegate.getOAObjectInfo(stackItemChild.li.getToClass());
		stackItemChild.node = stackItem.node.get(stackItemChild.li.getLowerName()).get(0);

		EqualQueryForReference eq = loader.getEqualQueryForReference(	stackItemChild,
																		PojoLinkOneDelegate
																				.getPojoLinkOne(stackItemChild.oi.getPojo(),
																								ReportInstanceProcess.P_ReportInfo)
																				.getPojoLinkUnique());
		assertEquals("reportInstanceProcessorInfo", eq.propPath);
		assertNotNull(eq.value);

		eq = loader.getEqualQueryForReference(	stackItemChild,
												PojoLinkOneDelegate
														.getPojoLinkOne(stackItemChild.oi.getPojo(),
																		ReportInstanceProcess.P_StoreInfo)
														.getPojoLinkUnique());
		assertEquals("reporterCorp", eq.propPath);
		assertNotNull(eq.value);

		eq = loader.getEqualQueryForReference(	stackItemChild,
												PojoLinkOneDelegate
														.getPojoLinkOne(stackItemChild.oi.getPojo(),
																		ReportInstanceProcess.P_ThreadInfo)
														.getPojoLinkUnique());
		assertEquals("reporterCorp", eq.propPath);
		assertNotNull(eq.value);
	}

}
