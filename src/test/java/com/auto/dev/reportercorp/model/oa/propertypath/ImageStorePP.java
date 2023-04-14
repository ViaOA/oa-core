package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ImageStore;

public class ImageStorePP {

	public static String id() {
		String s = ImageStore.P_Id;
		return s;
	}

	public static String created() {
		String s = ImageStore.P_Created;
		return s;
	}

	public static String bytes() {
		String s = ImageStore.P_Bytes;
		return s;
	}

	public static String origFileName() {
		String s = ImageStore.P_OrigFileName;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
