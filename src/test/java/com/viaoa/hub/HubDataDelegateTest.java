package com.viaoa.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.viaoa.OAUnitTest;

import test.hifive.HifiveDataGenerator;
import test.hifive.delegate.ModelDelegate;
import test.hifive.model.oa.Ecard;
import test.hifive.model.oa.Program;

public class HubDataDelegateTest extends OAUnitTest {

	@Test
	public void test() {
		reset();
		HifiveDataGenerator data = new HifiveDataGenerator();
		data.createSampleData();

		for (Program program : ModelDelegate.getPrograms()) {
			Hub<Ecard> hub = program.getEcards();
			for (Ecard ec : hub) {
				//boolean b = OAObjectHubDelegate.isInHub(ec, hub);
				boolean b2 = hub.contains(ec);
				assertTrue(b2);
			}
		}

		Hub<Ecard> hubEcard = new Hub<>(Ecard.class);

		HubFilter filter = new HubFilter(ModelDelegate.getPrograms().getAt(0).getEcards(), hubEcard);
		assertEquals(ModelDelegate.getPrograms().getAt(0).getEcards().size(), hubEcard.size());
		reset();
	}

}
