// Generated by OABuilder
package com.corptostore.model.oa.search;

import java.util.*;
import java.util.logging.*;

import com.corptostore.model.oa.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.Store;
import com.corptostore.model.oa.Transmit;
import com.corptostore.model.oa.TransmitBatch;
import com.corptostore.model.oa.propertypath.TransmitPP;
import com.corptostore.model.oa.search.StoreSearch;
import com.corptostore.model.oa.search.TransmitBatchSearch;
import com.corptostore.model.oa.search.TransmitSearch;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.util.OADate;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;

@OAClass(useDataSource=false, localOnly=true)
public class TransmitSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(TransmitSearch.class.getName());

    public static final String P_TransmitId = "TransmitId";
    public static final String P_TransmitId2 = "TransmitId2";
    public static final String P_MessageCode = "MessageCode";
    public static final String P_MessageCode2 = "MessageCode2";
    public static final String P_StoreStoreNumber = "StoreStoreNumber";
    public static final String P_StoreStoreNumber2 = "StoreStoreNumber2";
    public static final String P_TransmitBatchTransmitBatchDate = "TransmitBatchTransmitBatchDate";
    public static final String P_Store = "Store";
    public static final String P_UseStoreSearch = "UseStoreSearch";
    public static final String P_TransmitBatch = "TransmitBatch";
    public static final String P_UseTransmitBatchSearch = "UseTransmitBatchSearch";
    public static final String P_MaxResults = "MaxResults";

    protected long transmitId;
    protected long transmitId2;
    protected int messageCode;
    protected int messageCode2;
    protected int storeStoreNumber;
    protected int storeStoreNumber2;
    protected OADate transmitBatchTransmitBatchDate;
    protected Store store;
    protected boolean useStoreSearch;
    protected StoreSearch searchStore;
    protected TransmitBatch transmitBatch;
    protected boolean useTransmitBatchSearch;
    protected TransmitBatchSearch searchTransmitBatch;
    protected int maxResults;

    public long getTransmitId() {
        return transmitId;
    }
    public void setTransmitId(long newValue) {
        long old = transmitId;
        fireBeforePropertyChange(P_TransmitId, old, newValue);
        this.transmitId = newValue;
        firePropertyChange(P_TransmitId, old, this.transmitId);
        if (isLoading()) return;
        if (transmitId > transmitId2) setTransmitId2(this.transmitId);
    } 
    public long getTransmitId2() {
        return transmitId2;
    }
    public void setTransmitId2(long newValue) {
        long old = transmitId2;
        fireBeforePropertyChange(P_TransmitId2, old, newValue);
        this.transmitId2 = newValue;
        firePropertyChange(P_TransmitId2, old, this.transmitId2);
        if (isLoading()) return;
        if (transmitId > transmitId2) setTransmitId(this.transmitId2);
    }
    public int getMessageCode() {
        return messageCode;
    }
    public void setMessageCode(int newValue) {
        int old = messageCode;
        fireBeforePropertyChange(P_MessageCode, old, newValue);
        this.messageCode = newValue;
        firePropertyChange(P_MessageCode, old, this.messageCode);
        if (isLoading()) return;
        if (messageCode > messageCode2) setMessageCode2(this.messageCode);
    } 
    public int getMessageCode2() {
        return messageCode2;
    }
    public void setMessageCode2(int newValue) {
        int old = messageCode2;
        fireBeforePropertyChange(P_MessageCode2, old, newValue);
        this.messageCode2 = newValue;
        firePropertyChange(P_MessageCode2, old, this.messageCode2);
        if (isLoading()) return;
        if (messageCode > messageCode2) setMessageCode(this.messageCode2);
    }
    public int getStoreStoreNumber() {
        return storeStoreNumber;
    }
    public void setStoreStoreNumber(int newValue) {
        int old = storeStoreNumber;
        fireBeforePropertyChange(P_StoreStoreNumber, old, newValue);
        this.storeStoreNumber = newValue;
        firePropertyChange(P_StoreStoreNumber, old, this.storeStoreNumber);
        if (isLoading()) return;
        if (storeStoreNumber > storeStoreNumber2) setStoreStoreNumber2(this.storeStoreNumber);
    } 
    public int getStoreStoreNumber2() {
        return storeStoreNumber2;
    }
    public void setStoreStoreNumber2(int newValue) {
        int old = storeStoreNumber2;
        fireBeforePropertyChange(P_StoreStoreNumber2, old, newValue);
        this.storeStoreNumber2 = newValue;
        firePropertyChange(P_StoreStoreNumber2, old, this.storeStoreNumber2);
        if (isLoading()) return;
        if (storeStoreNumber > storeStoreNumber2) setStoreStoreNumber(this.storeStoreNumber2);
    }
    public OADate getTransmitBatchTransmitBatchDate() {
        return transmitBatchTransmitBatchDate;
    }
    public void setTransmitBatchTransmitBatchDate(OADate newValue) {
        OADate old = transmitBatchTransmitBatchDate;
        fireBeforePropertyChange(P_TransmitBatchTransmitBatchDate, old, newValue);
        this.transmitBatchTransmitBatchDate = newValue;
        firePropertyChange(P_TransmitBatchTransmitBatchDate, old, this.transmitBatchTransmitBatchDate);
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
    public TransmitBatch getTransmitBatch() {
        if (transmitBatch == null) {
            transmitBatch = (TransmitBatch) getObject(P_TransmitBatch);
        }
        return transmitBatch;
    }
    public void setTransmitBatch(TransmitBatch newValue) {
        TransmitBatch old = this.transmitBatch;
        this.transmitBatch = newValue;
        firePropertyChange(P_TransmitBatch, old, this.transmitBatch);
    }
    public boolean getUseTransmitBatchSearch() {
        return useTransmitBatchSearch;
    }
    public void setUseTransmitBatchSearch(boolean newValue) {
        boolean old = this.useTransmitBatchSearch;
        this.useTransmitBatchSearch = newValue;
        firePropertyChange(P_UseTransmitBatchSearch, old, this.useTransmitBatchSearch);
    }
    public TransmitBatchSearch getTransmitBatchSearch() {
        return this.searchTransmitBatch;
    }
    public void setTransmitBatchSearch(TransmitBatchSearch newValue) {
        this.searchTransmitBatch = newValue;
    }

    public void reset() {
        setTransmitId(0);
        setNull(P_TransmitId);
        setTransmitId2(0);
        setNull(P_TransmitId2);
        setMessageCode(0);
        setNull(P_MessageCode);
        setMessageCode2(0);
        setNull(P_MessageCode2);
        setStoreStoreNumber(0);
        setNull(P_StoreStoreNumber);
        setStoreStoreNumber2(0);
        setNull(P_StoreStoreNumber2);
        setTransmitBatchTransmitBatchDate(null);
        setStore(null);
        setUseStoreSearch(false);
        setTransmitBatch(null);
        setUseTransmitBatchSearch(false);
    }

    public boolean isDataEntered() {
        if (!isNull(P_TransmitId)) return true;
        if (!isNull(P_MessageCode)) return true;
        if (!isNull(P_StoreStoreNumber)) return true;
        if (getTransmitBatchTransmitBatchDate() != null) return true;
        if (getStore() != null) return true;
        if (getUseStoreSearch()) return true;
        if (getTransmitBatch() != null) return true;
        if (getUseTransmitBatchSearch()) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<Transmit> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<Transmit> f = new OAQueryFilter<Transmit>(Transmit.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<Transmit> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<Transmit> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<Transmit> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (!isNull(P_TransmitId)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_TransmitId2) && transmitId != transmitId2) {
                sql += Transmit.P_TransmitId + " >= ?";
                args = OAArray.add(Object.class, args, getTransmitId());
                sql += " AND " + Transmit.P_TransmitId + " <= ?";
                args = OAArray.add(Object.class, args, getTransmitId2());
            }
            else {
                sql += Transmit.P_TransmitId + " = ?";
                args = OAArray.add(Object.class, args, getTransmitId());
            }
        }
        if (!isNull(P_MessageCode)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_MessageCode2) && messageCode != messageCode2) {
                sql += Transmit.P_MessageCode + " >= ?";
                args = OAArray.add(Object.class, args, getMessageCode());
                sql += " AND " + Transmit.P_MessageCode + " <= ?";
                args = OAArray.add(Object.class, args, getMessageCode2());
            }
            else {
                sql += Transmit.P_MessageCode + " = ?";
                args = OAArray.add(Object.class, args, getMessageCode());
            }
        }
        if (!isNull(P_StoreStoreNumber)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_StoreStoreNumber2) && storeStoreNumber != storeStoreNumber2) {
                sql += TransmitPP.store().storeNumber() + " >= ?";
                args = OAArray.add(Object.class, args, getStoreStoreNumber());
                sql += " AND " + TransmitPP.store().storeNumber() + " <= ?";
                args = OAArray.add(Object.class, args, getStoreStoreNumber2());
            }
            else {
                sql += TransmitPP.store().storeNumber() + " = ?";
                args = OAArray.add(Object.class, args, getStoreStoreNumber());
            }
        }
        if (transmitBatchTransmitBatchDate != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += TransmitPP.transmitBatch().transmitBatchDate() + " = ?";
            args = OAArray.add(Object.class, args, this.transmitBatchTransmitBatchDate);
        }
        if (!useStoreSearch && getStore() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += TransmitPP.store().pp + " = ?";
            args = OAArray.add(Object.class, args, getStore());
        }
        if (!useTransmitBatchSearch && getTransmitBatch() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += TransmitPP.transmitBatch().pp + " = ?";
            args = OAArray.add(Object.class, args, getTransmitBatch());
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<Transmit> select = new OASelect<Transmit>(Transmit.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useStoreSearch && getStoreSearch() != null) {
            getStoreSearch().appendSelect(TransmitPP.store().pp, select);
        }
        if (useTransmitBatchSearch && getTransmitBatchSearch() != null) {
            getTransmitBatchSearch().appendSelect(TransmitPP.transmitBatch().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (!isNull(P_TransmitId)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_TransmitId2) && transmitId != transmitId2) {
                sql += prefix + Transmit.P_TransmitId + " >= ?";
                args = OAArray.add(Object.class, args, getTransmitId());
                sql += " AND " + prefix + Transmit.P_TransmitId + " <= ?";
                args = OAArray.add(Object.class, args, getTransmitId2());
            }
            else {
                sql += prefix + Transmit.P_TransmitId + " = ?";
                args = OAArray.add(Object.class, args, getTransmitId());
            }
        }
        if (!isNull(P_MessageCode)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_MessageCode2) && messageCode != messageCode2) {
                sql += prefix + Transmit.P_MessageCode + " >= ?";
                args = OAArray.add(Object.class, args, getMessageCode());
                sql += " AND " + prefix + Transmit.P_MessageCode + " <= ?";
                args = OAArray.add(Object.class, args, getMessageCode2());
            }
            else {
                sql += prefix + Transmit.P_MessageCode + " = ?";
                args = OAArray.add(Object.class, args, getMessageCode());
            }
        }
        if (!isNull(P_StoreStoreNumber)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_StoreStoreNumber2) && storeStoreNumber != storeStoreNumber2) {
                sql += prefix + TransmitPP.store().storeNumber() + " >= ?";
                args = OAArray.add(Object.class, args, getStoreStoreNumber());
                sql += " AND " + prefix + TransmitPP.store().storeNumber() + " <= ?";
                args = OAArray.add(Object.class, args, getStoreStoreNumber2());
            }
            else {
                sql += prefix + TransmitPP.store().storeNumber() + " = ?";
                args = OAArray.add(Object.class, args, getStoreStoreNumber());
            }
        }
        if (transmitBatchTransmitBatchDate != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + TransmitPP.transmitBatch().transmitBatchDate() + " = ?";
            args = OAArray.add(Object.class, args, this.transmitBatchTransmitBatchDate);
        }
        if (!useStoreSearch && getStore() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + TransmitPP.store().pp + " = ?";
            args = OAArray.add(Object.class, args, getStore());
        }
        if (useStoreSearch && getStoreSearch() != null) {
            getStoreSearch().appendSelect(prefix + TransmitPP.store().pp, select);
        }
        if (!useTransmitBatchSearch && getTransmitBatch() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + TransmitPP.transmitBatch().pp + " = ?";
            args = OAArray.add(Object.class, args, getTransmitBatch());
        }
        if (useTransmitBatchSearch && getTransmitBatchSearch() != null) {
            getTransmitBatchSearch().appendSelect(prefix + TransmitPP.transmitBatch().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<Transmit> filterDataSourceFilter;
    public OAFilter<Transmit> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<Transmit>() {
            @Override
            public boolean isUsed(Transmit transmit) {
                return TransmitSearch.this.isUsedForDataSourceFilter(transmit);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<Transmit> filterCustomFilter;
    public OAFilter<Transmit> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<Transmit>() {
            @Override
            public boolean isUsed(Transmit transmit) {
                boolean b = TransmitSearch.this.isUsedForCustomFilter(transmit);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(Transmit searchTransmit) {
        return true;
    }
    public boolean isUsedForCustomFilter(Transmit searchTransmit) {
        return true;
    }
}
