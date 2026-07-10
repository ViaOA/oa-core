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
import com.test.pos.model.oa.method.*;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "storeSafeLedgerEntry",
    pluralName = "StoreSafeLedgerEntries",
    shortName = "ssl",
    displayName = "Store Safe Ledger Entry",
    displayProperty = "created",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StoreSafeLedgerEntryStoreDayOpen", fkey = true, columns = { @OAIndexColumn(name = "StoreDayOpenId") }), 
        @OAIndex(name = "StoreSafeLedgerEntryStoreSafe", fkey = true, columns = { @OAIndexColumn(name = "StoreSafeId") }), 
        @OAIndex(name = "StoreSafeLedgerEntryTeamMember", fkey = true, columns = { @OAIndexColumn(name = "TeamMemberId") })
    }
)
public class StoreSafeLedgerEntry extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StoreSafeLedgerEntry.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Type = "type";
    public static final String P_TypeString = "typeString";
    public static final String P_TypeEnum = "typeEnum";
    public static final String P_TypeDisplay = "typeDisplay";
    public static final String P_LooseCashAmount = "looseCashAmount";
    public static final String P_CheckCount = "checkCount";
    public static final String P_CheckAmount = "checkAmount";
    public static final String P_PettyCashAmount = "pettyCashAmount";
    public static final String P_Note = "note";
    public static final String P_Posted = "posted";
     
    public static final String P_TotalCashAmount = "totalCashAmount";
    public static final String P_CalcCheckCount = "calcCheckCount";
    public static final String P_TotalCheckAmount = "totalCheckAmount";
    public static final String P_TotalAmount = "totalAmount";
    public static final String P_CanPost = "canPost";
    public static final String P_CantPostReason = "cantPostReason";
    public static final String P_UsesCash = "usesCash";
    public static final String P_UsesChecks = "usesChecks";
    public static final String P_UsesPettyCash = "usesPettyCash";
    public static final String P_UsesLedgerDenominationBundle = "usesLedgerDenominationBundle";
    public static final String P_NeedsToCreateTillLedgerEntry = "needsToCreateTillLedgerEntry";
    public static final String P_UsesInvoicePaymentChecks = "usesInvoicePaymentChecks";
     
    public static final String P_CalcStore = "calcStore";
    public static final String P_InvoicePaymentChecks = "invoicePaymentChecks";
    public static final String P_InvoicePaymentChecksId = "invoicePaymentChecksId"; // fkey
    public static final String P_LedgerDenominationBundles = "ledgerDenominationBundles";
    public static final String P_ManualPurchaseOrder = "manualPurchaseOrder";
    public static final String P_StoreDayOpen = "storeDayOpen";
    public static final String P_StoreDayOpenId = "storeDayOpenId"; // fkey
    public static final String P_StoreSafe = "storeSafe";
    public static final String P_StoreSafeId = "storeSafeId"; // fkey
    public static final String P_TeamMember = "teamMember";
    public static final String P_TeamMemberId = "teamMemberId"; // fkey
    public static final String P_TillLedgerEntry = "tillLedgerEntry";
     
    public static final String M_Post = "post";
    public static final String M_CreateTillLedgerEntry = "createTillLedgerEntry";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int type;

    public static enum Type {
        Unknown("Unknown"),
        TillCashToSafe("Till Cash To Safe"),
        SafeCashToTill("Safe Cash To Till"),
        TillChecksToSafe("Till Checks To Safe"),
        SafeChecksToTill("Safe Checks To Till"),
        PettyCashToSafe("Petty Cash To Safe"),
        SafeCashToPettyCash("Safe Cash To Petty Cash"),
        PettyCashUsed("Petty Cash Used"),
        ExchangeCash("Exchange Cash"),
        ReturnedCheckFee("Returned Check Fee"),
        BankToSafe("Bank To Safe"),
        SafeToBank("Safe To Bank"),
        Audit("Audit"),
        Variance("Variance"),
        Validation("Validation");

        private String display;
        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE_Unknown = 0;
    public static final int TYPE_TillCashToSafe = 1;
    public static final int TYPE_SafeCashToTill = 2;
    public static final int TYPE_TillChecksToSafe = 3;
    public static final int TYPE_SafeChecksToTill = 4;
    public static final int TYPE_PettyCashToSafe = 5;
    public static final int TYPE_SafeCashToPettyCash = 6;
    public static final int TYPE_PettyCashUsed = 7;
    public static final int TYPE_ExchangeCash = 8;
    public static final int TYPE_ReturnedCheckFee = 9;
    public static final int TYPE_BankToSafe = 10;
    public static final int TYPE_SafeToBank = 11;
    public static final int TYPE_Audit = 12;
    public static final int TYPE_Variance = 13;
    public static final int TYPE_Validation = 14;

    protected volatile double looseCashAmount;
    protected volatile int checkCount;
    protected volatile double checkAmount;
    protected volatile double pettyCashAmount;
    protected volatile String note;
    protected volatile OADateTime posted;
     
    // Links to other objects.
    protected transient Hub<InvoicePaymentCheck> hubInvoicePaymentChecks;
    protected transient Hub<LedgerDenominationBundle> hubLedgerDenominationBundles;
    protected volatile transient ManualPurchaseOrder manualPurchaseOrder;
    protected volatile transient StoreDayOpen storeDayOpen;
    protected volatile transient StoreSafe storeSafe;
    protected volatile transient TeamMember teamMember;
    protected volatile transient TillLedgerEntry tillLedgerEntry;
     
    public StoreSafeLedgerEntry() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StoreSafeLedgerEntry(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(enabledProperty = StoreSafeLedgerEntry.P_Posted, enabledValue = false
    )
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

    @OAProperty(lowerName = "type", trackPrimitiveNull = false, displayLength = 22, uiColumnLength = 19, hasCustomCode = true, isNameValue = true)
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
        StoreSafeLedgerEntryDelegate.afterSettingType(this);
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
    @OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 22, columnLength = 19, properties = {P_Type} )
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
     
    @OAObjCallback(enabledProperty = StoreSafeLedgerEntry.P_UsesCash)
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
     
    @OAObjCallback(enabledProperty = StoreSafeLedgerEntry.P_UsesChecks)
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
     
    @OAObjCallback(enabledProperty = StoreSafeLedgerEntry.P_UsesChecks)
    public void checkAmountCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "pettyCashAmount", displayName = "Petty Cash Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 17)
    @OAColumn(name = "PettyCashAmount", sqlType = java.sql.Types.NUMERIC)
    public double getPettyCashAmount() {
        return pettyCashAmount;
    }
    public void setPettyCashAmount(double newValue) {
        double old = pettyCashAmount;
        fireBeforePropertyChange(P_PettyCashAmount, old, newValue);
        this.pettyCashAmount = newValue;
        firePropertyChange(P_PettyCashAmount, old, this.pettyCashAmount);
    }
     
    @OAObjCallback(enabledProperty = StoreSafeLedgerEntry.P_UsesPettyCash)
    public void pettyCashAmountCallback(OAObjectCallback callback) {
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

    @OAProperty(lowerName = "posted", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
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
    @OACalculatedProperty(displayName = "Total Cash Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 17, properties = {P_LooseCashAmount, P_LedgerDenominationBundles+"."+LedgerDenominationBundle.P_TotalAmount})
    public double getTotalCashAmount() {
        return StoreSafeLedgerEntryDelegate.getTotalCashAmount(this);
    }
    @OACalculatedProperty(displayName = "Calc Check Count", displayLength = 6, columnLength = 16, properties = {P_InvoicePaymentChecks, P_TillLedgerEntry+"."+TillLedgerEntry.P_InvoicePaymentChecks, P_CheckCount})
    public int getCalcCheckCount() {
        return StoreSafeLedgerEntryDelegate.getCalcCheckCount(this);
    }
    @OACalculatedProperty(displayName = "Total Check Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 18, properties = {P_InvoicePaymentChecks+"."+InvoicePaymentCheck.P_InvoicePayment+"."+InvoicePayment.P_Amount, P_TillLedgerEntry+"."+TillLedgerEntry.P_InvoicePaymentChecks+"."+InvoicePaymentCheck.P_InvoicePayment+"."+InvoicePayment.P_Amount, P_CheckAmount})
    public double getTotalCheckAmount() {
        return StoreSafeLedgerEntryDelegate.getTotalCheckAmount(this);
    }
    @OACalculatedProperty(displayName = "Total Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 12, properties = {P_TotalCashAmount, P_TotalCheckAmount})
    public double getTotalAmount() {
        return StoreSafeLedgerEntryDelegate.getTotalAmount(this);
    }
    @OACalculatedProperty(displayName = "Can Post", displayLength = 5, columnLength = 8, properties = {P_Type, P_TotalCashAmount, P_TotalCheckAmount, P_PettyCashAmount})
    public boolean getCanPost() {
        return StoreSafeLedgerEntryDelegate.getCanPost(this);
    }
    public boolean isCanPost() {
        return getCanPost();
    }
    public boolean canPost() {
        return getCanPost();
    }
    @OACalculatedProperty(displayName = "Cant Post Reason", displayLength = 20, properties = {P_CanPost})
    public String getCantPostReason() {
        return StoreSafeLedgerEntryDelegate.getCantPostReason(this); 
    }
    @OACalculatedProperty(displayName = "Uses Cash", displayLength = 5, columnLength = 9, properties = {P_Type})
    public boolean getUsesCash() {
        return StoreSafeLedgerEntryDelegate.getUsesCash(this);
    }
    public boolean isUsesCash() {
        return getUsesCash();
    }
    @OACalculatedProperty(displayName = "Uses Checks", displayLength = 5, columnLength = 11, properties = {P_Type})
    public boolean getUsesChecks() {
        return StoreSafeLedgerEntryDelegate.getUsesChecks(this);
    }
    public boolean isUsesChecks() {
        return getUsesChecks();
    }
    @OACalculatedProperty(displayName = "Uses Petty Cash", displayLength = 5, columnLength = 15, properties = {P_Type})
    public boolean getUsesPettyCash() {
        return StoreSafeLedgerEntryDelegate.getUsesPettyCash(this);
    }
    public boolean isUsesPettyCash() {
        return getUsesPettyCash();
    }
    @OACalculatedProperty(displayName = "Uses Ledger Denomination Bundle", displayLength = 5, columnLength = 31, properties = {P_Type})
    public boolean getUsesLedgerDenominationBundle() {
        return StoreSafeLedgerEntryDelegate.getUsesLedgerDenominationBundle(this);
    }
    public boolean isUsesLedgerDenominationBundle() {
        return getUsesLedgerDenominationBundle();
    }
    @OACalculatedProperty(displayName = "Needs To Create Till Ledger Entry", displayLength = 5, columnLength = 33, properties = {P_Type, P_TillLedgerEntry})
    public boolean getNeedsToCreateTillLedgerEntry() {
        return StoreSafeLedgerEntryDelegate.getNeedsToCreateTillLedgerEntry(this);
    }
    @OACalculatedProperty(displayName = "Uses Invoice Payment Checks", displayLength = 5, columnLength = 27, properties = {P_Type})
    public boolean getUsesInvoicePaymentChecks() {
        return StoreSafeLedgerEntryDelegate.getUsesInvoicePaymentChecks(this);
    }

    @OAOne(
        displayName = "Store", 
        isCalculated = true, 
        reverseName = Store.P_CalcStoreSafeLedgerEntries, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    public Store getCalcStore() {
        // Custom code
        return StoreSafeLedgerEntryDelegate.getCalcStore(this);
    }

    @OAMany(
        displayName = "Invoice Payment Checks", 
        toClass = InvoicePaymentCheck.class, 
        reverseName = InvoicePaymentCheck.P_StoreSafeLedgerEntries, 
        selectFromPath = P_StoreSafe + "." + StoreSafe.P_InvoicePaymentChecks
    )
    @OALinkTable(name = "StoreSafeLedgerEntryInvoicePaymentCheck", indexName = "InvoicePaymentCheckStoreSafeLedgerEntry", columns = {"StoreSafeLedgerEntryId"})
    public Hub<InvoicePaymentCheck> getInvoicePaymentChecks() {
        if (hubInvoicePaymentChecks == null) {
            hubInvoicePaymentChecks = (Hub<InvoicePaymentCheck>) getHub(P_InvoicePaymentChecks);
        }
        return hubInvoicePaymentChecks;
    }
    @OAObjCallback(enabledProperty = StoreSafeLedgerEntry.P_UsesInvoicePaymentChecks)
    public void invoicePaymentChecksCallback(OAObjectCallback cb) {
        if (cb == null) return;
        switch (cb.getType()) {
        }
    }

    @OAMany(
        displayName = "Ledger Denomination Bundles", 
        toClass = LedgerDenominationBundle.class, 
        reverseName = LedgerDenominationBundle.P_StoreSafeLedgerEntry, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<LedgerDenominationBundle> getLedgerDenominationBundles() {
        if (hubLedgerDenominationBundles == null) {
            hubLedgerDenominationBundles = (Hub<LedgerDenominationBundle>) getHub(P_LedgerDenominationBundles);
        }
        return hubLedgerDenominationBundles;
    }
    @OAObjCallback(enabledProperty = StoreSafeLedgerEntry.P_UsesLedgerDenominationBundle)
    public void ledgerDenominationBundlesCallback(OAObjectCallback cb) {
        if (cb == null) return;
        switch (cb.getType()) {
        }
    }

    @OAOne(
        displayName = "Manual Purchase Order", 
        reverseName = ManualPurchaseOrder.P_StoreSafeLedgerEntry, 
        allowCreateNew = false
    )
    public ManualPurchaseOrder getManualPurchaseOrder() {
        if (manualPurchaseOrder == null) {
            manualPurchaseOrder = (ManualPurchaseOrder) getObject(P_ManualPurchaseOrder);
        }
        return manualPurchaseOrder;
    }
    public void setManualPurchaseOrder(ManualPurchaseOrder newValue) {
        ManualPurchaseOrder old = this.manualPurchaseOrder;
        fireBeforePropertyChange(P_ManualPurchaseOrder, old, newValue);
        this.manualPurchaseOrder = newValue;
        firePropertyChange(P_ManualPurchaseOrder, old, this.manualPurchaseOrder);
    }

    @OAOne(
        displayName = "Store Day Open", 
        reverseName = StoreDayOpen.P_StoreSafeLedgerEntries, 
        isProcessed = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreDayOpenId, toProperty = StoreDayOpen.P_Id)}
    )
    public StoreDayOpen getStoreDayOpen() {
        if (storeDayOpen == null) {
            storeDayOpen = (StoreDayOpen) getObject(P_StoreDayOpen);
        }
        return storeDayOpen;
    }
    public void setStoreDayOpen(StoreDayOpen newValue) {
        StoreDayOpen old = this.storeDayOpen;
        fireBeforePropertyChange(P_StoreDayOpen, old, newValue);
        this.storeDayOpen = newValue;
        firePropertyChange(P_StoreDayOpen, old, this.storeDayOpen);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreDayOpenId")
    public Integer getStoreDayOpenId() {
        return (Integer) getFkeyProperty(P_StoreDayOpenId);
    }
    public void setStoreDayOpenId(Integer newValue) {
        this.storeDayOpen = null;
        setFkeyProperty(P_StoreDayOpenId, newValue);
    }

    @OAOne(
        displayName = "Store Safe", 
        reverseName = StoreSafe.P_StoreSafeLedgerEntries, 
        required = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreSafeId, toProperty = StoreSafe.P_Id)}
    )
    public StoreSafe getStoreSafe() {
        if (storeSafe == null) {
            storeSafe = (StoreSafe) getObject(P_StoreSafe);
        }
        return storeSafe;
    }
    public void setStoreSafe(StoreSafe newValue) {
        StoreSafe old = this.storeSafe;
        fireBeforePropertyChange(P_StoreSafe, old, newValue);
        this.storeSafe = newValue;
        firePropertyChange(P_StoreSafe, old, this.storeSafe);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreSafeId")
    public Integer getStoreSafeId() {
        return (Integer) getFkeyProperty(P_StoreSafeId);
    }
    public void setStoreSafeId(Integer newValue) {
        this.storeSafe = null;
        setFkeyProperty(P_StoreSafeId, newValue);
    }

    @OAOne(
        displayName = "Team Member", 
        reverseName = TeamMember.P_StoreSafeLedgerEntries, 
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
        displayName = "Till Ledger Entry", 
        reverseName = TillLedgerEntry.P_StoreSafeLedgerEntry, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false
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
    @OAMethod(displayName = "Post")
    public void post() throws Exception {
        // use this to run on server
        if (isRemoteAvailable()) {
            remote();
            return;
        }
        // custom code
        StoreSafeLedgerEntryDelegate.post(this);
    }
    @OAObjCallback(enabledProperty = StoreSafeLedgerEntry.P_CanPost)
    public void postCallback(OAObjectCallback cb) {
    }

    @OAMethod(displayName = "Create Till Ledger Entry")
    public void createTillLedgerEntry(final StoreSafeLedgerEntryCreateTillLedgerEntryMethod data) throws Exception {
        if (data == null) return;
        // custom code
        StoreSafeLedgerEntryDelegate.createTillLedgerEntry(data, this);
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
        this.pettyCashAmount = rs.getDouble(7);
        setPrimitiveNull(P_PettyCashAmount, rs.wasNull());
        this.note = rs.getString(8);
        timestamp = rs.getTimestamp(9);
        if (timestamp != null) this.posted = new OADateTime(timestamp);
        int storeDayOpenFkey = rs.getInt(10);
        setFkeyProperty(P_StoreDayOpen, rs.wasNull() ? null : storeDayOpenFkey);
        int storeSafeFkey = rs.getInt(11);
        setFkeyProperty(P_StoreSafe, rs.wasNull() ? null : storeSafeFkey);
        int teamMemberFkey = rs.getInt(12);
        setFkeyProperty(P_TeamMember, rs.wasNull() ? null : teamMemberFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
