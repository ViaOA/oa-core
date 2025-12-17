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
package com.viaoa.util;

import java.util.*;
import java.io.*;

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
