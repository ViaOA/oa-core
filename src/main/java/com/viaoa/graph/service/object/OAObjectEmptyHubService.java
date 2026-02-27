package com.viaoa.graph.service.object;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.viaoa.object.OACallback;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OAArray;
import com.viaoa.util.OADateTime;

public abstract class OAObjectEmptyHubService {
	private static final Logger LOG = Logger.getLogger(OAObjectEmptyHubService.class.getName());

    public OAObjectEmptyHubService() {
    }
	
    /**
     * In-memory structure storing empty-hub metadata.
     *
     * The outer map is keyed by class name.
     * The inner map is keyed by integer primary-key values and contains
     * arrays of property names representing reference hubs that were
     * recorded as loaded and empty.
     */
    private static Map<String, Map<Integer, String[]>> map;    

    /**
     * Flag indicating whether empty-hub tracking is enabled.
     * When disabled, no metadata is saved or restored.
     */
    private static boolean bEnabled;

    
    /**
     * Initializes any reference hubs on the specified object that were
     * previously recorded as empty. This prevents database access by
     * restoring empty-hub metadata loaded during startup.
     *
     * @param obj the object whose empty reference hubs should be initialized;
     *            ignored if {@code null} or no metadata exists
     */
    public void initialize(OAObject obj) {
        if (map == null) return;
        if (obj == null) return;
        
        Class<? extends OAObject> clazz = obj.getClass();

        Map<Integer, String[]> hm = map.get(clazz.getName());
        if (hm == null) return;
        
        OAObjectKey key = callKeyGetKey(obj);
        if (key == null) return;
        
        Object[] keys = key.getObjectIds();
        if (keys == null || keys.length != 1 || !(keys[0] instanceof Integer)) return;
            
        int x = (Integer) keys[0];
        
        Object objx = hm.get(x);
        if (objx == null) return;

        hm.remove(x);
        
        for (String s : (String[]) objx) {
        	callPropertySetProperty(obj, s, null);
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
    @SuppressWarnings("unchecked")
    public void load(File file) throws Exception {
        if (file == null || !file.exists()) {
            LOG.fine("file does not exist");
            return;
        }
        FileInputStream fis = new FileInputStream(file);

        ObjectInputStream ois = new ObjectInputStream(fis);
        
        OADateTime dt = (OADateTime) ois.readObject();
        
        map = (Map<String, Map<Integer, String[]>>) ois.readObject();
        
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
    public void save(File file) throws Exception {
        LOG.fine("saving all null properties");

        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(new OADateTime());
        
        final Map<String, Map<Integer, String[]>> mapx = new HashMap<>();
        
        callCacheCallback(new OACallback<OAObject>() {
            int cnt = 0;
            @Override
            public boolean updateObject(OAObject obj) {
                if (!(obj instanceof OAObject)) return true;
                cnt++;
                if (cnt % 250 == 0) {
                    LOG.fine(cnt+") saving "+obj);
                }
                
                String[] ssNew = null;
                String[] ss = callPropertyGetPropertyNames((OAObject) obj);
                if (ss != null) { 
                    for (String s : ss) {
                        if (callReflectIsReferenceHubLoadedAndEmpty((OAObject) obj, s)) {
                            ssNew = (String[]) OAArray.add(String.class, ssNew, s);
                        }
                    }
                }
                if (ssNew == null) return true;
                
                OAObjectKey key = callKeyGetKey((OAObject)obj);
                if (key == null) return true;
                
                Object[] keys = key.getObjectIds();
                if (keys == null || keys.length != 1 || !(keys[0] instanceof Integer)) return true;
                    
                int keyId = (Integer) keys[0];
                
                Class<? extends OAObject> clazz = obj.getClass();
                Map<Integer, String[]> hm = mapx.get(clazz.getName());
                if (hm == null) {
                    hm = new HashMap<>();
                    mapx.put(clazz.getName(), hm);
                }
                hm.put(keyId, ssNew);
                return true;
            }
        });
        
        oos.writeObject(mapx);
        oos.close();
    }

	public abstract OAObjectKey callKeyGetKey(OAObject oaObj); 
	public abstract void callPropertySetProperty(OAObject oaObj, String name, Object value); 
	public abstract void callCacheCallback(OACallback<OAObject> callback); 
	public abstract String[] callPropertyGetPropertyNames(OAObject oaObj);
	public abstract boolean callReflectIsReferenceHubLoadedAndEmpty(OAObject oaObj, String propertyName);
}
