// Generated by OABuilder
package test.xice.tsac.model.oa.propertypath;
 
import java.io.Serializable;

import test.xice.tsac.model.oa.*;
 
public class EnvironmentPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public EnvironmentPPx(String name) {
        this(null, name);
    }

    public EnvironmentPPx(PPxInterface parent, String name) {
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

    public CompanyPPx companies() {
        CompanyPPx ppx = new CompanyPPx(this, Environment.P_Companies);
        return ppx;
    }

    public EnvironmentTypePPx environmentType() {
        EnvironmentTypePPx ppx = new EnvironmentTypePPx(this, Environment.P_EnvironmentType);
        return ppx;
    }

    public IDLPPx idL() {
        IDLPPx ppx = new IDLPPx(this, Environment.P_IDL);
        return ppx;
    }

    public MarketTypePPx marketTypes() {
        MarketTypePPx ppx = new MarketTypePPx(this, Environment.P_MarketTypes);
        return ppx;
    }

    public MRADServerPPx mradServer() {
        MRADServerPPx ppx = new MRADServerPPx(this, Environment.P_MRADServer);
        return ppx;
    }

    public RCDeployPPx rcDeploy() {
        RCDeployPPx ppx = new RCDeployPPx(this, Environment.P_RCDeploy);
        return ppx;
    }

    public RCInstalledVersionPPx rcInstalledVersions() {
        RCInstalledVersionPPx ppx = new RCInstalledVersionPPx(this, Environment.P_RCInstalledVersions);
        return ppx;
    }

    public RCPackageListPPx rcPackageLists() {
        RCPackageListPPx ppx = new RCPackageListPPx(this, Environment.P_RCPackageLists);
        return ppx;
    }

    public RCRepoVersionPPx rcRepoVersions() {
        RCRepoVersionPPx ppx = new RCRepoVersionPPx(this, Environment.P_RCRepoVersions);
        return ppx;
    }

    public RCServerListPPx rcServerLists() {
        RCServerListPPx ppx = new RCServerListPPx(this, Environment.P_RCServerLists);
        return ppx;
    }

    public RCServiceListPPx rcServiceLists() {
        RCServiceListPPx ppx = new RCServiceListPPx(this, Environment.P_RCServiceLists);
        return ppx;
    }

    public SiloPPx silos() {
        SiloPPx ppx = new SiloPPx(this, Environment.P_Silos);
        return ppx;
    }

    public SitePPx site() {
        SitePPx ppx = new SitePPx(this, Environment.P_Site);
        return ppx;
    }

    public String id() {
        return pp + "." + Environment.P_Id;
    }

    public String name() {
        return pp + "." + Environment.P_Name;
    }

    public String abbrevName() {
        return pp + "." + Environment.P_AbbrevName;
    }

    public String teAbbrevName() {
        return pp + "." + Environment.P_TEAbbrevName;
    }

    public String usesDNS() {
        return pp + "." + Environment.P_UsesDNS;
    }

    public String usesFirewall() {
        return pp + "." + Environment.P_UsesFirewall;
    }

    public String usesVip() {
        return pp + "." + Environment.P_UsesVip;
    }

    public String envImport() {
        return pp + ".envImport";
    }

    public String envExport() {
        return pp + ".envExport";
    }

    public String mradminImport() {
        return pp + ".mradminImport";
    }

    @Override
    public String toString() {
        return pp;
    }
}
 
