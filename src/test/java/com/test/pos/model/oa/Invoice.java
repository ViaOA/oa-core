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
    lowerName = "invoice",
    pluralName = "Invoices",
    shortName = "inv",
    displayName = "Invoice",
    displayProperty = "id",
    filterClasses = {InvoiceOpenFilter.class},
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "InvoiceCustomer", fkey = true, columns = { @OAIndexColumn(name = "CustomerId") }), 
        @OAIndex(name = "InvoiceRegisterSession", fkey = true, columns = { @OAIndexColumn(name = "RegisterSessionId") })
    }
)
public class Invoice extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Invoice.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Completed = "completed";
     
    public static final String P_CanBeCompleted = "canBeCompleted";
    public static final String P_TotalItemAmount = "totalItemAmount";
    public static final String P_TotalDiscountAmount = "totalDiscountAmount";
    public static final String P_TotalTaxAmount = "totalTaxAmount";
    public static final String P_TotalAmountDue = "totalAmountDue";
    public static final String P_TotalPaymentAmount = "totalPaymentAmount";
    public static final String P_RemainingBalanceAmount = "remainingBalanceAmount";
    public static final String P_TotalRefundAmount = "totalRefundAmount";
    public static final String P_IsPaidInFull = "isPaidInFull";
     
    public static final String P_Customer = "customer";
    public static final String P_CustomerId = "customerId"; // fkey
    public static final String P_InvoiceBaskets = "invoiceBaskets";
    public static final String P_InvoicePayments = "invoicePayments";
    public static final String P_PurchaseOrders = "purchaseOrders";
    public static final String P_PurchaseOrdersId = "purchaseOrdersId"; // fkey
    public static final String P_Quote = "quote";
    public static final String P_RefundInvoices = "refundInvoices";
    public static final String P_RegisterSession = "registerSession";
    public static final String P_RegisterSessionId = "registerSessionId"; // fkey
     
    public static final String M_UpdateWithNetPriceCaclulator = "updateWithNetPriceCaclulator";
    public static final String M_CompleteSale = "completeSale";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile OADateTime completed;
     
    // Links to other objects.
    protected volatile transient Customer customer;
    protected transient Hub<InvoiceBasket> hubInvoiceBaskets;
    protected transient Hub<InvoicePayment> hubInvoicePayments;
    protected transient Hub<PurchaseOrder> hubPurchaseOrders;
    protected volatile transient Quote quote;
    protected transient Hub<RefundInvoice> hubRefundInvoices;
    protected volatile transient RegisterSession registerSession;
     
    public Invoice() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Invoice(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(enabledProperty = Invoice.P_Completed, enabledValue = false)
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

    @OAProperty(lowerName = "completed", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "Completed", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCompleted() {
        return completed;
    }
    public void setCompleted(OADateTime newValue) {
        OADateTime old = completed;
        fireBeforePropertyChange(P_Completed, old, newValue);
        this.completed = newValue;
        firePropertyChange(P_Completed, old, this.completed);
    }
     
    @OAObjCallback(enabledProperty = Invoice.P_CanBeCompleted)
    public void completedCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }
    @OACalculatedProperty(displayName = "Can Be Completed", displayLength = 5, columnLength = 16, properties = {P_InvoicePayments+"."+InvoicePayment.P_Amount, P_Completed, P_InvoiceBaskets+"."+InvoiceBasket.P_LineItems+"."+LineItem.P_TotalItemAmount})
    public boolean getCanBeCompleted() {
        return InvoiceDelegate.getCanBeCompleted(this);
    }
    public boolean canBeCompleted() {
        return getCanBeCompleted();
    }
    @OACalculatedProperty(displayName = "Total Item Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 17, properties = {P_InvoiceBaskets+"."+InvoiceBasket.P_LineItems+"."+LineItem.P_TotalItemAmount})
    public double getTotalItemAmount() {
        return InvoiceDelegate.getTotalItemAmount(this);
    }
    @OACalculatedProperty(displayName = "Total Discount Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 21)
    public double getTotalDiscountAmount() {
        return InvoiceDelegate.getTotalDiscountAmount(this);
    }
    @OACalculatedProperty(displayName = "Total Tax Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 16)
    public double getTotalTaxAmount() {
        return InvoiceDelegate.getTotalTaxAmount(this);
    }
    @OACalculatedProperty(displayName = "Total Amount Due", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 16)
    public double getTotalAmountDue() {
        return InvoiceDelegate.getTotalAmountDue(this);    
    }
    @OACalculatedProperty(displayName = "Total Payment Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 20, properties = {P_InvoicePayments+"."+InvoicePayment.P_Amount, P_InvoicePayments+"."+InvoicePayment.P_Applied})
    public double getTotalPaymentAmount() {
        return InvoiceDelegate.getTotalPaymentAmount(this);    
    }
    @OACalculatedProperty(displayName = "Remaining Balance Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 24)
    public double getRemainingBalanceAmount() {
        return InvoiceDelegate.getRemainingBalanceAmount(this);
    }
    @OACalculatedProperty(displayName = "Total Refund Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 19)
    public double getTotalRefundAmount() {
        return InvoiceDelegate.getTotalRefundAmount(this);
    }
    @OACalculatedProperty(displayName = "Is Paid In Full", displayLength = 5, columnLength = 15)
    public boolean getIsPaidInFull() {
        return InvoiceDelegate.getIsPaidInFull(this);
    }
    public boolean isIsPaidInFull() {
        return getIsPaidInFull();
    }

    @OAOne(
        reverseName = Customer.P_Invoices, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_CustomerId, toProperty = Customer.P_Id)}
    )
    public Customer getCustomer() {
        if (customer == null) {
            customer = (Customer) getObject(P_Customer);
        }
        return customer;
    }
    public void setCustomer(Customer newValue) {
        Customer old = this.customer;
        fireBeforePropertyChange(P_Customer, old, newValue);
        this.customer = newValue;
        firePropertyChange(P_Customer, old, this.customer);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "CustomerId")
    public Integer getCustomerId() {
        return (Integer) getFkeyProperty(P_CustomerId);
    }
    public void setCustomerId(Integer newValue) {
        this.customer = null;
        setFkeyProperty(P_CustomerId, newValue);
    }

    @OAMany(
        displayName = "Invoice Baskets", 
        toClass = InvoiceBasket.class, 
        owner = true, 
        reverseName = InvoiceBasket.P_Invoice, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<InvoiceBasket> getInvoiceBaskets() {
        if (hubInvoiceBaskets == null) {
            hubInvoiceBaskets = (Hub<InvoiceBasket>) getHub(P_InvoiceBaskets);
        }
        return hubInvoiceBaskets;
    }

    @OAMany(
        displayName = "Invoice Payments", 
        toClass = InvoicePayment.class, 
        owner = true, 
        reverseName = InvoicePayment.P_Invoice, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<InvoicePayment> getInvoicePayments() {
        if (hubInvoicePayments == null) {
            hubInvoicePayments = (Hub<InvoicePayment>) getHub(P_InvoicePayments);
        }
        return hubInvoicePayments;
    }

    @OAMany(
        displayName = "Purchase Orders", 
        toClass = PurchaseOrder.class, 
        reverseName = PurchaseOrder.P_Invoices
    )
    @OALinkTable(name = "InvoicePurchaseOrder", indexName = "PurchaseOrderInvoice", columns = {"InvoiceId"})
    public Hub<PurchaseOrder> getPurchaseOrders() {
        if (hubPurchaseOrders == null) {
            hubPurchaseOrders = (Hub<PurchaseOrder>) getHub(P_PurchaseOrders);
        }
        return hubPurchaseOrders;
    }

    @OAOne(
        reverseName = Quote.P_Invoice, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    public Quote getQuote() {
        if (quote == null) {
            quote = (Quote) getObject(P_Quote);
        }
        return quote;
    }
    public void setQuote(Quote newValue) {
        Quote old = this.quote;
        fireBeforePropertyChange(P_Quote, old, newValue);
        this.quote = newValue;
        firePropertyChange(P_Quote, old, this.quote);
    }

    @OAMany(
        displayName = "Refund Invoices", 
        toClass = RefundInvoice.class, 
        reverseName = RefundInvoice.P_Invoice
    )
    public Hub<RefundInvoice> getRefundInvoices() {
        if (hubRefundInvoices == null) {
            hubRefundInvoices = (Hub<RefundInvoice>) getHub(P_RefundInvoices);
        }
        return hubRefundInvoices;
    }

    @OAOne(
        displayName = "Register Session", 
        reverseName = RegisterSession.P_Invoices, 
        allowCreateNew = false, 
        allowAddExisting = false, 
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
    @OAMethod(displayName = "Update With Net Price Caclulator")
    public void updateWithNetPriceCaclulator() throws Exception {
        // custom code
        InvoiceDelegate.updateWithNetPriceCaclulator(this);
    }

    @OAMethod(displayName = "Complete Sale")
    public void completeSale() throws Exception {
        // custom code
        InvoiceDelegate.completeSale(this);
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(3);
        if (timestamp != null) this.completed = new OADateTime(timestamp);
        int customerFkey = rs.getInt(4);
        setFkeyProperty(P_Customer, rs.wasNull() ? null : customerFkey);
        int registerSessionFkey = rs.getInt(5);
        setFkeyProperty(P_RegisterSession, rs.wasNull() ? null : registerSessionFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
