// Generated by OABuilder
package com.messagedesigner.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.scheduler.*;
import com.viaoa.util.*;
import com.messagedesigner.delegate.oa.*;
import com.messagedesigner.model.oa.filter.*;
import com.messagedesigner.model.oa.propertypath.*;
import com.messagedesigner.delegate.oa.MessageGroupDelegate;
import com.messagedesigner.model.oa.MessageGroup;
import com.messagedesigner.model.oa.MessageRecord;
import com.messagedesigner.model.oa.MessageType;
import com.messagedesigner.model.oa.MessageTypeColumn;
import com.messagedesigner.model.oa.MessageTypeRecord;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;
 
@OAClass(
    lowerName = "messageGroup",
    pluralName = "MessageGroups",
    shortName = "msg",
    displayName = "Message Group",
    displayProperty = "name"
)
@OATable(
    indexes = {
        @OAIndex(name = "MessageGroupMessageType", fkey = true, columns = { @OAIndexColumn(name = "MessageTypeId") })
    }
)
public class MessageGroup extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(MessageGroup.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
     
    public static final String P_PojoCode = "pojoCode";
     
    public static final String P_MessageRecords = "messageRecords";
    public static final String P_MessageType = "messageType";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
     
    // Links to other objects.
    protected transient Hub<MessageRecord> hubMessageRecords;
    protected volatile transient MessageType messageType;
     
    public MessageGroup() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public MessageGroup(int id) {
        this();
        setId(id);
    }
     

    @OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId()
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }
    @OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }
    @OAProperty(maxLength = 55, displayLength = 15)
    @OAColumn(maxLength = 55)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
    @OACalculatedProperty(displayName = "Pojo Code", displayLength = 30, columnLength = 20, properties = {P_Name, P_MessageRecords, P_MessageRecords+"."+MessageRecord.P_MessageTypeRecord+"."+MessageTypeRecord.P_Name, P_MessageRecords+"."+MessageRecord.P_MessageTypeRecord+"."+MessageTypeRecord.P_MessageTypeColumns+"."+MessageTypeColumn.P_Name, P_MessageRecords+"."+MessageRecord.P_MessageTypeRecord+"."+MessageTypeRecord.P_MessageTypeColumns+"."+MessageTypeColumn.P_RpgType})
    public String getPojoCode() {
        String pojoCode = MessageGroupDelegate.getPojoCode(this);
        return pojoCode;
    }
    @OAMany(
        displayName = "Message Records", 
        toClass = MessageRecord.class, 
        reverseName = MessageRecord.P_MessageGroup
    )
    public Hub<MessageRecord> getMessageRecords() {
        if (hubMessageRecords == null) {
            hubMessageRecords = (Hub<MessageRecord>) getHub(P_MessageRecords);
        }
        return hubMessageRecords;
    }
    @OAOne(
        displayName = "Message Type", 
        reverseName = MessageType.P_MessageGroups, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"MessageTypeId"})
    public MessageType getMessageType() {
        if (messageType == null) {
            messageType = (MessageType) getObject(P_MessageType);
        }
        return messageType;
    }
    public void setMessageType(MessageType newValue) {
        MessageType old = this.messageType;
        fireBeforePropertyChange(P_MessageType, old, newValue);
        this.messageType = newValue;
        firePropertyChange(P_MessageType, old, this.messageType);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        int messageTypeFkey = rs.getInt(4);
        if (!rs.wasNull() && messageTypeFkey > 0) {
            setProperty(P_MessageType, new OAObjectKey(messageTypeFkey));
        }
        if (rs.getMetaData().getColumnCount() != 4) {
            throw new SQLException("invalid number of columns for load method");
        }

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 