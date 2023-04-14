// Copied from OATemplate project by OABuilder 12/09/22 04:06 PM
// Copied from OATemplate project by OABuilder 12/09/22 03:58 PM
// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.auto.dev.reportercorp.model.oa.cs;

import com.auto.dev.reportercorp.model.oa.AppServer;
import com.auto.dev.reportercorp.model.oa.AppUser;
import com.auto.dev.reportercorp.model.oa.AppUserError;
import com.auto.dev.reportercorp.model.oa.AppUserLogin;
import com.auto.dev.reportercorp.model.oa.Environment;
import com.auto.dev.reportercorp.model.oa.ProcessStep;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportTemplate;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.auto.dev.reportercorp.model.oa.StoreImport;
import com.auto.dev.reportercorp.model.oa.propertypath.AppUserPP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubMerger;
import com.viaoa.object.OAObject;

/**
 * Root Object that is automatically updated between the Server and Clients. ServerController will do the selects for these objects. Model
 * will share these hubs after the application is started.
 */

@OAClass(useDataSource = false, displayProperty = "Id")
public class ServerRoot extends OAObject {
	private static final long serialVersionUID = 1L;

	public static final String PROPERTY_Id = "Id";
	public static final String P_Id = "Id";

	/*$$Start: ServerRoot1 $$*/
	// lookups, preselects
	public static final String P_AppServers = "AppServers";
	public static final String P_AppUsers = "AppUsers";
	public static final String P_Environments = "Environments";
	public static final String P_ProcessSteps = "ProcessSteps";
	public static final String P_Reports = "Reports";
	public static final String P_ReporterCorps = "ReporterCorps";
	// autoCreateOne
	public static final String P_CreateOneAppServerHub = "CreateOneAppServerHub";
	public static final String P_CreateOneStoreImportHub = "CreateOneStoreImportHub";
	// filters
	// UI containers
	public static final String P_MasterOnlyReports = "MasterOnlyReports";
	public static final String P_NeedsTemplateReportTemplates = "NeedsTemplateReportTemplates";
	public static final String P_AppUserLogins = "AppUserLogins";
	public static final String P_AppUserErrors = "AppUserErrors";
	/*$$End: ServerRoot1 $$*/

	protected int id;
	/*$$Start: ServerRoot2 $$*/
	// lookups, preselects
	protected transient Hub<AppServer> hubAppServers;
	protected transient Hub<AppUser> hubAppUsers;
	protected transient Hub<Environment> hubEnvironments;
	protected transient Hub<ProcessStep> hubProcessSteps;
	protected transient Hub<Report> hubReports;
	protected transient Hub<ReporterCorp> hubReporterCorps;
	// autoCreateOne
	protected transient Hub<AppServer> hubCreateOneAppServer;
	protected transient Hub<StoreImport> hubCreateOneStoreImport;
	// filters
	// UI containers
	protected transient Hub<Report> hubReports1;
	protected transient Hub<ReportTemplate> hubNeedsTemplateReportTemplates;
	protected transient Hub<AppUserLogin> hubAppUserLogins;
	protected transient Hub<AppUserError> hubAppUserErrors;
	/*$$End: ServerRoot2 $$*/

	public ServerRoot() {
		setId(777);
	}

	@OAProperty(displayName = "Id")
	@OAId
	public int getId() {
		return id;
	}

	public void setId(int id) {
		int old = this.id;
		this.id = id;
		firePropertyChange(PROPERTY_Id, old, id);
	}

	/*$$Start: ServerRoot3 $$*/
	// lookups, preselects
	@OAMany(toClass = AppServer.class, cascadeSave = true)
	public Hub<AppServer> getAppServers() {
		if (hubAppServers == null) {
			hubAppServers = (Hub<AppServer>) super.getHub(P_AppServers);
		}
		return hubAppServers;
	}

	@OAMany(toClass = AppUser.class, cascadeSave = true)
	public Hub<AppUser> getAppUsers() {
		if (hubAppUsers == null) {
			hubAppUsers = (Hub<AppUser>) super.getHub(P_AppUsers);
		}
		return hubAppUsers;
	}

	@OAMany(toClass = Environment.class, cascadeSave = true)
	public Hub<Environment> getEnvironments() {
		if (hubEnvironments == null) {
			hubEnvironments = (Hub<Environment>) super.getHub(P_Environments);
		}
		return hubEnvironments;
	}

	@OAMany(toClass = ProcessStep.class, sortProperty = ProcessStep.P_Step, cascadeSave = true)
	public Hub<ProcessStep> getProcessSteps() {
		if (hubProcessSteps == null) {
			hubProcessSteps = (Hub<ProcessStep>) super.getHub(P_ProcessSteps);
		}
		return hubProcessSteps;
	}

	@OAMany(toClass = Report.class, sortProperty = Report.P_FileName, cascadeSave = true)
	public Hub<Report> getReports() {
		if (hubReports == null) {
			hubReports = (Hub<Report>) super.getHub(P_Reports);
		}
		return hubReports;
	}

	@OAMany(toClass = ReporterCorp.class, cascadeSave = true)
	public Hub<ReporterCorp> getReporterCorps() {
		if (hubReporterCorps == null) {
			hubReporterCorps = (Hub<ReporterCorp>) super.getHub(P_ReporterCorps);
		}
		return hubReporterCorps;
	}

	// autoCreatedOne
	@OAMany(toClass = AppServer.class, cascadeSave = true)
	public Hub<AppServer> getCreateOneAppServerHub() {
		if (hubCreateOneAppServer == null) {
			hubCreateOneAppServer = (Hub<AppServer>) super.getHub(P_CreateOneAppServerHub);
		}
		return hubCreateOneAppServer;
	}

	@OAMany(toClass = StoreImport.class, cascadeSave = true)
	public Hub<StoreImport> getCreateOneStoreImportHub() {
		if (hubCreateOneStoreImport == null) {
			hubCreateOneStoreImport = (Hub<StoreImport>) super.getHub(P_CreateOneStoreImportHub);
		}
		return hubCreateOneStoreImport;
	}

	// filters
	// UI containers
	@OAMany(toClass = Report.class, sortProperty = Report.P_FileName, cascadeSave = true)
	public Hub<Report> getMasterOnlyReports() {
		if (hubReports1 == null) {
			hubReports1 = (Hub<Report>) super.getHub(P_MasterOnlyReports);
		}
		return hubReports1;
	}

	@OAMany(toClass = ReportTemplate.class, cascadeSave = true)
	public Hub<ReportTemplate> getNeedsTemplateReportTemplates() {
		if (hubNeedsTemplateReportTemplates == null) {
			hubNeedsTemplateReportTemplates = (Hub<ReportTemplate>) super.getHub(P_NeedsTemplateReportTemplates);
		}
		return hubNeedsTemplateReportTemplates;
	}

	@OAMany(toClass = AppUserLogin.class, isCalculated = true, cascadeSave = true)
	public Hub<AppUserLogin> getAppUserLogins() {
		if (hubAppUserLogins == null) {
			hubAppUserLogins = (Hub<AppUserLogin>) super.getHub(P_AppUserLogins);
			String pp = AppUserPP.appUserLogins().lastDayFilter().pp;
			HubMerger hm = new HubMerger(this.getAppUsers(), hubAppUserLogins, pp, false, true);
		}
		return hubAppUserLogins;
	}

	@OAMany(toClass = AppUserError.class, isCalculated = true, cascadeSave = true)
	public Hub<AppUserError> getAppUserErrors() {
		if (hubAppUserErrors == null) {
			hubAppUserErrors = (Hub<AppUserError>) super.getHub(P_AppUserErrors);
			String pp = AppUserPP.appUserLogins().appUserErrors().pp;
			HubMerger hm = new HubMerger(this.getAppUsers(), hubAppUserErrors, pp, false, true);
		}
		return hubAppUserErrors;
	}
	/*$$End: ServerRoot3 $$*/
}
