package com.auto.dev.reportercorp.model.oa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAIndex;
import com.viaoa.annotation.OAIndexColumn;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATable;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "reportInstance", pluralName = "ReportInstances", shortName = "rpi", displayName = "Report Instance", displayProperty = "id", sortProperty = "id")
@OATable(name = "report_instance", indexes = {
		@OAIndex(name = "report_instance_store_number_file_name", unique = true, columns = { @OAIndexColumn(name = "store_number"),
				@OAIndexColumn(name = "file_name", lowerName = "file_nameLower") }),
		@OAIndex(name = "report_instance_file_name", columns = { @OAIndexColumn(name = "file_name", lowerName = "file_nameLower") }),
		@OAIndex(name = "report_instance_parent_composite_report_instance", fkey = true, columns = {
				@OAIndexColumn(name = "parent_composite_report_instance_id") }),
		@OAIndex(name = "report_instance_report_version", fkey = true, columns = { @OAIndexColumn(name = "report_version_id") })
})
public class ReportInstance extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReportInstance.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_StoreNumber = "storeNumber";
	public static final String P_FileName = "fileName";
	public static final String P_Title = "title";
	public static final String P_CompositePos = "compositePos";
	public static final String P_Data = "data";

	public static final String P_CalcStoreNumber = "calcStoreNumber";
	public static final String P_CalcReportName = "calcReportName";
	public static final String P_CalcPdfUrl = "calcPdfUrl";

	public static final String P_CalcReportInstanceData = "calcReportInstanceData";
	public static final String P_CompositeReportInstances = "compositeReportInstances";
	public static final String P_ParentCompositeReportInstance = "parentCompositeReportInstance";
	public static final String P_ParentCompositeReportInstanceId = "parentCompositeReportInstanceId"; // fkey
	public static final String P_PypeReportMessage = "pypeReportMessage";
	public static final String P_ReportVersion = "reportVersion";
	public static final String P_ReportVersionId = "reportVersionId"; // fkey

	protected volatile long id;
	protected volatile OADateTime created;
	protected volatile int storeNumber;
	protected volatile String fileName;
	protected volatile String title;
	protected volatile int compositePos;
	protected volatile String data;

	// Links to other objects.
	protected transient Hub<ReportInstance> hubCompositeReportInstances;
	protected volatile transient ReportInstance parentCompositeReportInstance;
	protected volatile transient PypeReportMessage pypeReportMessage;
	protected volatile transient ReportVersion reportVersion;

	public ReportInstance() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public ReportInstance(long id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
	@OAId
	@OAColumn(name = "id", sqlType = java.sql.Types.BIGINT)
	public long getId() {
		return id;
	}

	public void setId(long newValue) {
		long old = id;
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

	@OAProperty(displayName = "Store Number", displayLength = 6, uiColumnLength = 12, importMatch = true)
	@OAColumn(name = "store_number", sqlType = java.sql.Types.INTEGER)
	public int getStoreNumber() {
		return storeNumber;
	}

	public void setStoreNumber(int newValue) {
		int old = storeNumber;
		fireBeforePropertyChange(P_StoreNumber, old, newValue);
		this.storeNumber = newValue;
		firePropertyChange(P_StoreNumber, old, this.storeNumber);
	}

	@OAProperty(displayName = "File Name", maxLength = 75, displayLength = 50, uiColumnLength = 20, isFileName = true, importMatch = true)
	@OAColumn(name = "file_name", maxLength = 75, lowerName = "file_nameLower")
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String newValue) {
		String old = fileName;
		fireBeforePropertyChange(P_FileName, old, newValue);
		this.fileName = newValue;
		firePropertyChange(P_FileName, old, this.fileName);
	}

	@OAProperty(maxLength = 75, displayLength = 35, uiColumnLength = 20)
	@OAColumn(name = "title", maxLength = 75)
	public String getTitle() {
		return title;
	}

	public void setTitle(String newValue) {
		String old = title;
		fireBeforePropertyChange(P_Title, old, newValue);
		this.title = newValue;
		firePropertyChange(P_Title, old, this.title);
	}

	@OAProperty(displayName = "Composite Pos", displayLength = 6, uiColumnLength = 13, isAutoSeq = true)
	@OAColumn(name = "composite_pos", sqlType = java.sql.Types.INTEGER)
	public int getCompositePos() {
		return compositePos;
	}

	public void setCompositePos(int newValue) {
		int old = compositePos;
		fireBeforePropertyChange(P_CompositePos, old, newValue);
		this.compositePos = newValue;
		firePropertyChange(P_CompositePos, old, this.compositePos);
	}

	@OAProperty(displayLength = 30, uiColumnLength = 20, isJson = true)
	@OAColumn(name = "data", sqlType = java.sql.Types.CLOB)
	public String getData() {
		return data;
	}

	public void setData(String newValue) {
		String old = data;
		fireBeforePropertyChange(P_Data, old, newValue);
		this.data = newValue;
		firePropertyChange(P_Data, old, this.data);
	}

	@OACalculatedProperty(displayName = "Calc Store Number", displayLength = 6, columnLength = 17, properties = { P_FileName })
	public int getCalcStoreNumber() {
		int calcStoreNumber = 0;
		/* qqqqqqqqq todo:
		String fileName = this.getFileName();
		fileName = this.getFileName();
		*/
		return calcStoreNumber;
	}

	@OACalculatedProperty(displayName = "Calc Report Name", displayLength = 20, properties = { P_FileName })
	public String getCalcReportName() {
		String calcReportName = "";

		/* qqqqqqqqq todo:
		String fileName = this.getFileName();
		calcReportName = OAString.concat(calcReportName, fileName, " ");
		*/
		return calcReportName;
	}

	@OACalculatedProperty(displayName = "PDF Url", displayLength = 30, columnLength = 20, isUrl = true, properties = { P_FileName,
			P_StoreNumber })
	public String getCalcPdfUrl() {
		String s = "http://localhost:19555/ReporterCorp/pdf/renderReport?storeNumber=" + getStoreNumber() + "&fileName=" + getFileName();
		return s;
	}

	@OAOne(displayName = "Report Instance Data", isCalculated = true, reverseName = ReportInstanceData.P_CalcReportInstance, allowCreateNew = false, allowAddExisting = false)
	public ReportInstanceData getCalcReportInstanceData() {
		// Custom code here to get CalcReportInstanceData
		if (reportInstanceData == null) {
			OAJson oj = new OAJson();
			try {
				reportInstanceData = oj.readObject(getData(), ReportInstanceData.class);
			} catch (Exception e) {
			}
		}
		return reportInstanceData;
	}

	private transient ReportInstanceData reportInstanceData;

	@OAMany(displayName = "Composite Report Instances", toClass = ReportInstance.class, reverseName = ReportInstance.P_ParentCompositeReportInstance, seqProperty = ReportInstance.P_CompositePos, sortProperty = ReportInstance.P_CompositePos)
	public Hub<ReportInstance> getCompositeReportInstances() {
		if (hubCompositeReportInstances == null) {
			hubCompositeReportInstances = (Hub<ReportInstance>) getHub(P_CompositeReportInstances);
		}
		return hubCompositeReportInstances;
	}

	@OAOne(displayName = "Parent Composite Report Instance", reverseName = ReportInstance.P_CompositeReportInstances, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ParentCompositeReportInstanceId, toProperty = ReportInstance.P_Id) })
	public ReportInstance getParentCompositeReportInstance() {
		if (parentCompositeReportInstance == null) {
			parentCompositeReportInstance = (ReportInstance) getObject(P_ParentCompositeReportInstance);
		}
		return parentCompositeReportInstance;
	}

	public void setParentCompositeReportInstance(ReportInstance newValue) {
		ReportInstance old = this.parentCompositeReportInstance;
		fireBeforePropertyChange(P_ParentCompositeReportInstance, old, newValue);
		this.parentCompositeReportInstance = newValue;
		firePropertyChange(P_ParentCompositeReportInstance, old, this.parentCompositeReportInstance);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "parent_composite_report_instance_id")
	public Long getParentCompositeReportInstanceId() {
		return (Long) getFkeyProperty(P_ParentCompositeReportInstanceId);
	}

	public void setParentCompositeReportInstanceId(Long newValue) {
		this.parentCompositeReportInstance = null;
		setFkeyProperty(P_ParentCompositeReportInstanceId, newValue);
	}

	@OAOne(displayName = "Pype Report Message", reverseName = PypeReportMessage.P_ReportInstance, isProcessed = true, allowCreateNew = false, allowAddExisting = false)
	public PypeReportMessage getPypeReportMessage() {
		if (pypeReportMessage == null) {
			pypeReportMessage = (PypeReportMessage) getObject(P_PypeReportMessage);
		}
		return pypeReportMessage;
	}

	public void setPypeReportMessage(PypeReportMessage newValue) {
		PypeReportMessage old = this.pypeReportMessage;
		fireBeforePropertyChange(P_PypeReportMessage, old, newValue);
		this.pypeReportMessage = newValue;
		firePropertyChange(P_PypeReportMessage, old, this.pypeReportMessage);
	}

	@OAOne(displayName = "Report Version", reverseName = ReportVersion.P_ReportInstances, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ReportVersionId, toProperty = ReportVersion.P_Id) })
	public ReportVersion getReportVersion() {
		if (reportVersion == null) {
			reportVersion = (ReportVersion) getObject(P_ReportVersion);
		}
		return reportVersion;
	}

	public void setReportVersion(ReportVersion newValue) {
		ReportVersion old = this.reportVersion;
		fireBeforePropertyChange(P_ReportVersion, old, newValue);
		this.reportVersion = newValue;
		firePropertyChange(P_ReportVersion, old, this.reportVersion);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "report_version_id")
	public Integer getReportVersionId() {
		return (Integer) getFkeyProperty(P_ReportVersionId);
	}

	public void setReportVersionId(Integer newValue) {
		this.reportVersion = null;
		setFkeyProperty(P_ReportVersionId, newValue);
	}

	public void load(ResultSet rs, long id) throws SQLException {
		this.id = id;
		java.sql.Timestamp timestamp;
		timestamp = rs.getTimestamp(2);
		if (timestamp != null) {
			this.created = new OADateTime(timestamp);
		}
		this.storeNumber = rs.getInt(3);
		OAObjectInfoDelegate.setPrimitiveNull(this, P_StoreNumber, rs.wasNull());
		this.fileName = rs.getString(4);
		this.title = rs.getString(5);
		this.compositePos = rs.getInt(6);
		OAObjectInfoDelegate.setPrimitiveNull(this, P_CompositePos, rs.wasNull());
		this.data = rs.getString(7);
		long parentCompositeReportInstanceFkey = rs.getLong(8);
		setFkeyProperty(P_ParentCompositeReportInstance, rs.wasNull() ? null : parentCompositeReportInstanceFkey);
		int reportVersionFkey = rs.getInt(9);
		setFkeyProperty(P_ReportVersion, rs.wasNull() ? null : reportVersionFkey);

		this.changedFlag = false;
		this.newFlag = false;
	}
}
