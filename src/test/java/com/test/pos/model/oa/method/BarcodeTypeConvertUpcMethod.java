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
public class BarcodeTypeConvertUpcMethod extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(BarcodeTypeConvertUpcMethod.class.getName());

    public static final String P_Upc = "Upc";
    public static final String P_BarcodeType = "barcodeType";

    protected String upc;
    protected BarcodeType barcodeType;

    @OAProperty(lowerName = "upc", displayLength = 20)
    public String getUpc() {
        return upc;
    }
    public void setUpc(String newValue) {
        String old = upc;
        fireBeforePropertyChange(P_Upc, old, newValue);
        this.upc = newValue;
        firePropertyChange(P_Upc, old, this.upc);
    }
      

    @OAOne
    public BarcodeType getBarcodeType() {
        if (barcodeType == null) {
            barcodeType = (BarcodeType) getObject(P_BarcodeType);
        }
        return barcodeType;
    }
    public void setBarcodeType(BarcodeType newValue) {
        BarcodeType old = this.barcodeType;
        this.barcodeType = newValue;
        firePropertyChange(P_BarcodeType, old, this.barcodeType);
    }

    public void reset() {
        setUpc(null);
    }
}
