// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.messagedesigner.model.oa.cs;

import com.messagedesigner.model.oa.Message;
import com.messagedesigner.model.oa.MessageType;
import com.messagedesigner.model.oa.MessageTypeColumn;
import com.messagedesigner.model.oa.MessageTypeRecord;
import com.messagedesigner.model.oa.RpgMessage;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Root Object that is automatically updated between the Server and Clients. ServerController will do the selects for these objects. Model
 * will share these hubs after the application is started.
 */
@OAClass(useDataSource = false, displayProperty = "Id")
public class ClientRoot extends OAObject {
	private static final long serialVersionUID = 1L;

	public static final String PROPERTY_Id = "Id";
	public static final String PROPERTY_ConnectionInfo = "ConnectionInfo";
	/*$$Start: ClientRoot1 $$*/
	// Hubs for Client UI
	public static final String P_JsonSearchMessages = "JsonSearchMessages";
	public static final String P_SearchRpgMessages = "SearchRpgMessages";
	public static final String P_SearchMessageTypes = "SearchMessageTypes";
	public static final String P_SearchMessageTypeRecords = "SearchMessageTypeRecords";
	public static final String P_SearchMessageTypeColumns = "SearchMessageTypeColumns";
	/*$$End: ClientRoot1 $$*/

	protected int id;

	// Hub
	/*$$Start: ClientRoot2 $$*/
	// Hubs for Client UI
	protected transient Hub<Message> hubJsonSearchMessages;
	protected transient Hub<RpgMessage> hubSearchRpgMessages;
	protected transient Hub<MessageType> hubSearchMessageTypes;
	protected transient Hub<MessageTypeRecord> hubSearchMessageTypeRecords;
	protected transient Hub<MessageTypeColumn> hubSearchMessageTypeColumns;
	/*$$End: ClientRoot2 $$*/

	@OAProperty(displayName = "Id")
	@OAId
	public int getId() {
		return id;
	}

	public void setId(int id) {
		int old = this.id;
		this.id = id;
		firePropertyChange("id", old, id);
	}

	/*$$Start: ClientRoot3 $$*/
	// Hubs for Client UI
	@OAMany(toClass = Message.class, cascadeSave = true, isProcessed = true)
	public Hub<Message> getJsonSearchMessages() {
		if (hubJsonSearchMessages == null) {
			hubJsonSearchMessages = (Hub<Message>) super.getHub(P_JsonSearchMessages);
		}
		return hubJsonSearchMessages;
	}

	@OAMany(toClass = RpgMessage.class, cascadeSave = true, isProcessed = true)
	public Hub<RpgMessage> getSearchRpgMessages() {
		if (hubSearchRpgMessages == null) {
			hubSearchRpgMessages = (Hub<RpgMessage>) super.getHub(P_SearchRpgMessages);
		}
		return hubSearchRpgMessages;
	}

	@OAMany(toClass = MessageType.class, cascadeSave = true)
	public Hub<MessageType> getSearchMessageTypes() {
		if (hubSearchMessageTypes == null) {
			hubSearchMessageTypes = (Hub<MessageType>) super.getHub(P_SearchMessageTypes);
		}
		return hubSearchMessageTypes;
	}

	@OAMany(toClass = MessageTypeRecord.class, cascadeSave = true)
	public Hub<MessageTypeRecord> getSearchMessageTypeRecords() {
		if (hubSearchMessageTypeRecords == null) {
			hubSearchMessageTypeRecords = (Hub<MessageTypeRecord>) super.getHub(P_SearchMessageTypeRecords);
		}
		return hubSearchMessageTypeRecords;
	}

	@OAMany(toClass = MessageTypeColumn.class, cascadeSave = true)
	public Hub<MessageTypeColumn> getSearchMessageTypeColumns() {
		if (hubSearchMessageTypeColumns == null) {
			hubSearchMessageTypeColumns = (Hub<MessageTypeColumn>) super.getHub(P_SearchMessageTypeColumns);
		}
		return hubSearchMessageTypeColumns;
	}
	/*$$End: ClientRoot3 $$*/

}
