// Generated by OABuilder
package com.corptostore.model.oa.search;

import java.util.*;
import java.util.logging.*;

import com.corptostore.model.oa.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.Batch;
import com.corptostore.model.oa.Receive;
import com.corptostore.model.oa.Store;
import com.corptostore.model.oa.propertypath.ReceivePP;
import com.corptostore.model.oa.search.BatchSearch;
import com.corptostore.model.oa.search.ReceiveSearch;
import com.corptostore.model.oa.search.StoreSearch;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;

@OAClass(useDataSource=false, localOnly=true)
public class ReceiveSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(ReceiveSearch.class.getName());

    public static final String P_MessageType = "MessageType";
    public static final String P_MessageName = "MessageName";
    public static final String P_MessageData = "MessageData";
    public static final String P_Store = "Store";
    public static final String P_UseStoreSearch = "UseStoreSearch";
    public static final String P_Batch = "Batch";
    public static final String P_UseBatchSearch = "UseBatchSearch";
    public static final String P_MaxResults = "MaxResults";

    protected String messageType;
    protected String messageName;
    protected String messageData;
    protected Store store;
    protected boolean useStoreSearch;
    protected StoreSearch searchStore;
    protected Batch batch;
    protected boolean useBatchSearch;
    protected BatchSearch searchBatch;
    protected int maxResults;

    public String getMessageType() {
        return messageType;
    }
    public void setMessageType(String newValue) {
        String old = messageType;
        fireBeforePropertyChange(P_MessageType, old, newValue);
        this.messageType = newValue;
        firePropertyChange(P_MessageType, old, this.messageType);
    }
      
    public String getMessageName() {
        return messageName;
    }
    public void setMessageName(String newValue) {
        String old = messageName;
        fireBeforePropertyChange(P_MessageName, old, newValue);
        this.messageName = newValue;
        firePropertyChange(P_MessageName, old, this.messageName);
    }
      
    public String getMessageData() {
        return messageData;
    }
    public void setMessageData(String newValue) {
        String old = messageData;
        fireBeforePropertyChange(P_MessageData, old, newValue);
        this.messageData = newValue;
        firePropertyChange(P_MessageData, old, this.messageData);
    }
      

    public int getMaxResults() {
        return maxResults;
    }
    public void setMaxResults(int newValue) {
        fireBeforePropertyChange(P_MaxResults, this.maxResults, newValue);
        int old = maxResults;
        this.maxResults = newValue;
        firePropertyChange(P_MaxResults, old, this.maxResults);
    }

    @OAOne
    public Store getStore() {
        if (store == null) {
            store = (Store) getObject(P_Store);
        }
        return store;
    }
    public void setStore(Store newValue) {
        Store old = this.store;
        this.store = newValue;
        firePropertyChange(P_Store, old, this.store);
    }
    public boolean getUseStoreSearch() {
        return useStoreSearch;
    }
    public void setUseStoreSearch(boolean newValue) {
        boolean old = this.useStoreSearch;
        this.useStoreSearch = newValue;
        firePropertyChange(P_UseStoreSearch, old, this.useStoreSearch);
    }
    public StoreSearch getStoreSearch() {
        return this.searchStore;
    }
    public void setStoreSearch(StoreSearch newValue) {
        this.searchStore = newValue;
    }

    @OAOne
    public Batch getBatch() {
        if (batch == null) {
            batch = (Batch) getObject(P_Batch);
        }
        return batch;
    }
    public void setBatch(Batch newValue) {
        Batch old = this.batch;
        this.batch = newValue;
        firePropertyChange(P_Batch, old, this.batch);
    }
    public boolean getUseBatchSearch() {
        return useBatchSearch;
    }
    public void setUseBatchSearch(boolean newValue) {
        boolean old = this.useBatchSearch;
        this.useBatchSearch = newValue;
        firePropertyChange(P_UseBatchSearch, old, this.useBatchSearch);
    }
    public BatchSearch getBatchSearch() {
        return this.searchBatch;
    }
    public void setBatchSearch(BatchSearch newValue) {
        this.searchBatch = newValue;
    }

    public void reset() {
        setMessageType(null);
        setMessageName(null);
        setMessageData(null);
        setStore(null);
        setUseStoreSearch(false);
        setBatch(null);
        setUseBatchSearch(false);
    }

    public boolean isDataEntered() {
        if (getMessageType() != null) return true;
        if (getMessageName() != null) return true;
        if (getMessageData() != null) return true;
        if (getStore() != null) return true;
        if (getUseStoreSearch()) return true;
        if (getBatch() != null) return true;
        if (getUseBatchSearch()) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<Receive> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<Receive> f = new OAQueryFilter<Receive>(Receive.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<Receive> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<Receive> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<Receive> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        OAFinder finder = null;
        if (OAString.isNotEmpty(this.messageType)) {
            if (sql.length() > 0) sql += " AND ";
            String value = OAString.convertToLikeSearch(messageType);
            if (value.indexOf("%") >= 0) {
                sql += Receive.P_MessageType + " LIKE ?";
            }
            else {
                sql += Receive.P_MessageType + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (OAString.isNotEmpty(this.messageName)) {
            if (sql.length() > 0) sql += " AND ";
            String value = OAString.convertToLikeSearch(messageName);
            if (value.indexOf("%") >= 0) {
                sql += Receive.P_MessageName + " LIKE ?";
            }
            else {
                sql += Receive.P_MessageName + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (OAString.isNotEmpty(this.messageData)) {
            if (sql.length() > 0) sql += " AND ";
            String value = OAString.convertToLikeSearch(messageData);
            if (value.indexOf("%") >= 0) {
                sql += Receive.P_MessageData + " LIKE ?";
            }
            else {
                sql += Receive.P_MessageData + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (!useStoreSearch && getStore() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += ReceivePP.store().pp + " = ?";
            args = OAArray.add(Object.class, args, getStore());
        }
        if (!useBatchSearch && getBatch() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += ReceivePP.batch().pp + " = ?";
            args = OAArray.add(Object.class, args, getBatch());
            finder = new OAFinder<Batch, Receive>(getBatch(), Batch.P_Receives);
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<Receive> select = new OASelect<Receive>(Receive.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useStoreSearch && getStoreSearch() != null) {
            getStoreSearch().appendSelect(ReceivePP.store().pp, select);
        }
        if (useBatchSearch && getBatchSearch() != null) {
            getBatchSearch().appendSelect(ReceivePP.batch().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (OAString.isNotEmpty(this.messageType)) {
            if (sql.length() > 0) sql += " AND ";
            String value = OAString.convertToLikeSearch(messageType);
            if (value.indexOf("%") >= 0) {
                sql += prefix + Receive.P_MessageType + " LIKE ?";
            }
            else {
                sql += prefix + Receive.P_MessageType + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (OAString.isNotEmpty(this.messageName)) {
            if (sql.length() > 0) sql += " AND ";
            String value = OAString.convertToLikeSearch(messageName);
            if (value.indexOf("%") >= 0) {
                sql += prefix + Receive.P_MessageName + " LIKE ?";
            }
            else {
                sql += prefix + Receive.P_MessageName + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (OAString.isNotEmpty(this.messageData)) {
            if (sql.length() > 0) sql += " AND ";
            String value = OAString.convertToLikeSearch(messageData);
            if (value.indexOf("%") >= 0) {
                sql += prefix + Receive.P_MessageData + " LIKE ?";
            }
            else {
                sql += prefix + Receive.P_MessageData + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (!useStoreSearch && getStore() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ReceivePP.store().pp + " = ?";
            args = OAArray.add(Object.class, args, getStore());
        }
        if (useStoreSearch && getStoreSearch() != null) {
            getStoreSearch().appendSelect(prefix + ReceivePP.store().pp, select);
        }
        if (!useBatchSearch && getBatch() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ReceivePP.batch().pp + " = ?";
            args = OAArray.add(Object.class, args, getBatch());
        }
        if (useBatchSearch && getBatchSearch() != null) {
            getBatchSearch().appendSelect(prefix + ReceivePP.batch().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<Receive> filterDataSourceFilter;
    public OAFilter<Receive> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<Receive>() {
            @Override
            public boolean isUsed(Receive receive) {
                return ReceiveSearch.this.isUsedForDataSourceFilter(receive);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<Receive> filterCustomFilter;
    public OAFilter<Receive> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<Receive>() {
            @Override
            public boolean isUsed(Receive receive) {
                boolean b = ReceiveSearch.this.isUsedForCustomFilter(receive);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(Receive searchReceive) {
        return true;
    }
    public boolean isUsedForCustomFilter(Receive searchReceive) {
        return true;
    }
}