// Copied from OATemplate project by OABuilder 06/19/18 08:57 AM
package com.cdi.delegate;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.cdi.model.oa.*;
import com.cdi.model.oa.cs.ClientRoot;
import com.cdi.model.oa.cs.ServerRoot;
import com.cdi.model.oa.filter.*;
import com.viaoa.hub.*;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAString;

/**
 * This is used to access all of the Root level Hubs.  This is so that they 
 * will not have to be passed into and through the models.
 * After client login, the Hubs will be shared with the Hubs in the ServerRoot object from the server.
 * @author vincevia
 * 
 * @see ClientController#initializeClientModel
 */
public class ModelDelegate {
    private static Logger LOG = Logger.getLogger(ModelDelegate.class.getName());
    
    private static final Hub<AppUserLogin> hubLocalAppUserLogin = new Hub<AppUserLogin>(AppUserLogin.class); 
    private static final Hub<AppUser> hubLocalAppUser = new Hub<AppUser>(AppUser.class); 

    /*$$Start: ModelDelegate1 $$*/
    // lookups, preselects
    private static final Hub<AppServer> hubAppServers = new Hub<AppServer>(AppServer.class);
    private static final Hub<AppUser> hubAppUsers = new Hub<AppUser>(AppUser.class);
    private static final Hub<ColorCode> hubColorCodes = new Hub<ColorCode>(ColorCode.class);
    private static final Hub<Country> hubCountries = new Hub<Country>(Country.class);
    private static final Hub<ItemAddOn> hubItemAddOns = new Hub<ItemAddOn>(ItemAddOn.class);
    private static final Hub<ItemCategory> hubItemCategories = new Hub<ItemCategory>(ItemCategory.class);
    private static final Hub<ItemQuote> hubItemQuotes = new Hub<ItemQuote>(ItemQuote.class);
    private static final Hub<Pallet> hubPallets = new Hub<Pallet>(Pallet.class);
    private static final Hub<PriceCode> hubPriceCodes = new Hub<PriceCode>(PriceCode.class);
    private static final Hub<ProductionArea> hubProductionAreas = new Hub<ProductionArea>(ProductionArea.class);
    private static final Hub<Quickbook> hubQuickbooks = new Hub<Quickbook>(Quickbook.class);
    private static final Hub<Region> hubRegions = new Hub<Region>(Region.class);
    private static final Hub<SalesOrderSource> hubSalesOrderSources = new Hub<SalesOrderSource>(SalesOrderSource.class);
    private static final Hub<SalesOrderStatus> hubSalesOrderStatuses = new Hub<SalesOrderStatus>(SalesOrderStatus.class);
    private static final Hub<ScheduleType> hubScheduleTypes = new Hub<ScheduleType>(ScheduleType.class);
    private static final Hub<ServiceCode> hubServiceCodes = new Hub<ServiceCode>(ServiceCode.class);
    private static final Hub<TaxCode> hubTaxCodes = new Hub<TaxCode>(TaxCode.class);
    private static final Hub<Texture> hubTextures = new Hub<Texture>(Texture.class);
    private static final Hub<User> hubUsers = new Hub<User>(User.class);
    private static final Hub<WebPage> hubWebPages = new Hub<WebPage>(WebPage.class);
    // filters
    private static final Hub<Customer> hubActiveCustomers = new Hub<Customer>(Customer.class);
    private static final Hub<Item> hubActiveItems = new Hub<Item>(Item.class);
    private static final Hub<ItemAddOn> hubActiveItemAddOns = new Hub<ItemAddOn>(ItemAddOn.class);
    private static final Hub<Order> hubOpenOrders = new Hub<Order>(Order.class);
    private static final Hub<SalesOrder> hubOpenSalesOrders = new Hub<SalesOrder>(SalesOrder.class);
    // UI containers
    private static final Hub<SalesOrder> hubSearchSalesOrders = new Hub<SalesOrder>(SalesOrder.class);
    private static final Hub<SalesOrder> hubNewSalesOrders = new Hub<SalesOrder>(SalesOrder.class);
    private static final Hub<WorkOrder> hubOpenWorkOrders = new Hub<WorkOrder>(WorkOrder.class);
    private static final Hub<Order> hubInvalidOrders = new Hub<Order>(Order.class);
    private static final Hub<Order> hubSearchOrders1 = new Hub<Order>(Order.class);
    private static final Hub<Delivery> hubAllDeliveries = new Hub<Delivery>(Delivery.class);
    private static final Hub<Delivery> hubSearchDeliveries = new Hub<Delivery>(Delivery.class);
    private static final Hub<Truck> hubSearchTrucks1 = new Hub<Truck>(Truck.class);
    private static final Hub<Item> hubSearchItems = new Hub<Item>(Item.class);
    private static final Hub<Mold> hubSearchMolds = new Hub<Mold>(Mold.class);
    private static final Hub<Customer> hubSearchCustomers = new Hub<Customer>(Customer.class);
    private static final Hub<Contact> hubSearchContacts = new Hub<Contact>(Contact.class);
    private static final Hub<Customer> hubSearchCustomers1 = new Hub<Customer>(Customer.class);
    private static final Hub<ImageStore> hubSearchImageStores = new Hub<ImageStore>(ImageStore.class);
    private static final Hub<Item> hubSearchItems1 = new Hub<Item>(Item.class);
    private static final Hub<Mold> hubSearchMolds1 = new Hub<Mold>(Mold.class);
    private static final Hub<Order> hubSearchOrders = new Hub<Order>(Order.class);
    private static final Hub<OrderItem> hubSearchOrderItems = new Hub<OrderItem>(OrderItem.class);
    private static final Hub<SalesCustomer> hubSearchSalesCustomers = new Hub<SalesCustomer>(SalesCustomer.class);
    private static final Hub<SalesOrder> hubSearchSalesOrders1 = new Hub<SalesOrder>(SalesOrder.class);
    private static final Hub<Truck> hubSearchTrucks = new Hub<Truck>(Truck.class);
    private static final Hub<WorkOrder> hubSearchWorkOrders = new Hub<WorkOrder>(WorkOrder.class);
    private static final Hub<AppUserLogin> hubAppUserLogins = new Hub<AppUserLogin>(AppUserLogin.class);
    private static final Hub<AppUserError> hubAppUserErrors = new Hub<AppUserError>(AppUserError.class);
    /*$$End: ModelDelegate1 $$*/    
    
    
    public static void initialize(ServerRoot rootServer, ClientRoot rootClient) {
        LOG.fine("selecting data");

        /*$$Start: ModelDelegate2 $$*/
        // lookups, preselects
        setSharedHub(getAppServers(), rootServer.getAppServers());
        setSharedHub(getAppUsers(), rootServer.getAppUsers());
        setSharedHub(getColorCodes(), rootServer.getColorCodes());
        setSharedHub(getCountries(), rootServer.getCountries());
        setSharedHub(getItemAddOns(), rootServer.getItemAddOns());
        setSharedHub(getItemCategories(), rootServer.getItemCategories());
        setSharedHub(getItemQuotes(), rootServer.getItemQuotes());
        setSharedHub(getPallets(), rootServer.getPallets());
        setSharedHub(getPriceCodes(), rootServer.getPriceCodes());
        setSharedHub(getProductionAreas(), rootServer.getProductionAreas());
        setSharedHub(getQuickbooks(), rootServer.getQuickbooks());
        setSharedHub(getRegions(), rootServer.getRegions());
        setSharedHub(getSalesOrderSources(), rootServer.getSalesOrderSources());
        setSharedHub(getSalesOrderStatuses(), rootServer.getSalesOrderStatuses());
        setSharedHub(getScheduleTypes(), rootServer.getScheduleTypes());
        setSharedHub(getServiceCodes(), rootServer.getServiceCodes());
        setSharedHub(getTaxCodes(), rootServer.getTaxCodes());
        setSharedHub(getTextures(), rootServer.getTextures());
        setSharedHub(getUsers(), rootServer.getUsers());
        setSharedHub(getWebPages(), rootServer.getWebPages());
        // filters
        setSharedHub(getActiveCustomers(), rootServer.getActiveCustomers());
        setSharedHub(getActiveItems(), rootServer.getActiveItems());
        setSharedHub(getActiveItemAddOns(), rootServer.getActiveItemAddOns());
        setSharedHub(getOpenOrders(), rootServer.getOpenOrders());
        setSharedHub(getOpenSalesOrders(), rootServer.getOpenSalesOrders());
        // UI containers
        if (rootClient != null) setSharedHub(getSearchSalesOrders(), rootClient.getSearchSalesOrders());
        if (rootClient != null) setSharedHub(getNewSalesOrders(), rootClient.getNewSalesOrders());
        getOpenWorkOrders().setSharedHub(rootServer.getOpenWorkOrders());
        getInvalidOrders().setSharedHub(rootServer.getInvalidOrders());
        if (rootClient != null) setSharedHub(getSearchOrders1(), rootClient.getSearchOrders1());
        if (rootClient != null) setSharedHub(getAllDeliveries(), rootClient.getAllDeliveries());
        if (rootClient != null) setSharedHub(getSearchDeliveries(), rootClient.getSearchDeliveries());
        if (rootClient != null) setSharedHub(getSearchTrucks1(), rootClient.getSearchTrucks1());
        if (rootClient != null) setSharedHub(getSearchItems(), rootClient.getSearchItems());
        if (rootClient != null) setSharedHub(getSearchMolds(), rootClient.getSearchMolds());
        if (rootClient != null) setSharedHub(getSearchCustomers(), rootClient.getSearchCustomers());
        if (rootClient != null) setSharedHub(getSearchContacts(), rootClient.getSearchContacts());
        if (rootClient != null) setSharedHub(getSearchCustomers1(), rootClient.getSearchCustomers1());
        if (rootClient != null) setSharedHub(getSearchImageStores(), rootClient.getSearchImageStores());
        if (rootClient != null) setSharedHub(getSearchItems1(), rootClient.getSearchItems1());
        if (rootClient != null) setSharedHub(getSearchMolds1(), rootClient.getSearchMolds1());
        if (rootClient != null) setSharedHub(getSearchOrders(), rootClient.getSearchOrders());
        if (rootClient != null) setSharedHub(getSearchOrderItems(), rootClient.getSearchOrderItems());
        if (rootClient != null) setSharedHub(getSearchSalesCustomers(), rootClient.getSearchSalesCustomers());
        if (rootClient != null) setSharedHub(getSearchSalesOrders1(), rootClient.getSearchSalesOrders1());
        if (rootClient != null) setSharedHub(getSearchTrucks(), rootClient.getSearchTrucks());
        if (rootClient != null) setSharedHub(getSearchWorkOrders(), rootClient.getSearchWorkOrders());
        getAppUserLogins().setSharedHub(rootServer.getAppUserLogins());
        getAppUserErrors().setSharedHub(rootServer.getAppUserErrors());
        /*$$End: ModelDelegate2 $$*/    
    
        for (int i=0; i<120 ;i++) {
            if (aiExecutor.get() == 0) break;
            if (i > 5) {
                LOG.fine(i+"/120) waiting on initialize to finish sharing hubs");
            }
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {}
        }
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
            queExecutorService = null;
        }
        LOG.fine("completed selecting data");
    }

//qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq    
    
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
    public static Hub<AppServer> getAppServers() {
        return hubAppServers;
    }
    public static Hub<AppUser> getAppUsers() {
        return hubAppUsers;
    }
    public static Hub<ColorCode> getColorCodes() {
        return hubColorCodes;
    }
    public static Hub<Country> getCountries() {
        return hubCountries;
    }
    public static Hub<ItemAddOn> getItemAddOns() {
        return hubItemAddOns;
    }
    public static Hub<ItemCategory> getItemCategories() {
        return hubItemCategories;
    }
    public static Hub<ItemQuote> getItemQuotes() {
        return hubItemQuotes;
    }
    public static Hub<Pallet> getPallets() {
        return hubPallets;
    }
    public static Hub<PriceCode> getPriceCodes() {
        return hubPriceCodes;
    }
    public static Hub<ProductionArea> getProductionAreas() {
        return hubProductionAreas;
    }
    public static Hub<Quickbook> getQuickbooks() {
        return hubQuickbooks;
    }
    public static Hub<Region> getRegions() {
        return hubRegions;
    }
    public static Hub<SalesOrderSource> getSalesOrderSources() {
        return hubSalesOrderSources;
    }
    public static Hub<SalesOrderStatus> getSalesOrderStatuses() {
        return hubSalesOrderStatuses;
    }
    public static Hub<ScheduleType> getScheduleTypes() {
        return hubScheduleTypes;
    }
    public static Hub<ServiceCode> getServiceCodes() {
        return hubServiceCodes;
    }
    public static Hub<TaxCode> getTaxCodes() {
        return hubTaxCodes;
    }
    public static Hub<Texture> getTextures() {
        return hubTextures;
    }
    public static Hub<User> getUsers() {
        return hubUsers;
    }
    public static Hub<WebPage> getWebPages() {
        return hubWebPages;
    }
    public static Hub<Customer> getActiveCustomers() {
        return hubActiveCustomers;
    }
    public static Hub<Item> getActiveItems() {
        return hubActiveItems;
    }
    public static Hub<ItemAddOn> getActiveItemAddOns() {
        return hubActiveItemAddOns;
    }
    public static Hub<Order> getOpenOrders() {
        return hubOpenOrders;
    }
    public static Hub<SalesOrder> getOpenSalesOrders() {
        return hubOpenSalesOrders;
    }
    public static Hub<SalesOrder> getSearchSalesOrders() {
        return hubSearchSalesOrders;
    }
    public static Hub<SalesOrder> getNewSalesOrders() {
        return hubNewSalesOrders;
    }
    public static Hub<WorkOrder> getOpenWorkOrders() {
        return hubOpenWorkOrders;
    }
    public static Hub<Order> getInvalidOrders() {
        return hubInvalidOrders;
    }
    public static Hub<Order> getSearchOrders1() {
        return hubSearchOrders1;
    }
    public static Hub<Delivery> getAllDeliveries() {
        return hubAllDeliveries;
    }
    public static Hub<Delivery> getSearchDeliveries() {
        return hubSearchDeliveries;
    }
    public static Hub<Truck> getSearchTrucks1() {
        return hubSearchTrucks1;
    }
    public static Hub<Item> getSearchItems() {
        return hubSearchItems;
    }
    public static Hub<Mold> getSearchMolds() {
        return hubSearchMolds;
    }
    public static Hub<Customer> getSearchCustomers() {
        return hubSearchCustomers;
    }
    public static Hub<Contact> getSearchContacts() {
        return hubSearchContacts;
    }
    public static Hub<Customer> getSearchCustomers1() {
        return hubSearchCustomers1;
    }
    public static Hub<ImageStore> getSearchImageStores() {
        return hubSearchImageStores;
    }
    public static Hub<Item> getSearchItems1() {
        return hubSearchItems1;
    }
    public static Hub<Mold> getSearchMolds1() {
        return hubSearchMolds1;
    }
    public static Hub<Order> getSearchOrders() {
        return hubSearchOrders;
    }
    public static Hub<OrderItem> getSearchOrderItems() {
        return hubSearchOrderItems;
    }
    public static Hub<SalesCustomer> getSearchSalesCustomers() {
        return hubSearchSalesCustomers;
    }
    public static Hub<SalesOrder> getSearchSalesOrders1() {
        return hubSearchSalesOrders1;
    }
    public static Hub<Truck> getSearchTrucks() {
        return hubSearchTrucks;
    }
    public static Hub<WorkOrder> getSearchWorkOrders() {
        return hubSearchWorkOrders;
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
                }
                finally {
                    aiExecutor.decrementAndGet();
                }
            }
        });
    }

    // Custom
    public static User getLoginUser() {
        AppUser au = getLocalAppUser();
        if (au == null) return null;
        return au.getUser();
    }
    public static boolean isAdminUser() {
        User user = getLoginUser();
        if (user != null && user.getAdminAccess()) return true;
        
        AppUser au = getLocalAppUser();
        if (au == null) return false;
        return (au != null && au.getAdmin());
    }

}
