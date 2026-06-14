package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class DepositSealPP {
    private static BankDepositPPx bankDeposit;
     

    public static BankDepositPPx bankDeposit() {
        if (bankDeposit == null) bankDeposit = new BankDepositPPx(DepositSeal.P_BankDeposit);
        return bankDeposit;
    }

    public static String id() {
        String s = DepositSeal.P_Id;
        return s;
    }

    public static String created() {
        String s = DepositSeal.P_Created;
        return s;
    }

    public static String sealNumber() {
        String s = DepositSeal.P_SealNumber;
        return s;
    }

    public static String issuedTo() {
        String s = DepositSeal.P_IssuedTo;
        return s;
    }

    public static String usedOn() {
        String s = DepositSeal.P_UsedOn;
        return s;
    }

    public static String status() {
        String s = DepositSeal.P_Status;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
