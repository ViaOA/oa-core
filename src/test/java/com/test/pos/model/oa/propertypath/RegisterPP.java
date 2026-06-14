package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class RegisterPP {
    private static RegisterSessionPPx registerSessions;
    private static StorePPx store;
    private static TillPPx till;
     

    public static RegisterSessionPPx registerSessions() {
        if (registerSessions == null) registerSessions = new RegisterSessionPPx(Register.P_RegisterSessions);
        return registerSessions;
    }

    public static StorePPx store() {
        if (store == null) store = new StorePPx(Register.P_Store);
        return store;
    }

    public static TillPPx till() {
        if (till == null) till = new TillPPx(Register.P_Till);
        return till;
    }

    public static String id() {
        String s = Register.P_Id;
        return s;
    }

    public static String created() {
        String s = Register.P_Created;
        return s;
    }

    public static String code() {
        String s = Register.P_Code;
        return s;
    }

    public static String delete() {
        String s = Register.P_Delete;
        return s;
    }

    public static String deleteReason() {
        String s = Register.P_DeleteReason;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
