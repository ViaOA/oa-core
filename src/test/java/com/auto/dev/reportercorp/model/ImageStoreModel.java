package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ImageStore;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.object.OAObjectModel;

public class ImageStoreModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ImageStoreModel.class.getName());

	// Hubs
	protected Hub<ImageStore> hub;
	// selected imageStores
	protected Hub<ImageStore> hubMultiSelect;

	public ImageStoreModel() {
		setDisplayName("Image Store");
		setPluralDisplayName("Image Stores");
	}

	public ImageStoreModel(Hub<ImageStore> hubImageStore) {
		this();
		if (hubImageStore != null) {
			HubDelegate.setObjectClass(hubImageStore, ImageStore.class);
		}
		this.hub = hubImageStore;
	}

	public ImageStoreModel(ImageStore imageStore) {
		this();
		getHub().add(imageStore);
		getHub().setPos(0);
	}

	public Hub<ImageStore> getOriginalHub() {
		return getHub();
	}

	public ImageStore getImageStore() {
		return getHub().getAO();
	}

	public Hub<ImageStore> getHub() {
		if (hub == null) {
			hub = new Hub<ImageStore>(ImageStore.class);
		}
		return hub;
	}

	public Hub<ImageStore> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ImageStore>(ImageStore.class);
		}
		return hubMultiSelect;
	}

	public HubCopy<ImageStore> createHubCopy() {
		Hub<ImageStore> hubImageStorex = new Hub<>(ImageStore.class);
		HubCopy<ImageStore> hc = new HubCopy<>(getHub(), hubImageStorex, true);
		return hc;
	}

	public ImageStoreModel createCopy() {
		ImageStoreModel mod = new ImageStoreModel(createHubCopy().getHub());
		return mod;
	}
}
