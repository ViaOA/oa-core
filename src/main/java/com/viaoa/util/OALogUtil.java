/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.util;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OALogUtil {
    private static Level levelRoot;

    public static void disable() {
        Logger log = Logger.getLogger("");
        log.setLevel(Level.OFF);        
        Handler[] hs = log.getHandlers();
        for (int i=0; hs != null && i<hs.length; i++) {
            hs[i].setLevel(Level.OFF);
        }
    }
    public static void consoleOnly(Level level) {
        if (levelRoot != null) {
            if (levelRoot.equals(level)) return; // already done
        }
        levelRoot = level;
        consoleOnly(level, "");
    }    

    /**
     * Send OAPerformance logging to console.
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
        log.addHandler(ch);
    }
    
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
    
    public static String getThreadDump() {
        StringBuilder sb = new StringBuilder(1024 * 4);
        Thread t = Thread.currentThread();
        String s = t.getName();
        sb.append(s + OAString.NL);
        StackTraceElement[] stes = t.getStackTrace();
        if (stes != null) {
            for (StackTraceElement ste : stes) {
                s = "  "+ste.toString(); //was:  ste.getClassName()+" "+ste.getMethodName()+" "+ste.getLineNumber();
                sb.append(s + OAString.NL);
            }
        }
        return new String(sb);
    }
}
