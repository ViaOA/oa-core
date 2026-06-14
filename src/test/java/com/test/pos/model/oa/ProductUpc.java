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
    lowerName = "productUpc",
    pluralName = "ProductUpcs",
    shortName = "pru",
    displayName = "Product Upc",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ProductUpcUpc", columns = {@OAIndexColumn(name = "Upc", lowerName = "UpcLower")}),
        @OAIndex(name = "ProductUpcBarcodeType", fkey = true, columns = { @OAIndexColumn(name = "BarcodeTypeId") }), 
        @OAIndex(name = "ProductUpcProduct", fkey = true, columns = { @OAIndexColumn(name = "ProductId") })
    }
)
public class ProductUpc extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ProductUpc.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_UPC = "upc";
     
    public static final String P_BarcodeType = "barcodeType";
    public static final String P_BarcodeTypeId = "barcodeTypeId"; // fkey
    public static final String P_Product = "product";
    public static final String P_ProductId = "productId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String upc;
     
    // Links to other objects.
    protected volatile transient BarcodeType barcodeType;
    protected volatile transient Product product;
     
    public ProductUpc() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public ProductUpc(int id) {
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

    @OAProperty(lowerName = "upc", maxLength = 35, displayLength = 18)
    @OAColumn(name = "Upc", maxLength = 35, lowerName = "UpcLower")
    public String getUPC() {
        return upc;
    }
    public void setUPC(String newValue) {
        String old = upc;
        fireBeforePropertyChange(P_UPC, old, newValue);
        this.upc = newValue;
        firePropertyChange(P_UPC, old, this.upc);
    }

    @OAOne(
        displayName = "Barcode Type", 
        reverseName = BarcodeType.P_ProductUpcs, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_BarcodeTypeId, toProperty = BarcodeType.P_Id)}
    )
    public BarcodeType getBarcodeType() {
        if (barcodeType == null) {
            barcodeType = (BarcodeType) getObject(P_BarcodeType);
        }
        return barcodeType;
    }
    public void setBarcodeType(BarcodeType newValue) {
        BarcodeType old = this.barcodeType;
        fireBeforePropertyChange(P_BarcodeType, old, newValue);
        this.barcodeType = newValue;
        firePropertyChange(P_BarcodeType, old, this.barcodeType);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "BarcodeTypeId")
    public Integer getBarcodeTypeId() {
        return (Integer) getFkeyProperty(P_BarcodeTypeId);
    }
    public void setBarcodeTypeId(Integer newValue) {
        this.barcodeType = null;
        setFkeyProperty(P_BarcodeTypeId, newValue);
    }

    @OAOne(
        reverseName = Product.P_ProductUpcs, 
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
        this.upc = rs.getString(3);
        int barcodeTypeFkey = rs.getInt(4);
        setFkeyProperty(P_BarcodeType, rs.wasNull() ? null : barcodeTypeFkey);
        int productFkey = rs.getInt(5);
        setFkeyProperty(P_Product, rs.wasNull() ? null : productFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
