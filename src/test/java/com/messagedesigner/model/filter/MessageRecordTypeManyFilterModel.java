// Generated by OABuilder
package com.messagedesigner.model.filter;

import java.util.logging.Logger;

import com.messagedesigner.model.oa.MessageRecord;
import com.messagedesigner.model.oa.filter.MessageRecordTypeManyFilter;
import com.viaoa.hub.Hub;

public class MessageRecordTypeManyFilterModel {
	private static Logger LOG = Logger.getLogger(MessageRecordTypeManyFilterModel.class.getName());

	// Hubs
	protected Hub<MessageRecordTypeManyFilter> hubFilter;

	// ObjectModels

	// object used for filter data
	protected MessageRecordTypeManyFilter filter;

	public MessageRecordTypeManyFilterModel(Hub<MessageRecord> hubMaster, Hub<MessageRecord> hub) {
		filter = new MessageRecordTypeManyFilter(hubMaster, hub);
	}

	public MessageRecordTypeManyFilterModel(Hub<MessageRecord> hub) {
		filter = new MessageRecordTypeManyFilter(hub);
	}

	// object used to input query data, to be used by filterHub
	public MessageRecordTypeManyFilter getFilter() {
		return filter;
	}

	// hub for filter UI object - used to bind with UI components for entering filter data
	public Hub<MessageRecordTypeManyFilter> getFilterHub() {
		if (hubFilter == null) {
			hubFilter = new Hub<MessageRecordTypeManyFilter>(MessageRecordTypeManyFilter.class);
			hubFilter.add(getFilter());
			hubFilter.setPos(0);
		}
		return hubFilter;
	}

	// get the Filtered hub
	public Hub<MessageRecord> getHub() {
		return getFilter().getHubFilter().getHub();
	}
}