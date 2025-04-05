// Copied from OATemplate project by OABuilder 04/01/25 01:43 PM
package com.cdi.delegate.oa;

import com.cdi.model.oa.*;

public class ReportDefDelegate {

    public static Class getTemplateTemplateRoot(ReportDef reportDef) {
        if (reportDef == null) return null;
        
        ReportClass rc = reportDef.getReportClass();
        if (rc == null) return null;
        
        Class cz = ReportClassDelegate.getClassToUse(rc);
        return cz;
    }
    
}
