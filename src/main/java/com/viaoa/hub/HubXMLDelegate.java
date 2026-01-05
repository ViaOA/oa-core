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
package com.viaoa.hub;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.xml.OAXMLWriter;

/**
 * Delegate for serializing {@link Hub} contents to XML using {@link com.viaoa.xml.OAXMLWriter}.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Write Hub contents with optional tag names and key-only modes.</li>
 *   <li>Ensure nested {@link OAObject}s are serialized with correct cascade scope.</li>
 *   <li>Maintain element indentation and tag consistency for XML output.</li>
 * </ul>
 *
 * <p>Used primarily by OA’s XML persistence layer and data-migration utilities.
 */
public class HubXMLDelegate {

	
	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubXMLService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubXMLService().?(?);
    */
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	/**
	 * Writes the contents of {@code thisHub} to XML using the supplied writer.
	 * Converts the boolean {@code bKeyOnly} flag into the appropriate writer
	 * mode and delegates to the 4-argument {@link #write(Hub, OAXMLWriter, String, int, OACascade)}
	 * method.
	 *
	 * @param thisHub  the Hub whose contents are being serialized
	 * @param ow       XML writer receiving the output
	 * @param tagName  optional tag name to wrap the Hub contents
	 * @param bKeyOnly true to write only object keys, false for full serialization
	 * @param cascade  cascade options controlling nested object serialization
	 */
	public static void write(Hub thisHub, OAXMLWriter ow, final String tagName, boolean bKeyOnly, OACascade cascade) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubXMLService().write(thisHub, ow, tagName, bKeyOnly, cascade);
	}

	/**
	 * Serializes all objects in the Hub using the given write type. Wraps the
	 * output in the provided tag name if non-null, ensuring proper push/pop
	 * behavior on the writer stack. Delegates actual content generation to
	 * the private {@link #_write(Hub, OAXMLWriter, String, int, OACascade)}.
	 *
	 * @param thisHub   the Hub whose objects are written
	 * @param ow        the XML writer
	 * @param tagName   optional outer tag name
	 * @param writeType configuration flag determining full or key-only output
	 * @param cascade   cascade settings for nested OAObject serialization
	 */
    public static void write(Hub thisHub, OAXMLWriter ow, final String tagName, int writeType, OACascade cascade) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubXMLService().write(thisHub, ow, tagName, writeType, cascade);
    }
	
}
