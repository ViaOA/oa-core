package com.viaoa.object;

import com.viaoa.annotation.OAFkey;

/**
 * Used by OALinkInfo for type=One to know which properties to match between objects. <br>
 * fkey to pkey
 *
 * @author vvia
 */
public class OAFkeyInfo implements java.io.Serializable {
	static final long serialVersionUID = 1L;

	private OAPropertyInfo fromPropertyInfo;
	private OAPropertyInfo toPropertyInfo;

	private OAFkey oaFkey;

	public OAPropertyInfo getFromPropertyInfo() {
		return fromPropertyInfo;
	}

	public void setFromPropertyInfo(OAPropertyInfo pi) {
		this.fromPropertyInfo = pi;
	}

	public OAPropertyInfo getToPropertyInfo() {
		return toPropertyInfo;
	}

	public void setToPropertyInfo(OAPropertyInfo pi) {
		this.toPropertyInfo = pi;
	}

	public void setOAFkey(OAFkey f) {
		oaFkey = f;
	}

	public OAFkey getOAFkey() {
		return oaFkey;
	}

}
