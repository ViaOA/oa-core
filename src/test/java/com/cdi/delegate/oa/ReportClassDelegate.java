// Copied from OATemplate project by OABuilder 04/01/25 01:43 PM
package com.cdi.delegate.oa;

import java.util.*;

import com.cdi.delegate.ModelDelegate;
import com.cdi.model.oa.*;
import com.viaoa.hub.Hub;
import com.viaoa.object.*;

public class ReportClassDelegate {

    private static final Map<ReportClass, Class> hmReportClass = new HashMap<>();
    private static final Map<Class, ReportClass> hmClassReport = new HashMap<>();
    
    public static void createReportClasses() {
        final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(Report.class);
        
        final Hub<ReportClass> hub = ModelDelegate.getReportClasses();
        
        for (OALinkInfo li : oi.getLinkInfos()) {
            if (li.getType() != OALinkInfo.TYPE_ONE) continue;
            if (!li.getOneAndOnlyOne()) continue;
            
            Class cz = li.getToClass();
            if (cz == null) continue;
            String cn = cz.getSimpleName();
            
            ReportClass rc = hub.find(ReportClass.P_ClassName, cn);
            if (rc == null) {
                OAObjectInfo oiz = OAObjectInfoDelegate.getOAObjectInfo(cz);
                rc = new ReportClass();
                rc.setName(oiz.getDisplayName());
                rc.setClassName(cn);
                hub.add(rc);
            }
            hmReportClass.put(rc, cz);
            hmClassReport.put(cz, rc);
        }
    }

    public static ReportClass getReportClassToUse(Class cz) {
        if (cz == null) return null;
        if (hmReportClass.size() == 0) {
            createReportClasses();
        }
        ReportClass rc = hmClassReport.get(cz);
        return rc;
    }
    
    public static Class getClassToUse(ReportClass rc) {
        if (rc == null) return null;
        if (hmReportClass.size() == 0) {
            createReportClasses();
        }
        Class cz = hmReportClass.get(rc);
        return cz;
    }
    
}
