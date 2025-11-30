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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.logging.Logger;

import com.viaoa.util.OAArray;
import com.viaoa.util.OADateTime;

/**
 * Persists and restores information about "empty" reference hubs for
 * {@link OAObject}s so that application restarts can reconstruct those
 * hubs without re-querying a data source.
 *
 * <p>At shutdown, {@link #save(File)} scans all cached objects and
 * records which reference hubs are loaded and empty.  On startup,
 * {@link #load(File)} reads that metadata so that subsequent calls to
 * {@link OAObjectReflectDelegate#getReferenceHub(OAObject,String)}
 * can create empty hubs without triggering database access.</p>
 *
 * <p><b>Key Responsibilities</b>:
 * <ul>
 *   <li>Serialize/deserialize hub-emptiness metadata to disk.</li>
 *   <li>Integrate with {@link OAObjectCacheDelegate#callback} to iterate
 *       over all cached objects.</li>
 *   <li>Initialize empty hubs during {@link OAObject#afterLoad()}.</li>
 * </ul>
 */
public class OAObjectEmptyHubDelegate {
    private static Logger LOG = Logger.getLogger(OAObjectEmptyHubDelegate.class.getName());

    private static HashMap<String, HashMap<Integer, String[]>> map;    
    private static boolean bEnabled;
    
    /**
     * Initializes any reference hubs on the specified object that were
     * previously recorded as empty. This prevents database access by
     * restoring empty-hub metadata loaded during startup.
     *
     * @param obj the object whose empty reference hubs should be initialized;
     *            ignored if {@code null} or no metadata exists
     */
    public static void initialize(OAObject obj) {
        if (map == null) return;
        if (obj == null) return;
        
        Class clazz = obj.getClass();

        HashMap<Integer, String[]> hm = map.get(clazz.getName());
        if (hm == null) return;
        
        OAObjectKey key = OAObjectKeyDelegate.getKey(obj);
        if (key == null) return;
        
        Object[] keys = key.getObjectIds();
        if (keys == null || keys.length != 1 || !(keys[0] instanceof Integer)) return;
            
        int x = (Integer) keys[0];
        
        Object objx = hm.get(x);
        if (objx == null) return;

        hm.remove(x);
        
        for (String s : (String[]) objx) {
            OAObjectPropertyDelegate.setProperty(obj, s, null);
        }
    }
 
    /**
     * Loads previously saved metadata describing empty reference hubs from
     * the specified file. The file contains a timestamp followed by the
     * serialized hub-emptiness map.
     *
     * @param file the file containing the serialized metadata
     * @throws Exception if the file cannot be read or deserialized
     */
    public static void load(File file) throws Exception {
        if (file == null || !file.exists()) {
            LOG.fine("file does not exist");
            return;
        }
        FileInputStream fis = new FileInputStream(file);

        ObjectInputStream ois = new ObjectInputStream(fis);
        
        OADateTime dt = (OADateTime) ois.readObject();
        
        map = (HashMap<String, HashMap<Integer, String[]>>) ois.readObject();
        
        ois.close();
        fis.close();
    }
    
    /**
     * Scans all cached {@link OAObject} instances and records the reference
     * hubs that are loaded and empty. The resulting metadata is serialized
     * to the specified file for later restoration via {@link #load(File)}.
     *
     * @param file the file to which the metadata is written
     * @throws Exception if writing or serialization fails
     */
    public static void save(File file) throws Exception {
        LOG.fine("saving all null properties");

        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(new OADateTime());
        
        final HashMap<String, HashMap<Integer, String[]>> mapx = new HashMap<String, HashMap<Integer,String[]>>();
        
        OAObjectCacheDelegate.callback(new OACallback() {
            int cnt = 0;
            @Override
            public boolean updateObject(Object obj) {
                if (!(obj instanceof OAObject)) return true;
                cnt++;
                if (cnt % 250 == 0) {
                    LOG.fine(cnt+") saving "+obj);
                }
                
                String[] ssNew = null;
                String[] ss = OAObjectPropertyDelegate.getPropertyNames((OAObject) obj);
                if (ss != null) { 
                    for (String s : ss) {
                        if (OAObjectReflectDelegate.isReferenceHubLoadedAndEmpty((OAObject) obj, s)) {
                            ssNew = (String[]) OAArray.add(String.class, ssNew, s);
                        }
                    }
                }
                if (ssNew == null) return true;
                
                OAObjectKey key = OAObjectKeyDelegate.getKey((OAObject)obj);
                if (key == null) return true;
                
                Object[] keys = key.getObjectIds();
                if (keys == null || keys.length != 1 || !(keys[0] instanceof Integer)) return true;
                    
                int keyId = (Integer) keys[0];
                
                Class clazz = obj.getClass();
                HashMap<Integer, String[]> hm = mapx.get(clazz.getName());
                if (hm == null) {
                    hm = new HashMap<Integer, String[]>();
                    mapx.put(clazz.getName(), hm);
                }
                hm.put(keyId, ssNew);
                return true;
            }
        });
        
        oos.writeObject(mapx);
        oos.close();
    }
    
}
