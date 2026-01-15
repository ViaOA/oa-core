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
package com.viaoa.comm.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.viaoa.graph.object.OAObjectPropertyService;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.Tuple;

/**
 * Customized {@link ObjectInputStream} used to deserialize OAObject streams
 * when class names or package names have changed, or when certain classes
 * no longer exist.
 *
 * <p>This stream allows legacy serialized data to be read even if:</p>
 * <ul>
 *   <li>the original package name has been renamed,</li>
 *   <li>specific class names have been replaced, or</li>
 *   <li>a class no longer exists (in which case it is mapped to {@code IODummy}).</li>
 * </ul>
 *
 * <p>When a class is replaced with {@code IODummy}, the object is created but
 * its properties are cleared via {@link OAObjectPropertyDelegate}, ensuring that
 * obsolete classes do not contaminate the deserialized object graph.</p>
 *
 * <p>This utility is vital for long-lived OA applications where models evolve,
 * packages move, or objects are removed, but existing serialized streams must
 * continue to load without failure.</p>
 */
public class OAObjectInputStream extends ObjectInputStream {

	/**
	 * Tracks whether a given class name should be replaced with {@code IODummy}
	 * when encountered in a class descriptor during deserialization.
	 */
	private Map<String, Boolean> hmReplace = new HashMap<String, Boolean>();
    
	/**
	 * Original and replacement package names used when remapping incoming
	 * serialized class names. If {@code newPackageName} is null, the old
	 * package name is reused.
	 */
	private String oldPackageName, newPackageName;
    
	/**
	 * Per-class rename mapping. Keys represent legacy class names (without the
	 * old package prefix), and values represent the new class names that should
	 * be substituted during deserialization.
	 */
	private Map<String, String> hmReplaceName = new HashMap<String, String>();

	/**
	 * Constructs a new input stream that reads serialized data from the given
	 * stream without performing any package-level remapping.
	 *
	 * @param is source input stream
	 * @throws IOException if the underlying stream cannot be read
	 */
    public OAObjectInputStream(InputStream is) throws IOException {
        this(is, null, null);
    }
    
    /**
     * Constructs an input stream that remaps classes originating from the
     * specified package into the same (old) package name. Effectively enables
     * detection and replacement of renamed classes within a single package.
     *
     * @param is source input stream
     * @param oldPackageName package name used to detect classes requiring rename
     * @throws IOException if initialization fails
     */
    public OAObjectInputStream(InputStream is, String oldPackageName) throws IOException {
        this(is, oldPackageName, oldPackageName);
    }

    /**
     * Constructs an input stream that remaps classes from an old package name
     * into a new package name. This allows legacy serialized data to be read
     * even after package restructuring.
     *
     * @param is source input stream
     * @param oldPackageName original package name to detect
     * @param newPackageName new package name to substitute; if null, the old package is reused
     * @throws IOException if initialization fails
     */
    public OAObjectInputStream(InputStream is, String oldPackageName, String newPackageName) throws IOException {
        super(is);
        enableResolveObject(true);
        this.oldPackageName = oldPackageName;
        if (newPackageName == null) newPackageName = oldPackageName;
        this.newPackageName = newPackageName;
    }
    
    /**
     * Registers a class rename mapping. When a serialized class descriptor
     * references {@code oldName} (relative to the package), it will be replaced
     * with {@code newName}.
     *
     * @param oldName original simple class name (without package prefix)
     * @param newName replacement simple class name
     */
    public void replaceClassName(String oldName, String newName) {
        hmReplaceName.put(oldName, newName);
    }
    
    /**
     * Ensures that objects deserialized as {@code IODummy} have their OAObject
     * properties cleared, preventing the presence of stale or irrelevant data.
     *
     * @param obj deserialized object
     * @return resolved object
     * @throws IOException if superclass resolution fails
     */
    @Override
    protected Object resolveObject(Object obj) throws IOException {
        obj = super.resolveObject(obj);
        if (obj instanceof IODummy) {
            final OAObjectPropertyService srvcOAObjectProperty = OARuntime.get().graph((OAObject) obj).objects().getOAObjectPropertyService();
            srvcOAObjectProperty.clearProperties((OAObject) obj);
        }
        return obj;
    }
    
    /**
     * Intercepts the reading of class descriptors to transparently remap class
     * names during deserialization.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Reads the default class descriptor.</li>
     *   <li>If {@code oldPackageName} is set and the descriptor's name begins with it:
     *       <ul>
     *         <li>Extracts the simple class name,</li>
     *         <li>Applies per-class rename mappings (if any),</li>
     *         <li>Constructs the new fully qualified name,</li>
     *         <li>Rewrites the descriptor's {@code name} field via reflection.</li>
     *       </ul>
     *   </li>
     *   <li>Checks whether the class should be replaced with {@code IODummy}.</li>
     *   <li>If so, rewrites the descriptor name to {@code IODummy}.</li>
     * </ul>
     *
     * <p>This mechanism enables backward compatibility for OA applications that
     * evolve their class structures while preserving old serialized data.</p>
     *
     * @return the possibly rewritten {@link ObjectStreamClass}
     * @throws IOException if descriptor reading fails
     * @throws ClassNotFoundException if a required class cannot be resolved
     */
    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass cd = super.readClassDescriptor();
        
        boolean bReplace = false;
        Field f = null;
        try {
            // 20200118
            String name = cd.getName();
            /* was:
            f = cd.getClass().getDeclaredField("name");
            f.setAccessible(true);
            String name = (String) f.get(cd);
            */
            String newName;
            if (oldPackageName != null) {
                if (!name.startsWith(oldPackageName)) return cd;
    
                String s = name.substring(oldPackageName.length()+1);
                newName = hmReplaceName.get(s);
                if (newName == null) newName = s;
                
                newName = newPackageName + "." + newName;

                f = cd.getClass().getDeclaredField("name");
                f.setAccessible(true);
                f.set(cd, newName);
            }
            else newName = name;
            
            Object objx = hmReplace.get(newName);
            
            if (objx == null) {
                hmReplace.put(newName, true);
                bReplace = true;
                //Class c = Class.forName(newName);
                bReplace = false;
                hmReplace.put(newName, false);
            }
            else {
                bReplace = ((Boolean) objx).booleanValue();
            }
            
        }
        catch (Exception e) {
        }
        
        if (bReplace) {
            try {
                f.set(cd, IODummy.class.getName());
            }
            catch (Exception e2) {
                // TODO: handle exception
            }
        }
        return cd;
    }


}
