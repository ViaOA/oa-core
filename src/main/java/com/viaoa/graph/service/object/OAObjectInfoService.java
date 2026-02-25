package com.viaoa.graph.service.object;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACalcInfo;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAString;

public abstract class OAObjectInfoService {
	private static final Logger LOG = Logger.getLogger(OAObjectInfoService.class.getName());

	private final OAObject.FriendAccess faObject;
	private final OAObjectInfo.FriendAccess faObjectInfo;
	
    public OAObjectInfoService(OAObject.FriendAccess faObject, OAObjectInfo.FriendAccess faObjectInfo) {
    	if (faObject == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faObject = faObject;
    	if (faObjectInfo == null) throw new IllegalArgumentException("OAObjectInfoFriendAccess can not be null");
    	this.faObjectInfo = faObjectInfo;
    }

	/**
	 * Global synchronization lock used to guard critical sections during
	 * OAObjectInfo creation and initialization. Ensures that metadata
	 * construction is thread-safe across all OAObject types.
	 */
	private final Object Lock = new Object();

	/**
	 * Cache of reflected methods for each class, keyed by uppercase method
	 * name. Used to accelerate method lookup and avoid repeated reflection.
	 */
    private final Map<Class, Map<String, Method>> hmClassMethod = new ConcurrentHashMap<>(151, 0.75F);
    
    /**
     * Tracks method names that were previously searched for but not found
     * on a per-class basis, preventing redundant failed lookups.
     */
    private final Map<Class, Set<String>> hmClassMethodNotFound = new ConcurrentHashMap<>(151, 0.75F);
    
    /**
     * Per-link read/write locks used to coordinate safe concurrent access
     * to each link’s Hub-cache structures.
     */
    private final Map<OALinkInfo, ReentrantReadWriteLock> hmLinkInfoCacheLock = new ConcurrentHashMap<>(47,0.75f);
	
    /**
     * Ordered list-based cache used for each link’s Hub instances, storing
     * Hubs in insertion order to support trimming by cache size.
     */
    private final Map<OALinkInfo, List> hmLinkInfoCacheList = new ConcurrentHashMap<OALinkInfo, List>(47,0.75f);
    
    /**
     * Set used for rapid membership checks for each link’s Hub cache,
     * ensuring efficient duplicate-prevention.
     */
    private final Map<OALinkInfo, Set> hmLinkInfoCacheSet = new ConcurrentHashMap<OALinkInfo, Set>(47,0.75f);
	
    /**
     * Maps each OAObjectInfo to its assigned root Hub when the type is
     * recursive and lacks an owner link.
     */
    private final Map<OAObjectInfo, Hub> hmRootHub = new ConcurrentHashMap<OAObjectInfo, Hub>(41, .75f);
    
    /**
     * Global cache mapping Java classes to their corresponding OAObjectInfo
     * instances. All metadata lookups consult this cache first.
     */
    private final Map<Class, OAObjectInfo> hmObjectInfo = new ConcurrentHashMap<Class, OAObjectInfo>(147, 0.75F);
	

    /**
     * Returns the OAObjectInfo associated with the class of the supplied
     * OAObject. Delegates to {@link #getOAObjectInfo(Class)} using the
     * object's runtime class, or null if the object is null.
     *
     * @param obj the OAObject whose metadata is requested.
     * @return the OAObjectInfo for the object's class.
     */
	public OAObjectInfo getOAObjectInfo(OAObject obj) {
		OAObjectInfo oi = getOAObjectInfo(obj == null ? null : obj.getClass());
		return oi;
	}

	/**
	 * Convenience wrapper around {@link #getOAObjectInfo(OAObject)}.
	 *
	 * @param obj the OAObject whose metadata is requested.
	 * @return the OAObjectInfo for the object's class.
	 */
	public OAObjectInfo getObjectInfo(OAObject obj) {
		return getOAObjectInfo(obj);
	}

	/**
	 * Returns the OAObjectInfo associated with the supplied class.
	 * If the class is null, not an OAObject subclass, or OAObject itself,
	 * returns a placeholder OAObjectInfo based on String.class. Otherwise,
	 * checks the cache and delegates to the recursive builder when needed.
	 *
	 * @param clazz the class to retrieve metadata for.
	 * @return the corresponding OAObjectInfo instance.
	 */
	public OAObjectInfo getOAObjectInfo(Class<?> clazz) {
		OAObjectInfo oi;
		if (clazz != null) {
			oi = hmObjectInfo.get(clazz);
			if (oi != null) {
				return oi;
			}
		}
		if (clazz == null || !OAObject.class.isAssignableFrom(clazz) || OAObject.class.equals(clazz)) {
			oi = hmObjectInfo.get(String.class); // fake out so that null is never returned
			if (oi != null) {
				return oi;
			}
		}

		oi = getOAObjectInfo(clazz, new HashMap<Class<?>, OAObjectInfo>());
		return oi;
	}

	/**
	 * Internal recursive helper used to build OAObjectInfo instances while
	 * preventing cycles via a per-call hash map. Handles non-OA classes,
	 * cache lookup, creation, annotation processing, reverse link creation,
	 * and final metadata augmentation.
	 *
	 * @param clazz the class being processed.
	 * @param hash  map used to prevent recursive reprocessing.
	 * @return the OAObjectInfo for the class.
	 */
	private OAObjectInfo getOAObjectInfo(Class<?> clazz, HashMap<Class<?>, OAObjectInfo> hash) {
		OAObjectInfo oi;
		boolean bNotOa = false;
		if (clazz == null || !OAObject.class.isAssignableFrom(clazz) || OAObject.class.equals(clazz)) {
			bNotOa = true;
			clazz = String.class;
		}
		oi = hash.get(clazz);
		if (oi != null) {
			return oi;
		}

		oi = hmObjectInfo.get(clazz);
		if (oi != null) {
			return oi;
		}

		oi = _getOAObjectInfo(clazz);
		hash.put(clazz, oi);

		if (bNotOa) {
			return oi;
		}

		// must be ran after oi is created and stored (in hash), since it will create propPaths, which will load other ObjectInfos
		callAnnotationUpdate2(oi, clazz);

		// make sure that reverse linkInfos are created.
		//   ex: ServerRoot.users, the User.class needs to have LinkInfo to serveRoot
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.MANY) {
				continue;
			}
			OALinkInfo liRev = getReverseLinkInfo(li);
			if (liRev != null) {
				continue;
			}
			Class c = li.getToClass();
			if (c == null) {
				continue;
			}
			OAObjectInfo oiRev = getOAObjectInfo(c, hash);

			String revName = li.getReverseName();
			if (OAString.isEmpty(revName)) {
				li.setReverseName(revName = "Reverse" + li.getName());
			}

			liRev = new OALinkInfo(revName, clazz, OALinkInfo.ONE, false, false, li.getName());
			liRev.setPrivateMethod(true);
			liRev.setNotUsed(true);
			oiRev.getLinkInfos().add(liRev);
		}

		// 20220503 load importMatch propertyPaths
		callAnnotationUpdateImportMatches(oi);

		// 20220918 load fkey
		callAnnotationUpdateLinkFkeys(oi);

		return oi;
	}

	/**
	 * Creates or returns the cached OAObjectInfo for the supplied class.
	 * Handles special cases for non-OA classes, invokes class-level
	 * getOAObjectInfo() when present, initializes metadata, merges with
	 * superclass metadata, applies annotations, and finalizes primitive
	 * and link settings.
	 *
	 * @param clazz the class to create metadata for.
	 * @return the constructed OAObjectInfo instance.
	 */
	private OAObjectInfo _getOAObjectInfo(Class<?> clazz) {
		boolean bSkip = false;
		if (clazz == null || !OAObject.class.isAssignableFrom(clazz) || OAObject.class.equals(clazz)) {
			bSkip = true;
			clazz = String.class; // fake out so that null is never returned
		}

		OAObjectInfo oi = hmObjectInfo.get(clazz);
		if (oi != null) {
			return oi;
		}

		synchronized (Lock) {
			oi = (OAObjectInfo) hmObjectInfo.get(clazz);
			if (oi != null) {
				return oi;
			}

			if (!bSkip) {
				Method m = null;
				try {
					m = clazz.getMethod("getOAObjectInfo", new Class[] {});
					if (m != null) {
						oi = (OAObjectInfo) m.invoke(null, (Object[]) null);
					}
				} catch (Exception e) {
					//System.out.println("srvcObject.getOAObjectInfoService().getOAObjectInfo "+e);
					//e.printStackTrace();
					oi = null;
				}
				if (oi == null) {
					oi = new OAObjectInfo();
				}

				initialize(oi, clazz); // this will load all props/links/primitives

				Class superClass = clazz.getSuperclass(); // if there is a superclass, then combine with oaobjectinfo
				if (superClass != null && !superClass.equals(OAObject.class)) {
					OAObjectInfo oi2 = getOAObjectInfo(superClass);
					oi = createCombinedObjectInfo(oi, oi2);
					oi.setForClass(clazz);
				}

				callAnnotationUpdate(oi, clazz);

				for (OALinkInfo li : oi.getLinkInfos()) {
					if (li.getPrivateMethod()) {
						continue;
					}
					Method method = getMethod(oi, "get" + li.getName(), 0);
					if (method == null) {
						li.setPrivateMethod(true);
					}
				}

				// clean up tracking primitives
				String[] pps = oi.getPrimitiveProperties(); 
				int x = pps == null ? 0 : pps.length;
				for (int i = 0; i < x; i++) {
					String prop = pps[i];
					OAPropertyInfo pi = oi.getPropertyInfo(prop);
					if (pi != null && pi.getIsPrimitive() && !pi.getTrackPrimitiveNull()) {
						if (!pi.getKey()) {
							pps = OAArray.removeAt(pps, i);
							i--;
							x--;
						}
					}
				}
				oi.setPrimitiveProperties(pps);
				hmObjectInfo.put(clazz, oi);
			}

			if (oi == null) {
				oi = new OAObjectInfo();
				initialize(oi, clazz);
				hmObjectInfo.put(clazz, oi);
			}
		}
		return oi;
	}

	
	// only "grabs" info from this clazz. If there is a superclass, then it will be combined by getOAObjectInfo (above)
	/**
	 * Populates the supplied OAObjectInfo with metadata discovered from
	 * the class. Scans declared getters/setters, identifies property,
	 * link, and hub relationships, constructs OAPropertyInfo entries,
	 * computes primitive and hub property lists, and finalizes internal
	 * caches.
	 *
	 * @param thisOI the OAObjectInfo being initialized.
	 * @param clazz  the class whose metadata is extracted.
	 */
	private void initialize(OAObjectInfo thisOI, Class<?> clazz) {
		if (thisOI.getForClass() != null) {
			return;
		}
		thisOI.setForClass(clazz);

		ArrayList<String> alPrimitive = new ArrayList<String>();
		ArrayList<String> alHub = new ArrayList<String>();

		// only get props for this class, then combine with superClass(es)
		String[] props = getPropertyNames(clazz, false);

		for (int i = 0; props != null && i < props.length; i++) {
			String name = props[i];
			if (name == null) {
				continue;
			}
			Method m = getMethod(thisOI, "get" + name, 0); // always use getter, since the setter could be overloaded (ex: with int,String,enum)

			if (m == null) {
				m = getMethod(thisOI, "is" + name);
				if (m == null) {
					continue;
				}
			}

			if (m.getReturnType().equals(Hub.class)) {
				if ((m.getModifiers() & Modifier.STATIC) != 0) {
					continue;
				}
				alHub.add(name.toUpperCase());
				createLink(thisOI, name, null, OALinkInfo.MANY);
				continue;
			}

			if (OAObject.class.isAssignableFrom(m.getReturnType())) {
				if ((m.getModifiers() & Modifier.STATIC) != 0) {
					continue;
				}
				createLink(thisOI, name, m.getReturnType(), OALinkInfo.ONE);
				continue;
			}

			OAPropertyInfo pi = thisOI.getPropertyInfo(name);
			if (pi == null) {
				pi = new OAPropertyInfo();
				pi.setName(name);
				pi.setClassType(m.getReturnType());
			}
			
			String[] ids = thisOI.getIdProperties();
			for (int j = 0; !pi.getId() && ids != null && j < ids.length; j++) {
				if (name.equalsIgnoreCase(ids[j])) {
					pi.setId(true);
					break;
				}
			}

			if (pi.getClassType() != null && pi.getClassType().isPrimitive() && (pi.getId() || pi.getTrackPrimitiveNull())) {
				alPrimitive.add(pi.getName().toUpperCase());
			} else if (pi.getClassType().isArray() && pi.getClassType().getComponentType().equals(byte.class)) { // 20121001
				alPrimitive.add(pi.getName().toUpperCase());
			}
			thisOI.getPropertyInfos().add(pi);
		}
		faObjectInfo.resetPropertyInfo(thisOI);

		// this must be sorted, so that they will be in the same order used by OAObject.nulls, and created the same on all other computers
		Collections.sort(alPrimitive);
		String[] ss = new String[alPrimitive.size()];
		alPrimitive.toArray(ss);
		faObjectInfo.setPrimitiveProps(thisOI, ss);

		// 20120827 track empty hubs
		// this must be sorted, so that they will be in the same order used by OAObject.nulls, and created the same on all other computers
		Collections.sort(alHub);
		ss = new String[alHub.size()];
		alHub.toArray(ss);
		faObjectInfo.setHubProps(thisOI, ss);
		
	}

	/**
	 * Convenience wrapper around {@link #getOAObjectInfo(Class)}.
	 *
	 * @param clazz the class whose metadata is requested.
	 * @return the OAObjectInfo for the class.
	 */
	public OAObjectInfo getObjectInfo(Class<?> clazz) {
		return getOAObjectInfo(clazz);
	}

	
	
	/**
	 * Adds a link definition to the OAObjectInfo for the supplied property
	 * name, unless a link with the same name already exists.
	 *
	 * @param thisOI the OAObjectInfo to modify.
	 * @param name   the link-property name.
	 * @param clazz  the target class for ONE links; null for MANY links.
	 * @param type   link type constant from OALinkInfo.
	 */
	private void createLink(OAObjectInfo thisOI, String name, Class<?> clazz, int type) {
		for (OALinkInfo li : thisOI.getLinkInfos()) {
			if (name.equalsIgnoreCase(li.getName())) {
				return; // already exists
			}
		}
		OALinkInfo li = new OALinkInfo(name, clazz, type, false, "");
		thisOI.getLinkInfos().add(li);
	}

	// Used by initialize properties.
	// get Properties, LinkOne, LinkMany (hub)
	//     note: does not get calcProps
	/**
	 * Discovers property names for the supplied class by examining public
	 * getters and setters while filtering out helper, JAXB, and synthetic
	 * method patterns. Returns property names that have both getter and
	 * setter (or hub getter), normalized for later metadata creation.
	 *
	 * @param clazzOrig             the class to inspect.
	 * @param bIncludeSuperClasses  true to scan superclasses.
	 * @return array of discovered property names.
	 */
	private String[] getPropertyNames(Class<?> clazzOrig, boolean bIncludeSuperClasses) {
		ArrayList<String> alFound = new ArrayList<>();

		HashSet<String> hsGetter = new HashSet<>();
		HashSet<String> hsSetter = new HashSet<>();

		int cnt = 0;
		for (Class c = clazzOrig; c != null && !c.equals(OAObject.class); c = c.getSuperclass()) {
			if (cnt++ > 0 && !bIncludeSuperClasses) {
				break;
			}
			Method[] methods = c.getDeclaredMethods();
			hsGetter.clear();
			hsSetter.clear();

			for (int i = 0; i < methods.length; i++) {
				if ((methods[i].getModifiers() & Modifier.PUBLIC) == 0) {
					continue;
				}

				String methodName = methods[i].getName();
				String methodNameLowerCase = methodName.toLowerCase();
				String propertyName;

				if (methodName.length() < 3) {
					continue;
				}
				if (methodNameLowerCase.startsWith("getjaxb")) {
					continue; // ignore all jaxb methods
				}
				if (methodNameLowerCase.startsWith("setjaxb")) {
					continue; // ignore all jaxb methods
				}
				if (methodName.equals("jaxbCreate")) {
					continue; // ignore all jaxb methods
				}

				String s2 = methodName.substring(0, 3);

				boolean bGetter;
				Class[] cs = methods[i].getParameterTypes();
				if (s2.equals("get")) {
					if (cs.length > 0) {
						continue;
					}
					bGetter = true;
					storeMethod(clazzOrig, methods[i]);
					propertyName = methodName.substring(3);
				} else if (s2.startsWith("is")) {
					if (cs.length > 0) {
						continue;
					}
					bGetter = true;
					storeMethod(clazzOrig, methods[i]);
					propertyName = methodName.substring(2);
				} else if (s2.equals("set")) {
					if (cs.length != 1) {
						continue;
					}
					bGetter = false;
					storeMethod(clazzOrig, methods[i]);
					propertyName = methodName.substring(3);
				} else {
					continue;
				}

				final String propertyNameUpperCase = propertyName.toUpperCase();

				if (methods[i].getReturnType().equals(Hub.class)) {
					alFound.add(propertyName);
				} else if (bGetter) {
					if (hsSetter.contains(propertyNameUpperCase)) {
						if (!alFound.contains(propertyName)) {
							alFound.add(propertyName);
						}
					}
					hsGetter.add(propertyNameUpperCase);
				} else {
					if (hsGetter.contains(propertyNameUpperCase)) {
						if (!alFound.contains(propertyName)) {
							alFound.add(propertyName);
						}
					}
					hsSetter.add(propertyNameUpperCase);
				}
			}
		}

		// 20211103 remove helper methods for enum props *String, *Enum
		List<String> alRemove = new ArrayList();
		for (final String propName : alFound) {
			boolean bFound = false;
			String s = propName.toUpperCase();
			if (s.endsWith("STRING") || s.endsWith("ENUM")) {
				for (String s2 : alFound) {
					s2 = s2.toUpperCase();
					if (s2.equals(s)) {
						continue;
					}
					if ((s2 + "STRING").equals(s) || (s2 + "ENUM").equals(s)) {
						bFound = true;
						break;
					}
				}
				if (bFound) {
					alRemove.add(propName);
				}
			}
		}
		alFound.removeAll(alRemove);

		String[] ss = new String[alFound.size()];
		alFound.toArray(ss);
		return ss;
	}

	// used by getOAObjectInfo to combine 2 OAObjectInfo's into one.
	/**
	 * Merges metadata from a child OAObjectInfo and its parent
	 * OAObjectInfo into a new OAObjectInfo instance. Combines properties,
	 * primitive lists, link infos, calc infos, and hub properties while
	 * retaining model-level annotation settings from the child or parent.
	 *
	 * @param child  metadata derived from the subclass.
	 * @param parent metadata derived from the superclass.
	 * @return a new combined OAObjectInfo instance.
	 */
	private OAObjectInfo createCombinedObjectInfo(OAObjectInfo child, OAObjectInfo parent) {
		OAObjectInfo thisOI = new OAObjectInfo();

		OAClass oaclass = (OAClass) child.getForClass().getAnnotation(OAClass.class);
		if (oaclass == null) {
			oaclass = (OAClass) parent.getForClass().getAnnotation(OAClass.class);
		}

		if (oaclass != null) {
			thisOI.setUseDataSource(oaclass.useDataSource());
			thisOI.setLocalOnly(oaclass.localOnly());
			thisOI.setAddToCache(oaclass.addToCache());
			thisOI.setInitializeNewObjects(oaclass.initialize());
			thisOI.setDisplayName(oaclass.displayName());
		}

		// combine PropertyInfos
		List alThis = thisOI.getPropertyInfos();
		for (int x = 0; x < 2; x++) {
			ArrayList al;
			if (x == 0) {
				al = child.getPropertyInfos();
			} else {
				al = parent.getPropertyInfos();
			}

			for (int i = 0; i < al.size(); i++) {
				OAPropertyInfo pi = (OAPropertyInfo) al.get(i);

				for (int ii = 0;; ii++) {
					if (ii == alThis.size()) {
						alThis.add(pi);
						break;
					}
					OAPropertyInfo piThis = (OAPropertyInfo) alThis.get(ii);
					if (pi.getName().equalsIgnoreCase(piThis.getName())) {
						break;
					}
				}
			}
		}

		// combined primitive properties
		ArrayList<String> alPrimitive = new ArrayList<String>();
		for (String s : parent.getPrimitiveProperties()) {
			alPrimitive.add(s);
		}
		for (String s : child.getPrimitiveProperties()) {
			alPrimitive.add(s);
		}
		Collections.sort(alPrimitive);
		String[] ss = new String[alPrimitive.size()];
		alPrimitive.toArray(ss);
		faObjectInfo.setPrimitiveProps(thisOI, ss);

		// combine LinkInfos
		alThis = thisOI.getLinkInfos();
		for (int x = 0; x < 2; x++) {
			List<OALinkInfo> al;
			if (x == 0) {
				al = child.getLinkInfos();
			} else {
				al = parent.getLinkInfos();
			}

			for (OALinkInfo li : al) {
				for (int ii = 0;; ii++) {
					if (ii == alThis.size()) {
						alThis.add(li);
						break;
					}
					OALinkInfo liThis = (OALinkInfo) alThis.get(ii);
					if (li.getName().equalsIgnoreCase(liThis.getName())) {
						break;
					}
				}
			}
		}

		// combine CalcInfos
		alThis = thisOI.getCalcInfos();
		for (int x = 0; x < 2; x++) {
			ArrayList al;
			if (x == 0) {
				al = child.getCalcInfos();
			} else {
				al = parent.getCalcInfos();
			}

			for (int i = 0; i < al.size(); i++) {
				OACalcInfo ci = (OACalcInfo) al.get(i);
				for (int ii = 0;; ii++) {
					if (ii == alThis.size()) {
						alThis.add(ci);
						break;
					}
					OACalcInfo ciThis = (OACalcInfo) alThis.get(ii);
					if (ci.getName().equalsIgnoreCase(ciThis.getName())) {
						break;
					}
				}
			}
		}

		// 20120827
		String[] s1 = faObjectInfo.getHubProps(child);
		String[] s2 = faObjectInfo.getHubProps(parent);

		ss = new String[s1.length + s2.length];
		System.arraycopy(s1, 0, ss, 0, s1.length);
		System.arraycopy(s2, 0, ss, s1.length, s2.length);
		
		faObjectInfo.setHubProps(thisOI, ss);

		return thisOI;
	}

	
	/**
	 * Adds the supplied link definition to the OAObjectInfo. If a link with
	 * the same name already exists, it is removed before adding the new one.
	 *
	 * @param thisOI the OAObjectInfo to update.
	 * @param li     the link info to add.
	 */
	public void addLinkInfo(OAObjectInfo thisOI, OALinkInfo li) {
		if (li == null) {
			return;
		}

		String name = li.getName();
		if (name != null && name.length() > 0) { // see if it was already created
			for (OALinkInfo lix : thisOI.getLinkInfos()) {
				if (name.equalsIgnoreCase(lix.getName())) {
					thisOI.getLinkInfos().remove(lix);
					break;
				}
			}
		}
		thisOI.addLinkInfo(li);
	}

	/**
	 * Adds the supplied calculated-property metadata to the OAObjectInfo
	 * if it is not null.
	 *
	 * @param thisOI the OAObjectInfo to update.
	 * @param ci     the calculated-property info to add.
	 */
	public void addCalcInfo(OAObjectInfo thisOI, OACalcInfo ci) {
		if (ci != null) {
			thisOI.getCalcInfos().add(ci);
		}
	}

	/**
	 * Looks up the calculated-property metadata by name within the
	 * OAObjectInfo. The comparison is case-insensitive.
	 *
	 * @param thisOI the OAObjectInfo to search.
	 * @param name   the calculated property name.
	 * @return the matching OACalcInfo, or null if not found.
	 */
	public OACalcInfo getOACalcInfo(OAObjectInfo thisOI, String name) {
		if (thisOI == null || name == null) {
			return null;
		}
		for (OACalcInfo ci : thisOI.getCalcInfos()) {
			if (name.equalsIgnoreCase(ci.getName())) {
				return ci;
			}
		}
		return null;
	}

	/**
	 * Returns the recursive link info for the specified type (ONE or MANY).
	 * Ensures recursive-link initialization occurs only once and then caches
	 * the result in the OAObjectInfo.
	 *
	 * @param thisOI the OAObjectInfo whose recursive link is requested.
	 * @param type   link type constant from OALinkInfo.
	 * @return the recursive link info, or null if none exists.
	 */
	public OALinkInfo getRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
		boolean b = faObjectInfo.getSetRecursive(thisOI);
		try {
			return _getRecursiveLinkInfo(thisOI, type);
		} finally {
			if (!b) {
				faObjectInfo.setSetRecursive(thisOI, true);
			}
		}
	}

	/**
	 * Internal implementation for determining recursive link information.
	 * Scans link definitions for those marked as recursive whose target
	 * class equals the source class. Sets cached ONE and MANY recursive
	 * links accordingly.
	 *
	 * @param thisOI the OAObjectInfo being examined.
	 * @param type   requested link type (ONE or MANY).
	 * @return the matching recursive link info, or null.
	 */
	private OALinkInfo _getRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
		if (thisOI == null) {
			return null;
		}
		if (faObjectInfo.getSetRecursive(thisOI)) {
			if (type == OALinkInfo.ONE) {
				return faObjectInfo.getRecursiveOneLinkInfo(thisOI);
			} else {
				return faObjectInfo.getRecursiveManyLinkInfo(thisOI);
			}
		}

		if (thisOI.getForClass() == null) {
			return null;
		}

		for (OALinkInfo li : thisOI.getLinkInfos()) {
			if (!li.getUsed()) {
				continue;
			}
			if (li.getCalculated()) {
				continue;
			}
			if (!li.getRecursive()) {
				continue; // 20131009
			}
			if (li.getToClass() != null && li.getToClass().equals(thisOI.getForClass())) {
				if (li.getType() == OALinkInfo.MANY) {
					faObjectInfo.setRecursiveManyLinkInfo(thisOI, li);
					if (faObjectInfo.getRecursiveOneLinkInfo(thisOI) == null) {
						faObjectInfo.setRecursiveOneLinkInfo(thisOI, getReverseLinkInfo(li)); // 20131010 type=One are not annotated as recursive
					}
					break;
				} else {
					faObjectInfo.setRecursiveOneLinkInfo(thisOI, li);
				}
			}
		}

		if (type == OALinkInfo.ONE) {
			return faObjectInfo.getRecursiveOneLinkInfo(thisOI);
		}
		return faObjectInfo.getRecursiveManyLinkInfo(thisOI);
	}

	/**
	 * Returns the link that identifies this object’s owner, if any.
	 * A link qualifies when its reverse link exists, is used, is marked
	 * as owner, and is not a recursive self-link. Caches the result in
	 * the OAObjectInfo.
	 *
	 * @param thisOI the OAObjectInfo to examine.
	 * @return the owner link info, or null if none.
	 */
	public OALinkInfo getLinkToOwner(OAObjectInfo thisOI) {
		if (thisOI == null) {
			return null;
		}
		if (faObjectInfo.getSetLinkToOwner(thisOI)) {
			return faObjectInfo.getLinkToOwner(thisOI);
		}

		for (OALinkInfo li : thisOI.getLinkInfos()) {
			if (!li.getUsed()) {
				continue;
			}
			OALinkInfo liRev = getReverseLinkInfo(li);
			if (liRev == null || !liRev.getUsed()) {
				continue;
			}
			if (liRev.getOwner()) {
				if (!li.getToClass().equals(thisOI.getForClass())) { // make sure that it is not also a recursive link.
					faObjectInfo.setLinkToOwner(thisOI, li);
					break;
				}
			}
		}
		faObjectInfo.setSetLinkToOwner(thisOI, true);
		return faObjectInfo.getLinkToOwner(thisOI);
	}

	/**
	 * Sets the root Hub for all objects of this OAObjectInfo when
	 * the type is recursive and does not have an owner. Stores or
	 * removes the Hub from the root-hub cache.
	 *
	 * @param thisOI the OAObjectInfo to update.
	 * @param h      the root Hub to assign, or null to remove.
	 */
	public void setRootHub(OAObjectInfo thisOI, Hub h) {
		if (thisOI == null) {
			return;
		}
		if (h == null) {
			hmRootHub.remove(thisOI);
		} else {
			hmRootHub.put(thisOI, h);
		}
	}

	/**
	 * Returns the root Hub previously assigned to this OAObjectInfo,
	 * or null if none has been set.
	 *
	 * @param thisOI the OAObjectInfo whose root Hub is requested.
	 * @return the root Hub or null.
	 */
	public Hub getRootHub(OAObjectInfo thisOI) {
		if (thisOI == null) {
			return null;
		}
		return (Hub) hmRootHub.get(thisOI);
	}

	/**
	 * Attempts to cache the supplied Hub instance for the given link info.
	 * Validates cache rules, acquires the per-link write lock, and delegates
	 * to the internal cache method. Returns true if the Hub was accepted
	 * into the cache.
	 *
	 * @param li  the link info whose cache is used.
	 * @param hub the Hub instance to cache.
	 * @return true if the Hub was cached; false otherwise.
	 */
	public boolean cacheHub(OALinkInfo li, final Hub<?> hub) {
		if (li == null || hub == null || li.getCacheSize() < 1) {
			return false;
		}

		ReentrantReadWriteLock rwLock = hmLinkInfoCacheLock.computeIfAbsent(li,  k -> new ReentrantReadWriteLock());
		List alCache = hmLinkInfoCacheList.computeIfAbsent(li, k -> new ArrayList(li.getCacheSize() + 1));
		Set hsCache = hmLinkInfoCacheSet.computeIfAbsent(li, k -> new HashSet(li.getCacheSize() + 3, .85f)); 

		try {
			rwLock.writeLock().lock();
			return _cacheHub(li, hub, alCache, hsCache);
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	/**
	 * Internal implementation for adding a Hub to the link’s cache.
	 * Prevents duplicates, enforces server-side constraints, adds the
	 * Hub to both list and set structures, and trims the cache to the
	 * link’s configured maximum size.
	 *
	 * @param li      the link info owning the cache.
	 * @param hub     the Hub instance to store.
	 * @param alCache the ordered cache list.
	 * @param hsCache the membership check set.
	 * @return true if the Hub was added or already cached.
	 */
	private boolean _cacheHub(OALinkInfo li, Hub<?> hub, List alCache, Set hsCache) {
		if (hsCache.contains(hub)) {
			return true;
		}

		boolean bIsServer = callSyncIsServer();
		if (bIsServer) {
			// dont cache on server if there is not storage
			//   by returning false, it will not be stored as a weakRef
			if (!getOAObjectInfo(li.getToClass()).getSupportsStorage()) {
				return false;
			}
		}

		alCache.add(hub);
		hsCache.add(hub);

		int x = alCache.size();
		if (x > li.getCacheSize()) {
			hsCache.remove(alCache.remove(0));
		}
		return true;
	}

	// for testing
	/**
	 * Returns true if the supplied Hub is currently present in the cache
	 * associated with the given link info. Acquires the per-link read lock
	 * and checks the cached set for membership.
	 *
	 * @param li  the link info whose cache is examined.
	 * @param hub the Hub instance to check.
	 * @return true if cached; false otherwise.
	 */
	public boolean isCached(OALinkInfo li, Hub<?> hub) {
		if (li == null || hub == null) {
			return false;
		}
		ReentrantReadWriteLock rwLock = hmLinkInfoCacheLock.get(li);
		if (rwLock == null) {
			return false;
		}

		try {
			rwLock.readLock().lock();

			Set hs = hmLinkInfoCacheSet.get(li);
			return hs != null && hs.contains(hub);
		} finally {
			rwLock.readLock().unlock();
		}
	}

	/**
	 * Returns the reverse link information for the supplied link info,
	 * or null if the link has no reverse relationship.
	 *
	 * @param thisLi the link info.
	 * @return the reverse link info, or null.
	 */
	public OALinkInfo getReverseLinkInfo(OALinkInfo thisLi) {
		if (thisLi == null) {
			return null;
		}
		return thisLi.getReverseLinkInfo();
	}

	/**
	 * Returns true if the supplied link and its reverse link both have
	 * type MANY, indicating a many-to-many relationship.
	 *
	 * @param thisLi the link info to evaluate.
	 * @return true if many-to-many.
	 */
	public boolean isMany2Many(OALinkInfo thisLi) {
		OALinkInfo rli = getReverseLinkInfo(thisLi);
		return (rli != null && thisLi.getType() == OALinkInfo.MANY && rli.getType() == OALinkInfo.MANY);
	}
	/**
	 * Returns true if the supplied link and its reverse link both have
	 * type ONE, indicating a one-to-one relationship.
	 *
	 * @param thisLi the link info to evaluate.
	 * @return true if one-to-one.
	 */
	public boolean isOne2One(OALinkInfo thisLi) {
		OALinkInfo rli = getReverseLinkInfo(thisLi);
		return (rli != null && thisLi.getType() == OALinkInfo.ONE && rli.getType() == OALinkInfo.ONE);
	}

	/**
	 * Retrieves a method by name from the supplied class. Ensures that
	 * OAObjectInfo is initialized so that the method cache is populated,
	 * then performs a cached lookup.
	 *
	 * @param clazz      the class to search.
	 * @param methodName the method name.
	 * @return the matching Method, or null if not found.
	 */
	public Method getMethod(Class<?> clazz, String methodName) {
		OAObjectInfo oi = getOAObjectInfo(clazz); // this will load up the methods
		return getMethod(oi, methodName);
	}

	/**
	 * Returns the getter Method associated with the supplied link info.
	 * Looks up the reverse link, obtains the target class, and retrieves
	 * the corresponding getter method for the link name.
	 *
	 * @param li the link info.
	 * @return the getter Method, or null.
	 */
	public Method getMethod(OALinkInfo li) {
		if (li == null) {
			return null;
		}
		OALinkInfo liRev = getReverseLinkInfo(li);
		if (liRev == null) {
			return null;
		}

		OAObjectInfo oi = getOAObjectInfo(liRev.getToClass()); // this will load up the methods
		return getMethod(oi, "get" + li.getName(), 0);
	}

	/**
	 * Convenience wrapper around {@link #getMethod(OAObjectInfo, String, int)}
	 * using an argument count of -1 to indicate that any parameter count
	 * is acceptable.
	 *
	 * @param oi         the OAObjectInfo whose class is examined.
	 * @param methodName the method name to resolve.
	 * @return the matching Method, or null.
	 */
	public Method getMethod(OAObjectInfo oi, String methodName) {
		return getMethod(oi, methodName, -1);
	}
	
	/**
	 * Retrieves a method from the OAObjectInfo’s class by name and
	 * argument count. Uses cached lookup when possible, otherwise performs
	 * reflective resolution and updates the method cache.
	 *
	 * @param oi            the OAObjectInfo providing the class context.
	 * @param methodName    the method name (case-insensitive).
	 * @param argumentCount expected number of parameters, or -1 for any.
	 * @return the matching Method, or null.
	 */
	public Method getMethod(OAObjectInfo oi, String methodName, int argumentCount) {
		if (methodName == null || oi == null) {
			return null;
		}
		methodName = methodName.toUpperCase();
		final Class clazz = oi.getForClass();
		Map<String, Method> map = getClassMethodMap(clazz);
		Method method = map.get(methodName);
		if (method != null && argumentCount < 0) {
			return method;
		}
		if (method == null) {
			Set<String> set = getClassMethodNotFoundMap(clazz);
			if (set.contains(methodName)) {
				return null;
			}
		}

		boolean bRecalc = false;
		if (method != null && argumentCount >= 0) {
			Class[] cs = method.getParameterTypes();
			if (cs.length != argumentCount) {
				bRecalc = true;
				method = null;
			}
		}
		if (method == null) {
			method = OAReflect.getMethod(clazz, methodName, argumentCount);
			if (method == null) {
				if (!bRecalc) {
					getClassMethodNotFoundMap(clazz).add(methodName);
				}
				return null;
			}
			method.setAccessible(true); // 20130131
			map.put(methodName, method);
		}
		return method;
	}

	/**
	 * Retrieves a method from the OAObjectInfo’s class by name and a
	 * single parameter type. Checks cached entries first, then resolves
	 * reflectively and updates the cache.
	 *
	 * @param oi         the OAObjectInfo providing the class context.
	 * @param methodName the method name (case-insensitive).
	 * @param classParam the expected parameter type.
	 * @return the matching Method, or null.
	 */
	public Method getMethod(OAObjectInfo oi, String methodName, final Class<?> classParam) {
		if (methodName == null || oi == null) {
			return null;
		}
		methodName = methodName.toUpperCase();
		Class clazz = oi.getForClass();
		final Map<String, Method> map = getClassMethodMap(clazz);
		Method method = map.get(methodName);
		if (method != null) {
			Class[] cs = method.getParameterTypes();
			if (cs != null && cs.length == 1 && OAReflect.isEqualEvenIfWrapper(classParam, cs[0])) {
				return method;
			}
		}
		method = OAReflect.getMethod(clazz, methodName, classParam);
		if (method != null) {
			map.put(methodName, method);
		}
		return method;
	}

	/**
	 * Stores the supplied method in the per-class method cache, ensuring
	 * accessibility is enabled for reflective invocation.
	 *
	 * @param clazz  the class whose cache is updated.
	 * @param method the method to store.
	 */
	public void storeMethod(Class<?> clazz, Method method) {
		//qqqqqqqq method was protected
		Map<String, Method> map = getClassMethodMap(clazz);
		method.setAccessible(true); // 20130131
		map.put(method.getName().toUpperCase(), method);
	}

	/**
	 * Returns all cached methods associated with the OAObjectInfo’s class.
	 * Extracts the values from the per-class method map and returns them
	 * as an array.
	 *
	 * @param oi the OAObjectInfo whose methods are requested.
	 * @return array of all cached methods.
	 */
	public Method[] getAllMethods(OAObjectInfo oi) {
		Class clazz = oi.getForClass();
		Map<String, Method> map = getClassMethodMap(clazz);
		Method[] ms = new Method[map.size()];
		int i = 0;
		for (Method mx : map.values()) {
			ms[i++] = mx;
		}
		return ms;
	}

	/**
	 * Returns the return type of the getter method for the named property
	 * within the supplied OAObjectInfo. Returns null if the getter is not
	 * found.
	 *
	 * @param oi           the OAObjectInfo containing metadata.
	 * @param propertyName the property name.
	 * @return the property’s class type, or null.
	 */
	public Class getPropertyClass(OAObjectInfo oi, String propertyName) {
		Method m = getMethod(oi, "get" + propertyName, 0);
		if (m == null) {
			return null;
		}
		return m.getReturnType();
	}

	/**
	 * Returns the return type of the getter method for the named property
	 * on the supplied class. Returns null if the getter is not found.
	 *
	 * @param clazz        the class to inspect.
	 * @param propertyName the property name.
	 * @return the property’s class type, or null.
	 */
	public Class<?> getPropertyClass(Class<? extends OAObject> clazz, String propertyName) {
		Method m = getMethod(clazz, "get" + propertyName);
		if (m == null) {
			return null;
		}
		return m.getReturnType();
	}

	/**
	 * Returns the target-class type of a hub property by locating the
	 * corresponding link info. Returns null if the link is not defined.
	 *
	 * @param clazz        the class to inspect.
	 * @param propertyName the hub-property name.
	 * @return the target class for the hub, or null.
	 */
	public Class<? extends OAObject> getHubPropertyClass(Class<? extends OAObject> clazz, String propertyName) {
		OALinkInfo li = getLinkInfo(clazz, propertyName);
		if (li != null) {
			return li.getToClass();
		}
		return null;
	}

	/**
	 * Returns the link info defined for the supplied property name on the
	 * given class by retrieving the class’s OAObjectInfo and delegating to
	 * the link-info lookup.
	 *
	 * @param clazz        the class to inspect.
	 * @param propertyName the link-property name.
	 * @return the matching OALinkInfo, or null.
	 */
	public OALinkInfo getLinkInfo(Class<? extends OAObject> clazz, String propertyName) {
		OAObjectInfo oi = getOAObjectInfo(clazz);
		return getLinkInfo(oi, propertyName);
	}

	/**
	 * Returns the link info defined for the supplied property name within
	 * the given OAObjectInfo, using the OAObjectInfo’s internal lookup.
	 *
	 * @param oi           the OAObjectInfo to inspect.
	 * @param propertyName the link-property name.
	 * @return the matching OALinkInfo, or null.
	 */
	public OALinkInfo getLinkInfo(OAObjectInfo oi, String propertyName) {
		OALinkInfo li = oi.getLinkInfo(propertyName);
		return li;
	}

	/**
	 * Returns all link infos that are marked as owned within the supplied
	 * OAObjectInfo.
	 *
	 * @param oi the OAObjectInfo to inspect.
	 * @return array of owned-link infos.
	 */
	public OALinkInfo[] getOwnedLinkInfos(OAObjectInfo oi) {
		return oi.getOwnedLinkInfos();
	}

	/**
	 * Returns all link infos that are marked as owned for the class of the
	 * supplied OAObject. Delegates to {@link #getOwndedLinkInfos(OAObjectInfo)}.
	 *
	 * @param obj the OAObject whose owned links are requested.
	 * @return array of owned-link infos.
	 */
	public OALinkInfo[] getOwnedLinkInfos(OAObject obj) {
		OAObjectInfo oi = getOAObjectInfo(obj);
		return oi.getOwnedLinkInfos();
	}

	/**
	 * Finds the link info whose reference on the supplied object matches
	 * the provided Hub instance. Scans all used link infos and compares
	 * raw references retrieved from the object.
	 *
	 * @param oi         the OAObjectInfo describing the object.
	 * @param fromObject the object whose links are examined.
	 * @param hub        the Hub instance to match.
	 * @return the associated link info, or null.
	 */
	public OALinkInfo getLinkInfo(OAObjectInfo oi, OAObject fromObject, Hub<?> hub) {
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (!li.getUsed()) {
				continue;
			}
			String s = li.getName();

			Object objx = callReflectGetRawReference(fromObject, s);
			if (objx == hub) {
				return li;
			}
		}
		return null;
	}

	/**
	 * Returns the link info that points from the source class to the
	 * target class by retrieving the source class’s OAObjectInfo and
	 * delegating to the class-level lookup.
	 *
	 * @param fromClass the source class.
	 * @param toClass   the target class.
	 * @return the matching link info, or null.
	 */
	public OALinkInfo getLinkInfo(Class<? extends OAObject> fromClass, Class<? extends OAObject> toClass) {
		OAObjectInfo oi = getOAObjectInfo(fromClass);
		return getLinkInfo(oi, toClass);
	}

	/**
	 * Returns the link info within the supplied OAObjectInfo whose target
	 * class matches the provided class. Only used link infos are examined.
	 *
	 * @param oi      the OAObjectInfo to inspect.
	 * @param toClass the target class.
	 * @return the matching link info, or null.
	 */
	public OALinkInfo getLinkInfo(OAObjectInfo oi, Class toClass) {
		if (oi == null || toClass == null) return null;
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (!li.getUsed()) {
				continue;
			}
			if (toClass.equals(li.getToClass())) {
				return li;
			}
		}
		return null;
	}

	/**
	 * Returns the OAPropertyInfo for the named property from the supplied
	 * OAObjectInfo, using its internal lookup method.
	 *
	 * @param oi           the OAObjectInfo containing metadata.
	 * @param propertyName the property name.
	 * @return the property info, or null.
	 */
	public OAPropertyInfo getPropertyInfo(OAObjectInfo oi, String propertyName) {
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		return pi;
	}

	/**
	 * Returns true if the supplied property name is listed among the
	 * OAObjectInfo's ID properties. Comparison is case-insensitive.
	 *
	 * @param oi           the OAObjectInfo to inspect.
	 * @param propertyName the property name.
	 * @return true if the property is an ID property.
	 */
	public boolean isIdProperty(OAObjectInfo oi, String propertyName) {
		
		String[] ss = oi.getIdProperties();
		for (int i = 0; ss != null && i < ss.length; i++) {
			if (ss[i] != null && ss[i].equalsIgnoreCase(propertyName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the supplied property info represents a primitive
	 * Java type. Validates that its class type is non-null and primitive.
	 *
	 * @param pi the property info.
	 * @return true if the property is primitive.
	 */
	public boolean isPrimitive(OAPropertyInfo pi) {
		return (pi != null && pi.getClassType() != null && pi.getClassType().isPrimitive());
	}

	/**
	 * Returns true if the named property is a primitive type. Looks up the
	 * OAPropertyInfo and checks the underlying Java class for primitiveness.
	 *
	 * @param oi           the OAObjectInfo containing metadata.
	 * @param propertyName the property name.
	 * @return true if the property is primitive.
	 */
	public boolean isPrimitiveProperty(OAObjectInfo oi, String propertyName) {
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi != null) {
			Class c = pi.getClassType();
			return (c != null && c.isPrimitive());
		}
		return false;
	}

	/**
	 * Returns true if the named property is a Hub property. Resolves the
	 * getter method and verifies that its return type is Hub.
	 *
	 * @param oi           the OAObjectInfo containing metadata.
	 * @param propertyName the property name.
	 * @return true if the property is a Hub property.
	 */
	public boolean isHubProperty(OAObjectInfo oi, String propertyName) {
		Method m = getMethod(oi.getForClass(), "get" + propertyName);
		if (m == null) return false;
		
		Class c = m.getReturnType();
		if (c == null) return false;
		return (c.equals(Hub.class));
	}

	/**
	 * Returns an array of ID property values for the supplied OAObject.
	 * Retrieves the ID-property list from the OAObjectInfo and extracts
	 * each value using raw property reflection.
	 *
	 * @param oaObj the OAObject whose ID values are requested.
	 * @return array of ID values; empty array if none; null if object is null.
	 */
	public Object[] getPropertyIdValues(OAObject oaObj) {
		if (oaObj == null) {
			return null;
		}
		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());
		String[] ids = oi.getIdProperties();
		if (ids == null) return new Object[0];
		Object[] objs = new Object[ids.length];
		for (int i = 0; i < ids.length; i++) {
			objs[i] = callReflectGetProperty(oaObj, ids[i]);
		}
		return objs;
	}

	/**
	 * Returns the null-bitmask array from the supplied OAObject, or null
	 * if the object is null. The bitmask indicates which primitive
	 * properties are currently null.
	 *
	 * @param oaObj the OAObject to inspect.
	 * @return the object's null-bitmask array, or null.
	 */
	public byte[] getNullBitMask(OAObject oaObj) {
		if (oaObj == null) {
			return null;
		}
		return faObject.getNulls(oaObj);
	}

	/**
	 * Returns a list of primitive property names for the supplied OAObject
	 * class that support null tracking. Delegates to the OAObjectInfo to
	 * retrieve the primitive-property list.
	 *
	 * @param clazz the OAObject class to inspect.
	 * @return list of primitive property names, or null if class is null.
	 */
	public List<String> getPrimitiveNullPropertyNames(Class<? extends OAObject> clazz) {
		if (clazz == null) {
			return null;
		}
		OAObjectInfo oi = getOAObjectInfo(clazz);

		String[] ss = oi.getPrimitiveProperties();
		return Arrays.asList(ss);
	}

	/**
	 * Returns a list of primitive property names whose null bit is set on
	 * the supplied OAObject. Determines bit positions using the OAObjectInfo’s
	 * primitive property list and inspects the object's null-bitmask.
	 *
	 * @param oaObj the OAObject to inspect.
	 * @return list of primitive property names marked as null, or null.
	 */
	public List<String> getPrimitiveNullProperties(OAObject oaObj) {
		if (oaObj == null) {
			return null;
		}
		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());

		List<String> al = new ArrayList<>();

		String[] ss = oi.getPrimitiveProperties();
		for (int i = 0; i < ss.length; i++) {

			int posByte = (i / 8);
			int posBit = 7 - (i % 8);
			
			byte[] bs = faObject.getNulls(oaObj);
			if (posByte >= bs.length) {
				break;
			}
			byte b = bs[posByte];

			byte b2 = 1;
			b2 = (byte) (b2 << posBit);
			b = (byte) ((byte) b & (byte) b2);

			if (b != 0) {
				al.add(ss[i]);
			}
		}

		return al;
	}

	/**
	 * Convenience wrapper around {@link #isPrimitiveNull(OAObject, String)}
	 * that returns whether the specified primitive property is null.
	 *
	 * @param oaObj        the OAObject to inspect.
	 * @param propertyName the property name.
	 * @return true if the primitive property is null.
	 */
	public boolean getPrimitiveNull(OAObject oaObj, String propertyName) {
		return isPrimitiveNull(oaObj, propertyName);
	}

	/**
	 * Returns true if the specified primitive property on the supplied
	 * object is marked as null in the object's null-bitmask. Validates that
	 * the property supports null-tracking and checks its assigned bit.
	 *
	 * @param oaObj        the OAObject to inspect.
	 * @param propertyName the property name (case-insensitive).
	 * @return true if the primitive property is null; false otherwise.
	 */
	public boolean isPrimitiveNull(OAObject oaObj, String propertyName) {
		if (oaObj == null || propertyName == null) {
			return false;
		}
		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());

		propertyName = propertyName.toUpperCase();
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi == null || !pi.getIsPrimitive() || (!pi.getTrackPrimitiveNull() && !pi.getId())) {
			return false;
		}

		String[] ss = oi.getPrimitiveProperties();
		for (int i = 0; i < ss.length; i++) {
			int x = propertyName.compareTo(ss[i]);
			if (x < 0) {
				break; // list is sorted
			}
			if (x != 0) {
				continue;
			}
			int posByte = (i / 8);
			int posBit = 7 - (i % 8);
			byte[] bs = faObject.getNulls(oaObj);
			if (posByte >= bs.length) {
				return false;
			}
			byte b = bs[posByte];

			byte b2 = 1;
			b2 = (byte) (b2 << posBit);
			b = (byte) ((byte) b & (byte) b2);

			return b != 0;
		}
		return false;
	}

	/**
	 * Sets or clears the null-bit for the specified primitive property on
	 * the supplied object. Computes the bit position based on the
	 * OAObjectInfo’s primitive-property list and updates the object's
	 * null-bitmask accordingly.
	 *
	 * @param oaObj        the OAObject whose bitmask is modified.
	 * @param propertyName the property name (case-insensitive).
	 * @param bSetToNull   true to mark the property as null; false to clear.
	 */
	public void setPrimitiveNull(OAObject oaObj, String propertyName, boolean bSetToNull) {
		if (oaObj == null || propertyName == null) {
			return;
		}

		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());
		propertyName = propertyName.toUpperCase();
		String[] ss = oi.getPrimitiveProperties();
		for (int i = 0; i < ss.length; i++) {
			int x = propertyName.compareTo(ss[i]);
			if (x < 0) {
				break; // list is sorted
			}
			if (x != 0) {
				continue;
			}

			byte[] bs = faObject.getNulls(oaObj);
			int posByte = (i / 8);
			if (posByte >= bs.length) {
				continue;
			}

			byte b = bs[posByte];
			int posBit = 7 - (i % 8);

			byte b2 = (byte) 1;
			b2 = (byte) (b2 << posBit);
			if (bSetToNull) {
				b |= b2;
			} else {
				b &= ~b2;
			}
			bs[posByte] = b;
			break;
		}
	}

	/*
	 * NOTE: 20100930 I started this to use for reversing from TreeNode to get path to top/root
	 * this wont work, unless the parent nodes are also used
	 * Take a property path that is "to" a class, and reverse it.
	 * Example: from a X class, the propPath "dept.manager.address.zipCode"
	 * where address.class would be the clazz; would return "manager.dept", used to get from an address to the dept.
	 */

	/**
	 * Reverses a property path by attempting to follow reverse link
	 * definitions from the supplied class. Tokenizes the path, builds a
	 * reversed version, then resolves each component through link
	 * relationships. Returns null if the reverse path cannot be
	 * determined.
	 *
	 * @param clazz        the starting class.
	 * @param propertyPath the forward property path.
	 * @return the reversed property path, or null.
	 */
	public String reversePath(Class<? extends OAObject> clazz, String propertyPath) {
		String revPropertyPath = "";
		StringTokenizer st = new StringTokenizer(propertyPath, ".");
		for (int i = 0; st.hasMoreTokens(); i++) {
			String value = st.nextToken();
			if (i > 0) {
				revPropertyPath = "." + revPropertyPath;
			}
			revPropertyPath = value + revPropertyPath;
		}

		propertyPath = revPropertyPath;
		revPropertyPath = "";
		st = new StringTokenizer(propertyPath, ".");
		for (int i = 0; st.hasMoreTokens(); i++) {
			String value = st.nextToken();

			OAObjectInfo oi = getOAObjectInfo(clazz);

			boolean bFound = false;
			for (OALinkInfo li : oi.getLinkInfos()) {
				OALinkInfo liRev = getReverseLinkInfo(li);
				if (liRev == null) continue;
				if (value.equalsIgnoreCase(liRev.getName())) {
					if (clazz.equals(liRev.getToClass())) {
						if (revPropertyPath.length() > 0) {
							revPropertyPath = "." + revPropertyPath;
						}
						revPropertyPath = li.getName() + revPropertyPath;
						clazz = li.getToClass();
						bFound = true;
						break;
					}
				}
			}
			if (bFound) {
				continue;
			}

			if (i == 0) { // could be a property, which is discarded
				if (getPropertyInfo(oi, value) != null) {
					continue;
				}
			}

			revPropertyPath = null;
			break;
		}

		return revPropertyPath;
	}

	/**
	 * Returns true if the supplied object is weak-referenceable based on
	 * its OAObjectInfo. Delegates to the OAObjectInfo-level evaluation.
	 *
	 * @param oaObj the OAObject to check.
	 * @return true if weak-referenceable.
	 */
	public boolean isWeakReferenceable(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		OAObjectInfo oi = getObjectInfo(oaObj);
		return isWeakReferenceable(oi, null);
	}

	/**
	 * Returns true if any parent link configuration indicates that objects
	 * of this type may be weak-referenceable. Delegates to the internal
	 * recursive evaluation.
	 *
	 * @param oi the OAObjectInfo to check.
	 * @return true if weak-referenceable.
	 */
	public boolean isWeakReferenceable(OAObjectInfo oi) {
		if (oi == null) {
			return false;
		}
		return isWeakReferenceable(oi, null);
	}

	/**
	 * Recursive implementation used to determine whether a type is
	 * weak-referenceable. Examines reverse links for MANY relationships
	 * with positive cache sizes and checks parent references while
	 * preventing cycles using a visited-set.
	 *
	 * @param oi         the OAObjectInfo being evaluated.
	 * @param hsVisited  set of already-visited OAObjectInfos.
	 * @return true if weak-referenceable.
	 */
	private boolean isWeakReferenceable(final OAObjectInfo oi, HashSet<OAObjectInfo> hsVisited) {
		if (oi == null) {
			return false;
		}
		
		int x = faObjectInfo.getWeakReferenceable(oi);
		if (x != -1) {
			return (x == 1);
		}
		if (hsVisited != null && hsVisited.contains(oi)) {
			return false;
		}

		boolean b = false;
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev == null) {
				continue;
			}
			if (liRev.getPrivateMethod()) {
				continue;
			}
			if (!liRev.getUsed()) {
				continue;
			}
			if (liRev.getType() != liRev.MANY) {
				continue;
			}
			if (liRev.getCacheSize() > 0) {
				b = true;
				break;
			}

			if (hsVisited == null) {
				hsVisited = new HashSet<OAObjectInfo>();
			}
			hsVisited.add(oi);
			OAObjectInfo oix = getObjectInfo(li.getToClass());
			b = isWeakReferenceable(oix, hsVisited);
			if (b) {
				break;
			}
		}
		faObjectInfo.setWeakReferenceable(oi, b ? 1 : 0);
		return b;
	}

	/**
	 * Returns true if the supplied OAObjectInfo is configured to use a
	 * singleton Pojo, either directly or via owner-link traversal.
	 *
	 * @param oi the OAObjectInfo to inspect.
	 * @return true if the type uses a singleton Pojo.
	 */
	public boolean isPojoSingleton(final OAObjectInfo oi) {
		if (oi == null) {
			return false;
		}

		if (oi.getSingleton() || (!oi.getNoPojo() && oi.getPojoSingleton())) {
			return true;
		}

		return isPojoSingleton2(oi);
	}
	
	/**
	 * Recursive implementation that walks the owner-link chain to
	 * determine whether any owner type is configured as a singleton Pojo.
	 *
	 * @param oi the OAObjectInfo being evaluated.
	 * @return true if a singleton Pojo is found in the chain.
	 */
	private boolean isPojoSingleton2(final OAObjectInfo oi) {
		if (oi == null) {
			return false;
		}

		OALinkInfo lix = getLinkToOwner(oi);
		if (lix == null) {
			return false;
		}

		OAObjectInfo oiOwner = lix.getToObjectInfo();

		if (oiOwner.getSingleton() || (!oiOwner.getNoPojo() && oiOwner.getPojoSingleton())) {
			return true;
		}
		if (!lix.getReverseLinkInfo().isOne()) {
			return false;
		}
		return isPojoSingleton2(oiOwner);
	}

	/**
	 * Returns the method-cache map for the supplied class, creating it if
	 * necessary. The cache stores methods keyed by their uppercase names.
	 *
	 * @param clazz the class whose method cache is requested.
	 * @return the method cache map.
	 */
	public Map<String, Method> getClassMethodMap(Class<?> clazz) {
    	//qqqqqqqqqq method was protected
		Map<String, Method> map = hmClassMethod.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
    	return map;
	}

	/**
	 * Returns the per-class set used to record method names that were
	 * previously searched for but not found. Creates the set if it does
	 * not already exist.
	 *
	 * @param clazz the class whose not-found map is requested.
	 * @return the not-found method-name set.
	 */
    public Set<String> getClassMethodNotFoundMap(Class<?> clazz) {
    	//qqqqqqqqqq method was protected
        Set<String> map = hmClassMethodNotFound.computeIfAbsent(clazz, k -> new HashSet<String>(3, .75f));
        return map;
    }

    /**
     * Returns the global map that associates each Class with its
     * OAObjectInfo instance. This is the shared cache used for all
     * metadata lookups.
     *
     * @return the Class-to-OAObjectInfo map.
     */
    public Map<Class, OAObjectInfo> getObjectInfoMap() {
    	return hmObjectInfo;
    }
    
	// @OAParentProvided (example = "srvcObject.getOAObjectAnnotationService().update2")
	public abstract void callAnnotationUpdate2(OAObjectInfo oi, Class<?> clazz);
    
	// @OAParentProvided (example = "srvcObject.getOAObjectAnnotationService().updateImportMatches")
	public abstract void callAnnotationUpdateImportMatches(OAObjectInfo oi); 

	// @OAParentProvided (example = "srvcObject.getOAObjectAnnotationService().updateLinkFkeys")
	public abstract void callAnnotationUpdateLinkFkeys(final OAObjectInfo oi);
	
	// @OAParentProvided (example = "srvcObject.getOAObjectAnnotationService().update")
	public abstract void callAnnotationUpdate(OAObjectInfo oi, Class<?> clazz); 
	
	// @OAParentProvided (example = "srvcObject.getOAObjectReflectService().getRawReference")
	public abstract Object callReflectGetRawReference(OAObject oaObj, String name);
	
	// @OAParentProvided (example = "srvcObject.getOAObjectReflectService().getProperty")
	public abstract Object callReflectGetProperty(OAObject oaObj, String propPath);
	
	// @OAParentProvided (example = "srvcSync.isServer(..)")
	public abstract boolean callSyncIsServer();
}
