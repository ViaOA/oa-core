package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "appUserError", pluralName = "AppUserErrors", shortName = "aue", displayName = "App User Error", useDataSource = false, isProcessed = true, displayProperty = "dateTime", noPojo = true)
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

	protected volatile int id;
	protected volatile OADateTime dateTime;
	protected volatile String message;
	protected volatile String stackTrace;
	protected volatile OADate reviewed;
	protected volatile String reviewNote;

	// Links to other objects.
	protected volatile transient AppUserLogin appUserLogin;

	public AppUserError() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	public AppUserError(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, displayLength = 5)
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

	@OAProperty(displayName = "Date TIme", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "date_time", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(OADateTime newValue) {
		OADateTime old = dateTime;
		fireBeforePropertyChange(P_DateTime, old, newValue);
		this.dateTime = newValue;
		firePropertyChange(P_DateTime, old, this.dateTime);
	}

	@OAProperty(maxLength = 250, displayLength = 35, uiColumnLength = 25)
	@OAColumn(name = "message", maxLength = 250)
	public String getMessage() {
		return message;
	}

	public void setMessage(String newValue) {
		String old = message;
		fireBeforePropertyChange(P_Message, old, newValue);
		this.message = newValue;
		firePropertyChange(P_Message, old, this.message);
	}

	@OAProperty(displayName = "Stack Trace", displayLength = 40, uiColumnLength = 25)
	@OAColumn(name = "stack_trace", sqlType = java.sql.Types.CLOB)
	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String newValue) {
		String old = stackTrace;
		fireBeforePropertyChange(P_StackTrace, old, newValue);
		this.stackTrace = newValue;
		firePropertyChange(P_StackTrace, old, this.stackTrace);
	}

	@OAProperty(displayLength = 8, isProcessed = true)
	@OAColumn(name = "reviewed", sqlType = java.sql.Types.DATE)
	public OADate getReviewed() {
		return reviewed;
	}

	public void setReviewed(OADate newValue) {
		OADate old = reviewed;
		fireBeforePropertyChange(P_Reviewed, old, newValue);
		this.reviewed = newValue;
		firePropertyChange(P_Reviewed, old, this.reviewed);
	}

	@OAProperty(displayName = "Review Note", maxLength = 254, displayLength = 40, isProcessed = true)
	@OAColumn(name = "review_note", maxLength = 254)
	public String getReviewNote() {
		return reviewNote;
	}

	public void setReviewNote(String newValue) {
		String old = reviewNote;
		fireBeforePropertyChange(P_ReviewNote, old, newValue);
		this.reviewNote = newValue;
		firePropertyChange(P_ReviewNote, old, this.reviewNote);
	}

	@OAOne(displayName = "App User Login", reverseName = AppUserLogin.P_AppUserErrors, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_AppUserLoginId, toProperty = AppUserLogin.P_Id) })
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
	@OAColumn(name = "app_user_login_id")
	public Integer getAppUserLoginId() {
		return (Integer) getFkeyProperty(P_AppUserLoginId);
	}

	public void setAppUserLoginId(Integer newValue) {
		this.appUserLogin = null;
		setFkeyProperty(P_AppUserLoginId, newValue);
	}
}
