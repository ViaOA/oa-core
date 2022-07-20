// Copied from OATemplate project by OABuilder 02/13/19 10:11 AM
package com.corptostore.delegate;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.corptostore.model.oa.AppServer;
import com.corptostore.model.oa.AppUser;
import com.corptostore.model.oa.AppUserError;
import com.corptostore.model.oa.AppUserLogin;
import com.corptostore.model.oa.Batch;
import com.corptostore.model.oa.Dashboard;
import com.corptostore.model.oa.Environment;
import com.corptostore.model.oa.Receive;
import com.corptostore.model.oa.Send;
import com.corptostore.model.oa.Store;
import com.corptostore.model.oa.StoreBatch;
import com.corptostore.model.oa.StoreTransmitBatch;
import com.corptostore.model.oa.Tester;
import com.corptostore.model.oa.TesterResultType;
import com.corptostore.model.oa.TesterStepType;
import com.corptostore.model.oa.Transmit;
import com.corptostore.model.oa.TransmitBatch;
import com.corptostore.model.oa.cs.ClientRoot;
import com.corptostore.model.oa.cs.ServerRoot;
import com.corptostore.delegate.ModelDelegate;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAODelegate;

/**
 * This is used to access all of the Root level Hubs. This is so that they will not have to be passed into and through the models. After
 * client login, the Hubs will be shared with the Hubs in the ServerRoot object from the server.
 *
 * @author vincevia
 * @see SingleController#initializeClientModel
 */
public class ModelDelegate {
	private static Logger LOG = Logger.getLogger(ModelDelegate.class.getName());

	private static final Hub<AppUserLogin> hubLocalAppUserLogin = new Hub<AppUserLogin>(AppUserLogin.class);
	private static final Hub<AppUser> hubLocalAppUser = new Hub<AppUser>(AppUser.class);

	/*$$Start: ModelDelegate1 $$*/
	// lookups, preselects
	private static final Hub<AppUser> hubAppUsers = new Hub<AppUser>(AppUser.class);
	private static final Hub<Environment> hubEnvironments = new Hub<Environment>(Environment.class);
	private static final Hub<TesterResultType> hubTesterResultTypes = new Hub<TesterResultType>(TesterResultType.class);
	private static final Hub<TesterStepType> hubTesterStepTypes = new Hub<TesterStepType>(TesterStepType.class);
	// autoCreateOne
	private static final Hub<AppServer> hubCreateOneAppServer = new Hub<AppServer>(AppServer.class);
	private static final Hub<Dashboard> hubCreateOneDashboard = new Hub<Dashboard>(Dashboard.class);
	// filters
	private static final Hub<Tester> hubOpenTesters = new Hub<Tester>(Tester.class);
	// UI containers
	private static final Hub<Tester> hubNewTest = new Hub<Tester>(Tester.class);
	private static final Hub<Tester> hubSearchTests = new Hub<Tester>(Tester.class);
	private static final Hub<Store> hubStores = new Hub<Store>(Store.class);
	private static final Hub<TransmitBatch> hubTransmitBatches = new Hub<TransmitBatch>(TransmitBatch.class);
	private static final Hub<Batch> hubSearchBatches = new Hub<Batch>(Batch.class);
	private static final Hub<Send> hubSearchSends = new Hub<Send>(Send.class);
	private static final Hub<Store> hubSearchStores = new Hub<Store>(Store.class);
	private static final Hub<StoreBatch> hubSearchStoreBatches = new Hub<StoreBatch>(StoreBatch.class);
	private static final Hub<StoreTransmitBatch> hubSearchStoreTransmitBatches = new Hub<StoreTransmitBatch>(StoreTransmitBatch.class);
	private static final Hub<TransmitBatch> hubSearchTransmitBatches = new Hub<TransmitBatch>(TransmitBatch.class);
	private static final Hub<Transmit> hubSearchTransmits = new Hub<Transmit>(Transmit.class);
	private static final Hub<Receive> hubSearchReceives = new Hub<Receive>(Receive.class);
	private static final Hub<AppUserLogin> hubAppUserLogins = new Hub<AppUserLogin>(AppUserLogin.class);
	private static final Hub<AppUserError> hubAppUserErrors = new Hub<AppUserError>(AppUserError.class);
	/*$$End: ModelDelegate1 $$*/

	public static void initialize(ServerRoot rootServer, ClientRoot rootClient) {
		LOG.fine("selecting data");

		/*$$Start: ModelDelegate2 $$*/
		// lookups, preselects
		setSharedHub(getAppUsers(), rootServer.getAppUsers());
		setSharedHub(getEnvironments(), rootServer.getEnvironments());
		setSharedHub(getTesterResultTypes(), rootServer.getTesterResultTypes());
		setSharedHub(getTesterStepTypes(), rootServer.getTesterStepTypes());
		// autoCreateOne
		setSharedHub(getCreateOneAppServerHub(), rootServer.getCreateOneAppServerHub());
		setSharedHub(getCreateOneDashboardHub(), rootServer.getCreateOneDashboardHub());
		// filters
		setSharedHub(getOpenTesters(), rootServer.getOpenTesters());
		// UI containers
		if (rootClient != null) {
			setSharedHub(getNewTest(), rootClient.getNewTest());
		}
		if (rootClient != null) {
			setSharedHub(getSearchTests(), rootClient.getSearchTests());
		}
		getStores().setSharedHub(rootServer.getStores());
		getTransmitBatches().setSharedHub(rootServer.getTransmitBatches());
		if (rootClient != null) {
			setSharedHub(getSearchBatches(), rootClient.getSearchBatches());
		}
		if (rootClient != null) {
			setSharedHub(getSearchSends(), rootClient.getSearchSends());
		}
		if (rootClient != null) {
			setSharedHub(getSearchStores(), rootClient.getSearchStores());
		}
		if (rootClient != null) {
			setSharedHub(getSearchStoreBatches(), rootClient.getSearchStoreBatches());
		}
		if (rootClient != null) {
			setSharedHub(getSearchStoreTransmitBatches(), rootClient.getSearchStoreTransmitBatches());
		}
		if (rootClient != null) {
			setSharedHub(getSearchTransmitBatches(), rootClient.getSearchTransmitBatches());
		}
		if (rootClient != null) {
			setSharedHub(getSearchTransmits(), rootClient.getSearchTransmits());
		}
		if (rootClient != null) {
			setSharedHub(getSearchReceives(), rootClient.getSearchReceives());
		}
		getAppUserLogins().setSharedHub(rootServer.getAppUserLogins());
		getAppUserErrors().setSharedHub(rootServer.getAppUserErrors());
		/*$$End: ModelDelegate2 $$*/

		for (int i = 0; i < 120; i++) {
			if (aiExecutor.get() == 0) {
				break;
			}
			if (i > 5) {
				LOG.fine(i + "/120 seconds) waiting on initialize to finish sharing hubs");
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		if (executorService != null) {
			executorService.shutdown();
			executorService = null;
			queExecutorService = null;
		}
		LOG.fine("completed selecting data");
	}

	public static Hub<AppUser> getLocalAppUserHub() {
		return hubLocalAppUser;
	}

	public static AppUser getLocalAppUser() {
		return getLocalAppUserHub().getAO();
	}

	public static void setLocalAppUser(AppUser user) {
		getLocalAppUserHub().add(user);
		getLocalAppUserHub().setAO(user);
	}

	public static Hub<AppUserLogin> getLocalAppUserLoginHub() {
		return hubLocalAppUserLogin;
	}

	public static AppUserLogin getLocalAppUserLogin() {
		return getLocalAppUserLoginHub().getAO();
	}

	public static void setLocalAppUserLogin(AppUserLogin userLogin) {
		getLocalAppUserLoginHub().add(userLogin);
		getLocalAppUserLoginHub().setAO(userLogin);
		if (userLogin != null) {
			setLocalAppUser(userLogin.getAppUser());
		}
	}

	/*$$Start: ModelDelegate3 $$*/
	public static Hub<AppUser> getAppUsers() {
		return hubAppUsers;
	}

	public static Hub<Environment> getEnvironments() {
		return hubEnvironments;
	}

	public static Hub<TesterResultType> getTesterResultTypes() {
		return hubTesterResultTypes;
	}

	public static Hub<TesterStepType> getTesterStepTypes() {
		return hubTesterStepTypes;
	}

	// autoCreateOne
	public static Hub<AppServer> getCreateOneAppServerHub() {
		return hubCreateOneAppServer;
	}

	public static AppServer getAppServer() {
		return hubCreateOneAppServer.getAt(0);
	}

	public static Hub<Dashboard> getCreateOneDashboardHub() {
		return hubCreateOneDashboard;
	}

	public static Dashboard getDashboard() {
		return hubCreateOneDashboard.getAt(0);
	}

	public static Hub<Tester> getOpenTesters() {
		return hubOpenTesters;
	}

	public static Hub<Tester> getNewTest() {
		return hubNewTest;
	}

	public static Hub<Tester> getSearchTests() {
		return hubSearchTests;
	}

	public static Hub<Store> getStores() {
		return hubStores;
	}

	public static Hub<TransmitBatch> getTransmitBatches() {
		return hubTransmitBatches;
	}

	public static Hub<Batch> getSearchBatches() {
		return hubSearchBatches;
	}

	public static Hub<Send> getSearchSends() {
		return hubSearchSends;
	}

	public static Hub<Store> getSearchStores() {
		return hubSearchStores;
	}

	public static Hub<StoreBatch> getSearchStoreBatches() {
		return hubSearchStoreBatches;
	}

	public static Hub<StoreTransmitBatch> getSearchStoreTransmitBatches() {
		return hubSearchStoreTransmitBatches;
	}

	public static Hub<TransmitBatch> getSearchTransmitBatches() {
		return hubSearchTransmitBatches;
	}

	public static Hub<Transmit> getSearchTransmits() {
		return hubSearchTransmits;
	}

	public static Hub<Receive> getSearchReceives() {
		return hubSearchReceives;
	}

	public static Hub<AppUserLogin> getAppUserLogins() {
		return hubAppUserLogins;
	}

	public static Hub<AppUserError> getAppUserErrors() {
		return hubAppUserErrors;
	}
	/*$$End: ModelDelegate3 $$*/

	// thread pool for initialize
	private static ThreadPoolExecutor executorService;
	private static LinkedBlockingQueue<Runnable> queExecutorService;
	private static final AtomicInteger aiExecutor = new AtomicInteger();

	private static void setSharedHub(final Hub h1, final Hub h2) {
		HubAODelegate.warnOnSettingAO(h1);
		if (executorService == null) {
			queExecutorService = new LinkedBlockingQueue<Runnable>(Integer.MAX_VALUE);
			// min/max must be equal, since new threads are only created when queue is full
			executorService = new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, queExecutorService);
			executorService.allowCoreThreadTimeOut(true); // ** must have this
		}

		aiExecutor.incrementAndGet();
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					h1.setSharedHub(h2, false);
				} finally {
					aiExecutor.decrementAndGet();
				}
			}
		});
	}
}
