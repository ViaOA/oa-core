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
    lowerName = "registerSession",
    pluralName = "RegisterSessions",
    shortName = "rgs",
    displayName = "Register Session",
    displayProperty = "teamMember",
    filterClasses = {RegisterSessionOpenFilter.class},
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "RegisterSessionRegister", fkey = true, columns = { @OAIndexColumn(name = "RegisterId") }), 
        @OAIndex(name = "RegisterSessionTeamMember", fkey = true, columns = { @OAIndexColumn(name = "TeamMemberId") })
    }
)
public class RegisterSession extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(RegisterSession.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Ended = "ended";
     
    public static final String P_Invoices = "invoices";
    public static final String P_Refunds = "refunds";
    public static final String P_Register = "register";
    public static final String P_RegisterId = "registerId"; // fkey
    public static final String P_TeamMember = "teamMember";
    public static final String P_TeamMemberId = "teamMemberId"; // fkey
    public static final String P_TillLedgerEntries = "tillLedgerEntries";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile OADateTime ended;
     
    // Links to other objects.
    protected transient Hub<Invoice> hubInvoices;
    protected transient Hub<Refund> hubRefunds;
    protected volatile transient Register register;
    protected volatile transient TeamMember teamMember;
    protected transient Hub<TillLedgerEntry> hubTillLedgerEntries;
     
    public RegisterSession() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public RegisterSession(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(enabledProperty = RegisterSession.P_Ended, enabledValue = false)
    public void callback(final OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
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

    @OAProperty(lowerName = "ended", displayLength = 15, ignoreTimeZone = true)
    @OAColumn(name = "Ended", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getEnded() {
        return ended;
    }
    public void setEnded(OADateTime newValue) {
        OADateTime old = ended;
        fireBeforePropertyChange(P_Ended, old, newValue);
        this.ended = newValue;
        firePropertyChange(P_Ended, old, this.ended);
    }

    @OAMany(
        toClass = Invoice.class, 
        reverseName = Invoice.P_RegisterSession
    )
    public Hub<Invoice> getInvoices() {
        if (hubInvoices == null) {
            hubInvoices = (Hub<Invoice>) getHub(P_Invoices);
        }
        return hubInvoices;
    }

    @OAMany(
        toClass = Refund.class, 
        reverseName = Refund.P_RegisterSession
    )
    public Hub<Refund> getRefunds() {
        if (hubRefunds == null) {
            hubRefunds = (Hub<Refund>) getHub(P_Refunds);
        }
        return hubRefunds;
    }

    @OAOne(
        reverseName = Register.P_RegisterSessions, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_RegisterId, toProperty = Register.P_Id)}
    )
    public Register getRegister() {
        if (register == null) {
            register = (Register) getObject(P_Register);
        }
        return register;
    }
    public void setRegister(Register newValue) {
        Register old = this.register;
        fireBeforePropertyChange(P_Register, old, newValue);
        this.register = newValue;
        firePropertyChange(P_Register, old, this.register);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "RegisterId")
    public Integer getRegisterId() {
        return (Integer) getFkeyProperty(P_RegisterId);
    }
    public void setRegisterId(Integer newValue) {
        this.register = null;
        setFkeyProperty(P_RegisterId, newValue);
    }

    @OAOne(
        displayName = "Team Member", 
        reverseName = TeamMember.P_RegisterSessions, 
        allowCreateNew = false, 
        selectFromPropertyPath = P_Register + "." + Register.P_Store + "." + Store.P_TeamMembers, 
        fkeys = {@OAFkey(fromProperty = P_TeamMemberId, toProperty = TeamMember.P_Id)}
    )
    public TeamMember getTeamMember() {
        if (teamMember == null) {
            teamMember = (TeamMember) getObject(P_TeamMember);
        }
        return teamMember;
    }
    public void setTeamMember(TeamMember newValue) {
        TeamMember old = this.teamMember;
        fireBeforePropertyChange(P_TeamMember, old, newValue);
        this.teamMember = newValue;
        firePropertyChange(P_TeamMember, old, this.teamMember);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "TeamMemberId")
    public Integer getTeamMemberId() {
        return (Integer) getFkeyProperty(P_TeamMemberId);
    }
    public void setTeamMemberId(Integer newValue) {
        this.teamMember = null;
        setFkeyProperty(P_TeamMemberId, newValue);
    }

    @OAMany(
        displayName = "Till Ledger Entries", 
        toClass = TillLedgerEntry.class, 
        reverseName = TillLedgerEntry.P_RegisterSession
    )
    public Hub<TillLedgerEntry> getTillLedgerEntries() {
        if (hubTillLedgerEntries == null) {
            hubTillLedgerEntries = (Hub<TillLedgerEntry>) getHub(P_TillLedgerEntries);
        }
        return hubTillLedgerEntries;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(3);
        if (timestamp != null) this.ended = new OADateTime(timestamp);
        int registerFkey = rs.getInt(4);
        setFkeyProperty(P_Register, rs.wasNull() ? null : registerFkey);
        int teamMemberFkey = rs.getInt(5);
        setFkeyProperty(P_TeamMember, rs.wasNull() ? null : teamMemberFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
