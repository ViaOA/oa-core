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
import java.awt.Color;
import com.viaoa.util.OAConverter;
 
@OAClass(
    shortName = "cc",
    displayName = "Color Code",
    isLookup = true,
    isPreSelect = true,
    displayProperty = "codeName",
    sortProperty = "code"
)
@OATable(
)
public class ColorCode extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ColorCode.class.getName());

    public static final String PROPERTY_Id = "Id";
    public static final String P_Id = "Id";
    public static final String PROPERTY_Code = "Code";
    public static final String P_Code = "Code";
    public static final String PROPERTY_Name = "Name";
    public static final String P_Name = "Name";
    public static final String PROPERTY_Color = "Color";
    public static final String P_Color = "Color";
    public static final String PROPERTY_Seq = "Seq";
    public static final String P_Seq = "Seq";
     
    public static final String PROPERTY_CodeName = "CodeName";
    public static final String P_CodeName = "CodeName";
     
    public static final String PROPERTY_Orders = "Orders";
    public static final String P_Orders = "Orders";
     
    protected volatile int id;
    protected volatile String code;
    protected volatile String name;
    protected volatile Color color;
    protected volatile int seq;
     
     
    public ColorCode() {
    }
     
    public ColorCode(int id) {
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
     
    @OAProperty(maxLength = 20, displayLength = 15)
    @OAColumn(maxLength = 20)
    public String getCode() {
        return code;
    }
    public void setCode(String newValue) {
        String old = code;
        fireBeforePropertyChange(P_Code, old, newValue);
        this.code = newValue;
        firePropertyChange(P_Code, old, this.code);
    }
     
    @OAProperty(maxLength = 50, displayLength = 15)
    @OAColumn(maxLength = 50)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
     
    @OAProperty(displayLength = 12, columnLength = 8)
    @OAColumn(maxLength = 16)
    public Color getColor() {
        return color;
    }
    public void setColor(Color newValue) {
        Color old = color;
        fireBeforePropertyChange(P_Color, old, newValue);
        this.color = newValue;
        firePropertyChange(P_Color, old, this.color);
    }
     
    @OAProperty(displayLength = 6, isAutoSeq = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getSeq() {
        return seq;
    }
    public void setSeq(int newValue) {
        int old = seq;
        fireBeforePropertyChange(P_Seq, old, newValue);
        this.seq = newValue;
        firePropertyChange(P_Seq, old, this.seq);
    }
     
    @OACalculatedProperty(displayName = "Color", displayLength = 20, columnLength = 15, properties = {P_Code, P_Name})
    public String getCodeName() {
        String s = OAString.concat(code, name, " - ");
        return s;
    }
     
    @OAMany(
        toClass = Order.class, 
        reverseName = Order.P_ColorCode, 
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
        this.color = (Color) OAConverter.convert(Color.class, rs.getString(4));
        this.seq = (int) rs.getInt(5);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, ColorCode.P_Seq, true);
        }
        if (rs.getMetaData().getColumnCount() != 5) {
            throw new SQLException("invalid number of columns for load method");
        }

        changedFlag = false;
        newFlag = false;
    }
}
 
