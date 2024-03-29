// Generated by OABuilder
package com.remodel.model.oa.propertypath;
 
import com.remodel.model.oa.*;
 
public class ProjectPP {
    private static DatabasePPx databases;
    private static RepositoryPPx repositories;
     

    public static DatabasePPx databases() {
        if (databases == null) databases = new DatabasePPx(Project.P_Databases);
        return databases;
    }

    public static RepositoryPPx repositories() {
        if (repositories == null) repositories = new RepositoryPPx(Project.P_Repositories);
        return repositories;
    }

    public static String id() {
        String s = Project.P_Id;
        return s;
    }

    public static String created() {
        String s = Project.P_Created;
        return s;
    }

    public static String name() {
        String s = Project.P_Name;
        return s;
    }

    public static String seq() {
        String s = Project.P_Seq;
        return s;
    }

    public static String codeDirectory() {
        String s = Project.P_CodeDirectory;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
