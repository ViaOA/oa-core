// Copied from OATemplate project by OABuilder 07/15/25 01:11 PM
package com.test.pos.delegate.oa;

import com.test.pos.model.oa.*;

public class ReportDefDelegate {

    public static Class getTemplateTemplateRoot(ReportDef reportDef) {
        if (reportDef == null) return null;
        
        ReportClass rc = reportDef.getReportClass();
        if (rc == null) return null;
        
        Class cz = ReportClassDelegate.getClassToUse(rc);
        return cz;
    }
    
}
