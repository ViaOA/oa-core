package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.filter.AppUserLoginConnectedFilter;
import com.auto.dev.reportercorp.model.oa.filter.AppUserLoginLastDayFilter;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "appUserLogin", pluralName = "AppUserLogins", shortName = "aul", displayName = "App User Login", useDataSource = false, isProcessed = true, displayProperty = "appUser.displayName", filterClasses = {
		AppUserLoginConnectedFilter.class, AppUserLoginLastDayFilter.class }, noPojo = true, rootTreePropertyPaths = {
				"[AppUser]." + AppUser.P_AppUserLogins
		})
public class AppUserLogin extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(AppUserLogin.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Location = "location";
	public static final String P_ComputerName = "computerName";
	public static final String P_Disconnected = "disconnected";
	public static final String P_ConnectionId = "connectionId";
	public static final String P_HostName = "hostName";
	public static final String P_IpAddress = "ipAddress";
	public static final String P_TotalMemory = "totalMemory";
	public static final String P_FreeMemory = "freeMemory";

	public static final String P_AppServers = "appServers";
	public static final String P_AppUser = "appUser";
	public static final String P_AppUserId = "appUserId"; // fkey
	public static final String P_AppUserErrors = "appUserErrors";

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String location;
	protected volatile String computerName;
	protected volatile OADateTime disconnected;
	protected volatile int connectionId;
	protected volatile String hostName;
	protected volatile String ipAddress;
	protected volatile long totalMemory;
	protected volatile long freeMemory;

	// Links to other objects.
	protected volatile transient AppUser appUser;
	protected transient Hub<AppUserError> hubAppUserErrors;

	public AppUserLogin() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public AppUserLogin(int id) {
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

	@OAProperty(maxLength = 50, displayLength = 18, uiColumnLength = 14, isProcessed = true)
	@OAColumn(name = "location", maxLength = 50)
	public String getLocation() {
		return location;
	}

	public void setLocation(String newValue) {
		String old = location;
		fireBeforePropertyChange(P_Location, old, newValue);
		this.location = newValue;
		firePropertyChange(P_Location, old, this.location);
	}

	@OAProperty(displayName = "Computer Name", maxLength = 32, displayLength = 32, uiColumnLength = 12, isProcessed = true)
	@OAColumn(name = "computer_name", maxLength = 32)
	public String getComputerName() {
		return computerName;
	}

	public void setComputerName(String newValue) {
		String old = computerName;
		fireBeforePropertyChange(P_ComputerName, old, newValue);
		this.computerName = newValue;
		firePropertyChange(P_ComputerName, old, this.computerName);
	}

	@OAProperty(displayLength = 15, isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "disconnected", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getDisconnected() {
		return disconnected;
	}

	public void setDisconnected(OADateTime newValue) {
		OADateTime old = disconnected;
		fireBeforePropertyChange(P_Disconnected, old, newValue);
		this.disconnected = newValue;
		firePropertyChange(P_Disconnected, old, this.disconnected);
	}

	@OAProperty(displayName = "Connection Id", displayLength = 5, isProcessed = true)
	@OAColumn(name = "connection_id", sqlType = java.sql.Types.INTEGER)
	public int getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(int newValue) {
		int old = connectionId;
		fireBeforePropertyChange(P_ConnectionId, old, newValue);
		this.connectionId = newValue;
		firePropertyChange(P_ConnectionId, old, this.connectionId);
	}

	@OAProperty(displayName = "Host Name", maxLength = 35, displayLength = 14, uiColumnLength = 10, isProcessed = true)
	@OAColumn(name = "host_name", maxLength = 35)
	public String getHostName() {
		return hostName;
	}

	public void setHostName(String newValue) {
		String old = hostName;
		fireBeforePropertyChange(P_HostName, old, newValue);
		this.hostName = newValue;
		firePropertyChange(P_HostName, old, this.hostName);
	}

	@OAProperty(displayName = "Ip Address", maxLength = 20, displayLength = 15, isProcessed = true)
	@OAColumn(name = "ip_address", maxLength = 20)
	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String newValue) {
		String old = ipAddress;
		fireBeforePropertyChange(P_IpAddress, old, newValue);
		this.ipAddress = newValue;
		firePropertyChange(P_IpAddress, old, this.ipAddress);
	}

	@OAProperty(displayName = "Total Memory", displayLength = 5, isProcessed = true)
	@OAColumn(name = "total_memory", sqlType = java.sql.Types.BIGINT)
	public long getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(long newValue) {
		long old = totalMemory;
		fireBeforePropertyChange(P_TotalMemory, old, newValue);
		this.totalMemory = newValue;
		firePropertyChange(P_TotalMemory, old, this.totalMemory);
	}

	@OAProperty(displayName = "Free Memory", displayLength = 5, isProcessed = true)
	@OAColumn(name = "free_memory", sqlType = java.sql.Types.BIGINT)
	public long getFreeMemory() {
		return freeMemory;
	}

	public void setFreeMemory(long newValue) {
		long old = freeMemory;
		fireBeforePropertyChange(P_FreeMemory, old, newValue);
		this.freeMemory = newValue;
		firePropertyChange(P_FreeMemory, old, this.freeMemory);
	}

	@OAMany(displayName = "App Servers", toClass = AppServer.class, reverseName = AppServer.P_AppUserLogin, isProcessed = true, createMethod = false)
	private Hub<AppServer> getAppServers() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAOne(displayName = "App User", reverseName = AppUser.P_AppUserLogins, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_AppUserId, toProperty = AppUser.P_Id) })
	public AppUser getAppUser() {
		if (appUser == null) {
			appUser = (AppUser) getObject(P_AppUser);
		}
		return appUser;
	}

	public void setAppUser(AppUser newValue) {
		AppUser old = this.appUser;
		fireBeforePropertyChange(P_AppUser, old, newValue);
		this.appUser = newValue;
		firePropertyChange(P_AppUser, old, this.appUser);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "app_user_id")
	public Integer getAppUserId() {
		return (Integer) getFkeyProperty(P_AppUserId);
	}

	public void setAppUserId(Integer newValue) {
		this.appUser = null;
		setFkeyProperty(P_AppUserId, newValue);
	}

	@OAMany(displayName = "App User Errors", toClass = AppUserError.class, owner = true, reverseName = AppUserError.P_AppUserLogin, cascadeSave = true, cascadeDelete = true)
	public Hub<AppUserError> getAppUserErrors() {
		if (hubAppUserErrors == null) {
			hubAppUserErrors = (Hub<AppUserError>) getHub(P_AppUserErrors);
		}
		return hubAppUserErrors;
	}
}
