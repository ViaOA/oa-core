package com.viaoa.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Used to set the "boundaries" of what objects & properties/methods can be accessed by a user (or system). Used to determine if an object
 * is included in a propertyPath from a root OAObject/Hub.AO Separate methods for Visible and Enabled property paths, both On and Off (Not).
 * Has methods to add multiple obj/hub and propertyPaths, so that all are searched to see if an Object is included in any of the root +
 * paths. Allows for adding child[ren] OAUserAccess <code>

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
 *
 * @author vvia
 */
public class OAUserAccess {

	private final ArrayList<UserAccess> alEnabledUserAccess = new ArrayList<>();
	private final ArrayList<UserAccess> alNotEnabledUserAccess = new ArrayList<>();

	private final ArrayList<UserAccess> alVisibleUserAccess = new ArrayList<>();
	private final ArrayList<UserAccess> alNotVisibleUserAccess = new ArrayList<>();

	// classes
	private final HashSet<Class<? extends OAObject>> hsEnabledClass = new HashSet<>();
	private final HashSet<Class<? extends OAObject>> hsNotEnabledClass = new HashSet<>();
	private final HashSet<Class<? extends OAObject>> hsVisibleClass = new HashSet<>();
	private final HashSet<Class<? extends OAObject>> hsNotVisibleClass = new HashSet<>();

	// classes properties
	private final HashMap<Class<? extends OAObject>, String[]> hmEnabledClass = new HashMap<>();
	private final HashMap<Class<? extends OAObject>, String[]> hmNotEnabledClass = new HashMap<>();
	private final HashMap<Class<? extends OAObject>, String[]> hmVisibleClass = new HashMap<>();
	private final HashMap<Class<? extends OAObject>, String[]> hmNotVisibleClass = new HashMap<>();

	private Package packageValid; // ignore/allow others

	// todo? add query extraWhereClause .....
	//     ... or use current prop paths to build it ..
	//    ex:  buyer.loc.company.clients.products.campaigns
	//          => AND campaign.propduct.client.company = buyer.loc.company
	// create a method to have oaselect use UserAccess to get this qqqqqqqq

	public boolean updateSelect(OASelect sel) {
		return false; // no changes made
	}

	// todo? allow param to determine if user has access
	// ex:  buyer.isManager  ... if true then skip the rule

	/**
	 * Default values if no defined userAccess.
	 */
	private boolean bDefaultEnabled, bDefaultVisible;

	/**
	 * Children OAUserAccess that will be called with the return value from this.
	 */
	private final ArrayList<OAUserAccess> alOAUserAccess = new ArrayList<>();

	public OAUserAccess() {

	}

	public void setValidPackage(Package packageValid) {
		this.packageValid = packageValid;
	}

	/**
	 * Create new OAUserAccess that can be used to see if a propertyPath is enabled or visible.
	 */
	public OAUserAccess(boolean bDefaultEnabled, boolean bDefaultVisible) {
		this.bDefaultEnabled = bDefaultEnabled;
		this.bDefaultVisible = bDefaultVisible;
	}

	/**
	 * Add child UserAccess to chain together, where the return value from the parent will be the default value when checking the children.
	 *
	 * @param ua
	 */
	public void addUserAccess(OAUserAccess ua) {
		if (ua != null) {
			alOAUserAccess.add(ua);
		}
	}

	/**
	 * Keeps track of all defined propertyPaths, with root obj/hub.ao
	 */
	protected static class UserAccess {
		Hub hub;
		OAObject obj;
		OAPropertyPath pp, ppReverse;
		boolean bOnlyEndProperty;
		String[] props;

		//qqqqqqqqqq addIsUsedCheck(object, propPath, value)
		// add custom method isUsed(boolean bDefault) to override qqqqqqq

		public UserAccess(OAObject obj, String pp, boolean bOnlyEndProperty) {
			this.obj = obj;
			this.pp = new OAPropertyPath(obj.getClass(), pp);
			this.ppReverse = this.pp.getReversePropertyPath();
			this.bOnlyEndProperty = bOnlyEndProperty;
		}

		public UserAccess(Hub hub, String pp, boolean bOnlyEndProperty) {
			this.obj = obj;
			this.pp = new OAPropertyPath(hub.getObjectClass(), pp);
			this.ppReverse = this.pp.getReversePropertyPath();
			this.bOnlyEndProperty = bOnlyEndProperty;
		}

		public void setProperties(String... props) {
			this.props = props;
		}

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
		UserAccess ua = new UserAccess(obj, pp, bOnlyEndProperty);
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
		UserAccess ua = new UserAccess(hub, pp, bOnlyEndProperty);
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
		UserAccess ua = new UserAccess(obj, pp, bOnlyEndProperty);
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
		UserAccess ua = new UserAccess(hub, pp, bOnlyEndProperty);
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
		UserAccess ua = new UserAccess(obj, pp, bOnlyEndProperty);
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
		UserAccess ua = new UserAccess(hub, pp, bOnlyEndProperty);
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
		UserAccess ua = new UserAccess(obj, pp, bOnlyEndProperty);
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
		UserAccess ua = new UserAccess(hub, pp, bOnlyEndProperty);
		if (OAString.isNotEmpty(propertyName)) {
			ua.props = new String[] { propertyName };
		}
		alNotVisibleUserAccess.add(ua);
	}

	public boolean getEnabled(OAObject obj) {
		if (obj == null) {
			return false;
		}
		boolean b = getEnabled(obj, obj.getClass(), null, bDefaultEnabled);
		return b;
	}

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
	 * Checks to see if an OAObject & (optional) propertyName should be enabled. Uses the following steps: 0: starts with result set to
	 * default value (true/false) 1: checks if the class is enabled (result set to true). 2: checks if the class is not enabled (result set
	 * to false). 3: if property name, then checks if class+propertyName is enabled (result set to true). 4: if property name, then checks
	 * if class+propertyName is not enabled (result set to false). 5: checks to see if obj [& prop] are in the enabled propert paths (result
	 * set to true) 6: checks to see if obj [& prop] are in the not enabled propert paths (result set to false) 7: calls child[ren]
	 * recursively setting result. 8: returns result
	 */
	protected boolean getEnabled(final OAObject obj, final Class cz, final String propertyName, final boolean bDefault) {
		boolean bResult = bDefault;

		if (cz != null && packageValid != null) {
			if (packageValid.equals(cz.getPackage())) {
				return true; // allow other packages
			}
		}

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

			boolean b = getIsInSamePropertyPath(obj, propertyName, alEnabledUserAccess);
			if (b) {
				bResult = true;
			}
			b = getIsInSamePropertyPath(obj, propertyName, alNotEnabledUserAccess);
			if (b) {
				bResult = false;
			}
		}

		for (OAUserAccess ua : alOAUserAccess) {
			bResult = ua.getEnabled(obj, cz, propertyName, bResult);
		}
		return bResult;
	}

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

	/**
	 * Checks to see if an OAObject & (optional) propertyName should be visible. Uses the following steps: 1: starts with result set to
	 * default value (true/false) 1: checks if the class is enabled (result set to true). 2: checks if the class is not enabled (result set
	 * to false). 3: if property name, then checks if class+propertyName is enabled (result set to true). 4: if property name, then checks
	 * if class+propertyName is not enabled (result set to false). 5: checks to see if obj [& prop] are in the enabled propert paths (result
	 * set to true) 6: checks to see if obj [& prop] are in the not enabled propert paths (result set to false) 7: calls child[ren]
	 * recursively setting result. 8: returns result
	 */
	protected boolean getVisible(final OAObject obj, final Class cz, final String propertyName, final boolean bDefault) {
		if (cz != null && packageValid != null) {
			if (!packageValid.equals(cz.getPackage())) {
				return true; // allow other packages
			}
		}

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

			boolean b = getIsInSamePropertyPath(obj, propertyName, alVisibleUserAccess);
			if (b) {
				bResult = true;
			}
			b = getIsInSamePropertyPath(obj, propertyName, alNotVisibleUserAccess);
			if (b) {
				bResult = false;
			}
		}

		for (OAUserAccess ua : alOAUserAccess) {
			bResult = ua.getVisible(obj, cz, propertyName, bResult);
		}
		return bResult;
	}

	/**
	 * See if an Object is included in any of the Root obj/hub + propertyPaths. This is done by using the property path to search from the
	 * root obj/hub.at and then reversing the pp from the search object to find a common root.
	 */
	protected boolean getIsInSamePropertyPath(final OAObject objSearch, final String propertyName,
			final ArrayList<UserAccess> alUserAccess) {
		if (objSearch == null || alUserAccess == null) {
			return false;
		}
		final Class cz = objSearch.getClass();

		for (final UserAccess ua : alUserAccess) {
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

			// see if obj type is in ua propertyPath type of objects
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
				for (; k < lis.length; k++) {
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
