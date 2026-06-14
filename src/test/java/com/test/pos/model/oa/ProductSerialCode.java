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
import com.viaoa.datetime.OADate;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "productSerialCode",
    pluralName = "ProductSerialCodes",
    shortName = "psc",
    displayName = "Product Serial Code",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ProductSerialCodeProduct", fkey = true, columns = { @OAIndexColumn(name = "ProductId") })
    }
)
public class ProductSerialCode extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ProductSerialCode.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_ReceivedDate = "receivedDate";
    public static final String P_SerialCode = "serialCode";
    public static final String P_SoldDate = "soldDate";
     
    public static final String P_Product = "product";
    public static final String P_ProductId = "productId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile OADate receivedDate;
    protected volatile String serialCode;
    protected volatile OADate soldDate;
     
    // Links to other objects.
    protected volatile transient Product product;
     
    public ProductSerialCode() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public ProductSerialCode(int id) {
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

    @OAProperty(lowerName = "receivedDate", displayName = "Received Date", displayLength = 8, uiColumnLength = 13)
    @OAColumn(name = "ReceivedDate", sqlType = java.sql.Types.DATE)
    public OADate getReceivedDate() {
        return receivedDate;
    }
    public void setReceivedDate(OADate newValue) {
        OADate old = receivedDate;
        fireBeforePropertyChange(P_ReceivedDate, old, newValue);
        this.receivedDate = newValue;
        firePropertyChange(P_ReceivedDate, old, this.receivedDate);
    }

    @OAProperty(lowerName = "serialCode", displayName = "Serial Code", maxLength = 35, displayLength = 18)
    @OAColumn(name = "SerialCode", maxLength = 35)
    public String getSerialCode() {
        return serialCode;
    }
    public void setSerialCode(String newValue) {
        String old = serialCode;
        fireBeforePropertyChange(P_SerialCode, old, newValue);
        this.serialCode = newValue;
        firePropertyChange(P_SerialCode, old, this.serialCode);
    }

    @OAProperty(lowerName = "soldDate", displayName = "Sold Date", displayLength = 8, uiColumnLength = 9)
    @OAColumn(name = "SoldDate", sqlType = java.sql.Types.DATE)
    public OADate getSoldDate() {
        return soldDate;
    }
    public void setSoldDate(OADate newValue) {
        OADate old = soldDate;
        fireBeforePropertyChange(P_SoldDate, old, newValue);
        this.soldDate = newValue;
        firePropertyChange(P_SoldDate, old, this.soldDate);
    }

    @OAOne(
        reverseName = Product.P_ProductSerialCodes, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ProductId, toProperty = Product.P_Id)}
    )
    public Product getProduct() {
        if (product == null) {
            product = (Product) getObject(P_Product);
        }
        return product;
    }
    public void setProduct(Product newValue) {
        Product old = this.product;
        fireBeforePropertyChange(P_Product, old, newValue);
        this.product = newValue;
        firePropertyChange(P_Product, old, this.product);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ProductId")
    public Integer getProductId() {
        return (Integer) getFkeyProperty(P_ProductId);
    }
    public void setProductId(Integer newValue) {
        this.product = null;
        setFkeyProperty(P_ProductId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        java.sql.Date date;
        date = rs.getDate(3);
        if (date != null) this.receivedDate = new OADate(date);
        this.serialCode = rs.getString(4);
        date = rs.getDate(5);
        if (date != null) this.soldDate = new OADate(date);
        int productFkey = rs.getInt(6);
        setFkeyProperty(P_Product, rs.wasNull() ? null : productFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
