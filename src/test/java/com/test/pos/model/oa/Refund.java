package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.lang.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "refund",
    pluralName = "Refunds",
    shortName = "rfn",
    displayName = "Refund",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "RefundRegisterSession", fkey = true, columns = { @OAIndexColumn(name = "RegisterSessionId") })
    }
)
public class Refund extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Refund.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_RefundInvoices = "refundInvoices";
    public static final String P_RegisterSession = "registerSession";
    public static final String P_RegisterSessionId = "registerSessionId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected transient Hub<RefundInvoice> hubRefundInvoices;
    protected volatile transient RegisterSession registerSession;
     
    public Refund() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Refund(int id) {
        this();
        setId(id);
    }

    @OAProperty(lowerName = "id", isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId
    @OAColumn(name = "Id", sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }

    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "Created", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }

    @OAMany(
        displayName = "Refund Invoices", 
        toClass = RefundInvoice.class, 
        owner = true, 
        reverseName = RefundInvoice.P_Refund, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<RefundInvoice> getRefundInvoices() {
        if (hubRefundInvoices == null) {
            hubRefundInvoices = (Hub<RefundInvoice>) getHub(P_RefundInvoices);
        }
        return hubRefundInvoices;
    }

    @OAOne(
        displayName = "Register Session", 
        reverseName = RegisterSession.P_Refunds, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_RegisterSessionId, toProperty = RegisterSession.P_Id)}
    )
    public RegisterSession getRegisterSession() {
        if (registerSession == null) {
            registerSession = (RegisterSession) getObject(P_RegisterSession);
        }
        return registerSession;
    }
    public void setRegisterSession(RegisterSession newValue) {
        RegisterSession old = this.registerSession;
        fireBeforePropertyChange(P_RegisterSession, old, newValue);
        this.registerSession = newValue;
        firePropertyChange(P_RegisterSession, old, this.registerSession);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "RegisterSessionId")
    public Integer getRegisterSessionId() {
        return (Integer) getFkeyProperty(P_RegisterSessionId);
    }
    public void setRegisterSessionId(Integer newValue) {
        this.registerSession = null;
        setFkeyProperty(P_RegisterSessionId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int registerSessionFkey = rs.getInt(3);
        setFkeyProperty(P_RegisterSession, rs.wasNull() ? null : registerSessionFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
