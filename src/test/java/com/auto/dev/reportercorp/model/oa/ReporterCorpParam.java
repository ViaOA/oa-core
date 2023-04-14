package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "reporterCorpParam", pluralName = "ReporterCorpParams", shortName = "rcp", displayName = "Reporter Corp Param", useDataSource = false, displayProperty = "id")
public class ReporterCorpParam extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReporterCorpParam.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Type = "type";
	public static final String P_TypeString = "typeString";
	public static final String P_TypeEnum = "typeEnum";
	public static final String P_TypeDisplay = "typeDisplay";
	public static final String P_Name = "name";
	public static final String P_Value = "value";

	public static final String P_ReporterCorp = "reporterCorp";
	public static final String P_ReporterCorpId = "reporterCorpId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile int type;

	public static enum Type {
		unknown("Unknown");

		private String display;

		Type(String display) {
			this.display = display;
		}

		public String getDisplay() {
			return display;
		}
	}

	public static final int TYPE_unknown = 0;

	protected volatile String name;
	protected volatile String value;

	// Links to other objects.
	protected volatile transient ReporterCorp reporterCorp;

	public ReporterCorpParam() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public ReporterCorpParam(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6, noPojo = true)
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

	@OAProperty(displayLength = 6, isNameValue = true)
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
			setNull(P_Type);
		} else {
			setType(x);
		}
	}

	@OAProperty(enumPropertyName = P_Type)
	public Type getTypeEnum() {
		if (isNull(P_Type)) {
			return null;
		}
		final int val = getType();
		if (val < 0 || val >= Type.values().length) {
			return null;
		}
		return Type.values()[val];
	}

	public void setTypeEnum(Type val) {
		if (val == null) {
			setNull(P_Type);
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

	@OAProperty(maxLength = 75, displayLength = 25, uiColumnLength = 20)
	@OAColumn(name = "name", maxLength = 75)
	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		String old = name;
		fireBeforePropertyChange(P_Name, old, newValue);
		this.name = newValue;
		firePropertyChange(P_Name, old, this.name);
	}

	@OAProperty(maxLength = 250, displayLength = 15, uiColumnLength = 22)
	@OAColumn(name = "value", maxLength = 250)
	public String getValue() {
		return value;
	}

	public void setValue(String newValue) {
		String old = value;
		fireBeforePropertyChange(P_Value, old, newValue);
		this.value = newValue;
		firePropertyChange(P_Value, old, this.value);
	}

	@OAOne(displayName = "Reporter Corp", reverseName = ReporterCorp.P_ReporterCorpParams, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ReporterCorpId, toProperty = ReporterCorp.P_Id) })
	public ReporterCorp getReporterCorp() {
		if (reporterCorp == null) {
			reporterCorp = (ReporterCorp) getObject(P_ReporterCorp);
		}
		return reporterCorp;
	}

	public void setReporterCorp(ReporterCorp newValue) {
		ReporterCorp old = this.reporterCorp;
		fireBeforePropertyChange(P_ReporterCorp, old, newValue);
		this.reporterCorp = newValue;
		firePropertyChange(P_ReporterCorp, old, this.reporterCorp);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "reporter_corp_id")
	public Integer getReporterCorpId() {
		return (Integer) getFkeyProperty(P_ReporterCorpId);
	}

	public void setReporterCorpId(Integer newValue) {
		this.reporterCorp = null;
		setFkeyProperty(P_ReporterCorpId, newValue);
	}
}
