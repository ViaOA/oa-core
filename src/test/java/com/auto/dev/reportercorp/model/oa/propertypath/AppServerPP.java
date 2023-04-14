package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.AppServer;

public class AppServerPP {
	private static AppUserLoginPPx appUserLogin;

	public static AppUserLoginPPx appUserLogin() {
		if (appUserLogin == null) {
			appUserLogin = new AppUserLoginPPx(AppServer.P_AppUserLogin);
		}
		return appUserLogin;
	}

	public static String id() {
		String s = AppServer.P_Id;
		return s;
	}

	public static String created() {
		String s = AppServer.P_Created;
		return s;
	}

	public static String started() {
		String s = AppServer.P_Started;
		return s;
	}

	public static String demoMode() {
		String s = AppServer.P_DemoMode;
		return s;
	}

	public static String release() {
		String s = AppServer.P_Release;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
