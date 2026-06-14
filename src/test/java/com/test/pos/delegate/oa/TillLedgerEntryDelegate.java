package com.test.pos.delegate.oa;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.test.pos.delegate.ModelDelegate;
import com.test.pos.model.oa.*;
import com.viaoa.compare.OACompare;
import com.viaoa.converter.OAConv;
import com.viaoa.datetime.OADateTime;
import com.viaoa.math.OAMath;
import com.viaoa.runtime.OARuntime;
import com.viaoa.datetime.OADate;

public class TillLedgerEntryDelegate {

	private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	

	public static void afterSettingType(TillLedgerEntry tle) {
		if (tle == null) return;
		if (OARuntime.thread().isRemoteThread()) return;
		
		TeamMember tm = tle.getTeamMember();
		if (tm == null) {
			tm = ModelDelegate.getCurrentTeamMember();
			tle.setTeamMember(tm);
		}
	}
	
	public static boolean getUsesCash(TillLedgerEntry tle) {
		if (tle == null) return false;

		boolean b = false;
		switch (tle.getType()) {
		case TillLedgerEntry.TYPE_Unknown:
			break;
		case TillLedgerEntry.TYPE_CashPurchase:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CashRefund:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CheckPurchase:
			break;
		case TillLedgerEntry.TYPE_CheckRefund:
			break;
		case TillLedgerEntry.TYPE_CashFromSafe:
			b = true;
		    break;
		case TillLedgerEntry.TYPE_CashToSafe:
			b = true;
			break;
		case TillLedgerEntry.TYPE_ChecksToSafe:
			break;
		case TillLedgerEntry.TYPE_ChecksFromSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToRegister:
			break;
		case TillLedgerEntry.TYPE_Audit:
			b = true;
			break;
		case TillLedgerEntry.TYPE_Variance:
			b = true;
			break;
		case TillLedgerEntry.TYPE_Validation:
			break;
		case TillLedgerEntry.TYPE_ExchangeCash:
			b = true;
			break;
		}
		return b;
	}

	public static boolean getUsesChecks(TillLedgerEntry tle) {
		if (tle == null) return false;

		boolean b = false;
		switch (tle.getType()) {
		case TillLedgerEntry.TYPE_Unknown:
			break;
		case TillLedgerEntry.TYPE_CashPurchase:
			break;
		case TillLedgerEntry.TYPE_CashRefund:
			break;
		case TillLedgerEntry.TYPE_CheckPurchase:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CheckRefund:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CashFromSafe:
		    break;
		case TillLedgerEntry.TYPE_CashToSafe:
			break;
		case TillLedgerEntry.TYPE_ChecksToSafe:
			b = true;
			break;
		case TillLedgerEntry.TYPE_ChecksFromSafe:
			b = true;
			break;
		case TillLedgerEntry.TYPE_MoveTillToSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToRegister:
			break;
		case TillLedgerEntry.TYPE_Audit:
			b = true;
			break;
		case TillLedgerEntry.TYPE_Variance:
			break;
		case TillLedgerEntry.TYPE_Validation:
			break;
		case TillLedgerEntry.TYPE_ExchangeCash:
			break;
		}
		return b;
	}
	
	public static boolean getUsesLedgerDenominationBundle(TillLedgerEntry tle) {
		if (tle == null) return false;
		boolean b = false;
		switch (tle.getType()) {
		case TillLedgerEntry.TYPE_Unknown:
			break;
		case TillLedgerEntry.TYPE_CashPurchase:
			break;
		case TillLedgerEntry.TYPE_CashRefund:
			break;
		case TillLedgerEntry.TYPE_CheckPurchase:
			break;
		case TillLedgerEntry.TYPE_CheckRefund:
			break;
		case TillLedgerEntry.TYPE_CashFromSafe:
			b = true;
		    break;
		case TillLedgerEntry.TYPE_CashToSafe:
			b = true;
			break;
		case TillLedgerEntry.TYPE_ChecksToSafe:
			break;
		case TillLedgerEntry.TYPE_ChecksFromSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToRegister:
			break;
		case TillLedgerEntry.TYPE_Audit:
			b = true;
			break;
		case TillLedgerEntry.TYPE_Variance:
			b = true;
			break;
		case TillLedgerEntry.TYPE_Validation:
			break;
		case TillLedgerEntry.TYPE_ExchangeCash:
			b = true;
			break;
		}
		return b;
	}

	public static boolean getUsesInvoicePayment(TillLedgerEntry tle) {
		if (tle == null) return false;
		boolean b = false;
		switch (tle.getType()) {
		case TillLedgerEntry.TYPE_Unknown:
			break;
		case TillLedgerEntry.TYPE_CashPurchase:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CashRefund:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CheckPurchase:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CheckRefund:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CashFromSafe:
		    break;
		case TillLedgerEntry.TYPE_CashToSafe:
			break;
		case TillLedgerEntry.TYPE_ChecksToSafe:
			break;
		case TillLedgerEntry.TYPE_ChecksFromSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToRegister:
			break;
		case TillLedgerEntry.TYPE_Audit:
			b = true;
			break;
		case TillLedgerEntry.TYPE_Variance:
			break;
		case TillLedgerEntry.TYPE_Validation:
			b = true;
			break;
		case TillLedgerEntry.TYPE_ExchangeCash:
			break;
		}
		return b;
	}
	
	
	public static boolean getUsesInvoicePaymentChecks(TillLedgerEntry tle) {
		if (tle == null) return false;
		boolean b = false;
		switch (tle.getType()) {
		case TillLedgerEntry.TYPE_Unknown:
			break;
		case TillLedgerEntry.TYPE_CashPurchase:
			break;
		case TillLedgerEntry.TYPE_CashRefund:
			break;
		case TillLedgerEntry.TYPE_CheckPurchase:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CheckRefund:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CashFromSafe:
		    break;
		case TillLedgerEntry.TYPE_CashToSafe:
			break;
		case TillLedgerEntry.TYPE_ChecksToSafe:
			b = true;
			break;
		case TillLedgerEntry.TYPE_ChecksFromSafe:
			b = true;
			break;
		case TillLedgerEntry.TYPE_MoveTillToSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToRegister:
			break;
		case TillLedgerEntry.TYPE_Audit:
			break;
		case TillLedgerEntry.TYPE_Variance:
			b = true;
			break;
		case TillLedgerEntry.TYPE_Validation:
			b = true;
			break;
		case TillLedgerEntry.TYPE_ExchangeCash:
			break;
		}
		return b;
	}
	
	public static boolean getChangesTill(TillLedgerEntry tle) {
		if (tle == null) return false;

		boolean b = false;
		switch (tle.getType()) {
		case TillLedgerEntry.TYPE_Unknown:
			break;
		case TillLedgerEntry.TYPE_CashPurchase:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CashRefund:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CheckPurchase:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CheckRefund:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CashFromSafe:
			b = true;
		    break;
		case TillLedgerEntry.TYPE_CashToSafe:
			b = true;
			break;
		case TillLedgerEntry.TYPE_ChecksToSafe:
			b = true;
			break;
		case TillLedgerEntry.TYPE_ChecksFromSafe:
			b = true;
			break;
		case TillLedgerEntry.TYPE_MoveTillToSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToRegister:
			break;
		case TillLedgerEntry.TYPE_Audit:
			break;
		case TillLedgerEntry.TYPE_Variance:
			b = true;
			break;
		case TillLedgerEntry.TYPE_Validation:
			break;
		case TillLedgerEntry.TYPE_ExchangeCash:
			break;
		}
		return b;
	}

	
	public static double getTotalCashAmount(TillLedgerEntry tle) {
		if (tle == null) return 0.0;
	    double d = tle.getLooseCashAmount();
        for (LedgerDenominationBundle bundle : tle.getLedgerDenominationBundles()) {
            d = OAMath.add(d,  bundle.getTotalAmount(), 2);
        }
		return d;
	}
	
	public static int getCalcCheckCount(TillLedgerEntry tle) {
		if (tle == null) return 0;
		int x;
		if (getUsesInvoicePaymentChecks(tle)) {
			x = tle.getInvoicePaymentChecks().getSize();
		}
		else {
			x = tle.getCheckCount();
		}
		return x;
	}
	
	
	public static double getCalcTotalCheckAmount(TillLedgerEntry tle) {
		if (tle == null) return 0.0;
		
		double d = 0.0;
        if (!getUsesInvoicePaymentChecks(tle) && tle.getCheckCount() > 0) {
        	d = tle.getCheckAmount();
        }
        else {
        	InvoicePaymentCheck ipc = null;
	        InvoicePayment ip = tle.getInvoicePayment();
	        if (ip != null) {
	        	ipc = ip.getInvoicePaymentCheck();
	        }
            if (ipc != null) d = ip.getAmount();
            else {
		        for (InvoicePaymentCheck ipcx : tle.getInvoicePaymentChecks()) {
		        	d = OAMath.add(d, ipcx.getInvoicePayment().getAmount(), 2);
		        }
	        }
        }
		return d;
	}
	
	
	public static double getTotalAmount(TillLedgerEntry tle) {
		if (tle == null) return 0.0;

		double d = getTotalCashAmount(tle);
        double d2 = getCalcTotalCheckAmount(tle);
        
        d = OAMath.add(d, d2, 2);
        return d;
	}
	
	
	public static boolean getCanPost(TillLedgerEntry tle) {
		return getCantPostReason(tle) == null;
	}
	
	public static String getCantPostReason(TillLedgerEntry tle) {
		if (tle == null) return "TillLedgerEntry is required";
		if (tle.getPosted() != null) return "already posted";
		
		Till till = tle.getTill();
		if (till == null) return "Till is required";
		RegisterSession rs = tle.getRegisterSession();
		
		TeamMember tm = tle.getTeamMember();
		if (tm == null) return "team member is required";
		
		
		double d, d2;
		String invalid = null;
		
		switch (tle.getType()) {
		case TillLedgerEntry.TYPE_Unknown:
			invalid = "type is unknown";
			break;
		case TillLedgerEntry.TYPE_CashPurchase:
			if (rs == null) invalid = "Register Session is required";
			else if (tle.getInvoicePayment() == null) invalid = "cash purchase requires an invoice payment";
			break;
		case TillLedgerEntry.TYPE_CashRefund:
			if (rs == null) invalid = "Register Session is required";
			else if (tle.getRefundPayment() == null) invalid = "cash refund requires a refund payment";
			break;

		case TillLedgerEntry.TYPE_CheckPurchase:
			if (rs == null) invalid = "Register Session is required";
			else if (tle.getInvoicePayment() == null) invalid = "check purchase requires an invoice payment";
			else if (tle.getInvoicePayment().getInvoicePaymentCheck() == null) invalid = "check purchase requires an invoice payment with payment check included";
			break;
			
		case TillLedgerEntry.TYPE_CheckRefund:
			if (rs == null) invalid = "Register Session is required";
			else if (tle.getRefundPayment() == null) invalid = "cash refund requires a refund payment";
			else if (tle.getRefundPayment().getInvoicePayment().getInvoicePaymentCheck() == null) invalid = "check refund requires an invoice payment with payment check included";
			else {
				// check needs to be in Till
				InvoicePaymentCheck ipc = tle.getRefundPayment().getInvoicePayment().getInvoicePaymentCheck();
				if (!till.getInvoicePaymentChecks().contains(ipc)) {
					invalid = "check is no longer in this Till";
				}
				else if ( !OACompare.isEqual( ipc.getInvoicePayment().getAmount(), tle.getCheckAmount(), 2)) {
					invalid = "refund amount must match check amount";
				}
			
				//qqqqqqqq more coverage needed
				// qqqqq else check has to be cleared by the Bank and then use cash refund.
			}
			break;
			
		case TillLedgerEntry.TYPE_CashFromSafe:
			if (tle.getStoreSafeLedgerEntry() == null) invalid = "cash from safe needs to a safe ledger entry"; 
		    break;
		case TillLedgerEntry.TYPE_CashToSafe:
			d = tle.getTotalCashAmount();
			d2 = till.getCashAmount();
			if (OACompare.compare(d, d2, 2) > 0) invalid = "cash to safe can not exceed cash amount in till";
			break;
		case TillLedgerEntry.TYPE_ChecksToSafe:
			if (tle.getInvoicePaymentChecks().size() == 0) invalid = "no checks have been selected to transfer to safe";
			break;
		case TillLedgerEntry.TYPE_ChecksFromSafe:
			StoreSafeLedgerEntry ssle = tle.getStoreSafeLedgerEntry();
			if (ssle == null) invalid = "Safe ledger entry is required";
			break;
		case TillLedgerEntry.TYPE_MoveTillToSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToRegister:
			break;
		case TillLedgerEntry.TYPE_Audit:
			d = tle.getTotalCashAmount();
			d2 = till.getCashAmount();
			if (!OACompare.isEqual(d, d2, 2)) invalid = "total cash must be same as the till's cash amount";
			else {
				if (tle.getCheckCount() != till.getInvoicePaymentChecks().size()) invalid = "check count needs to be same as the number of checks in the till";
				else {
					if (!OACompare.isEqual(till.getTotalCheckAmount(), tle.getCheckAmount(), 2)) invalid = "check amount must equal total of the checks in till";
				}
			}
			break;
		case TillLedgerEntry.TYPE_Variance:
			break;
			
		case TillLedgerEntry.TYPE_Validation:
			TillLedgerEntry tleAudit = null;
			for (TillLedgerEntry tlex : till.getTillLedgerEntries()) {
				if (tlex.getType() == TillLedgerEntry.TYPE_Audit) tleAudit = tlex;
			}
			if (tleAudit == null) {
				invalid = "cant do a validation without a past Audit";
			}
			break;
		case TillLedgerEntry.TYPE_ExchangeCash:
			break;
		}
		
		return invalid;
	}
	
	public static void post(final TillLedgerEntry tle) throws Exception {
		if (tle == null) return;
		if (tle.getPosted() != null) return; 
		if (!tle.canPost()) return;
		
		Till till = tle.getTill();
		if (till == null) return;
		
		double d, d2; 
		InvoicePayment invoicePayment;
		
		boolean bValid = true;
		switch (tle.getType()) {
		case TillLedgerEntry.TYPE_Unknown: 
			bValid = false;
			break;
		case TillLedgerEntry.TYPE_CashPurchase: 
			invoicePayment = tle.getInvoicePayment();
			if (invoicePayment == null) {
				bValid = false;
				break;
			}
			d = till.getCashAmount();
			d2 = tle.getLooseCashAmount();
			till.setCashAmount(OAMath.add(d, d2, 2));
			break;
		case TillLedgerEntry.TYPE_CashRefund: 
			d = till.getCashAmount();
			d2 = tle.getLooseCashAmount();
			till.setCashAmount(OAMath.subtract(d, d2, 2));
			break;
		case TillLedgerEntry.TYPE_CheckPurchase:
			invoicePayment = tle.getInvoicePayment();
			if (invoicePayment == null || invoicePayment.getInvoicePaymentCheck() == null) {
				bValid = false;
				break;
			}
			till.getInvoicePaymentChecks().add(invoicePayment.getInvoicePaymentCheck());
			break;
		
		case TillLedgerEntry.TYPE_CheckRefund:
			InvoicePaymentCheck ipc = tle.getRefundPayment().getInvoicePayment().getInvoicePaymentCheck();
			till.getInvoicePaymentChecks().remove(ipc);
			ipc.setLocation(ipc.LOCATION_ReturnedToCustomer);
			break;
			
		case TillLedgerEntry.TYPE_CashFromSafe:
			d = till.getCashAmount();
			d2 = tle.getTotalCashAmount();
			till.setCashAmount(OAMath.add(d, d2, 2));
			break;
			
		case TillLedgerEntry.TYPE_CashToSafe: 
			d = till.getCashAmount();
			d2 = tle.getTotalCashAmount();
			till.setCashAmount(OAMath.subtract(d, d2, 2));
			
			StoreSafeLedgerEntry ssle = new StoreSafeLedgerEntry();
			ssle.setTeamMember(tle.getTeamMember());
			ssle.setType(StoreSafeLedgerEntry.TYPE_TillCashToSafe);
			ssle.setLooseCashAmount(d2);
			ssle.setTillLedgerEntry(tle);
			ssle.setStoreSafe(till.getStore().getStoreSafe());
			ssle.post(); 
			break;

			
//qqqqqqqqqqqqqqqqq need a way to only allow legerEntry.type to be created from a specific use case / flow qqqqqqqqqqqqqqqqqqqqqq
			// ex: checksToSafe must be created from Till (not Safe)
		
		case TillLedgerEntry.TYPE_ChecksToSafe:
			ssle = new StoreSafeLedgerEntry();
			ssle.setTeamMember(tle.getTeamMember());
			ssle.setType(StoreSafeLedgerEntry.TYPE_TillChecksToSafe);
			ssle.setTillLedgerEntry(tle);
			
			for (InvoicePaymentCheck ipcx : tle.getInvoicePaymentChecks()) {
				till.getInvoicePaymentChecks().remove(ipcx);
				ssle.getInvoicePaymentChecks().add(ipcx);
			}
			
			ssle.setStoreSafe(till.getStore().getStoreSafe());
			ssle.post();
			
			break;

		case TillLedgerEntry.TYPE_ChecksFromSafe:
			for (InvoicePaymentCheck ipcx : tle.getInvoicePaymentChecks()) {
				till.getInvoicePaymentChecks().add(ipcx);
				ipcx.setLocation(InvoicePaymentCheck.LOCATION_Till);
			}
			break;
			
		case TillLedgerEntry.TYPE_MoveTillToSafe: 
			//qqqqqq
			break;
		case TillLedgerEntry.TYPE_MoveTillToRegister:
			//qqqqqq
			break;
		case TillLedgerEntry.TYPE_Audit:
			break;
		case TillLedgerEntry.TYPE_Variance:
			d = till.getCashAmount();
			d2 = tle.getTotalCashAmount();
			till.setCashAmount(OAMath.add(d, d2, 2));
			break;
		case TillLedgerEntry.TYPE_Validation:

			TillLedgerEntry tleAudit = null;
			for (TillLedgerEntry tlex : till.getTillLedgerEntries()) {
				if (tlex.getType() == TillLedgerEntry.TYPE_Audit) tleAudit = tlex;
			}
			if (tleAudit == null) {
				bValid = false;
				break;
			}
			
			boolean bFound = false;
			double dCash = 0.0;
			int cntChecks = 0;
			double dChecks = 0.0;
			
			for (TillLedgerEntry tlex : till.getTillLedgerEntries()) {
				if (tlex == tle) break;
				
				if (!bFound) {
					if (tlex == tleAudit) {
						bFound = true;
						dCash = tlex.getTotalCashAmount();
						cntChecks = tlex.getCheckCount();
						dChecks = tlex.getCheckAmount();
					}
					continue;
				}
				
				switch (tlex.getType()) {
				case TillLedgerEntry.TYPE_Unknown:
					break;
				case TillLedgerEntry.TYPE_CashPurchase:
					dCash = OAMath.add(dCash, tlex.getTotalCashAmount(), 2);
					break;
				case TillLedgerEntry.TYPE_CashRefund:
					dCash = OAMath.subtract(dCash, tlex.getTotalCashAmount(), 2);
					break;
				case TillLedgerEntry.TYPE_CheckPurchase:
					cntChecks++;
					dChecks = OAMath.add(dChecks, tlex.getCalcTotalCheckAmount(), 2);
					break;
				case TillLedgerEntry.TYPE_CheckRefund:
					cntChecks--;
					dChecks = OAMath.subtract(dChecks, tlex.getCalcTotalCheckAmount(), 2);
					break;
				case TillLedgerEntry.TYPE_CashFromSafe:
					dCash = OAMath.add(dCash, tlex.getTotalCashAmount(), 2);
				    break;
				case TillLedgerEntry.TYPE_CashToSafe:
					dCash = OAMath.subtract(dCash, tlex.getTotalCashAmount(), 2);
					break;
				case TillLedgerEntry.TYPE_ChecksToSafe:
					cntChecks -= tlex.getCalcCheckCount();
					dChecks = OAMath.subtract(dChecks, tlex.getCalcTotalCheckAmount(), 2);
					break;
				case TillLedgerEntry.TYPE_ChecksFromSafe:
					cntChecks += tlex.getCalcCheckCount();
					dChecks = OAMath.add(dChecks, tlex.getCalcTotalCheckAmount(), 2);
					break;
				case TillLedgerEntry.TYPE_MoveTillToSafe:
					break;
				case TillLedgerEntry.TYPE_MoveTillToRegister:
				case TillLedgerEntry.TYPE_Audit:
				case TillLedgerEntry.TYPE_Variance:
				case TillLedgerEntry.TYPE_Validation:
				case TillLedgerEntry.TYPE_ExchangeCash:
					break;
				}
			}

			tle.setLooseCashAmount(dCash);
			tle.setCheckCount(cntChecks);
			tle.setCheckAmount(dChecks);
			
			if (OACompare.compare(dCash, till.getCashAmount(), 2) != 0) bValid = false;
			else if (OACompare.compare(dChecks, till.getTotalCheckAmount(), 2) != 0) bValid = false;
			else if (till.getInvoicePaymentChecks().size() != cntChecks) bValid = false;
			
			break;
			
		case TillLedgerEntry.TYPE_ExchangeCash:
			break;
		}
		
		if (bValid) tle.setPosted(new OADateTime());
	}



	public static boolean getCalcEnabledInvoicePayment(TillLedgerEntry tle) {
		if (tle == null) return false;

		boolean b = false;
		
		switch (tle.getType()) {
		case TillLedgerEntry.TYPE_Unknown:
			break;
		case TillLedgerEntry.TYPE_CashPurchase:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CashRefund:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CheckPurchase:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CheckRefund:
			b = true;
			break;
		case TillLedgerEntry.TYPE_CashFromSafe:
		    break;
		case TillLedgerEntry.TYPE_CashToSafe:
			break;
		case TillLedgerEntry.TYPE_ChecksToSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToSafe:
			break;
		case TillLedgerEntry.TYPE_MoveTillToRegister:
			break;
		case TillLedgerEntry.TYPE_Audit:
			break;
		case TillLedgerEntry.TYPE_Variance:
			break;
		case TillLedgerEntry.TYPE_Validation:
			break;
		case TillLedgerEntry.TYPE_ExchangeCash:
			break;
		}
		return b;
	}



	
	
/*
	switch (tle.getType()) {
	case TillLedgerEntry.TYPE_Unknown:
		break;
	case TillLedgerEntry.TYPE_CashPurchase: // InvoicePayment.apply() ... TESTED1
		break;
	case TillLedgerEntry.TYPE_CashRefund: // RefundPayment.apply()  ... TESTED1
		break;
	case TillLedgerEntry.TYPE_CheckPurchase: // InvoicePayment.apply()  ... TESTED1
		break;
	case TillLedgerEntry.TYPE_CheckRefund: // RefundPayment.apply()  ... TESTED1
		break;
	case TillLedgerEntry.TYPE_CashFromSafe: // StoreSafeLedgerEntry.type=SafeCashToTill  ... TESTED1
	    break;
	case TillLedgerEntry.TYPE_CashToSafe: // RegisterSession.type=CashToSafe  ... TESTED1
		break;
	case TillLedgerEntry.TYPE_ChecksToSafe: // RegisterSession.type=ChecksToSafe  ... TESTED1
		break;
	case TillLedgerEntry.TYPE_ChecksFromSafe:
		break;
	case TillLedgerEntry.TYPE_MoveTillToSafe: // Till.setRegister(null)  ... TESTED1
		break;
	case TillLedgerEntry.TYPE_MoveTillToRegister: // Till.setRegister(reg)  ... TESTED1
		break;
		
	case TillLedgerEntry.TYPE_Audit: // RegisterSession.type=Audit  ... TESTED1
		break;
	case TillLedgerEntry.TYPE_Variance: // RegisterSession.type=Variance  ... TESTED1
		break;
	}
	case TillLedgerEntry.TYPE_Validatiion: // TillLedgerEntry.type=Validation  ... TESTED1
		break;
	case TillLedgerEntry.TYPE_ExchangeCash:
		break;
	}
*/	


/*	
	
	
*/	
	
}





