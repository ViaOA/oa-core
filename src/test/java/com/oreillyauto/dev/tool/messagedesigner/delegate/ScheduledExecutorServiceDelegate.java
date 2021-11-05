// Copied from OATemplate project by OABuilder 06/04/21 04:05 PM
package com.oreillyauto.dev.tool.messagedesigner.delegate;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.concurrent.OAScheduledExecutorService;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

public class ScheduledExecutorServiceDelegate {
    private static Logger LOG = Logger.getLogger(ScheduledExecutorServiceDelegate.class.getName());
    private static OAScheduledExecutorService scheduledExecutorService = new OAScheduledExecutorService();

    
    public static Future<?> schedule(Runnable r, OADateTime dt) throws Exception {
        return getScheduledExecutorService().schedule(r, dt);
    }
    public static Future<?> schedule(Runnable r, int delay, TimeUnit tu) throws Exception {
        return getScheduledExecutorService().schedule(r, delay, tu);
    }

    
    public static Future<?> schedule(Callable<?> c, int delay, TimeUnit tu) throws Exception {
        return getScheduledExecutorService().schedule(c, delay, tu);
    }

    public static Future<?> scheduleEvery(Runnable r, OATime time) throws Exception {
        return getScheduledExecutorService().scheduleEvery(r, time);
    }
    
    public static Future<?> scheduleEvery(Runnable r, int initialDelay, int period, TimeUnit tu) throws Exception {
        return getScheduledExecutorService().scheduleEvery(r, period, initialDelay, tu);
    }
    
    
    public static OAScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }
}
