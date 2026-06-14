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
    lowerName = "vertexTaxCode",
    pluralName = "VertexTaxCodes",
    shortName = "vtc",
    displayName = "Vertex Tax Code",
    isLookup = true,
    isPreSelect = true,
    displayProperty = "taxCode",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "VertexTaxCodeTaxCode", columns = {@OAIndexColumn(name = "TaxCode", lowerName = "TaxCodeLower")})
    }
)
public class VertexTaxCode extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(VertexTaxCode.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_TaxCode = "taxCode";
    public static final String P_TaxAuthority = "taxAuthority";
     
    public static final String P_CurrentVertexTaxCodeRate = "currentVertexTaxCodeRate";
    public static final String P_Items = "items";
    public static final String P_ItemsId = "itemsId"; // fkey
    public static final String P_RootItemCategories = "rootItemCategories";
    public static final String P_VertexTaxCodeRates = "vertexTaxCodeRates";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String taxCode;
    protected volatile String taxAuthority;
     
    // Links to other objects.
    protected transient Hub<VertexTaxCodeRate> hubVertexTaxCodeRates;
     
    public VertexTaxCode() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public VertexTaxCode(int id) {
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

    @OAProperty(lowerName = "taxCode", displayName = "Tax Code", maxLength = 8, displayLength = 8)
    @OAColumn(name = "TaxCode", maxLength = 8, lowerName = "TaxCodeLower")
    public String getTaxCode() {
        return taxCode;
    }
    public void setTaxCode(String newValue) {
        String old = taxCode;
        fireBeforePropertyChange(P_TaxCode, old, newValue);
        this.taxCode = newValue;
        firePropertyChange(P_TaxCode, old, this.taxCode);
    }

    @OAProperty(lowerName = "taxAuthority", displayName = "Tax Authority", maxLength = 20, displayLength = 15)
    @OAColumn(name = "TaxAuthority", maxLength = 20)
    public String getTaxAuthority() {
        return taxAuthority;
    }
    public void setTaxAuthority(String newValue) {
        String old = taxAuthority;
        fireBeforePropertyChange(P_TaxAuthority, old, newValue);
        this.taxAuthority = newValue;
        firePropertyChange(P_TaxAuthority, old, this.taxAuthority);
    }

    @OAOne(
        displayName = "Current Vertex Tax Code Rate", 
        isCalculated = true, 
        calcDependentProperties = {P_VertexTaxCodeRates+"."+VertexTaxCodeRate.P_BeginDate, P_VertexTaxCodeRates+"."+VertexTaxCodeRate.P_EndDate, P_VertexTaxCodeRates+"."+VertexTaxCodeRate.P_MinTaxable, P_VertexTaxCodeRates+"."+VertexTaxCodeRate.P_MaxTaxable}, 
        reverseName = VertexTaxCodeRate.P_CalcVertexTaxCode, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    public VertexTaxCodeRate getCurrentVertexTaxCodeRate() {
        // Custom code here to get CurrentVertexTaxCodeRate
        return VertexTaxCodeDelegate.getCurrentVertexTaxCodeRate(this);
    }

    @OAMany(
        toClass = Item.class, 
        reverseName = Item.P_VertexTaxCodes, 
        createMethod = false
    )
    @OALinkTable(name = "VertexTaxCodeItem", indexName = "ItemVertexTaxCode", columns = {"VertexTaxCodeId"})
    private Hub<Item> getItems() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Root Item Categories", 
        toClass = ItemCategory.class, 
        recursive = false, 
        reverseName = ItemCategory.P_VertexTaxCode, 
        createMethod = false
    )
    private Hub<ItemCategory> getRootItemCategories() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Vertex Tax Code Rates", 
        toClass = VertexTaxCodeRate.class, 
        owner = true, 
        reverseName = VertexTaxCodeRate.P_VertexTaxCode, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<VertexTaxCodeRate> getVertexTaxCodeRates() {
        if (hubVertexTaxCodeRates == null) {
            hubVertexTaxCodeRates = (Hub<VertexTaxCodeRate>) getHub(P_VertexTaxCodeRates);
        }
        return hubVertexTaxCodeRates;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.taxCode = rs.getString(3);
        this.taxAuthority = rs.getString(4);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
