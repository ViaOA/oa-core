// Generated by OABuilder

package com.oreillyauto.remodel.model;

import java.util.logging.*;
import com.viaoa.object.*;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.filter.*;
import com.viaoa.datasource.*;

import com.oreillyauto.remodel.model.oa.*;
import com.oreillyauto.remodel.model.oa.propertypath.*;
import com.oreillyauto.remodel.model.oa.search.*;
import com.oreillyauto.remodel.model.oa.filter.*;
import com.oreillyauto.remodel.model.search.*;
import com.oreillyauto.remodel.model.filter.*;
import com.oreillyauto.remodel.delegate.ModelDelegate;
import com.oreillyauto.remodel.resource.Resource;

public class QueryTableModel extends OAObjectModel {
    private static Logger LOG = Logger.getLogger(QueryTableModel.class.getName());
    
    // Hubs
    protected Hub<QueryTable> hub;
    // selected queryTables
    protected Hub<QueryTable> hubMultiSelect;
    // detail hubs
    protected Hub<QueryTable> hubJoinTable;
    protected Hub<QueryInfo> hubQueryInfo;
    protected Hub<Table> hubTable;
    protected Hub<QueryTable> hubJoinedTables;
    protected Hub<QueryColumn> hubQueryColumns;
    
    // AddHubs used for references
    protected Hub<QueryTable> hubJoinTableSelectFrom;
    protected Hub<Table> hubTableSelectFrom;
    
    // pickFrom
    protected Hub<Column> hubQueryColumnsPickFromColumn;
    
    // ObjectModels
    protected QueryTableModel modelJoinTable;
    protected QueryInfoModel modelQueryInfo;
    protected TableModel modelTable;
    protected QueryTableModel modelJoinedTables;
    protected QueryColumnModel modelQueryColumns;
    
    // selectFrom
    protected QueryTableModel modelJoinTableSelectFrom;
    protected TableModel modelTableSelectFrom;
    
    // pickFrom
    protected ColumnModel modelQueryColumnsPickFromColumn;
    protected ColumnSearchModel modelQueryColumnsPickFromColumnSearch;
    
    // SearchModels used for references
    protected QueryTableSearchModel modelJoinTableSearch;
    protected QueryInfoSearchModel modelQueryInfoSearch;
    protected TableSearchModel modelTableSearch;
    protected QueryTableSearchModel modelJoinedTablesSearch;
    
    public QueryTableModel() {
        setDisplayName("Query Table");
        setPluralDisplayName("Query Tables");
    }
    
    public QueryTableModel(Hub<QueryTable> hubQueryTable) {
        this();
        if (hubQueryTable != null) HubDelegate.setObjectClass(hubQueryTable, QueryTable.class);
        this.hub = hubQueryTable;
    }
    public QueryTableModel(QueryTable queryTable) {
        this();
        getHub().add(queryTable);
        getHub().setPos(0);
    }
    
    public Hub<QueryTable> getOriginalHub() {
        return getHub();
    }
    
    public Hub<QueryTable> getJoinTableHub() {
        if (hubJoinTable != null) return hubJoinTable;
        hubJoinTable = getHub().getDetailHub(QueryTable.P_JoinTable);
        return hubJoinTable;
    }
    public Hub<QueryInfo> getQueryInfoHub() {
        if (hubQueryInfo != null) return hubQueryInfo;
        // this is the owner, use detailHub
        hubQueryInfo = getHub().getDetailHub(QueryTable.P_QueryInfo);
        return hubQueryInfo;
    }
    public Hub<Table> getTableHub() {
        if (hubTable != null) return hubTable;
        hubTable = getHub().getDetailHub(QueryTable.P_Table);
        return hubTable;
    }
    public Hub<QueryTable> getJoinedTables() {
        if (hubJoinedTables == null) {
            hubJoinedTables = getHub().getDetailHub(QueryTable.P_JoinedTables);
        }
        return hubJoinedTables;
    }
    public Hub<QueryColumn> getQueryColumns() {
        if (hubQueryColumns == null) {
            hubQueryColumns = getHub().getDetailHub(QueryTable.P_QueryColumns);
        }
        return hubQueryColumns;
    }
    public Hub<QueryTable> getJoinTableSelectFromHub() {
        if (hubJoinTableSelectFrom != null) return hubJoinTableSelectFrom;
        hubJoinTableSelectFrom = new Hub<QueryTable>(QueryTable.class);
        Hub<QueryTable>hubJoinTableSelectFrom1 = new Hub<QueryTable>(QueryTable.class);
        new HubMerger(getHub(), hubJoinTableSelectFrom1, QueryTablePP.queryInfo().queryTables().pp, false);
        HubCombined<QueryTable> hubCombined = new HubCombined(hubJoinTableSelectFrom, hubJoinTableSelectFrom1, getJoinTableHub());
        hubJoinTableSelectFrom.setLinkHub(getHub(), QueryTable.P_JoinTable); 
        return hubJoinTableSelectFrom;
    }
    public Hub<Table> getTableSelectFromHub() {
        if (hubTableSelectFrom != null) return hubTableSelectFrom;
        hubTableSelectFrom = new Hub<Table>(Table.class);
        Hub<Table>hubTableSelectFrom1 = new Hub<Table>(Table.class);
        new HubMerger(getHub(), hubTableSelectFrom1, QueryTablePP.queryInfo().repository().project().databases().tables().pp, false);
        HubCombined<Table> hubCombined = new HubCombined(hubTableSelectFrom, hubTableSelectFrom1, getTableHub());
        hubTableSelectFrom.setLinkHub(getHub(), QueryTable.P_Table); 
        return hubTableSelectFrom;
    }
    public Hub<Column> getQueryColumnsPickFromColumnHub() {
        if (hubQueryColumnsPickFromColumn != null) return hubQueryColumnsPickFromColumn;
        hubQueryColumnsPickFromColumn = new Hub<Column>(Column.class);
        new HubMerger(getHub(), hubQueryColumnsPickFromColumn, QueryTablePP.table().columns().pp, false);
        return hubQueryColumnsPickFromColumn;
    }
    public QueryTable getQueryTable() {
        return getHub().getAO();
    }
    
    public Hub<QueryTable> getHub() {
        if (hub == null) {
            hub = new Hub<QueryTable>(QueryTable.class);
        }
        return hub;
    }
    
    public Hub<QueryTable> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<QueryTable>(QueryTable.class);
        }
        return hubMultiSelect;
    }
    
    public QueryTableModel getJoinTableModel() {
        if (modelJoinTable != null) return modelJoinTable;
        modelJoinTable = new QueryTableModel(getJoinTableHub());
        modelJoinTable.setDisplayName("Join Table");
        modelJoinTable.setPluralDisplayName("Query Tables");
        modelJoinTable.setForJfc(getForJfc());
        modelJoinTable.setAllowNew(false);
        modelJoinTable.setAllowSave(true);
        modelJoinTable.setAllowAdd(false);
        modelJoinTable.setAllowRemove(true);
        modelJoinTable.setAllowClear(true);
        modelJoinTable.setAllowDelete(false);
        modelJoinTable.setAllowSearch(true);
        modelJoinTable.setAllowHubSearch(true);
        modelJoinTable.setAllowGotoEdit(true);
        modelJoinTable.setViewOnly(true);
        // call QueryTable.joinTableModelCallback(QueryTableModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(QueryTable.class, QueryTable.P_JoinTable, modelJoinTable);
    
        return modelJoinTable;
    }
    public QueryInfoModel getQueryInfoModel() {
        if (modelQueryInfo != null) return modelQueryInfo;
        modelQueryInfo = new QueryInfoModel(getQueryInfoHub());
        modelQueryInfo.setDisplayName("Query Info");
        modelQueryInfo.setPluralDisplayName("Query Infos");
        modelQueryInfo.setForJfc(getForJfc());
        modelQueryInfo.setAllowNew(false);
        modelQueryInfo.setAllowSave(true);
        modelQueryInfo.setAllowAdd(false);
        modelQueryInfo.setAllowRemove(false);
        modelQueryInfo.setAllowClear(false);
        modelQueryInfo.setAllowDelete(false);
        modelQueryInfo.setAllowSearch(true);
        modelQueryInfo.setAllowHubSearch(true);
        modelQueryInfo.setAllowGotoEdit(true);
        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
        modelQueryInfo.setCreateUI(li == null || !QueryTable.P_QueryInfo.equals(li.getName()) );
        modelQueryInfo.setViewOnly(getViewOnly());
        // call QueryTable.queryInfoModelCallback(QueryInfoModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(QueryTable.class, QueryTable.P_QueryInfo, modelQueryInfo);
    
        return modelQueryInfo;
    }
    public TableModel getTableModel() {
        if (modelTable != null) return modelTable;
        modelTable = new TableModel(getTableHub());
        modelTable.setDisplayName("Table");
        modelTable.setPluralDisplayName("Tables");
        modelTable.setForJfc(getForJfc());
        modelTable.setAllowNew(false);
        modelTable.setAllowSave(true);
        modelTable.setAllowAdd(false);
        modelTable.setAllowRemove(false);
        modelTable.setAllowClear(false);
        modelTable.setAllowDelete(false);
        modelTable.setAllowSearch(true);
        modelTable.setAllowHubSearch(true);
        modelTable.setAllowGotoEdit(true);
        modelTable.setViewOnly(getViewOnly());
        // call QueryTable.tableModelCallback(TableModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(QueryTable.class, QueryTable.P_Table, modelTable);
    
        return modelTable;
    }
    public QueryTableModel getJoinedTablesModel() {
        if (modelJoinedTables != null) return modelJoinedTables;
        modelJoinedTables = new QueryTableModel(getJoinedTables());
        modelJoinedTables.setDisplayName("Query Table");
        modelJoinedTables.setPluralDisplayName("Query Tables");
        if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getJoinedTables())) {
            modelJoinedTables.setCreateUI(false);
        }
        modelJoinedTables.setForJfc(getForJfc());
        modelJoinedTables.setAllowNew(false);
        modelJoinedTables.setAllowSave(true);
        modelJoinedTables.setAllowAdd(false);
        modelJoinedTables.setAllowMove(false);
        modelJoinedTables.setAllowRemove(false);
        modelJoinedTables.setAllowDelete(true);
        modelJoinedTables.setAllowSearch(false);
        modelJoinedTables.setAllowHubSearch(true);
        modelJoinedTables.setAllowGotoEdit(true);
        modelJoinedTables.setViewOnly(getViewOnly());
        modelJoinedTables.setAllowNew(false);
        modelJoinedTables.setAllowTableFilter(true);
        modelJoinedTables.setAllowTableSorting(true);
        modelJoinedTables.setAllowMultiSelect(false);
        modelJoinedTables.setAllowCopy(false);
        modelJoinedTables.setAllowCut(false);
        modelJoinedTables.setAllowPaste(false);
        // call QueryTable.joinedTablesModelCallback(QueryTableModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(QueryTable.class, QueryTable.P_JoinedTables, modelJoinedTables);
    
        return modelJoinedTables;
    }
    public QueryColumnModel getQueryColumnsModel() {
        if (modelQueryColumns != null) return modelQueryColumns;
        modelQueryColumns = new QueryColumnModel(getQueryColumns());
        modelQueryColumns.setDisplayName("Query Column");
        modelQueryColumns.setPluralDisplayName("Query Columns");
        if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getQueryColumns())) {
            modelQueryColumns.setCreateUI(false);
        }
        modelQueryColumns.setForJfc(getForJfc());
        modelQueryColumns.setAllowNew(false); // turned off and replaced by allowAdd=true, so user must first pick Column
        modelQueryColumns.setAllowSave(true);
        modelQueryColumns.setAllowAdd(true); // Add is overwritten to create new QueryColumn by first selecting Column
        modelQueryColumns.setAllowMove(false);
        modelQueryColumns.setAllowRemove(false);
        modelQueryColumns.setAllowDelete(true);
        modelQueryColumns.setAllowSearch(false);
        modelQueryColumns.setAllowHubSearch(false);
        modelQueryColumns.setAllowGotoEdit(true);
        modelQueryColumns.setViewOnly(getViewOnly());
        modelQueryColumns.setAllowNew(false); // turned off and replaced by allowAdd=true, so user must first pick Column
        modelQueryColumns.setAllowTableFilter(true);
        modelQueryColumns.setAllowTableSorting(true);
        modelQueryColumns.setAllowMultiSelect(false);
        modelQueryColumns.setAllowCopy(false);
        modelQueryColumns.setAllowCut(false);
        modelQueryColumns.setAllowPaste(false);
        // call QueryTable.queryColumnsModelCallback(QueryColumnModel) to be able to customize this model
        OAObjectCallbackDelegate.onObjectCallbackModel(QueryTable.class, QueryTable.P_QueryColumns, modelQueryColumns);
    
        return modelQueryColumns;
    }
    
    public QueryTableModel getJoinTableSelectFromModel() {
        if (modelJoinTableSelectFrom != null) return modelJoinTableSelectFrom;
        modelJoinTableSelectFrom = new QueryTableModel(getJoinTableSelectFromHub());
        modelJoinTableSelectFrom.setDisplayName("Query Table");
        modelJoinTableSelectFrom.setPluralDisplayName("Query Tables");
        modelJoinTableSelectFrom.setForJfc(getForJfc());
        modelJoinTableSelectFrom.setAllowNew(false);
        modelJoinTableSelectFrom.setAllowSave(true);
        modelJoinTableSelectFrom.setAllowAdd(false);
        modelJoinTableSelectFrom.setAllowMove(false);
        modelJoinTableSelectFrom.setAllowRemove(false);
        modelJoinTableSelectFrom.setAllowDelete(false);
        modelJoinTableSelectFrom.setAllowSearch(true);
        modelJoinTableSelectFrom.setAllowHubSearch(true);
        modelJoinTableSelectFrom.setAllowGotoEdit(true);
        modelJoinTableSelectFrom.setViewOnly(getViewOnly());
        modelJoinTableSelectFrom.setAllowNew(false);
        modelJoinTableSelectFrom.setAllowTableFilter(true);
        modelJoinTableSelectFrom.setAllowTableSorting(true);
        modelJoinTableSelectFrom.setAllowCut(false);
        modelJoinTableSelectFrom.setAllowCopy(false);
        modelJoinTableSelectFrom.setAllowPaste(false);
        modelJoinTableSelectFrom.setAllowMultiSelect(false);
        return modelJoinTableSelectFrom;
    }
    public TableModel getTableSelectFromModel() {
        if (modelTableSelectFrom != null) return modelTableSelectFrom;
        modelTableSelectFrom = new TableModel(getTableSelectFromHub());
        modelTableSelectFrom.setDisplayName("Table");
        modelTableSelectFrom.setPluralDisplayName("Tables");
        modelTableSelectFrom.setForJfc(getForJfc());
        modelTableSelectFrom.setAllowNew(false);
        modelTableSelectFrom.setAllowSave(true);
        modelTableSelectFrom.setAllowAdd(false);
        modelTableSelectFrom.setAllowMove(false);
        modelTableSelectFrom.setAllowRemove(false);
        modelTableSelectFrom.setAllowDelete(false);
        modelTableSelectFrom.setAllowSearch(true);
        modelTableSelectFrom.setAllowHubSearch(true);
        modelTableSelectFrom.setAllowGotoEdit(true);
        modelTableSelectFrom.setViewOnly(getViewOnly());
        modelTableSelectFrom.setAllowNew(false);
        modelTableSelectFrom.setAllowTableFilter(true);
        modelTableSelectFrom.setAllowTableSorting(true);
        modelTableSelectFrom.setAllowCut(false);
        modelTableSelectFrom.setAllowCopy(false);
        modelTableSelectFrom.setAllowPaste(false);
        modelTableSelectFrom.setAllowMultiSelect(false);
        return modelTableSelectFrom;
    }
    public ColumnModel getQueryColumnsPickFromColumnModel() {
        if (modelQueryColumnsPickFromColumn != null) return modelQueryColumnsPickFromColumn;
        modelQueryColumnsPickFromColumn = new ColumnModel(getQueryColumnsPickFromColumnHub());
        modelQueryColumnsPickFromColumn.setDisplayName("Query Column");
        modelQueryColumnsPickFromColumn.setPluralDisplayName("Query Columns");
        modelQueryColumnsPickFromColumn.setForJfc(getForJfc());
        modelQueryColumnsPickFromColumn.setAllowNew(false);
        modelQueryColumnsPickFromColumn.setAllowSave(true);
        modelQueryColumnsPickFromColumn.setAllowAdd(false);
        modelQueryColumnsPickFromColumn.setAllowMove(false);
        modelQueryColumnsPickFromColumn.setAllowRemove(false);
        modelQueryColumnsPickFromColumn.setAllowDelete(false);
        modelQueryColumnsPickFromColumn.setAllowSearch(true);
        modelQueryColumnsPickFromColumn.setAllowHubSearch(true);
        modelQueryColumnsPickFromColumn.setAllowGotoEdit(true);
        modelQueryColumnsPickFromColumn.setViewOnly(getViewOnly());
        modelQueryColumnsPickFromColumn.setAllowNew(false);
        modelQueryColumnsPickFromColumn.setAllowTableFilter(true);
        modelQueryColumnsPickFromColumn.setAllowTableSorting(true);
        modelQueryColumnsPickFromColumn.setAllowCut(false);
        modelQueryColumnsPickFromColumn.setAllowCopy(false);
        modelQueryColumnsPickFromColumn.setAllowPaste(false);
        modelQueryColumnsPickFromColumn.setAllowMultiSelect(true);
        new HubMerger<QueryColumn, Column>(getQueryColumns(), modelQueryColumnsPickFromColumn.getMultiSelectHub(), QueryColumn.P_Column);
        return modelQueryColumnsPickFromColumn;
    }
    public ColumnSearchModel getQueryColumnsPickFromColumnSearchModel() {
        if (modelQueryColumnsPickFromColumnSearch != null) return modelQueryColumnsPickFromColumnSearch;
        modelQueryColumnsPickFromColumnSearch = new ColumnSearchModel();
        OAFilter filter = new OAInFilter(getQueryColumns(), QueryColumnPP.queryTable().table().columns().pp);
        modelQueryColumnsPickFromColumnSearch.getColumnSearch().setExtraWhereFilter(filter);
        return modelQueryColumnsPickFromColumnSearch;
    }
    public QueryTableSearchModel getJoinTableSearchModel() {
        if (modelJoinTableSearch != null) return modelJoinTableSearch;
        modelJoinTableSearch = new QueryTableSearchModel();
        OAFilter filter = new OAInFilter(QueryTableModel.this.getHub(), QueryTablePP.queryInfo().queryTables().pp);
        modelJoinTableSearch.getQueryTableSearch().setExtraWhereFilter(filter);
        return modelJoinTableSearch;
    }
    public QueryInfoSearchModel getQueryInfoSearchModel() {
        if (modelQueryInfoSearch != null) return modelQueryInfoSearch;
        modelQueryInfoSearch = new QueryInfoSearchModel();
        HubSelectDelegate.adoptWhereHub(modelQueryInfoSearch.getHub(), QueryTable.P_QueryInfo, getHub());
        return modelQueryInfoSearch;
    }
    public TableSearchModel getTableSearchModel() {
        if (modelTableSearch != null) return modelTableSearch;
        modelTableSearch = new TableSearchModel();
        OAFilter filter = new OAInFilter(QueryTableModel.this.getHub(), QueryTablePP.queryInfo().repository().project().databases().tables().pp);
        modelTableSearch.getTableSearch().setExtraWhereFilter(filter);
        return modelTableSearch;
    }
    public QueryTableSearchModel getJoinedTablesSearchModel() {
        if (modelJoinedTablesSearch != null) return modelJoinedTablesSearch;
        modelJoinedTablesSearch = new QueryTableSearchModel();
        return modelJoinedTablesSearch;
    }
    
    public HubCopy<QueryTable> createHubCopy() {
        Hub<QueryTable> hubQueryTablex = new Hub<>(QueryTable.class);
        HubCopy<QueryTable> hc = new HubCopy<>(getHub(), hubQueryTablex, true);
        return hc;
    }
    public QueryTableModel createCopy() {
        QueryTableModel mod = new QueryTableModel(createHubCopy().getHub());
        return mod;
    }
}
