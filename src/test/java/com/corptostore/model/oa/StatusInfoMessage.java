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
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.StatusInfo;
import com.corptostore.model.oa.StatusInfoMessage;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;
 
@OAClass(
    lowerName = "statusInfoMessage",
    pluralName = "StatusInfoMessages",
    shortName = "sim",
    displayName = "Status Info Message",
    useDataSource = false,
    displayProperty = "id"
)
public class StatusInfoMessage extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StatusInfoMessage.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Type = "type";
    public static final String P_TypeAsString = "typeString";
    public static final String P_Message = "message";
    public static final String P_Counter = "counter";
    public static final String P_Exception = "exception";
     
     
    public static final String P_StatusInfo = "statusInfo";
    public static final String P_StatusInfoActivity = "statusInfoActivity";
    public static final String P_StatusInfoAlert = "statusInfoAlert";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int type;
    public static enum Type {
        status("Status"),
        activity("Activity"),
        alert("Alert");

        private String display;
        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE_status = 0;
    public static final int TYPE_activity = 1;
    public static final int TYPE_alert = 2;
    public static final Hub<String> hubType;
    static {
        hubType = new Hub<String>(String.class);
        hubType.addElement("Status");
        hubType.addElement("Activity");
        hubType.addElement("Alert");
    }
    protected volatile String message;
    protected volatile int counter;
    protected volatile String exception;
     
    // Links to other objects.
    protected volatile transient StatusInfo statusInfo;
     
    public StatusInfoMessage() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StatusInfoMessage(int id) {
        this();
        setId(id);
    }
     

    @OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId
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
    @OAProperty(defaultValue = "new OADateTime()", displayLength = 15, columnLength = 18, inputMask = "MM/dd hh:mm:ss.S", outputFormat = "MM/dd hh:mm:ss.S", isProcessed = true)
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
    @OAProperty(trackPrimitiveNull = false, displayLength = 6, isNameValue = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        int old = type;
        fireBeforePropertyChange(P_Type, old, newValue);
        this.type = newValue;
        firePropertyChange(P_Type, old, this.type);
        firePropertyChange(P_Type + "String");
        firePropertyChange(P_Type + "Enum");
    }

    public String getTypeString() {
        Type type = getTypeEnum();
        if (type == null) return null;
        return type.name();
    }
    public void setTypeString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            Type type = Type.valueOf(val);
            if (type != null) x = type.ordinal();
        }
        if (x < 0) setNull(P_Type);
        else setType(x);
    }


    public Type getTypeEnum() {
        if (isNull(P_Type)) return null;
        final int val = getType();
        if (val < 0 || val >= Type.values().length) return null;
        return Type.values()[val];
    }

    public void setTypeEnum(Type val) {
        if (val == null) {
            setNull(P_Type);
        }
        else {
            setType(val.ordinal());
        }
    }
    @OAProperty(maxLength = 250, displayLength = 25, columnLength = 20)
    @OAColumn(maxLength = 250)
    public String getMessage() {
        return message;
    }
    public void setMessage(String newValue) {
        String old = message;
        fireBeforePropertyChange(P_Message, old, newValue);
        this.message = newValue;
        firePropertyChange(P_Message, old, this.message);
    }
    @OAProperty(displayLength = 5, columnLength = 7, importMatch = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getCounter() {
        return counter;
    }
    public void setCounter(int newValue) {
        int old = counter;
        fireBeforePropertyChange(P_Counter, old, newValue);
        this.counter = newValue;
        firePropertyChange(P_Counter, old, this.counter);
    }
    @OAProperty(displayLength = 30, columnLength = 20)
    @OAColumn(sqlType = java.sql.Types.CLOB)
    public String getException() {
        return exception;
    }
    public void setException(String newValue) {
        String old = exception;
        fireBeforePropertyChange(P_Exception, old, newValue);
        this.exception = newValue;
        firePropertyChange(P_Exception, old, this.exception);
    }
    @OAOne(
        displayName = "Status Info", 
        reverseName = StatusInfo.P_StatusInfoMessages, 
        required = true, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    @OAFkey(columns = {"status_info_id"})
    public StatusInfo getStatusInfo() {
        if (statusInfo == null) {
            statusInfo = (StatusInfo) getObject(P_StatusInfo);
        }
        return statusInfo;
    }
    public void setStatusInfo(StatusInfo newValue) {
        StatusInfo old = this.statusInfo;
        fireBeforePropertyChange(P_StatusInfo, old, newValue);
        this.statusInfo = newValue;
        firePropertyChange(P_StatusInfo, old, this.statusInfo);
    }
    @OAOne(
        displayName = "Status Info", 
        reverseName = StatusInfo.P_StatusInfoActivityMessages, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    @OALinkTable(name = "status_info_status_info_message", indexName = "status_info_status_info_activity_message", columns = {"status_info_message_id"})
    private StatusInfo getStatusInfoActivity() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    @OAOne(
        displayName = "Status Info", 
        reverseName = StatusInfo.P_StatusInfoAlertMessages, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    @OALinkTable(name = "status_info_status_info_message2", indexName = "status_info_status_info_alert_message", columns = {"status_info_message_id"})
    private StatusInfo getStatusInfoAlert() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
}
 