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
package com.viaoa.hub;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAString;

/**
 * Combines multiple hubs into one.
 */
public class HubCombined<T> {
	private static Logger LOG = Logger.getLogger(HubCombined.class.getName());
	private static final long serialVersionUID = 1L;

	protected final Hub<T> hubMaster;
	protected final ArrayList<Hub<T>> alHub = new ArrayList<>();
	protected ArrayList<HubListener<T>> alHubListener;
	protected final HubListener<T> hlMaster;
	protected Hub<T> hubFirst;
	protected volatile boolean bUpdatingMasterHub;

	public HubCombined(final Hub<T> hubMaster, final Hub<T>... hubs) {
		this.hubMaster = hubMaster;

		if (hubs != null) {
			for (Hub h : hubs) {
				add(h);
			}
		}

		hlMaster = new HubListenerAdapter<T>(this, "HubCombined.hubMaster", "") {
			@Override
			public void afterAdd(HubEvent<T> e) {
				if (bUpdatingMasterHub) {
					return;
				}
				T objx = e.getObject();
				boolean bUsed = true;
				for (Hub h : hubs) {
					if (h.contains(objx)) {
						bUsed = false;
						break;
					}
				}
				if (bUsed && hubFirst != null) {
					if (hubFirst.isValid()) {
						hubFirst.add(e.getObject());
					} else {
						//int xx = 4;
						//xx++;
					}
				}
			}

			@Override
			public void afterInsert(HubEvent<T> e) {
				afterAdd(e);
			}

			@Override
			public void afterRemove(HubEvent<T> e) {
				if (bUpdatingMasterHub) {
					return;
				}
				T obj = e.getObject();
				for (Hub<T> h : alHub) {
					h.remove(obj);
				}
			}

			@Override
			public void beforeRemoveAll(HubEvent<T> e) {
				if (bUpdatingMasterHub) {
					return;
				}
				for (T obj : hubMaster) {
					for (Hub<T> h : alHub) {
						h.remove(obj);
					}
				}
			}
		};
		hubMaster.addHubListener(hlMaster);
	}

	public Hub<T> getMasterHub() {
		return hubMaster;
	}

	public void close() {
		int i = 0;
		if (alHubListener != null) {
			for (Hub h : alHub) {
				h.removeHubListener(alHubListener.get(i++));
			}
			alHubListener.clear();
		}
		alHub.clear();
		if (hlMaster != null) {
			hubMaster.removeHubListener(hlMaster);
		}
	}

	public ArrayList<Hub<T>> getHubs() {
		return alHub;
	}

	public void add(final OAObject object, final String property) {
		if (object == null) {
			return;
		}
		if (OAString.isEmpty(property)) {
			return;
		}

		final Hub<T> hubNew = new Hub();
		T obj = (T) object.getProperty(property);
		if (obj != null) {
			hubNew.add(obj);
		}
		add(hubNew);

		final Hub hub = new Hub();
		hub.add(object);

		HubListener hl = new HubListenerAdapter(this, "HubCombined.object", "") {
			@Override
			public void afterPropertyChange(HubEvent e) {
				if (!property.equalsIgnoreCase(e.getPropertyName())) {
					return;
				}
				Object objn = e.getNewValue();
				Object objo = e.getOldValue();
				if (objn == objo) {
					return;
				}

				hub.clear();
				if (objn != null) {
					hub.add(objn);
				}
			}
		};
		hub.addHubListener(hl, property);
	}

	public void add(Hub<T> hub) {
		if (alHub.size() == 0) {
			hubFirst = hub;
		}
		alHub.add(hub);

		HubListener hl = new HubListenerAdapter<T>() {
			@Override
			public void afterAdd(HubEvent<T> e) {
				try {
					bUpdatingMasterHub = true;
					hubMaster.add(e.getObject());
				} finally {
					bUpdatingMasterHub = false;
				}
			}

			@Override
			public void afterInsert(HubEvent<T> e) {
				afterAdd(e);
			}

			@Override
			public void afterRemove(HubEvent<T> e) {
				T obj = e.getObject();
				boolean bUsed = false;
				for (Hub<T> hx : alHub) {
					if (hx.contains(obj)) {
						bUsed = true;
						break;
					}
				}
				if (!bUsed) {
					try {
						bUpdatingMasterHub = true;
						hubMaster.remove(obj);
					} finally {
						bUpdatingMasterHub = false;
					}
				}
			}

			@Override
			public void afterRemoveAll(HubEvent<T> e) {
				onNewList(e);
			}

			@Override
			public void onNewList(HubEvent<T> e) {
				try {
					bUpdatingMasterHub = true;
					OAThreadLocalDelegate.setLoading(true);
					for (Object obj : hubMaster) {
						boolean bUsed = false;
						for (Hub<T> hx : alHub) {
							if (hx.contains(obj)) {
								bUsed = true;
								break;
							}
						}
						if (!bUsed) {
							hubMaster.remove(obj);
						}
					}
					for (T obj : e.getHub()) {
						hubMaster.add(obj);
					}
				} finally {
					bUpdatingMasterHub = false;
					OAThreadLocalDelegate.setLoading(false);
				}
				HubEventDelegate.fireOnNewListEvent(hubMaster, true);
			}
		};
		hub.addHubListener(hl);
		if (alHubListener == null) {
			alHubListener = new ArrayList<HubListener<T>>();
		}
		alHubListener.add(hl);

		for (T obj : hub) {
			hubMaster.add(obj);
		}
	}

	public void refresh() {
		for (Object obj : hubMaster) {
			boolean bUsed = false;
			for (Hub<T> hx : alHub) {
				if (hx.contains(obj)) {
					bUsed = true;
					break;
				}
			}
			if (!bUsed) {
				hubMaster.remove(obj);
			}
		}
		for (Hub<T> hx : alHub) {
			for (T obj : hx) {
				hubMaster.add(obj);
			}
		}
	}

}
