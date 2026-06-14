package com.test.pos.delegate.oa;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.test.pos.delegate.ModelDelegate;
import com.test.pos.model.oa.*;
import com.test.pos.model.oa.method.StoreSafeLedgerEntryCreateTillLedgerEntryMethod;
import com.viaoa.compare.OACompare;
import com.viaoa.converter.OAConv;
import com.viaoa.datetime.OADateTime;
import com.viaoa.math.OAMath;
import com.viaoa.runtime.OARuntime;

public class StoreSafeLedgerEntryDelegate {

	private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	public static void afterSettingType(StoreSafeLedgerEntry ssle) {
		if (ssle == null) return;
		if (OARuntime.thread().isRemoteThread()) return;
		TeamMember tm = ssle.getTeamMember();
		if (tm == null) {
			tm = ModelDelegate.getCurrentTeamMember();
			ssle.setTeamMember(tm);
		}
	}
	
	public static boolean getUsesCash(StoreSafeLedgerEntry safeLedgerEntry) {
		if (safeLedgerEntry == null) return false;
		boolean b = false;
		switch (safeLedgerEntry.getType()) {
		case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_ExchangeCash:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_BankToSafe:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeToBank:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_Audit:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_Variance:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_Validation:
			b = true;
			break;
		}
		return b;
	}

	public static boolean getUsesChecks(StoreSafeLedgerEntry safeLedgerEntry) {
		if (safeLedgerEntry == null) return false;
		boolean b = false;
		switch (safeLedgerEntry.getType()) {
		case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
			break;
		case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
			break;
		case StoreSafeLedgerEntry.TYPE_ExchangeCash:
			break;
		case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_BankToSafe:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeToBank:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_Variance:
			break;
		case StoreSafeLedgerEntry.TYPE_Audit:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_Validation:
			break;
		}
		return b;
	}

	public static boolean getUsesPettyCash(StoreSafeLedgerEntry safeLedgerEntry) {
		if (safeLedgerEntry == null) return false;
		boolean b = false;
		switch (safeLedgerEntry.getType()) {
		case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
			break;
		case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_ExchangeCash:
			break;
		case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
			break;
		case StoreSafeLedgerEntry.TYPE_BankToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeToBank:
			break;
		case StoreSafeLedgerEntry.TYPE_Variance:
			break;
		case StoreSafeLedgerEntry.TYPE_Audit:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_Validation:
			break;
		}
		return b;
	}
	
	public static boolean getUsesLedgerDenominationBundle(StoreSafeLedgerEntry ssle) {
		boolean b = false;
		switch (ssle.getType()) {
		case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_ExchangeCash:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_BankToSafe:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeToBank:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_Variance:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_Audit:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_Validation:
			b = true;
			break;
		}
		return b;
	}
	
	public static boolean getUsesInvoicePaymentChecks(StoreSafeLedgerEntry ssle) {
		boolean b = false;
		switch (ssle.getType()) {
		case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
			break;
		case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
			break;
		case StoreSafeLedgerEntry.TYPE_ExchangeCash:
			break;
		case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
			break;
		case StoreSafeLedgerEntry.TYPE_BankToSafe:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeToBank:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_Variance:
			break;
		case StoreSafeLedgerEntry.TYPE_Audit:
			break;
		case StoreSafeLedgerEntry.TYPE_Validation:
			break;
		}
		return b;
	}

	
	public static Store getCalcStore(StoreSafeLedgerEntry safeLedgerEntry) {
		if (safeLedgerEntry == null) return null;
		Store store = null;
		StoreSafe ss = safeLedgerEntry.getStoreSafe();
		if (ss != null) store = ss.getStore();
		if (store == null) {
			TillLedgerEntry tle = safeLedgerEntry.getTillLedgerEntry();
			if (tle != null) store = tle.getTill().getStore();
		}
		return store;
	}

	
	
	public static double getTotalAmount(StoreSafeLedgerEntry ssle) {
		if (ssle == null) return 0.0;
		double d = OAMath.add(getTotalCashAmount(ssle), getTotalCheckAmount(ssle));
		return d;
	}
	
	
	public static double getTotalCashAmount(StoreSafeLedgerEntry storeSafeLedgerEntry) {
		if (storeSafeLedgerEntry == null) return 0.0;
		double d, d2; 
		
		d = storeSafeLedgerEntry.getLooseCashAmount();
		for (LedgerDenominationBundle ldb : storeSafeLedgerEntry.getLedgerDenominationBundles()) {
			d2 = ldb.getTotalAmount();
			d = OAMath.add(d, d2, 2);
		}
		return d;
	}

	public static int getCalcCheckCount(StoreSafeLedgerEntry ssle) {
		if (ssle == null) return 0;
		int x;
		if (getUsesInvoicePaymentChecks(ssle)) {
			x = ssle.getInvoicePaymentChecks().getSize();
		}
		else {
			TillLedgerEntry tle = ssle.getTillLedgerEntry();
			if (tle != null && tle.getUsesInvoicePaymentChecks()) {
				x = tle.getCalcCheckCount();
			}
			else x = ssle.getCheckCount();
		}
		return x;
	}

	public static double getTotalCheckAmount(StoreSafeLedgerEntry ssle) {
		if (ssle == null) return 0.0;
		double d = 0.0;
		if (getUsesInvoicePaymentChecks(ssle)) {
			for (InvoicePaymentCheck pc : ssle.getInvoicePaymentChecks()) {
				InvoicePayment ip = pc.getInvoicePayment();
				if (ip != null) d = OAMath.add(d, ip.getAmount(), 2);
			}
		}		
		else {
			TillLedgerEntry tle = ssle.getTillLedgerEntry();
			if (tle != null && tle.getUsesInvoicePaymentChecks()) {
				for (InvoicePaymentCheck pc : tle.getInvoicePaymentChecks()) {
					InvoicePayment ip = pc.getInvoicePayment();
					if (ip != null) d = OAMath.add(d, ip.getAmount(), 2);
				}
			}
			else d = ssle.getCheckAmount();
		}
		return d;
	}

	public static boolean getCanPost(final StoreSafeLedgerEntry ssle) {
		return getCantPostReason(ssle) == null;
	}
	
	public static String getCantPostReason(final StoreSafeLedgerEntry ssle) {
		if (ssle == null) return "Safe Ledger Entry is required";
		if (ssle.getPosted() != null) return "already posted";
		
		StoreSafe storeSafe = ssle.getStoreSafe();
		if (storeSafe == null) return "Store Safe is required";
		
		double d, d2;
		String invalid = null;
		TillLedgerEntry tle;
		
		switch (ssle.getType()) {
		case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
			tle = ssle.getTillLedgerEntry(); 
			if (tle == null) return "Till ledger entry is required";
			if (OACompare.compare(tle.getTotalCashAmount(), 0.00, 2) <= 0) return "Must enter cash amount";
			if (!OACompare.isEqual(ssle.getTotalCashAmount(), tle.getTotalCashAmount(), 2)) return "Cash amount from Till entry does not match amount for Safe entry";
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
			tle = ssle.getTillLedgerEntry(); 
			if (tle == null) return "Till ledger entry is required";
			if (ssle.getTotalCashAmount() == 0.0) return "No cash amount  entered";
			break;
		case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
			tle = ssle.getTillLedgerEntry(); 
			if (tle == null) return "Till ledger entry is required";
			break;
		case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
			tle = ssle.getTillLedgerEntry(); 
			if (ssle.getInvoicePaymentChecks().size() == 0) return "no checks have been selected from Safe";
			if (tle == null) return "Till ledger entry is required";
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
			if (OACompare.compare(ssle.getTotalCashAmount(), 0.00, 2) <= 0) return "Must enter cash amount";
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
			if (ssle.getTotalCashAmount() == 0.0) return "No cash amount entered";
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
			if (OACompare.compare(ssle.getTotalCashAmount(), 0.00, 2) <= 0) return "Must enter cash amount";
			break;
		case StoreSafeLedgerEntry.TYPE_ExchangeCash:
			if (OACompare.compare(ssle.getTotalCashAmount(), 0.00, 2) <= 0) return "Must enter cash amount";
			break;
		case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
			if (OACompare.compare(ssle.getTotalCashAmount(), 0.00, 2) <= 0) return "Must enter cash amount for fee";
			break;
		case StoreSafeLedgerEntry.TYPE_BankToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeToBank:
			break;
		case StoreSafeLedgerEntry.TYPE_Variance:
			if (OACompare.compare(ssle.getTotalCashAmount(), 0.00, 2) == 0) return "Must enter cash amount";
			break;
		case StoreSafeLedgerEntry.TYPE_Audit:
			if (!OACompare.isEqual(ssle.getTotalCashAmount(), storeSafe.getCashAmount(), 2)) {
				invalid = "total cash must be same as the safe's cash amount";				
			}
			else {
				if (!OACompare.isEqual(ssle.getPettyCashAmount(), storeSafe.getPettyCashAmount(), 2)) {
					invalid = "total petty cash must be same as the safe's petty cash amount";				
				}
				else {
					if (ssle.getCheckCount() != storeSafe.getCheckCount()) {
						invalid = "check count needs to be same as the number of checks in the safe";
					}
					if (!OACompare.isEqual(ssle.getTotalCheckAmount(), storeSafe.getTotalCheckAmount(), 2)) {
						invalid = "check amount needs to be same as the all of the checks currently in the safe";
					}
				}
			}
			break;
		case StoreSafeLedgerEntry.TYPE_Validation:
			StoreSafeLedgerEntry sleAudit = null;
			for (StoreSafeLedgerEntry slex : storeSafe.getStoreSafeLedgerEntries()) {
				if (slex.getType() == StoreSafeLedgerEntry.TYPE_Audit) sleAudit = slex;
			}
			if (sleAudit == null) {
				invalid = "can't do a validation without a past Audit";
			}
			break;
		}
		return invalid;
	}
	
	
	
	public static void post(final StoreSafeLedgerEntry ssle) throws Exception {
		if (ssle.getPosted() != null) return;
		if (!getCanPost(ssle)) return;

		final StoreSafe ss = ssle.getStoreSafe();
		if (ss == null) return;
		double d, d2; 

		boolean bValid = true;
		switch (ssle.getType()) {
		case StoreSafeLedgerEntry.TYPE_Unknown: 
			break;
		case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
			d = ssle.getTotalCashAmount();
			d = OAMath.add(ss.getCashAmount(), d, 2);
			ss.setCashAmount(d);
			break;

		case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
			// initiated from Till ledger entry
			d = ssle.getTotalCashAmount();
			d = OAMath.subtract(ss.getCashAmount(), d, 2);
			ss.setCashAmount(d);
			
			TillLedgerEntry tle = ssle.getTillLedgerEntry();
			d = ssle.getTotalCashAmount();
			tle.setLooseCashAmount(d);
			tle.post();
			break;
			
		case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
			for (InvoicePaymentCheck ipc : ssle.getInvoicePaymentChecks()) {
				ss.getInvoicePaymentChecks().add(ipc);
				ipc.setLocation(InvoicePaymentCheck.LOCATION_Safe);
			}
			break;
			
		case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
			// remove from Safe
			tle = ssle.getTillLedgerEntry();
			for (InvoicePaymentCheck ipc : ssle.getInvoicePaymentChecks()) {
				ss.getInvoicePaymentChecks().remove(ipc);
				tle.getInvoicePaymentChecks().add(ipc);
			}
			
			tle.post();
			break;
			
		case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
			d = ssle.getTotalCashAmount();
			
			d2 = OAMath.add(ss.getCashAmount(), d, 2);
			ss.setCashAmount(d2);

			d2 = OAMath.subtract(ss.getPettyCashAmount(), d, 2);
			ss.setPettyCashAmount(d2);
			break;
			
		case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
			d = ssle.getTotalCashAmount();

			d2 = OAMath.subtract(ss.getCashAmount(), d, 2);
			ss.setCashAmount(d2);
			
			d2 = OAMath.add(ss.getPettyCashAmount(), d, 2);
			ss.setPettyCashAmount(d2);
			break;
			
		case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
			d = ssle.getTotalCashAmount();
			d2 = OAMath.subtract(ss.getPettyCashAmount(), d, 2);
			ss.setPettyCashAmount(d2);
			break;
			
		case StoreSafeLedgerEntry.TYPE_ExchangeCash:
			break;
			
		case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
			d = ssle.getTotalCashAmount();
			ss.setCashAmount(OAMath.add(ss.getCashAmount(), d, 2));
			break;
			
		case StoreSafeLedgerEntry.TYPE_BankToSafe:
//qqqqqqqqqqqqqqqqqqqqqqqqq needs to also handle checks
			d = ssle.getTotalCashAmount();
			ss.setCashAmount(OAMath.add(ss.getCashAmount(), d, 2));
			break;
		case StoreSafeLedgerEntry.TYPE_SafeToBank:
//qqqqqqqqqqqqqqqqqqqqqqqqq needs to also handle checks
			d = ssle.getTotalCashAmount();
			ss.setCashAmount(OAMath.subtract(ss.getCashAmount(), d, 2));
			break;

		case StoreSafeLedgerEntry.TYPE_Variance:
			d = ssle.getTotalCashAmount();
			ss.setCashAmount(OAMath.add(ss.getCashAmount(), d, 2));
			break;
		case StoreSafeLedgerEntry.TYPE_Audit:
			break;
		case StoreSafeLedgerEntry.TYPE_Validation:
	
			StoreSafeLedgerEntry sleAudit = null;
			for (StoreSafeLedgerEntry slex : ss.getStoreSafeLedgerEntries()) {
				if (slex.getType() == StoreSafeLedgerEntry.TYPE_Audit) sleAudit = slex;
			}
			if (sleAudit == null) {
				bValid = false;
				break;
			}
			
			boolean bFound = false;
			double dCash = 0.0;
			int cntChecks = 0;
			double dChecks = 0.0;
			double dPettyCash = 0.0;
			
			for (StoreSafeLedgerEntry slex : ss.getStoreSafeLedgerEntries()) {
				if (slex == ssle) break;
				if (!bFound) {
					if (slex == sleAudit) {
						bFound = true;
						dCash = slex.getTotalCashAmount();
						cntChecks = slex.getCheckCount();
						dChecks = slex.getCheckAmount();
						dPettyCash = slex.getPettyCashAmount();
					}
					continue;
				}

				switch (slex.getType()) {
				case StoreSafeLedgerEntry.TYPE_Unknown:
					break;
				case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
					dCash = OAMath.add(dCash, slex.getTotalCashAmount());
					break;
				case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
					dCash = OAMath.subtract(dCash, slex.getTotalCashAmount());
					break;
				case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
					cntChecks += slex.getCalcCheckCount();
					dChecks = OAMath.add(dChecks, slex.getTotalCheckAmount(), 2);
					break;
				case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
					cntChecks -= slex.getCalcCheckCount();
					dChecks = OAMath.subtract(dChecks, slex.getTotalCheckAmount(), 2);
					break;
				case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
					dCash = OAMath.add(dCash, slex.getTotalCashAmount());
					dPettyCash = OAMath.subtract(dCash, slex.getTotalCashAmount());
					break;
				case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
					dCash = OAMath.subtract(dCash, slex.getTotalCashAmount());
					dPettyCash = OAMath.add(dCash, slex.getTotalCashAmount());
					break;
				case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
					dPettyCash = OAMath.subtract(dCash, slex.getTotalCashAmount());
					break;
				case StoreSafeLedgerEntry.TYPE_ExchangeCash:
					break;
				case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
					dCash = OAMath.add(dCash, slex.getTotalCashAmount());
					break;
				case StoreSafeLedgerEntry.TYPE_BankToSafe:
					dCash = OAMath.add(dCash, slex.getTotalCashAmount());
					break;
				case StoreSafeLedgerEntry.TYPE_SafeToBank:
					dCash = OAMath.subtract(dCash, slex.getTotalCashAmount());
					break;
				case StoreSafeLedgerEntry.TYPE_Audit:
					break;
				case StoreSafeLedgerEntry.TYPE_Variance:
					dCash = OAMath.add(dCash, slex.getTotalCashAmount());
					break;
				case StoreSafeLedgerEntry.TYPE_Validation:
					break;
				}
			}
			
			ssle.setLooseCashAmount(dCash);
			ssle.setCheckCount(cntChecks);
			ssle.setCheckAmount(dChecks);
			ssle.setPettyCashAmount(dPettyCash);
			
			if (OACompare.compare(dCash, ss.getCashAmount(), 2) != 0) bValid = false;
			else if (OACompare.compare(dChecks, ss.getTotalCheckAmount(), 2) != 0) bValid = false;
			else if (ss.getInvoicePaymentChecks().size() != cntChecks) bValid = false;
			else if (OACompare.compare(dPettyCash, ss.getPettyCashAmount(), 2) != 0) bValid = false;
			
			break;
		}
		if (bValid) ssle.setPosted(new OADateTime());
	}

	public static boolean getNeedsToCreateTillLedgerEntry(StoreSafeLedgerEntry ssle) {
		if (ssle == null) return false;
	    int type = ssle.getType();
	    
	    boolean b = false;
		switch (ssle.getType()) {
		case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
			break;
		case StoreSafeLedgerEntry.TYPE_BankToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeToBank:
			break;
		case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
			b = true;
			break;
		case StoreSafeLedgerEntry.TYPE_ExchangeCash:
			break;
		case StoreSafeLedgerEntry.TYPE_Variance:
			break;
		case StoreSafeLedgerEntry.TYPE_Audit:
			break;
		}

		if (b) {
	        TillLedgerEntry tillLedgerEntry = ssle.getTillLedgerEntry();
		    if (tillLedgerEntry != null) b = false;
		}
		return b;
	}

	public static void createTillLedgerEntry(StoreSafeLedgerEntryCreateTillLedgerEntryMethod data, StoreSafeLedgerEntry ssle) {
		if (data == null || ssle == null) return;
		Till till = data.getTill();
		if (till == null) return;

		if (!getNeedsToCreateTillLedgerEntry(ssle)) return;
		
		int type = 0;
		
		switch (ssle.getType()) {
		case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
			type = TillLedgerEntry.TYPE_CashFromSafe;
			break;
		case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
			type = TillLedgerEntry.TYPE_ChecksFromSafe;
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
			break;
		case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
			break;
		case StoreSafeLedgerEntry.TYPE_ExchangeCash:
			break;
		case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
			break;
		case StoreSafeLedgerEntry.TYPE_BankToSafe:
			break;
		case StoreSafeLedgerEntry.TYPE_SafeToBank:
			break;
		case StoreSafeLedgerEntry.TYPE_Variance:
			break;
		case StoreSafeLedgerEntry.TYPE_Audit:
			break;
		case StoreSafeLedgerEntry.TYPE_Validation:
			break;
		}
		if (type == 0) return;
		
		TillLedgerEntry tle = new TillLedgerEntry();
		tle.setType(type);
		ssle.setTillLedgerEntry(tle);
		tle.setTill(till);
	}


	
/*	qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq
	switch (safeLedgerEntry.getType()) {
	case StoreSafeLedgerEntry.TYPE_Unknown:
		break;
	case StoreSafeLedgerEntry.TYPE_TillCashToSafe:
		break;
	case StoreSafeLedgerEntry.TYPE_SafeCashToTill:
		break;
	case StoreSafeLedgerEntry.TYPE_TillChecksToSafe:
		break;
	case StoreSafeLedgerEntry.TYPE_SafeChecksToTill:
		break;
	case StoreSafeLedgerEntry.TYPE_PettyCashToSafe:
		break;
	case StoreSafeLedgerEntry.TYPE_SafeCashToPettyCash:
		break;
	case StoreSafeLedgerEntry.TYPE_PettyCashUsed:
		break;
	case StoreSafeLedgerEntry.TYPE_ExchangeCash:
		break;
	case StoreSafeLedgerEntry.TYPE_ReturnedCheckFee:
		break;
	case StoreSafeLedgerEntry.TYPE_BankToSafe:
		break;
	case StoreSafeLedgerEntry.TYPE_SafeToBank:
		break;
	case StoreSafeLedgerEntry.TYPE_Audit:
		break;
	case StoreSafeLedgerEntry.TYPE_Variance:
		break;
	case StoreSafeLedgerEntry.TYPE_Validation:
		break;
	}
*/

/* Work Flows .. Testing

>Till	
invoicePayment.type=cash -> tle.CashPurchase -> +till ... TESTED
invoiceRefund.type=cash -> tle.CashRefund -> -till ... 
invoicePayment.type=check -> tle.CheckPurchase -> +till
invoiceRefund.type=check -> tle.CheckRefund -> -till
till.setRegister(register) -> tle.MoveTillToRegister ... TESTED
till.setRegister(null) -> tle.MoveTillToSafe  ... TESTED
tle.Audit  ... TESTED
tle.variance -> till  ... TESTED
tle.validation  ... TESTED
tle.ExchangeCash ... TESTED

>Till&Safe
tle.CashToSafe -> -till -> sle.TillCashToSafe -> +safe  ... TESTED
sle.SafeCashToTill -> -safe -> tle.CashFromSafe -> +till  ... TESTED
tle.ChecksToSafe -> -till -> sle.TillChecksToSafe -> +safe  ... TESTED
sle.SafeChecksToTill -> -safe -> tle.ChecksFromSafe -> +till  ... TESTED

>Safe
sle.PettyCashToSafe -> -pettyCase,+safe  ... TESTED
sle.SafeCashToPettyCash -> -safe,+pettyCash  ... TESTED
sle.PettyCashUsed -> -pettyCash  ... TESTED (qqqq needs to use ManualPO)
sle.ExchangeCash  ... TESTED
sle.ReturnedCheckFee -> +safe
sle.BankToSafe  ... TESTED

sle.SafeToBank  ... TESTED
sle.audit  ... TESTED
sle.variance -> safe  ... TESTED
sle.validation  ... TESTED



qqqqqqqqq track checks	

qqqqqqqqqqqqq
  remove POST and replace logic for the Save command	
	
	
	
	
*/	
	
}




















