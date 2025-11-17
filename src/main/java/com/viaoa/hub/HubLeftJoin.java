/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
package com.viaoa.hub;

import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.object.*;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAString;

/**
 * Creates a live "left join" view between two {@link Hub}s, conceptually similar
 * to a SQL LEFT JOIN.
 *
 * <p>Produces a combined Hub of {@link OALeftJoin}&lt;A,B&gt; where each row contains
 * references to paired A (left) and B (right) objects.</p>
 *
 * <p><b>Features</b>:
 * <ul>
 *   <li>Maintains full two-way synchronization via Hub listeners.</li>
 *   <li>Propagates Active-Object changes between the joined and source Hubs.</li>
 *   <li>Supports property-path joins and auto-updates on right-side property changes.</li>
 * </ul>
 *
 * <p>Ideal for UI or analytical composite lists spanning related domains.</p>
 */
public class HubLeftJoin<A extends OAObject, B extends OAObject> {

	private Hub<A> hubA;
	private Hub<B> hubB;
	private Hub<OALeftJoin<A, B>> hubCombined;
	private String propertyPath;
	private String listenPropertyName;
	private boolean bSetAO;

	private final static AtomicInteger aiCnt = new AtomicInteger();

	/**
	 * Combine a left and right hubs on a propertyPath to form Hub.
	 *
	 * @param hubA         left object
	 * @param hubB         right object
	 * @param propertyPath pp of the property from the right object to get left object.
	 */
	public HubLeftJoin(Hub<A> hubA, Hub<B> hubB, String propertyPath) {
		this(hubA, hubB, propertyPath, true);
	}

	public HubLeftJoin(Hub<A> hubA, Hub<B> hubB, String propertyPath, boolean bSetAO) {
		this.hubA = hubA;
		this.hubB = hubB;
		this.bSetAO = bSetAO;
		this.propertyPath = propertyPath;
		setup();
	}

	/**
	 * @return Hub of combined objects
	 */
	public Hub<OALeftJoin<A, B>> getCombinedHub() {
		if (hubCombined != null) {
			return hubCombined;
		}
		hubCombined = new Hub(OALeftJoin.class);
		return hubCombined;
	}

	void setup() {
		getCombinedHub().addHubListener(new HubListenerAdapter<OALeftJoin<A, B>>() {
			@Override
			public void afterChangeActiveObject(HubEvent e) {
				// set the active object in hub A&B when hubCombine.AO is changed
				OALeftJoin obj = (OALeftJoin) e.getObject();
				if (obj == null) {
					hubA.setAO(null);
					hubB.setAO(null);
				} else {
					hubA.setAO(obj.getA());
					hubB.setAO(obj.getB());
				}
			}

			@Override
			public void afterPropertyChange(HubEvent<OALeftJoin<A, B>> e) {
				String name = e.getPropertyName();
				if (OAString.isEmpty(name)) {
					return;
				}
				if (!"b".equalsIgnoreCase(name)) {
					return;
				}

				OALeftJoin lj = e.getObject();
				if (lj == null) {
					return;
				}
				Object oldValue = e.getOldValue();
				Object newValue = e.getNewValue();

				if (OACompare.isEqual(oldValue, newValue)) {
					return;
				}

				if (oldValue != null) {
					hubB.remove(oldValue);
				}
				if (newValue != null) {
					hubB.add((B) newValue);
				}
			}

		});

		hubA.addHubListener(new HubListenerAdapter() {
			@Override
			public void afterInsert(HubEvent e) {
				afterAdd(e);
			}

			@Override
			public void afterAdd(HubEvent e) {
				A a = (A) e.getObject();
				OALeftJoin<A, B> c = new OALeftJoin(a, null);
				hubCombined.add(c);
			}

			@Override
			public void afterRemove(HubEvent e) {
				A a = (A) e.getObject();
				for (;;) {
					OALeftJoin c = hubCombined.find(OALeftJoin.P_A, a);
					if (c == null) {
						break;
					}
					hubCombined.remove(c);
				}
			}

			@Override
			public void onNewList(HubEvent e) {
				hubCombined.clear();
				for (A a : hubA) {
					hubCombined.add(new OALeftJoin(a, null));
				}
				for (B b : hubB) {
					add(b);
				}
			}
		});

		for (A a : hubA) {
			hubCombined.add(new OALeftJoin(a, null));
		}

		HubListener hl = new HubListenerAdapter() {
			@Override
			public void afterInsert(HubEvent e) {
				afterAdd(e);
			}

			@Override
			public void afterAdd(HubEvent e) {
				B b = (B) e.getObject();
				add(b);
			}

			@Override
			public void afterRemove(HubEvent e) {
				B b = (B) e.getObject();
				remove(b);
			}

			@Override
			public void afterPropertyChange(HubEvent e) {
				String s = e.getPropertyName();
				if (!listenPropertyName.equalsIgnoreCase(s)) {
					return;
				}

				remove((B) e.getObject());
				add((B) e.getObject());
			}

			@Override
			public void onNewList(HubEvent e) {
				HubAddRemoveDelegate.clear(hubCombined, false, false); // 20240403 dont send newList event
				OAThreadLocalDelegate.setLoading(true);
				try {
    				for (A a : hubA) {
    					hubCombined.add(new OALeftJoin(a, null));
    				}
    				for (B b : hubB) {
    					add(b);
    				}
				}
				finally {
	                OAThreadLocalDelegate.setLoading(false);
                    hubCombined.setActiveObject(null);
	                HubEventDelegate.fireOnNewListEvent(hubCombined, true);
				}
			}

			@Override
			public void afterChangeActiveObject(HubEvent e) {
				B b = (B) e.getObject();
				OALeftJoin lj;
				if (b != null) {
					lj = hubCombined.find(OALeftJoin.P_B, b);
				} else {
					lj = null;
				}
				hubCombined.setAO(lj);
			}
		};

		if (propertyPath == null || propertyPath.indexOf('.') < 0) {
			listenPropertyName = propertyPath;
			hubB.addHubListener(hl, propertyPath);
		} else {
			listenPropertyName = "hubCombined" + aiCnt.getAndIncrement();
			hubB.addHubListener(hl, listenPropertyName, new String[] { propertyPath });
		}

		for (B b : hubB) {
			add(b);
		}
	}

	private void add(B b) {
		Object valueA = b.getProperty(propertyPath);

		boolean bFound = false;
		OALeftJoin ljEmpty = null;
		for (OALeftJoin lj : hubCombined) {
			if (lj.getA() != valueA) {
				continue;
			}
			B bx = (B) lj.getB();
			if (bx == b) {
				bFound = true;
				break;
			}
			if (bx == null) {
				ljEmpty = lj;
			}
		}
		if (!bFound) {
			if (ljEmpty != null) {
				ljEmpty.setB(b);
			} else {
				OALeftJoin ljx = new OALeftJoin();
				ljx.setA((A) valueA);
				ljx.setB(b);
				hubCombined.add(ljx);
			}
		}
	}

	private void remove(B b) {
		OALeftJoin found = null;
		for (OALeftJoin lj : hubCombined) {
			B bx = (B) lj.getB();
			if (bx == b) {
				found = lj;
				break;
			}
		}
		if (found == null) {
			return;
		}

		Object valueA = found.getA();
		boolean bFoundAnother = false;

		for (OALeftJoin lj : hubCombined) {
			if (lj.getA() != valueA) {
				continue;
			}

			B bx = (B) lj.getB();
			if (bx != b) {
				bFoundAnother = true;
				break;
			}
		}

		if (bFoundAnother) {
			hubCombined.remove(found);
		} else {
			found.setB(null);
		}
	}
}
