package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class RefundPP {
    private static RefundInvoicePPx refundInvoices;
    private static RegisterSessionPPx registerSession;
     

    public static RefundInvoicePPx refundInvoices() {
        if (refundInvoices == null) refundInvoices = new RefundInvoicePPx(Refund.P_RefundInvoices);
        return refundInvoices;
    }

    public static RegisterSessionPPx registerSession() {
        if (registerSession == null) registerSession = new RegisterSessionPPx(Refund.P_RegisterSession);
        return registerSession;
    }

    public static String id() {
        String s = Refund.P_Id;
        return s;
    }

    public static String created() {
        String s = Refund.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
