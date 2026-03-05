// Copied from OATemplate project by OABuilder 09/21/15 03:11 PM
package test.xice.tsam.model.oa;
 
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;

import test.xice.tsac.model.oa.ServerInfo.Status;

import com.viaoa.annotation.*;
 
@OAClass(
    shortName = "si",
    displayName = "Server Info",
    isLookup = true,
    isPreSelect = true,
    useDataSource = false,
    displayProperty = "dateTime"
)
public class ServerInfo extends OAObject {
    private static final long serialVersionUID = 1L;
    public static final String PROPERTY_DateTime = "DateTime";
    public static final String PROPERTY_SendMessage = "SendMessage";
    public static final String PROPERTY_SendMessageDateTime = "SendMessageDateTime";
     
     
    protected OADateTime dateTime;
    protected String sendMessage;
    protected OADateTime sendMessageDateTime;

    public static final String P_Status = "status";
    public static final String P_StatusString = "statusString";
    public static final String P_StatusEnum = "statusEnum";
    public static final String P_StatusDisplay = "statusDisplay";
    protected int status;
    public static final int STATUS_Starting = 0;
    public static final int STATUS_Running = 1;
    public static final int STATUS_Stopping = 2;
    public static final int STATUS_Stopped = 3;
    public static enum Status {
    	Starting("Starting"),
    	Running("Running"),
    	Stopping("Stopping"),
    	Stopped("Stopped");

        private String display;
        Status(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    @OAProperty(displayLength = 14, isProcessed = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getStatus() {
        return status;
    }
    public void setStatus(int newValue) {
        int old = status;
        fireBeforePropertyChange(P_Status, old, newValue);
        this.status = newValue;
        firePropertyChange(P_Status, old, this.status);
    }
    @OAProperty(enumPropertyName = P_Status)
    public String getStatusString() {
        Status status = getStatusEnum();
        if (status == null) return null;
        return status.name();
    }
    public void setStatusString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            Status status = Status.valueOf(val);
            if (status != null) x = status.ordinal();
        }
        if (x < 0) setNull(P_Status);
        else setStatus(x);
    }
    @OAProperty(enumPropertyName = P_Status)
    public Status getStatusEnum() {
        if (isNull(P_Status)) return null;
        final int val = getStatus();
        if (val < 0 || val >= Status.values().length) return null;
        return Status.values()[val];
    }
    public void setStatusEnum(Status val) {
        if (val == null) {
            setNull(P_Status);
        }
        else {
            setStatus(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_Status, displayName = "Status", displayLength = 14, columnLength = 6, properties = {P_Status} )
    public String getStatusDisplay() {
        Status status = getStatusEnum();
        if (status == null) return null;
        return status.getDisplay();
    }
    
     
    public ServerInfo() {
        if (!isLoading()) {
            setDateTime(new OADateTime());
        }
    }
     
    @OAProperty(displayName = "Date Time", defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
    @OAColumn(name = "DateTimeValue", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getDateTime() {
        return dateTime;
    }
    
    public void setDateTime(OADateTime newValue) {
        OADateTime old = dateTime;
        fireBeforePropertyChange(PROPERTY_DateTime, old, newValue);
        this.dateTime = newValue;
        firePropertyChange(PROPERTY_DateTime, old, this.dateTime);
    }
    
     
    @OAProperty(displayName = "Send Message", maxLength = 250, displayLength = 40, columnLength = 25, hasCustomCode = true)
    @OAColumn(sqlType = java.sql.Types.CLOB)
    public String getSendMessage() {
        return sendMessage;
    }
    
    public void setSendMessage(String newValue) {
        String old = sendMessage;
        fireBeforePropertyChange(PROPERTY_SendMessage, old, newValue);
        this.sendMessage = newValue;
        firePropertyChange(PROPERTY_SendMessage, old, this.sendMessage);
    }
    
     
    @OAProperty(displayName = "Send Message Date Time", displayLength = 15)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getSendMessageDateTime() {
        return sendMessageDateTime;
    }
    
    public void setSendMessageDateTime(OADateTime newValue) {
        OADateTime old = sendMessageDateTime;
        fireBeforePropertyChange(PROPERTY_SendMessageDateTime, old, newValue);
        this.sendMessageDateTime = newValue;
        firePropertyChange(PROPERTY_SendMessageDateTime, old, this.sendMessageDateTime);
    }
    
     
     
    // custom method
    // this will set the dateTime, which the clients will use to know to display the message.
    public void setMessageDateTime() {
        setSendMessageDateTime(new OADateTime());
    }
    
     
}
 
