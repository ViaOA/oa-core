// Generated by OABuilder
package com.messagedesigner.model.method;

import java.util.logging.Logger;

import com.messagedesigner.model.oa.method.MessageSourceSaveAsJsonResourceMethod;
import com.viaoa.hub.Hub;

public class MessageSourceSaveAsJsonResourceMethodModel {
	private static Logger LOG = Logger.getLogger(MessageSourceSaveAsJsonResourceMethodModel.class.getName());

	// Hubs
	protected Hub<MessageSourceSaveAsJsonResourceMethod> hub;

	// ObjectModels

	// object used for method data
	protected MessageSourceSaveAsJsonResourceMethod method;

	public MessageSourceSaveAsJsonResourceMethodModel() {
	}

	// object used to input query data, to be used by methodHub
	public MessageSourceSaveAsJsonResourceMethod getMethod() {
		if (method == null) {
			method = new MessageSourceSaveAsJsonResourceMethod();
		}
		return method;
	}

	public Hub<MessageSourceSaveAsJsonResourceMethod> getHub() {
		if (hub == null) {
			hub = new Hub<MessageSourceSaveAsJsonResourceMethod>(MessageSourceSaveAsJsonResourceMethod.class);
			hub.add(getMethod());
			hub.setPos(0);
		}
		return hub;
	}

}
