package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.lang.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "report",
    pluralName = "Reports",
    shortName = "rpr",
    displayName = "Report",
    displayProperty = "reportDef",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ReportReportDef", fkey = true, columns = { @OAIndexColumn(name = "ReportDefId") }), 
    }
)
public class Report extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Report.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Generated = "generated";
    public static final String P_Html = "html";
     
    public static final String P_AppServer = "appServer";
    public static final String P_AppServerId = "appServerId"; // fkey
    public static final String P_AppUser = "appUser";
    public static final String P_AppUserId = "appUserId"; // fkey
    public static final String P_AppUserError = "appUserError";
    public static final String P_AppUserErrorId = "appUserErrorId"; // fkey
    public static final String P_AppUserLogin = "appUserLogin";
    public static final String P_AppUserLoginId = "appUserLoginId"; // fkey
    public static final String P_CalcReportClass = "calcReportClass";
    public static final String P_ReportDef = "reportDef";
    public static final String P_ReportDefId = "reportDefId"; // fkey
     
    public static final String M_Generate = "generate";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile OADateTime generated;
    protected volatile String html;
     
    // Links to other objects.
    protected volatile transient ReportDef reportDef;
     
    public Report() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Report(int id) {
        this();
        setId(id);
    }

    @OAProperty(lowerName = "id", isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId
    @OAColumn(name = "Id", sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }

    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "Created", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }

    @OAProperty(lowerName = "generated", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "Generated", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getGenerated() {
        return generated;
    }
    public void setGenerated(OADateTime newValue) {
        OADateTime old = generated;
        fireBeforePropertyChange(P_Generated, old, newValue);
        this.generated = newValue;
        firePropertyChange(P_Generated, old, this.generated);
    }

    @OAProperty(lowerName = "html", displayLength = 30, uiColumnLength = 20, isProcessed = true)
    @OAColumn(name = "Html", sqlType = java.sql.Types.CLOB)
    public String getHtml() {
        return html;
    }
    public void setHtml(String newValue) {
        String old = html;
        fireBeforePropertyChange(P_Html, old, newValue);
        this.html = newValue;
        firePropertyChange(P_Html, old, this.html);
    }

    @OAOne(
        displayName = "App Server", 
        reverseName = AppServer.P_Reports, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        isOneAndOnlyOne = true, 
        fkeys = {@OAFkey(fromProperty = P_AppServerId, toProperty = AppServer.P_Id)}
    )
    @OALinkTable(name = "AppServerReport", indexName = "AppServerReport", columns = {"ReportId"})
    private AppServer getAppServer() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        displayName = "App User", 
        reverseName = AppUser.P_Reports, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        isOneAndOnlyOne = true, 
        fkeys = {@OAFkey(fromProperty = P_AppUserId, toProperty = AppUser.P_Id)}
    )
    @OALinkTable(name = "AppUserReport", indexName = "AppUserReport", columns = {"ReportId"})
    private AppUser getAppUser() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        displayName = "App User Error", 
        reverseName = AppUserError.P_Reports, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        isOneAndOnlyOne = true, 
        fkeys = {@OAFkey(fromProperty = P_AppUserErrorId, toProperty = AppUserError.P_Id)}
    )
    @OALinkTable(name = "AppUserErrorReport", indexName = "AppUserErrorReport", columns = {"ReportId"})
    private AppUserError getAppUserError() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        displayName = "App User Login", 
        reverseName = AppUserLogin.P_Reports, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        isOneAndOnlyOne = true, 
        fkeys = {@OAFkey(fromProperty = P_AppUserLoginId, toProperty = AppUserLogin.P_Id)}
    )
    @OALinkTable(name = "AppUserLoginReport", indexName = "AppUserLoginReport", columns = {"ReportId"})
    private AppUserLogin getAppUserLogin() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        displayName = "Report Class", 
        isCalculated = true, 
        reverseName = ReportClass.P_CalcReports, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    public ReportClass getCalcReportClass() {
        return ReportDelegate.getCalcReportClass(this);
    }

    @OAOne(
        displayName = "Report Def", 
        reverseName = ReportDef.P_Reports, 
        allowCreateNew = false, 
        selectFromPath = P_CalcReportClass + "." + ReportClass.P_ReportDefs, 
        fkeys = {@OAFkey(fromProperty = P_ReportDefId, toProperty = ReportDef.P_Id)}
    )
    public ReportDef getReportDef() {
        if (reportDef == null) {
            reportDef = (ReportDef) getObject(P_ReportDef);
        }
        return reportDef;
    }
    public void setReportDef(ReportDef newValue) {
        ReportDef old = this.reportDef;
        fireBeforePropertyChange(P_ReportDef, old, newValue);
        this.reportDef = newValue;
        firePropertyChange(P_ReportDef, old, this.reportDef);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ReportDefId")
    public Integer getReportDefId() {
        return (Integer) getFkeyProperty(P_ReportDefId);
    }
    public void setReportDefId(Integer newValue) {
        this.reportDef = null;
        setFkeyProperty(P_ReportDefId, newValue);
    }
    @OAMethod(displayName = "Generate")
    public void generate() throws Exception {
        // custom code
        ReportDelegate.generate(this);
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(3);
        if (timestamp != null) this.generated = new OADateTime(timestamp);
        this.html = rs.getString(4);
        int reportDefFkey = rs.getInt(5);
        setFkeyProperty(P_ReportDef, rs.wasNull() ? null : reportDefFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
