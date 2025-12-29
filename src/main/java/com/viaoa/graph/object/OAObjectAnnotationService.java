package com.viaoa.graph.object;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAMethod;
import com.viaoa.annotation.OAObjCallback;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATriggerMethod;
import com.viaoa.graph.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.object.OAAnnotationDelegate;
import com.viaoa.object.OACalcInfo;
import com.viaoa.object.OAFkeyInfo;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAMethodInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectModel;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OATriggerDelegate;
import com.viaoa.object.OATriggerListener;
import com.viaoa.object.OATriggerMethodListener;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAStr;
import com.viaoa.util.OAString;

public class OAObjectAnnotationService {
	private static final Logger LOG = Logger.getLogger(OAObjectAnnotationService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;
	private final OAObjectInfo.FriendAccess faObjectInfo;
	
    public OAObjectAnnotationService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess, OAObjectInfo.FriendAccess oaObjectInfoFriendAccess) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService cant be null");
    	this.srvcObject = srvcObject;
    	if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faObject = oaObjectFriendAccess;
    	if (oaObjectInfoFriendAccess == null) throw new IllegalArgumentException("OAObjectInfoFriendAccess can not be null");
    	this.faObjectInfo = oaObjectInfoFriendAccess;
    }
	
    public OAObjectService getObjectService() {
    	return srvcObject;
    }
    
	/**
	 * Updates the specified {@link OAObjectInfo} by processing all OA-related
	 * annotations declared on the given class and its superclasses (excluding
	 * {@link OAObject} itself).
	 * <p>
	 * Each annotation type is applied once per class hierarchy level, using an
	 * internal tracking set to prevent duplicate processing.
	 *
	 * @param oi    the metadata object to update
	 * @param clazz the class whose annotations will be processed
	 */
	public void update(OAObjectInfo oi, Class clazz) {
		HashSet<String> hs = new HashSet<String>();
		for (; clazz != null;) {
			if (OAObject.class.equals(clazz)) {
				break;
			}
			_update(oi, clazz, hs);
			clazz = clazz.getSuperclass();
		}
	}

	/**
	 * Performs a second-phase annotation update on the specified {@link OAObjectInfo},
	 * processing {@link OATriggerMethod} annotations declared on the class and its
	 * superclasses (excluding {@link OAObject}).
	 *
	 * @param oi    the metadata object to update
	 * @param clazz the class whose trigger annotations will be processed
	 */
	public void update2(OAObjectInfo oi, Class clazz) {
		for (; clazz != null;) {
			if (OAObject.class.equals(clazz)) {
				break;
			}
			_update2(oi, clazz);
			clazz = clazz.getSuperclass();
		}
	}

	/**
	 * Internal helper that processes all annotation types declared on the given class
	 * and applies their metadata to the specified {@link OAObjectInfo}.
	 * <p>
	 * This method updates class-level settings, ID properties, regular properties,
	 * foreign-key properties, calculated properties, link definitions, Many/One
	 * relationships, annotated methods, and callback registrations. A tracking set
	 * prevents duplicate processing within the class hierarchy.
	 *
	 * @param oi    the metadata object being updated
	 * @param clazz the class whose annotations are processed
	 * @param hs    a set used to track processed annotation categories
	 */
	private void _update(final OAObjectInfo oi, final Class clazz, HashSet<String> hs) {
		String s;

		if (!hs.contains("OAClass")) {
			OAClass oaclass = (OAClass) clazz.getAnnotation(OAClass.class);
			if (oaclass != null) {
				hs.add("OAClass");
				oi.setName(clazz.getSimpleName());
				oi.setUseDataSource(oaclass.useDataSource());
				oi.setLocalOnly(oaclass.localOnly());
				oi.setAddToCache(oaclass.addToCache());
				oi.setInitializeNewObjects(oaclass.initialize());

				oi.setDisplayName(oaclass.displayName());

				String[] pps = oaclass.rootTreePropertyPaths();
				oi.setRootTreePropertyPaths(pps);
				oi.setLookup(oaclass.isLookup());
				oi.setProcessed(oaclass.isProcessed());
				oi.setPluralName(oaclass.pluralName());
				
	            s = oaclass.lowerName();
	            if (OAStr.isEmpty(s)) s = OAString.mfcl(clazz.getSimpleName()); 
	            oi.setLowerName(s);

				oi.setSoftDeleteProperty(oaclass.softDeleteProperty());
				oi.setSoftDeleteReasonProperty(oaclass.softDeleteProperty());
				oi.setVersionProperty(oaclass.versionProperty());
				oi.setVersionLinkProperty(oaclass.versionLinkProperty());
				oi.setTimeSeriesProperty(oaclass.timeSeriesProperty());
                oi.setFreezeProperty(oaclass.freezeProperty());

				oi.setSingleton(oaclass.singleton());
				oi.setPojoSingleton(oaclass.pojoSingleton());
				oi.setNoPojo(oaclass.noPojo());
				oi.setJsonUsesCapital(oaclass.jsonUsesCapital());
			}
			OAObjCallback eq = (OAObjCallback) clazz.getAnnotation(OAObjCallback.class);
			if (eq != null) {
				oi.setEnabledProperty(eq.enabledProperty());
				oi.setEnabledValue(eq.enabledValue());
				oi.setVisibleProperty(eq.visibleProperty());
				oi.setVisibleValue(eq.visibleValue());
				oi.setContextEnabledProperty(eq.contextEnabledProperty());
				oi.setContextEnabledValue(eq.contextEnabledValue());
				oi.setContextVisibleProperty(eq.contextVisibleProperty());
				oi.setContextVisibleValue(eq.contextVisibleValue());
				oi.setViewDependentProperties(eq.viewDependentProperties());
				oi.setContextDependentProperties(eq.contextDependentProperties());
			}
		}
		// prop ids
		final Method[] methods = clazz.getDeclaredMethods();
		String[] ss = oi.getIdProperties();
		for (Method m : methods) {
			OAId oaid = m.getAnnotation(OAId.class);
			if (oaid == null) {
				continue;
			}
			OAProperty oaprop = (OAProperty) m.getAnnotation(OAProperty.class);
			if (oaprop == null) {
				String sx = "annotation OAId - should also have OAProperty annotation";
				LOG.log(Level.WARNING, sx, new Exception(sx));
			}
			s = getPropertyName(m.getName());
			if (hs.contains("OAId." + s)) {
				continue;
			}
			hs.add("OAId." + s);

			int pos = oaid.pos();

			if (ss == null) {
				ss = new String[pos + 1];
			} else {
				if (pos >= ss.length) {
					int x = Math.max(pos, ss.length);
					String[] ss2 = new String[x + 1];
					System.arraycopy(ss, 0, ss2, 0, ss.length);
					ss = ss2;
				}
				if (ss[pos] != null) {
					if (!ss[pos].equalsIgnoreCase(s)) {
						String sx = "annotation OAId - duplicate pos for property id";
						LOG.log(Level.WARNING, sx, new Exception(sx));
					}
				}
			}
			ss[pos] = s;
		}

		for (String sx : ss) {
			if (sx == null) {
				sx = "annotation OAId - missing pos for property id(s)";
				LOG.log(Level.WARNING, sx, new Exception(sx));
			}
		}
		faObjectInfo.setPropertyIds(oi, ss);

		/* used for debugging
		if (clazz.getName().equals("com.cdi.model.oa.SalesOrder")) {
		    int xx = 4;
		    xx++;
		}
		*/
		// properties
		for (Method m : methods) {
			OAProperty oaprop = (OAProperty) m.getAnnotation(OAProperty.class);
			if (oaprop == null) {
				continue;
			}
			final String name = getPropertyName(m.getName());
			if (hs.contains("prop." + name)) {
				continue;
			}
			hs.add("prop." + name);

			OAPropertyInfo pi = oi.getPropertyInfo(name);
			if (pi == null) {
				pi = new OAPropertyInfo();
				pi.setName(name);
				oi.addPropertyInfo(pi);
			}

			s = oaprop.lowerName();
            if (OAStr.isEmpty(s)) s = OAString.mfcl(name); 
            pi.setLowerName(s);

			pi.setImportMatch(oaprop.importMatch());

			pi.setFkeyOnly(oaprop.isFkeyOnly());
			
			s = oaprop.displayName();
			if (OAString.isEmpty(s)) {
				s = OAString.getDisplayName(name);
			}
			pi.setDisplayName(s);

			s = oaprop.enumPropertyName();
			if (!OAString.isEmpty(s)) {
				pi.setEnumPropertyName(s);
			}

			pi.setDisplayLength(oaprop.displayLength());
            pi.setMinLength(oaprop.minLength());
            pi.setMaxLength(oaprop.maxLength());

			s = oaprop.uiColumnName();
			if (OAString.isEmpty(s)) {
				s = oaprop.columnName();
			}
			pi.setUIColumnName(s); // for UI grid/table header

			int x = oaprop.uiColumnLength();
			if (x == 0) {
				x = oaprop.columnLength();
			}
			pi.setUIColumnLength(x); // for UI grid/table header

			pi.setRequired(oaprop.required());
			pi.setDecimalPlaces(oaprop.decimalPlaces());
			pi.setId(m.getAnnotation(OAId.class) != null);
			pi.setUnique(oaprop.isUnique());
			pi.setProcessed(oaprop.isProcessed());
			pi.setHtml(oaprop.isHtml());
			pi.setJson(oaprop.isJson());
			pi.setTimestamp(oaprop.isTimestamp());
			pi.setSubmit(oaprop.isSubmit());
			pi.setObjectStatus(oaprop.isObjectStatus());
			pi.setIgnoreTimeZone(oaprop.ignoreTimeZone());
			pi.setTimeZonePropertyPath(oaprop.timeZonePropertyPath());

			pi.setClassType(m.getReturnType());
			pi.setTrackPrimitiveNull(pi.getIsPrimitive() && oaprop.trackPrimitiveNull() && !oaprop.isFkeyOnly());

			pi.setFormat(oaprop.format());

			OAColumn oacol = (OAColumn) m.getAnnotation(OAColumn.class);
			if (oacol != null) {
				pi.setOAColumn(oacol);
				x = oacol.maxLength();
				if (x > 0) {
					pi.setMaxLength(x);
				}
			}

			OAId oaid = (OAId) m.getAnnotation(OAId.class);
			if (oaid != null) {
				pi.setAutoAssign(oaid.autoAssign());
			}

			boolean b = oaprop.isBlob();
			if (b) {
				b = false;
				Class c = m.getReturnType();
				if (c.isArray()) {
					c = c.getComponentType();
					if (c.equals(byte.class)) {
						b = true;
					}
				}
			}
			pi.setBlob(b);
			pi.setNameValue(oaprop.isNameValue());
			pi.setUnicode(oaprop.isUnicode());

			pi.setEncrypted(oaprop.isEncrypted());
			pi.setSHAHash(oaprop.isSHAHash() || oaprop.isPassword());

			pi.setUpper(oaprop.isUpper());
			pi.setLower(oaprop.isLower());

			pi.setSensitiveData(oaprop.sensitiveData());

			pi.setCurrency(oaprop.isCurrency());
			pi.setOAProperty(oaprop);

			if (oaprop.isNameValue()) {
				// 20220803, 20220917 populate hubs
				Hub<String> h = pi.getNameValues();
				Hub<String> h2 = pi.getDisplayNameValues();
				try {
				    Method mx = OAReflect.getMethod(clazz, "get" + pi.getName()+"Enum");
				    Class cz;
				    if (mx != null) cz = mx.getReturnType();
				    else cz = Class.forName(clazz.getName() + "$" + pi.getName());
					// Field[] allFields = cz.getDeclaredFields();

					Method mz = cz.getMethod("values", new Class[] {});
					Object objzs = mz.invoke(null, (Object[]) null);
					int xz = Array.getLength(objzs);
					Method displayMethod = null;
					boolean bDisplayMethod = false;
					for (int i = 0; i < xz; i++) {
					    if (i == 0 && h.size() > 0) break;
						Object objz = Array.get(objzs, i);

						h.add(objz.toString());
						if (displayMethod == null && !bDisplayMethod) {
							bDisplayMethod = true;
							try {
								displayMethod = objz.getClass().getMethod("getDisplay", new Class[] {});
							} catch (Exception e) {
							}
						}
						if (displayMethod != null) {
							Object objxs = displayMethod.invoke(objz, (Object[]) null);
							h2.add(objxs.toString());
						} else {
							h2.add(objz.toString());
						}
					}
				} catch (Exception e) {
					System.out.println("OAAnnotationDelegate exception loading enum names for class.property " + clazz.getSimpleName() + "."
							+ pi.getName() + ", exception: " + e + ", will continue");
					// e.printStackTrace();
				}
			}

			pi.setNoPojo(oaprop.noPojo());
			pi.setPojoKeyPos(oaprop.pojoKeyPos());

			OAObjCallback eq = (OAObjCallback) m.getAnnotation(OAObjCallback.class);
			if (eq != null) {
				pi.setEnabledProperty(eq.enabledProperty());
				pi.setEnabledValue(eq.enabledValue());
				pi.setVisibleProperty(eq.visibleProperty());
				pi.setVisibleValue(eq.visibleValue());
				pi.setContextEnabledProperty(eq.contextEnabledProperty());
				pi.setContextEnabledValue(eq.contextEnabledValue());
				pi.setContextVisibleProperty(eq.contextVisibleProperty());
				pi.setContextVisibleValue(eq.contextVisibleValue());
				pi.setViewDependentProperties(eq.viewDependentProperties());
				pi.setContextDependentProperties(eq.contextDependentProperties());
			}
		}

		// fkey properties
		for (Method m : methods) {
			OAColumn oacol = (OAColumn) m.getAnnotation(OAColumn.class);
			if (oacol == null) {
				continue;
			}

			// fkey properties are in with the oi.propertyInfo
			//     they are created based get/set methods that are not links
			OAProperty oaprop = (OAProperty) m.getAnnotation(OAProperty.class);
			if (oaprop != null) {
				continue;
			}

			String name = getPropertyName(m.getName());

			if (hs.contains("fkprop." + name)) {
				continue;
			}
			hs.add("fkprop." + name);

			OAPropertyInfo pi = oi.getPropertyInfo(name);
			if (pi == null) {
				continue;
			}

			pi.setOAColumn(oacol);
			int x = oacol.maxLength();
			if (x > 0) {
				pi.setMaxLength(x);
			}
		}

		// calcProperties
		ArrayList<OACalcInfo> alCalc = oi.getCalcInfos();
		for (Method m : methods) {
			OACalculatedProperty annotation = (OACalculatedProperty) m.getAnnotation(OACalculatedProperty.class);
			if (annotation == null) {
				continue;
			}

			boolean bHub = false;
			Class[] cs = m.getParameterTypes();
			if (cs != null && cs.length == 1 && Hub.class.equals(cs[0])) {
				if (Modifier.isStatic(m.getModifiers())) {
					bHub = true;
				}
			}

			String name = getPropertyName(m.getName(), false);
			if (hs.contains("calc." + name)) {
				continue;
			}
			hs.add("calc." + name);

			OACalcInfo ci = OAObjectInfoDelegate.getOACalcInfo(oi, name);
			if (ci == null) {
				ci = new OACalcInfo(name, annotation.properties(), bHub);
				oi.addCalcInfo(ci);
			} else {
				ci.setDependentProperties(annotation.properties());
			}
			
            s = annotation.lowerName();
            if (OAStr.isEmpty(s)) s = OAString.mfcl(name); 
            ci.setLowerName(s);
			ci.setOACalculatedProperty(annotation);
			ci.setClassType(m.getReturnType());
			ci.setHtml(annotation.isHtml());
			ci.setObjectStatus(annotation.isObjectStatus());

			OAObjCallback eq = (OAObjCallback) m.getAnnotation(OAObjCallback.class);
			if (eq != null) {
				ci.setEnabledProperty(eq.enabledProperty());
				ci.setEnabledValue(eq.enabledValue());
				ci.setVisibleProperty(eq.visibleProperty());
				ci.setVisibleValue(eq.visibleValue());
				ci.setContextEnabledProperty(eq.contextEnabledProperty());
				ci.setContextEnabledValue(eq.contextEnabledValue());
				ci.setContextVisibleProperty(eq.contextVisibleProperty());
				ci.setContextVisibleValue(eq.contextVisibleValue());
				ci.setViewDependentProperties(eq.viewDependentProperties());
				ci.setContextDependentProperties(eq.contextDependentProperties());
			}
			ci.setObjectCallbackMethod(m);
		}

		// linkInfos
		List<OALinkInfo> alLinkInfo = oi.getLinkInfos();
		// Ones
		for (Method m : methods) {
			OAOne annotation = (OAOne) m.getAnnotation(OAOne.class);
			if (annotation == null) {
				continue;
			}
			Class c = m.getReturnType();

			String name = getPropertyName(m.getName(), false);
			if (hs.contains("link." + name)) {
				continue;
			}
			hs.add("link." + name);

			OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, name);
			if (li == null) {
				li = new OALinkInfo(name, m.getReturnType(), OALinkInfo.ONE);
				oi.addLinkInfo(li);
			}

			li.setImportMatch(annotation.importMatch());

            s = annotation.lowerName();
            if (OAStr.isEmpty(s)) s = OAString.mfcl(name); 
            li.setLowerName(s);

			s = annotation.displayName();
			if (OAString.isEmpty(s)) {
				s = OAString.getDisplayName(name);
			}
			li.setDisplayName(s);

			li.setRequired(annotation.required());
			li.setCalcDependentProperties(annotation.calcDependentProperties());
			li.setCascadeSave(annotation.cascadeSave());
			li.setCascadeDelete(annotation.cascadeDelete());
			li.setReverseName(annotation.reverseName());
			li.setOwner(annotation.owner());
			li.setAutoCreateNew(annotation.autoCreateNew());
			li.setMustBeEmptyForDelete(annotation.mustBeEmptyForDelete());
			li.setCalculated(annotation.isCalculated());
			li.setProcessed(annotation.isProcessed());
			//li.setRecursive(annotation.recursive());

			for (OAFkey fkey : annotation.fkeys()) {
				OAPropertyInfo pi = oi.getPropertyInfo(fkey.fromProperty());
				OAFkeyInfo fki = new OAFkeyInfo();
				fki.setOAFkey(fkey);
				fki.setFromPropertyInfo(pi);

				// this will be done by updateLinkFkeys(), after all OAObjectInfos are loaded
				// pi = li.getToObjectInfo().getPropertyInfo(fkey.toProperty());
				// fki.setToPropertyInfo(pi);

				li.getFkeyInfos().add(fki);
			}

			li.setOAOne(annotation);

			boolean b = annotation.isOneAndOnlyOne();
			if (b) {
				oi.setHasOneAndOnlyOneLink(b);
			}
			li.setOneAndOnlyOne(b);

			li.setDefaultPropertyPath(annotation.defaultPropertyPath());
			li.setDefaultPropertyPathIsHierarchy(annotation.defaultPropertyPathIsHierarchy());
			li.setDefaultPropertyPathCanBeChanged(annotation.defaultPropertyPathCanBeChanged());

			li.setDefaultContextPropertyPath(annotation.defaultContextPropertyPath());

			li.setEqualPropertyPath(annotation.equalPropertyPath());
			li.setSelectFromPropertyPath(annotation.selectFromPropertyPath());

			OAObjCallback eq = (OAObjCallback) m.getAnnotation(OAObjCallback.class);
			if (eq != null) {
				li.setEnabledProperty(eq.enabledProperty());
				li.setEnabledValue(eq.enabledValue());
				li.setVisibleProperty(eq.visibleProperty());
				li.setVisibleValue(eq.visibleValue());
				li.setContextEnabledProperty(eq.contextEnabledProperty());
				li.setContextEnabledValue(eq.contextEnabledValue());
				li.setContextVisibleProperty(eq.contextVisibleProperty());
				li.setContextVisibleValue(eq.contextVisibleValue());
				li.setViewDependentProperties(eq.viewDependentProperties());
				li.setContextDependentProperties(eq.contextDependentProperties());
			}
		}

		// Manys
		for (final Method m : methods) {
			Class c = m.getReturnType();
			if (!Hub.class.isAssignableFrom(c)) {
				continue; // 20111027 added
			}
			if ((m.getModifiers() & Modifier.STATIC) != 0) {
				continue;
			}

			String name = getPropertyName(m.getName(), false);

			OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, name);
			OAMany annotation = (OAMany) m.getAnnotation(OAMany.class);
			Class cx = OAAnnotationDelegate.getHubObjectClass(annotation, m);

			if (li == null) {
				li = new OALinkInfo(name, cx, OALinkInfo.MANY);
				oi.addLinkInfo(li);
			}

			if (cx != null) {
				li.setToClass(cx);
			}
			if (annotation == null) {
				continue;
			}

			if (hs.contains("link." + name)) {
				continue;
			}
			hs.add("link." + name);

            s = annotation.lowerName();
            if (OAStr.isEmpty(s)) s = OAString.mfcl(name); 
            li.setLowerName(s);
			
			s = annotation.displayName();
			if (OAString.isEmpty(s)) {
				s = OAString.getDisplayName(name);
			}
			li.setDisplayName(s);
			li.setCascadeSave(annotation.cascadeSave());
			li.setCascadeDelete(annotation.cascadeDelete());
			li.setReverseName(annotation.reverseName());
			li.setOwner(annotation.owner());
			li.setRecursive(annotation.recursive());
			li.setCacheSize(annotation.cacheSize());
			li.setCouldBeLarge(annotation.couldBeLarge());
			li.setProcessed(annotation.isProcessed());

			s = annotation.matchHub();
			if (s != null && s.length() == 0) {
				s = null;
			}
			li.setMatchHub(s);

			s = annotation.matchProperty();
			if (s != null && s.length() == 0) {
				s = null;
			}
			li.setMatchProperty(s);

			s = annotation.matchStopProperty();
			if (s != null && s.length() == 0) {
				s = null;
			}
			li.setMatchStopProperty(s);
			
			s = annotation.autoCreateProperty();
			if (s != null && s.length() == 0) {
				s = null;
			}
			li.setAutoCreateProperty(s);

			s = annotation.uniqueProperty();
			if (s != null && s.length() == 0) {
				s = null;
			}
			li.setUniqueProperty(s);

			s = annotation.sortProperty();
			if (s != null && s.length() == 0) {
				s = null;
			}
			li.setSortProperty(s);
			li.setSortAsc(annotation.sortAsc());

			s = annotation.seqProperty();
			if (s != null && s.length() == 0) {
				s = null;
			}
			li.setSeqProperty(s);

			li.setMustBeEmptyForDelete(annotation.mustBeEmptyForDelete());
			li.setCalculated(annotation.isCalculated());
			li.setCalcDependentProperties(annotation.calcDependentProperties());
			li.setServerSideCalc(annotation.isServerSideCalc());
			li.setPrivateMethod(!annotation.createMethod());
			li.setCacheSize(annotation.cacheSize());
			s = annotation.mergerPropertyPath();
			li.setMergerPropertyPath(s);
			li.setEqualPropertyPath(annotation.equalPropertyPath());
			li.setSelectFromPropertyPath(annotation.selectFromPropertyPath());
			li.setOAMany(annotation);

			OAObjCallback eq = (OAObjCallback) m.getAnnotation(OAObjCallback.class);
			if (eq != null) {
				li.setEnabledProperty(eq.enabledProperty());
				li.setEnabledValue(eq.enabledValue());
				li.setVisibleProperty(eq.visibleProperty());
				li.setVisibleValue(eq.visibleValue());
				li.setContextEnabledProperty(eq.contextEnabledProperty());
				li.setContextEnabledValue(eq.contextEnabledValue());
				li.setContextVisibleProperty(eq.contextVisibleProperty());
				li.setContextVisibleValue(eq.contextVisibleValue());
				li.setViewDependentProperties(eq.viewDependentProperties());
				li.setContextDependentProperties(eq.contextDependentProperties());
			}
		}

		// methods
		for (Method m : methods) {
			OAMethod oamethod = (OAMethod) m.getAnnotation(OAMethod.class);
			if (oamethod == null) {
				continue;
			}
			final String name = m.getName();
			if (hs.contains("method." + name)) {
				continue;
			}
			hs.add("method." + name);

			OAMethodInfo mi = oi.getMethodInfo(name);
			if (mi == null) {
				mi = new OAMethodInfo();
				mi.setName(name);
				mi.setOAMethod(oamethod);
				oi.addMethodInfo(mi);
			}

			OAObjCallback eq = (OAObjCallback) m.getAnnotation(OAObjCallback.class);
			if (eq != null) {
				mi.setEnabledProperty(eq.enabledProperty());
				mi.setEnabledValue(eq.enabledValue());
				mi.setVisibleProperty(eq.visibleProperty());
				mi.setVisibleValue(eq.visibleValue());
				mi.setContextEnabledProperty(eq.contextEnabledProperty());
				mi.setContextEnabledValue(eq.contextEnabledValue());
				mi.setContextVisibleProperty(eq.contextVisibleProperty());
				mi.setContextVisibleValue(eq.contextVisibleValue());
				mi.setViewDependentProperties(eq.viewDependentProperties());
				mi.setContextDependentProperties(eq.contextDependentProperties());
			}
		}

		/*
		 *  check for any methods that have one param OAObjectCallback, and method name matching either onObjectCallback*  or *Callback
		 *
		 *  example:
		 *  @OAObjCallback   // (not required, but helpful)
		 *  public void lastNameCallback(OAObjectCallback)
		 */
		//  xxxCallback
		for (final Method m : methods) {
			String name = m.getName();
			final OAObjCallback eq = (OAObjCallback) m.getAnnotation(OAObjCallback.class);
			final Class[] cs = m.getParameterTypes();
			if (eq == null) {
				if (cs != null && cs.length == 1 && cs[0].equals(OAObjectCallback.class)) {
				} else {
					s = name.toUpperCase();
					if (s.endsWith("CALLBACK") && s.indexOf("$") < 0) {
						if (!s.endsWith("MODELCALLBACK") && !s.startsWith("GET") && !s.startsWith("SET")) {
							OATriggerMethod tm = (OATriggerMethod) m.getAnnotation(OATriggerMethod.class);
							if (tm == null) {
								s = "note: missing @OAObjCallback() annotation, class=" + clazz + ", method=" + m + ", will continue";
								LOG.log(Level.WARNING, s, new Exception(s));
							}
						}
					} else {
						continue;
					}
				}
			}
			if (oi.getMethodInfo(name) != null) {
				continue;
			}
			s = getPropertyName(name);
			if (!s.equals(name)) {
				boolean b = true;
				if (oi.getPropertyInfo(s) == null) {
					if (oi.getLinkInfo(s) == null) {
						if (oi.getCalcInfo(s) == null) {
							b = false;
						}
					}
				}
				if (b) {
					continue;
				}
			}

			String suffixName = null;
			// can also be xXX"Callback"(..)
			if (name.endsWith("Callback")) {
				suffixName = "Callback";
			} else if (name.equals("callback")) {
				suffixName = "callback";
			} else {
				if (eq != null) {
					s = "OAObjCallback annotation, class=" + clazz + ", method=" + m + ", should be named *Callback, will continue";
					LOG.log(Level.WARNING, s, new Exception(s));
				}
				continue;
			}

			boolean b = (cs != null && cs.length == 1);
			if (b) {
				b = cs[0].equals(OAObjectCallback.class);
				if (!Modifier.isPublic(m.getModifiers())) {
					s = "OAObjCallback annotation, class=" + clazz + ", method=" + m + ", should be public, will continue";
					LOG.log(Level.WARNING, s, new Exception(s));
				}
				if (!b && cs[0].isAssignableFrom(OAObjectModel.class)) {
					// public static void addressesModelCallback(OAObjectModel model)
					if (name.endsWith("ModelCallback")) {
						s = s.substring(0, s.length() - 13);
					} else {
						s = "OAObjCallback annotation, class=" + clazz + ", method=" + m
								+ ", should be named *ModelCallback, will continue";
						LOG.log(Level.WARNING, s, new Exception(s));
					}

					OALinkInfo lix = oi.getLinkInfo(s);
					if (lix == null) {
						s = "OAObjCallback annotation, class=" + clazz + ", method=" + m + ", link not found, name=" + s;
						LOG.log(Level.WARNING, s + ", will continue", new Exception(s));
					}
					if (!Modifier.isStatic(m.getModifiers())) {
						s = "OAObjCallback annotation, class=" + clazz + ", method=" + m + ", should be static, will continue";
						LOG.log(Level.WARNING, s, new Exception(s));
					}
					continue;
				}
				if (Modifier.isStatic(m.getModifiers())) {
					s = "OAObjCallback annotation, class=" + clazz + ", method=" + m + ", should not be static, will continue";
					LOG.log(Level.WARNING, s, new Exception(s));
				}
			}
			if (!b) {
				if (eq == null) {
					continue;
				}
				s = "OAObjCallback annotation, class=" + clazz + ", method=" + m
						+ ", should have one param of OAObjectCallback, will continue";
				LOG.log(Level.WARNING, s, new Exception(s));
			}
			name = name.substring(0, name.length() - suffixName.length());

			oi.addObjectCallbackMethod(name, m);

			// make sure it belongs to a prop/calc/link/method
			OAPropertyInfo pi = oi.getPropertyInfo(name);
			if (pi != null) {
				pi.setObjectCallbackMethod(m);
				if (eq != null) {
					s = eq.enabledProperty();
					if (OAString.isNotEmpty(s)) {
						pi.setEnabledProperty(s);
						pi.setEnabledValue(eq.enabledValue());
					}
					s = eq.visibleProperty();
					if (OAString.isNotEmpty(s)) {
						pi.setVisibleProperty(s);
						pi.setVisibleValue(eq.visibleValue());
					}
					s = eq.contextEnabledProperty();
					if (OAString.isNotEmpty(s)) {
						pi.setContextEnabledProperty(s);
						pi.setContextEnabledValue(eq.contextEnabledValue());
					}
					s = eq.contextVisibleProperty();
					if (OAString.isNotEmpty(s)) {
						pi.setContextVisibleProperty(s);
						pi.setContextVisibleValue(eq.contextVisibleValue());
					}
					pi.setViewDependentProperties(eq.viewDependentProperties());
					pi.setContextDependentProperties(eq.contextDependentProperties());
				}
			} else {
				if (name.length() == 0) {
					oi.setObjectCallbackMethod(m);
					if (eq != null) {
						s = eq.enabledProperty();
						if (OAString.isNotEmpty(s)) {
							oi.setEnabledProperty(s);
							oi.setEnabledValue(eq.enabledValue());
						}
						s = eq.visibleProperty();
						if (OAString.isNotEmpty(s)) {
							oi.setVisibleProperty(s);
							oi.setVisibleValue(eq.visibleValue());
						}
						s = eq.contextEnabledProperty();
						if (OAString.isNotEmpty(s)) {
							oi.setContextEnabledProperty(s);
							oi.setContextEnabledValue(eq.contextEnabledValue());
						}
						s = eq.contextVisibleProperty();
						if (OAString.isNotEmpty(s)) {
							oi.setContextVisibleProperty(s);
							oi.setContextVisibleValue(eq.contextVisibleValue());
						}
						oi.setViewDependentProperties(eq.viewDependentProperties());
						oi.setContextDependentProperties(eq.contextDependentProperties());
					}
				} else {
					OALinkInfo li = oi.getLinkInfo(name);
					if (li != null) {
						li.setObjectCallbackMethod(m);
						if (eq != null) {
							s = eq.enabledProperty();
							if (OAString.isNotEmpty(s)) {
								li.setEnabledProperty(s);
								li.setEnabledValue(eq.enabledValue());
							}
							s = eq.visibleProperty();
							if (OAString.isNotEmpty(s)) {
								li.setVisibleProperty(s);
								li.setVisibleValue(eq.visibleValue());
							}
							s = eq.contextEnabledProperty();
							if (OAString.isNotEmpty(s)) {
								li.setContextEnabledProperty(s);
								li.setContextEnabledValue(eq.contextEnabledValue());
							}
							s = eq.contextVisibleProperty();
							if (OAString.isNotEmpty(s)) {
								li.setContextVisibleProperty(s);
								li.setContextVisibleValue(eq.contextVisibleValue());
							}
							li.setViewDependentProperties(eq.viewDependentProperties());
							li.setContextDependentProperties(eq.contextDependentProperties());
						}
					} else {
						OACalcInfo ci = oi.getCalcInfo(name);
						if (ci != null) {
							ci.setObjectCallbackMethod(m);
							if (eq != null) {
								s = eq.enabledProperty();
								if (OAString.isNotEmpty(s)) {
									ci.setEnabledProperty(s);
									ci.setEnabledValue(eq.enabledValue());
								}
								s = eq.visibleProperty();
								if (OAString.isNotEmpty(s)) {
									ci.setVisibleProperty(s);
									ci.setVisibleValue(eq.visibleValue());
								}
								s = eq.contextEnabledProperty();
								if (OAString.isNotEmpty(s)) {
									ci.setContextEnabledProperty(s);
									ci.setContextEnabledValue(eq.contextEnabledValue());
								}
								s = eq.contextVisibleProperty();
								if (OAString.isNotEmpty(s)) {
									ci.setContextVisibleProperty(s);
									ci.setContextVisibleValue(eq.contextVisibleValue());
								}
								ci.setViewDependentProperties(eq.viewDependentProperties());
								ci.setContextDependentProperties(eq.contextDependentProperties());
							}
						} else {
							OAMethodInfo mi = oi.getMethodInfo(name);
							if (mi != null) {
								mi.setObjectCallbackMethod(m);
								if (eq != null) {
									s = eq.enabledProperty();
									if (OAString.isNotEmpty(s)) {
										mi.setEnabledProperty(s);
										mi.setEnabledValue(eq.enabledValue());
									}
									s = eq.visibleProperty();
									if (OAString.isNotEmpty(s)) {
										mi.setVisibleProperty(s);
										mi.setVisibleValue(eq.visibleValue());
									}
									s = eq.contextEnabledProperty();
									if (OAString.isNotEmpty(s)) {
										mi.setContextEnabledProperty(s);
										mi.setContextEnabledValue(eq.contextEnabledValue());
									}
									s = eq.contextVisibleProperty();
									if (OAString.isNotEmpty(s)) {
										mi.setContextVisibleProperty(s);
										mi.setContextVisibleValue(eq.contextVisibleValue());
									}
									mi.setViewDependentProperties(eq.viewDependentProperties());
									mi.setContextDependentProperties(eq.contextDependentProperties());
								}
							} else {
								b = false;
								for (Method mx : methods) {
									if (mx.getName().equals(name)) {
										b = true;
										break;
									}
								}
								if (!b) {
									s = "OAObjCallback annotation, class=" + clazz + ", method=" + m
											+ ", could not find method that it goes with, ex: get" + name;
									LOG.log(Level.WARNING, s, new Exception(s));
								}
							}
						}
					}
				}
			}
		}

		/* 20190515
		 *  check for any methods that have one param OAScheduler, and method name matching or *Callback
		 *  example:
		 *  public void calendarCallback(OAScheduler)
		 */
		for (final Method m : methods) {
			String name = m.getName();

			final Class[] cs = m.getParameterTypes();
			if (cs == null) {
				continue;
			}
			if (cs.length != 1 || !cs[0].getSimpleName().equals("OAScheduler")) {
				continue;
			}
			s = "Callback";
			if (!name.endsWith(s)) {
				continue;
			}
			s = name.substring(0, name.length() - s.length());
			OALinkInfo li = oi.getLinkInfo(s);
			if (li != null) {
				li.setSchedulerMethod(m);
			}
		}
	}

    
	/**
	 * Internal helper that processes {@link OATriggerMethod} annotations declared
	 * on the given class. Each trigger method is validated for correct signature
	 * and then registered with the trigger system.
	 *
	 * @param oi    the metadata object being updated
	 * @param clazz the class whose trigger methods are processed
	 */
	private void _update2(final OAObjectInfo oi, final Class clazz) {
		Method[] methods = clazz.getDeclaredMethods();
		if (methods == null) {
			return;
		}
		String s;
		String[] ss;

		for (Method method : methods) {
			OATriggerMethod annotation = (OATriggerMethod) method.getAnnotation(OATriggerMethod.class);
			if (annotation == null) {
				continue;
			}

			String[] props = annotation.properties();
			if (props == null || props.length == 0) {
				continue;
			}
			final boolean bBackgroundThread = annotation.runInBackgroundThread();
			final boolean bOnlyUseLoadedData = annotation.onlyUseLoadedData();
			boolean bServerSideOnly = annotation.runOnServer();

			// verify that method signature is correct, else log.warn
			s = "public void callbackName(HubEvent hubEvent)";
			s = ("callback method signature for class=" + clazz.getSimpleName() + ", callbackMethod=" + method.getName() + ", must match: "
					+ s);
			Class[] cs = method.getParameterTypes();
			if (cs == null || cs.length != 1 || !Modifier.isPublic(method.getModifiers())) {
				throw new RuntimeException(s);
			}
			if (!cs[0].equals(HubEvent.class)) {
				throw new RuntimeException(s);
			}

			// 20160625
			OATriggerListener tl = new OATriggerMethodListener(clazz, method, bOnlyUseLoadedData);
			OATriggerDelegate.createTrigger(method.getName(), clazz, tl, props, bOnlyUseLoadedData, bServerSideOnly, bBackgroundThread,
											true);
		}
	}

	/**
	 * Determines the OAObject class contained within a {@link Hub} return type.
	 * <p>
	 * The hub's element type is first resolved using reflection; if unavailable,
	 * the {@link OAMany#toClass()} annotation value is used when defined.
	 *
	 * @param annotation the {@link OAMany} annotation on the method, or {@code null}
	 * @param method     the accessor method returning a {@link Hub}
	 * @return the class of objects stored in the hub, or {@code null} if unresolved
	 */
	public Class getHubObjectClass(OAMany annotation, Method method) {
		Class cx = OAObjectReflectDelegate.getHubObjectClass(method);
		if (cx == null && annotation != null) {
			Class cz = annotation.toClass();
			if (cz != null && !cz.equals(Object.class)) {
				cx = cz;
			}
		}
		return cx;
	}
	
	/**
	 * Resolves and updates foreign-key metadata for all ONE-type links in the
	 * specified {@link OAObjectInfo}.
	 * <p>
	 * For each link, the corresponding target-property metadata is assigned to
	 * its {@link OAFkeyInfo} entries. This must be called after all object infos
	 * have been initialized.
	 *
	 * @param oi the metadata object whose link foreign-key information will be updated
	 */
	public void updateLinkFkeys(final OAObjectInfo oi) {
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != OALinkInfo.ONE) {
				continue;
			}
			for (OAFkeyInfo fki : li.getFkeyInfos()) {
				OAFkey fkey = fki.getOAFkey();
				OAPropertyInfo pi = li.getToObjectInfo().getPropertyInfo(fkey.toProperty());
				fki.setToPropertyInfo(pi);
			}

		}
	}

	
	/**
	 * Builds import-match property mappings for the specified {@link OAObjectInfo}.
	 * <p>
	 * All properties and links marked as import-match are collected, and recursive
	 * property paths are generated so that import operations can match incoming
	 * data to the correct object fields.
	 *
	 * @param oi the metadata object whose import-match mappings will be populated
	 */
	public void updateImportMatches(OAObjectInfo oi) {
		// get all ImportMatches and create propertyPaths for each
		final ArrayList<Object> al = new ArrayList<>();

		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (pi.isImportMatch()) {
				al.add(pi);
			}
		}

		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.isImportMatch()) {
				al.add(li);
			}
		}

		final ArrayList<String> alPropertyName = new ArrayList<>();
		final ArrayList<String> alPropertyPath = new ArrayList<>();

		for (Object obj : al) {
			if (obj == null) {
				continue;
			}
			if (obj instanceof OAPropertyInfo) {
				OAPropertyInfo pi = (OAPropertyInfo) obj;
				alPropertyName.add(pi.getLowerName());
				alPropertyPath.add(""); // "this"
			} else {
				OALinkInfo li = (OALinkInfo) obj;

				/*was, removed 20220918
				String prefixName;
				if (li.getPojoNames() != null && li.getPojoNames().length > 0) {
					prefixName = "";
				} else {
					prefixName = li.getLowerName();
				}
				recurseUpdateImportMatches(	0, prefixName, li.getLowerName(), li, alPropertyName, alPropertyPath,
											new HashSet<OALinkInfo>());
				*/

				recurseUpdateImportMatches(	0, "", li.getLowerName(), li, alPropertyName, alPropertyPath,
											new HashSet<OALinkInfo>());
			}
		}
		faObjectInfo.setImportMatchPropertyNames(oi, alPropertyName.toArray(new String[alPropertyName.size()]));
		faObjectInfo.setImportMatchPropertyPaths(oi, alPropertyPath.toArray(new String[alPropertyPath.size()]));
	}

	
	
	/**
	 * Recursively generates import-match property names and paths for a link
	 * hierarchy.  
	 * <p>
	 * For each link marked as import-match, dependent properties and child links
	 * are traversed, producing flattened property-name and property-path entries
	 * used during import processing.
	 *
	 * @param level          recursion depth
	 * @param prefixName     accumulated name prefix
	 * @param prefixPath     accumulated property-path prefix
	 * @param li             the link being processed
	 * @param alPropertyName list collecting generated property names
	 * @param alPropertyPath list collecting generated property paths
	 * @param hsLi           tracks visited links to prevent recursion cycles
	 */
	private static void recurseUpdateImportMatches(final int level, final String prefixName, final String prefixPath, final OALinkInfo li,
			final ArrayList<String> alPropertyName,
			final ArrayList<String> alPropertyPath, final HashSet<OALinkInfo> hsLi) {

		if (hsLi.contains(li)) {
			return;
		}
		hsLi.add(li);

		if (li == null || !li.isImportMatch()) {
			return;
		}

		final OAObjectInfo oiTo = li.getToObjectInfo();

		boolean b = false;
		for (OAPropertyInfo pi : oiTo.getPropertyInfos()) {
			if (pi.isImportMatch()) {
				b = true;
				break;
			}
		}
		for (OALinkInfo lix : oiTo.getLinkInfos()) {
			if (lix.isImportMatch()) {
				b = true;
				break;
			}
		}
		if (!b) {
			// 20220918
			if (li.getFkeyInfos().size() > 0) {
				for (OAFkeyInfo fki : li.getFkeyInfos()) {
					if (fki.getFromPropertyInfo() == null) {
						continue;
					}
					alPropertyName.add(fki.getFromPropertyInfo().getName());
					String s = prefixPath + "." + fki.getFromPropertyInfo().getName();
					alPropertyPath.add(s);
				}
			} else {
				for (String pkName : oiTo.getKeyProperties()) {
					OAPropertyInfo pi = oiTo.getPropertyInfo(pkName);
					alPropertyName.add(prefixName + pi.getName());
					String s = (prefixPath == null || prefixPath.length() == 0) ? pi.getLowerName() : (prefixPath + "." + pi.getName());
					alPropertyPath.add(s);
				}
			}
		} else {
			for (OAPropertyInfo pi : oiTo.getPropertyInfos()) {
				if (!pi.isImportMatch()) {
					continue;
				}
				alPropertyName.add(prefixName + pi.getName());
				String s = (prefixPath == null || prefixPath.length() == 0) ? pi.getLowerName() : (prefixPath + "." + pi.getName());
				alPropertyPath.add(s);
			}

			for (OALinkInfo lix : oiTo.getLinkInfos()) {
				if (!lix.isImportMatch()) {
					continue;
				}
				recurseUpdateImportMatches(	level + 1, prefixName + lix.getName(), prefixPath + "." + lix.getName(), lix, alPropertyName,
											alPropertyPath,
											hsLi);
			}
		}
	}
	
	
	
	
	
//qqqqqqqq same methods are in OAObjectDatabaseService qqqqq find new home	
	public String getPropertyName(String s) {
		return getPropertyName(s, true);
	}
	public String getPropertyName(String s, boolean bToLower) {
		boolean b = true;
		if (s.startsWith("get")) {
			s = s.substring(3);
		} else if (s.startsWith("is")) {
			s = s.substring(2);
		} else if (s.startsWith("has")) {
			s = s.substring(3);
		} else if (s.startsWith("set")) {
			s = s.substring(3);
		} else {
			b = false;
		}
		if (bToLower && b && s.length() > 1) {
			s = Character.toLowerCase(s.charAt(0)) + s.substring(1);
		}
		return s;
	}
	
}
