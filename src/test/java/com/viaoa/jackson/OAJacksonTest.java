package com.viaoa.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oreillyauto.dev.tool.messagedesigner.model.oa.JsonType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageRecord;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageSource;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageTypeColumn;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageTypeRecord;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.RpgType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.propertypath.MessageSourcePP;
import com.viaoa.OAUnitTest;
import com.viaoa.object.OAObjectCacheDelegate;

public class OAJacksonTest extends OAUnitTest {

	@Test
	public void configTest() {
		OAJackson oaj = new OAJackson();

		assertTrue(oaj.getIncludeOwned());
		oaj.setIncludeOwned(false);
		assertFalse(oaj.getIncludeOwned());
		oaj.setIncludeOwned(true);
		assertTrue(oaj.getIncludeOwned());

		assertFalse(oaj.getIncludeAll());
		oaj.setIncludeAll(false);
		assertFalse(oaj.getIncludeAll());
		oaj.setIncludeAll(true);
		assertTrue(oaj.getIncludeAll());
	}

	@Test
	public void jsonTest() throws Exception {
		MessageSource ms = new MessageSource();
		ms.setId(7777);

		OAJackson oaj = new OAJackson();

		String json = oaj.write(ms);

		reset();

		MessageSource ms2 = oaj.readObject(json, MessageSource.class, false);

		String json2 = oaj.write(ms2);
		assertEquals(json, json2);
	}

	@Test
	public void json2Test() throws Exception {
		MessageSource ms = new MessageSource();
		ms.setId(3);

		ms.getMessageTypes().add(new MessageType());

		MessageTypeRecord mtr = new MessageTypeRecord();
		ms.getMessageTypeRecords().add(mtr);

		OAJackson oaj = new OAJackson();
		oaj.setIncludeOwned(true);
		String json = oaj.write(ms);

		OAObjectCacheDelegate.setUnitTestMode(true);
		OAObjectCacheDelegate.resetCache();

		MessageSource ms2 = oaj.readObject(json, MessageSource.class, false);

		String json2 = oaj.write(ms2);
		assertEquals(json, json2);
	}

	public void test2() throws Exception {
		MessageSource ms = new MessageSource();
		ms.setId(7777);

		ms.getMessageTypes().add(new MessageType());
		//		ms.getMessageTypes().add(new MessageType());
		ms.getMessageTypeRecords().add(new MessageTypeRecord());
		//		ms.getMessageTypeRecords().add(new MessageTypeRecord());

		MessageRecord mr = new MessageRecord();
		mr.setMessageTypeRecord(ms.getMessageTypeRecords().getAt(0));
		ms.getMessageTypes().getAt(0).getMessageRecords().add(mr);

		OAJackson oaj = new OAJackson();
		oaj.setIncludeOwned(false);

		ms.getMessageTypes().getAt(0).getMessageRecords().getAt(0).getMessageTypeRecord().getMessageTypeColumns();

		oaj.addPropertyPath(MessageSourcePP.messageTypes().messageRecords().messageTypeRecord().pp);

		oaj.addPropertyPath(MessageSourcePP.messageTypes().messageRecords().messageTypeRecord().messageTypeColumns().rpgType()
				.jsonType().pp);

		MessageTypeColumn col = new MessageTypeColumn();
		RpgType rpgType = new RpgType();
		rpgType.setName("rpg type name");
		rpgType.setJsonType(new JsonType());
		col.setRpgType(rpgType);
		ms.getMessageTypes().getAt(0).getMessageRecords().getAt(0).getMessageTypeRecord().getMessageTypeColumns().add(col);

		String json = oaj.write(ms);

		System.out.println("JSON=" + json);

		OAObjectCacheDelegate.setUnitTestMode(true);
		OAObjectCacheDelegate.resetCache();

		MessageSource ms2 = oaj.readObject(json, MessageSource.class, false);

		int xx = 4;
		xx++;

	}

}