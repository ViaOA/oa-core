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
    lowerName = "bankDepositCheck",
    pluralName = "BankDepositChecks",
    shortName = "bdc",
    displayName = "Bank Deposit Check",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "BankDepositCheckBankDeposit", fkey = true, columns = { @OAIndexColumn(name = "BankDepositId") }), 
        @OAIndex(name = "BankDepositCheckInvoicePaymentCheck", fkey = true, columns = { @OAIndexColumn(name = "InvoicePaymentCheckId") })
    }
)
public class BankDepositCheck extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(BankDepositCheck.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Cleared = "cleared";
    public static final String P_Rejected = "rejected";
    public static final String P_RejectedFeeCollected = "rejectedFeeCollected";
    public static final String P_FeeAmountCollected = "feeAmountCollected";
     
    public static final String P_BankDeposit = "bankDeposit";
    public static final String P_BankDepositId = "bankDepositId"; // fkey
    public static final String P_InvoicePaymentCheck = "invoicePaymentCheck";
    public static final String P_InvoicePaymentCheckId = "invoicePaymentCheckId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile OADateTime cleared;
    protected volatile OADateTime rejected;
    protected volatile OADateTime rejectedFeeCollected;
    protected volatile double feeAmountCollected;
     
    // Links to other objects.
    protected volatile transient BankDeposit bankDeposit;
    protected volatile transient InvoicePayment invoicePaymentCheck;
     
    public BankDepositCheck() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public BankDepositCheck(int id) {
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

    @OAProperty(lowerName = "cleared", displayLength = 15, ignoreTimeZone = true)
    @OAColumn(name = "Cleared", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCleared() {
        return cleared;
    }
    public void setCleared(OADateTime newValue) {
        OADateTime old = cleared;
        fireBeforePropertyChange(P_Cleared, old, newValue);
        this.cleared = newValue;
        firePropertyChange(P_Cleared, old, this.cleared);
    }

    @OAProperty(lowerName = "rejected", displayLength = 15, ignoreTimeZone = true)
    @OAColumn(name = "Rejected", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getRejected() {
        return rejected;
    }
    public void setRejected(OADateTime newValue) {
        OADateTime old = rejected;
        fireBeforePropertyChange(P_Rejected, old, newValue);
        this.rejected = newValue;
        firePropertyChange(P_Rejected, old, this.rejected);
    }

    @OAProperty(lowerName = "rejectedFeeCollected", displayName = "Rejected Fee Collected", displayLength = 15, uiColumnLength = 22, ignoreTimeZone = true)
    @OAColumn(name = "RejectedFeeCollected", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getRejectedFeeCollected() {
        return rejectedFeeCollected;
    }
    public void setRejectedFeeCollected(OADateTime newValue) {
        OADateTime old = rejectedFeeCollected;
        fireBeforePropertyChange(P_RejectedFeeCollected, old, newValue);
        this.rejectedFeeCollected = newValue;
        firePropertyChange(P_RejectedFeeCollected, old, this.rejectedFeeCollected);
    }

    @OAProperty(lowerName = "feeAmountCollected", displayName = "Fee Amount Collected", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 20)
    @OAColumn(name = "FeeAmountCollected", sqlType = java.sql.Types.NUMERIC)
    public double getFeeAmountCollected() {
        return feeAmountCollected;
    }
    public void setFeeAmountCollected(double newValue) {
        double old = feeAmountCollected;
        fireBeforePropertyChange(P_FeeAmountCollected, old, newValue);
        this.feeAmountCollected = newValue;
        firePropertyChange(P_FeeAmountCollected, old, this.feeAmountCollected);
    }

    @OAOne(
        displayName = "Bank Deposit", 
        reverseName = BankDeposit.P_BankDepositChecks, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_BankDepositId, toProperty = BankDeposit.P_Id)}
    )
    public BankDeposit getBankDeposit() {
        if (bankDeposit == null) {
            bankDeposit = (BankDeposit) getObject(P_BankDeposit);
        }
        return bankDeposit;
    }
    public void setBankDeposit(BankDeposit newValue) {
        BankDeposit old = this.bankDeposit;
        fireBeforePropertyChange(P_BankDeposit, old, newValue);
        this.bankDeposit = newValue;
        firePropertyChange(P_BankDeposit, old, this.bankDeposit);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "BankDepositId")
    public Integer getBankDepositId() {
        return (Integer) getFkeyProperty(P_BankDepositId);
    }
    public void setBankDepositId(Integer newValue) {
        this.bankDeposit = null;
        setFkeyProperty(P_BankDepositId, newValue);
    }

    @OAOne(
        displayName = "Invoice Payment Check", 
        reverseName = InvoicePayment.P_BankDepositCheck, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_InvoicePaymentCheckId, toProperty = InvoicePayment.P_Id)}
    )
    public InvoicePayment getInvoicePaymentCheck() {
        if (invoicePaymentCheck == null) {
            invoicePaymentCheck = (InvoicePayment) getObject(P_InvoicePaymentCheck);
        }
        return invoicePaymentCheck;
    }
    public void setInvoicePaymentCheck(InvoicePayment newValue) {
        InvoicePayment old = this.invoicePaymentCheck;
        fireBeforePropertyChange(P_InvoicePaymentCheck, old, newValue);
        this.invoicePaymentCheck = newValue;
        firePropertyChange(P_InvoicePaymentCheck, old, this.invoicePaymentCheck);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "InvoicePaymentCheckId")
    public Integer getInvoicePaymentCheckId() {
        return (Integer) getFkeyProperty(P_InvoicePaymentCheckId);
    }
    public void setInvoicePaymentCheckId(Integer newValue) {
        this.invoicePaymentCheck = null;
        setFkeyProperty(P_InvoicePaymentCheckId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(3);
        if (timestamp != null) this.cleared = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(4);
        if (timestamp != null) this.rejected = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(5);
        if (timestamp != null) this.rejectedFeeCollected = new OADateTime(timestamp);
        this.feeAmountCollected = rs.getDouble(6);
        setPrimitiveNull(P_FeeAmountCollected, rs.wasNull());
        int bankDepositFkey = rs.getInt(7);
        setFkeyProperty(P_BankDeposit, rs.wasNull() ? null : bankDepositFkey);
        int invoicePaymentCheckFkey = rs.getInt(8);
        setFkeyProperty(P_InvoicePaymentCheck, rs.wasNull() ? null : invoicePaymentCheckFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
