package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "appServer", pluralName = "AppServers", shortName = "as", displayName = "App Server", isLookup = true, isPreSelect = true, useDataSource = false, isProcessed = true, displayProperty = "appUserLogin.hostName", singleton = true, pojoSingleton = true, noPojo = true)
public class AppServer extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(AppServer.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Started = "started";
	public static final String P_DemoMode = "demoMode";
	public static final String P_Release = "release";

	public static final String P_AppUserLogin = "appUserLogin";
	public static final String P_AppUserLoginId = "appUserLoginId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile OADateTime started;
	protected volatile boolean demoMode;
	protected volatile String release;

	// Links to other objects.
	protected volatile transient AppUserLogin appUserLogin;

	public AppServer() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public AppServer(int id) {
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

	@OAProperty(displayLength = 15, isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "started", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getStarted() {
		return started;
	}

	public void setStarted(OADateTime newValue) {
		OADateTime old = started;
		fireBeforePropertyChange(P_Started, old, newValue);
		this.started = newValue;
		firePropertyChange(P_Started, old, this.started);
	}

	@OAProperty(displayName = "Demo Mode", displayLength = 5, isProcessed = true)
	@OAColumn(name = "demo_mode", sqlType = java.sql.Types.BOOLEAN)
	public boolean getDemoMode() {
		return demoMode;
	}

	public boolean isDemoMode() {
		return getDemoMode();
	}

	public void setDemoMode(boolean newValue) {
		boolean old = demoMode;
		fireBeforePropertyChange(P_DemoMode, old, newValue);
		this.demoMode = newValue;
		firePropertyChange(P_DemoMode, old, this.demoMode);
	}

	@OAProperty(maxLength = 18, displayLength = 18, uiColumnLength = 7, isProcessed = true)
	@OAColumn(name = "release", maxLength = 18)
	public String getRelease() {
		return release;
	}

	public void setRelease(String newValue) {
		String old = release;
		fireBeforePropertyChange(P_Release, old, newValue);
		this.release = newValue;
		firePropertyChange(P_Release, old, this.release);
	}

	@OAOne(displayName = "App User Login", reverseName = AppUserLogin.P_AppServers, isProcessed = true, fkeys = {
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
