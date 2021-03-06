// Generated by OABuilder
package com.cdi.model.oa.propertypath;
 
import java.io.Serializable;
import com.cdi.model.oa.*;
 
public class ServiceCodePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ServiceCodePPx(String name) {
        this(null, name);
    }

    public ServiceCodePPx(PPxInterface parent, String name) {
        String s = null;
        if (parent != null) {
            s = parent.toString();
        }
        if (s == null) s = "";
        if (name != null && name.length() > 0) {
            if (s.length() > 0 && name.charAt(0) != ':') s += ".";
            s += name;
        }
        pp = s;
    }

    public SalesOrderItemPPx salesOrderItems() {
        SalesOrderItemPPx ppx = new SalesOrderItemPPx(this, ServiceCode.P_SalesOrderItems);
        return ppx;
    }

    public String id() {
        return pp + "." + ServiceCode.P_Id;
    }

    public String name() {
        return pp + "." + ServiceCode.P_Name;
    }

    public String description() {
        return pp + "." + ServiceCode.P_Description;
    }

    public String price() {
        return pp + "." + ServiceCode.P_Price;
    }

    public String itemCode() {
        return pp + "." + ServiceCode.P_ItemCode;
    }

    public String qbListId() {
        return pp + "." + ServiceCode.P_QbListId;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
