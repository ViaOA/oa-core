package com.test.pos.delegate.oa;

import com.test.pos.model.oa.*;
import com.viaoa.runtime.OARuntime;

public class TillDelegate {

	public static void afterSetRegister(Till till, Register old, Register newValue) {
		if (till == null) return;
		if (OARuntime.thread().isRemoteThread()) return;

		TillLedgerEntry tle = null;

		tle = new TillLedgerEntry();

		if (newValue == null) {
			tle.setType(TillLedgerEntry.TYPE_MoveTillToSafe);
		}
		else {
			tle.setType(TillLedgerEntry.TYPE_MoveTillToRegister);
		}
		
		tle.setTill(till);
		
		try {
			tle.post();
		}
		catch (Exception e) {}
	}


}
