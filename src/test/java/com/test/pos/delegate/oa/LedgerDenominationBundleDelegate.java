package com.test.pos.delegate.oa;

import com.test.pos.model.oa.*;
import com.viaoa.converter.OAConv;
import com.viaoa.datetime.OADateTime;
import com.viaoa.math.OAMath;

public class LedgerDenominationBundleDelegate {

	public static double getTotalAmount(LedgerDenominationBundle ledgerDenominationBundle) {
		if (ledgerDenominationBundle == null) return 0.0;
		
		final int qty = ledgerDenominationBundle.getQuantity();
		if (qty == 0) return 0.0;
		
		DenominationBundle db = ledgerDenominationBundle.getDenominationBundle();
		if (db == null) return 0.0;
		
		int packSize = db.getPackSize();
		if (packSize == 0) return 0.0;
		
		CurrencyDenomination cd = db.getCurrencyDenomination();
		if (cd == null) return 0.0;

		double unitValue = cd.getUnitValue();
		if (unitValue == 0.0) return 0.0;
		
		double d = OAMath.multiply(packSize, unitValue, 2);
		
		d = OAMath.multiply(d, qty, 2);
		
		return d;
	}

	public static Store getCalcStore(LedgerDenominationBundle db) {
		if (db == null) return null;
		
		StoreSafeLedgerEntry sle = db.getStoreSafeLedgerEntry();
		if (sle != null) {
			StoreSafe ss = sle.getStoreSafe();
			if (ss != null) {
				Store store = ss.getStore();
				if (store != null) return store;
			}
		}
		TillLedgerEntry tle = db.getTillLedgerEntry();
		if (tle != null) {
			Till till = tle.getTill();
			if (till != null) {
				Store store = till.getStore();
				if (store != null) return store;
			}
		}
		return null;
	}

	public static OADateTime getPosted(final LedgerDenominationBundle ldb) {
		if (ldb == null) return null;

		OADateTime posted = null;
		StoreSafeLedgerEntry storeSafeLedgerEntry = ldb.getStoreSafeLedgerEntry();
		if (storeSafeLedgerEntry != null) {
			posted = storeSafeLedgerEntry.getPosted();
		} else {
			TillLedgerEntry tillLedgerEntry = ldb.getTillLedgerEntry();
			if (tillLedgerEntry != null) {
				posted = tillLedgerEntry.getPosted();
			}
		}
		return posted;
	}

	public static boolean getCalcEnabled(LedgerDenominationBundle ldb) {
		if (ldb == null) return false;
		return getPosted(ldb) == null;
	}
	
	
}
