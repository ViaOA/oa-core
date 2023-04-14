package com.viaoa.json.jackson;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessorInfo;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.viaoa.json.OAJson;
import com.viaoa.util.OADateTime;

public class OAJsonTest {
	OAJson oaj;
	ReporterCorp rc = new ReporterCorp();

	@Before
	public void before() {
		oaj = new OAJson();
	}

	@Test
	public void test1() throws Exception {
		OAJson oj = new OAJson();
		String json = oj.write(rc);

		assertNotNull(json);
	}

	@Test
	public void testOAObjectDeserializer() throws Exception {
		OAJson oj = new OAJson();
		String json = oj.write(rc);

		assertNotNull(json);

		ObjectNode nodeRoot = (ObjectNode) oaj.readTree(json);

		int xx = 4;
		xx++;
	}

	public void jsonTest2() throws Exception {

		ReportInstanceProcessorInfo ripi = rc.getReportInstanceProcessorInfo();
		ripi.setPaused(new OADateTime());

		OAJson oj = new OAJson();
		oj.setWriteAsPojo(true);

		String json = oj.write(rc);

		com.auto.reportercorp.model.pojo.ReporterCorp rcx = oj
				.readObject(json, com.auto.reportercorp.model.pojo.ReporterCorp.class);

		json = oj.write(rcx);
		System.out.println("========\n" + json);

		oj.readIntoObject(json, rc);

		int xx = 4;
		xx++;

	}

	public static void main(String[] args) throws Exception {
		OAJsonTest t = new OAJsonTest();
		// t.test();
	}
}