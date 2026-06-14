package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class BankDepositCheckPP {
    private static BankDepositPPx bankDeposit;
    private static InvoicePaymentPPx invoicePaymentCheck;
     

    public static BankDepositPPx bankDeposit() {
        if (bankDeposit == null) bankDeposit = new BankDepositPPx(BankDepositCheck.P_BankDeposit);
        return bankDeposit;
    }

    public static InvoicePaymentPPx invoicePaymentCheck() {
        if (invoicePaymentCheck == null) invoicePaymentCheck = new InvoicePaymentPPx(BankDepositCheck.P_InvoicePaymentCheck);
        return invoicePaymentCheck;
    }

    public static String id() {
        String s = BankDepositCheck.P_Id;
        return s;
    }

    public static String created() {
        String s = BankDepositCheck.P_Created;
        return s;
    }

    public static String cleared() {
        String s = BankDepositCheck.P_Cleared;
        return s;
    }

    public static String rejected() {
        String s = BankDepositCheck.P_Rejected;
        return s;
    }

    public static String rejectedFeeCollected() {
        String s = BankDepositCheck.P_RejectedFeeCollected;
        return s;
    }

    public static String feeAmountCollected() {
        String s = BankDepositCheck.P_FeeAmountCollected;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
