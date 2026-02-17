// Copied from OATemplate project by OABuilder 04/01/25 01:43 PM
package com.cdi.delegate.oa;

import com.cdi.model.oa.*;
import com.viaoa.object.*;
import com.viaoa.template.OATemplate;
import com.viaoa.util.OADateTime;

public class ReportDelegate {

    /**
     * This is used to determine what ReportClass.reportDefs can be used for a Report.reportDef
     */
    public static ReportClass getCalcReportClass(Report report) {
        if (report == null) return null;
        
        ReportClass rc = null; 
        final OAObjectInfo oi = OAObjectInfoDelegate.callInfoGetObjectInfo(Report.class);
        for (OALinkInfo li : oi.getLinkInfos()) {
            if (li.getType() != OALinkInfo.TYPE_ONE) continue;
            if (!li.getOneAndOnlyOne()) continue;
            if (li.getValue(report) != null) {
                rc = ReportClassDelegate.getReportClassToUse(li.getToClass());
                break;
            }
        }        
        return rc;
    }

    public static void generate(Report report) throws Exception {
        if (report == null) return;
        
        ReportDef rd = report.getReportDef();
        if (rd == null) return;
        
        final OATemplate ot = new OATemplate();
        String cdiScheduler = rd.getTemplate();
        ot.setTemplate(cdiScheduler);
        
        OAObject ref = null;
        final OAObjectInfo oi = OAObjectInfoDelegate.callInfoGetObjectInfo(Report.class);
        for (OALinkInfo li : oi.getLinkInfos()) {
            if (li.getType() != OALinkInfo.TYPE_ONE) continue;
            if (!li.getOneAndOnlyOne()) continue;
            ref = (OAObject) li.getValue(report);
            if (ref != null) break;
        }
        
        String html = ot.process(ref);
        report.setHtml(html);
        report.setGenerated(new OADateTime());
    }
}
