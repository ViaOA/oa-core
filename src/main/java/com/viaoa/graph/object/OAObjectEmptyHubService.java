package com.viaoa.graph.object;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.logging.Logger;

import com.viaoa.graph.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACallback;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OAArray;
import com.viaoa.util.OADateTime;

public class OAObjectEmptyHubService {
	private static final Logger LOG = Logger.getLogger(OAObjectEmptyHubService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;
	
    public OAObjectEmptyHubService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faObject = oaObjectFriendAccess;
    }
	
    public OAObjectService getObjectService() {
    	return srvcObject;
    }

    /**
     * In-memory structure storing empty-hub metadata.
     *
     * The outer map is keyed by class name.
     * The inner map is keyed by integer primary-key values and contains
     * arrays of property names representing reference hubs that were
     * recorded as loaded and empty.
     */
    private static HashMap<String, HashMap<Integer, String[]>> map;    

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
        
        Class clazz = obj.getClass();

        HashMap<Integer, String[]> hm = map.get(clazz.getName());
        if (hm == null) return;
        
        OAObjectKey key = srvcObject.getOAObjectKeyService().getKey(obj);
        if (key == null) return;
        
        Object[] keys = key.getObjectIds();
        if (keys == null || keys.length != 1 || !(keys[0] instanceof Integer)) return;
            
        int x = (Integer) keys[0];
        
        Object objx = hm.get(x);
        if (objx == null) return;

        hm.remove(x);
        
        for (String s : (String[]) objx) {
        	srvcObject.getOAObjectPropertyService().setProperty(obj, s, null);
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
    public void load(File file) throws Exception {
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
    public void save(File file) throws Exception {
        LOG.fine("saving all null properties");

        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(new OADateTime());
        
        final HashMap<String, HashMap<Integer, String[]>> mapx = new HashMap<String, HashMap<Integer,String[]>>();
        
        srvcObject.getOAObjectCacheService().callback(new OACallback() {
            int cnt = 0;
            @Override
            public boolean updateObject(Object obj) {
                if (!(obj instanceof OAObject)) return true;
                cnt++;
                if (cnt % 250 == 0) {
                    LOG.fine(cnt+") saving "+obj);
                }
                
                String[] ssNew = null;
                String[] ss = srvcObject.getOAObjectPropertyService().getPropertyNames((OAObject) obj);
                if (ss != null) { 
                    for (String s : ss) {
                        if (srvcObject.getOAObjectReflectService().isReferenceHubLoadedAndEmpty((OAObject) obj, s)) {
                            ssNew = (String[]) OAArray.add(String.class, ssNew, s);
                        }
                    }
                }
                if (ssNew == null) return true;
                
                OAObjectKey key = srvcObject.getOAObjectKeyService().getKey((OAObject)obj);
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
