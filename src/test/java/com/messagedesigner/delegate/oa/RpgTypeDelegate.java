package com.messagedesigner.delegate.oa;

import com.messagedesigner.delegate.ModelDelegate;
import com.messagedesigner.model.oa.RpgType;
import com.viaoa.util.OAString;

public class RpgTypeDelegate {

	public static RpgType getStringRpgType() {
		for (RpgType rt : ModelDelegate.getRpgTypes()) {
			if (rt.getEncodeType() == RpgType.ENCODETYPE_None) {
				if (OAString.isEmpty(rt.getDefaultFormat())) {
					if (rt.getName().toLowerCase().indexOf("date") < 0) {
						return rt;
					}
				}
			}
		}
		return null;
	}

}
