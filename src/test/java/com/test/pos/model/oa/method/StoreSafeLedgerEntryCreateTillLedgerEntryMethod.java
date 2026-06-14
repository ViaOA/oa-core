package com.test.pos.model.oa.method;

import java.util.*;
import java.util.logging.*;
import com.test.pos.model.oa.*;
import com.test.pos.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.hub.filter.*;

@OAClass(useDataSource=false, localOnly=true)
public class StoreSafeLedgerEntryCreateTillLedgerEntryMethod extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(StoreSafeLedgerEntryCreateTillLedgerEntryMethod.class.getName());

    public static final String P_Till = "Till";
    public static final String P_StoreSafeLedgerEntry = "storeSafeLedgerEntry";

    protected Till till;
    protected StoreSafeLedgerEntry storeSafeLedgerEntry;


    @OAOne
    public StoreSafeLedgerEntry getStoreSafeLedgerEntry() {
        if (storeSafeLedgerEntry == null) {
            storeSafeLedgerEntry = (StoreSafeLedgerEntry) getObject(P_StoreSafeLedgerEntry);
        }
        return storeSafeLedgerEntry;
    }
    public void setStoreSafeLedgerEntry(StoreSafeLedgerEntry newValue) {
        StoreSafeLedgerEntry old = this.storeSafeLedgerEntry;
        this.storeSafeLedgerEntry = newValue;
        firePropertyChange(P_StoreSafeLedgerEntry, old, this.storeSafeLedgerEntry);
    }

    @OAOne(selectFromPropertyPath = (StoreSafeLedgerEntry.P_StoreSafe+"."+StoreSafe.P_Store+"."+Store.P_Tills))
    public Till getTill() {
        if (till == null) {
            till = (Till) getObject(P_Till);
        }
        return till;
    }
    public void setTill(Till newValue) {
        Till old = this.till;
        this.till = newValue;
        firePropertyChange(P_Till, old, this.till);
    }

    public void reset() {
        setTill(null);
    }
}
