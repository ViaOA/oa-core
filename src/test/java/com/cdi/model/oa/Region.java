// Generated by OABuilder
package com.cdi.model.oa;
 
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.annotation.*;
import com.cdi.delegate.oa.*;
import com.cdi.model.oa.filter.*;
import com.cdi.model.oa.propertypath.*;
 
@OAClass(
    shortName = "reg",
    displayName = "Region",
    isLookup = true,
    isPreSelect = true,
    displayProperty = "codeName",
    sortProperty = "code"
)
@OATable(
)
public class Region extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Region.class.getName());

    public static final String PROPERTY_Id = "Id";
    public static final String P_Id = "Id";
    public static final String PROPERTY_Code = "Code";
    public static final String P_Code = "Code";
    public static final String PROPERTY_Name = "Name";
    public static final String P_Name = "Name";
    public static final String PROPERTY_Description = "Description";
    public static final String P_Description = "Description";
     
    public static final String PROPERTY_CodeName = "CodeName";
    public static final String P_CodeName = "CodeName";
     
    public static final String PROPERTY_Customers = "Customers";
    public static final String P_Customers = "Customers";
    public static final String PROPERTY_Orders = "Orders";
    public static final String P_Orders = "Orders";
     
    protected volatile int id;
    protected volatile String code;
    protected volatile String name;
    protected volatile String description;
     
     
    public Region() {
    }
     
    public Region(int id) {
        this();
        setId(id);
    }
     

    @OAProperty(isUnique = true, displayLength = 6)
    @OAId()
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }
     
    @OAProperty(maxLength = 15, displayLength = 10)
    @OAColumn(maxLength = 15)
    public String getCode() {
        return code;
    }
    public void setCode(String newValue) {
        String old = code;
        fireBeforePropertyChange(P_Code, old, newValue);
        this.code = newValue;
        firePropertyChange(P_Code, old, this.code);
    }
     
    @OAProperty(maxLength = 175, displayLength = 20, columnLength = 15)
    @OAColumn(maxLength = 175)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
     
    @OAProperty(maxLength = 254, displayLength = 20, columnLength = 15)
    @OAColumn(maxLength = 254)
    public String getDescription() {
        return description;
    }
    public void setDescription(String newValue) {
        String old = description;
        fireBeforePropertyChange(P_Description, old, newValue);
        this.description = newValue;
        firePropertyChange(P_Description, old, this.description);
    }
     
    @OACalculatedProperty(displayName = "Code Name", displayLength = 20, columnLength = 15, properties = {P_Code, P_Name})
    public String getCodeName() {
        String result = "";
        if (code != null) {
            result = code + " ";
        }
        if (name != null) result += name;
        return result;
    }
     
    @OAMany(
        toClass = Customer.class, 
        reverseName = Customer.P_Region, 
        createMethod = false
    )
    private Hub<Customer> getCustomers() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
     
    @OAMany(
        toClass = Order.class, 
        reverseName = Order.P_Region, 
        isProcessed = true, 
        createMethod = false
    )
    private Hub<Order> getOrders() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
     
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        this.code = rs.getString(2);
        this.name = rs.getString(3);
        this.description = rs.getString(4);
        if (rs.getMetaData().getColumnCount() != 4) {
            throw new SQLException("invalid number of columns for load method");
        }

        changedFlag = false;
        newFlag = false;
    }
}
 
