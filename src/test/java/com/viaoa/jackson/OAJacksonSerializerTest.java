package com.viaoa.jackson;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.object.OAGroupBy;

import test.xice.tsam.model.oa.Application;
import test.xice.tsam.model.oa.Server;

public class OAJacksonSerializerTest extends OAUnitTest {

	@Test
	public void test() {

		OAGroupBy<Server, Application> gb = new OAGroupBy<Server, Application>();
	}

}