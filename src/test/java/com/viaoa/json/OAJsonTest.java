package com.viaoa.json;

import org.junit.Test;

import com.corptostore.model.oa.CorpToStore;
import com.corptostore.model.oa.PurgeWindow;
import com.corptostore.model.pojo.CorpToStorePojoTest;
import com.viaoa.datasource.objectcache.OADataSourceObjectCache;

public class OAJsonTest {

	@Test
	public void jsonTest() throws Exception {

		OADataSourceObjectCache ds = new OADataSourceObjectCache();
		ds.setAssignIdOnCreate(true);

		OAJson oj = new OAJson();
		oj.setIncludeAll(true);

		CorpToStorePojoTest pojoTest = new CorpToStorePojoTest();

		String json = pojoTest.jsonTest();
		System.out.println("1: =========\n" + json);

		CorpToStore cts = oj.readObject(json, CorpToStore.class, true);
		String json2 = oj.write(cts);
		System.out.println("2: =========\n" + json2);

		oj.readIntoObject(json, cts, true);
		json2 = oj.write(cts);
		System.out.println("3: =========\n" + json2);

		json = pojoTest.jsonTest(false);
		// cts = oj.readObject(json, CorpToStore.class, true);
		oj.readIntoObject(json, cts, true);

		json2 = oj.write(cts);
		System.out.println("4: =========\n" + json2);

		cts.getPurgeWindows().add(new PurgeWindow());

		oj.readIntoObject(json, cts, true);
		json2 = oj.write(cts);
		System.out.println("5: =========\n" + json2);

		int xx = 4;
		xx++;
	}

	public static void main(String[] args) throws Exception {
		OAJsonTest test = new OAJsonTest();
		test.jsonTest();
	}
}