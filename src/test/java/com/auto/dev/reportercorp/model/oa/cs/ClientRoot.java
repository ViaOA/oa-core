// Copied from OATemplate project by OABuilder 12/09/22 04:06 PM
// Copied from OATemplate project by OABuilder 12/09/22 03:58 PM
// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.auto.dev.reportercorp.model.oa.cs;

import com.auto.dev.reportercorp.model.oa.PypeReportMessage;
import com.auto.dev.reportercorp.model.oa.ReportInstance;
import com.auto.dev.reportercorp.model.oa.ReportTemplate;
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
	public static final String P_Id = "Id";
	public static final String PROPERTY_ConnectionInfo = "ConnectionInfo";
	public static final String P_ConnectionInfo = "ConnectionInfo";
	/*$$Start: ClientRoot1 $$*/
	// Hubs for Client UI
	public static final String P_SearchReportTemplates = "SearchReportTemplates";
	public static final String P_SearchReportInstances = "SearchReportInstances";
	public static final String P_SearchPypeReportMessages = "SearchPypeReportMessages";
	/*$$End: ClientRoot1 $$*/

	protected int id;

	// Hub
	/*$$Start: ClientRoot2 $$*/
	// Hubs for Client UI
	protected transient Hub<ReportTemplate> hubSearchReportTemplates;
	protected transient Hub<ReportInstance> hubSearchReportInstances;
	protected transient Hub<PypeReportMessage> hubSearchPypeReportMessages;
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
	@OAMany(toClass = ReportTemplate.class, cascadeSave = true)
	public Hub<ReportTemplate> getSearchReportTemplates() {
		if (hubSearchReportTemplates == null) {
			hubSearchReportTemplates = (Hub<ReportTemplate>) super.getHub(P_SearchReportTemplates);
		}
		return hubSearchReportTemplates;
	}

	@OAMany(toClass = ReportInstance.class, cascadeSave = true)
	public Hub<ReportInstance> getSearchReportInstances() {
		if (hubSearchReportInstances == null) {
			hubSearchReportInstances = (Hub<ReportInstance>) super.getHub(P_SearchReportInstances);
		}
		return hubSearchReportInstances;
	}

	@OAMany(toClass = PypeReportMessage.class, cascadeSave = true, isProcessed = true)
	public Hub<PypeReportMessage> getSearchPypeReportMessages() {
		if (hubSearchPypeReportMessages == null) {
			hubSearchPypeReportMessages = (Hub<PypeReportMessage>) super.getHub(P_SearchPypeReportMessages);
		}
		return hubSearchPypeReportMessages;
	}
	/*$$End: ClientRoot3 $$*/

}
