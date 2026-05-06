package com.viaoa.graph.service.object;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.compare.OANotExist;
import com.viaoa.concurrent.OAThrottle;
import com.viaoa.find.OAFinder;
import com.viaoa.graph.sibling.OASiblingHelper;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.path.OAPath;

public abstract class OAObjectSiblingService {
	private static final Logger LOG = Logger.getLogger(OAObjectSiblingService.class.getName());


	public OAObjectSiblingService() {
	}

	/**
	 * Maximum number of milliseconds allowed for sibling-search operations
	 * before terminating the search early to maintain UI responsiveness.
	 */
	private final static long MaxMs = 25;

	/**
	 * Throttle used to limit logging frequency during debug mode.
	 * Prevents excessive console output when sibling lookups occur often.
	 */
	private static final OAThrottle throttle = new OAThrottle(2500);

	/**
	 * Notifies all thread-local OASiblingHelper instances that a reference
	 * property was accessed on the given object so they can record the link
	 * step for sibling detection.
	 *
	 * @param obj               the object whose reference was accessed
	 * @param linkPropertyName  the accessed link-property name
	 */
	public void onGetObjectReference(final OAObject obj, final String linkPropertyName) {
		List<OASiblingHelper<?>> al = callThreadLocalGetSiblingHelpers();
		if (al == null) {
			return;
		}

		for (OASiblingHelper<?> sh : al) {
			sh.onGetReference(obj, linkPropertyName);
		}

	}

	/**
	 * Convenience wrapper that delegates to the overloaded getSiblings method
	 * without an ignore map.
	 *
	 * @param mainObject the object requesting siblings
	 * @param property   the property name being accessed
	 * @param maxAmount  maximum number of siblings to return
	 * @return an array of sibling object keys
	 */
	public OAObjectKey[] getSiblings(final OAObject mainObject, final String property, final int maxAmount) {
		return getSiblings(mainObject, property, maxAmount, null);
	}

	/**
	 * Returns sibling objects that are likely to require the same property
	 * to be loaded. Enforces per-thread call limits, measures runtime, and
	 * delegates to the internal _getSiblings method.
	 *
	 * @param mainObject the object requesting sibling evaluation
	 * @param property   the property name being accessed
	 * @param maxAmount  maximum number of sibling keys to return
	 * @param hmIgnore   keys already being processed by concurrent requests
	 * @return an array of sibling object keys
	 */
	public OAObjectKey[] getSiblings(final OAObject mainObject, final String property, final int maxAmount,
			ConcurrentHashMap<UUID, Boolean> hmIgnore) {
		
		if (callThreadLocalGetAndIncrementGetSiblingCalledCount() > 0) {
			return new OAObjectKey[0];
		}

		long msStarted = System.currentTimeMillis();
		if (OAObject.getDebugMode()) {
			msStarted = 0L;
		}
		OAObjectKey[] keys = null;
		try {
			keys = _getSiblings(mainObject, property, maxAmount, hmIgnore, msStarted);
			/**
			 * testing if (keys == null || keys.length == 0) { keys = _getSiblings(mainObject, property, maxAmount, hmIgnore, msStarted); }
			 */
		} catch (Exception e) {
			// e.printStackTrace();// testing, can be removed
			throw new RuntimeException("OAObjectSiblingDelegate error", e);
		} finally {
			callThreadLocalClearGetSiblingCalledCount();
		}

		if (OAObject.getDebugMode()) {
			long x = msStarted == 0 ? 0 : (System.currentTimeMillis() - msStarted);
			if (throttle.check() || x > (MaxMs * 2)) {
				System.out.println((throttle.getCheckCount()) + ") OASiblingHelper " + x + "ms, obj="
						+ (mainObject == null ? "" : mainObject.getClass().getSimpleName()) + ", prop=" + property + ", sibs="
						+ (keys == null ? 0 : keys.length));
				// System.out.println((throttle.getCheckCount())+") OASiblingHelper "+x+"ms, obj="+mainObject.getClass().getSimpleName()+", prop="+property+", hmIgnore="+(hmIgnore==null?0:hmIgnore.size())+", alRemove="+keys.length);
			}
		}

		if (keys == null) {
			keys = new OAObjectKey[0];
		}
		return keys;
	}

	/**
	 * Container used to represent the result of a sibling-property resolution
	 * performed by an {@link OASiblingHelper}. Instances capture both the
	 * helper that produced the detail path and the resolved sibling property
	 * path itself.
	 *
	 * <p>DetailInfo objects are returned from calls that compute a detail
	 * property-path for a given source Hub or OAObject. They carry the
	 * resolved path along with the originating sibling helper so the caller
	 * can evaluate or cache the result.</p>
	 */
	protected static class DetailInfo {
		/**
		 * The sibling helper instance that produced the property-path used for
		 * sibling discovery. Provides context for resolving detail paths.
		 */
		OASiblingHelper<?> siblingHelper;

		/**
		 * Property-path expression returned by the associated sibling helper.
		 * Used as input when evaluating hubs and reachable objects for
		 * potential siblings.
		 */
		String getDetailPropertyPath;

		DetailInfo(OASiblingHelper<?> siblingHelper, String getDetailPropertyPath) {
			this.siblingHelper = siblingHelper;
			this.getDetailPropertyPath = getDetailPropertyPath;
		}
	}

	/**
	 * Internal implementation for locating sibling objects. Examines learned
	 * property paths, evaluates hub relationships, scans nearby hub objects,
	 * and applies time and recursion limits.
	 *
	 * @param mainObject the object requesting siblings
	 * @param property   the property being accessed
	 * @param maxAmount  maximum number of siblings to return
	 * @param hmIgnore   map of objects to skip during evaluation
	 * @param msStarted  start time for timeout budgeting
	 * @return an array of sibling object keys, or null if invalid input
	 */
	private OAObjectKey[] _getSiblings(final OAObject mainObject, final String property, final int maxAmount,
			ConcurrentHashMap<UUID, Boolean> hmIgnore, final long msStarted) {
		if (mainObject == null || OAString.isEmpty(property) || maxAmount < 1) {
			return null;
		}
		if (hmIgnore == null) {
			hmIgnore = new ConcurrentHashMap<>();
		}
		final OALinkInfo linkInfo = callInfoGetLinkInfo(mainObject.getClass(), property);

		// set by Finder, HubMerger, HubGroupBy, LoadReferences, etc - where it will be loading from a Root Hub using a PropertyPath

		Hub<?> getDetailHub = null;
		String getDetailPropertyPath = null;

		OAPath<?> ppGetDetailPropertyPath = null;

		// 20180704
		List<OASiblingHelper<?>> al = callThreadLocalGetSiblingHelpers();

		// 20180807 find all pp to use, instead of just the first one.
		ArrayList<DetailInfo> alDetailInfo = new ArrayList<>();
		if (al != null) {
			for (OASiblingHelper<?> sh : al) {
				for (int i = 0;; i++) {
					String s = sh.getPropertyPath(mainObject, property, i > 0);
					if (s == null) {
						break;
					}
					DetailInfo di = new DetailInfo(sh, s);
					alDetailInfo.add(di);
					if (alDetailInfo.size() >= 5) {
						break;
					}
				}
				if (alDetailInfo.size() >= 5) {
					break;
				}
			}
		}

		final ArrayList<OAObjectKey> alObjectKey = new ArrayList<>();
		final HashMap<OAObjectKey, OAObjectKey> hsKeys = new HashMap<>();
		boolean bDone = false;

		// 20180807
		for (int cntDetailInfo = 0; !bDone; cntDetailInfo++) {
			if (cntDetailInfo >= alDetailInfo.size()) {
				if (cntDetailInfo > 0) {
					break;
				}
			} else {
				DetailInfo di = alDetailInfo.get(cntDetailInfo);
				getDetailHub = di.siblingHelper.getHub();
				getDetailPropertyPath = di.getDetailPropertyPath;
				ppGetDetailPropertyPath = new OAPath<>(di.siblingHelper.getHub().getObjectClass(), getDetailPropertyPath);
			}

			String ppPrefix = null;
			boolean bValid = false;
			if (ppGetDetailPropertyPath != null) {
				// find property is in the detailPP, and build the ppPrefix from the getDetailHub
				boolean b = false;
				for (OALinkInfo li : ppGetDetailPropertyPath.getLinkInfos()) {
					if (property.equalsIgnoreCase(li.getName())) {
						bValid = true;
						break;
					}
					if (b) {
						// found mainObj, but the next prop in pp was not not a match, see if pp can be truncated
						b = false;
						OALinkInfo lix = callInfoGetLinkInfo(mainObject.getClass(), property);
						if (lix != null) {
							bValid = true;
							break;
						}
					}
					if (mainObject.getClass().equals(li.getToClass())) {
						b = true;
					}

					if (ppPrefix == null) {
						if (!li.getRecursive() || !li.getToClass().equals(mainObject.getClass())) {
							ppPrefix = li.getName();
						}
					} else {
						ppPrefix += "." + li.getName();
					}
				}
				if (b) {
					OALinkInfo lix = callInfoGetLinkInfo(mainObject.getClass(), property);
					if (lix != null) {
						bValid = true;
					}
				}

				if (!bValid) {
					// see if property is off of the detailPP
					ppPrefix = null;
					for (OALinkInfo li : ppGetDetailPropertyPath.getLinkInfos()) {
						Class<?> c = li.getToClass();
						OALinkInfo lix = callInfoGetLinkInfo(c, mainObject.getClass());
						if (lix != null) {
							if (!lix.getPrivateMethod()) {
								bValid = true;
								break;
							}
						}
						if (ppPrefix == null) {
							ppPrefix = li.getName();
						} else {
							ppPrefix += "." + li.getName();
						}
					}
				}
			}

			if (!bValid && getDetailHub != null && !getDetailHub.getObjectClass().equals(mainObject.getClass())) {
				// need to get to mainObject.class
				Class<?> c = getDetailHub.getObjectClass();
				OALinkInfo li = callInfoGetLinkInfo(c, mainObject.getClass());
				if (li == null || li.getPrivateMethod()) {
					getDetailHub = null;
					ppPrefix = null;
					bValid = false;
				} else {
					ppPrefix = li.getName();
					bValid = true;
				}
			}

			Hub hub = null;
			OAPath ppReverse = null;

			if (getDetailHub != null && ppPrefix != null) {
				OAPath<?> ppForward = new OAPath<>(getDetailHub.getObjectClass(), ppPrefix);
				OALinkInfo[] lis = ppForward.getLinkInfos();
				boolean b = true;
				if (lis != null) {
					for (OALinkInfo li : lis) {
						if (li.getType() != OALinkInfo.TYPE_MANY) {
							b = false;
							break;
						}
					}
				}
				if (b) {
					ppReverse = ppForward.getReversePropertyPath();
				}
			}

			OAObject objInHub = mainObject;
			int ppReversePos = 0;
			boolean bCalledFindBestSiblingHub = false;

			if (ppReverse != null) {
				OALinkInfo[] lis = ppReverse.getLinkInfos();
				OALinkInfo lix = null;
				if (lis != null && lis.length > 0) {
					lix = lis[0];
				}
				hub = findBestSiblingHub(mainObject, lix);
				bCalledFindBestSiblingHub = true;
				ppPrefix = null;
				if (hub == null || callHubDetailGetLinkInfoFromDetailToMaster(hub) != lix) {
					ppReverse = null;
				}
			} else if (getDetailHub != null) {
				hub = getDetailHub;
				if (ppPrefix != null) {
					OAFinder f = new OAFinder(ppPrefix) {
						@Override
						protected boolean isUsed(OAObject obj) {
							return obj == mainObject;
						}
					};
					f.setUseOnlyLoadedData(true);
					f.setAllowRecursiveRoot(true); // 20180705
					if (f.findFirst(hub) == null) {
						objInHub = null;
					} else {
						objInHub = (OAObject) hub.getAt(f.getRootHubPos());
					}
				}
			} else {
				hub = findBestSiblingHub(mainObject, null);
				bCalledFindBestSiblingHub = true;
				ppPrefix = null;
			}

			final HashSet<Hub> hsHubVisited = new HashSet<>();
			final HashMap<OAObjectKey, OAObject> hmTypeOneObjKey = new HashMap<>();

			for (int ix = 0; ix < 2 && !bDone; ix++) {
				if (ix == 1) {
					if (bCalledFindBestSiblingHub) {
						break;
					}

					if (alDetailInfo != null && alDetailInfo.size() > 1) {
						break;
					}

					objInHub = mainObject;
					hub = findBestSiblingHub(mainObject, null);
					ppPrefix = null;
					ppReverse = null;
				}

				for (int cnt = 0; hub != null; cnt++) {
					if (hsHubVisited.contains(hub)) {
						break;
					}
					hsHubVisited.add(hub);

					int startPosHubRoot = hub.getPos(objInHub);
					int x = maxAmount;
					for (int i = 0; i <= cnt; i++) {
						x /= 2;
					}
					x = Math.min(x, 25);
					startPosHubRoot = Math.max(0, startPosHubRoot - x);

					findSiblings(	alObjectKey, hub, startPosHubRoot, ppPrefix, property, linkInfo, mainObject, hmTypeOneObjKey, hmIgnore,
									maxAmount, msStarted, cnt);
					if (alObjectKey.size() >= maxAmount) {
						bDone = true;
						break;
					}

					if (msStarted > 0) {
						long lx = (System.currentTimeMillis() - msStarted);
						if (lx > MaxMs) { //  && !OAObject.getDebugMode()) {
							bDone = true;
							break;
						}
					}
					if (cnt > 3) {
						break;
					}

					// find next hub to use

					final OALinkInfo lix = callHubDetailGetLinkInfoFromMasterHubToDetail(hub);
					if (lix == null || lix.getToClass() == null) {
						//bDone = true;
						break; // could be using GroupBy as hub
					}

					if (ppPrefix == null) {
						ppPrefix = lix.getName();
					} else {
						ppPrefix = lix.getName() + "." + ppPrefix;
					}

					objInHub = hub.getMasterObject();

					Hub<?> hubx = null;
					if (ppReverse != null && objInHub != null) {
						OALinkInfo[] lis = ppReverse.getLinkInfos();
						OALinkInfo liz = (lis == null || lis.length <= ppReversePos) ? null : lis[ppReversePos];
						ppReversePos++;
						if (liz != null && liz.getToClass().equals(objInHub.getClass())) {
							hubx = findBestSiblingHub(objInHub, liz);
							if (hubx == null) {
								ppReverse = null;
							} else if (callHubDetailGetLinkInfoFromMasterToDetail(hubx) != liz.getReverseLinkInfo()) {
								ppReverse = null;
							}
							hub = hubx;
						} else {
							ppReverse = null;
						}
					} else {
						ppReverse = null;
					}

					if (hubx == null && hub != null) {
						hubx = hub.getMasterHub();
						if (hubx != null) {
							hub = hubx;
						} else {
							if (objInHub == null) {
								break;
							}
							hub = findBestSiblingHub(objInHub, null);
						}
					}
				}
			}
		}
		int x = alObjectKey.size();
		OAObjectKey[] keys = new OAObjectKey[x];
		alObjectKey.toArray(keys);

		return keys;
	}

	/**
	 * Scans the given hub for objects that require the same property to be
	 * loaded. Uses an OAFinder with loaded-data constraints and adds each
	 * qualifying object's key to the results.
	 *
	 * @param alFoundObjectKey list collecting found sibling keys
	 * @param hubRoot          the hub to scan
	 * @param startPosHubRoot  starting hub index for scanning
	 * @param finderPropertyPath the property path used for scanning
	 * @param origProperty     the original property being accessed
	 * @param linkInfo         metadata describing the property link
	 * @param mainObject       the object requesting siblings
	 * @param hmTypeOneObjKey  per-thread one-to-one key tracking
	 * @param hmIgnore         map of objects to skip
	 * @param maxAmount        maximum number of siblings to find
	 * @param msStarted        start time for enforcing time limits
	 * @param runCount         recursion/iteration counter
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public void findSiblings(
			final ArrayList<OAObjectKey> alFoundObjectKey,
			final Hub<?> hubRoot, final int startPosHubRoot, final String finderPropertyPath, final String origProperty,
			final OALinkInfo linkInfo,
			final OAObject mainObject,
			final HashMap<OAObjectKey, OAObject> hmTypeOneObjKey, // for calling thread, refobjs already looked at
			final ConcurrentHashMap<UUID, Boolean> hmIgnore, // for all threads
			final int maxAmount,
			final long msStarted,
			final int runCount) {
		final String property = origProperty.toUpperCase();
		final boolean bIsMany = (linkInfo != null) && (linkInfo.getType() == OALinkInfo.TYPE_MANY);
		boolean b = !bIsMany && (linkInfo != null) && (linkInfo.isOne2One());
		if (b) {
			OALinkInfo rli = linkInfo.getReverseLinkInfo();
			if (!linkInfo.getPrivateMethod() && rli != null && rli.getPrivateMethod()) {
				b = false;
			}
		}
		final boolean bNormalOne2One = b;

		final Class<? extends OAObject> clazz = (linkInfo == null) ? null : linkInfo.getToClass();

		OAFinder f = new OAFinder(finderPropertyPath) {
			@Override
			protected boolean isUsed(OAObject oaObject) {
				if (oaObject == mainObject) {
					return false;
				}

				Object propertyValue = callPropertyGetProperty(oaObject, property, true, true);

				if (bIsMany) {
					if (propertyValue instanceof Hub) {
						return false;
					}
				} else if (linkInfo != null && propertyValue instanceof OAObject) {
					return false;
				} else if (linkInfo != null && propertyValue instanceof OAObjectKey) {
					if (hmTypeOneObjKey.containsKey((OAObjectKey) propertyValue)) {
						return false;
					}
					hmTypeOneObjKey.put((OAObjectKey) propertyValue, null);
					if (callCacheGet(clazz, (OAObjectKey) propertyValue) != null) {
						return false;
					}
				} else if (linkInfo != null) {
					if (!bNormalOne2One) {
						return false;
					}
				} else if (linkInfo == null) { // must be blob
					if (!(propertyValue instanceof OANotExist)) {
						return false;
					}
				}

				boolean bExisted = hmIgnore.put(oaObject.getGuid(), Boolean.TRUE) != null;

				if (!bExisted) {
					OAObjectKey ok = oaObject.getObjectKey();
					alFoundObjectKey.add(ok);
					if (alFoundObjectKey.size() >= maxAmount) {
						stop();
					}
				}
				return false; // always returns
			}

			@Override
			protected void find(Object obj, int pos) {
				super.find(obj, pos);
				if (msStarted > 0) {
					long lx = (System.currentTimeMillis() - msStarted);
					if (lx > MaxMs) { // && !OAObject.getDebugMode()) {
						stop();
					}
				}
			}
		};
		f.setUseOnlyLoadedData(true);
		OAObject objx = null;
		if (startPosHubRoot > 0) {
			objx = (OAObject) hubRoot.getAt(startPosHubRoot - 1);
		}
		
		f.find(hubRoot, objx);
	}

	/**
	 * Returns the hub that provides the best candidate set of sibling objects
	 * for the given master object, using link alignment and hub hierarchy
	 * scoring.
	 *
	 * @param masterObject the object whose hubs are being evaluated
	 * @param liToMaster   optional link-restriction for selecting the hub
	 * @return the hub best suited for sibling evaluation, or null if none match
	 */
	public Hub<?> findBestSiblingHub(OAObject masterObject, OALinkInfo liToMaster) {
		Hub[] hubs = callHubGetHubReferences(masterObject);

		int siblingHits = 0;
		Hub siblingHub = null;

		for (int i = 0; (hubs != null && i < hubs.length); i++) {
			Hub hub = hubs[i];
			if (hub == null) {
				continue;
			}

			if (liToMaster != null && callHubDetailGetLinkInfoFromDetailToMaster(hub) == liToMaster) {
				siblingHub = hub;
				break;
			}

			int hits = 1;
			if (hub.getMasterHub() != null) {
				hits += 3;
			} else if (hub.getMasterObject() != null) {
				hits += 2;
			}

			if (hits > siblingHits) {
				siblingHits = hits;
				siblingHub = hub;
			} else if (hits == siblingHits) {
				if (hub.getSize() > siblingHub.getSize()) {
					siblingHub = hub;
				}
			}
		}
		return siblingHub;
	}

	public abstract OALinkInfo callInfoGetLinkInfo(Class<?> clazz, String propertyName);
	public abstract OALinkInfo callInfoGetLinkInfo(Class<?> fromClass, Class<?> toClass); 
	public abstract Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef);
	public abstract <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok);
	public abstract Hub[] callHubGetHubReferences(OAObject oaObj); 
	public abstract OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub);
	public abstract OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<?> thisDetailHub);
	public abstract OALinkInfo callHubDetailGetLinkInfoFromMasterToDetail(Hub<?> thisDetailHub);
	public abstract List<OASiblingHelper<?>> callThreadLocalGetSiblingHelpers();
	public abstract int callThreadLocalGetAndIncrementGetSiblingCalledCount();
	public abstract void callThreadLocalClearGetSiblingCalledCount();
	
}
