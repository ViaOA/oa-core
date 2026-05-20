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
package com.viaoa.config;


import java.util.*;

import com.viaoa.converter.OAConv;
import com.viaoa.converter.OAConverter;
import com.viaoa.io.OAFile;
import com.viaoa.lang.OAString;

import java.io.*;


/*qqqqqqqqqqqqqqqqqqqqqqq
CODEX

1. src/main/java/com/viaoa/config/OAProperties.java / load(String fileName)

  Concrete bug: loading a missing file silently succeeds and leaves the properties empty/stale.

  Runtime scenario: production code calls new OAProperties("server.properties") or props.load("datasource.properties")
  with a misspelled, missing, or deployment-wrong path. Line 149 checks if (!file.exists()) return;, so the load
  operation completes without any caller-visible failure.

  Why this violates OA/OG config semantics: configuration loading must resolve the intended source. A missing config
  file can silently fall through to defaults or existing in-memory values, causing wrong datasource, runtime mode,
  sync/remote/replication, or path settings while appearing successfully configured.

  Minimal fix direction: define the contract explicitly. Either add a strict load method that throws on missing file
  and use it for required config, or make load(String) report/record missing-source state. At minimum, do not let
  required config loads silently no-op.

  Suggested CODEX comment location: line 149, before if (!file.exists()) return;.

  Suggested regression test: testLoadMissingRequiredConfigFailsVisibly.

  2. src/main/java/com/viaoa/config/OAProperties.java / load(String fileName) and load(InputStream in)

  Concrete bug: reload overlays new properties without clearing existing properties, so removed config keys remain
  active.

  Runtime scenario:

  1. props.load("app.properties") loads sync.enabled=true.
  2. The file is changed to remove sync.enabled.
  3. props.load("app.properties") is called again to reload configuration.
  4. super.load(in) adds/replaces keys but does not remove keys absent from the new file.
  5. props.getBoolean("sync.enabled") still returns true.

  Why this violates OA/OG config semantics: reload must not leave stale values unless overlay/merge behavior is
  explicitly contracted. Stale config can keep old datasource, sync, remote, or runtime settings active after reload.

  Minimal fix direction: define separate loadOverlay vs reload behavior. For ordinary full-file reload, clear both
  super properties and alKeys before loading, preferably only committing after successful parse if atomic reload is
  required.

  Suggested CODEX comment location: line 169, before super.load(in), or line 144 load(String).

  Suggested regression test: testReloadRemovesKeysMissingFromNewFile.

  3. src/main/java/com/viaoa/config/OAProperties.java / put(Object key, Object obj)

  Concrete bug: case-insensitive null removal removes the property from Properties but leaves the old key in alKeys.

  Runtime scenario:

  1. props.put("ServerPort", "1099") stores key and adds "ServerPort" to alKeys.
  2. props.put("serverport", null) enters the case-insensitive match path.
  3. Line 422 returns super.remove(s) directly.
  4. alKeys.remove(s) is never called.
  5. keys() still enumerates "ServerPort" even though the property has been removed.

  Why this violates OA/OG config semantics: key enumeration and property state drift apart. Save, reporting, ordered
  config inspection, and reload tooling can see stale keys that no longer have values, producing misleading config ou
  tput or downstream failures depending on how Properties.save/store consumes keys().

  Minimal fix direction: replace return super.remove(s) with return remove(s) or explicitly remove s from alKeys
  before removing from super.

  Suggested CODEX comment location: line 422.

  Suggested regression test: testCaseInsensitiveNullPutRemovesKeyFromOrderedKeys.

  4. src/main/java/com/viaoa/config/OAProperties.java / load(InputStream in) and save(String fileName, String title)

  Concrete bug: resource streams are not closed on failure.

  Runtime scenario: super.load(in) throws due to malformed/truncated config, or super.save(os, title) throws during
  write. In load(InputStream), in.close() is only reached after successful super.load at line 169. In save, os.close()
  is only reached after successful super.save at line 233. Failure paths wrap and throw, but leave the stream open.

  Why this violates OA/OG config semantics: config file/resource streams must be closed on success and failure. In
  production reload/save loops, leaked file handles can block updates, retain resources, or cause later config
  operations to fail.

  Minimal fix direction: use try-with-resources for FileInputStream/FileOutputStream. For load(InputStream), either
  document that ownership transfers and always close in finally, or add a non-closing variant if caller owns the
  stream.

  Suggested CODEX comment location: lines 167-170 and lines 228-234.

  Suggested regression test: testLoadClosesInputStreamWhenParseFails, testSaveClosesOutputStreamWhenStoreFails.

  5. src/main/java/com/viaoa/config/OAProperties.java / load(String fileName) and save(String fileName, String title)

  Concrete bug: fileName is committed before the load/save succeeds, so a failed operation can corrupt the associated
  config source.

  Runtime scenario:

  1. props is associated with good.properties.
  2. Code calls props.load("bad.properties").
  3. Line 146 calls setFileName(fileName) before opening/parsing.
  4. If the file is missing, unreadable, or invalid, the object remains associated with bad.properties.
  5. A later props.save() or props.load() uses the failed path.

  The same pattern exists in save(String, String) at line 225 before directory creation/open/write succeeds.

  Why this violates OA/OG config semantics: partial setup should not commit source identity before the operation
  succeeds unless explicitly documented. After failed load/save, retry behavior can target the wrong file and
  overwrite or read the wrong config.

  Minimal fix direction: commit this.fileName only after successful load/save, or preserve the old value and restore
  it on failure. If missing-file no-op remains allowed, document whether it intentionally changes the associated file.

  Suggested CODEX comment location: line 146 and line 225.

  Suggested regression test: testFailedLoadDoesNotChangeAssociatedFileName,
  testFailedSaveDoesNotChangeAssociatedFileName.

  6. src/main/java/com/viaoa/config/OAProperties.java / getProperty, keys, put, remove

  Concrete bug: case-insensitive lookup enumerates a live ArrayList-backed key list while other synchronized methods
  can mutate it after the enumeration is returned.

  Runtime scenario: one thread calls getProperty("X"). It calls keys() and receives an enumeration over alKeys at
  lines 375-390. The lock is released when keys() returns. Another thread calls put, remove, or clear, mutating
  alKeys. The first thread then continues hasMoreElements() / nextElement() against the changed list, which can skip
  keys, see stale keys, or throw IndexOutOfBoundsException.

  Why this violates OA/OG config semantics: shared config state must be thread-safe or safely published. Runtime
  config reads racing with reload/update must not return nondeterministic values or fail from internal key-list
  mutation.

  Minimal fix direction: make keys() return a snapshot enumeration, or synchronize the full case-insensitive lookup
  while iterating. A LinkedHashMap/case-normalized map would also avoid dual-state drift.

  Suggested CODEX comment location: line 375 keys() and line 255 getProperty enumeration path.

  Suggested regression test: testConcurrentCaseInsensitiveLookupDuringMutationDoesNotThrowOrSkipCommittedKey.


 1. src/main/java/com/viaoa/config/OAProperties.java / getBoolean(String name, boolean bDefault)

  Concrete bug: invalid boolean values ignore the supplied default and silently return false.

  Runtime scenario: production config has sync.enabled=maybe or remote.enabled=enabled, and caller uses
  getBoolean("sync.enabled", true) expecting the default when the configured value is absent or unusable. The method
  returns the default only when the property is missing at line 733. If conversion fails, line 735 returns false.

  Why this violates OA/OG config semantics: invalid config conversion must fail visibly or follow the documented
  default contract. Returning false can silently disable runtime features such as sync, replication, remote behavior,
  or datasource options even when the caller supplied true as the intended fallback.

  Minimal fix direction: for the defaulted overload, return bDefault when conversion returns null, or add a strict
  variant that throws on invalid values and document the current behavior as “invalid means false” if truly intended.

  Suggested CODEX comment location: line 735.

  Suggested regression test: testGetBooleanDefaultUsedWhenConfiguredValueInvalid.

  2. src/main/java/com/viaoa/config/OAProperties.java / getInt(String name, int iDefault)

  Concrete bug: invalid integer values ignore the supplied default and silently return -1.

  Runtime scenario: production config has server.port=abc and caller uses getInt("server.port", 1099). The property
  exists, conversion fails, and line 660 returns -1 instead of the caller’s default. That can then be interpreted as a
  real port/count/timeout setting by downstream runtime code.

  Why this violates OA/OG config semantics: typed config conversion must preserve semantic value and fail visibly or
  default according to the method contract. Returning sentinel -1 from the defaulted overload is a misleading fallback
  that can produce wrong runtime mode, connection, timeout, or datasource settings.

  Minimal fix direction: for getInt(name, default), return iDefault when conversion fails, or provide a strict
  conversion API that throws on invalid numeric config.

  Suggested CODEX comment location: line 660.

  Suggested regression test: testGetIntDefaultUsedWhenConfiguredValueInvalid.

  3. src/main/java/com/viaoa/config/OAProperties.java / getString(String name, String strDefault)

  Concrete bug: name == null ignores the supplied default and returns null.

  Runtime scenario: config key composition fails upstream and passes null into getString(null, "defaultValue"). The
  defaulted overload returns null at line 693 instead of the supplied default. That can flow into path, datasource
  URL, mode, or logging config code as “no value” even though a default was supplied.

  Why this violates OA/OG config semantics: defaulted getters should apply their default consistently for absent/
  unresolvable keys. Returning null from a defaulted getter is a silent semantic mismatch.

  Minimal fix direction: return strDefault when name == null, or explicitly document that null keys bypass defaults.
  Align with the intended behavior of other defaulted accessors.

  Suggested CODEX comment location: line 693.

  Suggested regression test: testGetStringDefaultUsedForNullName.


*/

/*
    URL url = new URL("https://github.com/properties.values");
    URLConnection conn = url.openConnection();

    Properties props = new OAProperties(conn.getInputStream());
*/

/*
    Subclass of java.util.Properties where all "names" for name/value pairs are case insensitive.<br>
    NOTE: All name/value pairs are converted to Strings.
    <p>
    This can be used for working with *.ini files.
*/
/**
 * Case-insensitive extension of {@link java.util.Properties} where all property
 * names are treated without regard to case and all values are stored as strings.
 *
 * <p>
 * This class provides convenience methods for loading and saving properties
 * from files or input streams, retrieving typed values, and preserving key
 * insertion order. It is commonly used for working with configuration and
 * {@code .ini}-style files.
 * </p>
 */
public class OAProperties extends java.util.Properties implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Name of the file associated with these properties.
     */
    protected String fileName;
    
    /**
     * Flag indicating whether property keys should be treated as uppercase.
     */
    protected boolean bUpperCase=true;
    
    /**
     * Maintains the ordered list of property keys.
     */
    private ArrayList<String> alKeys = new ArrayList<String>();


    /**
     * Creates a new empty {@code OAProperties} instance.
     */
    public OAProperties() {
    }

    /*
        Creates a new OAProperties using the specified file name and loads the file.
        @see #load
    */
    /**
     * Creates a new {@code OAProperties} instance associated with the given file
     * name and loads the properties from that file.
     *
     * @param fname the properties file name
     */
    public OAProperties(String fname) {
        setFileName(fname);
        load();
    }

    /**
     * Creates a new {@code OAProperties} instance and loads properties from the
     * given input stream.
     *
     * @param in the input stream containing properties data
     */
    public OAProperties(InputStream in) {
        load(in);
    }

    /**
     * Sets the file name associated with these properties.
     *
     * @param fname the file name to associate
     */
    public void setFileName(String fname) {
        this.fileName = fname;
    }

    /**
     * Returns the file name associated with these properties.
     *
     * @return the associated file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Loads name/value properties from the associated file.
     *
     * @throws IllegalArgumentException if the file name has not been set
     */
    public void load() {
        if (fileName == null) throw new IllegalArgumentException("fileName must be set before calling load()");
        load(fileName);
    }

    /**
     * Loads name/value properties from the specified file after normalizing the
     * file path.
     *
     * @param fileName the file name to load properties from
     */
    public void load(String fileName) {
        fileName = OAString.convertFileName(fileName);
        setFileName(fileName);
        try {
            File file = new File(fileName);
            if (!file.exists()) return;
            FileInputStream in = new FileInputStream(file);
            load(in);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Loads name/values from a file.
     * NOTE: if value has spaces after it on the line, they will be included in the value of the property. 
     */
    /**
     * Loads name/value properties from the given input stream.
     *
     * @param in the input stream containing properties data
     */
    public void load(InputStream in) {
        try {
            super.load(in);
            in.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /*
        Saves name/value properties to file.
        @throws IllegalArgumentException if fileName has not been set.
        @see #setFileName
        @see #save(String,String)
    */
    /**
     * Saves name/value properties to the associated file.
     *
     * @throws IllegalArgumentException if the file name has not been set
     */
    public void save() {
        if (fileName == null) throw new IllegalArgumentException("fileName must be set before calling save()");
        save(fileName,"");
    }

    /*
        Converts fileName path to correct system file.separator chars and saves name/value properties to file.
        see #ssave(String,String)
    */
    /**
     * Saves name/value properties to the specified file after normalizing the
     * file path.
     *
     * @param fileName the file name to save properties to
     */
    public void save(String fileName) {
        save(fileName, "");
    }

    /*
        Converts fileName path to correct system file.separator chars and saves name/value properties to file.
        @param fileName name of file to store name/values.
        @param title is commented title used within file.
    */
    /**
     * Saves name/value properties to the specified file with an optional title
     * comment.
     *
     * @param fileName the file name to save properties to
     * @param title a comment title to include in the file
     */
    public void save(String fileName, String title) {
        if (fileName == null) {
            throw new RuntimeException("OAProperties.save() fileName is not assigned (null)");
        }
        fileName = OAString.convertFileName(fileName);
        setFileName(fileName);

        OAFile.mkdirsForFile(fileName);
        FileOutputStream os = null;
        try {
            File file = new File(fileName);
            os = new FileOutputStream(file);
            if (title == null) title = "";
            super.save(os, title);
            os.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /*
        Retrieve value of a name/value pair where name is case insensitive.
        @param name is not case sensitive
    */
    /**
     * Returns the value of a property using a case-insensitive key lookup.
     *
     * @param name the property name (case-insensitive)
     * @return the property value, or {@code null} if not found
     */
    public String getProperty(String name) {
        if (name == null) return null;

        String s = (String) super.getProperty(name);
        if (s != null) return s;

        Enumeration enumx = this.keys();
        for (;enumx.hasMoreElements();) {
            s = (String) enumx.nextElement();
            if (s != null && s.equalsIgnoreCase(name)) return super.getProperty(s);
        }
        return null;
    }

    /*
        Retrieve value of a name/value pair where name is case insensitive.  If property does not exists, then
        a default value can be specified to use.
        @param name is name of property and is not case sensitive.
        @param defaultValue is returned if property does not exist.
    */
    /**
     * Returns the value of a property using a case-insensitive key lookup, or a
     * default value if the property does not exist.
     *
     * @param name the property name (case-insensitive)
     * @param defaultValue the value to return if the property does not exist
     * @return the property value or the default value
     */
    public String getProperty(String name, String defaultValue) {
        String s = this.getProperty(name);
        if (s != null) return s;
        return defaultValue;
    }


    /*
        Retrieve value of a name/value pair where name is case insensitive.
        @param name is name of property and is not case sensitive.
    */
    /**
     * Returns the value of a property using a case-insensitive key lookup.
     *
     * @param name the property name (case-insensitive)
     * @return the property value, or {@code null} if not found
     */
    public Object get(String name) {
        if (name == null) return null;
        return this.getProperty(name);
    }

    /**
     * Sets or replaces a property value using a case-insensitive key.
     *
     * @param name the property name (case-insensitive)
     * @param value the property value
     * @return the previous value associated with the key, or {@code null}
     */
    public Object setProperty(String name, String value) {
        return this.put(name, value);
    }

    /**
     * Sets or replaces a property value using a case-insensitive key and a boolean value.
     *
     * @param name the property name (case-insensitive)
     * @param value the boolean value to store
     * @return the previous value associated with the key, or {@code null}
     */
    public Object setProperty(String name, boolean value) {
    	return put(name, OAConv.toString(value));
    }
    
    /**
     * Sets or replaces a property value using a case-insensitive key and an integer value.
     *
     * @param name the property name (case-insensitive)
     * @param value the integer value to store
     * @return the previous value associated with the key, or {@code null}
     */
    public Object setProperty(String name, int value) {
    	return put(name, OAConv.toString(value));
    }

    /**
     * Sets or replaces a property value using a case-insensitive key and a long value.
     *
     * @param name the property name (case-insensitive)
     * @param value the long value to store
     * @return the previous value associated with the key, or {@code null}
     */
    public Object setProperty(String name, long value) {
    	return put(name, OAConv.toString(value));
    }
    
    /**
     * Sets or replaces a property value using a case-insensitive key and a double value.
     *
     * @param name the property name (case-insensitive)
     * @param value the double value to store
     * @return the previous value associated with the key, or {@code null}
     */
    public Object setProperty(String name, double value) {
    	return put(name, OAConv.toString(value));
    }

    /**
     * Sets or replaces a property value using a case-insensitive key and an object value,
     * converting the value to a string representation.
     *
     * @param name the property name (case-insensitive)
     * @param value the value to store
     * @return the previous value associated with the key, or {@code null}
     */
    public Object setProperty(String name, Object value) {
    	return put(name, OAConv.toString(value));
    }
    
    
    /**
     * Returns an enumeration of property keys in insertion order.
     *
     * @return an enumeration of property keys
     */
    @Override
    public synchronized Enumeration keys() {
        if (alKeys == null) {
            return super.keys();
        }
        Enumeration enumx = new Enumeration() {
            int pos = 0;
            @Override
            public boolean hasMoreElements() {
                return pos < alKeys.size();
            }
            @Override
            public Object nextElement() {
                Object obj = alKeys.get(pos);
                pos++;
                return obj;
            }
        };
        return enumx;
    }

    /**
     * Removes all properties and clears the internal key list.
     */
    @Override
    public synchronized void clear() {
        super.clear();
        alKeys.clear();
    }
   
    /**
     * Adds or replaces a property value using a case-insensitive key.
     *
     * @param key the property key (must be a {@link String})
     * @param obj the value to associate with the key
     * @return the previous value associated with the key, or {@code null}
     */
    @Override
    public synchronized Object put(Object key, Object obj) {
        if (!(key instanceof String)) return null;
        String name = (String) key;
        
        if (super.getProperty(name) == null) {
            // see if it exists under a different case
            Enumeration enumx = this.keys();
            for (;enumx.hasMoreElements();) {
                String s = (String) enumx.nextElement();
                if (s != null && s.equalsIgnoreCase(name)) {
                    if (obj == null) return super.remove(s);
                    remove(s);
                    break;
                }
            }
            if (obj == null) return null;
        }
        else {
            if (obj == null) return remove(name);
        }
        if (!alKeys.contains(name)) alKeys.add(name);
        return super.put(name, OAConv.toString(obj));
    }


    /**
     * Removes a property using a case-insensitive key.
     *
     * @param key the property key (must be a {@link String})
     * @return the removed value, or {@code null} if none existed
     */
    @Override
    public synchronized Object remove(Object key) {
        if (!(key instanceof String)) return null;
        String name = (String) key;
        alKeys.remove(name);
        Object obj = super.remove(name);
        if (obj != null) return obj;

        Enumeration enumx = this.keys();
        for (;enumx.hasMoreElements();) {
            String s = (String) enumx.nextElement();
            if (s != null && s.equalsIgnoreCase(name)) {
                alKeys.remove(name);
                return super.remove(s);
            }
        }
        return null;
    }

    /**
     * Adds or replaces a property value using a case-insensitive key and an integer value.
     *
     * @param name the property name (case-insensitive)
     * @param i the integer value to store
     */
    public void put(String name, int i) {
        if (name != null) put(name, (i+""));
    }
    
    /**
     * Adds or replaces a property value using a case-insensitive key and a boolean value.
     *
     * @param name the property name (case-insensitive)
     * @param b the boolean value to store
     */
    public void put(String name, boolean b) {
        if (name != null) put(name, b+"");
    }
    
    /**
     * Adds or replaces a property value using a case-insensitive key and an integer value.
     *
     * @param name the property name (case-insensitive)
     * @param i the integer value to store
     */
    public void putInt(String name, int i) {
        this.put(name, i);
    }

    /**
     * Returns the property value converted to an integer, or {@code -1} if the
     * property does not exist or cannot be converted.
     *
     * @param name the property name (case-insensitive)
     * @return the integer value, or {@code -1} if unavailable
     */
    public int getInt(String name) {
        Object obj = getProperty(name);
        if (obj == null) return -1;
        obj = (Object) OAConv.convert(Integer.class, obj);
        if (obj == null || !(obj instanceof Number)) return -1;
        return ((Number)obj).intValue();
    }

    /*
        Returns a property that is converted to an "int".  If name/value does not exist,
        then iDefault is returned.
        if value is not a number, then -1 is returned.
        @param name is name of property and is not case sensitive.
    */
    /**
     * Returns the property value converted to an integer, or the specified default
     * value if the property does not exist.
     *
     * @param name the property name (case-insensitive)
     * @param iDefault the default value to return if the property does not exist
     * @return the integer value or the default value
     */
    public int getInt(String name, int iDefault) {
        Object obj = getProperty(name);
        if (obj == null) return iDefault;
        obj = (Object) OAConv.convert(Integer.class, obj);
        if (obj == null || !(obj instanceof Number)) return -1;
        return ((Number)obj).intValue();
    }

    /*
        Returns a property value.  If property does not exist, then null is returned.
        @param name is name of property and is not case sensitive.
    */
    /**
     * Returns the property value as a string, or {@code null} if the property does
     * not exist.
     *
     * @param name the property name (case-insensitive)
     * @return the property value or {@code null}
     */
    public String getString(String name) {
        if (name == null) return null;
        return this.getProperty(name);
    }

    /*
        Returns a property value.  If property does not exist, then null is returned.
        @param name is name of property and is not case sensitive.
    */
    /**
     * Returns the property value as a string, or the specified default value if the
     * property does not exist.
     *
     * @param name the property name (case-insensitive)
     * @param strDefault the default value to return if the property does not exist
     * @return the property value or the default value
     */
    public String getString(String name, String strDefault) {
        if (name == null) return null;
        String s = this.getProperty(name);
        if (s == null) return strDefault;
        return s;
    }

    /*
        Returns a property that is converted to a "boolean".  If name/value does not exist, then false is returned.
        @param name is name of property and is not case sensitive.
    */
    /**
     * Returns the property value converted to a boolean, or {@code false} if the
     * property does not exist or cannot be converted.
     *
     * @param name the property name (case-insensitive)
     * @return the boolean value
     */
    public boolean getBoolean(String name) {
        if (name == null) return false;
        Object obj = this.getProperty(name);
        Boolean b = (Boolean) OAConverter.convert(Boolean.class, obj);
        if (b == null) return false;
        return ((Boolean)b).booleanValue();
    }

    /*
        Returns a property that is converted to a "boolean".  If name/value does not exist, then false is returned.
        @param name is name of property and is not case sensitive.
    */
    /**
     * Returns the property value converted to a boolean, or the specified default
     * value if the property does not exist.
     *
     * @param name the property name (case-insensitive)
     * @param bDefault the default value to return if the property does not exist
     * @return the boolean value
     */
    public boolean getBoolean(String name, boolean bDefault) {
        if (name == null) return false;
        Object obj = this.getProperty(name);
        if (obj == null) return bDefault;
        Boolean b = (Boolean) OAConverter.convert(Boolean.class, obj);
        if (b == null) return false;
        return ((Boolean)b).booleanValue();
    }


    /*
        Returns true if property name exists.
        @param name is name of property and is not case sensitive.
    */
    /**
     * Returns {@code true} if a property with the given name exists.
     *
     * @param name the property name (case-insensitive)
     * @return {@code true} if the property exists, {@code false} otherwise
     */
    public boolean exists(String name) {
        if (name == null) return false;
        Object obj = this.getProperty(name);
        return obj != null;
    }

}
