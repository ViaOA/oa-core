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

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAOne;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;

/**
 * Lightweight internal log record representing a single persistence command
 * executed against an {@link OAObject}.
 *
 * <p>Used internally by OA to record "save" and "delete" operations, mainly
 * for transaction logs or replication queues.  Each record carries the
 * command verb and a transient reference to the affected object.</p>
 *
 * <p><b>Highlights</b>:
 * <ul>
 *   <li>Defines static command constants ({@code save}, {@code delete}).</li>
 *   <li>Property-change notification for both {@code object} and
 *       {@code command} fields.</li>
 *   <li>{@link OAObjectInfo} configured as local-only, non-persistent,
 *       and excluded from caches.</li>
 * </ul>
 */
@OAClass(
    lowerName = "logRecord",
    pluralName = "logRecords",
    shortName = "lr",
    displayName = "Log Record",
    displayProperty = "command",
    noPojo = true,
    localOnly = true,
    addToCache = false,
    initialize = false,
    useDataSource = false
)
public class OALogRecord extends OAObject {
    private static final long serialVersionUID = 1L;
   
    public static final String COMMAND_SAVE = "save";
    public static final String COMMAND_DELETE = "delete";
   
    public static final String P_Command = "Command";
    public static final String P_Object = "Object";
    
    private String command;
    private transient OAObject object;

    @OAOne(
        displayName = "Object" 
    )
    public OAObject getObject() {
        if (object == null) {
            object = (OAObject) getObject(P_Object);
        }
        return object;
    }

    public void setObject(OAObject newObject) {
        OAObject old = getObject();
        fireBeforePropertyChange(P_Object, old, newObject);
        this.object = newObject;
        firePropertyChange(P_Object, old, object);
    }
    
    
    public String getCommand() {
        return command;
    }

    public void setCommand(String newCommand) {
        String old = command;
        fireBeforePropertyChange(P_Command, old, newCommand);
        this.command = newCommand;
        firePropertyChange(P_Command, old, command);
    }
}
