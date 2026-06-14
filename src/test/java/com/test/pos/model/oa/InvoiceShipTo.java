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
    lowerName = "invoiceShipTo",
    pluralName = "InvoiceShipTos",
    shortName = "ist",
    displayName = "Invoice Ship To",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "InvoiceShipToAddress", fkey = true, columns = { @OAIndexColumn(name = "AddressId") }), 
        @OAIndex(name = "InvoiceShipToInvoiceBasket", fkey = true, columns = { @OAIndexColumn(name = "InvoiceBasketId") })
    }
)
public class InvoiceShipTo extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(InvoiceShipTo.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_Address = "address";
    public static final String P_AddressId = "addressId"; // fkey
    public static final String P_InvoiceBasket = "invoiceBasket";
    public static final String P_InvoiceBasketId = "invoiceBasketId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected volatile transient Address address;
    protected volatile transient InvoiceBasket invoiceBasket;
     
    public InvoiceShipTo() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public InvoiceShipTo(int id) {
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

    @OAOne(
        reverseName = Address.P_InvoiceShipTos, 
        allowAddExisting = false, 
        fkeys = {@OAFkey(fromProperty = P_AddressId, toProperty = Address.P_Id)}
    )
    public Address getAddress() {
        if (address == null) {
            address = (Address) getObject(P_Address);
        }
        return address;
    }
    public void setAddress(Address newValue) {
        Address old = this.address;
        fireBeforePropertyChange(P_Address, old, newValue);
        this.address = newValue;
        firePropertyChange(P_Address, old, this.address);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "AddressId")
    public Integer getAddressId() {
        return (Integer) getFkeyProperty(P_AddressId);
    }
    public void setAddressId(Integer newValue) {
        this.address = null;
        setFkeyProperty(P_AddressId, newValue);
    }

    @OAOne(
        displayName = "Invoice Basket", 
        reverseName = InvoiceBasket.P_InvoiceShipTo, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_InvoiceBasketId, toProperty = InvoiceBasket.P_Id)}
    )
    public InvoiceBasket getInvoiceBasket() {
        if (invoiceBasket == null) {
            invoiceBasket = (InvoiceBasket) getObject(P_InvoiceBasket);
        }
        return invoiceBasket;
    }
    public void setInvoiceBasket(InvoiceBasket newValue) {
        InvoiceBasket old = this.invoiceBasket;
        fireBeforePropertyChange(P_InvoiceBasket, old, newValue);
        this.invoiceBasket = newValue;
        firePropertyChange(P_InvoiceBasket, old, this.invoiceBasket);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "InvoiceBasketId")
    public Integer getInvoiceBasketId() {
        return (Integer) getFkeyProperty(P_InvoiceBasketId);
    }
    public void setInvoiceBasketId(Integer newValue) {
        this.invoiceBasket = null;
        setFkeyProperty(P_InvoiceBasketId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int addressFkey = rs.getInt(3);
        setFkeyProperty(P_Address, rs.wasNull() ? null : addressFkey);
        int invoiceBasketFkey = rs.getInt(4);
        setFkeyProperty(P_InvoiceBasket, rs.wasNull() ? null : invoiceBasketFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
