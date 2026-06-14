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
import com.viaoa.datetime.OADate;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "appUserError",
    pluralName = "AppUserErrors",
    shortName = "aue",
    displayName = "App User Error",
    isProcessed = true,
    displayProperty = "dateTime",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "AppUserErrorAppUserLogin", fkey = true, columns = { @OAIndexColumn(name = "AppUserLoginId") })
    }
)
public class AppUserError extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(AppUserError.class.getName());

    public static final String P_Id = "id";
    public static final String P_DateTime = "dateTime";
    public static final String P_Message = "message";
    public static final String P_StackTrace = "stackTrace";
    public static final String P_Reviewed = "reviewed";
    public static final String P_ReviewNote = "reviewNote";
     
    public static final String P_AppUserLogin = "appUserLogin";
    public static final String P_AppUserLoginId = "appUserLoginId"; // fkey
    public static final String P_Reports = "reports";
    public static final String P_ReportsId = "reportsId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime dateTime;
    protected volatile String message;
    protected volatile String stackTrace;
    protected volatile OADate reviewed;
    protected volatile String reviewNote;
     
    // Links to other objects.
    protected volatile transient AppUserLogin appUserLogin;
    protected transient Hub<Report> hubReports;
     
    public AppUserError() {
        if (!isLoading()) setObjectDefaults();
    }
     
    public AppUserError(int id) {
        this();
        setId(id);
    }

    @OAProperty(lowerName = "id", isUnique = true, displayLength = 5)
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

    @OAProperty(lowerName = "dateTime", displayName = "Date TIme", displayLength = 15, isProcessed = true)
    @OAColumn(name = "DateTimeValue", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getDateTime() {
        return dateTime;
    }
    public void setDateTime(OADateTime newValue) {
        OADateTime old = dateTime;
        fireBeforePropertyChange(P_DateTime, old, newValue);
        this.dateTime = newValue;
        firePropertyChange(P_DateTime, old, this.dateTime);
    }

    @OAProperty(lowerName = "message", maxLength = 250, displayLength = 35, uiColumnLength = 25)
    @OAColumn(name = "Message", maxLength = 250)
    public String getMessage() {
        return message;
    }
    public void setMessage(String newValue) {
        String old = message;
        fireBeforePropertyChange(P_Message, old, newValue);
        this.message = newValue;
        firePropertyChange(P_Message, old, this.message);
    }

    @OAProperty(lowerName = "stackTrace", displayName = "Stack Trace", displayLength = 40, uiColumnLength = 25)
    @OAColumn(name = "StackTrace", sqlType = java.sql.Types.CLOB)
    public String getStackTrace() {
        return stackTrace;
    }
    public void setStackTrace(String newValue) {
        String old = stackTrace;
        fireBeforePropertyChange(P_StackTrace, old, newValue);
        this.stackTrace = newValue;
        firePropertyChange(P_StackTrace, old, this.stackTrace);
    }

    @OAProperty(lowerName = "reviewed", displayLength = 8, isProcessed = true)
    @OAColumn(name = "Reviewed", sqlType = java.sql.Types.DATE)
    public OADate getReviewed() {
        return reviewed;
    }
    public void setReviewed(OADate newValue) {
        OADate old = reviewed;
        fireBeforePropertyChange(P_Reviewed, old, newValue);
        this.reviewed = newValue;
        firePropertyChange(P_Reviewed, old, this.reviewed);
    }

    @OAProperty(lowerName = "reviewNote", displayName = "Review Note", maxLength = 254, displayLength = 40, isProcessed = true)
    @OAColumn(name = "ReviewNote", maxLength = 254)
    public String getReviewNote() {
        return reviewNote;
    }
    public void setReviewNote(String newValue) {
        String old = reviewNote;
        fireBeforePropertyChange(P_ReviewNote, old, newValue);
        this.reviewNote = newValue;
        firePropertyChange(P_ReviewNote, old, this.reviewNote);
    }

    @OAOne(
        displayName = "App User Login", 
        reverseName = AppUserLogin.P_AppUserErrors, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_AppUserLoginId, toProperty = AppUserLogin.P_Id)}
    )
    public AppUserLogin getAppUserLogin() {
        if (appUserLogin == null) {
            appUserLogin = (AppUserLogin) getObject(P_AppUserLogin);
        }
        return appUserLogin;
    }
    public void setAppUserLogin(AppUserLogin newValue) {
        AppUserLogin old = this.appUserLogin;
        fireBeforePropertyChange(P_AppUserLogin, old, newValue);
        this.appUserLogin = newValue;
        firePropertyChange(P_AppUserLogin, old, this.appUserLogin);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "AppUserLoginId")
    public Integer getAppUserLoginId() {
        return (Integer) getFkeyProperty(P_AppUserLoginId);
    }
    public void setAppUserLoginId(Integer newValue) {
        this.appUserLogin = null;
        setFkeyProperty(P_AppUserLoginId, newValue);
    }

    @OAMany(
        toClass = Report.class, 
        reverseName = Report.P_AppUserError
    )
    @OALinkTable(name = "AppUserErrorReport", indexName = "ReportAppUserError", columns = {"AppUserErrorId"})
    public Hub<Report> getReports() {
        if (hubReports == null) {
            hubReports = (Hub<Report>) getHub(P_Reports);
        }
        return hubReports;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.dateTime = new OADateTime(timestamp);
        this.message = rs.getString(3);
        this.stackTrace = rs.getString(4);
        java.sql.Date date;
        date = rs.getDate(5);
        if (date != null) this.reviewed = new OADate(date);
        this.reviewNote = rs.getString(6);
        int appUserLoginFkey = rs.getInt(7);
        setFkeyProperty(P_AppUserLogin, rs.wasNull() ? null : appUserLoginFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
