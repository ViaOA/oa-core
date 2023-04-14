package com.viaoa.json.jackson;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import com.auto.reportercorp.model.pojo.ProcessStep;
import com.auto.reportercorp.model.pojo.PypeReportMessage;
import com.auto.reportercorp.model.pojo.Report;
import com.auto.reportercorp.model.pojo.ReportInfo;
import com.auto.reportercorp.model.pojo.ReportInstance;
import com.auto.reportercorp.model.pojo.ReportInstanceProcess;
import com.auto.reportercorp.model.pojo.ReportInstanceProcessStep;
import com.auto.reportercorp.model.pojo.ReportInstanceProcessorInfo;
import com.auto.reportercorp.model.pojo.ReportTemplate;
import com.auto.reportercorp.model.pojo.ReportVersion;
import com.auto.reportercorp.model.pojo.ReporterCorp;
import com.auto.reportercorp.model.pojo.StoreInfo;
import com.auto.reportercorp.model.pojo.ThreadInfo;
import com.viaoa.datasource.autonumber.OADataSourceAuto;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.pojo.Pojo;
import com.viaoa.pojo.PojoDelegate;
import com.viaoa.pojo.PojoProperty;

// qqqqqqqqqqq this was used to produce test*.json resource files
// qqqq   used by OAJacksonDeserializerLoaderTester

public class OAJacksonDeserializerLoaderTestPojo {

	private OAJson oaj;
	private static int Id;
	// private OAJacksonDeserializerLoader loader;

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
		oaj.setReadingPojo(true);
		// loader = new OAJacksonDeserializerLoader(oaj);
		OAObjectCacheDelegate.resetCache();

		List alRpt = new ArrayList();
		for (int id : Arrays.asList(1, 4)) {
			com.auto.dev.reportercorp.model.oa.Report rpt = new com.auto.dev.reportercorp.model.oa.Report();
			rpt.setId(id);
			rpt.save();
			alRpt.add(rpt);
		}
	}

	/* creating Resource files

	src/test/resources/com/viaoa/json/jackson

	test1.json   reporterCorp->reportInstanceProcessInfo->reportInstanceProcesses
	ReporterCorp.java
		@JsonPropertyOrder({ "created", "stopped", "paused", "reportInstanceProcessorInfo", "storeInfos", "threadInfos" })
	ReportInstanceProcessorInfo.java
		@JsonPropertyOrder({ "created", "paused", "receivedCount", "processedCount", "fixedJsonCount", "pypeErrorCount", "processingErrorCount", "reporterCorp","reportInstanceProcesses", "reportInfos" })
	
	test2.json   reporterCorp->reportInstanceProcessInfo->reportInfos->reportInstanceProcesses
	ReporterCorp.java
		@JsonPropertyOrder({ "created", "stopped", "paused", "reportInstanceProcessorInfo", "storeInfos", "threadInfos" })
	ReportInstanceProcessorInfo.java
		@JsonPropertyOrder({ "created", "paused", "receivedCount", "processedCount", "fixedJsonCount", "pypeErrorCount", "processingErrorCount", "reporterCorp", "reportInfos", "reportInstanceProcesses" })


	test3.json   reporterCorp->storeInfos->reportInstanceProcesses
	ReporterCorp.java
		@JsonPropertyOrder({ "created", "stopped", "paused", "storeInfos", "threadInfos", "reportInstanceProcessorInfo" })
	ReportInstanceProcessorInfo.java
		@JsonPropertyOrder({ "created", "paused", "receivedCount", "processedCount", "fixedJsonCount", "pypeErrorCount", "processingErrorCount", "reporterCorp","reportInstanceProcesses","reportInfos" })


	test4.json   reporterCorp->threadInfos->reportInstanceProcesses
	ReporterCorp.java
		@JsonPropertyOrder({ "created", "stopped", "paused", "threadInfos", "storeInfos", "reportInstanceProcessorInfo" })
	ReportInstanceProcessorInfo.java
		@JsonPropertyOrder({ "created", "paused", "receivedCount", "processedCount", "fixedJsonCount", "pypeErrorCount", "processingErrorCount", "reporterCorp","reportInstanceProcesses","reportInfos" })

	test5.json
		reporterCorp->reportInstanceProcessInfo->reportInfos[->report]

	*/

	// @Test
	public void createTestDataTest() throws Exception {
		ReporterCorp rc = createTestData();

		// need to create OAObjects are not in the pojo, but will be needed when deserializing
		List<Report> alReport = new ArrayList();
		for (ReportInfo ri : rc.getReportInstanceProcessorInfo().getReportInfos()) {
			if (!alReport.contains(ri.getReport())) {
				alReport.add(ri.getReport());
			}
		}
		String json = oaj.write(alReport);
		List<com.auto.dev.reportercorp.model.oa.Report> alRpt = oaj
				.readList(json, com.auto.dev.reportercorp.model.oa.Report.class, false);

		com.auto.dev.reportercorp.model.oa.ProcessStep ps = new com.auto.dev.reportercorp.model.oa.ProcessStep();
		ps.setStep(com.auto.dev.reportercorp.model.oa.ProcessStep.STEP_getReport);

		//
		json = oaj.write(rc);
		System.out.println(json); //qqqqqqqqqq

		OADataSourceAuto dsAuto = new OADataSourceAuto();
		dsAuto.setAssignIdOnCreate(true);

		com.auto.dev.reportercorp.model.oa.ReporterCorp rcx = oaj
				.readObject(json, com.auto.dev.reportercorp.model.oa.ReporterCorp.class);

		oaj.readIntoObject(json, rcx);

		assertEquals(2, rcx.getThreadInfos().size());
		assertEquals(2, rcx.getReportInstanceProcessorInfo().getReportInfos().size());

		assertEquals(3, rcx.getReportInstanceProcessorInfo().getReportInstanceProcesses().size());

		assertEquals(3, OAObjectCacheDelegate.getTotal(com.auto.dev.reportercorp.model.oa.ReportInstanceProcess.class));

		OAObjectInfo oi = OAObjectInfoDelegate.getObjectInfo(com.auto.dev.reportercorp.model.oa.ReportInstanceProcess.class);
		Pojo pojo = oi.getPojo();
		List<PojoProperty> al = PojoDelegate.getPojoPropertyKeys(pojo);

		oi = OAObjectInfoDelegate.getObjectInfo(com.auto.dev.reportercorp.model.oa.ThreadInfo.class);
		pojo = oi.getPojo();
		al = PojoDelegate.getPojoPropertyKeys(pojo);

		oi = OAObjectInfoDelegate.getObjectInfo(com.auto.dev.reportercorp.model.oa.StatusInfoMessage.class);
		pojo = oi.getPojo();
		al = PojoDelegate.getPojoPropertyKeys(pojo);

		//qqqqqqq add asserts

		//qqqqqqqqqqqq reload again and make sure that no new objects are created
		//qqqqqqqq since there are already matching objects that exist

		//qqqq need to test a model that has a compundKey

		int xx = 4;
		xx++;

	}

	public ReporterCorp createTestData() throws Exception {
		reset();

		ReporterCorp rc = new ReporterCorp();

		ReportInstanceProcessorInfo ripi = rc.getReportInstanceProcessorInfo();
		ripi.setReporterCorp(rc);

		for (int i = 0; i < 2; i++) {
			StoreInfo si = new StoreInfo();
			si.setStoreNumber(9000 + i);
			rc.getStoreInfos().add(si);
			si.setReporterCorp(rc);

			si.getStatusInfo().setStatus("status here");
			si.getStatusInfo().setActivity("activity here");
			si.getStatusInfo().setAlert("alert here");
		}

		for (int i = 0; i < 2; i++) {
			ThreadInfo ti = new ThreadInfo();
			ti.setName("name." + i);
			rc.getThreadInfos().add(ti);
			ti.setReporterCorp(rc);

			ti.getStatusInfo().setStatus("status here");
			ti.getStatusInfo().setActivity("activity here");
			ti.getStatusInfo().setAlert("alert here");
		}

		for (int i = 0; i < 2; i++) {
			ReportInfo ri = new ReportInfo();
			rc.getReportInstanceProcessorInfo().getReportInfos().add(ri);

			Report report = new Report();
			report.setId(++Id);
			report.setName("name." + i);
			report.setFileName("filename." + i);
			ri.setReport(report);

			ReportTemplate rt = new ReportTemplate();
			rt.setId(++Id);
			rt.setMd5hash("rt." + rt.getId());
			rt.setReport(report);
			report.getReportTemplates().add(rt);

			ReportVersion rv = new ReportVersion();
			rv.setId(++Id);
			rv.setReportTemplate(rt);
			rt.getReportVersions().add(rv);
		}

		for (int i = 0; i < 3; i++) {
			ReportInstanceProcess rip = new ReportInstanceProcess();
			rip.setCounter(i);
			rc.getReportInstanceProcessorInfo().getReportInstanceProcesses().add(rip);
			rip.setReportInstanceProcessorInfo(ripi);

			int x = i % 2;
			StoreInfo si = rc.getStoreInfos().get(x);
			rip.setStoreInfo(si);
			si.getReportInstanceProcesses().add(rip);

			x = i % 2;
			ThreadInfo ti = rc.getThreadInfos().get(x);
			rip.setThreadInfo(ti);
			ti.getReportInstanceProcesses().add(rip);

			ReportInfo ri = rc.getReportInstanceProcessorInfo().getReportInfos().get(x);
			rip.setReportInfo(ri);
			ri.getReportInstanceProcesses().add(rip);

			PypeReportMessage prm = new PypeReportMessage();
			prm.setId(++Id);
			prm.setStore(1234);
			prm.setFilename("fn." + prm.getId());
			rip.setPypeReportMessage(prm);

			ReportInstance rix = new ReportInstance();
			rix.setPypeReportMessage(prm);
			prm.setReportInstance(rix);
			rix.setId(++Id);
			rix.setStoreNumber(1234);
			rix.setFileName("fn." + rix.getId());

			rix.setReportVersion(rip.getReportInfo().getReport().getReportTemplates().get(0).getReportVersions().get(0));

			rip.getStatusInfo().setStatus("status here");
			rip.getStatusInfo().setActivity("activity here");
			if (i % 2 == 0) {
				rip.getStatusInfo().setAlert("alert here");
			}

			if (i == 0) {
				ReportInstanceProcessStep rips = new ReportInstanceProcessStep();
				ProcessStep ps = new ProcessStep();
				ps.setStep(ProcessStep.Step.getReport);
				rips.setProcessStep(ps);
				rip.getReportInstanceProcessSteps().add(rips);
			}
		}
		return rc;
	}

	// @Test
	public void createTestData2() throws Exception {
		reset();

		ReporterCorp rc = new ReporterCorp();

		ReportInstanceProcessorInfo ripi = rc.getReportInstanceProcessorInfo();
		ripi.setReporterCorp(rc);

		for (int i = 0; i < 2; i++) {
			StoreInfo si = new StoreInfo();
			si.setStoreNumber(1000 + i);
			rc.getStoreInfos().add(si);
			si.setReporterCorp(rc);
		}

		for (int i = 0; i < 3; i++) {
			ReportInstanceProcess rip = new ReportInstanceProcess();
			rip.setCounter(i);
			rc.getReportInstanceProcessorInfo().getReportInstanceProcesses().add(rip);
			rip.setReportInstanceProcessorInfo(ripi);

			/*
			StoreInfo si = new StoreInfo();
			si.setStoreNumber(1000 + i);
			*/

			StoreInfo si = rc.getStoreInfos().get(0);
			rip.setStoreInfo(si);
		}

		String json = oaj.write(rc);

		OADataSourceAuto dsAuto = new OADataSourceAuto();
		dsAuto.setAssignIdOnCreate(true);

		com.auto.dev.reportercorp.model.oa.ReporterCorp rcx = oaj
				.readObject(json, com.auto.dev.reportercorp.model.oa.ReporterCorp.class);

		int xx = 4;
		xx++;
	}
}
