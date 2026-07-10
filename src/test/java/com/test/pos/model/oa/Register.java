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
    lowerName = "register",
    pluralName = "Registers",
    shortName = "rgs",
    displayName = "Register",
    displayProperty = "code",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "RegisterStore", fkey = true, columns = { @OAIndexColumn(name = "StoreId") })
    }
)
public class Register extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Register.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Code = "code";
    public static final String P_Delete = "delete";
    public static final String P_DeleteReason = "deleteReason";
     
    public static final String P_RegisterSessions = "registerSessions";
    public static final String P_Store = "store";
    public static final String P_StoreId = "storeId"; // fkey
    public static final String P_Till = "till";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String code;
    protected volatile OADateTime delete;
    protected volatile String deleteReason;
     
    // Links to other objects.
    protected transient Hub<RegisterSession> hubRegisterSessions;
    protected volatile transient Store store;
    protected volatile transient Till till;
     
    public Register() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Register(int id) {
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

    @OAProperty(lowerName = "code", maxLength = 15, displayLength = 10)
    @OAColumn(name = "Code", maxLength = 15)
    public String getCode() {
        return code;
    }
    public void setCode(String newValue) {
        String old = code;
        fireBeforePropertyChange(P_Code, old, newValue);
        this.code = newValue;
        firePropertyChange(P_Code, old, this.code);
    }

    @OAProperty(lowerName = "delete", displayLength = 15, ignoreTimeZone = true)
    @OAColumn(name = "Delete", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getDelete() {
        return delete;
    }
    public void setDelete(OADateTime newValue) {
        OADateTime old = delete;
        fireBeforePropertyChange(P_Delete, old, newValue);
        this.delete = newValue;
        firePropertyChange(P_Delete, old, this.delete);
    }

    @OAProperty(lowerName = "deleteReason", displayName = "Delete Reason", maxLength = 120, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "DeleteReason", maxLength = 120)
    public String getDeleteReason() {
        return deleteReason;
    }
    public void setDeleteReason(String newValue) {
        String old = deleteReason;
        fireBeforePropertyChange(P_DeleteReason, old, newValue);
        this.deleteReason = newValue;
        firePropertyChange(P_DeleteReason, old, this.deleteReason);
    }

    @OAMany(
        displayName = "Register Sessions", 
        toClass = RegisterSession.class, 
        owner = true, 
        reverseName = RegisterSession.P_Register, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<RegisterSession> getRegisterSessions() {
        if (hubRegisterSessions == null) {
            hubRegisterSessions = (Hub<RegisterSession>) getHub(P_RegisterSessions);
        }
        return hubRegisterSessions;
    }
    @OAObjCallback(enabledProperty = Register.P_Till)
    public void registerSessionsCallback(OAObjectCallback cb) {
        if (cb == null) return;
        switch (cb.getType()) {
        }
    }

    @OAOne(
        reverseName = Store.P_Registers, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreId, toProperty = Store.P_Id)}
    )
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
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreId")
    public Integer getStoreId() {
        return (Integer) getFkeyProperty(P_StoreId);
    }
    public void setStoreId(Integer newValue) {
        this.store = null;
        setFkeyProperty(P_StoreId, newValue);
    }

    @OAOne(
        reverseName = Till.P_Register, 
        allowCreateNew = false, 
        selectFromPath = P_Store + "." + Store.P_Tills
    )
    public Till getTill() {
        if (till == null) {
            till = (Till) getObject(P_Till);
        }
        return till;
    }
    public void setTill(Till newValue) {
        Till old = this.till;
        fireBeforePropertyChange(P_Till, old, newValue);
        this.till = newValue;
        firePropertyChange(P_Till, old, this.till);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.code = rs.getString(3);
        timestamp = rs.getTimestamp(4);
        if (timestamp != null) this.delete = new OADateTime(timestamp);
        this.deleteReason = rs.getString(5);
        int storeFkey = rs.getInt(6);
        setFkeyProperty(P_Store, rs.wasNull() ? null : storeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
