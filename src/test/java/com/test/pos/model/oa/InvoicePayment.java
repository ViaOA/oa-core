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
    lowerName = "invoicePayment",
    pluralName = "InvoicePayments",
    shortName = "inp",
    displayName = "Invoice Payment",
    displayProperty = "type",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "InvoicePaymentInvoice", fkey = true, columns = { @OAIndexColumn(name = "InvoiceId") }), 
        @OAIndex(name = "InvoicePaymentTillLedgerEntry", fkey = true, columns = { @OAIndexColumn(name = "TillLedgerEntryId") })
    }
)
public class InvoicePayment extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(InvoicePayment.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Type = "type";
    public static final String P_TypeString = "typeString";
    public static final String P_TypeEnum = "typeEnum";
    public static final String P_TypeDisplay = "typeDisplay";
    public static final String P_InputCode = "inputCode";
    public static final String P_OutputCode = "outputCode";
    public static final String P_Amount = "amount";
    public static final String P_CashIn = "cashIn";
    public static final String P_CashOut = "cashOut";
    public static final String P_Applied = "applied";
     
    public static final String P_TypeIsCash = "typeIsCash";
    public static final String P_TypeIsCheck = "typeIsCheck";
     
    public static final String P_BankDepositCheck = "bankDepositCheck";
    public static final String P_Invoice = "invoice";
    public static final String P_InvoiceId = "invoiceId"; // fkey
    public static final String P_InvoicePaymentCheck = "invoicePaymentCheck";
    public static final String P_RefundPayments = "refundPayments";
    public static final String P_TillLedgerEntry = "tillLedgerEntry";
    public static final String P_TillLedgerEntryId = "tillLedgerEntryId"; // fkey
     
    public static final String M_Apply = "apply";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int type;

    public static enum Type {
        unknown("Unknown"),
        cash("Cash"),
        check("Check"),
        giftCard("Gift Card"),
        creditCard("Credit Card"),
        debitCard("Debit Card");

        private String display;
        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE_unknown = 0;
    public static final int TYPE_cash = 1;
    public static final int TYPE_check = 2;
    public static final int TYPE_giftCard = 3;
    public static final int TYPE_creditCard = 4;
    public static final int TYPE_debitCard = 5;

    protected volatile String inputCode;
    protected volatile String outputCode;
    protected volatile double amount;
    protected volatile double cashIn;
    protected volatile double cashOut;
    protected volatile OADateTime applied;
     
    // Links to other objects.
    protected volatile transient BankDepositCheck bankDepositCheck;
    protected volatile transient Invoice invoice;
    protected volatile transient InvoicePaymentCheck invoicePaymentCheck;
    protected transient Hub<RefundPayment> hubRefundPayments;
    protected volatile transient TillLedgerEntry tillLedgerEntry;
     
    public InvoicePayment() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public InvoicePayment(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(enabledProperty = InvoicePayment.P_Applied, enabledValue = false)
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

    @OAProperty(lowerName = "type", displayLength = 10, uiColumnLength = 11, isNameValue = true)
    @OAColumn(name = "Type", sqlType = java.sql.Types.INTEGER)
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        int old = type;
        fireBeforePropertyChange(P_Type, old, newValue);
        this.type = newValue;
        firePropertyChange(P_Type, old, this.type);
    }

    @OAProperty(enumPropertyName = P_Type)
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
    @OAProperty(enumPropertyName = P_Type)
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
    @OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 10, columnLength = 11, properties = {P_Type} )
    public String getTypeDisplay() {
        Type type = getTypeEnum();
        if (type == null) return null;
        return type.getDisplay();
    }

    @OAProperty(lowerName = "inputCode", displayName = "Input Code", maxLength = 75, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "InputCode", maxLength = 75)
    public String getInputCode() {
        return inputCode;
    }
    public void setInputCode(String newValue) {
        String old = inputCode;
        fireBeforePropertyChange(P_InputCode, old, newValue);
        this.inputCode = newValue;
        firePropertyChange(P_InputCode, old, this.inputCode);
    }

    @OAProperty(lowerName = "outputCode", displayName = "Output Code", maxLength = 75, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "OutputCode", maxLength = 75)
    public String getOutputCode() {
        return outputCode;
    }
    public void setOutputCode(String newValue) {
        String old = outputCode;
        fireBeforePropertyChange(P_OutputCode, old, newValue);
        this.outputCode = newValue;
        firePropertyChange(P_OutputCode, old, this.outputCode);
    }

    @OAProperty(lowerName = "amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 8)
    @OAColumn(name = "Amount", sqlType = java.sql.Types.NUMERIC)
    public double getAmount() {
        return amount;
    }
    public void setAmount(double newValue) {
        double old = amount;
        fireBeforePropertyChange(P_Amount, old, newValue);
        this.amount = newValue;
        firePropertyChange(P_Amount, old, this.amount);
    }

    @OAProperty(lowerName = "cashIn", displayName = "Cash In", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 8)
    @OAColumn(name = "CashIn", sqlType = java.sql.Types.NUMERIC)
    public double getCashIn() {
        return cashIn;
    }
    public void setCashIn(double newValue) {
        double old = cashIn;
        fireBeforePropertyChange(P_CashIn, old, newValue);
        this.cashIn = newValue;
        firePropertyChange(P_CashIn, old, this.cashIn);
    }
     
    @OAObjCallback(visibleProperty = InvoicePayment.P_CashIn)
    public void cashInCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "cashOut", displayName = "Cash Out", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 8)
    @OAColumn(name = "CashOut", sqlType = java.sql.Types.NUMERIC)
    public double getCashOut() {
        return cashOut;
    }
    public void setCashOut(double newValue) {
        double old = cashOut;
        fireBeforePropertyChange(P_CashOut, old, newValue);
        this.cashOut = newValue;
        firePropertyChange(P_CashOut, old, this.cashOut);
    }
     
    @OAObjCallback(visibleProperty = InvoicePayment.P_TypeIsCash)
    public void cashOutCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "applied", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "Applied", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getApplied() {
        return applied;
    }
    public void setApplied(OADateTime newValue) {
        OADateTime old = applied;
        fireBeforePropertyChange(P_Applied, old, newValue);
        this.applied = newValue;
        firePropertyChange(P_Applied, old, this.applied);
    }
    @OACalculatedProperty(displayName = "Type Is Cash", displayLength = 5, columnLength = 12, properties = {P_Type})
    public boolean getTypeIsCash() {
        return InvoicePaymentDelegate.getTypeIsCash(this);
    }
    public boolean isTypeIsCash() {
        return getTypeIsCash();
    }
    @OACalculatedProperty(displayName = "Type Is Check", displayLength = 5, columnLength = 13, properties = {P_Type})
    public boolean getTypeIsCheck() {
        return InvoicePaymentDelegate.getTypeIsCheck(this);
    }
    public boolean isTypeIsCheck() {
        return getTypeIsCheck();
    }

    @OAOne(
        displayName = "Bank Deposit Check", 
        reverseName = BankDepositCheck.P_InvoicePaymentCheck
    )
    public BankDepositCheck getBankDepositCheck() {
        if (bankDepositCheck == null) {
            bankDepositCheck = (BankDepositCheck) getObject(P_BankDepositCheck);
        }
        return bankDepositCheck;
    }
    public void setBankDepositCheck(BankDepositCheck newValue) {
        BankDepositCheck old = this.bankDepositCheck;
        fireBeforePropertyChange(P_BankDepositCheck, old, newValue);
        this.bankDepositCheck = newValue;
        firePropertyChange(P_BankDepositCheck, old, this.bankDepositCheck);
    }

    @OAOne(
        reverseName = Invoice.P_InvoicePayments, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_InvoiceId, toProperty = Invoice.P_Id)}
    )
    public Invoice getInvoice() {
        if (invoice == null) {
            invoice = (Invoice) getObject(P_Invoice);
        }
        return invoice;
    }
    public void setInvoice(Invoice newValue) {
        Invoice old = this.invoice;
        fireBeforePropertyChange(P_Invoice, old, newValue);
        this.invoice = newValue;
        firePropertyChange(P_Invoice, old, this.invoice);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "InvoiceId")
    public Integer getInvoiceId() {
        return (Integer) getFkeyProperty(P_InvoiceId);
    }
    public void setInvoiceId(Integer newValue) {
        this.invoice = null;
        setFkeyProperty(P_InvoiceId, newValue);
    }

    @OAOne(
        displayName = "Invoice Payment Check", 
        reverseName = InvoicePaymentCheck.P_InvoicePayment
    )
    public InvoicePaymentCheck getInvoicePaymentCheck() {
        if (invoicePaymentCheck == null) {
            invoicePaymentCheck = (InvoicePaymentCheck) getObject(P_InvoicePaymentCheck);
        }
        return invoicePaymentCheck;
    }
    public void setInvoicePaymentCheck(InvoicePaymentCheck newValue) {
        InvoicePaymentCheck old = this.invoicePaymentCheck;
        fireBeforePropertyChange(P_InvoicePaymentCheck, old, newValue);
        this.invoicePaymentCheck = newValue;
        firePropertyChange(P_InvoicePaymentCheck, old, this.invoicePaymentCheck);
    }

    @OAMany(
        displayName = "Refund Payments", 
        toClass = RefundPayment.class, 
        reverseName = RefundPayment.P_InvoicePayment
    )
    public Hub<RefundPayment> getRefundPayments() {
        if (hubRefundPayments == null) {
            hubRefundPayments = (Hub<RefundPayment>) getHub(P_RefundPayments);
        }
        return hubRefundPayments;
    }

    @OAOne(
        displayName = "Till Ledger Entry", 
        reverseName = TillLedgerEntry.P_InvoicePayment, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        isOneAndOnlyOne = true, 
        fkeys = {@OAFkey(fromProperty = P_TillLedgerEntryId, toProperty = TillLedgerEntry.P_Id)}
    )
    public TillLedgerEntry getTillLedgerEntry() {
        if (tillLedgerEntry == null) {
            tillLedgerEntry = (TillLedgerEntry) getObject(P_TillLedgerEntry);
        }
        return tillLedgerEntry;
    }
    public void setTillLedgerEntry(TillLedgerEntry newValue) {
        TillLedgerEntry old = this.tillLedgerEntry;
        fireBeforePropertyChange(P_TillLedgerEntry, old, newValue);
        this.tillLedgerEntry = newValue;
        firePropertyChange(P_TillLedgerEntry, old, this.tillLedgerEntry);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "TillLedgerEntryId")
    public Integer getTillLedgerEntryId() {
        return (Integer) getFkeyProperty(P_TillLedgerEntryId);
    }
    public void setTillLedgerEntryId(Integer newValue) {
        this.tillLedgerEntry = null;
        setFkeyProperty(P_TillLedgerEntryId, newValue);
    }
    @OAMethod(displayName = "Apply")
    public void apply() throws Exception {
        // custom code
        InvoicePaymentDelegate.apply(this);
    }
    @OAObjCallback(enabledProperty = InvoicePayment.P_Applied, enabledValue = false)
    public void applyCallback(OAObjectCallback cb) {
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.type = rs.getInt(3);
        setPrimitiveNull(P_Type, rs.wasNull());
        this.inputCode = rs.getString(4);
        this.outputCode = rs.getString(5);
        this.amount = rs.getDouble(6);
        setPrimitiveNull(P_Amount, rs.wasNull());
        this.cashIn = rs.getDouble(7);
        setPrimitiveNull(P_CashIn, rs.wasNull());
        this.cashOut = rs.getDouble(8);
        setPrimitiveNull(P_CashOut, rs.wasNull());
        timestamp = rs.getTimestamp(9);
        if (timestamp != null) this.applied = new OADateTime(timestamp);
        int invoiceFkey = rs.getInt(10);
        setFkeyProperty(P_Invoice, rs.wasNull() ? null : invoiceFkey);
        int tillLedgerEntryFkey = rs.getInt(11);
        setFkeyProperty(P_TillLedgerEntry, rs.wasNull() ? null : tillLedgerEntryFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
