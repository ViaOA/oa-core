/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.log;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.lang.OAString;

/*qqqqqqqqqqqqqqqqqqqqqqq
CODEX

1. src/main/java/com/viaoa/log/OALogUtil.java:71 consoleOnly(Level) can silently leave logging disabled or routed to
     stale handlers.

  - Concrete bug: levelRoot is used as a global “already done” cache, but disable() and consoleOnly(Level,String) do
    not reset/update it.
  - Runtime scenario: call consoleOnly(Level.FINE), then disable(), then consoleOnly(Level.FINE) again. The second
    setup returns early because levelRoot == FINE, leaving handlers disabled.
  - Why it violates OA logging semantics: critical diagnostics can be silently lost after runtime/test
    reconfiguration.
  - Minimal fix direction: clear levelRoot in disable(), update/invalidate it in consoleOnly(Level,String), or remove
    the early-return cache unless it verifies actual handler state.
  - Suggested CODEX comment location: near levelRoot and consoleOnly(Level).

  2. src/main/java/com/viaoa/log/OALogUtil.java:111 removed handlers are not closed.

  - Concrete bug: consoleOnly(Level,String) removes existing root and named logger handlers without calling close().
  - Runtime scenario: an existing FileHandler is installed, consoleOnly is called during reconfiguration, and the file
    handler is removed but its file descriptor remains open.
  - Why it violates OA logging semantics: log files/resources can leak and buffered records might not be flushed/
    closed before replacement.
  - Minimal fix direction: flush/close handlers after removal, or explicitly document ownership transfer if callers
    must close them.
  - Suggested CODEX comment location: loops at lines 111-115 and 122-126.


*/

/**
 * Utility methods for configuring Java's built-in {@link java.util.logging}
 * system and for generating thread-dump diagnostics. The logging helpers
 * support disabling all logging, routing log output exclusively to a console
 * handler at a specified level, and enabling fine-grained performance logging
 * for OA components. These operations modify the JVM's global logging
 * configuration and apply to all loggers in the process. <p>
 *
 * The thread-dump helpers provide formatted snapshots of either the current
 * thread or all JVM threads using {@link Thread#getAllStackTraces()}, and
 * return the results as multi-line strings using {@link OAString#NL} for line
 * separation. The class is stateless aside from an internal caching of the
 * root log level and is thread-safe for normal use.
 */
public class OALogUtil {
    
	/**
	 * Caches the root logging level to avoid redundant reconfiguration.
	 */
	private static Level levelRoot;

	/**
	 * Disables all logging by turning off the root logger and its handlers.
	 */
    public static void disable() {
        Logger log = Logger.getLogger("");
        log.setLevel(Level.OFF);        
        Handler[] hs = log.getHandlers();
        for (int i=0; hs != null && i<hs.length; i++) {
            hs[i].setLevel(Level.OFF);
        }
    }

    /**
     * Configures logging to output only to the console at the specified level.
     *
     * @param level the logging level to use
     */
    public static void consoleOnly(Level level) {
        if (levelRoot != null) {
            if (levelRoot.equals(level)) return; // already done
        }
        levelRoot = level;
        consoleOnly(level, "");
    }    

    /**
     * Sends performance-related logging to the console at {@link Level#FINE}.
     */
    public static void consolePerformance() {
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.FINE);

        Logger log = Logger.getLogger("com.viaoa.object.OAPerformance");
        log.setLevel(Level.FINE);
        log.addHandler(ch);
    }    

    /**
        **** SAMPLE ****
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.FINEST);
        Logger log = Logger.getLogger("com.cpex.web.salesforce.mft");
        log.setLevel(Level.FINEST);
        log.addHandler(ch);

     */
    
    /**
     * Configures console-only logging for the specified logger name.
     *
     * @param level the logging level to use
     * @param name the logger name
     */
    public static void consoleOnly(Level level, String name) {
        Logger log = Logger.getLogger("");
        log.setLevel(Level.OFF);        

        Handler[] hs = log.getHandlers();
        for (int i=0; hs != null && i<hs.length; i++) {
            hs[i].setLevel(Level.OFF);
            log.removeHandler(hs[i]);
        }

        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(level);

        log = Logger.getLogger(name);
        log.setLevel(level);
        hs = log.getHandlers();
        for (int i=0; hs != null && i<hs.length; i++) {
            hs[i].setLevel(Level.OFF);
            log.removeHandler(hs[i]);
        }
        
        log.addHandler(ch);
    }
    
    /**
     * Returns a formatted stack trace dump of all JVM threads.
     *
     * @return a string containing the stack traces of all threads
     */
    public static String getAllThreadDump() {
        StringBuilder sb = new StringBuilder(1024 * 32);
        String s;

        Map<Thread,StackTraceElement[]> map = Thread.getAllStackTraces();
        Iterator it = map.entrySet().iterator();
        for (int i=1 ; it.hasNext(); i++) {
            Map.Entry me = (Map.Entry) it.next();
            Thread t = (Thread) me.getKey();
            s = i+") " + t.getName();
            sb.append(s + OAString.NL);
            
            StackTraceElement[] stes = (StackTraceElement[]) me.getValue();
            if (stes == null) continue;
            for (StackTraceElement ste : stes) {
                s = "  "+ste.toString(); //was:  ste.getClassName()+" "+ste.getMethodName()+" "+ste.getLineNumber();
                sb.append(s + OAString.NL);
            }
        }
        return new String(sb);
    }
    
    /**
     * Returns a formatted stack trace dump of the current thread.
     *
     * @return a string containing the current thread stack trace
     */
    public static String getThreadDump() {
        StringBuilder sb = new StringBuilder(1024 * 4);
        Thread t = Thread.currentThread();
        String s = t.getName();
        sb.append(s + OAString.NL);
        StackTraceElement[] stes = t.getStackTrace();
        if (stes != null) {
            for (StackTraceElement ste : stes) {
                s = "\tat "+ste.toString(); //was:  ste.getClassName()+" "+ste.getMethodName()+" "+ste.getLineNumber();
                sb.append(s + OAString.NL);
            }
        }
        return new String(sb);
    }

    /**
     * Returns a formatted stack trace string for the given exception.
     *
     * @param e the exception whose stack trace is returned
     * @return the formatted stack trace, or null if the exception is null
     */
    public static String getStackTrace(Exception e) {
        if (e == null) return null;
        StringBuilder sb = new StringBuilder(1024 * 2);
        Thread t = Thread.currentThread();
        String s = t.getName();
        sb.append(s + OAString.NL);
        StackTraceElement[] stes = e.getStackTrace();
        if (stes != null) {
            for (StackTraceElement ste : stes) {
                s = "\tat "+ste.toString(); //was:  ste.getClassName()+" "+ste.getMethodName()+" "+ste.getLineNumber();
                sb.append(s + OAString.NL);
            }
        }
        return new String(sb);
    }
    
}
