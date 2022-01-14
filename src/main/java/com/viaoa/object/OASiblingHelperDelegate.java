/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.util.OANotExist;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;
import com.viaoa.util.OAThrottle;

/**
 * Find the closet siblings objects that need the same property loaded. Used by DS and CS to be able to get extra data per request to
 * server/datasource, and increase performance.
 *
 * @author vvia
 */
public class OASiblingHelperDelegate {

	private final static long MaxMs = 25; // max ms for finding

	private static final OAThrottle throttle = new OAThrottle(2500);

	/**
	 * used by OAObject.getReference so taht siblingHelpers can update their pp
	 */
	public static void onGetObjectReference(final OAObject obj, final String linkPropertyName) {
		ArrayList<OASiblingHelper> al = OAThreadLocalDelegate.getSiblingHelpers();
		if (al == null) {
			return;
		}

		for (OASiblingHelper sh : al) {
			sh.onGetReference(obj, linkPropertyName);
		}

	}

	/**
	 * Used to find any siblings that also need the same property loaded.
	 */
	public static OAObjectKey[] getSiblings(final OAObject mainObject, final String property, final int maxAmount) {
		return getSiblings(mainObject, property, maxAmount, null);
	}

	/**
	 * @param mainObject
	 * @param property
	 * @param maxAmount
	 * @param hmIgnore   ignore list, because they are "inflight" with other concurrent requests
	 * @return list of keys that are siblings
	 */
	public static OAObjectKey[] getSiblings(final OAObject mainObject, final String property, final int maxAmount,
			ConcurrentHashMap<Integer, Boolean> hmIgnore) {
		OAThreadLocal tl = OAThreadLocalDelegate.getThreadLocal(true);
		if (tl.cntGetSiblingCalled++ > 0) {
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
			tl.cntGetSiblingCalled = 0;
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

	protected static class DetailInfo {
		OASiblingHelper siblingHelper;
		String getDetailPropertyPath;

		DetailInfo(OASiblingHelper siblingHelper, String getDetailPropertyPath) {
			this.siblingHelper = siblingHelper;
			this.getDetailPropertyPath = getDetailPropertyPath;
		}
	}

	private static OAObjectKey[] _getSiblings(final OAObject mainObject, final String property, final int maxAmount,
			ConcurrentHashMap<Integer, Boolean> hmIgnore, final long msStarted) {
		if (mainObject == null || OAString.isEmpty(property) || maxAmount < 1) {
			return null;
		}
		if (hmIgnore == null) {
			hmIgnore = new ConcurrentHashMap<>();
		}
		final OALinkInfo linkInfo = OAObjectInfoDelegate.getLinkInfo(mainObject.getClass(), property);

		// set by Finder, HubMerger, HubGroupBy, LoadReferences, etc - where it will be loading from a Root Hub using a PropertyPath

		Hub getDetailHub = null;
		String getDetailPropertyPath = null;

		OAPropertyPath ppGetDetailPropertyPath = null;

		// 20180704
		ArrayList<OASiblingHelper> al = OAThreadLocalDelegate.getSiblingHelpers();

		// 20180807 find all pp to use, instead of just the first one.
		ArrayList<DetailInfo> alDetailInfo = new ArrayList<>();
		if (al != null) {
			for (OASiblingHelper sh : al) {
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
				ppGetDetailPropertyPath = new OAPropertyPath(di.siblingHelper.getHub().getObjectClass(), getDetailPropertyPath);
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
						OALinkInfo lix = OAObjectInfoDelegate.getLinkInfo(mainObject.getClass(), property);
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
					OALinkInfo lix = OAObjectInfoDelegate.getLinkInfo(mainObject.getClass(), property);
					if (lix != null) {
						bValid = true;
					}
				}

				if (!bValid) {
					// see if property is off of the detailPP
					ppPrefix = null;
					for (OALinkInfo li : ppGetDetailPropertyPath.getLinkInfos()) {
						Class c = li.getToClass();
						OALinkInfo lix = OAObjectInfoDelegate.getLinkInfo(c, mainObject.getClass());
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
				Class c = getDetailHub.getObjectClass();
				OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(c, mainObject.getClass());
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
			OAPropertyPath ppReverse = null;

			if (getDetailHub != null && ppPrefix != null) {
				OAPropertyPath ppForward = new OAPropertyPath(getDetailHub.getObjectClass(), ppPrefix);
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
				if (hub == null || HubDetailDelegate.getLinkInfoFromDetailToMaster(hub) != lix) {
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

					final OALinkInfo lix = HubDetailDelegate.getLinkInfoFromMasterHubToDetail(hub);
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

					Hub hubx = null;
					if (ppReverse != null && objInHub != null) {
						OALinkInfo[] lis = ppReverse.getLinkInfos();
						OALinkInfo liz = (lis == null || lis.length <= ppReversePos) ? null : lis[ppReversePos];
						ppReversePos++;
						if (liz != null && liz.getToClass().equals(objInHub.getClass())) {
							hubx = findBestSiblingHub(objInHub, liz);
							if (hubx == null) {
								ppReverse = null;
							} else if (HubDetailDelegate.getLinkInfoFromMasterToDetail(hubx) != liz.getReverseLinkInfo()) {
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

	protected static void findSiblings(
			final ArrayList<OAObjectKey> alFoundObjectKey,
			final Hub hubRoot, final int startPosHubRoot, final String finderPropertyPath, final String origProperty,
			final OALinkInfo linkInfo,
			final OAObject mainObject,
			final HashMap<OAObjectKey, OAObject> hmTypeOneObjKey, // for calling thread, refobjs already looked at
			final ConcurrentHashMap<Integer, Boolean> hmIgnore, // for all threads
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

		final Class clazz = (linkInfo == null) ? null : linkInfo.getToClass();

		OAFinder f = new OAFinder(finderPropertyPath) {
			@Override
			protected boolean isUsed(OAObject oaObject) {
				if (oaObject == mainObject) {
					return false;
				}

				Object propertyValue = OAObjectPropertyDelegate.getProperty(oaObject, property, true, true);

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
					if (OAObjectCacheDelegate.get(clazz, (OAObjectKey) propertyValue) != null) {
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
					if (ok.guid == 0) {
						ok.guid = oaObject.getGuid();
					}

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

	// find the Hub that has the best set of siblings
	public static Hub findBestSiblingHub(OAObject masterObject, OALinkInfo liToMaster) {
		Hub[] hubs = OAObjectHubDelegate.getHubReferences(masterObject);

		int siblingHits = 0;
		Hub siblingHub = null;

		for (int i = 0; (hubs != null && i < hubs.length); i++) {
			Hub hub = hubs[i];
			if (hub == null) {
				continue;
			}

			if (liToMaster != null && HubDetailDelegate.getLinkInfoFromDetailToMaster(hub) == liToMaster) {
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
}
