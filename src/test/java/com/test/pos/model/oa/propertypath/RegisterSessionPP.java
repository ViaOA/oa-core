package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class RegisterSessionPP {
    private static InvoicePPx invoices;
    private static RefundPPx refunds;
    private static RegisterPPx register;
    private static TeamMemberPPx teamMember;
    private static TillLedgerEntryPPx tillLedgerEntries;
     

    public static InvoicePPx invoices() {
        if (invoices == null) invoices = new InvoicePPx(RegisterSession.P_Invoices);
        return invoices;
    }

    public static RefundPPx refunds() {
        if (refunds == null) refunds = new RefundPPx(RegisterSession.P_Refunds);
        return refunds;
    }

    public static RegisterPPx register() {
        if (register == null) register = new RegisterPPx(RegisterSession.P_Register);
        return register;
    }

    public static TeamMemberPPx teamMember() {
        if (teamMember == null) teamMember = new TeamMemberPPx(RegisterSession.P_TeamMember);
        return teamMember;
    }

    public static TillLedgerEntryPPx tillLedgerEntries() {
        if (tillLedgerEntries == null) tillLedgerEntries = new TillLedgerEntryPPx(RegisterSession.P_TillLedgerEntries);
        return tillLedgerEntries;
    }

    public static String id() {
        String s = RegisterSession.P_Id;
        return s;
    }

    public static String created() {
        String s = RegisterSession.P_Created;
        return s;
    }

    public static String ended() {
        String s = RegisterSession.P_Ended;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
