package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.AppUserLogin;

public class AppUserLoginPP {
	private static AppServerPPx appServers;
	private static AppUserPPx appUser;
	private static AppUserErrorPPx appUserErrors;

	public static AppServerPPx appServers() {
		if (appServers == null) {
			appServers = new AppServerPPx(AppUserLogin.P_AppServers);
		}
		return appServers;
	}

	public static AppUserPPx appUser() {
		if (appUser == null) {
			appUser = new AppUserPPx(AppUserLogin.P_AppUser);
		}
		return appUser;
	}

	public static AppUserErrorPPx appUserErrors() {
		if (appUserErrors == null) {
			appUserErrors = new AppUserErrorPPx(AppUserLogin.P_AppUserErrors);
		}
		return appUserErrors;
	}

	public static String id() {
		String s = AppUserLogin.P_Id;
		return s;
	}

	public static String created() {
		String s = AppUserLogin.P_Created;
		return s;
	}

	public static String location() {
		String s = AppUserLogin.P_Location;
		return s;
	}

	public static String computerName() {
		String s = AppUserLogin.P_ComputerName;
		return s;
	}

	public static String disconnected() {
		String s = AppUserLogin.P_Disconnected;
		return s;
	}

	public static String connectionId() {
		String s = AppUserLogin.P_ConnectionId;
		return s;
	}

	public static String hostName() {
		String s = AppUserLogin.P_HostName;
		return s;
	}

	public static String ipAddress() {
		String s = AppUserLogin.P_IpAddress;
		return s;
	}

	public static String totalMemory() {
		String s = AppUserLogin.P_TotalMemory;
		return s;
	}

	public static String freeMemory() {
		String s = AppUserLogin.P_FreeMemory;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}