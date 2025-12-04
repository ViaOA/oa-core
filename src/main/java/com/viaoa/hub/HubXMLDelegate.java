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

import com.viaoa.object.*;
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
		write(thisHub, ow, tagName, bKeyOnly ? OAXMLWriter.WRITE_KEYONLY : OAXMLWriter.WRITE_YES, cascade);
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
        if (thisHub == null || ow == null) return;
        try {
            if (tagName != null) ow.push(tagName);
            _write(thisHub, ow, tagName, writeType, cascade);
        }
        finally {
            if (tagName != null) ow.pop();
        }
    }
	
    /**
     * Core implementation responsible for generating the XML representation of
     * the Hub. Handles indentation, tag formatting, key-only logic, and
     * per-object serialization via {@link OAObjectXMLDelegate#write}.
     * Also manages pre-scan and removal tracking for objects that will be
     * serialized to avoid redundant output.
     *
     * @param thisHub   the Hub being serialized
     * @param ow        the XML writer producing the output
     * @param tagName   wrapper tag name, or null for default <Hub> tags
     * @param writeType determines full, key-only, or conditional key-only output
     * @param cascade   cascade options governing nested serialization
     */
	private static void _write(Hub thisHub, OAXMLWriter ow, final String tagName, int writeType, OACascade cascade) {
	    boolean bKeyOnly = (writeType == OAXMLWriter.WRITE_KEYONLY || writeType == OAXMLWriter.WRITE_NONEW_KEYONLY);
	    ow.indent();
	    
        if (tagName == null) {
            ow.println("<Hub class=\""+ow.getClassName(thisHub.getObjectClass())+"\" total=\""+thisHub.getSize()+"\">");
            // ow.println("<"+ow.getClassName(Hub.class)+" ObjectClass=\""+ow.getClassName(thisHub.getObjectClass())+"\" total=\""+thisHub.getSize()+"\">");
        }
        else {
            ow.println("<"+tagName+" total=\""+thisHub.getSize()+"\">");
        }
	    
	    ow.indent++;

	    for (int i=0; ;i++) {
            Object obj = thisHub.elementAt(i);
            if (obj == null) break;
            if (obj instanceof OAObject) ow.addWillBeWriting((OAObject) obj);
	    }
	    
	    for (int i=0; ;i++) {
	        Object obj = thisHub.elementAt(i);
	        if (obj == null) break;
            if (obj instanceof OAObject) ow.removeWillBeWriting((OAObject) obj);
	        if (writeType == OAXMLWriter.WRITE_NONEW_KEYONLY && obj instanceof OAObject) {
	        	if (((OAObject) obj).getNew()) continue;
	        }
	        String name = thisHub.getObjectClass().getSimpleName();
	        if (obj instanceof OAObject) OAObjectXMLDelegate.write((OAObject)obj, ow, name, bKeyOnly, cascade);
	    }
	    ow.indent--;
	    ow.indent();
        if (tagName == null) {
            ow.println("</Hub>");
            // ow.println("</"+ow.getClassName(Hub.class)+">");
        }
        else ow.println("</"+tagName+">");
    }
	
	
}
