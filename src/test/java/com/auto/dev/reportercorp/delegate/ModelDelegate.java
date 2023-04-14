// Copied from OATemplate project by OABuilder 12/09/22 04:06 PM
// Copied from OATemplate project by OABuilder 12/09/22 03:58 PM
// Copied from OATemplate project by OABuilder 02/13/19 10:11 AM
package com.auto.dev.reportercorp.delegate;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.AppServer;
import com.auto.dev.reportercorp.model.oa.AppUser;
import com.auto.dev.reportercorp.model.oa.AppUserError;
import com.auto.dev.reportercorp.model.oa.AppUserLogin;
import com.auto.dev.reportercorp.model.oa.Environment;
import com.auto.dev.reportercorp.model.oa.ProcessStep;
import com.auto.dev.reportercorp.model.oa.PypeReportMessage;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportInstance;
import com.auto.dev.reportercorp.model.oa.ReportTemplate;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.auto.dev.reportercorp.model.oa.StoreImport;
import com.auto.dev.reportercorp.model.oa.cs.ClientRoot;
import com.auto.dev.reportercorp.model.oa.cs.ServerRoot;
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
	private static final Hub<ProcessStep> hubProcessSteps = new Hub<ProcessStep>(ProcessStep.class);
	private static final Hub<Report> hubReports = new Hub<Report>(Report.class);
	private static final Hub<ReporterCorp> hubReporterCorps = new Hub<ReporterCorp>(ReporterCorp.class);
	// autoCreateOne
	private static final Hub<AppServer> hubCreateOneAppServer = new Hub<AppServer>(AppServer.class);
	private static final Hub<StoreImport> hubCreateOneStoreImport = new Hub<StoreImport>(StoreImport.class);
	// UI containers
	private static final Hub<Report> hubMasterOnlyReports = new Hub<Report>(Report.class);
	private static final Hub<ReportTemplate> hubSearchReportTemplates = new Hub<ReportTemplate>(ReportTemplate.class);
	private static final Hub<ReportTemplate> hubNeedsTemplateReportTemplates = new Hub<ReportTemplate>(ReportTemplate.class);
	private static final Hub<ReportInstance> hubSearchReportInstances = new Hub<ReportInstance>(ReportInstance.class);
	private static final Hub<PypeReportMessage> hubSearchPypeReportMessages = new Hub<PypeReportMessage>(PypeReportMessage.class);
	private static final Hub<AppUserLogin> hubAppUserLogins = new Hub<AppUserLogin>(AppUserLogin.class);
	private static final Hub<AppUserError> hubAppUserErrors = new Hub<AppUserError>(AppUserError.class);
	/*$$End: ModelDelegate1 $$*/

	public static void initialize(ServerRoot rootServer, ClientRoot rootClient) {
		LOG.fine("selecting data");

		/*$$Start: ModelDelegate2 $$*/
		// lookups, preselects
		setSharedHub(getAppUsers(), rootServer.getAppUsers());
		setSharedHub(getEnvironments(), rootServer.getEnvironments());
		setSharedHub(getProcessSteps(), rootServer.getProcessSteps());
		setSharedHub(getReports(), rootServer.getReports());
		setSharedHub(getReporterCorps(), rootServer.getReporterCorps());
		// autoCreateOne
		setSharedHub(getCreateOneAppServerHub(), rootServer.getCreateOneAppServerHub());
		setSharedHub(getCreateOneStoreImportHub(), rootServer.getCreateOneStoreImportHub());
		// filters
		// UI containers
		getMasterOnlyReports().setSharedHub(rootServer.getMasterOnlyReports());
		if (rootClient != null) {
			setSharedHub(getSearchReportTemplates(), rootClient.getSearchReportTemplates());
		}
		getNeedsTemplateReportTemplates().setSharedHub(rootServer.getNeedsTemplateReportTemplates());
		if (rootClient != null) {
			setSharedHub(getSearchReportInstances(), rootClient.getSearchReportInstances());
		}
		if (rootClient != null) {
			setSharedHub(getSearchPypeReportMessages(), rootClient.getSearchPypeReportMessages());
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

	public static Hub<ProcessStep> getProcessSteps() {
		return hubProcessSteps;
	}

	public static Hub<Report> getReports() {
		return hubReports;
	}

	public static Hub<ReporterCorp> getReporterCorps() {
		return hubReporterCorps;
	}

	// autoCreateOne
	public static Hub<AppServer> getCreateOneAppServerHub() {
		return hubCreateOneAppServer;
	}

	public static AppServer getAppServer() {
		return hubCreateOneAppServer.getAt(0);
	}

	public static Hub<StoreImport> getCreateOneStoreImportHub() {
		return hubCreateOneStoreImport;
	}

	public static StoreImport getStoreImport() {
		return hubCreateOneStoreImport.getAt(0);
	}

	public static Hub<Report> getMasterOnlyReports() {
		return hubMasterOnlyReports;
	}

	public static Hub<ReportTemplate> getSearchReportTemplates() {
		return hubSearchReportTemplates;
	}

	public static Hub<ReportTemplate> getNeedsTemplateReportTemplates() {
		return hubNeedsTemplateReportTemplates;
	}

	public static Hub<ReportInstance> getSearchReportInstances() {
		return hubSearchReportInstances;
	}

	public static Hub<PypeReportMessage> getSearchPypeReportMessages() {
		return hubSearchPypeReportMessages;
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
