// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.test.pos.model.oa.cs;

import java.util.*;

import com.test.pos.model.oa.*;
import com.test.pos.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.object.*;

/**
 * Root Object that is automatically updated between the Server and Clients. ServerController will do the selects for these objects. Model
 * will share these hubs after the application is started.
 */

@OAClass(useDataSource = false, displayProperty = "Id")
public class ServerRoot extends OAObject {
	private static final long serialVersionUID = 1L;

	public static final String PROPERTY_Id = "Id";
	public static final String P_Id = "Id";

	/*$$Start: ServerRoot1 $$*/
    // lookups, preselects
    public static final String P_AppServers = "AppServers";
    public static final String P_AppUsers = "AppUsers";
    public static final String P_BarcodeTypes = "BarcodeTypes";
    public static final String P_CurrencyTypes = "CurrencyTypes";
    public static final String P_DeliveryServices = "DeliveryServices";
    public static final String P_DistCenters = "DistCenters";
    public static final String P_ItemCategories = "ItemCategories";
    public static final String P_ItemLines = "ItemLines";
    public static final String P_ItemOptionTypes = "ItemOptionTypes";
    public static final String P_ItemPackTypes = "ItemPackTypes";
    public static final String P_MeasureTypes = "MeasureTypes";
    public static final String P_ReportClasses = "ReportClasses";
    public static final String P_RewardTypes = "RewardTypes";
    public static final String P_TMPermissions = "TMPermissions";
    public static final String P_VertexTaxCodes = "VertexTaxCodes";
    // autoCreateOne
    public static final String P_CreateOneAppServerHub = "CreateOneAppServerHub";
    public static final String P_CreateOneCronProcessHub = "CreateOneCronProcessHub";
    public static final String P_CreateOneCustomerConnectorHub = "CreateOneCustomerConnectorHub";
    public static final String P_CreateOneDemoHub = "CreateOneDemoHub";
    public static final String P_CreateOneInventoryConnectorHub = "CreateOneInventoryConnectorHub";
    public static final String P_CreateOneNewNetPriceCalculaterHub = "CreateOneNewNetPriceCalculaterHub";
    public static final String P_CreateOneOPPConnectorHub = "CreateOneOPPConnectorHub";
    public static final String P_CreateOneStoreHub = "CreateOneStoreHub";
    public static final String P_CreateOneVertexConnectorHub = "CreateOneVertexConnectorHub";
    public static final String P_CreateOneVinLookupHub = "CreateOneVinLookupHub";
    // filters
    public static final String P_OpenInvoices = "OpenInvoices";
    public static final String P_InvalidRuleSearchValueItemRestrictions = "InvalidRuleSearchValueItemRestrictions";
    // UI containers
    public static final String P_OpenRegisterSessions = "OpenRegisterSessions";
    public static final String P_AppUserLogins = "AppUserLogins";
    public static final String P_AppUserErrors = "AppUserErrors";
/*$$End: ServerRoot1 $$*/

	protected int id;
	/*$$Start: ServerRoot2 $$*/
    // lookups, preselects
    protected transient Hub<AppServer> hubAppServers;
    protected transient Hub<AppUser> hubAppUsers;
    protected transient Hub<BarcodeType> hubBarcodeTypes;
    protected transient Hub<CurrencyType> hubCurrencyTypes;
    protected transient Hub<DeliveryService> hubDeliveryServices;
    protected transient Hub<DistCenter> hubDistCenters;
    protected transient Hub<ItemCategory> hubItemCategories;
    protected transient Hub<ItemLine> hubItemLines;
    protected transient Hub<ItemOptionType> hubItemOptionTypes;
    protected transient Hub<ItemPackType> hubItemPackTypes;
    protected transient Hub<MeasureType> hubMeasureTypes;
    protected transient Hub<ReportClass> hubReportClasses;
    protected transient Hub<RewardType> hubRewardTypes;
    protected transient Hub<TMPermission> hubTMPermissions;
    protected transient Hub<VertexTaxCode> hubVertexTaxCodes;
    // autoCreateOne
    protected transient Hub<AppServer> hubCreateOneAppServer;
    protected transient Hub<CronProcess> hubCreateOneCronProcess;
    protected transient Hub<CustomerConnector> hubCreateOneCustomerConnector;
    protected transient Hub<Demo> hubCreateOneDemo;
    protected transient Hub<InventoryConnector> hubCreateOneInventoryConnector;
    protected transient Hub<NewNetPriceCalculater> hubCreateOneNewNetPriceCalculater;
    protected transient Hub<OPPConnector> hubCreateOneOPPConnector;
    protected transient Hub<Store> hubCreateOneStore;
    protected transient Hub<VertexConnector> hubCreateOneVertexConnector;
    protected transient Hub<VinLookup> hubCreateOneVinLookup;
    // filters
    protected transient Hub<Invoice> hubOpenInvoices;
    protected transient Hub<ItemRestriction> hubInvalidRuleSearchValueItemRestrictions;
    // UI containers
    protected transient Hub<RegisterSession> hubOpenRegisterSessions;
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
    // lookups, preselects
    @OAMany(toClass = AppServer.class, cascadeSave = true, isProcessed = true)
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
    @OAMany(toClass = BarcodeType.class, cascadeSave = true)
    public Hub<BarcodeType> getBarcodeTypes() {
        if (hubBarcodeTypes == null) {
            hubBarcodeTypes = (Hub<BarcodeType>) super.getHub(P_BarcodeTypes);
        }
        return hubBarcodeTypes;
    }
    @OAMany(toClass = CurrencyType.class, cascadeSave = true)
    public Hub<CurrencyType> getCurrencyTypes() {
        if (hubCurrencyTypes == null) {
            hubCurrencyTypes = (Hub<CurrencyType>) super.getHub(P_CurrencyTypes);
        }
        return hubCurrencyTypes;
    }
    @OAMany(toClass = DeliveryService.class, cascadeSave = true)
    public Hub<DeliveryService> getDeliveryServices() {
        if (hubDeliveryServices == null) {
            hubDeliveryServices = (Hub<DeliveryService>) super.getHub(P_DeliveryServices);
        }
        return hubDeliveryServices;
    }
    @OAMany(toClass = DistCenter.class, cascadeSave = true)
    public Hub<DistCenter> getDistCenters() {
        if (hubDistCenters == null) {
            hubDistCenters = (Hub<DistCenter>) super.getHub(P_DistCenters);
        }
        return hubDistCenters;
    }
    @OAMany(toClass = ItemCategory.class, recursive = true, cascadeSave = true)
    public Hub<ItemCategory> getItemCategories() {
        if (hubItemCategories == null) {
            hubItemCategories = (Hub<ItemCategory>) super.getHub(P_ItemCategories);
            hubItemCategories.setRootHub();
        }
        return hubItemCategories;
    }
    @OAMany(toClass = ItemLine.class, sortProperty = ItemLine.P_Seq, cascadeSave = true)
    public Hub<ItemLine> getItemLines() {
        if (hubItemLines == null) {
            hubItemLines = (Hub<ItemLine>) super.getHub(P_ItemLines, ItemLine.P_Seq, true);
        }
        return hubItemLines;
    }
    @OAMany(toClass = ItemOptionType.class, cascadeSave = true)
    public Hub<ItemOptionType> getItemOptionTypes() {
        if (hubItemOptionTypes == null) {
            hubItemOptionTypes = (Hub<ItemOptionType>) super.getHub(P_ItemOptionTypes);
        }
        return hubItemOptionTypes;
    }
    @OAMany(toClass = ItemPackType.class, cascadeSave = true)
    public Hub<ItemPackType> getItemPackTypes() {
        if (hubItemPackTypes == null) {
            hubItemPackTypes = (Hub<ItemPackType>) super.getHub(P_ItemPackTypes);
        }
        return hubItemPackTypes;
    }
    @OAMany(toClass = MeasureType.class, cascadeSave = true)
    public Hub<MeasureType> getMeasureTypes() {
        if (hubMeasureTypes == null) {
            hubMeasureTypes = (Hub<MeasureType>) super.getHub(P_MeasureTypes);
        }
        return hubMeasureTypes;
    }
    @OAMany(toClass = ReportClass.class, cascadeSave = true, isProcessed = true)
    public Hub<ReportClass> getReportClasses() {
        if (hubReportClasses == null) {
            hubReportClasses = (Hub<ReportClass>) super.getHub(P_ReportClasses);
        }
        return hubReportClasses;
    }
    @OAMany(toClass = RewardType.class, cascadeSave = true)
    public Hub<RewardType> getRewardTypes() {
        if (hubRewardTypes == null) {
            hubRewardTypes = (Hub<RewardType>) super.getHub(P_RewardTypes);
        }
        return hubRewardTypes;
    }
    @OAMany(toClass = TMPermission.class, cascadeSave = true)
    public Hub<TMPermission> getTMPermissions() {
        if (hubTMPermissions == null) {
            hubTMPermissions = (Hub<TMPermission>) super.getHub(P_TMPermissions);
        }
        return hubTMPermissions;
    }
    @OAMany(toClass = VertexTaxCode.class, cascadeSave = true)
    public Hub<VertexTaxCode> getVertexTaxCodes() {
        if (hubVertexTaxCodes == null) {
            hubVertexTaxCodes = (Hub<VertexTaxCode>) super.getHub(P_VertexTaxCodes);
        }
        return hubVertexTaxCodes;
    }
    // autoCreatedOne
    @OAMany(toClass = AppServer.class, cascadeSave = true)
    public Hub<AppServer> getCreateOneAppServerHub() {
        if (hubCreateOneAppServer == null) {
            hubCreateOneAppServer = (Hub<AppServer>) super.getHub(P_CreateOneAppServerHub);
        }
        return hubCreateOneAppServer;
    }
    @OAMany(toClass = CronProcess.class, cascadeSave = true)
    public Hub<CronProcess> getCreateOneCronProcessHub() {
        if (hubCreateOneCronProcess == null) {
            hubCreateOneCronProcess = (Hub<CronProcess>) super.getHub(P_CreateOneCronProcessHub);
        }
        return hubCreateOneCronProcess;
    }
    @OAMany(toClass = CustomerConnector.class, cascadeSave = true)
    public Hub<CustomerConnector> getCreateOneCustomerConnectorHub() {
        if (hubCreateOneCustomerConnector == null) {
            hubCreateOneCustomerConnector = (Hub<CustomerConnector>) super.getHub(P_CreateOneCustomerConnectorHub);
        }
        return hubCreateOneCustomerConnector;
    }
    @OAMany(toClass = Demo.class, cascadeSave = true)
    public Hub<Demo> getCreateOneDemoHub() {
        if (hubCreateOneDemo == null) {
            hubCreateOneDemo = (Hub<Demo>) super.getHub(P_CreateOneDemoHub);
        }
        return hubCreateOneDemo;
    }
    @OAMany(toClass = InventoryConnector.class, cascadeSave = true)
    public Hub<InventoryConnector> getCreateOneInventoryConnectorHub() {
        if (hubCreateOneInventoryConnector == null) {
            hubCreateOneInventoryConnector = (Hub<InventoryConnector>) super.getHub(P_CreateOneInventoryConnectorHub);
        }
        return hubCreateOneInventoryConnector;
    }
    @OAMany(toClass = NewNetPriceCalculater.class, cascadeSave = true)
    public Hub<NewNetPriceCalculater> getCreateOneNewNetPriceCalculaterHub() {
        if (hubCreateOneNewNetPriceCalculater == null) {
            hubCreateOneNewNetPriceCalculater = (Hub<NewNetPriceCalculater>) super.getHub(P_CreateOneNewNetPriceCalculaterHub);
        }
        return hubCreateOneNewNetPriceCalculater;
    }
    @OAMany(toClass = OPPConnector.class, cascadeSave = true)
    public Hub<OPPConnector> getCreateOneOPPConnectorHub() {
        if (hubCreateOneOPPConnector == null) {
            hubCreateOneOPPConnector = (Hub<OPPConnector>) super.getHub(P_CreateOneOPPConnectorHub);
        }
        return hubCreateOneOPPConnector;
    }
    @OAMany(toClass = Store.class, cascadeSave = true)
    public Hub<Store> getCreateOneStoreHub() {
        if (hubCreateOneStore == null) {
            hubCreateOneStore = (Hub<Store>) super.getHub(P_CreateOneStoreHub);
        }
        return hubCreateOneStore;
    }
    @OAMany(toClass = VertexConnector.class, cascadeSave = true)
    public Hub<VertexConnector> getCreateOneVertexConnectorHub() {
        if (hubCreateOneVertexConnector == null) {
            hubCreateOneVertexConnector = (Hub<VertexConnector>) super.getHub(P_CreateOneVertexConnectorHub);
        }
        return hubCreateOneVertexConnector;
    }
    @OAMany(toClass = VinLookup.class, cascadeSave = true)
    public Hub<VinLookup> getCreateOneVinLookupHub() {
        if (hubCreateOneVinLookup == null) {
            hubCreateOneVinLookup = (Hub<VinLookup>) super.getHub(P_CreateOneVinLookupHub);
        }
        return hubCreateOneVinLookup;
    }
    // filters
    @OAMany(toClass = Invoice.class, cascadeSave = true)
    public Hub<Invoice> getOpenInvoices() {
        if (hubOpenInvoices == null) {
            hubOpenInvoices = (Hub<Invoice>) super.getHub(P_OpenInvoices);
        }
        return hubOpenInvoices;
    }
    @OAMany(toClass = ItemRestriction.class, cascadeSave = true)
    public Hub<ItemRestriction> getInvalidRuleSearchValueItemRestrictions() {
        if (hubInvalidRuleSearchValueItemRestrictions == null) {
            hubInvalidRuleSearchValueItemRestrictions = (Hub<ItemRestriction>) super.getHub(P_InvalidRuleSearchValueItemRestrictions);
        }
        return hubInvalidRuleSearchValueItemRestrictions;
    }
    // UI containers
    @OAMany(toClass = RegisterSession.class, isCalculated = true, cascadeSave = true)
    public Hub<RegisterSession> getOpenRegisterSessions() {
        if (hubOpenRegisterSessions == null) {
            hubOpenRegisterSessions = (Hub<RegisterSession>) super.getHub(P_OpenRegisterSessions);
            String pp = StorePP.registers().registerSessions().openFilter().pp;
            HubMerger hm = new HubMerger(this.getCreateOneStoreHub(), hubOpenRegisterSessions, pp, false, true);
        }
        return hubOpenRegisterSessions;
    }
    @OAMany(toClass = AppUserLogin.class, isCalculated = true, cascadeSave = true)
    public Hub<AppUserLogin> getAppUserLogins() {
        if (hubAppUserLogins == null) {
            hubAppUserLogins = (Hub<AppUserLogin>) super.getHub(P_AppUserLogins);
            String pp = AppUserPP.appUserLogins().lastDayFilter().pp;
            HubMerger hm = new HubMerger(this.getAppUsers(), hubAppUserLogins, pp, false, true);
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
