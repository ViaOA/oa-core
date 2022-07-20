// Copied from OATemplate project by OABuilder 11/20/20 05:51 PM
package com.remodel.delegate;

import java.util.logging.Logger;

import com.viaoa.process.*;

public class CronDelegate {
    private static Logger LOG = Logger.getLogger(CronDelegate.class.getName());
    private static OACronProcessor cronProcessor; 
    
    public static void add(OACron cron) {
        if (cron == null) return;
        LOG.fine("add cron="+cron.getDescription());
        if (cronProcessor == null) {
            LOG.warning("cronProcess has not been created.");
            return;
        }
        cronProcessor.add(cron);
    }
    public static void remove(OACron cron) {
        if (cron == null) return;
        LOG.fine("remove cron="+cron.getDescription());
        if (cronProcessor == null) return;
        cronProcessor.remove(cron);
    }
    
    
    public static OACronProcessor getCronProcessor() {
        return cronProcessor;
    }
    public static void setCronProcessor(OACronProcessor cp) {
        LOG.fine("setting cronProcessor="+cp);
        cronProcessor = cp;;
    }
}
