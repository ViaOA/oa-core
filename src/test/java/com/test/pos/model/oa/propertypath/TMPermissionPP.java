package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class TMPermissionPP {
    private static TeamMemberPPx teamMembers;
     

    public static TeamMemberPPx teamMembers() {
        if (teamMembers == null) teamMembers = new TeamMemberPPx(TMPermission.P_TeamMembers);
        return teamMembers;
    }

    public static String id() {
        String s = TMPermission.P_Id;
        return s;
    }

    public static String created() {
        String s = TMPermission.P_Created;
        return s;
    }

    public static String type() {
        String s = TMPermission.P_Type;
        return s;
    }

    public static String name() {
        String s = TMPermission.P_Name;
        return s;
    }

    public static String description() {
        String s = TMPermission.P_Description;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
