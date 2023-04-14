package com.viaoa.json.jackson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.auto.dev.reportercorp.model.oa.PypeReportMessage;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportInfo;
import com.auto.dev.reportercorp.model.oa.ReportInstance;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessorInfo;
import com.auto.dev.reportercorp.model.oa.ReportTemplate;
import com.auto.dev.reportercorp.model.oa.ReportVersion;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.auto.dev.reportercorp.model.oa.StoreInfo;
import com.auto.dev.reportercorp.model.oa.ThreadInfo;
import com.auto.dev.reportercorp.model.oa.propertypath.ReporterCorpPP;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.viaoa.json.OAJson;
import com.viaoa.json.OAJson.StackItem;
import com.viaoa.object.OAObjectCacheDelegate;

public class ReportInstanceProcessJsonTest {

	private OAJson oaj;
	private static int Id;
	private OAJacksonDeserializerLoader loader;

	@Before
	public void before() {
		oaj = new OAJson();
		OAObjectCacheDelegate.setUnitTestMode(true);
	}

	@After
	public void after() {
		OAObjectCacheDelegate.setUnitTestMode(false);
	}

	protected void reset() throws Exception {
		oaj.setStackItem(null);
		loader = new OAJacksonDeserializerLoader(oaj);
		OAObjectCacheDelegate.resetCache();
	}

	static class TestData {
		boolean deleteStatisInfo = true;

		int cntThreadInfo = 2;
		int cntStoreInfo = 2;
		int cntReportInstanceProcess = 2;
		int cntReportInfo = 3;

		ReporterCorp rc;
		ReportInstanceProcessorInfo ripi;
		StoreInfo si;
		ThreadInfo ti;
		ReportInstanceProcess rip;
		ReportInfo ri;
		Report rpt;
		ReportTemplate rt;

		String json;

		ObjectNode nodeReporterCorp;
		JsonNode nodeReportInstanceProcessorInfo;
		JsonNode nodeStoreInfo;
		JsonNode nodeThreadInfo;
		JsonNode nodeReportInstanceProcess;
		JsonNode nodeReportInfo;
		JsonNode nodeReport;
		JsonNode nodeReportTemplate;

		StackItem siReporterCorp;
		StackItem siStoreInfo;
		StackItem siThreadInfo;
		StackItem siReportInstanceProcessorInfo;
		StackItem siReportInstanceProcess;
		StackItem siReportInfo;
		StackItem siReport;
		StackItem siReportTemplate;
		StackItem siPypeReportMessage;

		StackItem siReportInstance;
		StackItem siReportVersion;
		StackItem siReportTemplate2;
	}

	// 1: reporterCorp->storeInfos->reportInstanceProcesses
	// 2: reporterCorp->threadInfos->reportInstanceProcesses
	// 3: reporterCorp->reportInstanceProcessInfo->reportInstanceProcesses
	// 4: reporterCorp->reportInstanceProcessInfo->reportInfos->reportInstanceProcesses

	// new rip
	//    add to 1, 2, 3, 4
	//    add to 2, 3, 4, 1
	//    add to 3, 4, 1, 2
	//    add to 4, 1, 2, 3

	@Test
	public void ripTest() throws Exception {
		reset();
		final TestData testData = new TestData();
		createData(testData);

		oaj.clearPropertyPaths();

		oaj.addPropertyPath(ReporterCorpPP.storeInfos().pp);
		oaj.addPropertyPath(ReporterCorpPP.threadInfos().pp);
		oaj.addPropertyPath(ReporterCorpPP.reportInstanceProcessorInfo().reportInfos().pp);
		oaj.addPropertyPath(ReporterCorpPP.reportInstanceProcessorInfo().reportInstanceProcesses().pp);

		createJsonNodes(testData);

		oaj.setStackItem(testData.siReporterCorp);

	}

	/* qqqqqq PPs
	
		oaj.addPropertyPath(ReporterCorpPP.storeInfos().pp);
		oaj.addPropertyPath(ReporterCorpPP.threadInfos().pp);
		oaj.addPropertyPath(ReporterCorpPP.reportInstanceProcessorInfo().reportInstanceProcesses().reportInfo().report()
				.reportTemplates().pp);

		oaj.addPropertyPath(ReporterCorpPP.reportInstanceProcessorInfo().reportInstanceProcesses().pypeReportMessage().reportInstance()
				.reportVersion().reportTemplate().pp);

		oaj.addPropertyPath(ReporterCorpPP.reportInstanceProcessorInfo().reportInfos().report().reportTemplates().pp);
	
	
	*/

	protected void createData(TestData testData) throws Exception {
		if (testData == null) {
			return;
		}

		testData.rc = new ReporterCorp();
		if (testData.deleteStatisInfo) {
			testData.rc.getStatusInfo().delete();
		}
		testData.rc.setId(++Id);
		if (testData.deleteStatisInfo) {
			testData.rc.getReportInstanceProcessorInfo().getStatusInfo().delete();
		}
		testData.ripi = testData.rc.getReportInstanceProcessorInfo();
		testData.ripi.setId(++Id);

		for (int i = 0; i < testData.cntThreadInfo; i++) {
			ThreadInfo ti = new ThreadInfo();
			ti.setId(++Id);
			ti.setName("name." + ti.getId());
			testData.rc.getThreadInfos().add(ti);
			if (testData.deleteStatisInfo) {
				ti.getStatusInfo().delete();
			}
			if (testData.ti == null) {
				testData.ti = ti;
			}
		}

		for (int i = 0; i < testData.cntStoreInfo; i++) {
			StoreInfo si = new StoreInfo();
			si.setId(++Id);
			si.setStoreNumber(9000 + si.getId());
			testData.rc.getStoreInfos().add(si);
			if (testData.deleteStatisInfo) {
				si.getStatusInfo().delete();
			}
			if (testData.si == null) {
				testData.si = si;
			}
		}

		for (int i = 0; i < testData.cntReportInfo; i++) {
			Report report = new Report();
			report.setId(++Id);
			report.setName("name." + report.getId());
			report.setFileName("filename." + report.getId());

			ReportTemplate rt = new ReportTemplate();
			rt.setId(++Id);
			rt.setMd5hash("rt." + rt.getId());
			rt.setReport(report);

			ReportVersion rv = new ReportVersion();
			rv.setId(++Id);
			rv.setReportTemplate(rt);

			ReportInfo ri = new ReportInfo();
			ri.setId(++Id);
			ri.setReport(report);

			testData.rc.getReportInstanceProcessorInfo().getReportInfos().add(ri);

			if (testData.ri == null) {
				testData.ri = ri;
				testData.rpt = report;
				testData.rt = rt;
			}
		}

		for (int i = 0; i < testData.cntReportInstanceProcess; i++) {
			ReportInstanceProcess rip = new ReportInstanceProcess();
			rip.setCounter(i);
			testData.rc.getReportInstanceProcessorInfo().getReportInstanceProcesses().add(rip);
			if (testData.deleteStatisInfo) {
				rip.getStatusInfo().delete();
			}

			if (testData.cntStoreInfo > 0) {
				int x = i % testData.cntStoreInfo;
				rip.setStoreInfo(testData.rc.getStoreInfos().get(x));
			}
			if (testData.cntThreadInfo > 0) {
				int x = i % testData.cntThreadInfo;
				rip.setThreadInfo(testData.rc.getThreadInfos().get(x));
			}
			if (testData.cntReportInfo > 0) {
				int x = (i % testData.cntReportInfo);
				rip.setReportInfo(testData.rc.getReportInstanceProcessorInfo().getReportInfos().get(x));
			}

			PypeReportMessage prm = new PypeReportMessage();
			prm.setId(++Id);
			prm.setStore(1234);
			prm.setFilename("fn." + prm.getId());
			rip.setPypeReportMessage(prm);

			ReportInstance ri = new ReportInstance();
			ri.setId(++Id);
			ri.setStoreNumber(1234);
			ri.setFileName("fn." + ri.getId());
			ri.setPypeReportMessage(prm);

			if (testData.cntReportInfo > 0) {
				ri.setReportVersion(rip.getReportInfo().getReport().getReportTemplates().get(0).getReportVersions().get(0));
			}
			if (testData.rip == null) {
				testData.rip = rip;
			}
		}
	}

	protected void createJsonNodes(TestData testData) throws Exception {
		if (testData == null) {
			return;
		}

		oaj.setStackItem(null);
		oaj.setWriteAsPojo(true);
		testData.json = oaj.write(testData.rc);
		System.out.println(testData.json); //qqqqqqqqqqqq
		oaj.setWriteAsPojo(false);

		oaj.setReadingPojo(true);
		testData.nodeReporterCorp = (ObjectNode) oaj.readTree(testData.json);
		oaj.setReadingPojo(false);

		testData.nodeReportInstanceProcessorInfo = (ObjectNode) testData.nodeReporterCorp.get(testData.rc.P_ReportInstanceProcessorInfo);

		if (testData.cntStoreInfo > 0) {
			testData.nodeStoreInfo = ((ArrayNode) testData.nodeReporterCorp.get(testData.rc.P_StoreInfos)).get(0);
		}

		if (testData.cntThreadInfo > 0) {
			testData.nodeThreadInfo = ((ArrayNode) testData.nodeReporterCorp.get(testData.rc.P_ThreadInfos)).get(0);
		}

		if (testData.cntReportInstanceProcess > 0) {
			testData.nodeReportInstanceProcess = ((ArrayNode) testData.nodeReporterCorp.get(testData.rc.P_ReportInstanceProcessorInfo)
					.get(ReportInstanceProcessorInfo.P_ReportInstanceProcesses)).get(0);
		}
	}
}
