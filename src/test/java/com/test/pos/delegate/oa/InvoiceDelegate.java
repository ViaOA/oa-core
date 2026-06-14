package com.test.pos.delegate.oa;

import com.test.pos.model.oa.*;
import com.viaoa.converter.OAConv;
import com.viaoa.math.OAMath;

public class InvoiceDelegate {

	public static void updateWithNetPriceCaclulator(Invoice invoice) {
		// TODO Auto-generated method stub
		
	}

	public static boolean getCanBeCompleted(Invoice invoice) {
		// TODO Auto-generated method stub

/*qqqqqqq		
	        Hub<InvoicePayment> hubInvoicePayments = this.getInvoicePayments();
	        for (InvoicePayment invoicePayment : hubInvoicePayments) {
	            amount = invoicePayment.getAmount();
	        }
	    
	        Hub<InvoiceBasket> hubInvoiceBaskets = this.getInvoiceBaskets();
	        for (InvoiceBasket invoiceBasket : hubInvoiceBaskets) {
	            Hub<LineItem> hubLineItems = invoiceBasket.getLineItems();
	            for (LineItem lineItem : hubLineItems) {
	                quantity = lineItem.getQuantity();
	            }
	        }
	    
	        Hub<InvoiceBasket> hubInvoiceBaskets = this.getInvoiceBaskets();
	        for (InvoiceBasket invoiceBasket : hubInvoiceBaskets) {
	            Hub<LineItem> hubLineItems = invoiceBasket.getLineItems();
	            for (LineItem lineItem : hubLineItems) {
	                Item item = lineItem.getItem();
	                if (item != null) {
	                }
	            }
	        }
*/		
		return false;
	}

	public static double getTotalItemAmount(Invoice invoice) {
		if (invoice == null) return 0.0;
		double d = 0.0;
		for (InvoiceBasket ib : invoice.getInvoiceBaskets()) {
			for (LineItem li : ib.getLineItems()) {
				d = OAMath.add(d,  li.getTotalItemAmount());
			}
		}
		return d;
	}

	public static double getTotalDiscountAmount(Invoice invoice) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static double getTotalTaxAmount(Invoice invoice) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static double getTotalAmountDue(Invoice invoice) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static double getTotalPaymentAmount(Invoice invoice) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static double getRemainingBalanceAmount(Invoice invoice) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static double getTotalRefundAmount(Invoice invoice) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static boolean getIsPaidInFull(Invoice invoice) {
		// TODO Auto-generated method stub
		return false;
	}

	public static void completeSale(Invoice invoice) {
		// TODO Auto-generated method stub
		//qqqqqqqqqqqqqqqqqqq
	}

}
