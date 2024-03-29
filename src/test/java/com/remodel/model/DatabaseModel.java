// Generated by OABuilder

package com.remodel.model;

import java.util.logging.*;
import com.viaoa.object.*;
import com.remodel.delegate.ModelDelegate;
import com.remodel.model.filter.*;
import com.remodel.model.oa.*;
import com.remodel.model.oa.filter.*;
import com.remodel.model.oa.propertypath.*;
import com.remodel.model.oa.search.*;
import com.remodel.model.search.*;
import com.remodel.resource.Resource;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.filter.*;
import com.viaoa.datasource.*;

public class DatabaseModel extends OAObjectModel {
    private static Logger LOG = Logger.getLogger(DatabaseModel.class.getName());
    
    // Hubs
    protected Hub<Database> hub;
    // selected databases
    protected Hub<Database> hubMultiSelect;
    // detail hubs
    protected Hub<DatabaseType> hubDatabaseType;
    protected Hub<Project> hubProjects;
    protected Hub<TableCategory> hubTableCategories;
    protected Hub<Table> hubTables;
    
    // AddHubs used for references
    protected Hub<DatabaseType> hubDatabaseTypeSelectFrom;
    protected Hub<Project> hubProjectsSelectFrom;
    
    // ObjectModels
    protected DatabaseTypeModel modelDatabaseType;
    protected ProjectModel modelProjects;
    protected TableCategoryModel modelTableCategories;
    protected TableModel modelTables;
    
    // selectFrom
    protected DatabaseTypeModel modelDatabaseTypeSelectFrom;
    protected ProjectModel modelProjectsSelectFrom;
    
    // SearchModels used for references
    protected ProjectSearchModel modelProjectsSearch;
    protected TableCategorySearchModel modelTableCategoriesSearch;
    protected TableSearchModel modelTablesSearch;
    
    public DatabaseModel() {
        setDisplayName("Database");
        setPluralDisplayName("Databases");
    }
    
    public DatabaseModel(Hub<Database> hubDatabase) {
        this();
        if (hubDatabase != null) HubDelegate.setObjectClass(hubDatabase, Database.class);
        this.hub = hubDatabase;
    }
    public DatabaseModel(Database database) {
        this();
        getHub().add(database);
        getHub().setPos(0);
    }
    
    public Hub<Database> getOriginalHub() {
        return getHub();
    }
    
    public Hub<DatabaseType> getDatabaseTypeHub() {
        if (hubDatabaseType != null) return hubDatabaseType;
        hubDatabaseType = getHub().getDetailHub(Database.P_DatabaseType);
        return hubDatabaseType;
    }
    public Hub<Project> getProjects() {
        if (hubProjects == null) {
            hubProjects = getHub().getDetailHub(Database.P_Projects);
        }
        return hubProjects;
    }
    public Hub<TableCategory> getTableCategories() {
        if (hubTableCategories == null) {
            hubTableCategories = getHub().getDetailHub(Database.P_TableCategories);
        }
        return hubTableCategories;
    }
    public Hub<Table> getTables() {
        if (hubTables == null) {
            hubTables = getHub().getDetailHub(Database.P_Tables);
        }
        return hubTables;
    }
    public Hub<DatabaseType> getDatabaseTypeSelectFromHub() {
        if (hubDatabaseTypeSelectFrom != null) return hubDatabaseTypeSelectFrom;
        hubDatabaseTypeSelectFrom = new Hub<DatabaseType>(DatabaseType.class);
        Hub<DatabaseType> hubDatabaseTypeSelectFrom1 = ModelDelegate.getDatabaseTypes().createSharedHub();
        HubCombined<DatabaseType> hubCombined = new HubCombined(hubDatabaseTypeSelectFrom, hubDatabaseTypeSelectFrom1, getDatabaseTypeHub());
        hubDatabaseTypeSelectFrom.setLinkHub(getHub(), Database.P_DatabaseType); 
        return hubDatabaseTypeSelectFrom;
    }
    public Hub<Project> getProjectsSelectFromHub() {
        if (hubProjectsSelectFrom != null) return hubProjectsSelectFrom;
        hubProjectsSelectFrom = ModelDelegate.getProjects().createSharedHub();
        return hubProjectsSelectFrom;
    }
    public Database getDatabase() {
        return getHub().getAO();
    }
    
    public Hub<Database> getHub() {
        if (hub == null) {
            hub = new Hub<Database>(Database.class);
        }
        return hub;
    }
    
    public Hub<Database> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<Database>(Database.class);
        }
        return hubMultiSelect;
    }
    
    public DatabaseTypeModel getDatabaseTypeModel() {
        if (modelDatabaseType != null) return modelDatabaseType;
        modelDatabaseType = new DatabaseTypeModel(getDatabaseTypeHub());
        modelDatabaseType.setDisplayName("Database Type");
        modelDatabaseType.setPluralDisplayName("Database Types");
        modelDatabaseType.setForJfc(getForJfc());
        modelDatabaseType.setAllowNew(false);
        modelDatabaseType.setAllowSave(true);
        modelDatabaseType.setAllowAdd(false);
        modelDatabaseType.setAllowRemove(false);
        modelDatabaseType.setAllowClear(false);
        modelDatabaseType.setAllowDelete(false);
        modelDatabaseType.setAllowSearch(false);
        modelDatabaseType.setAllowHubSearch(false);
        modelDatabaseType.setAllowGotoEdit(false);
        modelDatabaseType.setViewOnly(true);
        // call Database.databaseTypeModelCallback(DatabaseTypeModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(Database.class, Database.P_DatabaseType, modelDatabaseType);
    
        return modelDatabaseType;
    }
    public ProjectModel getProjectsModel() {
        if (modelProjects != null) return modelProjects;
        modelProjects = new ProjectModel(getProjects());
        modelProjects.setDisplayName("Project");
        modelProjects.setPluralDisplayName("Projects");
        if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getProjects())) {
            modelProjects.setCreateUI(false);
        }
        modelProjects.setForJfc(getForJfc());
        modelProjects.setAllowNew(false);
        modelProjects.setAllowSave(true);
        modelProjects.setAllowAdd(true);
        modelProjects.setAllowMove(false);
        modelProjects.setAllowRemove(true);
        modelProjects.setAllowDelete(false);
        modelProjects.setAllowSearch(false);
        modelProjects.setAllowHubSearch(true);
        modelProjects.setAllowGotoEdit(true);
        modelProjects.setViewOnly(getViewOnly());
        modelProjects.setAllowNew(false);
        modelProjects.setAllowTableFilter(true);
        modelProjects.setAllowTableSorting(true);
        modelProjects.setAllowMultiSelect(true);
        modelProjects.setAllowCopy(false);
        modelProjects.setAllowCut(false);
        modelProjects.setAllowPaste(false);
        // call Database.projectsModelCallback(ProjectModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(Database.class, Database.P_Projects, modelProjects);
    
        return modelProjects;
    }
    public TableCategoryModel getTableCategoriesModel() {
        if (modelTableCategories != null) return modelTableCategories;
        modelTableCategories = new TableCategoryModel(getTableCategories());
        modelTableCategories.setDisplayName("Table Category");
        modelTableCategories.setPluralDisplayName("Table Categories");
        if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getTableCategories())) {
            modelTableCategories.setCreateUI(false);
        }
        modelTableCategories.setForJfc(getForJfc());
        modelTableCategories.setAllowNew(true);
        modelTableCategories.setAllowSave(true);
        modelTableCategories.setAllowAdd(false);
        modelTableCategories.setAllowMove(false);
        modelTableCategories.setAllowRemove(false);
        modelTableCategories.setAllowDelete(true);
        modelTableCategories.setAllowSearch(false);
        modelTableCategories.setAllowHubSearch(true);
        modelTableCategories.setAllowGotoEdit(true);
        modelTableCategories.setViewOnly(getViewOnly());
        modelTableCategories.setAllowNew(true);
        modelTableCategories.setAllowTableFilter(true);
        modelTableCategories.setAllowTableSorting(true);
        modelTableCategories.setAllowRecursive(true);
        modelTableCategories.setAllowMultiSelect(false);
        modelTableCategories.setAllowCopy(false);
        modelTableCategories.setAllowCut(false);
        modelTableCategories.setAllowPaste(false);
        // call Database.tableCategoriesModelCallback(TableCategoryModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(Database.class, Database.P_TableCategories, modelTableCategories);
    
        return modelTableCategories;
    }
    public TableModel getTablesModel() {
        if (modelTables != null) return modelTables;
        modelTables = new TableModel(getTables());
        modelTables.setDisplayName("Table");
        modelTables.setPluralDisplayName("Tables");
        if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getTables())) {
            modelTables.setCreateUI(false);
        }
        modelTables.setForJfc(getForJfc());
        modelTables.setAllowNew(true);
        modelTables.setAllowSave(true);
        modelTables.setAllowAdd(false);
        modelTables.setAllowMove(false);
        modelTables.setAllowRemove(false);
        modelTables.setAllowDelete(true);
        modelTables.setAllowSearch(false);
        modelTables.setAllowHubSearch(true);
        modelTables.setAllowGotoEdit(true);
        modelTables.setViewOnly(getViewOnly());
        modelTables.setAllowNew(true);
        modelTables.setAllowTableFilter(true);
        modelTables.setAllowTableSorting(true);
        modelTables.setAllowMultiSelect(true);
        modelTables.setAllowCopy(false);
        modelTables.setAllowCut(false);
        modelTables.setAllowPaste(false);
        // call Database.tablesModelCallback(TableModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(Database.class, Database.P_Tables, modelTables);
    
        return modelTables;
    }
    
    public DatabaseTypeModel getDatabaseTypeSelectFromModel() {
        if (modelDatabaseTypeSelectFrom != null) return modelDatabaseTypeSelectFrom;
        modelDatabaseTypeSelectFrom = new DatabaseTypeModel(getDatabaseTypeSelectFromHub());
        modelDatabaseTypeSelectFrom.setDisplayName("Database Type");
        modelDatabaseTypeSelectFrom.setPluralDisplayName("Database Types");
        modelDatabaseTypeSelectFrom.setForJfc(getForJfc());
        modelDatabaseTypeSelectFrom.setAllowNew(false);
        modelDatabaseTypeSelectFrom.setAllowSave(true);
        modelDatabaseTypeSelectFrom.setAllowAdd(false);
        modelDatabaseTypeSelectFrom.setAllowMove(false);
        modelDatabaseTypeSelectFrom.setAllowRemove(false);
        modelDatabaseTypeSelectFrom.setAllowDelete(false);
        modelDatabaseTypeSelectFrom.setAllowSearch(false);
        modelDatabaseTypeSelectFrom.setAllowHubSearch(true);
        modelDatabaseTypeSelectFrom.setAllowGotoEdit(true);
        modelDatabaseTypeSelectFrom.setViewOnly(getViewOnly());
        modelDatabaseTypeSelectFrom.setAllowNew(false);
        modelDatabaseTypeSelectFrom.setAllowTableFilter(true);
        modelDatabaseTypeSelectFrom.setAllowTableSorting(true);
        modelDatabaseTypeSelectFrom.setAllowCut(false);
        modelDatabaseTypeSelectFrom.setAllowCopy(false);
        modelDatabaseTypeSelectFrom.setAllowPaste(false);
        modelDatabaseTypeSelectFrom.setAllowMultiSelect(false);
        return modelDatabaseTypeSelectFrom;
    }
    public ProjectModel getProjectsSelectFromModel() {
        if (modelProjectsSelectFrom != null) return modelProjectsSelectFrom;
        modelProjectsSelectFrom = new ProjectModel(getProjectsSelectFromHub());
        modelProjectsSelectFrom.setDisplayName("Project");
        modelProjectsSelectFrom.setPluralDisplayName("Projects");
        modelProjectsSelectFrom.setForJfc(getForJfc());
        modelProjectsSelectFrom.setAllowNew(false);
        modelProjectsSelectFrom.setAllowSave(true);
        modelProjectsSelectFrom.setAllowAdd(false);
        modelProjectsSelectFrom.setAllowMove(false);
        modelProjectsSelectFrom.setAllowRemove(false);
        modelProjectsSelectFrom.setAllowDelete(false);
        modelProjectsSelectFrom.setAllowSearch(true);
        modelProjectsSelectFrom.setAllowHubSearch(true);
        modelProjectsSelectFrom.setAllowGotoEdit(true);
        modelProjectsSelectFrom.setViewOnly(getViewOnly());
        modelProjectsSelectFrom.setAllowNew(false);
        modelProjectsSelectFrom.setAllowTableFilter(true);
        modelProjectsSelectFrom.setAllowTableSorting(true);
        modelProjectsSelectFrom.setAllowCut(false);
        modelProjectsSelectFrom.setAllowCopy(false);
        modelProjectsSelectFrom.setAllowPaste(false);
        modelProjectsSelectFrom.setAllowMultiSelect(true);
        new HubMakeCopy(getProjects(), modelProjectsSelectFrom.getMultiSelectHub());
        return modelProjectsSelectFrom;
    }
    public ProjectSearchModel getProjectsSearchModel() {
        if (modelProjectsSearch != null) return modelProjectsSearch;
        modelProjectsSearch = new ProjectSearchModel();
        return modelProjectsSearch;
    }
    public TableCategorySearchModel getTableCategoriesSearchModel() {
        if (modelTableCategoriesSearch != null) return modelTableCategoriesSearch;
        modelTableCategoriesSearch = new TableCategorySearchModel();
        return modelTableCategoriesSearch;
    }
    public TableSearchModel getTablesSearchModel() {
        if (modelTablesSearch != null) return modelTablesSearch;
        modelTablesSearch = new TableSearchModel();
        return modelTablesSearch;
    }
    
    public HubCopy<Database> createHubCopy() {
        Hub<Database> hubDatabasex = new Hub<>(Database.class);
        HubCopy<Database> hc = new HubCopy<>(getHub(), hubDatabasex, true);
        return hc;
    }
    public DatabaseModel createCopy() {
        DatabaseModel mod = new DatabaseModel(createHubCopy().getHub());
        return mod;
    }
}

