package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.AppUserLogin;

public class AppUserLoginPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public AppUserLoginPPx(String name) {
		this(null, name);
	}

	public AppUserLoginPPx(PPxInterface parent, String name) {
		String s = null;
		if (parent != null) {
			s = parent.toString();
		}
		if (s == null) {
			s = "";
		}
		if (name != null && name.length() > 0) {
			if (s.length() > 0 && name.charAt(0) != ':') {
				s += ".";
			}
			s += name;
		}
		pp = s;
	}

	public AppServerPPx appServers() {
		AppServerPPx ppx = new AppServerPPx(this, AppUserLogin.P_AppServers);
		return ppx;
	}

	public AppUserPPx appUser() {
		AppUserPPx ppx = new AppUserPPx(this, AppUserLogin.P_AppUser);
		return ppx;
	}

	public AppUserErrorPPx appUserErrors() {
		AppUserErrorPPx ppx = new AppUserErrorPPx(this, AppUserLogin.P_AppUserErrors);
		return ppx;
	}

	public String id() {
		return pp + "." + AppUserLogin.P_Id;
	}

	public String created() {
		return pp + "." + AppUserLogin.P_Created;
	}

	public String location() {
		return pp + "." + AppUserLogin.P_Location;
	}

	public String computerName() {
		return pp + "." + AppUserLogin.P_ComputerName;
	}

	public String disconnected() {
		return pp + "." + AppUserLogin.P_Disconnected;
	}

	public String connectionId() {
		return pp + "." + AppUserLogin.P_ConnectionId;
	}

	public String hostName() {
		return pp + "." + AppUserLogin.P_HostName;
	}

	public String ipAddress() {
		return pp + "." + AppUserLogin.P_IpAddress;
	}

	public String totalMemory() {
		return pp + "." + AppUserLogin.P_TotalMemory;
	}

	public String freeMemory() {
		return pp + "." + AppUserLogin.P_FreeMemory;
	}

	public AppUserLoginPPx connectedFilter() {
		AppUserLoginPPx ppx = new AppUserLoginPPx(this, ":connected()");
		return ppx;
	}

	public AppUserLoginPPx lastDayFilter() {
		AppUserLoginPPx ppx = new AppUserLoginPPx(this, ":lastDay()");
		return ppx;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
