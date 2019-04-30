// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.cdi.model.oa.cs;

import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;

/**
 * Root Object that is automatically updated between the Server and Clients.
 * ServerController will do the selects for these objects.
 * Model will share these hubs after the application is started.
 * */

@OAClass(
    useDataSource = false,
    displayProperty = "Id"
)
public class ServerRoot extends OAObject {
    private static final long serialVersionUID = 1L;

    public static final String PROPERTY_Id = "Id";
    public static final String P_Id = "Id";

    /*$$Start: ServerRoot1 $$*/
    // lookups, preselects, autoCreated
    public static final String P_AppServers = "AppServers";
    public static final String P_AppUsers = "AppUsers";
    public static final String P_ColorCodes = "ColorCodes";
    public static final String P_Countries = "Countries";
    public static final String P_ItemAddOns = "ItemAddOns";
    public static final String P_ItemCategories = "ItemCategories";
    public static final String P_ItemQuotes = "ItemQuotes";
    public static final String P_Pallets = "Pallets";
    public static final String P_PriceCodes = "PriceCodes";
    public static final String P_ProductionAreas = "ProductionAreas";
    public static final String P_Quickbooks = "Quickbooks";
    public static final String P_Regions = "Regions";
    public static final String P_SalesOrderSources = "SalesOrderSources";
    public static final String P_SalesOrderStatuses = "SalesOrderStatuses";
    public static final String P_ScheduleTypes = "ScheduleTypes";
    public static final String P_ServiceCodes = "ServiceCodes";
    public static final String P_TaxCodes = "TaxCodes";
    public static final String P_Textures = "Textures";
    public static final String P_Users = "Users";
    public static final String P_WebPages = "WebPages";
    // filters
    public static final String P_ActiveCustomers = "ActiveCustomers";
    public static final String P_ActiveItems = "ActiveItems";
    public static final String P_ActiveItemAddOns = "ActiveItemAddOns";
    public static final String P_OpenOrders = "OpenOrders";
    public static final String P_OpenSalesOrders = "OpenSalesOrders";
    // UI containers
    public static final String P_NewSalesOrders = "NewSalesOrders";
    public static final String P_OpenWorkOrders = "OpenWorkOrders";
    public static final String P_InvalidOrders = "InvalidOrders";
    public static final String P_AppUserLogins = "AppUserLogins";
    public static final String P_AppUserErrors = "AppUserErrors";
    /*$$End: ServerRoot1 $$*/

    protected int id;
    /*$$Start: ServerRoot2 $$*/
    // lookups, preselects, autoCreated
    protected transient Hub<AppServer> hubAppServers;
    protected transient Hub<AppUser> hubAppUsers;
    protected transient Hub<ColorCode> hubColorCodes;
    protected transient Hub<Country> hubCountries;
    protected transient Hub<ItemAddOn> hubItemAddOns;
    protected transient Hub<ItemCategory> hubItemCategories;
    protected transient Hub<ItemQuote> hubItemQuotes;
    protected transient Hub<Pallet> hubPallets;
    protected transient Hub<PriceCode> hubPriceCodes;
    protected transient Hub<ProductionArea> hubProductionAreas;
    protected transient Hub<Quickbook> hubQuickbooks;
    protected transient Hub<Region> hubRegions;
    protected transient Hub<SalesOrderSource> hubSalesOrderSources;
    protected transient Hub<SalesOrderStatus> hubSalesOrderStatuses;
    protected transient Hub<ScheduleType> hubScheduleTypes;
    protected transient Hub<ServiceCode> hubServiceCodes;
    protected transient Hub<TaxCode> hubTaxCodes;
    protected transient Hub<Texture> hubTextures;
    protected transient Hub<User> hubUsers;
    protected transient Hub<WebPage> hubWebPages;
    // filters
    protected transient Hub<Customer> hubActiveCustomers;
    protected transient Hub<Item> hubActiveItems;
    protected transient Hub<ItemAddOn> hubActiveItemAddOns;
    protected transient Hub<Order> hubOpenOrders;
    protected transient Hub<SalesOrder> hubOpenSalesOrders;
    // UI containers
    protected transient Hub<SalesOrder> hubNewSalesOrders;
    protected transient Hub<WorkOrder> hubOpenWorkOrders;
    protected transient Hub<Order> hubInvalidOrders;
    protected transient Hub<AppUserLogin> hubAppUserLogins;
    protected transient Hub<AppUserError> hubAppUserErrors;
    /*$$End: ServerRoot2 $$*/

    
	public ServerRoot() {
		setId(777);
	}

    @OAProperty(displayName = "Id")
    @OAId
	public int getId() {
		return id;
	}
	public void setId(int id) {
		int old = this.id;
		this.id = id;
		firePropertyChange(PROPERTY_Id, old, id);
	}

    /*$$Start: ServerRoot3 $$*/
    // lookups, preselects, autoCreated
    @OAMany(toClass = AppServer.class, cascadeSave = true)
    public Hub<AppServer> getAppServers() {
        if (hubAppServers == null) {
            hubAppServers = (Hub<AppServer>) super.getHub(P_AppServers);
        }
        return hubAppServers;
    }
    @OAMany(toClass = AppUser.class, cascadeSave = true)
    public Hub<AppUser> getAppUsers() {
        if (hubAppUsers == null) {
            hubAppUsers = (Hub<AppUser>) super.getHub(P_AppUsers);
        }
        return hubAppUsers;
    }
    @OAMany(toClass = ColorCode.class, sortProperty = ColorCode.P_Code, cascadeSave = true)
    public Hub<ColorCode> getColorCodes() {
        if (hubColorCodes == null) {
            hubColorCodes = (Hub<ColorCode>) super.getHub(P_ColorCodes);
        }
        return hubColorCodes;
    }
    @OAMany(toClass = Country.class, sortProperty = Country.P_Code, cascadeSave = true)
    public Hub<Country> getCountries() {
        if (hubCountries == null) {
            hubCountries = (Hub<Country>) super.getHub(P_Countries);
        }
        return hubCountries;
    }
    @OAMany(toClass = ItemAddOn.class, sortProperty = ItemAddOn.P_Code, cascadeSave = true)
    public Hub<ItemAddOn> getItemAddOns() {
        if (hubItemAddOns == null) {
            hubItemAddOns = (Hub<ItemAddOn>) super.getHub(P_ItemAddOns);
        }
        return hubItemAddOns;
    }
    @OAMany(toClass = ItemCategory.class, recursive = true, cascadeSave = true)
    public Hub<ItemCategory> getItemCategories() {
        if (hubItemCategories == null) {
            hubItemCategories = (Hub<ItemCategory>) super.getHub(P_ItemCategories);
            hubItemCategories.setRootHub();
        }
        return hubItemCategories;
    }
    @OAMany(toClass = ItemQuote.class, cascadeSave = true)
    public Hub<ItemQuote> getItemQuotes() {
        if (hubItemQuotes == null) {
            hubItemQuotes = (Hub<ItemQuote>) super.getHub(P_ItemQuotes);
        }
        return hubItemQuotes;
    }
    @OAMany(toClass = Pallet.class, sortProperty = Pallet.P_Seq, cascadeSave = true)
    public Hub<Pallet> getPallets() {
        if (hubPallets == null) {
            hubPallets = (Hub<Pallet>) super.getHub(P_Pallets, Pallet.P_Seq, true);
        }
        return hubPallets;
    }
    @OAMany(toClass = PriceCode.class, sortProperty = PriceCode.P_Name, cascadeSave = true)
    public Hub<PriceCode> getPriceCodes() {
        if (hubPriceCodes == null) {
            hubPriceCodes = (Hub<PriceCode>) super.getHub(P_PriceCodes);
        }
        return hubPriceCodes;
    }
    @OAMany(toClass = ProductionArea.class, sortProperty = ProductionArea.P_Code, cascadeSave = true)
    public Hub<ProductionArea> getProductionAreas() {
        if (hubProductionAreas == null) {
            hubProductionAreas = (Hub<ProductionArea>) super.getHub(P_ProductionAreas);
        }
        return hubProductionAreas;
    }
    @OAMany(toClass = Quickbook.class, cascadeSave = true)
    public Hub<Quickbook> getQuickbooks() {
        if (hubQuickbooks == null) {
            hubQuickbooks = (Hub<Quickbook>) super.getHub(P_Quickbooks);
        }
        return hubQuickbooks;
    }
    @OAMany(toClass = Region.class, sortProperty = Region.P_Code, cascadeSave = true)
    public Hub<Region> getRegions() {
        if (hubRegions == null) {
            hubRegions = (Hub<Region>) super.getHub(P_Regions);
        }
        return hubRegions;
    }
    @OAMany(toClass = SalesOrderSource.class, sortProperty = SalesOrderSource.P_Name, cascadeSave = true)
    public Hub<SalesOrderSource> getSalesOrderSources() {
        if (hubSalesOrderSources == null) {
            hubSalesOrderSources = (Hub<SalesOrderSource>) super.getHub(P_SalesOrderSources);
        }
        return hubSalesOrderSources;
    }
    @OAMany(toClass = SalesOrderStatus.class, sortProperty = SalesOrderStatus.P_Name, cascadeSave = true)
    public Hub<SalesOrderStatus> getSalesOrderStatuses() {
        if (hubSalesOrderStatuses == null) {
            hubSalesOrderStatuses = (Hub<SalesOrderStatus>) super.getHub(P_SalesOrderStatuses);
        }
        return hubSalesOrderStatuses;
    }
    @OAMany(toClass = ScheduleType.class, sortProperty = ScheduleType.P_Id, cascadeSave = true)
    public Hub<ScheduleType> getScheduleTypes() {
        if (hubScheduleTypes == null) {
            hubScheduleTypes = (Hub<ScheduleType>) super.getHub(P_ScheduleTypes);
        }
        return hubScheduleTypes;
    }
    @OAMany(toClass = ServiceCode.class, sortProperty = ServiceCode.P_Name, cascadeSave = true)
    public Hub<ServiceCode> getServiceCodes() {
        if (hubServiceCodes == null) {
            hubServiceCodes = (Hub<ServiceCode>) super.getHub(P_ServiceCodes);
        }
        return hubServiceCodes;
    }
    @OAMany(toClass = TaxCode.class, sortProperty = TaxCode.P_Code, cascadeSave = true)
    public Hub<TaxCode> getTaxCodes() {
        if (hubTaxCodes == null) {
            hubTaxCodes = (Hub<TaxCode>) super.getHub(P_TaxCodes);
        }
        return hubTaxCodes;
    }
    @OAMany(toClass = Texture.class, sortProperty = Texture.P_Code, cascadeSave = true)
    public Hub<Texture> getTextures() {
        if (hubTextures == null) {
            hubTextures = (Hub<Texture>) super.getHub(P_Textures);
        }
        return hubTextures;
    }
    @OAMany(toClass = User.class, sortProperty = User.P_LastName, cascadeSave = true)
    public Hub<User> getUsers() {
        if (hubUsers == null) {
            hubUsers = (Hub<User>) super.getHub(P_Users);
        }
        return hubUsers;
    }
    @OAMany(toClass = WebPage.class, sortProperty = WebPage.P_Seq, cascadeSave = true)
    public Hub<WebPage> getWebPages() {
        if (hubWebPages == null) {
            hubWebPages = (Hub<WebPage>) super.getHub(P_WebPages, WebPage.P_Seq, true);
        }
        return hubWebPages;
    }
    // filters
    @OAMany(toClass = Customer.class, sortProperty = Customer.P_Name, cascadeSave = true)
    public Hub<Customer> getActiveCustomers() {
        if (hubActiveCustomers == null) {
            hubActiveCustomers = (Hub<Customer>) super.getHub(P_ActiveCustomers);
        }
        return hubActiveCustomers;
    }
    @OAMany(toClass = Item.class, sortProperty = Item.P_Name, cascadeSave = true)
    public Hub<Item> getActiveItems() {
        if (hubActiveItems == null) {
            hubActiveItems = (Hub<Item>) super.getHub(P_ActiveItems);
        }
        return hubActiveItems;
    }
    @OAMany(toClass = ItemAddOn.class, sortProperty = ItemAddOn.P_Code, cascadeSave = true)
    public Hub<ItemAddOn> getActiveItemAddOns() {
        if (hubActiveItemAddOns == null) {
            hubActiveItemAddOns = (Hub<ItemAddOn>) super.getHub(P_ActiveItemAddOns);
        }
        return hubActiveItemAddOns;
    }
    @OAMany(toClass = Order.class, sortProperty = Order.P_SalesOrderNumber, cascadeSave = true)
    public Hub<Order> getOpenOrders() {
        if (hubOpenOrders == null) {
            hubOpenOrders = (Hub<Order>) super.getHub(P_OpenOrders);
        }
        return hubOpenOrders;
    }
    @OAMany(toClass = SalesOrder.class, sortProperty = SalesOrder.P_Date, cascadeSave = true)
    public Hub<SalesOrder> getOpenSalesOrders() {
        if (hubOpenSalesOrders == null) {
            hubOpenSalesOrders = (Hub<SalesOrder>) super.getHub(P_OpenSalesOrders);
        }
        return hubOpenSalesOrders;
    }
    // UI containers
    @OAMany(toClass = SalesOrder.class, sortProperty = SalesOrder.P_Date, cascadeSave = true)
    public Hub<SalesOrder> getNewSalesOrders() {
        if (hubNewSalesOrders == null) {
            hubNewSalesOrders = (Hub<SalesOrder>) super.getHub(P_NewSalesOrders);
        }
        return hubNewSalesOrders;
    }
    @OAMany(toClass = WorkOrder.class, cascadeSave = true, isProcessed = true)
    public Hub<WorkOrder> getOpenWorkOrders() {
        if (hubOpenWorkOrders == null) {
            hubOpenWorkOrders = (Hub<WorkOrder>) super.getHub(P_OpenWorkOrders);
        }
        return hubOpenWorkOrders;
    }
    @OAMany(toClass = Order.class, sortProperty = Order.P_SalesOrderNumber, cascadeSave = true, isProcessed = true)
    public Hub<Order> getInvalidOrders() {
        if (hubInvalidOrders == null) {
            hubInvalidOrders = (Hub<Order>) super.getHub(P_InvalidOrders);
        }
        return hubInvalidOrders;
    }
    @OAMany(toClass = AppUserLogin.class, cascadeSave = true, isProcessed = true)
    public Hub<AppUserLogin> getAppUserLogins() {
        if (hubAppUserLogins == null) {
            hubAppUserLogins = (Hub<AppUserLogin>) super.getHub(P_AppUserLogins);
        }
        return hubAppUserLogins;
    }
    @OAMany(toClass = AppUserError.class, isCalculated = true, cascadeSave = true)
    public Hub<AppUserError> getAppUserErrors() {
        if (hubAppUserErrors == null) {
            hubAppUserErrors = (Hub<AppUserError>) super.getHub(P_AppUserErrors);
            String pp = AppUserPP.appUserLogins().appUserErrors().pp;
            HubMerger hm = new HubMerger(this.getAppUsers(), hubAppUserErrors, pp, false, true);
        }
        return hubAppUserErrors;
    }
    /*$$End: ServerRoot3 $$*/
}

