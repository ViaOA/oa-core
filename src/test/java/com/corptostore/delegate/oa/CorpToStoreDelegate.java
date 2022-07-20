package com.corptostore.delegate.oa;

import com.corptostore.model.oa.CorpToStore;
import com.viaoa.hub.HubEvent;
import com.viaoa.remote.rest.OARestClient;
import com.viaoa.sync.OASync;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

public class CorpToStoreDelegate {

	private static volatile boolean bUpdatingInfo;

	public static void onPauseChangeTrigger(final CorpToStore corpToStore, HubEvent hubEvent) {

		//qqqqqqqqqqqqqqqqq
		if (true || false) {
			return;
		}

		if (corpToStore == null) {
			return;
		}

		if (bUpdatingInfo) {
			return;
		}
		if (!OASync.isServer()) {
			return;
		}

		String url = corpToStore.getBaseUrl();
		if (OAString.isEmpty(url)) {
			return;
		}
		url += "/admin/";

		if (corpToStore.getThreadsPaused() != null) {
			url += "pause";
		} else {
			url += "continue";
		}

		OARestClient client = new OARestClient();

		try {
			corpToStore.setConsole("Calling url=" + url);
			client.callJsonEndpoint("get", url, "", "");
			corpToStore.setConsole("Successful");
		} catch (Exception e) {
			corpToStore.setConsole("Failed: " + e.getMessage());
			System.out.println("Exception calling endpt: " + url);
			e.printStackTrace();
		}
	}

	public static void updateInfo(CorpToStore corpToStore) {
		if (corpToStore == null) {
			return;
		}

		String url = corpToStore.getBaseUrl();
		if (OAString.isEmpty(url)) {
			return;
		}

		url += "/admin/status";

		try {
			bUpdatingInfo = true;

			OARestClient client = new OARestClient();

			corpToStore.setLastSync(new OADateTime());
			corpToStore.setConsole(null);
			corpToStore.setConsole("Calling url=" + url);
			String s = client.callJsonEndpoint("get", url, "", "");
			corpToStore.setConsole("Successful");

			boolean b = s.indexOf("continue") >= 0;
			OADateTime dt = b ? (new OADateTime()) : null;
			corpToStore.setThreadsPaused(dt);

		} catch (Exception e) {
			corpToStore.setConsole("Failed: " + e.getMessage());
			System.out.println("Exception calling endpt: " + url);
			e.printStackTrace();
		} finally {
			bUpdatingInfo = false;
		}

	}

	public static String getStoreTransmitInfo(CorpToStore corpToStore) throws Exception {
		// public String callJsonEndpoint(String httpMethod, String urlPath, String query, String jsonBody) throws Exception {
		OARestClient client = new OARestClient();
		String url = corpToStore.getBaseUrl() + "/admin/storetransmit";

		// http://rtl-p-tcapp-corpsync1a-1.oreillyauto.com/MessageServiceCorpToStore/admin/storetransmit

		String json = client.callJsonEndpoint("get", url, "", "");
		return json;
	}

}
