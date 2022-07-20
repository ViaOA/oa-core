// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.remodel.model.oa.cs;

import com.remodel.model.oa.AppUser;
import com.remodel.model.oa.Column;
import com.remodel.model.oa.Database;
import com.remodel.model.oa.ForeignTable;
import com.remodel.model.oa.ForeignTableColumn;
import com.remodel.model.oa.Index;
import com.remodel.model.oa.IndexColumn;
import com.remodel.model.oa.JsonColumn;
import com.remodel.model.oa.JsonObject;
import com.remodel.model.oa.Project;
import com.remodel.model.oa.QueryColumn;
import com.remodel.model.oa.QueryInfo;
import com.remodel.model.oa.QuerySort;
import com.remodel.model.oa.QueryTable;
import com.remodel.model.oa.Repository;
import com.remodel.model.oa.Table;
import com.remodel.model.oa.TableCategory;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Root Object that is automatically updated between the Server and Clients. ServerController will do the selects for these objects. Model
 * will share these hubs after the application is started.
 */
@OAClass(useDataSource = false, displayProperty = "Id")
public class ClientRoot extends OAObject {
	private static final long serialVersionUID = 1L;

	public static final String PROPERTY_Id = "Id";
	public static final String PROPERTY_ConnectionInfo = "ConnectionInfo";
	/*$$Start: ClientRoot1 $$*/
	// Hubs for Client UI
	public static final String P_SearchColumns = "SearchColumns";
	public static final String P_SearchDatabases = "SearchDatabases";
	public static final String P_SearchForeignTableColumns = "SearchForeignTableColumns";
	public static final String P_SearchForeignTables = "SearchForeignTables";
	public static final String P_SearchIndexColumns = "SearchIndexColumns";
	public static final String P_SearchIndexes = "SearchIndexes";
	public static final String P_SearchJsonObjects = "SearchJsonObjects";
	public static final String P_SearchJsonColumns = "SearchJsonColumns";
	public static final String P_SearchQueryTables = "SearchQueryTables";
	public static final String P_SearchTableCategories = "SearchTableCategories";
	public static final String P_SearchTables1 = "SearchTables1";
	public static final String P_SearchProjects = "SearchProjects";
	public static final String P_SearchRepositories = "SearchRepositories";
	public static final String P_SearchQueryColumns = "SearchQueryColumns";
	public static final String P_SearchQueryInfos1 = "SearchQueryInfos1";
	public static final String P_SearchQuerySorts = "SearchQuerySorts";
	public static final String P_SearchAppUsers = "SearchAppUsers";
	/*$$End: ClientRoot1 $$*/

	protected int id;

	// Hub
	/*$$Start: ClientRoot2 $$*/
	// Hubs for Client UI
	protected transient Hub<Column> hubSearchColumns;
	protected transient Hub<Database> hubSearchDatabases;
	protected transient Hub<ForeignTableColumn> hubSearchForeignTableColumns;
	protected transient Hub<ForeignTable> hubSearchForeignTables;
	protected transient Hub<IndexColumn> hubSearchIndexColumns;
	protected transient Hub<Index> hubSearchIndexes;
	protected transient Hub<JsonObject> hubSearchJsonObjects;
	protected transient Hub<JsonColumn> hubSearchJsonColumns;
	protected transient Hub<QueryTable> hubSearchQueryTables;
	protected transient Hub<TableCategory> hubSearchTableCategories;
	protected transient Hub<Table> hubSearchTables1;
	protected transient Hub<Project> hubSearchProjects;
	protected transient Hub<Repository> hubSearchRepositories;
	protected transient Hub<QueryColumn> hubSearchQueryColumns;
	protected transient Hub<QueryInfo> hubSearchQueryInfos1;
	protected transient Hub<QuerySort> hubSearchQuerySorts;
	protected transient Hub<AppUser> hubSearchAppUsers;
	/*$$End: ClientRoot2 $$*/

	@OAProperty(displayName = "Id")
	@OAId
	public int getId() {
		return id;
	}

	public void setId(int id) {
		int old = this.id;
		this.id = id;
		firePropertyChange("id", old, id);
	}

	/*$$Start: ClientRoot3 $$*/
	// Hubs for Client UI
	@OAMany(toClass = Column.class, cascadeSave = true)
	public Hub<Column> getSearchColumns() {
		if (hubSearchColumns == null) {
			hubSearchColumns = (Hub<Column>) super.getHub(P_SearchColumns);
		}
		return hubSearchColumns;
	}

	@OAMany(toClass = Database.class, cascadeSave = true)
	public Hub<Database> getSearchDatabases() {
		if (hubSearchDatabases == null) {
			hubSearchDatabases = (Hub<Database>) super.getHub(P_SearchDatabases);
		}
		return hubSearchDatabases;
	}

	@OAMany(toClass = ForeignTableColumn.class, cascadeSave = true)
	public Hub<ForeignTableColumn> getSearchForeignTableColumns() {
		if (hubSearchForeignTableColumns == null) {
			hubSearchForeignTableColumns = (Hub<ForeignTableColumn>) super.getHub(P_SearchForeignTableColumns);
		}
		return hubSearchForeignTableColumns;
	}

	@OAMany(toClass = ForeignTable.class, cascadeSave = true)
	public Hub<ForeignTable> getSearchForeignTables() {
		if (hubSearchForeignTables == null) {
			hubSearchForeignTables = (Hub<ForeignTable>) super.getHub(P_SearchForeignTables);
		}
		return hubSearchForeignTables;
	}

	@OAMany(toClass = IndexColumn.class, cascadeSave = true)
	public Hub<IndexColumn> getSearchIndexColumns() {
		if (hubSearchIndexColumns == null) {
			hubSearchIndexColumns = (Hub<IndexColumn>) super.getHub(P_SearchIndexColumns);
		}
		return hubSearchIndexColumns;
	}

	@OAMany(toClass = Index.class, cascadeSave = true)
	public Hub<Index> getSearchIndexes() {
		if (hubSearchIndexes == null) {
			hubSearchIndexes = (Hub<Index>) super.getHub(P_SearchIndexes);
		}
		return hubSearchIndexes;
	}

	@OAMany(toClass = JsonObject.class, cascadeSave = true)
	public Hub<JsonObject> getSearchJsonObjects() {
		if (hubSearchJsonObjects == null) {
			hubSearchJsonObjects = (Hub<JsonObject>) super.getHub(P_SearchJsonObjects);
		}
		return hubSearchJsonObjects;
	}

	@OAMany(toClass = JsonColumn.class, cascadeSave = true)
	public Hub<JsonColumn> getSearchJsonColumns() {
		if (hubSearchJsonColumns == null) {
			hubSearchJsonColumns = (Hub<JsonColumn>) super.getHub(P_SearchJsonColumns);
		}
		return hubSearchJsonColumns;
	}

	@OAMany(toClass = QueryTable.class, cascadeSave = true)
	public Hub<QueryTable> getSearchQueryTables() {
		if (hubSearchQueryTables == null) {
			hubSearchQueryTables = (Hub<QueryTable>) super.getHub(P_SearchQueryTables);
		}
		return hubSearchQueryTables;
	}

	@OAMany(toClass = TableCategory.class, cascadeSave = true)
	public Hub<TableCategory> getSearchTableCategories() {
		if (hubSearchTableCategories == null) {
			hubSearchTableCategories = (Hub<TableCategory>) super.getHub(P_SearchTableCategories);
		}
		return hubSearchTableCategories;
	}

	@OAMany(toClass = Table.class, cascadeSave = true)
	public Hub<Table> getSearchTables1() {
		if (hubSearchTables1 == null) {
			hubSearchTables1 = (Hub<Table>) super.getHub(P_SearchTables1);
		}
		return hubSearchTables1;
	}

	@OAMany(toClass = Project.class, cascadeSave = true)
	public Hub<Project> getSearchProjects() {
		if (hubSearchProjects == null) {
			hubSearchProjects = (Hub<Project>) super.getHub(P_SearchProjects);
		}
		return hubSearchProjects;
	}

	@OAMany(toClass = Repository.class, cascadeSave = true)
	public Hub<Repository> getSearchRepositories() {
		if (hubSearchRepositories == null) {
			hubSearchRepositories = (Hub<Repository>) super.getHub(P_SearchRepositories);
		}
		return hubSearchRepositories;
	}

	@OAMany(toClass = QueryColumn.class, cascadeSave = true)
	public Hub<QueryColumn> getSearchQueryColumns() {
		if (hubSearchQueryColumns == null) {
			hubSearchQueryColumns = (Hub<QueryColumn>) super.getHub(P_SearchQueryColumns);
		}
		return hubSearchQueryColumns;
	}

	@OAMany(toClass = QueryInfo.class, cascadeSave = true)
	public Hub<QueryInfo> getSearchQueryInfos1() {
		if (hubSearchQueryInfos1 == null) {
			hubSearchQueryInfos1 = (Hub<QueryInfo>) super.getHub(P_SearchQueryInfos1);
		}
		return hubSearchQueryInfos1;
	}

	@OAMany(toClass = QuerySort.class, cascadeSave = true)
	public Hub<QuerySort> getSearchQuerySorts() {
		if (hubSearchQuerySorts == null) {
			hubSearchQuerySorts = (Hub<QuerySort>) super.getHub(P_SearchQuerySorts);
		}
		return hubSearchQuerySorts;
	}

	@OAMany(toClass = AppUser.class, cascadeSave = true)
	public Hub<AppUser> getSearchAppUsers() {
		if (hubSearchAppUsers == null) {
			hubSearchAppUsers = (Hub<AppUser>) super.getHub(P_SearchAppUsers);
		}
		return hubSearchAppUsers;
	}
	/*$$End: ClientRoot3 $$*/

}
