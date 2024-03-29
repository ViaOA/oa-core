// Generated by OABuilder
package com.messagedesigner.model.oa.propertypath;

import com.messagedesigner.model.oa.JsonType;

public class JsonTypePP {
	private static RpgTypePPx rpgTypes;

	public static RpgTypePPx rpgTypes() {
		if (rpgTypes == null) {
			rpgTypes = new RpgTypePPx(JsonType.P_RpgTypes);
		}
		return rpgTypes;
	}

	public static String id() {
		String s = JsonType.P_Id;
		return s;
	}

	public static String created() {
		String s = JsonType.P_Created;
		return s;
	}

	public static String name() {
		String s = JsonType.P_Name;
		return s;
	}

	public static String type() {
		String s = JsonType.P_Type;
		return s;
	}

	public static String seq() {
		String s = JsonType.P_Seq;
		return s;
	}

	public static String defaultJavaClassType() {
		String s = JsonType.P_DefaultJavaClassType;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
