// Copied from OATemplate project by OABuilder 02/13/19 10:11 AM
package com.remodel.delegate;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.remodel.model.oa.AppServer;
import com.remodel.model.oa.AppUser;
import com.remodel.model.oa.AppUserError;
import com.remodel.model.oa.AppUserLogin;
import com.remodel.model.oa.Column;
import com.remodel.model.oa.DataType;
import com.remodel.model.oa.Database;
import com.remodel.model.oa.DatabaseType;
import com.remodel.model.oa.ForeignTable;
import com.remodel.model.oa.ForeignTableColumn;
import com.remodel.model.oa.Index;
import com.remodel.model.oa.IndexColumn;
import com.remodel.model.oa.JavaType;
import com.remodel.model.oa.JsonColumn;
import com.remodel.model.oa.JsonObject;
import com.remodel.model.oa.Project;
import com.remodel.model.oa.QueryColumn;
import com.remodel.model.oa.QueryInfo;
import com.remodel.model.oa.QuerySort;
import com.remodel.model.oa.QueryTable;
import com.remodel.model.oa.Repository;
import com.remodel.model.oa.SqlType;
import com.remodel.model.oa.Table;
import com.remodel.model.oa.TableCategory;
import com.remodel.model.oa.cs.ClientRoot;
import com.remodel.model.oa.cs.ServerRoot;
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
	private static final Hub<Database> hubDatabases = new Hub<Database>(Database.class);
	private static final Hub<DatabaseType> hubDatabaseTypes = new Hub<DatabaseType>(DatabaseType.class);
	private static final Hub<DataType> hubDataTypes = new Hub<DataType>(DataType.class);
	private static final Hub<JavaType> hubJavaTypes = new Hub<JavaType>(JavaType.class);
	private static final Hub<Project> hubProjects = new Hub<Project>(Project.class);
	private static final Hub<SqlType> hubSqlTypes = new Hub<SqlType>(SqlType.class);
	// autoCreateOne
	private static final Hub<AppServer> hubCreateOneAppServer = new Hub<AppServer>(AppServer.class);
	// UI containers
	private static final Hub<Column> hubSearchColumns = new Hub<Column>(Column.class);
	private static final Hub<Database> hubSearchDatabases = new Hub<Database>(Database.class);
	private static final Hub<ForeignTableColumn> hubSearchForeignTableColumns = new Hub<ForeignTableColumn>(ForeignTableColumn.class);
	private static final Hub<ForeignTable> hubSearchForeignTables = new Hub<ForeignTable>(ForeignTable.class);
	private static final Hub<IndexColumn> hubSearchIndexColumns = new Hub<IndexColumn>(IndexColumn.class);
	private static final Hub<Index> hubSearchIndexes = new Hub<Index>(Index.class);
	private static final Hub<JsonObject> hubSearchJsonObjects = new Hub<JsonObject>(JsonObject.class);
	private static final Hub<JsonColumn> hubSearchJsonColumns = new Hub<JsonColumn>(JsonColumn.class);
	private static final Hub<QueryTable> hubSearchQueryTables = new Hub<QueryTable>(QueryTable.class);
	private static final Hub<TableCategory> hubSearchTableCategories = new Hub<TableCategory>(TableCategory.class);
	private static final Hub<Table> hubSearchTables1 = new Hub<Table>(Table.class);
	private static final Hub<Project> hubSearchProjects = new Hub<Project>(Project.class);
	private static final Hub<Repository> hubSearchRepositories = new Hub<Repository>(Repository.class);
	private static final Hub<QueryColumn> hubSearchQueryColumns = new Hub<QueryColumn>(QueryColumn.class);
	private static final Hub<QueryInfo> hubSearchQueryInfos1 = new Hub<QueryInfo>(QueryInfo.class);
	private static final Hub<QuerySort> hubSearchQuerySorts = new Hub<QuerySort>(QuerySort.class);
	private static final Hub<AppUserLogin> hubAppUserLogins = new Hub<AppUserLogin>(AppUserLogin.class);
	private static final Hub<AppUserError> hubAppUserErrors = new Hub<AppUserError>(AppUserError.class);
	private static final Hub<AppUser> hubSearchAppUsers = new Hub<AppUser>(AppUser.class);
	/*$$End: ModelDelegate1 $$*/

	public static void initialize(ServerRoot rootServer, ClientRoot rootClient) {
		LOG.fine("selecting data");

		/*$$Start: ModelDelegate2 $$*/
		// lookups, preselects
		setSharedHub(getAppUsers(), rootServer.getAppUsers());
		setSharedHub(getDatabases(), rootServer.getDatabases());
		setSharedHub(getDatabaseTypes(), rootServer.getDatabaseTypes());
		setSharedHub(getDataTypes(), rootServer.getDataTypes());
		setSharedHub(getJavaTypes(), rootServer.getJavaTypes());
		setSharedHub(getProjects(), rootServer.getProjects());
		setSharedHub(getSqlTypes(), rootServer.getSqlTypes());
		// autoCreateOne
		setSharedHub(getCreateOneAppServerHub(), rootServer.getCreateOneAppServerHub());
		// filters
		// UI containers
		if (rootClient != null) {
			setSharedHub(getSearchColumns(), rootClient.getSearchColumns());
		}
		if (rootClient != null) {
			setSharedHub(getSearchDatabases(), rootClient.getSearchDatabases());
		}
		if (rootClient != null) {
			setSharedHub(getSearchForeignTableColumns(), rootClient.getSearchForeignTableColumns());
		}
		if (rootClient != null) {
			setSharedHub(getSearchForeignTables(), rootClient.getSearchForeignTables());
		}
		if (rootClient != null) {
			setSharedHub(getSearchIndexColumns(), rootClient.getSearchIndexColumns());
		}
		if (rootClient != null) {
			setSharedHub(getSearchIndexes(), rootClient.getSearchIndexes());
		}
		if (rootClient != null) {
			setSharedHub(getSearchJsonObjects(), rootClient.getSearchJsonObjects());
		}
		if (rootClient != null) {
			setSharedHub(getSearchJsonColumns(), rootClient.getSearchJsonColumns());
		}
		if (rootClient != null) {
			setSharedHub(getSearchQueryTables(), rootClient.getSearchQueryTables());
		}
		if (rootClient != null) {
			setSharedHub(getSearchTableCategories(), rootClient.getSearchTableCategories());
		}
		if (rootClient != null) {
			setSharedHub(getSearchTables1(), rootClient.getSearchTables1());
		}
		if (rootClient != null) {
			setSharedHub(getSearchProjects(), rootClient.getSearchProjects());
		}
		if (rootClient != null) {
			setSharedHub(getSearchRepositories(), rootClient.getSearchRepositories());
		}
		if (rootClient != null) {
			setSharedHub(getSearchQueryColumns(), rootClient.getSearchQueryColumns());
		}
		if (rootClient != null) {
			setSharedHub(getSearchQueryInfos1(), rootClient.getSearchQueryInfos1());
		}
		if (rootClient != null) {
			setSharedHub(getSearchQuerySorts(), rootClient.getSearchQuerySorts());
		}
		getAppUserLogins().setSharedHub(rootServer.getAppUserLogins());
		getAppUserErrors().setSharedHub(rootServer.getAppUserErrors());
		if (rootClient != null) {
			setSharedHub(getSearchAppUsers(), rootClient.getSearchAppUsers());
			/*$$End: ModelDelegate2 $$*/
		}

		for (int i = 0; i < 120; i++) {
			if (aiExecutor.get() == 0) {
				break;
			}
			if (i > 5) {
				LOG.fine(i + "/120) waiting on initialize to finish sharing hubs");
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

	public static Hub<Database> getDatabases() {
		return hubDatabases;
	}

	public static Hub<DatabaseType> getDatabaseTypes() {
		return hubDatabaseTypes;
	}

	public static Hub<DataType> getDataTypes() {
		return hubDataTypes;
	}

	public static Hub<JavaType> getJavaTypes() {
		return hubJavaTypes;
	}

	public static Hub<Project> getProjects() {
		return hubProjects;
	}

	public static Hub<SqlType> getSqlTypes() {
		return hubSqlTypes;
	}

	// autoCreateOne
	public static Hub<AppServer> getCreateOneAppServerHub() {
		return hubCreateOneAppServer;
	}

	public static AppServer getAppServer() {
		return hubCreateOneAppServer.getAt(0);
	}

	public static Hub<Column> getSearchColumns() {
		return hubSearchColumns;
	}

	public static Hub<Database> getSearchDatabases() {
		return hubSearchDatabases;
	}

	public static Hub<ForeignTableColumn> getSearchForeignTableColumns() {
		return hubSearchForeignTableColumns;
	}

	public static Hub<ForeignTable> getSearchForeignTables() {
		return hubSearchForeignTables;
	}

	public static Hub<IndexColumn> getSearchIndexColumns() {
		return hubSearchIndexColumns;
	}

	public static Hub<Index> getSearchIndexes() {
		return hubSearchIndexes;
	}

	public static Hub<JsonObject> getSearchJsonObjects() {
		return hubSearchJsonObjects;
	}

	public static Hub<JsonColumn> getSearchJsonColumns() {
		return hubSearchJsonColumns;
	}

	public static Hub<QueryTable> getSearchQueryTables() {
		return hubSearchQueryTables;
	}

	public static Hub<TableCategory> getSearchTableCategories() {
		return hubSearchTableCategories;
	}

	public static Hub<Table> getSearchTables1() {
		return hubSearchTables1;
	}

	public static Hub<Project> getSearchProjects() {
		return hubSearchProjects;
	}

	public static Hub<Repository> getSearchRepositories() {
		return hubSearchRepositories;
	}

	public static Hub<QueryColumn> getSearchQueryColumns() {
		return hubSearchQueryColumns;
	}

	public static Hub<QueryInfo> getSearchQueryInfos1() {
		return hubSearchQueryInfos1;
	}

	public static Hub<QuerySort> getSearchQuerySorts() {
		return hubSearchQuerySorts;
	}

	public static Hub<AppUserLogin> getAppUserLogins() {
		return hubAppUserLogins;
	}

	public static Hub<AppUserError> getAppUserErrors() {
		return hubAppUserErrors;
	}

	public static Hub<AppUser> getSearchAppUsers() {
		return hubSearchAppUsers;
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
