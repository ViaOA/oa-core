package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StorePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StorePPx(String name) {
        this(null, name);
    }

    public StorePPx(PPxInterface parent, String name) {
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

    public AddressPPx address() {
        AddressPPx ppx = new AddressPPx(this, Store.P_Address);
        return ppx;
    }

    public LedgerDenominationBundlePPx calcLedgerDenominationBundles() {
        LedgerDenominationBundlePPx ppx = new LedgerDenominationBundlePPx(this, Store.P_CalcLedgerDenominationBundles);
        return ppx;
    }

    public StoreSafeLedgerEntryPPx calcStoreSafeLedgerEntries() {
        StoreSafeLedgerEntryPPx ppx = new StoreSafeLedgerEntryPPx(this, Store.P_CalcStoreSafeLedgerEntries);
        return ppx;
    }

    public CurrencyTypePPx currencyType() {
        CurrencyTypePPx ppx = new CurrencyTypePPx(this, Store.P_CurrencyType);
        return ppx;
    }

    public ManualPurchaseOrderPPx manualPurchaseOrders() {
        ManualPurchaseOrderPPx ppx = new ManualPurchaseOrderPPx(this, Store.P_ManualPurchaseOrders);
        return ppx;
    }

    public RegisterPPx registers() {
        RegisterPPx ppx = new RegisterPPx(this, Store.P_Registers);
        return ppx;
    }

    public StoreClosedDatePPx storeClosedDates() {
        StoreClosedDatePPx ppx = new StoreClosedDatePPx(this, Store.P_StoreClosedDates);
        return ppx;
    }

    public StoreHoursOpenPPx storeHoursOpens() {
        StoreHoursOpenPPx ppx = new StoreHoursOpenPPx(this, Store.P_StoreHoursOpens);
        return ppx;
    }

    public StoreSafePPx storeSafe() {
        StoreSafePPx ppx = new StoreSafePPx(this, Store.P_StoreSafe);
        return ppx;
    }

    public StoreSchedulePPx storeSchedules() {
        StoreSchedulePPx ppx = new StoreSchedulePPx(this, Store.P_StoreSchedules);
        return ppx;
    }

    public StoreToStoreTransferPPx storeToStoreTransfers() {
        StoreToStoreTransferPPx ppx = new StoreToStoreTransferPPx(this, Store.P_StoreToStoreTransfers);
        return ppx;
    }

    public TeamMemberPPx teamMembers() {
        TeamMemberPPx ppx = new TeamMemberPPx(this, Store.P_TeamMembers);
        return ppx;
    }

    public TillPPx tills() {
        TillPPx ppx = new TillPPx(this, Store.P_Tills);
        return ppx;
    }

    public String id() {
        return pp + "." + Store.P_Id;
    }

    public String created() {
        return pp + "." + Store.P_Created;
    }

    public String storeNumber() {
        return pp + "." + Store.P_StoreNumber;
    }

    public String name() {
        return pp + "." + Store.P_Name;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
