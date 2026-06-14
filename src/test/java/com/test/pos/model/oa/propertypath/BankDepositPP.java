package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class BankDepositPP {
    private static BankDepositCheckPPx bankDepositChecks;
    private static DepositSealPPx depositSeal;
    private static StoreSafePPx storeSafe;
     

    public static BankDepositCheckPPx bankDepositChecks() {
        if (bankDepositChecks == null) bankDepositChecks = new BankDepositCheckPPx(BankDeposit.P_BankDepositChecks);
        return bankDepositChecks;
    }

    public static DepositSealPPx depositSeal() {
        if (depositSeal == null) depositSeal = new DepositSealPPx(BankDeposit.P_DepositSeal);
        return depositSeal;
    }

    public static StoreSafePPx storeSafe() {
        if (storeSafe == null) storeSafe = new StoreSafePPx(BankDeposit.P_StoreSafe);
        return storeSafe;
    }

    public static String id() {
        String s = BankDeposit.P_Id;
        return s;
    }

    public static String created() {
        String s = BankDeposit.P_Created;
        return s;
    }

    public static String cash() {
        String s = BankDeposit.P_Cash;
        return s;
    }

    public static String referenceCode() {
        String s = BankDeposit.P_ReferenceCode;
        return s;
    }

    public static String confirmed() {
        String s = BankDeposit.P_Confirmed;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
