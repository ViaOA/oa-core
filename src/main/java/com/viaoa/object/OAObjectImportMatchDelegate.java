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
package com.viaoa.object;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.object.OAObjectImportMatchService.ImportMatch;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;


/**
 * Resolves or creates {@link OAObject} instances during JSON or POJO import
 * when a primary key is not available, using declared "import match" rules.
 *
 * <p>Many generated model classes define one or more properties or links
 * as <i>import matches</i>—fields uniquely identifying an object within a
 * domain or hierarchy.  This delegate examines those match definitions to
 * locate the correct target object in cache or data source; if none exists,
 * it automatically constructs the object and any required link hierarchy.</p>
 *
 * <p><b>Core Responsibilities</b>:
 * <ul>
 *   <li>Combine multiple import-match properties and link paths into a single query.</li>
 *   <li>Traverse equal-property and owner-link rules recursively.</li>
 *   <li>Leverage {@link OASelect}, {@link OAFinder}, and {@link OAObjectCacheDelegate}
 *       for lookup before creating new objects via reflection.</li>
 *   <li>Maintain referential integrity between the source and newly created target objects.</li>
 * </ul>
 *
 * <p>This mechanism allows OA to reconstruct a full object graph from lightweight
 * JSON or external data that omits primary keys, providing “identity by content.”</p>
 */
public class OAObjectImportMatchDelegate {

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectHubService().??(oaObj);
    */
	
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		// if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	/**
	 * Resolves or creates the target object defined by the supplied
	 * {@link ImportMatch}. Performs validation, evaluates match
	 * properties, builds a query, and searches for an existing object
	 * via {@link OASelect}, {@link OAFinder}, or
	 * {@link OAObjectCacheDelegate}. If no match is found, constructs
	 * a new object and initializes required hierarchy and owner links.
	 *
	 * @param importMatch definition of the source object, link info,
	 *        and match property values used to locate or create the
	 *        target object.
	 */
	public static void process(final ImportMatch importMatch) {
		if (importMatch == null) return;
		OAGraph g = getGraph(null, importMatch.fromObject);
		if (g == null) return;
		g.objects().getOAObjectImportMatchService().process(importMatch);
		
	}


}
