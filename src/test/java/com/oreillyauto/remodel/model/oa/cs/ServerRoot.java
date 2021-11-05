// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.oreillyauto.remodel.model.oa.cs;

import com.oreillyauto.remodel.model.oa.AppServer;
import com.oreillyauto.remodel.model.oa.AppUser;
import com.oreillyauto.remodel.model.oa.AppUserError;
import com.oreillyauto.remodel.model.oa.AppUserLogin;
import com.oreillyauto.remodel.model.oa.DataType;
import com.oreillyauto.remodel.model.oa.Database;
import com.oreillyauto.remodel.model.oa.DatabaseType;
import com.oreillyauto.remodel.model.oa.JavaType;
import com.oreillyauto.remodel.model.oa.Project;
import com.oreillyauto.remodel.model.oa.SqlType;
import com.oreillyauto.remodel.model.oa.propertypath.AppUserPP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubMerger;
import com.viaoa.object.OAObject;

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
    public static final String P_Databases = "Databases";
    public static final String P_DatabaseTypes = "DatabaseTypes";
    public static final String P_DataTypes = "DataTypes";
    public static final String P_JavaTypes = "JavaTypes";
    public static final String P_Projects = "Projects";
    public static final String P_SqlTypes = "SqlTypes";
    // autoCreateOne
    public static final String P_CreateOneAppServerHub = "CreateOneAppServerHub";
    // filters
    // UI containers
    public static final String P_AppUserLogins = "AppUserLogins";
    public static final String P_AppUserErrors = "AppUserErrors";
/*$$End: ServerRoot1 $$*/

	protected int id;
	/*$$Start: ServerRoot2 $$*/
    // lookups, preselects
    protected transient Hub<AppServer> hubAppServers;
    protected transient Hub<AppUser> hubAppUsers;
    protected transient Hub<Database> hubDatabases;
    protected transient Hub<DatabaseType> hubDatabaseTypes;
    protected transient Hub<DataType> hubDataTypes;
    protected transient Hub<JavaType> hubJavaTypes;
    protected transient Hub<Project> hubProjects;
    protected transient Hub<SqlType> hubSqlTypes;
    // autoCreateOne
    protected transient Hub<AppServer> hubCreateOneAppServer;
    // filters
    // UI containers
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
    @OAMany(toClass = Database.class, sortProperty = Database.P_Name, cascadeSave = true)
    public Hub<Database> getDatabases() {
        if (hubDatabases == null) {
            hubDatabases = (Hub<Database>) super.getHub(P_Databases);
        }
        return hubDatabases;
    }
    @OAMany(toClass = DatabaseType.class, cascadeSave = true)
    public Hub<DatabaseType> getDatabaseTypes() {
        if (hubDatabaseTypes == null) {
            hubDatabaseTypes = (Hub<DatabaseType>) super.getHub(P_DatabaseTypes);
        }
        return hubDatabaseTypes;
    }
    @OAMany(toClass = DataType.class, sortProperty = DataType.P_Seq, cascadeSave = true)
    public Hub<DataType> getDataTypes() {
        if (hubDataTypes == null) {
            hubDataTypes = (Hub<DataType>) super.getHub(P_DataTypes, DataType.P_Seq, true);
        }
        return hubDataTypes;
    }
    @OAMany(toClass = JavaType.class, sortProperty = JavaType.P_Seq, cascadeSave = true)
    public Hub<JavaType> getJavaTypes() {
        if (hubJavaTypes == null) {
            hubJavaTypes = (Hub<JavaType>) super.getHub(P_JavaTypes, JavaType.P_Seq, true);
        }
        return hubJavaTypes;
    }
    @OAMany(toClass = Project.class, sortProperty = Project.P_Seq, cascadeSave = true)
    public Hub<Project> getProjects() {
        if (hubProjects == null) {
            hubProjects = (Hub<Project>) super.getHub(P_Projects, Project.P_Seq, true);
        }
        return hubProjects;
    }
    @OAMany(toClass = SqlType.class, sortProperty = SqlType.P_Seq, cascadeSave = true)
    public Hub<SqlType> getSqlTypes() {
        if (hubSqlTypes == null) {
            hubSqlTypes = (Hub<SqlType>) super.getHub(P_SqlTypes, SqlType.P_Seq, true);
        }
        return hubSqlTypes;
    }
    // autoCreatedOne
    @OAMany(toClass = AppServer.class, cascadeSave = true)
    public Hub<AppServer> getCreateOneAppServerHub() {
        if (hubCreateOneAppServer == null) {
            hubCreateOneAppServer = (Hub<AppServer>) super.getHub(P_CreateOneAppServerHub);
        }
        return hubCreateOneAppServer;
    }
    // filters
    // UI containers
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
