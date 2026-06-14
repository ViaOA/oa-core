package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class DepositSealPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public DepositSealPPx(String name) {
        this(null, name);
    }

    public DepositSealPPx(PPxInterface parent, String name) {
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

    public BankDepositPPx bankDeposit() {
        BankDepositPPx ppx = new BankDepositPPx(this, DepositSeal.P_BankDeposit);
        return ppx;
    }

    public String id() {
        return pp + "." + DepositSeal.P_Id;
    }

    public String created() {
        return pp + "." + DepositSeal.P_Created;
    }

    public String sealNumber() {
        return pp + "." + DepositSeal.P_SealNumber;
    }

    public String issuedTo() {
        return pp + "." + DepositSeal.P_IssuedTo;
    }

    public String usedOn() {
        return pp + "." + DepositSeal.P_UsedOn;
    }

    public String status() {
        return pp + "." + DepositSeal.P_Status;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
