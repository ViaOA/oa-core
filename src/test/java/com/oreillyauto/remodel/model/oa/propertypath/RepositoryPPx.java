// Generated by OABuilder
package com.oreillyauto.remodel.model.oa.propertypath;
 
import java.io.Serializable;

import com.oreillyauto.remodel.model.oa.*;
 
public class RepositoryPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public RepositoryPPx(String name) {
        this(null, name);
    }

    public RepositoryPPx(PPxInterface parent, String name) {
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

    public TablePPx mainTable() {
        TablePPx ppx = new TablePPx(this, Repository.P_MainTable);
        return ppx;
    }

    public ProjectPPx project() {
        ProjectPPx ppx = new ProjectPPx(this, Repository.P_Project);
        return ppx;
    }

    public QueryInfoPPx queryInfos() {
        QueryInfoPPx ppx = new QueryInfoPPx(this, Repository.P_QueryInfos);
        return ppx;
    }

    public String id() {
        return pp + "." + Repository.P_Id;
    }

    public String created() {
        return pp + "." + Repository.P_Created;
    }

    public String fileName() {
        return pp + "." + Repository.P_FileName;
    }

    public String packageName() {
        return pp + "." + Repository.P_PackageName;
    }

    public String seq() {
        return pp + "." + Repository.P_Seq;
    }

    public String description() {
        return pp + "." + Repository.P_Description;
    }

    public String fileExists() {
        return pp + "." + Repository.P_FileExists;
    }

    public String parse() {
        return pp + ".parse";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 