package com.test.pos.delegate.oa;

import com.test.pos.delegate.ModelDelegate;
import com.test.pos.model.oa.*;
import com.viaoa.converter.OAConv;
import com.viaoa.datetime.OADateTime;

public class InvoicePaymentDelegate {

	public static boolean getTypeIsCash(InvoicePayment invoicePayment) {
		if (invoicePayment == null) return false;
		return invoicePayment.getType() == InvoicePayment.TYPE_cash;
	}

	public static boolean getTypeIsCheck(InvoicePayment invoicePayment) {
		if (invoicePayment == null) return false;
		return invoicePayment.getType() == InvoicePayment.TYPE_check;
	}

	public static void apply(InvoicePayment invoicePayment) throws Exception {
		if (invoicePayment == null) return;
		if (invoicePayment.getApplied() != null) return;

		Invoice inv = invoicePayment.getInvoice();
		
		RegisterSession sess = inv.getRegisterSession();
		Register reg = sess.getRegister();
		Till till = reg.getTill();
		
		TillLedgerEntry tle = null;
		if (invoicePayment.getTypeIsCash()) {
			tle = new TillLedgerEntry();
			tle.setType(TillLedgerEntry.TYPE_CashPurchase);
			tle.setLooseCashAmount(invoicePayment.getAmount());
		}
		else if (invoicePayment.getTypeIsCheck()) {
			InvoicePaymentCheck ipc = invoicePayment.getInvoicePaymentCheck();
			if (ipc == null) throw new RuntimeException("Requires Invoice Payment Check"); //qqqqqq add to isValid

			tle = new TillLedgerEntry();
			tle.setType(TillLedgerEntry.TYPE_CheckPurchase);
			
			ipc.setLocation(InvoicePaymentCheck.LOCATION_Till);
		}

		//qqqqqqq other types of payment need to setApplied(now)
		
		if (tle != null) {
			TeamMember tm = ModelDelegate.getCurrentTeamMember();
			tle.setTeamMember(tm);
			
			tle.setInvoicePayment(invoicePayment);
			tle.setTill(till);
			tle.setRegisterSession(sess);
			
			tle.post();

			invoicePayment.setApplied(new OADateTime());
		}
		
	}

}
