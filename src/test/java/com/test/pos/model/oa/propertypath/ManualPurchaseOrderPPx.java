package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ManualPurchaseOrderPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ManualPurchaseOrderPPx(String name) {
        this(null, name);
    }

    public ManualPurchaseOrderPPx(PPxInterface parent, String name) {
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

    public StorePPx store() {
        StorePPx ppx = new StorePPx(this, ManualPurchaseOrder.P_Store);
        return ppx;
    }

    public StoreSafeLedgerEntryPPx storeSafeLedgerEntry() {
        StoreSafeLedgerEntryPPx ppx = new StoreSafeLedgerEntryPPx(this, ManualPurchaseOrder.P_StoreSafeLedgerEntry);
        return ppx;
    }

    public String id() {
        return pp + "." + ManualPurchaseOrder.P_Id;
    }

    public String created() {
        return pp + "." + ManualPurchaseOrder.P_Created;
    }

    public String cashAmount() {
        return pp + "." + ManualPurchaseOrder.P_CashAmount;
    }

    public String note() {
        return pp + "." + ManualPurchaseOrder.P_Note;
    }

    public String applied() {
        return pp + "." + ManualPurchaseOrder.P_Applied;
    }

    public String apply() {
        return pp + ".apply";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
