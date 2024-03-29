// Generated by OABuilder
package com.messagedesigner.model.filter;

import java.util.logging.Logger;

import com.messagedesigner.model.oa.MessageTypeColumn;
import com.messagedesigner.model.oa.filter.MessageTypeColumnInvalidRpgTypeFilter;
import com.viaoa.hub.Hub;

public class MessageTypeColumnInvalidRpgTypeFilterModel {
	private static Logger LOG = Logger.getLogger(MessageTypeColumnInvalidRpgTypeFilterModel.class.getName());

	// Hubs
	protected Hub<MessageTypeColumnInvalidRpgTypeFilter> hubFilter;

	// ObjectModels

	// object used for filter data
	protected MessageTypeColumnInvalidRpgTypeFilter filter;

	public MessageTypeColumnInvalidRpgTypeFilterModel(Hub<MessageTypeColumn> hubMaster, Hub<MessageTypeColumn> hub) {
		filter = new MessageTypeColumnInvalidRpgTypeFilter(hubMaster, hub);
	}

	public MessageTypeColumnInvalidRpgTypeFilterModel(Hub<MessageTypeColumn> hub) {
		filter = new MessageTypeColumnInvalidRpgTypeFilter(hub);
	}

	// object used to input query data, to be used by filterHub
	public MessageTypeColumnInvalidRpgTypeFilter getFilter() {
		return filter;
	}

	// hub for filter UI object - used to bind with UI components for entering filter data
	public Hub<MessageTypeColumnInvalidRpgTypeFilter> getFilterHub() {
		if (hubFilter == null) {
			hubFilter = new Hub<MessageTypeColumnInvalidRpgTypeFilter>(MessageTypeColumnInvalidRpgTypeFilter.class);
			hubFilter.add(getFilter());
			hubFilter.setPos(0);
		}
		return hubFilter;
	}

	// get the Filtered hub
	public Hub<MessageTypeColumn> getHub() {
		return getFilter().getHubFilter().getHub();
	}
}
