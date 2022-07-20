package com.corptostore.delegate.oa;

import com.corptostore.model.oa.TesterStepType;

public class TesterStepTypeDelegate {

	public static TesterStepType getTesterStepType(int type) {
		return null; //was: ModelDelegate.getTesterStepTypes().find(TesterStepType.P_Type, type);
	}

	public static TesterStepType getTesterStepType(TesterStepType.Type type) {
		if (type == null) {
			return null;
		}
		return null; //was: ModelDelegate.getTesterStepTypes().find(TesterStepType.P_Type, type.ordinal());
	}

}
