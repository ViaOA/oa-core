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
package com.viaoa.analysis;

import java.util.HashSet;

import com.viaoa.callback.OACallback;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

/*qqqqqqqqqqqqqqqqqqqqqq
CODEX

1. src/main/java/com/viaoa/analysis/OAObjectAnalyzer.java / OAObjectAnalyzer.load

  Concrete bug: load() only enumerates classes from the default graph, so it silently omits cached objects that live
  only in non-default/package graphs.

  Runtime scenario: OA has a default graph plus a package-specific graph registered through OARuntime.createGraph(...)
  or package routing. Objects are cached in the package graph, but their classes are not present in the default graph
  cache. load() does:

  - line 62: OARuntime.graph() to get only the default graph
  - line 64: ogx.objectsInternal().callObjectCacheGetClasses()
  - line 65: resolves each class back to its routed graph

  Classes that exist only in non-default graph caches are never visited, and the analysis completes without saying it
  was partial.

  Why this violates OA/OG analysis semantics: the class documentation says it scans all cached OAObject instances. In
  OG/OA 4.0, graph ownership and package routing are semantic boundaries. A diagnostic analysis that silently skips
  non-default graph caches can produce false-negative architecture/cache reports and misleading tooling guidance.

  Minimal fix direction: either make OAObjectAnalyzer graph-scoped and document/require the target OAGraph, or
  enumerate all registered runtime graphs and scan each graph’s object cache directly. Do not claim global
  completeness unless all relevant graphs were visited.

  Suggested CODEX comment location: immediately before line 62, where OARuntime.graph() fixes the scan to the default
  graph.

  Suggested regression test: OAObjectAnalyzerScansNonDefaultPackageGraphCache.

  2. src/main/java/com/viaoa/analysis/OAObjectAnalyzer.java / OAObjectAnalyzer.hsHub and load

  Concrete bug: analysis result state is retained across runs because hsHub is an instance field and load() never
  clears it.

  Runtime scenario: create one OAObjectAnalyzer, call load(), mutate/remove Hub memberships or clear parts of the
  cache, then call load() again. hsHub still contains Hubs discovered during the previous run. Since the field holds
  strong references, it can also retain old Hub graphs longer than intended during repeated diagnostic analysis.

  Why this violates OA/OG analysis semantics: analysis state should describe the current scan. Reusing stale Hub state
  causes false positives if the field is inspected/debugged or later exposed, and it creates a diagnostic-time
  retention leak. Analysis temporary state should be isolated per run.

  Minimal fix direction: clear hsHub at the start of load(), or make it a local variable returned/reported by the
  scan. If cross-run accumulation is intentional, rename/document it explicitly.

  Suggested CODEX comment location: line 48 field declaration or at the start of load() before line 62.

  Suggested regression test: OAObjectAnalyzerLoadDoesNotRetainHubResultsAcrossRuns.


*/

/**
 * Diagnostic utility that traverses all cached {@link OAObject}s and
 * analyzes their {@link com.viaoa.hub.Hub} memberships.
 *
 * <p>Primarily used for debugging or memory-analysis scenarios to identify
 * objects participating in excessive Hub references or cyclic graphs.</p>
 *
 * <p><b>Functions</b>:
 * <ul>
 *   <li>Iterates over all registered classes in
 *       {@link OAObjectCacheDelegate#getClasses()}.</li>
 *   <li>For each object, collects the set of all Hubs referencing it using
 *       {@link OAObjectHubDelegate#callHubGetHubReferences(OAObject)}.</li>
 *   <li>Prints summary output for objects associated with many Hubs.</li>
 * </ul>
 */
public class OAObjectAnalyzer {

    
    HashSet<Hub> hsHub = new HashSet<Hub>();

    
    /**
     * Scans all cached {@link OAObject} instances and reports their
     * {@link com.viaoa.hub.Hub} memberships for diagnostic analysis.
     *
     * <p>The method iterates through all classes registered in the object
     * cache, invoking a callback for each object to count and record the
     * hubs referencing it. Objects associated with more than ten hubs are
     * printed to standard output. A running set of all discovered hubs is
     * maintained for summary inspection.</p>
     */
    public void load() {
		OAGraphInternal ogx = (OAGraphInternal) OARuntime.graph();
    	
        for (Class cs : ogx.objectsInternal().callObjectCacheGetClasses()) {
        	OAGraphInternal og = (OAGraphInternal) OARuntime.graph(cs);

    		System.out.println("Starting class="+cs.getSimpleName()+", total="+og.objectsInternal().callObjectCacheGetTotal(cs));
            
            OACallback cb = new OACallback() {
                @Override
                public boolean updateObject(Object object) {
                    OAObject obj = (OAObject) object;
                    Hub[] hubs = og.objectsInternal().callObjectHubGetHubReferences(obj);
                    if (hubs == null) return true;
                    int cnt = 0;
                    for (Hub h : hubs) {
                        if (h == null) continue;
                        cnt++;
                        hsHub.add(h);
                    }
                    if (cnt > 10) {
                        System.out.println("   guid="+obj.getObjectKey().getGuid()+", cntHubs="+cnt);
                    }
                    return true;
                }
            };
            og.objectsInternal().callObjectCacheCallback(cs, cb);
        }    
        int xx = hsHub.size();
        xx++;
    }

    
    
    
}


