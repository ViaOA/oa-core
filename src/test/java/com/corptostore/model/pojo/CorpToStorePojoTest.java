package com.corptostore.model.pojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.viaoa.json.OAJson;

/**
 * Used for Unit Test, to generate pojo objects for OAJacksonDeserialization test
 *
 * @author vvia
 */
public class CorpToStorePojoTest {

	// @Test
	public String jsonTest() throws Exception {
		return jsonTest(true);
	}

	public String jsonTest(boolean bAssignThread) throws Exception {

		OAJson oj = new OAJson();
		oj.setIncludeAll(true);

		final CorpToStore corpToStore = new CorpToStore();

		final int maxThreads = 2;
		for (int i = 0; i < maxThreads; i++) {
			ThreadInfo threadInfo = new ThreadInfo();
			threadInfo.setThreadId(i + 1);
			threadInfo.setName("" + (new Character((char) ('A' + i))));
			corpToStore.getThreadInfos().add(threadInfo);
		}

		final int maxStores = 2;
		for (int i = 0; i < maxStores; i++) {
			StoreInfo storeInfo = new StoreInfo();
			storeInfo.setStoreNumber(12345 + i);
			corpToStore.getStoreInfos().add(storeInfo);
		}

		final int maxStoreLocks = 2;
		for (int i = 0; i < maxStoreLocks; i++) {
			StoreLockInfo storeLockInfo = new StoreLockInfo();
			storeLockInfo.setStoreInfo(corpToStore.getStoreInfos().get(i % maxStores));
			if (bAssignThread) {
				storeLockInfo.setThreadInfo(corpToStore.getThreadInfos().get(i % maxThreads));
			}
			corpToStore.getStoreLockServiceInfo().getStoreLockInfos().add(storeLockInfo);
		}

		/*
		corpToStore.getStoreLockServiceInfo().getStoreLockInfos().get(0).setThreadInfo(corpToStore.getThreadInfos().get(0));
		corpToStore.getStoreLockServiceInfo().getStoreLockInfos().get(0).setStoreInfo(corpToStore.getStoreInfos().get(0));
		*/

		// corpToStore.getThreadInfos().get(0).setStoreLockInfo(corpToStore.getStoreLockServiceInfo().getStoreLockInfos().get(0));

		String json = oj.write(corpToStore);

		// System.out.println(json);

		// Object obj2 = oj.readObject(json, CorpToStore.class, true);


		
		return json;
	}

	public static void main(String[] args) throws Exception {
		CorpToStorePojoTest test = new CorpToStorePojoTest();
		test.jsonTest();
	}
}