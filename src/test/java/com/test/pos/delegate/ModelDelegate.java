// Copied from OATemplate project by OABuilder 02/13/19 10:11 AM
package com.test.pos.delegate;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.test.pos.model.oa.*;
import com.test.pos.model.oa.cs.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.session.OASessionUser;
import com.viaoa.hub.*;
import com.viaoa.object.OAObject;

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
    private static final Hub<BarcodeType> hubBarcodeTypes = new Hub<BarcodeType>(BarcodeType.class);
    private static final Hub<CurrencyType> hubCurrencyTypes = new Hub<CurrencyType>(CurrencyType.class);
    private static final Hub<DeliveryService> hubDeliveryServices = new Hub<DeliveryService>(DeliveryService.class);
    private static final Hub<DistCenter> hubDistCenters = new Hub<DistCenter>(DistCenter.class);
    private static final Hub<ItemCategory> hubItemCategories = new Hub<ItemCategory>(ItemCategory.class);
    private static final Hub<ItemLine> hubItemLines = new Hub<ItemLine>(ItemLine.class);
    private static final Hub<ItemOptionType> hubItemOptionTypes = new Hub<ItemOptionType>(ItemOptionType.class);
    private static final Hub<ItemPackType> hubItemPackTypes = new Hub<ItemPackType>(ItemPackType.class);
    private static final Hub<MeasureType> hubMeasureTypes = new Hub<MeasureType>(MeasureType.class);
    private static final Hub<ReportClass> hubReportClasses = new Hub<ReportClass>(ReportClass.class);
    private static final Hub<RewardType> hubRewardTypes = new Hub<RewardType>(RewardType.class);
    private static final Hub<TMPermission> hubTMPermissions = new Hub<TMPermission>(TMPermission.class);
    private static final Hub<VertexTaxCode> hubVertexTaxCodes = new Hub<VertexTaxCode>(VertexTaxCode.class);
    // autoCreateOne
    private static final Hub<AppServer> hubCreateOneAppServer = new Hub<AppServer>(AppServer.class);
    private static final Hub<CronProcess> hubCreateOneCronProcess = new Hub<CronProcess>(CronProcess.class);
    private static final Hub<CustomerConnector> hubCreateOneCustomerConnector = new Hub<CustomerConnector>(CustomerConnector.class);
    private static final Hub<Demo> hubCreateOneDemo = new Hub<Demo>(Demo.class);
    private static final Hub<InventoryConnector> hubCreateOneInventoryConnector = new Hub<InventoryConnector>(InventoryConnector.class);
    private static final Hub<NewNetPriceCalculater> hubCreateOneNewNetPriceCalculater = new Hub<NewNetPriceCalculater>(NewNetPriceCalculater.class);
    private static final Hub<OPPConnector> hubCreateOneOPPConnector = new Hub<OPPConnector>(OPPConnector.class);
    private static final Hub<Store> hubCreateOneStore = new Hub<Store>(Store.class);
    private static final Hub<VertexConnector> hubCreateOneVertexConnector = new Hub<VertexConnector>(VertexConnector.class);
    private static final Hub<VinLookup> hubCreateOneVinLookup = new Hub<VinLookup>(VinLookup.class);
    // filters
    private static final Hub<Invoice> hubOpenInvoices = new Hub<Invoice>(Invoice.class);
    private static final Hub<ItemRestriction> hubInvalidRuleSearchValueItemRestrictions = new Hub<ItemRestriction>(ItemRestriction.class);
    // UI containers
    private static final Hub<RegisterSession> hubOpenRegisterSessions = new Hub<RegisterSession>(RegisterSession.class);
    private static final Hub<Invoice> hubSearchInvoices = new Hub<Invoice>(Invoice.class);
    private static final Hub<Customer> hubSearchCustomers = new Hub<Customer>(Customer.class);
    private static final Hub<Item> hubSearchItems = new Hub<Item>(Item.class);
    private static final Hub<BankDeposit> hubSearchBankDeposits1 = new Hub<BankDeposit>(BankDeposit.class);
    private static final Hub<AppUserLogin> hubAppUserLogins = new Hub<AppUserLogin>(AppUserLogin.class);
    private static final Hub<AppUserError> hubAppUserErrors = new Hub<AppUserError>(AppUserError.class);
/*$$End: ModelDelegate1 $$*/

	public static void initialize(ServerRoot rootServer, ClientRoot rootClient) {
		LOG.fine("selecting data");

		/*$$Start: ModelDelegate2 $$*/
        // lookups, preselects
        setSharedHub(getAppUsers(), rootServer.getAppUsers());
        setSharedHub(getBarcodeTypes(), rootServer.getBarcodeTypes());
        setSharedHub(getCurrencyTypes(), rootServer.getCurrencyTypes());
        setSharedHub(getDeliveryServices(), rootServer.getDeliveryServices());
        setSharedHub(getDistCenters(), rootServer.getDistCenters());
        setSharedHub(getItemCategories(), rootServer.getItemCategories());
        setSharedHub(getItemLines(), rootServer.getItemLines());
        setSharedHub(getItemOptionTypes(), rootServer.getItemOptionTypes());
        setSharedHub(getItemPackTypes(), rootServer.getItemPackTypes());
        setSharedHub(getMeasureTypes(), rootServer.getMeasureTypes());
        setSharedHub(getReportClasses(), rootServer.getReportClasses());
        setSharedHub(getRewardTypes(), rootServer.getRewardTypes());
        setSharedHub(getTMPermissions(), rootServer.getTMPermissions());
        setSharedHub(getVertexTaxCodes(), rootServer.getVertexTaxCodes());
        // autoCreateOne
        setSharedHub(getCreateOneAppServerHub(), rootServer.getCreateOneAppServerHub());
        setSharedHub(getCreateOneCronProcessHub(), rootServer.getCreateOneCronProcessHub());
        setSharedHub(getCreateOneCustomerConnectorHub(), rootServer.getCreateOneCustomerConnectorHub());
        setSharedHub(getCreateOneDemoHub(), rootServer.getCreateOneDemoHub());
        setSharedHub(getCreateOneInventoryConnectorHub(), rootServer.getCreateOneInventoryConnectorHub());
        setSharedHub(getCreateOneNewNetPriceCalculaterHub(), rootServer.getCreateOneNewNetPriceCalculaterHub());
        setSharedHub(getCreateOneOPPConnectorHub(), rootServer.getCreateOneOPPConnectorHub());
        setSharedHub(getCreateOneStoreHub(), rootServer.getCreateOneStoreHub());
        setSharedHub(getCreateOneVertexConnectorHub(), rootServer.getCreateOneVertexConnectorHub());
        setSharedHub(getCreateOneVinLookupHub(), rootServer.getCreateOneVinLookupHub());
        // filters
        setSharedHub(getOpenInvoices(), rootServer.getOpenInvoices());
        setSharedHub(getInvalidRuleSearchValueItemRestrictions(), rootServer.getInvalidRuleSearchValueItemRestrictions());
        // UI containers
        getOpenRegisterSessions().setSharedHub(rootServer.getOpenRegisterSessions());
        if (rootClient != null) setSharedHub(getSearchInvoices(), rootClient.getSearchInvoices());
        if (rootClient != null) setSharedHub(getSearchCustomers(), rootClient.getSearchCustomers());
        if (rootClient != null) setSharedHub(getSearchItems(), rootClient.getSearchItems());
        if (rootClient != null) setSharedHub(getSearchBankDeposits1(), rootClient.getSearchBankDeposits1());
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

//qqqqqqqqqqqqqqqqq Custom
	public static TeamMember getCurrentTeamMember() {
		Hub<AppUser> hubAppUser = (Hub<AppUser>) OARuntime.oa(TeamMember.class).modelUser().getCurrent();
		
		AppUser appUser = hubAppUser.getAO();
		
		if (appUser == null) return null;
		TeamMember tm = appUser.getTeamMember(); 
		return tm;
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
    public static Hub<BarcodeType> getBarcodeTypes() {
        return hubBarcodeTypes;
    }
    public static Hub<CurrencyType> getCurrencyTypes() {
        return hubCurrencyTypes;
    }
    public static Hub<DeliveryService> getDeliveryServices() {
        return hubDeliveryServices;
    }
    public static Hub<DistCenter> getDistCenters() {
        return hubDistCenters;
    }
    public static Hub<ItemCategory> getItemCategories() {
        return hubItemCategories;
    }
    public static Hub<ItemLine> getItemLines() {
        return hubItemLines;
    }
    public static Hub<ItemOptionType> getItemOptionTypes() {
        return hubItemOptionTypes;
    }
    public static Hub<ItemPackType> getItemPackTypes() {
        return hubItemPackTypes;
    }
    public static Hub<MeasureType> getMeasureTypes() {
        return hubMeasureTypes;
    }
    public static Hub<ReportClass> getReportClasses() {
        return hubReportClasses;
    }
    public static Hub<RewardType> getRewardTypes() {
        return hubRewardTypes;
    }
    public static Hub<TMPermission> getTMPermissions() {
        return hubTMPermissions;
    }
    public static Hub<VertexTaxCode> getVertexTaxCodes() {
        return hubVertexTaxCodes;
    }
    // autoCreateOne
    public static Hub<AppServer> getCreateOneAppServerHub() {
        return hubCreateOneAppServer;
    }
    public static AppServer getAppServer() {
        return hubCreateOneAppServer.getAt(0);
    }
    public static Hub<CronProcess> getCreateOneCronProcessHub() {
        return hubCreateOneCronProcess;
    }
    public static CronProcess getCronProcess() {
        return hubCreateOneCronProcess.getAt(0);
    }
    public static Hub<CustomerConnector> getCreateOneCustomerConnectorHub() {
        return hubCreateOneCustomerConnector;
    }
    public static CustomerConnector getCustomerConnector() {
        return hubCreateOneCustomerConnector.getAt(0);
    }
    public static Hub<Demo> getCreateOneDemoHub() {
        return hubCreateOneDemo;
    }
    public static Demo getDemo() {
        return hubCreateOneDemo.getAt(0);
    }
    public static Hub<InventoryConnector> getCreateOneInventoryConnectorHub() {
        return hubCreateOneInventoryConnector;
    }
    public static InventoryConnector getInventoryConnector() {
        return hubCreateOneInventoryConnector.getAt(0);
    }
    public static Hub<NewNetPriceCalculater> getCreateOneNewNetPriceCalculaterHub() {
        return hubCreateOneNewNetPriceCalculater;
    }
    public static NewNetPriceCalculater getNewNetPriceCalculater() {
        return hubCreateOneNewNetPriceCalculater.getAt(0);
    }
    public static Hub<OPPConnector> getCreateOneOPPConnectorHub() {
        return hubCreateOneOPPConnector;
    }
    public static OPPConnector getOPPConnector() {
        return hubCreateOneOPPConnector.getAt(0);
    }
    public static Hub<Store> getCreateOneStoreHub() {
        return hubCreateOneStore;
    }
    public static Store getStore() {
        return hubCreateOneStore.getAt(0);
    }
    public static Hub<VertexConnector> getCreateOneVertexConnectorHub() {
        return hubCreateOneVertexConnector;
    }
    public static VertexConnector getVertexConnector() {
        return hubCreateOneVertexConnector.getAt(0);
    }
    public static Hub<VinLookup> getCreateOneVinLookupHub() {
        return hubCreateOneVinLookup;
    }
    public static VinLookup getVinLookup() {
        return hubCreateOneVinLookup.getAt(0);
    }
    public static Hub<Invoice> getOpenInvoices() {
        return hubOpenInvoices;
    }
    public static Hub<ItemRestriction> getInvalidRuleSearchValueItemRestrictions() {
        return hubInvalidRuleSearchValueItemRestrictions;
    }
    public static Hub<RegisterSession> getOpenRegisterSessions() {
        return hubOpenRegisterSessions;
    }
    public static Hub<Invoice> getSearchInvoices() {
        return hubSearchInvoices;
    }
    public static Hub<Customer> getSearchCustomers() {
        return hubSearchCustomers;
    }
    public static Hub<Item> getSearchItems() {
        return hubSearchItems;
    }
    public static Hub<BankDeposit> getSearchBankDeposits1() {
        return hubSearchBankDeposits1;
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
		//qqqqqqq HubAODelegate.warnOnSettingAO(h1);
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
