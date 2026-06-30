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
package com.viaoa.runtime.context;

/*qqqqqqqqqq
CODEX

 #3
  file/class/method: src/main/java/com/viaoa/graph/context/OAUserAccess.java:571, src/main/java/com/viaoa/graph/
  context/OAUserAccess.java:679
  exact concern: packageValid logic is inconsistent. getVisible allows classes outside the package and evaluates
  classes inside it. getEnabled immediately returns true for classes inside the package, bypassing enabled/disabled
  rules.
  why it matters: this can grant enabled access to every class in the restricted package and ignore explicit deny
  rules. Visibility and enabled semantics diverge in a way that looks accidental and security-relevant.
  severity: bug
  minimal fix: align getEnabled with getVisible if the package contract is “only evaluate this package, allow
  others.” Otherwise document the different enabled contract and add tests proving it.
  suggested invariant ID/name: UA-PACKAGE-SCOPE-CONSISTENT
  suggested test coverage: set packageValid, add explicit not-enabled/not-visible rules for a class in that package,
  verify both methods apply package scoping consistently.

#4
  file/class/method: src/main/java/com/viaoa/graph/context/OAUserAccess.java:747
  exact concern: getIsInSamePath assumes ua.pp.getLinkInfos() is non-null and immediately uses lis.length.
  Public addEnabled/addVisible/addNot... methods accept empty or scalar property paths and create OAPath without
  guarding link info.
  why it matters: a valid-looking rule with an empty/scalar path can fail later during permission evaluation instead
  of behaving as root-only/no-traversal access.
  severity: bug
  minimal fix: guard lis == null || lis.length == 0 and treat it as no traversal after direct object/Hub AO checks.
  Also guard ppReverse/reverse link info before reverse traversal.
  suggested invariant ID/name: UA-EMPTY-PATH-NO-THROW
  suggested test coverage: add access rules with "", null if allowed, and scalar-only paths; call enabled/visible
  checks against root and unrelated objects.

#5
  file/class/method: src/main/java/com/viaoa/graph/context/OAUserAccess.java:832
  exact concern: reverse traversal calculates indexes from liz but loops with k < lis.length and indexes liz[k].
  why it matters: if forward and reverse link arrays ever differ, this can skip reverse links or throw
  ArrayIndexOutOfBoundsException. Even if normal paths usually match, the invariant is implicit and fragile around
  casts/calculated/private links.
  severity: invariant risk
  minimal fix: loop against liz.length, guard liz != null, and validate k >= 0.
  suggested invariant ID/name: UA-REVERSE-PATH-BOUNDS
  suggested test coverage: permission paths with multi-hop links, casts, calculated endpoints, and non-one links;
  verify reverse/common-master matching and no index exceptions.


#6
  file/class/method: src/main/java/com/viaoa/graph/context/OAUserAccess.java:89
  exact concern: access rule collections are mutable ArrayList/HashSet/HashMap and are read during permission checks
  without synchronization or snapshotting.
  why it matters: if an OAUserAccess is shared through OAContext while another thread mutates rules, permission
  results can be inconsistent or throw ConcurrentModificationException.
  severity: invariant risk
  minimal fix: define the contract as configure-before-publish, or publish immutable/snapshot views when installing
  into OAContext.
  suggested invariant ID/name: UA-CONFIGURE-BEFORE-PUBLISH
  suggested test coverage: install access object in context, concurrently evaluate and mutate rules; either assert
  unsupported mutation is documented or make evaluation stable.
  



*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.viaoa.hub.Hub;
import com.viaoa.lang.OAArray;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;
import com.viaoa.select.OASelect;

/**
 * Used to set the "boundaries" of what objects & properties/methods can be accessed by a user (or system). <br>
 * Used to determine if an object is included in a Path from a root OAObject/Hub.AO Separate methods for 
 * Visible and Enabled property paths, both On and Off (Not).<br>
 * Has methods to add multiple obj/hub and Paths, so that all are searched to see if an Object is included in any of the root +
 * paths. Allows for adding child[ren] OAUserAccess 
 *
Used by OAContext<p> 

<pre>
<code>
    OAUserAccess ua = new OAUserAccess();
    ua.addVisible(buyer, BuyerPP.location().company().clients().products().campaigns().pp);

    boolean bx = ua.getHasVisible(camp);
    bx = ua.getHasVisible(new AppUser());
    bx = ua.getHasVisible(new Campaign());

    bx = ua.getHasVisible(company);
    bx = ua.getHasVisible(new Company());

    bx = ua.getHasVisible(client);
    bx = ua.getHasVisible(product);
    bx = ua.getHasVisible(null);
    bx = ua.getHasVisible(new Product());
</code>
</pre>
 *
 * @author vvia
 */

/**
 * Defines the visibility and enabled/disabled access rules for OAObjects and
 * their properties based on class membership, named properties, or membership
 * in specific property-path relationships. <p>
 *
 * OAUserAccess allows an application to specify:
 * <ul>
 *   <li>Which classes are visible or enabled.</li>
 *   <li>Which properties of those classes are visible or enabled.</li>
 *   <li>Which objects are reachable from one or more root objects or hubs via
 *       configured {@link com.viaoa.path.OAPath} instances.</li>
 *   <li>Negative rules (“not visible”, “not enabled”) that override positive
 *       rules.</li>
 *   <li>Chained access evaluation across multiple OAUserAccess instances.</li>
 * </ul>
 *
 * Permission checks are performed using a deterministic evaluation order:
 * class rules, class+property rules, path-based rules, and then chained
 * OAUserAccess instances. Path-based evaluation uses both forward and reverse
 * traversal of the property path so that objects sharing a common ancestor
 * with the defined root are treated as included. <p>
 *
 * OAUserAccess is used by {@link com.viaoa.context.OAContext} to implement
 * application-level permission enforcement, determining which OAObjects and
 * properties are visible or enabled for a given user or thread context.
 */
public class OAContextAccess {

	/** Path-based rules granting “enabled” access. */
	private final List<ContextAccess> alEnabledUserAccess = new ArrayList<>();

	/** Path-based rules denying “enabled” access. */
	private final List<ContextAccess> alNotEnabledUserAccess = new ArrayList<>();

	/** Path-based rules granting “visible” access. */
	private final List<ContextAccess> alVisibleUserAccess = new ArrayList<>();

	/** Path-based rules denying “visible” access. */
	private final List<ContextAccess> alNotVisibleUserAccess = new ArrayList<>();

	/** Classes that are explicitly marked as enabled. */
	private final Set<Class<? extends OAObject>> hsEnabledClass = new HashSet<>();

	/** Classes explicitly marked as not enabled. */
	private final Set<Class<? extends OAObject>> hsNotEnabledClass = new HashSet<>();
	
	/** Classes that are explicitly marked as visible. */
	private final Set<Class<? extends OAObject>> hsVisibleClass = new HashSet<>();
	
	/** Classes explicitly marked as not visible. */
	private final Set<Class<? extends OAObject>> hsNotVisibleClass = new HashSet<>();

	/** Enabled rules for specific properties of specific classes. */
	private final Map<Class<? extends OAObject>, String[]> hmEnabledClass = new HashMap<>();

	/** Not-enabled rules for specific properties of specific classes. */
	private final Map<Class<? extends OAObject>, String[]> hmNotEnabledClass = new HashMap<>();
	
	/** Visible rules for specific properties of specific classes. */
	private final Map<Class<? extends OAObject>, String[]> hmVisibleClass = new HashMap<>();
	
	/** Not-visible rules for specific properties of specific classes. */
	private final Map<Class<? extends OAObject>, String[]> hmNotVisibleClass = new HashMap<>();


	//qqqqqqqqqqqqq
	// todo? add query extraWhereClause .....
	//     ... or use current prop paths to build it ..
	//    ex:  buyer.loc.company.clients.products.campaigns
	//          => AND campaign.propduct.client.company = buyer.loc.company
	// create a method to have oaselect use UserAccess to get this qqqqqqqq

	/**
	 * Placeholder method for updating an OASelect based on access rules.
	 * Currently returns false to indicate no changes were made.
	 *
	 * @param sel the OASelect to update
	 * @return always false
	 */
	public boolean updateSelect(OASelect sel) {
		return false; // no changes made
	}

	/**
	 * Placeholder for generating an SQL extra WHERE clause based on access rules.
	 * Currently returns null.
	 *
	 * @param clazz class for which a clause might be generated
	 * @return null
	 */
	public String getExtraWhereClause(Class clazz) {
	    String whereClause = null;
	    //qqqqqqqq ?
	    return whereClause;
	}
	
	
	// todo? allow param to determine if user has access
	// ex:  buyer.isManager  ... if true then skip the rule

	/** Default access values when no rule applies. */
	private boolean bDefaultEnabled, bDefaultVisible;

	/**
	 * Child OAUserAccess instances. After this OAUserAccess computes a result,
	 * each child reevaluates it, allowing hierarchical permission rules.
	 */
	private final ArrayList<OAContextAccess> alOAUserAccess = new ArrayList<>();

	/**
	 * Constructs an OAUserAccess with default values of false for both enabled
	 * and visible. No package restriction is applied.
	 */
	public OAContextAccess() {

	}

	/**
	 * Constructs an OAUserAccess with the specified default enabled and visible
	 * values.
	 *
	 * @param bDefaultEnabled default enabled flag
	 * @param bDefaultVisible default visible flag
	 */
	public OAContextAccess(boolean bDefaultEnabled, boolean bDefaultVisible) {
		this.bDefaultEnabled = bDefaultEnabled;
		this.bDefaultVisible = bDefaultVisible;
	}

	/**
	 * Adds a child OAUserAccess to be evaluated after this one. The result of
	 * this OAUserAccess becomes the default input for the child.
	 *
	 * @param ua child OAUserAccess
	 */
	public void addUserAccess(OAContextAccess ua) {
		if (ua != null) {
			alOAUserAccess.add(ua);
		}
	}

	/**
	 * Holds a root object or hub with an associated property path defining a
	 * visibility or enabled rule. Includes both forward and reverse property
	 * paths to determine reachability and common ancestors.
	 */
	protected static class ContextAccess {
		/** Optional root hub for path evaluation. */
		Hub hub;

		/** Optional root object for path evaluation. */
		OAObject obj;
		
		/** Forward property path used for searching from the root. */
		OAPath pp;

		/** Reverse property path for searching backward from the target object. */
		OAPath ppReverse;
		
		/** If true, only the final segment of the property path is evaluated. */
		boolean bOnlyEndProperty;
		
		/** Optional list of property names this rule applies to. */
		String[] props;

		//qqqqqqqqqq addIsUsedCheck(object, propPath, value)
		// add custom method isUsed(boolean bDefault) to override qqqqqqq

		/**
		 * Creates a UserAccess starting from an OAObject root with a property path.
		 *
		 * @param obj root OAObject
		 * @param pp property path string
		 * @param bOnlyEndProperty whether only the final path segment applies
		 */
		public ContextAccess(OAObject obj, String pp, boolean bOnlyEndProperty) {
			this.obj = obj;
			this.pp = new OAPath(obj.getClass(), pp);
			this.ppReverse = this.pp.getReversePath();
			this.bOnlyEndProperty = bOnlyEndProperty;
		}

		/**
		 * Creates a UserAccess starting from a Hub root with a property path.
		 *
		 * @param hub root Hub
		 * @param pp property path string
		 * @param bOnlyEndProperty whether only the final segment applies
		 */
		public ContextAccess(Hub hub, String pp, boolean bOnlyEndProperty) {
			this.hub = hub;
			this.pp = new OAPath(hub.getObjectClass(), pp);
			this.ppReverse = this.pp.getReversePath();
			this.bOnlyEndProperty = bOnlyEndProperty;
		}

		/**
		 * Assigns property names this rule applies to.
		 *
		 * @param props property names
		 */
		public void setProperties(String... props) {
			this.props = props;
		}

		/**
		 * Determines whether the given property name matches one of this rule's
		 * property names.
		 *
		 * @param prop property name to test
		 * @return true if matched; false otherwise
		 */
		public boolean find(String prop) {
			if (prop == null || prop.length() == 0) {
				return false;
			}
			if (props == null) {
				return false;
			}
			for (String s : props) {
				if (prop.equalsIgnoreCase(s)) {
					return true;
				}
			}
			return false;
		}
	}

	public void addEnabled(Class<? extends OAObject> c) {
		hsEnabledClass.add(c);
	}

	public void addNotEnabled(Class<? extends OAObject> c) {
		hsNotEnabledClass.add(c);
	}

	public void addVisible(Class<? extends OAObject> c) {
		hsVisibleClass.add(c);
	}

	public void addNotVisible(Class<? extends OAObject> c) {
		hsNotVisibleClass.add(c);
	}

	public void addEnabled(Class<? extends OAObject> c, String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		hmEnabledClass.put(c, (String[]) OAArray.add(String.class, hmEnabledClass.get(c), propertyName));
	}

	public void addNotEnabled(Class<? extends OAObject> c, String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		hmNotEnabledClass.put(c, (String[]) OAArray.add(String.class, hmNotEnabledClass.get(c), propertyName));
	}

	public void addVisible(Class<? extends OAObject> c, String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		hmVisibleClass.put(c, (String[]) OAArray.add(String.class, hmVisibleClass.get(c), propertyName));
	}

	public void addNotVisible(Class<? extends OAObject> c, String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		hmNotVisibleClass.put(c, (String[]) OAArray.add(String.class, hmNotVisibleClass.get(c), propertyName));
	}

	public void addEnabled(OAObject obj, String pp) {
		addEnabled(obj, pp, null, false);
	}

	public void addEnabled(OAObject obj, String pp, String propertyName) {
		addEnabled(obj, pp, propertyName, false);
	}

	public void addEnabled(OAObject obj, String pp, String propertyName, boolean bOnlyEndProperty) {
		if (obj == null) {
			return;
		}
		ContextAccess ua = new ContextAccess(obj, pp, bOnlyEndProperty);
		if (OAString.isNotEmpty(propertyName)) {
			ua.props = new String[] { propertyName };
		}
		alEnabledUserAccess.add(ua);
	}

	public void addEnabled(Hub hub, String pp) {
		addEnabled(hub, pp, null, false);
	}

	public void addEnabled(Hub hub, String pp, String propertyName) {
		addEnabled(hub, pp, propertyName, false);
	}

	public void addEnabled(Hub hub, String pp, String propertyName, boolean bOnlyEndProperty) {
		if (hub == null) {
			return;
		}
		if (hub.getObjectClass() == null) {
			throw new RuntimeException("hub getObjectClass can not be null");
		}
		ContextAccess ua = new ContextAccess(hub, pp, bOnlyEndProperty);
		if (OAString.isNotEmpty(propertyName)) {
			ua.props = new String[] { propertyName };
		}
		alEnabledUserAccess.add(ua);
	}

	public void addNotEnabled(OAObject obj, String pp) {
		addNotEnabled(obj, pp, null, false);
	}

	public void addNotEnabled(OAObject obj, String pp, String propertyName) {
		addNotEnabled(obj, pp, propertyName, false);
	}

	public void addNotEnabled(OAObject obj, String pp, String propertyName, boolean bOnlyEndProperty) {
		if (obj == null) {
			return;
		}
		ContextAccess ua = new ContextAccess(obj, pp, bOnlyEndProperty);
		if (OAString.isNotEmpty(propertyName)) {
			ua.props = new String[] { propertyName };
		}
		alNotEnabledUserAccess.add(ua);
	}

	public void addNotEnabled(Hub hub, String pp, String propertyName) {
		addNotEnabled(hub, pp, propertyName, false);
	}

	public void addNotEnabled(Hub hub, String pp, String propertyName, boolean bOnlyEndProperty) {
		if (hub == null) {
			return;
		}
		if (hub.getObjectClass() == null) {
			throw new RuntimeException("hub getObjectClass can not be null");
		}
		ContextAccess ua = new ContextAccess(hub, pp, bOnlyEndProperty);
		if (OAString.isNotEmpty(propertyName)) {
			ua.props = new String[] { propertyName };
		}
		alNotEnabledUserAccess.add(ua);
	}

	public void addVisible(OAObject obj, String pp) {
		addVisible(obj, pp, null, false);
	}

	public void addVisible(OAObject obj, String pp, String propertyName) {
		addVisible(obj, pp, propertyName, false);
	}

	public void addVisible(OAObject obj, String pp, String propertyName, boolean bOnlyEndProperty) {
		if (obj == null) {
			return;
		}
		ContextAccess ua = new ContextAccess(obj, pp, bOnlyEndProperty);
		if (OAString.isNotEmpty(propertyName)) {
			ua.props = new String[] { propertyName };
		}
		alVisibleUserAccess.add(ua);
	}

	public void addVisible(Hub hub, String pp) {
		addVisible(hub, pp, null, false);
	}

	public void addVisible(Hub hub, String pp, String propertyName) {
		addVisible(hub, pp, propertyName, false);
	}

	/**
	 * @param hub
	 * @param pp
	 * @param propertyName
	 * @param bOnlyEndProperty
	 */
	public void addVisible(Hub hub, String pp, String propertyName, boolean bOnlyEndProperty) {
		if (hub == null) {
			return;
		}
		if (hub.getObjectClass() == null) {
			throw new RuntimeException("hub getObjectClass can not be null");
		}
		ContextAccess ua = new ContextAccess(hub, pp, bOnlyEndProperty);
		if (OAString.isNotEmpty(propertyName)) {
			ua.props = new String[] { propertyName };
		}
		alVisibleUserAccess.add(ua);
	}

	public void addNotVisible(OAObject obj, String pp) {
		addNotVisible(obj, pp, null, false);
	}

	public void addNotVisible(OAObject obj, String pp, String propertyName) {
		addNotVisible(obj, pp, propertyName, false);
	}

	public void addNotVisible(OAObject obj, String pp, String propertyName, boolean bOnlyEndProperty) {
		if (obj == null) {
			return;
		}
		ContextAccess ua = new ContextAccess(obj, pp, bOnlyEndProperty);
		if (OAString.isNotEmpty(propertyName)) {
			ua.props = new String[] { propertyName };
		}
		alNotVisibleUserAccess.add(ua);
	}

	public void addNotVisible(Hub hub, String pp) {
		addNotVisible(hub, pp, null, false);
	}

	public void addNotVisible(Hub hub, String pp, String propertyName, boolean bOnlyEndProperty) {
		if (hub == null) {
			return;
		}
		if (hub.getObjectClass() == null) {
			throw new RuntimeException("hub getObjectClass can not be null");
		}
		ContextAccess ua = new ContextAccess(hub, pp, bOnlyEndProperty);
		if (OAString.isNotEmpty(propertyName)) {
			ua.props = new String[] { propertyName };
		}
		alNotVisibleUserAccess.add(ua);
	}

	/**
	 * Returns whether the specified OAObject is enabled using default rules.
	 *
	 * @param obj OAObject to evaluate
	 * @return true if enabled; false otherwise
	 */
	public boolean getEnabled(OAObject obj) {
		if (obj == null) {
			return false;
		}
		boolean b = getEnabled(obj, obj.getClass(), null, bDefaultEnabled);
		return b;
	}

	/**
	 * Core enabled-evaluation algorithm. Applies class rules, class+property
	 * rules, path rules, and chained OAUserAccess instances.
	 *
	 * @return final evaluated enabled flag
	 */
	public boolean getEnabled(OAObject obj, String propertyName) {
		if (obj == null) {
			return false;
		}
		boolean b = getEnabled(obj, obj.getClass(), propertyName, bDefaultEnabled);
		return b;
	}

	public boolean getEnabled(final Class clazz) {
		if (clazz == null) {
			return false;
		}
		return getEnabled(null, clazz, null, bDefaultEnabled);
	}

	public boolean getEnabled(final OAObject obj, final String propertyName, final boolean bDefault) {
		if (obj == null) {
			return false;
		}
		return getEnabled(obj, obj.getClass(), propertyName, bDefault);
	}

	/**
	 * Checks to see if an OAObject & (optional) propertyName should be enabled. Uses the following steps: 
	 * <ol>
	 * <li>starts with result set to
	 * default value (true/false) 
	 * <li>checks if the class is enabled (result set to true). 
	 * <li>checks if the class is not enabled (result set
	 * to false). 
	 * <li>if property name, then checks if class+propertyName is enabled (result set to true). 
	 * <li>if property name, then checks
	 * if class+propertyName is not enabled (result set to false). 
	 * <li>checks to see if obj [& prop] are in the enabled propert paths (result
	 * set to true) 
	 * <li>checks to see if obj [& prop] are in the not enabled propert paths (result set to false) 
	 * <li>calls child[ren]
	 * recursively setting result. 
	 * <li>returns result
	 * </ol>
	 */
	protected boolean getEnabled(final OAObject obj, final Class cz, final String propertyName, final boolean bDefault) {
		boolean bResult = bDefault;

		if (hsEnabledClass.contains(cz)) {
			bResult = true;
		}
		if (hsNotEnabledClass.contains(cz)) {
			bResult = false;
		}

		if (obj != null) {
			if (propertyName != null) {
				String[] ss = hmEnabledClass.get(cz);
				if (ss != null) {
					for (String s : ss) {
						if (propertyName.equalsIgnoreCase(s)) {
							bResult = true;
						}
					}
				}
				ss = hmNotEnabledClass.get(cz);
				if (ss != null) {
					for (String s : ss) {
						if (propertyName.equalsIgnoreCase(s)) {
							bResult = false;
						}
					}
				}
			}

			boolean b = getIsInSamePath(obj, propertyName, alEnabledUserAccess);
			if (b) {
				bResult = true;
			}
			b = getIsInSamePath(obj, propertyName, alNotEnabledUserAccess);
			if (b) {
				bResult = false;
			}
		}

		for (OAContextAccess ua : alOAUserAccess) {
			bResult = ua.getEnabled(obj, cz, propertyName, bResult);
		}
		return bResult;
	}

	/**
	 * Returns whether objects of the specified class are visible.
	 *
	 * @param clazz class to evaluate
	 * @return true if visible; false otherwise
	 */
	public boolean getVisible(Class clazz) {
		if (clazz == null) {
			return false;
		}
		boolean b = getVisible(null, clazz, null, bDefaultVisible);
		return b;
	}

	public boolean getVisible(OAObject obj) {
		if (obj == null) {
			return false;
		}
		boolean b = getVisible(obj, obj.getClass(), null, bDefaultVisible);
		return b;
	}

	public boolean getVisible(OAObject obj, String propertyName) {
		if (obj == null) {
			return false;
		}
		boolean b = getVisible(obj, obj.getClass(), propertyName, bDefaultVisible);
		return b;
	}

	protected boolean getVisible(final OAObject obj, final String propertyName, final boolean bDefault) {
		if (obj == null) {
			return false;
		}
		return getVisible(obj, obj.getClass(), propertyName, bDefault);
	}

	/*
	 * Checks to see if an OAObject & (optional) propertyName should be visible. Uses the following steps:
	 * <ol> 
	 * <li>starts with result set to default value (true/false) 1: checks if the class is enabled (result set to true). 
	 * <li>checks if the class is not enabled (result set to false). 
	 * <li>if property name, then checks if class+propertyName is enabled (result set to true). 
	 * <li>if property name, then checks if class+propertyName is not enabled (result set to false). 
	 * <li>checks to see if obj [& prop] are in the enabled propert paths (result set to true) 
	 * <li>checks to see if obj [& prop] are in the not enabled propert paths (result set to false) 
	 * <li>calls child[ren] recursively setting result. 8: returns result
	 * </ul>
	 */
	
	/**
	 * Core visibility-evaluation algorithm. Applies class, property, path, and
	 * chained rules to compute a final visible flag.
	 *
	 * @return visibility result
	 */
	protected boolean getVisible(final OAObject obj, final Class cz, final String propertyName, final boolean bDefault) {

		boolean bResult = bDefault;

		if (hsVisibleClass.contains(cz)) {
			bResult = true;
		}
		if (hsNotVisibleClass.contains(cz)) {
			bResult = false;
		}

		if (obj != null) {
			if (propertyName != null) {
				String[] ss = hmVisibleClass.get(cz);
				if (ss != null) {
					for (String s : ss) {
						if (propertyName.equalsIgnoreCase(s)) {
							bResult = true;
						}
					}
				}
				ss = hmNotVisibleClass.get(cz);
				if (ss != null) {
					for (String s : ss) {
						if (propertyName.equalsIgnoreCase(s)) {
							bResult = false;
						}
					}
				}
			}

			boolean b = getIsInSamePath(obj, propertyName, alVisibleUserAccess);
			if (b) {
				bResult = true;
			}
			b = getIsInSamePath(obj, propertyName, alNotVisibleUserAccess);
			if (b) {
				bResult = false;
			}
		}

		for (OAContextAccess ua : alOAUserAccess) {
			bResult = ua.getVisible(obj, cz, propertyName, bResult);
		}
		return bResult;
	}

	/**
	 * Determines whether the target object participates in the same property path
	 * hierarchy as any UserAccess rule. The algorithm:
	 *
	 * <ol>
	 *   <li>Checks direct root equality</li>
	 *   <li>Checks Hub AO equality</li>
	 *   <li>Traverses forward property paths to check reachability</li>
	 *   <li>Traverses reverse property paths to detect shared ancestors</li>
	 * </ol>
	 *
	 * @param objSearch object being evaluated
	 * @param propertyName optional property name
	 * @param alUserAccess list of rules to evaluate
	 * @return true if object matches rule; false otherwise
	 */
	protected boolean getIsInSamePath(final OAObject objSearch, final String propertyName,
			final List<ContextAccess> alUserAccess) {
		if (objSearch == null || alUserAccess == null) {
			return false;
		}
		final Class cz = objSearch.getClass();

		for (final ContextAccess ua : alUserAccess) {
			if (propertyName != null) {
				if (ua.props == null) {
					continue;
				}
			} else if (ua.props != null) {
				continue;
			}

			if (ua.obj == objSearch) {
				return true;
			}

			if (ua.hub != null && ua.hub.getAO() == objSearch) {
				if (OAString.isNotEmpty(propertyName)) {
					if (ua.props != null) {
						return ua.find(propertyName);
					}
				}
				return true;
			}

			// see if obj type is in ua Path type of objects
			OALinkInfo[] lis = ua.pp.getLinkInfos();

			int i = 0;
			if (ua.bOnlyEndProperty) {
				i = Math.max(0, lis.length - 1);
			}
			for (; i < lis.length; i++) {
				OALinkInfo li = lis[i];

				if (!li.getToClass().equals(cz)) {
					continue;
				}

				Object objx = ua.obj;
				if (objx == null) {
					if (ua.hub == null) {
						break;
					}
					objx = ua.hub.getAO();
					if (objx == null) {
						break;
					}
				}
				if (objx == objSearch) {

					if (OAString.isNotEmpty(propertyName)) {
						if (ua.props != null) {
							return ua.find(propertyName);
						}
					}
					return true;
				}

				int j = 0;
				for (; j <= i; j++) {
					if (lis[j].getType() != OALinkInfo.TYPE_ONE) {
						break;
					}
					objx = lis[j].getValue(objx);
					if (objx == null) {
						break;
					}
					if (objx == objSearch) {
						if (OAString.isNotEmpty(propertyName)) {
							if (ua.props != null) {
								return ua.find(propertyName);
							}
						}
						return true;
					}
				}
				if (objx == null) {
					continue;
				}

				OALinkInfo[] liz = ua.ppReverse.getLinkInfos();
				int k = (liz.length - i) - 1;
				Object objz = objSearch;
				for (; k < liz.length; k++) {
					if (liz[k].getType() != OALinkInfo.TYPE_ONE) {
						break;
					}
					objz = liz[k].getValue(objz);
					if (objz == null) {
						break;
					}
					if (objz == objx) {
						if (OAString.isNotEmpty(propertyName)) {
							if (ua.props != null) {
								return ua.find(propertyName);
							}
						}
						return true; // common master
					}
				}
			}
		}
		return false;
	}
}
