package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.converter.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.lang.*;
import com.viaoa.math.OAMath;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "till",
    pluralName = "Tills",
    shortName = "tll",
    displayName = "Till",
    displayProperty = "code",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "TillRegister", fkey = true, columns = { @OAIndexColumn(name = "RegisterId") }), 
        @OAIndex(name = "TillStore", fkey = true, columns = { @OAIndexColumn(name = "StoreId") })
    }
)
public class Till extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Till.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Code = "code";
    public static final String P_CashAmount = "cashAmount";
     
    public static final String P_TotalCheckAmount = "totalCheckAmount";
     
    public static final String P_InvoicePaymentChecks = "invoicePaymentChecks";
    public static final String P_Register = "register";
    public static final String P_RegisterId = "registerId"; // fkey
    public static final String P_Store = "store";
    public static final String P_StoreId = "storeId"; // fkey
    public static final String P_TillLedgerEntries = "tillLedgerEntries";
     
    public static final String M_MoveCashToSafe = "moveCashToSafe";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String code;
    protected volatile double cashAmount;
     
    // Links to other objects.
    protected transient Hub<InvoicePaymentCheck> hubInvoicePaymentChecks;
    protected volatile transient Register register;
    protected volatile transient Store store;
    protected transient Hub<TillLedgerEntry> hubTillLedgerEntries;
     
    public Till() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Till(int id) {
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

    @OAProperty(lowerName = "cashAmount", displayName = "Cash Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 11, isProcessed = true)
    @OAColumn(name = "CashAmount", sqlType = java.sql.Types.NUMERIC)
    public double getCashAmount() {
        return cashAmount;
    }
    public void setCashAmount(double newValue) {
        double old = cashAmount;
        fireBeforePropertyChange(P_CashAmount, old, newValue);
        this.cashAmount = newValue;
        firePropertyChange(P_CashAmount, old, this.cashAmount);
    }
    @OACalculatedProperty(displayName = "Total Check Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 18, properties = {P_InvoicePaymentChecks+"."+InvoicePaymentCheck.P_InvoicePayment+"."+InvoicePayment.P_Amount})
    public double getTotalCheckAmount() {
        double d = 0.0;
        for (InvoicePaymentCheck invoicePaymentCheck : this.getInvoicePaymentChecks()) {
            InvoicePayment ip = invoicePaymentCheck.getInvoicePayment();
            if (ip != null) d = OAMath.add(d, ip.getAmount(), 2);
        }
        return d;
    }

    @OAMany(
        displayName = "Invoice Payment Checks", 
        toClass = InvoicePaymentCheck.class, 
        reverseName = InvoicePaymentCheck.P_Till, 
        isProcessed = true
    )
    public Hub<InvoicePaymentCheck> getInvoicePaymentChecks() {
        if (hubInvoicePaymentChecks == null) {
            hubInvoicePaymentChecks = (Hub<InvoicePaymentCheck>) getHub(P_InvoicePaymentChecks);
        }
        return hubInvoicePaymentChecks;
    }

    @OAOne(
        reverseName = Register.P_Till, 
        allowCreateNew = false, 
        selectFromPropertyPath = P_Store + "." + Store.P_Registers, 
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
    
        // Custom
        TillDelegate.afterSetRegister(this, old, newValue);
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
        reverseName = Store.P_Tills, 
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

    @OAMany(
        displayName = "Till Ledger Entries", 
        toClass = TillLedgerEntry.class, 
        owner = true, 
        reverseName = TillLedgerEntry.P_Till, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<TillLedgerEntry> getTillLedgerEntries() {
        if (hubTillLedgerEntries == null) {
            hubTillLedgerEntries = (Hub<TillLedgerEntry>) getHub(P_TillLedgerEntries);
        }
        return hubTillLedgerEntries;
    }
    @OAMethod(displayName = "Move Cash To Safe")
    public void moveCashToSafe() throws Exception {
        // custom code
    //qqqqqqqqqqqqqqqqqqqq
    //    TillDelegate.moveCashToSafe(this);
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.code = rs.getString(3);
        this.cashAmount = rs.getDouble(4);
        setPrimitiveNull(P_CashAmount, rs.wasNull());
        int registerFkey = rs.getInt(5);
        setFkeyProperty(P_Register, rs.wasNull() ? null : registerFkey);
        int storeFkey = rs.getInt(6);
        setFkeyProperty(P_Store, rs.wasNull() ? null : storeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
