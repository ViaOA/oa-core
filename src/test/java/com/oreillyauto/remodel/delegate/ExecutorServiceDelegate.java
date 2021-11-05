// Copied from OATemplate project by OABuilder 11/20/20 05:51 PM
package com.oreillyauto.remodel.delegate;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.viaoa.concurrent.OAExecutorService;

public class ExecutorServiceDelegate {
    private static Logger LOG = Logger.getLogger(ExecutorServiceDelegate.class.getName());
    private static final OAExecutorService executorService = new OAExecutorService(); 
    
    public static Future submit(Runnable r) {
        return getExecutorService().submit(r);
    }
    public static Future submitAndWait(Runnable r, int maxWait, TimeUnit tu) throws Exception {
        return getExecutorService().submitAndWait(r, maxWait, tu);
    }

    public static Future submit(Callable r) {
        return getExecutorService().submit(r);
    }
    public static Future submitAndWait(Callable r, int maxWait, TimeUnit tu) throws Exception {
        return getExecutorService().submitAndWait(r, maxWait, tu);
    }

    public static OAExecutorService getExecutorService() {
        return executorService;
    }
}
