package com.auto.dev.reportercorp.model.oa;

import java.awt.Color;
import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.oa.EnvironmentDelegate;
import com.auto.dev.reportercorp.model.oa.filter.EnvironmentNonProdFilter;
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
import com.viaoa.util.OAString;

@OAClass(lowerName = "environment", pluralName = "Environments", shortName = "env", displayName = "Environment", isLookup = true, isPreSelect = true, useDataSource = false, displayProperty = "name", filterClasses = {
		EnvironmentNonProdFilter.class }, noPojo = true)
public class Environment extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(Environment.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Type = "type";
	public static final String P_TypeString = "typeString";
	public static final String P_TypeEnum = "typeEnum";
	public static final String P_TypeDisplay = "typeDisplay";
	public static final String P_Name = "name";
	public static final String P_NodeCount = "nodeCount";
	public static final String P_LoadBalanceUrl = "loadBalanceUrl";
	public static final String P_TemplateUrl = "templateUrl";
	public static final String P_JdbcUrl = "jdbcUrl";
	public static final String P_Color = "color";

	public static final String P_IsProduction = "isProduction";
	public static final String P_CurrentRuntimeEnvironment = "currentRuntimeEnvironment";

	public static final String P_EnvironmentSnapshots = "environmentSnapshots";
	public static final String P_ReporterCorps = "reporterCorps";

	public static final String M_UpdateReporterCorps = "updateReporterCorps";
	public static final String M_PauseAll = "pauseAll";
	public static final String M_UnpauseAll = "unpauseAll";
	public static final String M_ClearCaches = "clearCaches";
	public static final String M_CreateSnapshot = "createSnapshot";
	public static final String M_SetDefaults = "setDefaults";
	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile int type;

	public static enum Type {
		Unknown("Unknown"),
		Local("Local"),
		Dev("Dev"),
		Test("Test"),
		Prod("Prod");

		private String display;

		Type(String display) {
			this.display = display;
		}

		public String getDisplay() {
			return display;
		}
	}

	public static final int TYPE_Unknown = 0;
	public static final int TYPE_Local = 1;
	public static final int TYPE_Dev = 2;
	public static final int TYPE_Test = 3;
	public static final int TYPE_Prod = 4;

	protected volatile String name;
	protected volatile int nodeCount;
	protected volatile String loadBalanceUrl;
	protected volatile String templateUrl;
	protected volatile String jdbcUrl;
	protected volatile Color color;

	// Links to other objects.
	protected transient Hub<EnvironmentSnapshot> hubEnvironmentSnapshots;
	protected transient Hub<ReporterCorp> hubReporterCorps;

	public Environment() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public Environment(int id) {
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

	@OAProperty(trackPrimitiveNull = false, displayLength = 6, isNameValue = true)
	@OAColumn(name = "type", sqlType = java.sql.Types.INTEGER)
	public int getType() {
		return type;
	}

	public void setType(int newValue) {
		int old = type;
		fireBeforePropertyChange(P_Type, old, newValue);
		this.type = newValue;
		firePropertyChange(P_Type, old, this.type);
	}

	@OAProperty(enumPropertyName = P_Type)
	public String getTypeString() {
		Type type = getTypeEnum();
		if (type == null) {
			return null;
		}
		return type.name();
	}

	public void setTypeString(String val) {
		int x = -1;
		if (OAString.isNotEmpty(val)) {
			Type type = Type.valueOf(val);
			if (type != null) {
				x = type.ordinal();
			}
		}
		if (x < 0) {
			x = 0;
		}
		setType(x);
	}

	@OAProperty(enumPropertyName = P_Type)
	public Type getTypeEnum() {
		final int val = getType();
		if (val < 0 || val >= Type.values().length) {
			return null;
		}
		return Type.values()[val];
	}

	public void setTypeEnum(Type val) {
		if (val == null) {
			setType(0);
		} else {
			setType(val.ordinal());
		}
	}

	@OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 6, columnLength = 6, properties = { P_Type })
	public String getTypeDisplay() {
		Type type = getTypeEnum();
		if (type == null) {
			return null;
		}
		return type.getDisplay();
	}

	@OAProperty(maxLength = 30, isUnique = true, displayLength = 20, importMatch = true)
	@OAColumn(name = "name", maxLength = 30)
	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		String old = name;
		fireBeforePropertyChange(P_Name, old, newValue);
		this.name = newValue;
		firePropertyChange(P_Name, old, this.name);
	}

	@OAProperty(displayName = "Node Count", displayLength = 6, uiColumnLength = 10, hasCustomCode = true)
	@OAColumn(name = "node_count", sqlType = java.sql.Types.INTEGER)
	public int getNodeCount() {
		return nodeCount;
	}

	public void setNodeCount(int newValue) {
		int old = nodeCount;
		fireBeforePropertyChange(P_NodeCount, old, newValue);
		this.nodeCount = newValue;
		firePropertyChange(P_NodeCount, old, this.nodeCount);

		if (isRemoteThread() || isLoading()) {
			return;
		}

		int x = getReporterCorps().size();
		if (x >= nodeCount) {
			return;
		}
		for (; x < nodeCount; x++) {
			getReporterCorps().add(new ReporterCorp());
		}
	}

	@OAProperty(displayName = "Load Balance Url", maxLength = 125, displayLength = 20, isUrl = true, noPojo = true)
	@OAColumn(name = "load_balance_url", maxLength = 125)
	public String getLoadBalanceUrl() {
		return loadBalanceUrl;
	}

	public void setLoadBalanceUrl(String newValue) {
		String old = loadBalanceUrl;
		fireBeforePropertyChange(P_LoadBalanceUrl, old, newValue);
		this.loadBalanceUrl = newValue;
		firePropertyChange(P_LoadBalanceUrl, old, this.loadBalanceUrl);
	}

	@OAProperty(displayName = "Template Url", maxLength = 50, displayLength = 20)
	@OAColumn(name = "template_url", maxLength = 50)
	public String getTemplateUrl() {
		return templateUrl;
	}

	public void setTemplateUrl(String newValue) {
		String old = templateUrl;
		fireBeforePropertyChange(P_TemplateUrl, old, newValue);
		this.templateUrl = newValue;
		firePropertyChange(P_TemplateUrl, old, this.templateUrl);
	}

	@OAProperty(displayName = "Jdbc Url", maxLength = 175, displayLength = 35, uiColumnLength = 20)
	@OAColumn(name = "jdbc_url", maxLength = 175)
	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String newValue) {
		String old = jdbcUrl;
		fireBeforePropertyChange(P_JdbcUrl, old, newValue);
		this.jdbcUrl = newValue;
		firePropertyChange(P_JdbcUrl, old, this.jdbcUrl);
	}

	@OAProperty(displayLength = 12, uiColumnLength = 8)
	@OAColumn(name = "color", maxLength = 16)
	public Color getColor() {
		return color;
	}

	public void setColor(Color newValue) {
		Color old = color;
		fireBeforePropertyChange(P_Color, old, newValue);
		this.color = newValue;
		firePropertyChange(P_Color, old, this.color);
	}

	@OACalculatedProperty(displayName = "Is Production", displayLength = 5, columnLength = 13, properties = { P_Type })
	public boolean getIsProduction() {
		boolean isProduction;
		int type = this.getType();
		type = this.getType();
		return (type == TYPE_Prod);
	}

	public boolean isIsProduction() {
		return getIsProduction();
	}

	@OACalculatedProperty(displayName = "Current Runtime Environment", displayLength = 5, columnLength = 27, properties = { P_JdbcUrl })
	public boolean getCurrentRuntimeEnvironment() {
		return EnvironmentDelegate.getCurrentRuntimeEnvironment() == this;
	}

	public boolean isCurrentRuntimeEnvironment() {
		return getCurrentRuntimeEnvironment();
	}

	@OAMany(displayName = "Environment Snapshots", toClass = EnvironmentSnapshot.class, owner = true, reverseName = EnvironmentSnapshot.P_Environment, cascadeSave = true, cascadeDelete = true)
	public Hub<EnvironmentSnapshot> getEnvironmentSnapshots() {
		if (hubEnvironmentSnapshots == null) {
			hubEnvironmentSnapshots = (Hub<EnvironmentSnapshot>) getHub(P_EnvironmentSnapshots);
		}
		return hubEnvironmentSnapshots;
	}

	@OAMany(displayName = "Reporter Corps", toClass = ReporterCorp.class, owner = true, reverseName = ReporterCorp.P_Environment, cascadeSave = true, cascadeDelete = true)
	public Hub<ReporterCorp> getReporterCorps() {
		if (hubReporterCorps == null) {
			hubReporterCorps = (Hub<ReporterCorp>) getHub(P_ReporterCorps);
		}
		return hubReporterCorps;
	}

	@OAMethod(displayName = "Update Info for all ReporterCorps")
	public void updateReporterCorps() {
		EnvironmentDelegate.updateReporterCorps(this);
	}

	@OAMethod(displayName = "Pause All")
	public void pauseAll() {
		EnvironmentDelegate.pauseAll(this);
	}

	@OAObjCallback(enabledValue = false)
	public void pauseAllCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Unpause All")
	public void unpauseAll() {
		EnvironmentDelegate.unpauseAll(this);
	}

	@OAObjCallback(enabledValue = false)
	public void unpauseAllCallback(OAObjectCallback cb) {
	}

	@OAMethod(displayName = "Clear Caches")
	public void clearCaches() {
		EnvironmentDelegate.clearCaches(this);
	}

	@OAMethod(displayName = "Create Snapshot")
	public void createSnapshot() {
		EnvironmentDelegate.createSnapshot(this);
	}

	@OAMethod(displayName = "Set Defaults")
	public void setDefaults() {
		EnvironmentDelegate.setDefaults();
		// EnvironmentDelegate.setDefaults(this);
	}

}
