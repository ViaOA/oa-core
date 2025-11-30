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

import java.util.logging.*;

//import sun.util.LocaleServiceProviderPool.LocalizedObjectGetter;

import com.viaoa.hub.Hub;
import com.viaoa.util.*;
import com.viaoa.xml.OAXMLReader;
import com.viaoa.xml.OAXMLWriter;

/**
 * Provides XML-based transaction logging for {@link OAObject} persistence
 * operations.  When enabled, each {@code save()} or {@code delete()} is
 * written as an {@link OALogRecord} to an XML file for later replay.
 *
 * <p>This mechanism supports auditing, replication, and offline synchronization
 * by allowing a system to reproduce changes without requiring a direct
 * connection to the data source.</p>
 *
 * <p><b>Key Behaviors</b>:
 * <ul>
 *   <li>{@link #createXMLLogFile(String)} — opens or switches the active log file.</li>
 *   <li>{@link #logToXmlFile(OAObject, boolean)} — writes a SAVE or DELETE record.</li>
 *   <li>{@link #restoreXMLLogFile(String)} — replays the log, invoking
 *       {@code save()} or {@code delete()} on each restored object.</li>
 *   <li>Special M2M handling to avoid premature creation of link objects.</li>
 * </ul>
 *
 * <p>The delegate uses an embedded {@link OAXMLWriter} instance with custom
 * property-writing rules to ensure that objects are written in key-only form
 * when appropriate, avoiding duplication during replay.</p>
 */
public class OAObjectLogDelegate {
    private static Logger LOG = Logger.getLogger(OAObjectLogDelegate.class.getName());
    private static volatile OAXMLWriter writerXml;

    /**
     * Opens a new XML log file for recording {@link OALogRecord} entries.
     * <p>
     * If an existing log is active, it is first closed and cleared. When a
     * non-null filename is supplied, a new {@link OAXMLWriter} instance is
     * created with custom property-handling rules:
     * </p>
     * <ul>
     *   <li>{@link OALogRecord} instances are always written.</li>
     *   <li>{@link OAObject} values are written in key-only form.</li>
     *   <li>Non-{@link Hub} values are written normally.</li>
     *   <li>For many-to-many links, the writer suppresses new object creation
     *       and writes key-only entries to avoid premature M2M construction
     *       during restore.</li>
     * </ul>
     *
     * @param fname the file name of the XML log to create, or {@code null}
     *              to close the current log
     */
    public static void createXMLLogFile(String fname) {
        if (writerXml != null) {
            writerXml.close();
            writerXml = null;
        }
        if (fname != null) {
            fname = OAString.convertFileName(fname);
            writerXml = new OAXMLWriter(fname) {
                public int writeProperty(Object obj, String propertyName, Object value) {
                    if (obj instanceof OALogRecord) return OAXMLWriter.WRITE_YES;
                    
                    if (value instanceof OAObject) return OAXMLWriter.WRITE_KEYONLY;
                    if (!(value instanceof Hub)) return OAXMLWriter.WRITE_YES;
                    
                    OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj.getClass());
                    OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, propertyName);
                    if (li != null && li.getType() == OALinkInfo.MANY) {
                        li = OAObjectInfoDelegate.getLinkInfo(oi, propertyName);
                        li = OAObjectInfoDelegate.getReverseLinkInfo(li);
                        if (li != null && li.getType() == OALinkInfo.MANY) {
                            // M2M dont write any new object, since it does not exist when this file is restored.
                            //        the restore will update/complete the M2M link tables when the other object
                            //        has it's M2M updated/loaded.
                            return OAXMLWriter.WRITE_NONEW_KEYONLY;
                        }
                    }
                    return OAXMLWriter.WRITE_NO;
                }           
            };
        }
    }
    
    /**
     * Closes the current XML log file, if any. This is equivalent to
     * invoking {@link #createXMLLogFile(String)} with a {@code null}
     * argument.
     */
    public static void closeXMLLogFile() {
        createXMLLogFile(null);
    }
    
    /**
     * Writes a SAVE or DELETE {@link OALogRecord} for the specified object
     * to the active XML log file. If no log file is open, the call is
     * ignored.
     * <p>
     * A new {@link OALogRecord} is created and populated with the object
     * reference and command type. The record is written to the underlying
     * {@link OAXMLWriter} within a synchronized block to ensure thread-safe
     * output, followed by a flush.
     * </p>
     *
     * @param oaObj the object being logged
     * @param bSave true to log a SAVE command, false to log a DELETE
     */
    protected static void logToXmlFile(OAObject oaObj, boolean bSave) {
        if (writerXml == null) return;
        OALogRecord rec = new OALogRecord();
        rec.setObject(oaObj);
        rec.setCommand(bSave ? OALogRecord.COMMAND_SAVE : OALogRecord.COMMAND_DELETE);
        synchronized (writerXml) {
            writerXml.write(rec);
            writerXml.flush();
        }
    }
    
    /**
     * Restores and replays all {@link OALogRecord} entries from the
     * specified XML log file.
     * <p>
     * A customized {@link OAXMLReader} is used to intercept completed
     * {@link OALogRecord} objects during parsing. For each record:
     * </p>
     * <ul>
     *   <li>If the command is SAVE, {@code save(OAObject.CASCADE_NONE)} is
     *       invoked on the underlying object.</li>
     *   <li>If the command is DELETE, {@code delete()} is invoked.</li>
     * </ul>
     * <p>
     * If {@code fname} is {@code null}, the operation is ignored.
     * </p>
     *
     * @param fname the filename of the XML log to restore
     * @throws Exception if an error occurs during XML reading
     */
    public static void restoreXMLLogFile(String fname) throws Exception {
        if(fname == null) return;
        fname = OAString.convertFileName(fname);
        OAXMLReader reader = new OAXMLReader() {
            public void endObject(OAObject obj, boolean bHasParent) {
                if (!(obj instanceof OALogRecord)) return;
                OALogRecord lr = (OALogRecord) obj;
                if (lr.getCommand().equals(OALogRecord.COMMAND_SAVE)) {
                    lr.getObject().save(OAObject.CASCADE_NONE);
                }
                else lr.getObject().delete();
            }
        };
        try {
            // OAObjectFlagDelegate.setThreadIgnoreEvents(true);
            reader.readFile(fname);
        }
        finally {
            // OAObjectFlagDelegate.setThreadIgnoreEvents(false);
        }
    }
    
}
