// Generated by OABuilder
package com.cdi.model.oa.propertypath;
 
import java.io.Serializable;
import com.cdi.model.oa.*;
 
public class ImageStorePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ImageStorePPx(String name) {
        this(null, name);
    }

    public ImageStorePPx(PPxInterface parent, String name) {
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

    public ItemPPx item() {
        ItemPPx ppx = new ItemPPx(this, ImageStore.P_Item);
        return ppx;
    }

    public SalesOrderNotePPx salesOrderNotes() {
        SalesOrderNotePPx ppx = new SalesOrderNotePPx(this, ImageStore.P_SalesOrderNotes);
        return ppx;
    }

    public WebPartPPx webPart() {
        WebPartPPx ppx = new WebPartPPx(this, ImageStore.P_WebPart);
        return ppx;
    }

    public String id() {
        return pp + "." + ImageStore.P_Id;
    }

    public String date() {
        return pp + "." + ImageStore.P_Date;
    }

    public String bytes() {
        return pp + "." + ImageStore.P_Bytes;
    }

    public String name() {
        return pp + "." + ImageStore.P_Name;
    }

    public String origFileName() {
        return pp + "." + ImageStore.P_OrigFileName;
    }

    public String updated() {
        return pp + "." + ImageStore.P_Updated;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
