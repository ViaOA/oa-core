package com.corptostore.delegate.oa;

import com.corptostore.model.oa.Tester;
import com.corptostore.model.oa.TesterStep;
import com.corptostore.model.oa.TesterStepType;
import com.corptostore.model.oa.propertypath.TesterStepPP;

public class TesterStepDelegate {

	public static TesterStep getTesterStep(final Tester tester, int type) {
		if (tester == null) {
			return null;
		}
		return tester.getTesterSteps().find(TesterStepPP.testerStepType().type(), type);
	}

	public static TesterStep getTesterStep(final Tester tester, TesterStepType.Type type) {
		if (tester == null || type == null) {
			return null;
		}
		return getTesterStep(tester, type.ordinal());
	}
}
