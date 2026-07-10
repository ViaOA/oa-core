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
    lowerName = "tillLedgerEntry",
    pluralName = "TillLedgerEntries",
    shortName = "tle",
    displayName = "Till Ledger Entry",
    displayProperty = "type",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "TillLedgerEntryRegisterSession", fkey = true, columns = { @OAIndexColumn(name = "RegisterSessionId") }), 
        @OAIndex(name = "TillLedgerEntryStoreSafeLedgerEntry", fkey = true, columns = { @OAIndexColumn(name = "StoreSafeLedgerEntryId") }), 
        @OAIndex(name = "TillLedgerEntryTeamMember", fkey = true, columns = { @OAIndexColumn(name = "TeamMemberId") }), 
        @OAIndex(name = "TillLedgerEntryTill", fkey = true, columns = { @OAIndexColumn(name = "TillId") })
    }
)
public class TillLedgerEntry extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(TillLedgerEntry.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Type = "type";
    public static final String P_TypeString = "typeString";
    public static final String P_TypeEnum = "typeEnum";
    public static final String P_TypeDisplay = "typeDisplay";
    public static final String P_LooseCashAmount = "looseCashAmount";
    public static final String P_CheckCount = "checkCount";
    public static final String P_CheckAmount = "checkAmount";
    public static final String P_Posted = "posted";
    public static final String P_Note = "note";
     
    public static final String P_TotalCashAmount = "totalCashAmount";
    public static final String P_CalcCheckCount = "calcCheckCount";
    public static final String P_CalcTotalCheckAmount = "calcTotalCheckAmount";
    public static final String P_TotalAmount = "totalAmount";
    public static final String P_CanPost = "canPost";
    public static final String P_CantPostReason = "cantPostReason";
    public static final String P_UsesCash = "usesCash";
    public static final String P_UsesChecks = "usesChecks";
    public static final String P_UsesLedgerDenominationBundle = "usesLedgerDenominationBundle";
    public static final String P_UsesInvoicePayment = "usesInvoicePayment";
    public static final String P_UsesInvoicePaymentChecks = "usesInvoicePaymentChecks";
     
    public static final String P_InvoicePayment = "invoicePayment";
    public static final String P_InvoicePaymentChecks = "invoicePaymentChecks";
    public static final String P_InvoicePaymentChecksId = "invoicePaymentChecksId"; // fkey
    public static final String P_LedgerDenominationBundles = "ledgerDenominationBundles";
    public static final String P_RefundPayment = "refundPayment";
    public static final String P_RegisterSession = "registerSession";
    public static final String P_RegisterSessionId = "registerSessionId"; // fkey
    public static final String P_StoreSafeLedgerEntry = "storeSafeLedgerEntry";
    public static final String P_StoreSafeLedgerEntryId = "storeSafeLedgerEntryId"; // fkey
    public static final String P_TeamMember = "teamMember";
    public static final String P_TeamMemberId = "teamMemberId"; // fkey
    public static final String P_Till = "till";
    public static final String P_TillId = "tillId"; // fkey
     
    public static final String M_Post = "post";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int type;

    public static enum Type {
        Unknown("Unknown"),
        CashPurchase("Cash Purchase"),
        CashRefund("Cash Refund"),
        CheckPurchase("Check Purchase"),
        CheckRefund("Check Refund"),
        CashFromSafe("Cash From Safe"),
        CashToSafe("Cash To Safe"),
        ChecksToSafe("Checks To Safe"),
        ChecksFromSafe("Checks From Safe"),
        MoveTillToSafe("Move Till To Safe"),
        MoveTillToRegister("Move Till To Register"),
        Audit("Audit"),
        Variance("Variance"),
        Validation("Validation"),
        ExchangeCash("Exchange Cash");

        private String display;
        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE_Unknown = 0;
    public static final int TYPE_CashPurchase = 1;
    public static final int TYPE_CashRefund = 2;
    public static final int TYPE_CheckPurchase = 3;
    public static final int TYPE_CheckRefund = 4;
    public static final int TYPE_CashFromSafe = 5;
    public static final int TYPE_CashToSafe = 6;
    public static final int TYPE_ChecksToSafe = 7;
    public static final int TYPE_ChecksFromSafe = 8;
    public static final int TYPE_MoveTillToSafe = 9;
    public static final int TYPE_MoveTillToRegister = 10;
    public static final int TYPE_Audit = 11;
    public static final int TYPE_Variance = 12;
    public static final int TYPE_Validation = 13;
    public static final int TYPE_ExchangeCash = 14;

    protected volatile double looseCashAmount;
    protected volatile int checkCount;
    protected volatile double checkAmount;
    protected volatile OADateTime posted;
    protected volatile String note;
     
    // Links to other objects.
    protected volatile transient InvoicePayment invoicePayment;
    protected transient Hub<InvoicePaymentCheck> hubInvoicePaymentChecks;
    protected transient Hub<LedgerDenominationBundle> hubLedgerDenominationBundles;
    protected volatile transient RefundPayment refundPayment;
    protected volatile transient RegisterSession registerSession;
    protected volatile transient StoreSafeLedgerEntry storeSafeLedgerEntry;
    protected volatile transient TeamMember teamMember;
    protected volatile transient Till till;
     
    public TillLedgerEntry() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public TillLedgerEntry(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(enabledProperty = TillLedgerEntry.P_Posted, enabledValue = false)
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

    @OAProperty(lowerName = "type", trackPrimitiveNull = false, displayLength = 20, uiColumnLength = 18, hasCustomCode = true, isNameValue = true)
    @OAColumn(name = "Type", sqlType = java.sql.Types.INTEGER)
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        int old = type;
        fireBeforePropertyChange(P_Type, old, newValue);
        this.type = newValue;
        firePropertyChange(P_Type, old, this.type);
        // custom
        TillLedgerEntryDelegate.afterSettingType(this);
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
        if (x < 0) x = 0;
        setType(x);
    }
    @OAProperty(enumPropertyName = P_Type)
    public Type getTypeEnum() {
        final int val = getType();
        if (val < 0 || val >= Type.values().length) return null;
        return Type.values()[val];
    }
    public void setTypeEnum(Type val) {
        if (val == null) {
            setType(0);
        }
        else {
            setType(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 20, columnLength = 18, properties = {P_Type} )
    public String getTypeDisplay() {
        Type type = getTypeEnum();
        if (type == null) return null;
        return type.getDisplay();
    }

    @OAProperty(lowerName = "looseCashAmount", displayName = "Loose Cash Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 17)
    @OAColumn(name = "LooseCashAmount", sqlType = java.sql.Types.NUMERIC)
    public double getLooseCashAmount() {
        return looseCashAmount;
    }
    public void setLooseCashAmount(double newValue) {
        double old = looseCashAmount;
        fireBeforePropertyChange(P_LooseCashAmount, old, newValue);
        this.looseCashAmount = newValue;
        firePropertyChange(P_LooseCashAmount, old, this.looseCashAmount);
    }
     
    @OAObjCallback(enabledProperty = TillLedgerEntry.P_UsesCash)
    public void looseCashAmountCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "checkCount", displayName = "Check Count", displayLength = 6, uiColumnLength = 11)
    @OAColumn(name = "CheckCount", sqlType = java.sql.Types.INTEGER)
    public int getCheckCount() {
        return checkCount;
    }
    public void setCheckCount(int newValue) {
        int old = checkCount;
        fireBeforePropertyChange(P_CheckCount, old, newValue);
        this.checkCount = newValue;
        firePropertyChange(P_CheckCount, old, this.checkCount);
    }
     
    @OAObjCallback(enabledProperty = TillLedgerEntry.P_UsesChecks)
    public void checkCountCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "checkAmount", displayName = "Check Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 12)
    @OAColumn(name = "CheckAmount", sqlType = java.sql.Types.NUMERIC)
    public double getCheckAmount() {
        return checkAmount;
    }
    public void setCheckAmount(double newValue) {
        double old = checkAmount;
        fireBeforePropertyChange(P_CheckAmount, old, newValue);
        this.checkAmount = newValue;
        firePropertyChange(P_CheckAmount, old, this.checkAmount);
    }
     
    @OAObjCallback(enabledProperty = TillLedgerEntry.P_UsesChecks)
    public void checkAmountCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "posted", isSubmit = true, displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "Posted", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getPosted() {
        return posted;
    }
    public void setPosted(OADateTime newValue) {
        OADateTime old = posted;
        fireBeforePropertyChange(P_Posted, old, newValue);
        this.posted = newValue;
        firePropertyChange(P_Posted, old, this.posted);
    }
     
    @OAObjCallback(enabledProperty = TillLedgerEntry.P_CanPost)
    public void postedCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "note", maxLength = 250, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "Note", maxLength = 250)
    public String getNote() {
        return note;
    }
    public void setNote(String newValue) {
        String old = note;
        fireBeforePropertyChange(P_Note, old, newValue);
        this.note = newValue;
        firePropertyChange(P_Note, old, this.note);
    }
    @OACalculatedProperty(displayName = "Total Cash Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 17, properties = {P_LooseCashAmount, P_LedgerDenominationBundles+"."+LedgerDenominationBundle.P_TotalAmount})
    public double getTotalCashAmount() {
        return TillLedgerEntryDelegate.getTotalCashAmount(this);
    }
    @OACalculatedProperty(displayName = "Calc Check Count", displayLength = 6, columnLength = 16, properties = {P_CheckCount, P_InvoicePaymentChecks})
    public int getCalcCheckCount() {
        return TillLedgerEntryDelegate.getCalcCheckCount(this);
    }
    @OACalculatedProperty(displayName = "Calc Total Check Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 23, properties = {P_CheckAmount, P_InvoicePaymentChecks})
    public double getCalcTotalCheckAmount() {
        return TillLedgerEntryDelegate.getCalcTotalCheckAmount(this);
    }
    @OACalculatedProperty(displayName = "Total Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 12, properties = {P_TotalCashAmount, P_InvoicePayment+"."+InvoicePayment.P_Amount, P_CheckAmount, P_InvoicePaymentChecks})
    public double getTotalAmount() {
        return TillLedgerEntryDelegate.getTotalAmount(this);
    }
    @OACalculatedProperty(displayName = "Can Post", displayLength = 5, columnLength = 8, properties = {P_Type, P_InvoicePayment, P_RegisterSession, P_Till, P_InvoicePayment+"."+InvoicePayment.P_InvoicePaymentCheck, P_StoreSafeLedgerEntry, P_TotalCashAmount, P_Till+"."+Till.P_CashAmount, P_Till+"."+Till.P_InvoicePaymentChecks, P_Till+"."+Till.P_TotalCheckAmount, P_CheckCount, P_CheckAmount, P_LooseCashAmount, P_TeamMember, P_InvoicePaymentChecks})
    public boolean getCanPost() {
        return TillLedgerEntryDelegate.getCanPost(this);
    }
    public boolean isCanPost() {
        return getCanPost();
    }
    public boolean canPost() {
        return getCanPost();
    }
    @OACalculatedProperty(displayName = "Cant Post Reason", displayLength = 20, columnLength = 22, properties = {P_CanPost})
    public String getCantPostReason() {
        return TillLedgerEntryDelegate.getCantPostReason(this);
    }
    @OACalculatedProperty(displayName = "Uses Cash", displayLength = 5, columnLength = 9, properties = {P_Type})
    public boolean getUsesCash() {
        return TillLedgerEntryDelegate.getUsesCash(this);
    }
    @OACalculatedProperty(displayName = "Uses Checks", displayLength = 5, columnLength = 11, properties = {P_Type})
    public boolean getUsesChecks() {
        return TillLedgerEntryDelegate.getUsesChecks(this);
    }
    @OACalculatedProperty(displayName = "Uses Ledger Denomination Bundle", displayLength = 5, columnLength = 31, properties = {P_Type})
    public boolean getUsesLedgerDenominationBundle() {
        return TillLedgerEntryDelegate.getUsesLedgerDenominationBundle(this);
    }
    public boolean isUsesLedgerDenominationBundle() {
        return getUsesLedgerDenominationBundle();
    }
    @OACalculatedProperty(displayName = "Uses Invoice Payment", displayLength = 5, columnLength = 20, properties = {P_Type})
    public boolean getUsesInvoicePayment() {
        return TillLedgerEntryDelegate.getUsesInvoicePayment(this);
    }
    @OACalculatedProperty(displayName = "Uses Invoice Payment Checks", displayLength = 5, columnLength = 27, properties = {P_Type})
    public boolean getUsesInvoicePaymentChecks() {
        return TillLedgerEntryDelegate.getUsesInvoicePaymentChecks(this);
    }

    @OAOne(
        displayName = "Invoice Payment", 
        reverseName = InvoicePayment.P_TillLedgerEntry, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    public InvoicePayment getInvoicePayment() {
        if (invoicePayment == null) {
            invoicePayment = (InvoicePayment) getObject(P_InvoicePayment);
        }
        return invoicePayment;
    }
    public void setInvoicePayment(InvoicePayment newValue) {
        InvoicePayment old = this.invoicePayment;
        fireBeforePropertyChange(P_InvoicePayment, old, newValue);
        this.invoicePayment = newValue;
        firePropertyChange(P_InvoicePayment, old, this.invoicePayment);
    }
    @OAObjCallback(enabledProperty = TillLedgerEntry.P_UsesInvoicePayment)
    public void invoicePaymentCallback(OAObjectCallback cb) {
        if (cb == null) return;
        switch (cb.getType()) {
        }
    }

    @OAMany(
        displayName = "Invoice Payment Checks", 
        toClass = InvoicePaymentCheck.class, 
        reverseName = InvoicePaymentCheck.P_TillLedgerEntries, 
        selectFromPath = P_Till + "." + Till.P_InvoicePaymentChecks
    )
    @OALinkTable(name = "TillLedgerEntryInvoicePaymentCheck", indexName = "InvoicePaymentCheckTillLedgerEntry", columns = {"TillLedgerEntryId"})
    public Hub<InvoicePaymentCheck> getInvoicePaymentChecks() {
        if (hubInvoicePaymentChecks == null) {
            hubInvoicePaymentChecks = (Hub<InvoicePaymentCheck>) getHub(P_InvoicePaymentChecks);
        }
        return hubInvoicePaymentChecks;
    }
    @OAObjCallback(enabledProperty = TillLedgerEntry.P_UsesInvoicePaymentChecks)
    public void invoicePaymentChecksCallback(OAObjectCallback cb) {
        if (cb == null) return;
        switch (cb.getType()) {
        }
    }

    @OAMany(
        displayName = "Ledger Denomination Bundles", 
        toClass = LedgerDenominationBundle.class, 
        reverseName = LedgerDenominationBundle.P_TillLedgerEntry
    )
    public Hub<LedgerDenominationBundle> getLedgerDenominationBundles() {
        if (hubLedgerDenominationBundles == null) {
            hubLedgerDenominationBundles = (Hub<LedgerDenominationBundle>) getHub(P_LedgerDenominationBundles);
        }
        return hubLedgerDenominationBundles;
    }
    @OAObjCallback(enabledProperty = TillLedgerEntry.P_UsesLedgerDenominationBundle)
    public void ledgerDenominationBundlesCallback(OAObjectCallback cb) {
        if (cb == null) return;
        switch (cb.getType()) {
        }
    }

    @OAOne(
        displayName = "Refund Payment", 
        reverseName = RefundPayment.P_TillLedgerEntry, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    public RefundPayment getRefundPayment() {
        if (refundPayment == null) {
            refundPayment = (RefundPayment) getObject(P_RefundPayment);
        }
        return refundPayment;
    }
    public void setRefundPayment(RefundPayment newValue) {
        RefundPayment old = this.refundPayment;
        fireBeforePropertyChange(P_RefundPayment, old, newValue);
        this.refundPayment = newValue;
        firePropertyChange(P_RefundPayment, old, this.refundPayment);
    }

    @OAOne(
        displayName = "Register Session", 
        reverseName = RegisterSession.P_TillLedgerEntries, 
        allowCreateNew = false, 
        selectFromPath = P_Till + "." + Till.P_Register + "." + Register.P_RegisterSessions, 
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

    @OAOne(
        displayName = "Store Safe Ledger Entry", 
        reverseName = StoreSafeLedgerEntry.P_TillLedgerEntry, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreSafeLedgerEntryId, toProperty = StoreSafeLedgerEntry.P_Id)}
    )
    public StoreSafeLedgerEntry getStoreSafeLedgerEntry() {
        if (storeSafeLedgerEntry == null) {
            storeSafeLedgerEntry = (StoreSafeLedgerEntry) getObject(P_StoreSafeLedgerEntry);
        }
        return storeSafeLedgerEntry;
    }
    public void setStoreSafeLedgerEntry(StoreSafeLedgerEntry newValue) {
        StoreSafeLedgerEntry old = this.storeSafeLedgerEntry;
        fireBeforePropertyChange(P_StoreSafeLedgerEntry, old, newValue);
        this.storeSafeLedgerEntry = newValue;
        firePropertyChange(P_StoreSafeLedgerEntry, old, this.storeSafeLedgerEntry);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreSafeLedgerEntryId")
    public Integer getStoreSafeLedgerEntryId() {
        return (Integer) getFkeyProperty(P_StoreSafeLedgerEntryId);
    }
    public void setStoreSafeLedgerEntryId(Integer newValue) {
        this.storeSafeLedgerEntry = null;
        setFkeyProperty(P_StoreSafeLedgerEntryId, newValue);
    }

    @OAOne(
        displayName = "Team Member", 
        reverseName = TeamMember.P_TillLedgerEntries, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
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

    @OAOne(
        reverseName = Till.P_TillLedgerEntries, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_TillId, toProperty = Till.P_Id)}
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
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "TillId")
    public Integer getTillId() {
        return (Integer) getFkeyProperty(P_TillId);
    }
    public void setTillId(Integer newValue) {
        this.till = null;
        setFkeyProperty(P_TillId, newValue);
    }
    @OAMethod(displayName = "Post")
    public void post() throws Exception {
        // use this to run on server
        if (isRemoteAvailable()) {
            remote();
            return;
        }
        // custom code
        TillLedgerEntryDelegate.post(this);
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.type = rs.getInt(3);
        this.looseCashAmount = rs.getDouble(4);
        setPrimitiveNull(P_LooseCashAmount, rs.wasNull());
        this.checkCount = rs.getInt(5);
        setPrimitiveNull(P_CheckCount, rs.wasNull());
        this.checkAmount = rs.getDouble(6);
        setPrimitiveNull(P_CheckAmount, rs.wasNull());
        timestamp = rs.getTimestamp(7);
        if (timestamp != null) this.posted = new OADateTime(timestamp);
        this.note = rs.getString(8);
        int registerSessionFkey = rs.getInt(9);
        setFkeyProperty(P_RegisterSession, rs.wasNull() ? null : registerSessionFkey);
        int storeSafeLedgerEntryFkey = rs.getInt(10);
        setFkeyProperty(P_StoreSafeLedgerEntry, rs.wasNull() ? null : storeSafeLedgerEntryFkey);
        int teamMemberFkey = rs.getInt(11);
        setFkeyProperty(P_TeamMember, rs.wasNull() ? null : teamMemberFkey);
        int tillFkey = rs.getInt(12);
        setFkeyProperty(P_Till, rs.wasNull() ? null : tillFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
