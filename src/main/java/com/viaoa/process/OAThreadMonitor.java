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
package com.viaoa.process;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sun.management.OperatingSystemMXBean;

import com.viaoa.util.OAString;

/**
 * used to monitor running threads and look for any alerts.
 * 20180410 under dev
 * @author vvia
 *
 */
public class OAThreadMonitor {
    protected final HashMap<Thread, OAThreadMonitor.ThreadInfo> hmThreadInfo = new HashMap<>();

    static class ThreadInfo {
        Thread thread;
        long tsCreated;
        StackTraceElement[] stes;
    }

    public void checkThreadDump() throws Exception {
        long tsNow = System.currentTimeMillis();
        Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
        Iterator it = map.entrySet().iterator();

        for (int i = 1; it.hasNext(); i++) {
            Map.Entry me = (Map.Entry) it.next();
            Thread t = (Thread) me.getKey();
            StackTraceElement[] stes = (StackTraceElement[]) me.getValue();

            System.out.println(t.getName());

            ThreadInfo ti = hmThreadInfo.get(t);
            if (ti == null) {
                ti = new ThreadInfo();
                hmThreadInfo.put(t, ti);
                ti.stes = stes;
                continue;
            }

            for (StackTraceElement ste : stes) {
                int xx = 4;
                xx++;
            }
        }
    }

    public static void main(String[] args) throws Exception {
/*        
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        // What % CPU load this current JVM is taking, from 0.0-1.0
        double d = osBean.getProcessCpuLoad();
        double d2 = osBean.getSystemCpuLoad();
*/        
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for ( ;;) {
                    Math.random();
                }
            }
        });
        // t.start();
        
        OAThreadMonitor tm = new OAThreadMonitor();
        for (;;) {
            tm.checkThreadDump();
            Thread.sleep(1000);
        }
    }

}
