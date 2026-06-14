package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class BankDepositPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public BankDepositPPx(String name) {
        this(null, name);
    }

    public BankDepositPPx(PPxInterface parent, String name) {
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

    public BankDepositCheckPPx bankDepositChecks() {
        BankDepositCheckPPx ppx = new BankDepositCheckPPx(this, BankDeposit.P_BankDepositChecks);
        return ppx;
    }

    public DepositSealPPx depositSeal() {
        DepositSealPPx ppx = new DepositSealPPx(this, BankDeposit.P_DepositSeal);
        return ppx;
    }

    public StoreSafePPx storeSafe() {
        StoreSafePPx ppx = new StoreSafePPx(this, BankDeposit.P_StoreSafe);
        return ppx;
    }

    public String id() {
        return pp + "." + BankDeposit.P_Id;
    }

    public String created() {
        return pp + "." + BankDeposit.P_Created;
    }

    public String cash() {
        return pp + "." + BankDeposit.P_Cash;
    }

    public String referenceCode() {
        return pp + "." + BankDeposit.P_ReferenceCode;
    }

    public String confirmed() {
        return pp + "." + BankDeposit.P_Confirmed;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
