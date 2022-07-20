package com.corptostore.delegate.oa;

import com.corptostore.model.oa.CorpToStore;
import com.corptostore.model.oa.Environment;
import com.viaoa.util.OADateTime;

public class EnvironmentDelegate {

	public static void updateCorpToStore(final Environment env) {
		if (env == null) {
			return;
		}

		for (CorpToStore sc : env.getCorpToStores()) {
			sc.updateInfo();
		}
	}

	public static void continueAll(Environment env) {
		if (env == null) {
			return;
		}
		for (CorpToStore cts : env.getCorpToStores()) {
			cts.setThreadsPaused(null);
		}
	}

	public static void pauseAll(Environment env) {
		if (env == null) {
			return;
		}
		for (CorpToStore cts : env.getCorpToStores()) {
			cts.setThreadsPaused(new OADateTime());
		}
	}

	public static boolean getIsAllRunning(Environment env) {
		if (env == null) {
			return true;
		}
		for (CorpToStore sc : env.getCorpToStores()) {
			if (sc.getThreadsPaused() != null) {
				return false;
			}
		}
		return true;
	}

	public static boolean getIsAllPaused(Environment env) {
		if (env == null) {
			return false;
		}
		for (CorpToStore sc : env.getCorpToStores()) {
			if (sc.getThreadsPaused() == null) {
				return false;
			}
		}
		return true;
	}
}
