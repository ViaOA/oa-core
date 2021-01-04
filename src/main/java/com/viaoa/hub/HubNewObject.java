package com.viaoa.hub;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectDSDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;

/**
 * This is used to allow for creating new object for a hub, but waiting until it is ready to be submitted before adding it to the Hub.
 * <p>
 * This is done by using a second hub to contain the new object. This second hub will be configured to act the same as the mainHub. For
 * example, if the mainHub has a masterObject/Hub.
 * <p>
 * This can be used by wizards that create new objects, or web pages, etc. to allow a new object to be created and then added to the mainHub
 * when it is ready to submit.
 *
 * @author vvia
 * @param <F>
 */
public class HubNewObject<F extends OAObject> {

	private final Hub<F> hubMain;
	private Hub<F> hubNewObject;

	/**
	 * @param hubMain      "real" hub that wants to have a second hub used for creating new objects.
	 * @param hubNewObject "temp" hub that is used for holding new objects, and then adding to hubMain when submit is called.
	 */
	public HubNewObject(Hub<F> hubMain, Hub<F> hubNewObject) {
		this.hubMain = hubMain;
		this.hubNewObject = hubNewObject;
		setup();
	}

	public HubNewObject(Hub<F> hubMain) {
		this.hubMain = hubMain;
		setup();
	}

	public Hub<F> getNewObjectHub() {
		return hubNewObject;
	}

	public Hub<F> getMainHub() {
		return hubMain;
	}

	protected void setup() {
		if (hubNewObject == null) {
			hubNewObject = new Hub(hubMain.getObjectClass());
		}

		hubNewObject.setSelectWhereHub(	HubSelectDelegate.getSelectWhereHub(hubMain),
										HubSelectDelegate.getSelectWhereHubPropertyPath(hubMain));

		// need to set up a filtered hub, so that hubNewObject can be associated with hubMain and it's masterObject/Hub, etc
		Hub hubEmptyFiltered = new Hub(hubMain.getObjectClass());
		HubFilter hf = new HubFilter(hubMain, hubEmptyFiltered) {
			public boolean isUsed(Object object) {
				return false;
			}
		};

		new HubCombined(new Hub(), hubEmptyFiltered, hubNewObject);

		hubNewObject.onAdd((event) -> {
			Object obj = event.getObject();
			if (!(obj instanceof OAObject)) {
				return;
			}
			((OAObject) obj).setAutoAdd(false);
		});

		hubNewObject.onRemove((event) -> {
			Object obj = event.getObject();
			if (!(obj instanceof OAObject)) {
				return;
			}
			((OAObject) obj).setAutoAdd(true);
		});
	}

	/**
	 * Move objects in hubNewObject to hubMain.
	 */
	public void submit() {
		OAObject obj = hubNewObject.getAO();

		if (obj != null) {
			OAObjectKey ok = obj.getObjectKey();
			if (obj.isNew() && ok.isEmpty()) {
				obj.setObjectDefaults();
				if (OAObjectDSDelegate.getAssignIdOnCreate(obj)) {
					OAObjectDSDelegate.assignId(obj);
				}
			}
		}

		hubMain.add(hubNewObject);
		hubNewObject.clear();
		hubMain.setAO(obj);
	}

	public void cancel() {
		OAObject obj = hubNewObject.getAO();
		hubNewObject.clear();

		if (obj != null) {
			OAObjectKey ok = obj.getObjectKey();
			if (obj.isNew() && ok.isEmpty()) {
				obj.delete();
			}
		}
	}

	/**
	 * Create a new object that can then be added to hubNewObject;
	 * 
	 * @return
	 */
	public F createNewObject() {
		F obj = null;
		try {
			OAThreadLocalDelegate.setLoading(true);
			Class<F> clazz = hubMain.getObjectClass();
			obj = (F) OAObjectReflectDelegate.createNewObject(clazz);
		} finally {
			OAThreadLocalDelegate.setLoading(false);
		}
		if (obj instanceof OAObject) {
			OAObjectDelegate.initializeAfterLoading((OAObject) obj, false, false);
		}
		return obj;
	}

}
