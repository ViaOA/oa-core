package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StoreDayOpenPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StoreDayOpenPPx(String name) {
        this(null, name);
    }

    public StoreDayOpenPPx(PPxInterface parent, String name) {
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

    public StoreSafeLedgerEntryPPx storeSafeLedgerEntries() {
        StoreSafeLedgerEntryPPx ppx = new StoreSafeLedgerEntryPPx(this, StoreDayOpen.P_StoreSafeLedgerEntries);
        return ppx;
    }

    public StoreSchedulePPx storeSchedule() {
        StoreSchedulePPx ppx = new StoreSchedulePPx(this, StoreDayOpen.P_StoreSchedule);
        return ppx;
    }

    public String id() {
        return pp + "." + StoreDayOpen.P_Id;
    }

    public String created() {
        return pp + "." + StoreDayOpen.P_Created;
    }

    public String createStoreSafeAudit() {
        return pp + ".createStoreSafeAudit";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
