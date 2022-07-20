// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.corptostore.model.oa.cs;

import java.util.*;

import com.corptostore.model.oa.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.AppServer;
import com.corptostore.model.oa.AppUser;
import com.corptostore.model.oa.AppUserError;
import com.corptostore.model.oa.AppUserLogin;
import com.corptostore.model.oa.Dashboard;
import com.corptostore.model.oa.Environment;
import com.corptostore.model.oa.Store;
import com.corptostore.model.oa.Tester;
import com.corptostore.model.oa.TesterResultType;
import com.corptostore.model.oa.TesterStepType;
import com.corptostore.model.oa.TransmitBatch;
import com.corptostore.model.oa.propertypath.AppUserPP;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;
import com.viaoa.object.*;
import com.viaoa.util.*;

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
    public static final String P_Environments = "Environments";
    public static final String P_TesterResultTypes = "TesterResultTypes";
    public static final String P_TesterStepTypes = "TesterStepTypes";
    // autoCreateOne
    public static final String P_CreateOneAppServerHub = "CreateOneAppServerHub";
    public static final String P_CreateOneDashboardHub = "CreateOneDashboardHub";
    // filters
    public static final String P_OpenTesters = "OpenTesters";
    // UI containers
    public static final String P_NewTest = "NewTest";
    public static final String P_Stores = "Stores";
    public static final String P_TransmitBatches = "TransmitBatches";
    public static final String P_AppUserLogins = "AppUserLogins";
    public static final String P_AppUserErrors = "AppUserErrors";
/*$$End: ServerRoot1 $$*/

	protected int id;
	/*$$Start: ServerRoot2 $$*/
    // lookups, preselects
    protected transient Hub<AppServer> hubAppServers;
    protected transient Hub<AppUser> hubAppUsers;
    protected transient Hub<Environment> hubEnvironments;
    protected transient Hub<TesterResultType> hubTesterResultTypes;
    protected transient Hub<TesterStepType> hubTesterStepTypes;
    // autoCreateOne
    protected transient Hub<AppServer> hubCreateOneAppServer;
    protected transient Hub<Dashboard> hubCreateOneDashboard;
    // filters
    protected transient Hub<Tester> hubOpenTesters;
    // UI containers
    protected transient Hub<Tester> hubNewTest;
    protected transient Hub<Store> hubStores;
    protected transient Hub<TransmitBatch> hubTransmitBatches;
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
    @OAMany(toClass = Environment.class, cascadeSave = true)
    public Hub<Environment> getEnvironments() {
        if (hubEnvironments == null) {
            hubEnvironments = (Hub<Environment>) super.getHub(P_Environments);
        }
        return hubEnvironments;
    }
    @OAMany(toClass = TesterResultType.class, sortProperty = TesterResultType.P_Type, cascadeSave = true)
    public Hub<TesterResultType> getTesterResultTypes() {
        if (hubTesterResultTypes == null) {
            hubTesterResultTypes = (Hub<TesterResultType>) super.getHub(P_TesterResultTypes);
        }
        return hubTesterResultTypes;
    }
    @OAMany(toClass = TesterStepType.class, sortProperty = TesterStepType.P_Type, cascadeSave = true)
    public Hub<TesterStepType> getTesterStepTypes() {
        if (hubTesterStepTypes == null) {
            hubTesterStepTypes = (Hub<TesterStepType>) super.getHub(P_TesterStepTypes);
        }
        return hubTesterStepTypes;
    }
    // autoCreatedOne
    @OAMany(toClass = AppServer.class, cascadeSave = true)
    public Hub<AppServer> getCreateOneAppServerHub() {
        if (hubCreateOneAppServer == null) {
            hubCreateOneAppServer = (Hub<AppServer>) super.getHub(P_CreateOneAppServerHub);
        }
        return hubCreateOneAppServer;
    }
    @OAMany(toClass = Dashboard.class, cascadeSave = true)
    public Hub<Dashboard> getCreateOneDashboardHub() {
        if (hubCreateOneDashboard == null) {
            hubCreateOneDashboard = (Hub<Dashboard>) super.getHub(P_CreateOneDashboardHub);
        }
        return hubCreateOneDashboard;
    }
    // filters
    @OAMany(toClass = Tester.class, cascadeSave = true)
    public Hub<Tester> getOpenTesters() {
        if (hubOpenTesters == null) {
            hubOpenTesters = (Hub<Tester>) super.getHub(P_OpenTesters);
        }
        return hubOpenTesters;
    }
    // UI containers
    @OAMany(toClass = Tester.class, cascadeSave = true)
    public Hub<Tester> getNewTest() {
        if (hubNewTest == null) {
            hubNewTest = (Hub<Tester>) super.getHub(P_NewTest);
        }
        return hubNewTest;
    }
    @OAMany(toClass = Store.class, sortProperty = Store.P_StoreNumber, cascadeSave = true)
    public Hub<Store> getStores() {
        if (hubStores == null) {
            hubStores = (Hub<Store>) super.getHub(P_Stores);
        }
        return hubStores;
    }
    @OAMany(toClass = TransmitBatch.class, sortProperty = TransmitBatch.P_TransmitBatchDate, cascadeSave = true)
    public Hub<TransmitBatch> getTransmitBatches() {
        if (hubTransmitBatches == null) {
            hubTransmitBatches = (Hub<TransmitBatch>) super.getHub(P_TransmitBatches);
        }
        return hubTransmitBatches;
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
