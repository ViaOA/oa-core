// Copied from OATemplate project by OABuilder 07/15/25 01:11 PM
package com.test.pos.delegate.oa;

import com.test.pos.model.oa.*;
import com.viaoa.object.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.template.OATemplate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.OA;

public class ReportDelegate {

    /**
     * This is used to determine what ReportClass.reportDefs can be used for a Report.reportDef
     */
    public static ReportClass getCalcReportClass(Report report) {
        if (report == null) return null;
    	OA oa = OARuntime.oa(Report.class);
        
        ReportClass rc = null; 
        final OAObjectInfo oi = oa.info(Report.class);
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
    	OA oa = OARuntime.oa(Report.class);
        
        ReportDef rd = report.getReportDef();
        if (rd == null) return;
        
        final OATemplate ot = new OATemplate();
        String oapos = rd.getTemplate();
        ot.setTemplate(oapos);
        
        OAObject ref = null;
        final OAObjectInfo oi = oa.info(Report.class);
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
