// Generated by OABuilder
package com.messagedesigner.model.filter;

import java.util.logging.Logger;

import com.messagedesigner.model.oa.MessageType;
import com.messagedesigner.model.oa.filter.MessageTypeChangedFilter;
import com.viaoa.hub.Hub;

public class MessageTypeChangedFilterModel {
	private static Logger LOG = Logger.getLogger(MessageTypeChangedFilterModel.class.getName());

	// Hubs
	protected Hub<MessageTypeChangedFilter> hubFilter;

	// ObjectModels

	// object used for filter data
	protected MessageTypeChangedFilter filter;

	public MessageTypeChangedFilterModel(Hub<MessageType> hubMaster, Hub<MessageType> hub) {
		filter = new MessageTypeChangedFilter(hubMaster, hub);
	}

	public MessageTypeChangedFilterModel(Hub<MessageType> hub) {
		filter = new MessageTypeChangedFilter(hub);
	}

	// object used to input query data, to be used by filterHub
	public MessageTypeChangedFilter getFilter() {
		return filter;
	}

	// hub for filter UI object - used to bind with UI components for entering filter data
	public Hub<MessageTypeChangedFilter> getFilterHub() {
		if (hubFilter == null) {
			hubFilter = new Hub<MessageTypeChangedFilter>(MessageTypeChangedFilter.class);
			hubFilter.add(getFilter());
			hubFilter.setPos(0);
		}
		return hubFilter;
	}

	// get the Filtered hub
	public Hub<MessageType> getHub() {
		return getFilter().getHubFilter().getHub();
	}
}