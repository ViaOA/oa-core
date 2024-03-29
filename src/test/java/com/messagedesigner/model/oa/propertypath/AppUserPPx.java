// Generated by OABuilder
package com.messagedesigner.model.oa.propertypath;

import java.io.Serializable;

import com.messagedesigner.model.oa.AppUser;

public class AppUserPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public AppUserPPx(String name) {
		this(null, name);
	}

	public AppUserPPx(PPxInterface parent, String name) {
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

	public AppUserLoginPPx appUserLogins() {
		AppUserLoginPPx ppx = new AppUserLoginPPx(this, AppUser.P_AppUserLogins);
		return ppx;
	}

	public String id() {
		return pp + "." + AppUser.P_Id;
	}

	public String loginId() {
		return pp + "." + AppUser.P_LoginId;
	}

	public String password() {
		return pp + "." + AppUser.P_Password;
	}

	public String admin() {
		return pp + "." + AppUser.P_Admin;
	}

	public String superAdmin() {
		return pp + "." + AppUser.P_SuperAdmin;
	}

	public String editProcessed() {
		return pp + "." + AppUser.P_EditProcessed;
	}

	public String firstName() {
		return pp + "." + AppUser.P_FirstName;
	}

	public String lastName() {
		return pp + "." + AppUser.P_LastName;
	}

	public String inactiveDate() {
		return pp + "." + AppUser.P_InactiveDate;
	}

	public String note() {
		return pp + "." + AppUser.P_Note;
	}

	public String fullName() {
		return pp + "." + AppUser.P_FullName;
	}

	public String displayName() {
		return pp + "." + AppUser.P_DisplayName;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
