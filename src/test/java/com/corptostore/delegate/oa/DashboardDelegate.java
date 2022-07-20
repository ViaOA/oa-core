package com.corptostore.delegate.oa;

import com.corptostore.delegate.ModelDelegate;
import com.corptostore.model.oa.CorpToStore;
import com.corptostore.model.oa.Dashboard;
import com.corptostore.model.oa.DashboardLine;
import com.corptostore.model.oa.Environment;
import com.corptostore.model.oa.StoreInfo;
import com.corptostore.model.oa.StoreLockInfo;
import com.corptostore.model.oa.StoreLockServiceInfo;
import com.corptostore.model.oa.ThreadInfo;

public class DashboardDelegate {

	public static void refresh(final Dashboard db) {
		db.getDashboardLines().deleteAll();

		for (Environment env : ModelDelegate.getEnvironments()) {
			for (CorpToStore cts : env.getCorpToStores()) {

				DashboardLine dl = new DashboardLine();
				dl.setEnvironment(env);
				dl.setLocation("loc");
				dl.setMessage("msg");
				db.getDashboardLines().add(dl);

				StoreLockServiceInfo slsi = cts.getStoreLockServiceInfo();

				for (StoreLockInfo sli : slsi.getStoreLockInfos()) {

					ThreadInfo ti = sli.getThreadInfo();

				}

				for (StoreInfo si : cts.getStoreInfos()) {

				}

			}
		}

	}

}
