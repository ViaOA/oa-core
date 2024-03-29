// Generated by OABuilder
package com.corptostore.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.scheduler.*;
import com.viaoa.util.*;
import com.corptostore.delegate.oa.*;
import com.corptostore.model.oa.filter.*;
import com.corptostore.model.oa.method.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.Batch;
import com.corptostore.model.oa.Send;
import com.corptostore.model.oa.Store;
import com.corptostore.model.oa.Transmit;
import com.corptostore.model.oa.filter.SendClosedFilter;
import com.corptostore.model.oa.filter.SendOpenFilter;
import com.corptostore.model.oa.method.SendResendMethod;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;
 
@OAClass(
    lowerName = "send",
    pluralName = "Sends",
    shortName = "snd",
    displayName = "Send",
    displayProperty = "sendId",
    sortProperty = "sendId",
    filterClasses = {SendOpenFilter.class, SendClosedFilter.class}
)
@OATable(
    name = "message_service_send",
    indexes = {
        @OAIndex(name = "message_service_send_batch", fkey = true, columns = { @OAIndexColumn(name = "batch_id") }), 
        @OAIndex(name = "message_service_send_begin_transmit", fkey = true, columns = { @OAIndexColumn(name = "begin_transmit_id") }), 
        @OAIndex(name = "message_service_send_store", fkey = true, columns = { @OAIndexColumn(name = "store_number") })
    }
)
public class Send extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Send.class.getName());

    public static final String P_SendId = "sendId";
    public static final String P_BatchSequenceNumber = "batchSequenceNumber";
    public static final String P_Created = "created";
    public static final String P_SentDate = "sentDate";
    public static final String P_MessageType = "messageType";
    public static final String P_MessageName = "messageName";
    public static final String P_MessageData = "messageData";
     
     
    public static final String P_Batch = "batch";
    public static final String P_BeginAllStoreBatches = "beginAllStoreBatches";
    public static final String P_BeginTransmit = "beginTransmit";
    public static final String P_EndAllStoreBatches = "endAllStoreBatches";
    public static final String P_Store = "store";
     
    public static final String M_Resend = "resend";
    protected volatile long sendId;
    protected volatile int batchSequenceNumber;
    protected volatile OADateTime created;
    protected volatile OADateTime sentDate;
    protected volatile String messageType;
    protected volatile String messageName;
    protected volatile String messageData;
     
    // Links to other objects.
    protected volatile transient Batch batch;
    protected volatile transient Transmit beginTransmit;
    protected volatile transient Store store;
     
    public Send() {
        if (!isLoading()) setObjectDefaults();
    }
     
    public Send(long sendId) {
        this();
        setSendId(sendId);
    }
     

    @OAProperty(displayName = "Send Id", isUnique = true, trackPrimitiveNull = false, displayLength = 6, columnLength = 7)
    @OAId
    @OAColumn(name = "send_id", sqlType = java.sql.Types.BIGINT)
    public long getSendId() {
        return sendId;
    }
    public void setSendId(long newValue) {
        long old = sendId;
        fireBeforePropertyChange(P_SendId, old, newValue);
        this.sendId = newValue;
        firePropertyChange(P_SendId, old, this.sendId);
    }
    @OAProperty(displayName = "Batch Sequence Number", displayLength = 6, columnLength = 21)
    @OAColumn(name = "batch_sequence_number", sqlType = java.sql.Types.INTEGER)
    public int getBatchSequenceNumber() {
        return batchSequenceNumber;
    }
    public void setBatchSequenceNumber(int newValue) {
        int old = batchSequenceNumber;
        fireBeforePropertyChange(P_BatchSequenceNumber, old, newValue);
        this.batchSequenceNumber = newValue;
        firePropertyChange(P_BatchSequenceNumber, old, this.batchSequenceNumber);
    }
    @OAProperty(displayLength = 15)
    @OAColumn(name = "created_date", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }
    @OAProperty(displayName = "Sent Date", displayLength = 15)
    @OAColumn(name = "sent_date", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getSentDate() {
        return sentDate;
    }
    public void setSentDate(OADateTime newValue) {
        OADateTime old = sentDate;
        fireBeforePropertyChange(P_SentDate, old, newValue);
        this.sentDate = newValue;
        firePropertyChange(P_SentDate, old, this.sentDate);
    }
    @OAProperty(displayName = "Message Type", maxLength = 35, displayLength = 15)
    @OAColumn(name = "message_type", maxLength = 35)
    public String getMessageType() {
        return messageType;
    }
    public void setMessageType(String newValue) {
        String old = messageType;
        fireBeforePropertyChange(P_MessageType, old, newValue);
        this.messageType = newValue;
        firePropertyChange(P_MessageType, old, this.messageType);
    }
    @OAProperty(displayName = "Message Name", maxLength = 50, displayLength = 15, columnLength = 14)
    @OAColumn(name = "message_name", maxLength = 50)
    public String getMessageName() {
        return messageName;
    }
    public void setMessageName(String newValue) {
        String old = messageName;
        fireBeforePropertyChange(P_MessageName, old, newValue);
        this.messageName = newValue;
        firePropertyChange(P_MessageName, old, this.messageName);
    }
    @OAProperty(displayName = "Message Data", displayLength = 30, columnLength = 20)
    @OAColumn(name = "message_data", sqlType = java.sql.Types.CLOB)
    public String getMessageData() {
        return messageData;
    }
    public void setMessageData(String newValue) {
        String old = messageData;
        fireBeforePropertyChange(P_MessageData, old, newValue);
        this.messageData = newValue;
        firePropertyChange(P_MessageData, old, this.messageData);
    }
    @OAOne(
        reverseName = Batch.P_Sends, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        pojoNames = { "batchId" }, 
        equalPropertyPath = "toStore"
    )
    @OAFkey(columns = {"batch_id"})
    public Batch getBatch() {
        if (batch == null) {
            batch = (Batch) getObject(P_Batch);
        }
        return batch;
    }
    public void setBatch(Batch newValue) {
        Batch old = this.batch;
        fireBeforePropertyChange(P_Batch, old, newValue);
        this.batch = newValue;
        firePropertyChange(P_Batch, old, this.batch);
    }
    @OAMany(
        displayName = "Begin All Store Batches", 
        toClass = Batch.class, 
        reverseName = Batch.P_BeginAllStoreSend, 
        createMethod = false
    )
    private Hub<Batch> getBeginAllStoreBatches() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    @OAOne(
        displayName = "Begin Transmit", 
        reverseName = Transmit.P_Send, 
        allowCreateNew = false, 
        pojoNames = { "beginTransmitId" }
    )
    @OAFkey(columns = {"begin_transmit_id"})
    public Transmit getBeginTransmit() {
        if (beginTransmit == null) {
            beginTransmit = (Transmit) getObject(P_BeginTransmit);
        }
        return beginTransmit;
    }
    public void setBeginTransmit(Transmit newValue) {
        Transmit old = this.beginTransmit;
        fireBeforePropertyChange(P_BeginTransmit, old, newValue);
        this.beginTransmit = newValue;
        firePropertyChange(P_BeginTransmit, old, this.beginTransmit);
    }
    @OAMany(
        displayName = "End All Store Batches", 
        toClass = Batch.class, 
        reverseName = Batch.P_EndAllStoreSend, 
        createMethod = false
    )
    private Hub<Batch> getEndAllStoreBatches() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    @OAOne(
        reverseName = Store.P_Sends, 
        required = true, 
        allowCreateNew = false, 
        pojoNames = { "storeNumber" }
    )
    @OAFkey(columns = {"store_number"})
    public Store getStore() {
        if (store == null) {
            store = (Store) getObject(P_Store);
        }
        return store;
    }
    public void setStore(Store newValue) {
        Store old = this.store;
        fireBeforePropertyChange(P_Store, old, newValue);
        this.store = newValue;
        firePropertyChange(P_Store, old, this.store);
    }
    @OAMethod(displayName = "Resend")
    public void resend(SendResendMethod data) {
        if (data == null) return;
        // todo: add custom code here
      // TEST ONLY, Remove this  qqqqqqqqqqqqqqqqqqqqqqqqqq
    }

    public void load(ResultSet rs, long sendId) throws SQLException {
        this.sendId = sendId;
        this.batchSequenceNumber = (int) rs.getInt(2);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Send.P_BatchSequenceNumber, true);
        }
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(3);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(4);
        if (timestamp != null) this.sentDate = new OADateTime(timestamp);
        this.messageType = rs.getString(5);
        this.messageName = rs.getString(6);
        this.messageData = rs.getString(7);
        long batchFkey = rs.getLong(8);
        if (!rs.wasNull() && batchFkey > 0) {
            setProperty(P_Batch, new OAObjectKey(batchFkey));
        }
        long beginTransmitFkey = rs.getLong(9);
        if (!rs.wasNull() && beginTransmitFkey > 0) {
            setProperty(P_BeginTransmit, new OAObjectKey(beginTransmitFkey));
        }
        int storeFkey = rs.getInt(10);
        if (!rs.wasNull() && storeFkey > 0) {
            setProperty(P_Store, new OAObjectKey(storeFkey));
        }
        if (rs.getMetaData().getColumnCount() != 10) {
            throw new SQLException("invalid number of columns for load method");
        }

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
