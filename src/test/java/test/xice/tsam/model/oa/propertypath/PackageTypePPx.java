// Generated by OABuilder
package test.xice.tsam.model.oa.propertypath;
 
import java.io.Serializable;

import test.xice.tsam.model.oa.PackageType;
import test.xice.tsam.model.oa.propertypath.ApplicationTypePPx;
import test.xice.tsam.model.oa.propertypath.ApplicationVersionPPx;
import test.xice.tsam.model.oa.propertypath.PPxInterface;
import test.xice.tsam.model.oa.propertypath.PackageVersionPPx;
import test.xice.tsam.model.oa.propertypath.SiloConfigVersioinPPx;

import test.xice.tsam.model.oa.*;
 
public class PackageTypePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public PackageTypePPx(String name) {
        this(null, name);
    }

    public PackageTypePPx(PPxInterface parent, String name) {
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

    public ApplicationTypePPx applicationTypes() {
        ApplicationTypePPx ppx = new ApplicationTypePPx(this, PackageType.P_ApplicationTypes);
        return ppx;
    }

    public ApplicationVersionPPx applicationVersions() {
        ApplicationVersionPPx ppx = new ApplicationVersionPPx(this, PackageType.P_ApplicationVersions);
        return ppx;
    }

    public PackageVersionPPx packageVersions() {
        PackageVersionPPx ppx = new PackageVersionPPx(this, PackageType.P_PackageVersions);
        return ppx;
    }

    public SiloConfigVersioinPPx siloConfigVersioins() {
        SiloConfigVersioinPPx ppx = new SiloConfigVersioinPPx(this, PackageType.P_SiloConfigVersioins);
        return ppx;
    }

    public String id() {
        return pp + "." + PackageType.P_Id;
    }

    public String code() {
        return pp + "." + PackageType.P_Code;
    }

    public String packageName() {
        return pp + "." + PackageType.P_PackageName;
    }

    public String pomGroupId() {
        return pp + "." + PackageType.P_PomGroupId;
    }

    public String pomArtifactId() {
        return pp + "." + PackageType.P_PomArtifactId;
    }

    @Override
    public String toString() {
        return pp;
    }
}
 
