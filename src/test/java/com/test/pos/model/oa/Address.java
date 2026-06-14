package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.lang.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "address",
    pluralName = "Addresses",
    shortName = "add",
    displayName = "Address",
    displayProperty = "name",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "AddressCustomer", fkey = true, columns = { @OAIndexColumn(name = "CustomerId") })
    }
)
public class Address extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Address.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
    public static final String P_Address1 = "address1";
    public static final String P_Address2 = "address2";
    public static final String P_City = "city";
    public static final String P_State = "state";
    public static final String P_Zip = "zip";
    public static final String P_Zip4 = "zip4";
    public static final String P_Type = "type";
    public static final String P_TypeString = "typeString";
    public static final String P_TypeEnum = "typeEnum";
    public static final String P_TypeDisplay = "typeDisplay";
    public static final String P_GIS = "gis";
    public static final String P_Timezone = "timezone";
     
    public static final String P_CalcCityStateZip = "calcCityStateZip";
     
    public static final String P_Customer = "customer";
    public static final String P_CustomerId = "customerId"; // fkey
    public static final String P_InvoiceShipTos = "invoiceShipTos";
    public static final String P_Store = "store";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
    protected volatile String address1;
    protected volatile String address2;
    protected volatile String city;
    protected volatile String state;
    protected volatile String zip;
    protected volatile String zip4;
    protected volatile int type;

    public static enum Type {
        Unknown("Unknown"),
        Home("Home"),
        Business("Business"),
        SecondHome("Second Home");

        private String display;
        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int TYPE_Unknown = 0;
    public static final int TYPE_Home = 1;
    public static final int TYPE_Business = 2;
    public static final int TYPE_SecondHome = 3;

    protected volatile String gis;
    protected volatile String timezone;
     
    // Links to other objects.
    protected volatile transient Customer customer;
    protected volatile transient Store store;
     
    public Address() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Address(int id) {
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

    @OAProperty(lowerName = "name", maxLength = 50, displayLength = 18)
    @OAColumn(name = "Name", maxLength = 50)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }

    @OAProperty(lowerName = "address1", maxLength = 50, displayLength = 18)
    @OAColumn(name = "Address1", maxLength = 50)
    public String getAddress1() {
        return address1;
    }
    public void setAddress1(String newValue) {
        String old = address1;
        fireBeforePropertyChange(P_Address1, old, newValue);
        this.address1 = newValue;
        firePropertyChange(P_Address1, old, this.address1);
    }

    @OAProperty(lowerName = "address2", maxLength = 50, displayLength = 18)
    @OAColumn(name = "Address2", maxLength = 50)
    public String getAddress2() {
        return address2;
    }
    public void setAddress2(String newValue) {
        String old = address2;
        fireBeforePropertyChange(P_Address2, old, newValue);
        this.address2 = newValue;
        firePropertyChange(P_Address2, old, this.address2);
    }

    @OAProperty(lowerName = "city", maxLength = 50, displayLength = 18)
    @OAColumn(name = "City", maxLength = 50)
    public String getCity() {
        return city;
    }
    public void setCity(String newValue) {
        String old = city;
        fireBeforePropertyChange(P_City, old, newValue);
        this.city = newValue;
        firePropertyChange(P_City, old, this.city);
    }

    @OAProperty(lowerName = "state", maxLength = 30, displayLength = 18, uiColumnLength = 8)
    @OAColumn(name = "State", maxLength = 30)
    public String getState() {
        return state;
    }
    public void setState(String newValue) {
        String old = state;
        fireBeforePropertyChange(P_State, old, newValue);
        this.state = newValue;
        firePropertyChange(P_State, old, this.state);
    }

    @OAProperty(lowerName = "zip", maxLength = 20, displayLength = 5)
    @OAColumn(name = "Zip", maxLength = 20)
    public String getZip() {
        return zip;
    }
    public void setZip(String newValue) {
        String old = zip;
        fireBeforePropertyChange(P_Zip, old, newValue);
        this.zip = newValue;
        firePropertyChange(P_Zip, old, this.zip);
    }

    @OAProperty(lowerName = "zip4", maxLength = 4, displayLength = 4)
    @OAColumn(name = "Zip4", maxLength = 4)
    public String getZip4() {
        return zip4;
    }
    public void setZip4(String newValue) {
        String old = zip4;
        fireBeforePropertyChange(P_Zip4, old, newValue);
        this.zip4 = newValue;
        firePropertyChange(P_Zip4, old, this.zip4);
    }

    @OAProperty(lowerName = "type", displayLength = 14, uiColumnLength = 6, isNameValue = true)
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
    @OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 14, columnLength = 6, properties = {P_Type} )
    public String getTypeDisplay() {
        Type type = getTypeEnum();
        if (type == null) return null;
        return type.getDisplay();
    }

    @OAProperty(lowerName = "gis", maxLength = 120, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "Gis", maxLength = 120)
    public String getGIS() {
        return gis;
    }
    public void setGIS(String newValue) {
        String old = gis;
        fireBeforePropertyChange(P_GIS, old, newValue);
        this.gis = newValue;
        firePropertyChange(P_GIS, old, this.gis);
    }

    @OAProperty(lowerName = "timezone", maxLength = 60, displayLength = 15, uiColumnLength = 14)
    @OAColumn(name = "Timezone", maxLength = 60)
    public String getTimezone() {
        return timezone;
    }
    public void setTimezone(String newValue) {
        String old = timezone;
        fireBeforePropertyChange(P_Timezone, old, newValue);
        this.timezone = newValue;
        firePropertyChange(P_Timezone, old, this.timezone);
    }
    @OACalculatedProperty(displayName = "City/State/Zip", displayLength = 32, columnLength = 20, properties = {P_City, P_State, P_Zip})
    public String getCalcCityStateZip() {
        String csz = OAStr.concat(getCity(), getState(), ", ");
        csz = OAStr.concat(csz, getZip(), " ");
        return csz;
    }

    @OAOne(
        reverseName = Customer.P_Addresses, 
        allowCreateNew = false, 
        isOneAndOnlyOne = true, 
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
        displayName = "Invoice Ship Tos", 
        toClass = InvoiceShipTo.class, 
        reverseName = InvoiceShipTo.P_Address, 
        createMethod = false
    )
    private Hub<InvoiceShipTo> getInvoiceShipTos() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        reverseName = Store.P_Address, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        isOneAndOnlyOne = true
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
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        this.address1 = rs.getString(4);
        this.address2 = rs.getString(5);
        this.city = rs.getString(6);
        this.state = rs.getString(7);
        this.zip = rs.getString(8);
        this.zip4 = rs.getString(9);
        this.type = rs.getInt(10);
        setPrimitiveNull(P_Type, rs.wasNull());
        this.gis = rs.getString(11);
        this.timezone = rs.getString(12);
        int customerFkey = rs.getInt(13);
        setFkeyProperty(P_Customer, rs.wasNull() ? null : customerFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
