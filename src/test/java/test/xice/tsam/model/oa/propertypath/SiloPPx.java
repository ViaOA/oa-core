// Generated by OABuilder
package test.xice.tsam.model.oa.propertypath;
 
import java.io.Serializable;

import test.xice.tsam.model.oa.Silo;
import test.xice.tsam.model.oa.propertypath.ApplicationGroupPPx;
import test.xice.tsam.model.oa.propertypath.EnvironmentPPx;
import test.xice.tsam.model.oa.propertypath.MRADServerPPx;
import test.xice.tsam.model.oa.propertypath.PPxInterface;
import test.xice.tsam.model.oa.propertypath.ServerPPx;
import test.xice.tsam.model.oa.propertypath.SiloConfigPPx;
import test.xice.tsam.model.oa.propertypath.SiloTypePPx;

import test.xice.tsam.model.oa.*;
 
public class SiloPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public SiloPPx(String name) {
        this(null, name);
    }

    public SiloPPx(PPxInterface parent, String name) {
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

    public ApplicationGroupPPx applicationGroups() {
        ApplicationGroupPPx ppx = new ApplicationGroupPPx(this, Silo.P_ApplicationGroups);
        return ppx;
    }

    public EnvironmentPPx environment() {
        EnvironmentPPx ppx = new EnvironmentPPx(this, Silo.P_Environment);
        return ppx;
    }

    public MRADServerPPx mradServer() {
        MRADServerPPx ppx = new MRADServerPPx(this, Silo.P_MRADServer);
        return ppx;
    }

    public ServerPPx servers() {
        ServerPPx ppx = new ServerPPx(this, Silo.P_Servers);
        return ppx;
    }

    public SiloConfigPPx siloConfigs() {
        SiloConfigPPx ppx = new SiloConfigPPx(this, Silo.P_SiloConfigs);
        return ppx;
    }

    public SiloTypePPx siloType() {
        SiloTypePPx ppx = new SiloTypePPx(this, Silo.P_SiloType);
        return ppx;
    }

    public String id() {
        return pp + "." + Silo.P_Id;
    }

    public String networkMask() {
        return pp + "." + Silo.P_NetworkMask;
    }

    public String currentTime() {
        return pp + "." + Silo.P_CurrentTime;
    }

    public String schedulerMessage() {
        return pp + "." + Silo.P_SchedulerMessage;
    }

    @Override
    public String toString() {
        return pp;
    }
}
 
