package com.test.pos.delegate.oa;

import com.test.pos.model.oa.*;
import com.viaoa.converter.OAConv;
import com.viaoa.math.OAMath;
import com.viaoa.runtime.OARuntime;

public class LineItemDelegate {

	public static double getTotalItemAmount(final LineItem lineItem) {
		if (lineItem == null) return 0.0;

		double d = lineItem.getPriceEach();
		int qty = lineItem.getQuantity();
		if (qty <= 0) return 0.0;

		d = OAMath.multiply(d, qty, 2);
		return d;
	}

	public static double getTotalTaxAmount(final LineItem lineItem) {
		if (lineItem == null) return 0.0;
		
		final double dPriceEa = lineItem.getPriceEach();

		double tax = 0.0;
		for (LineItemTax lit : lineItem.getLineItemTaxes()) {
			double dTaxRate = OAMath.divide(lit.getTaxPercent(), 100, 6);
			
			double d = OAMath.multiply(dPriceEa, dTaxRate, 2);
			
			d = OAMath.multiply(d, lineItem.getQuantity(), 2);
			
			tax = OAMath.add(tax, d, 2);
		}
		return tax;
	}


	public static void afterSetProduct(final LineItem lineItem) {
		if (lineItem == null) return;
		if (!lineItem.startServerOnly()) return;
		try {
			_afterSetProduct(lineItem);
		}
		finally {
			lineItem.endServerOnly();
		}
	}

	private static void _afterSetProduct(final LineItem lineItem) {
		Product product = lineItem.getProduct();
		double dSaleEach = 0.0;
		if (product != null) {
			PriceBookEntry pbe = product.getCurrentPriceBookEntry();
			if (pbe != null) dSaleEach = pbe.getSalePrice();
		}
		lineItem.setPriceEach(dSaleEach);
		
		for (LineItemTax lit : lineItem.getLineItemTaxes()) {
			VertexTaxCodeRate tcr = lit.getVertexTaxCodeRate();
			double d = 0.0;
			if (tcr != null) d = tcr.getTaxPercent(); 
			lit.setTaxPercent(d);
		}
	}
}
