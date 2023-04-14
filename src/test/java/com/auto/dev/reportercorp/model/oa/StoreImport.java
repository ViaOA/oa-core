package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.oa.StoreImportDelegate;
import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAMethod;
import com.viaoa.annotation.OAObjCallback;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "storeImport", pluralName = "StoreImports", shortName = "sti", displayName = "Store Import", useDataSource = false, displayProperty = "id", singleton = true, pojoSingleton = true, noPojo = true)
public class StoreImport extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(StoreImport.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_ReporterCorpServerBaseUrl = "reporterCorpServerBaseUrl";
	public static final String P_Console = "console";

	public static final String P_HasCorpReportsToUpdate = "hasCorpReportsToUpdate";
	public static final String P_HasCorpTemplatesToUpdate = "hasCorpTemplatesToUpdate";

	public static final String P_StoreImportReports = "storeImportReports";
	public static final String P_StoreImportTemplates = "storeImportTemplates";

	public static final String M_LoadReportsFromStore = "loadReportsFromStore";
	public static final String M_LoadTemplatesFromStore = "loadTemplatesFromStore";
	public static final String M_UpdateCorpReports = "updateCorpReports";
	public static final String M_UpdateCorpTemplates = "updateCorpTemplates";
	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String reporterCorpServerBaseUrl;
	protected volatile String console;

	// Links to other objects.
	protected transient Hub<StoreImportReport> hubStoreImportReports;
	protected transient Hub<StoreImportTemplate> hubStoreImportTemplates;

	public StoreImport() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public StoreImport(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
	@OAId
	@OAColumn(name = "id", sqlType = java.sql.Types.INTEGER)
	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		int old = id;
		fireBeforePropertyChange(P_Id, old, newValue);
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
	}

	@OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "created", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
	}

	@OAProperty(displayName = "Reporter Corp Server Base Url", maxLength = 120, displayLength = 25, uiColumnLength = 29)
	@OAColumn(name = "reporter_corp_server_base_url", maxLength = 120)
	public String getReporterCorpServerBaseUrl() {
		return reporterCorpServerBaseUrl;
	}

	public void setReporterCorpServerBaseUrl(String newValue) {
		String old = reporterCorpServerBaseUrl;
		fireBeforePropertyChange(P_ReporterCorpServerBaseUrl, old, newValue);
		this.reporterCorpServerBaseUrl = newValue;
		firePropertyChange(P_ReporterCorpServerBaseUrl, old, this.reporterCorpServerBaseUrl);
	}

	@OAProperty(maxLength = 20, displayLength = 20)
	public String getConsole() {
		return console;
	}

	public void setConsole(String newValue) {
		String old = console;
		fireBeforePropertyChange(P_Console, old, newValue);
		this.console = newValue;
		firePropertyChange(P_Console, old, this.console);
	}

	@OACalculatedProperty(displayName = "Has Corp Reports To Update", displayLength = 5, columnLength = 26, properties = {
			P_StoreImportReports })
	public boolean getHasCorpReportsToUpdate() {
		Hub<StoreImportReport> hub = this.getStoreImportReports();
		return hub.size() > 0;
	}

	public boolean isHasCorpReportsToUpdate() {
		return getHasCorpReportsToUpdate();
	}

	@OACalculatedProperty(displayName = "Has Corp Templates To Update", displayLength = 5, columnLength = 28, properties = {
			P_StoreImportTemplates })
	public boolean getHasCorpTemplatesToUpdate() {
		Hub<StoreImportTemplate> hub = this.getStoreImportTemplates();
		return hub.size() > 0;
	}

	public boolean isHasCorpTemplatesToUpdate() {
		return getHasCorpTemplatesToUpdate();
	}

	@OAMany(displayName = "Store Import Reports", toClass = StoreImportReport.class, owner = true, reverseName = StoreImportReport.P_StoreImport, isProcessed = true, cascadeSave = true, cascadeDelete = true)
	public Hub<StoreImportReport> getStoreImportReports() {
		if (hubStoreImportReports == null) {
			hubStoreImportReports = (Hub<StoreImportReport>) getHub(P_StoreImportReports);
		}
		return hubStoreImportReports;
	}

	@OAMany(displayName = "Store Import Templates", toClass = StoreImportTemplate.class, owner = true, reverseName = StoreImportTemplate.P_StoreImport, isProcessed = true, cascadeSave = true, cascadeDelete = true)
	public Hub<StoreImportTemplate> getStoreImportTemplates() {
		if (hubStoreImportTemplates == null) {
			hubStoreImportTemplates = (Hub<StoreImportTemplate>) getHub(P_StoreImportTemplates);
		}
		return hubStoreImportTemplates;
	}

	@OAMethod(displayName = "Load Reports From Store")
	public void loadReportsFromStore() {
		StoreImportDelegate.importReportsFromStore(this);
	}

	@OAMethod(displayName = "Load Templates From Store")
	public void loadTemplatesFromStore() {
		StoreImportDelegate.importTemplatesFromStore(this);
	}

	@OAMethod(displayName = "Update Corp Reports")
	public void updateCorpReports() {
		StoreImportDelegate.updateCorpReports(this);
	}

	@OAObjCallback(enabledProperty = StoreImport.P_HasCorpReportsToUpdate)
	public void updateCorpReportsCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Update Corp Templates")
	public void updateCorpTemplates() {
		StoreImportDelegate.updateCorpTemplates(this);
	}

	@OAObjCallback(enabledProperty = StoreImport.P_HasCorpTemplatesToUpdate)
	public void updateCorpTemplatesCallback(OAObjectCallback cb) {
	}

}
