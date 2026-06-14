package com.test.pos.delegate.oa;

import com.test.pos.delegate.ModelDelegate;
import com.test.pos.model.oa.*;
import com.viaoa.datetime.OADateTime;

public class RefundPaymentDelegate {

	public static void apply(RefundPayment refundPayment) throws Exception {
		if (refundPayment == null) return;
		if (refundPayment.getApplied() != null) return;
		
		InvoicePayment invoicePayment = refundPayment.getInvoicePayment();
		
		RefundInvoice refundInvoice = refundPayment.getRefundInvoice();
		Refund refund = refundInvoice.getRefund();
		
		RegisterSession sess = refund.getRegisterSession();
		Register reg = sess.getRegister();
		Till till = reg.getTill();

		
		TillLedgerEntry tle = null;
		if (invoicePayment.getTypeIsCash()) {
			tle = new TillLedgerEntry();
			tle.setType(TillLedgerEntry.TYPE_CashRefund);

			tle.setLooseCashAmount(refundPayment.getAmount());
		}
		else if (invoicePayment.getTypeIsCheck()) {
//qqqqqqqqqqqqq check must be in Till or Safe to get it back
			// if it's cleared the bank, then give cash
			// otherwise need to wait

			tle = new TillLedgerEntry();
			tle.setType(TillLedgerEntry.TYPE_CheckRefund);
			tle.setCheckCount(1);
			tle.setCheckAmount(refundPayment.getAmount());
			
			
		
		}
//qqqqqqqq others		
		
		if (tle != null) {
			TeamMember tm = ModelDelegate.getCurrentTeamMember();
			tle.setTeamMember(tm);
			
			tle.setRefundPayment(refundPayment);
			tle.setTill(till);
			tle.setRegisterSession(sess);
			
			tle.post();

			refundPayment.setApplied(new OADateTime());
		}
		
		
	}

}
