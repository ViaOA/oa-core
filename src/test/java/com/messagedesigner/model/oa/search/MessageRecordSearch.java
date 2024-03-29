// Generated by OABuilder
package com.messagedesigner.model.oa.search;

import java.util.logging.*;

import com.messagedesigner.model.oa.*;
import com.messagedesigner.model.oa.propertypath.*;
import com.messagedesigner.model.oa.MessageRecord;
import com.messagedesigner.model.oa.MessageType;
import com.messagedesigner.model.oa.MessageTypeRecord;
import com.messagedesigner.model.oa.propertypath.MessageRecordPP;
import com.messagedesigner.model.oa.search.MessageRecordSearch;
import com.messagedesigner.model.oa.search.MessageTypeRecordSearch;
import com.messagedesigner.model.oa.search.MessageTypeSearch;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.util.OADateTime;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;

@OAClass(useDataSource=false, localOnly=true)
public class MessageRecordSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(MessageRecordSearch.class.getName());
    public static final String P_Created = "Created";
    public static final String P_RelationshipType = "RelationshipType";
    public static final String P_RelationshipType2 = "RelationshipType2";
    public static final String P_MessageType = "MessageType";
    public static final String P_UseMessageTypeSearch = "UseMessageTypeSearch";
    public static final String P_MessageTypeRecord = "MessageTypeRecord";
    public static final String P_UseMessageTypeRecordSearch = "UseMessageTypeRecordSearch";
    public static final String P_MaxResults = "MaxResults";

    protected OADateTime created;
    protected int relationshipType;
    protected int relationshipType2;
    protected MessageType messageType;
    protected boolean useMessageTypeSearch;
    protected MessageTypeSearch searchMessageType;
    protected MessageTypeRecord messageTypeRecord;
    protected boolean useMessageTypeRecordSearch;
    protected MessageTypeRecordSearch searchMessageTypeRecord;
    protected int maxResults;

    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }
      

    public int getRelationshipType() {
        return relationshipType;
    }
    public void setRelationshipType(int newValue) {
        int old = relationshipType;
        fireBeforePropertyChange(P_RelationshipType, old, newValue);
        this.relationshipType = newValue;
        firePropertyChange(P_RelationshipType, old, this.relationshipType);
        firePropertyChange(P_RelationshipType + "String");
        firePropertyChange(P_RelationshipType + "Enum");
        if (isLoading()) return;
        if (relationshipType > relationshipType2) setRelationshipType2(this.relationshipType);
    } 
    public int getRelationshipType2() {
        return relationshipType2;
    }
    public void setRelationshipType2(int newValue) {
        int old = relationshipType2;
        fireBeforePropertyChange(P_RelationshipType2, old, newValue);
        this.relationshipType2 = newValue;
        firePropertyChange(P_RelationshipType2, old, this.relationshipType2);
        firePropertyChange(P_RelationshipType + "String");
        firePropertyChange(P_RelationshipType + "Enum");
        if (isLoading()) return;
        if (relationshipType > relationshipType2) setRelationshipType(this.relationshipType2);
    }

    public String getRelationshipTypeString() {
        MessageRecord.RelationshipType relationshipType = getRelationshipTypeEnum();
        if (relationshipType == null) return null;
        return relationshipType.name();
    }
    public void setRelationshipTypeString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            MessageRecord.RelationshipType relationshipType = MessageRecord.RelationshipType.valueOf(val);
            if (relationshipType != null) x = relationshipType.ordinal();
        }
        if (x < 0) setNull(P_RelationshipType);
        else setRelationshipType(x);
    }


    public MessageRecord.RelationshipType getRelationshipTypeEnum() {
        if (isNull(P_RelationshipType)) return null;
        final int val = getRelationshipType();
        if (val < 0 || val >= MessageRecord.RelationshipType.values().length) return null;
        return MessageRecord.RelationshipType.values()[val];
    }

    public void setRelationshipTypeEnum(MessageRecord.RelationshipType val) {
        if (val == null) {
            setNull(P_RelationshipType);
        }
        else {
            setRelationshipType(val.ordinal());
        }
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
    public MessageType getMessageType() {
        if (messageType == null) {
            messageType = (MessageType) getObject(P_MessageType);
        }
        return messageType;
    }
    public void setMessageType(MessageType newValue) {
        MessageType old = this.messageType;
        this.messageType = newValue;
        firePropertyChange(P_MessageType, old, this.messageType);
    }
    public boolean getUseMessageTypeSearch() {
        return useMessageTypeSearch;
    }
    public void setUseMessageTypeSearch(boolean newValue) {
        boolean old = this.useMessageTypeSearch;
        this.useMessageTypeSearch = newValue;
        firePropertyChange(P_UseMessageTypeSearch, old, this.useMessageTypeSearch);
    }
    public MessageTypeSearch getMessageTypeSearch() {
        return this.searchMessageType;
    }
    public void setMessageTypeSearch(MessageTypeSearch newValue) {
        this.searchMessageType = newValue;
    }

    @OAOne
    public MessageTypeRecord getMessageTypeRecord() {
        if (messageTypeRecord == null) {
            messageTypeRecord = (MessageTypeRecord) getObject(P_MessageTypeRecord);
        }
        return messageTypeRecord;
    }
    public void setMessageTypeRecord(MessageTypeRecord newValue) {
        MessageTypeRecord old = this.messageTypeRecord;
        this.messageTypeRecord = newValue;
        firePropertyChange(P_MessageTypeRecord, old, this.messageTypeRecord);
    }
    public boolean getUseMessageTypeRecordSearch() {
        return useMessageTypeRecordSearch;
    }
    public void setUseMessageTypeRecordSearch(boolean newValue) {
        boolean old = this.useMessageTypeRecordSearch;
        this.useMessageTypeRecordSearch = newValue;
        firePropertyChange(P_UseMessageTypeRecordSearch, old, this.useMessageTypeRecordSearch);
    }
    public MessageTypeRecordSearch getMessageTypeRecordSearch() {
        return this.searchMessageTypeRecord;
    }
    public void setMessageTypeRecordSearch(MessageTypeRecordSearch newValue) {
        this.searchMessageTypeRecord = newValue;
    }

    public void reset() {
        setCreated(null);
        setRelationshipType(0);
        setNull(P_RelationshipType);
        setRelationshipType2(0);
        setNull(P_RelationshipType2);
        setMessageType(null);
        setUseMessageTypeSearch(false);
        setMessageTypeRecord(null);
        setUseMessageTypeRecordSearch(false);
    }

    public boolean isDataEntered() {
        if (getCreated() != null) return true;
        if (!isNull(P_RelationshipType)) return true;
        if (getMessageType() != null) return true;
        if (getUseMessageTypeSearch()) return true;
        if (getMessageTypeRecord() != null) return true;
        if (getUseMessageTypeRecordSearch()) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<MessageRecord> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<MessageRecord> f = new OAQueryFilter<MessageRecord>(MessageRecord.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<MessageRecord> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<MessageRecord> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<MessageRecord> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        OAFinder finder = null;
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += MessageRecord.P_Created + " = ?";
            args = OAArray.add(Object.class, args, this.created);
        }
        if (!isNull(P_RelationshipType)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_RelationshipType2) && relationshipType != relationshipType2) {
                sql += MessageRecord.P_RelationshipType + " >= ?";
                args = OAArray.add(Object.class, args, getRelationshipType());
                sql += " AND " + MessageRecord.P_RelationshipType + " <= ?";
                args = OAArray.add(Object.class, args, getRelationshipType2());
            }
            else {
                sql += MessageRecord.P_RelationshipType + " = ?";
                args = OAArray.add(Object.class, args, getRelationshipType());
            }
        }
        if (!useMessageTypeSearch && getMessageType() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += MessageRecordPP.messageType().pp + " = ?";
            args = OAArray.add(Object.class, args, getMessageType());
            finder = new OAFinder<MessageType, MessageRecord>(getMessageType(), MessageType.P_MessageRecords);
        }
        if (!useMessageTypeRecordSearch && getMessageTypeRecord() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += MessageRecordPP.messageTypeRecord().pp + " = ?";
            args = OAArray.add(Object.class, args, getMessageTypeRecord());
            finder = new OAFinder<MessageTypeRecord, MessageRecord>(getMessageTypeRecord(), MessageTypeRecord.P_MessageRecords);
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<MessageRecord> select = new OASelect<MessageRecord>(MessageRecord.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useMessageTypeSearch && getMessageTypeSearch() != null) {
            getMessageTypeSearch().appendSelect(MessageRecordPP.messageType().pp, select);
        }
        if (useMessageTypeRecordSearch && getMessageTypeRecordSearch() != null) {
            getMessageTypeRecordSearch().appendSelect(MessageRecordPP.messageTypeRecord().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + MessageRecord.P_Created + " = ?";
            args = OAArray.add(Object.class, args, this.created);
        }
        if (!isNull(P_RelationshipType)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_RelationshipType2) && relationshipType != relationshipType2) {
                sql += prefix + MessageRecord.P_RelationshipType + " >= ?";
                args = OAArray.add(Object.class, args, getRelationshipType());
                sql += " AND " + prefix + MessageRecord.P_RelationshipType + " <= ?";
                args = OAArray.add(Object.class, args, getRelationshipType2());
            }
            else {
                sql += prefix + MessageRecord.P_RelationshipType + " = ?";
                args = OAArray.add(Object.class, args, getRelationshipType());
            }
        }
        if (!useMessageTypeSearch && getMessageType() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + MessageRecordPP.messageType().pp + " = ?";
            args = OAArray.add(Object.class, args, getMessageType());
        }
        if (useMessageTypeSearch && getMessageTypeSearch() != null) {
            getMessageTypeSearch().appendSelect(prefix + MessageRecordPP.messageType().pp, select);
        }
        if (!useMessageTypeRecordSearch && getMessageTypeRecord() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + MessageRecordPP.messageTypeRecord().pp + " = ?";
            args = OAArray.add(Object.class, args, getMessageTypeRecord());
        }
        if (useMessageTypeRecordSearch && getMessageTypeRecordSearch() != null) {
            getMessageTypeRecordSearch().appendSelect(prefix + MessageRecordPP.messageTypeRecord().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<MessageRecord> filterDataSourceFilter;
    public OAFilter<MessageRecord> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<MessageRecord>() {
            @Override
            public boolean isUsed(MessageRecord messageRecord) {
                return MessageRecordSearch.this.isUsedForDataSourceFilter(messageRecord);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<MessageRecord> filterCustomFilter;
    public OAFilter<MessageRecord> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<MessageRecord>() {
            @Override
            public boolean isUsed(MessageRecord messageRecord) {
                boolean b = MessageRecordSearch.this.isUsedForCustomFilter(messageRecord);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(MessageRecord searchMessageRecord) {
        return true;
    }
    public boolean isUsedForCustomFilter(MessageRecord searchMessageRecord) {
        return true;
    }
}
