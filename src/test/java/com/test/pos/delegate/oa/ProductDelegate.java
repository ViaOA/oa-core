package com.test.pos.delegate.oa;

import com.test.pos.model.oa.PriceBookEntry;
import com.test.pos.model.oa.Product;

public class ProductDelegate {

	public static PriceBookEntry getCurrentPriceBookEntry(Product product) {
		// TODO Auto-generated method stub
		
		PriceBookEntry pbeFound = null;
		for (PriceBookEntry pbe : product.getItem().getPriceBookEntries()) {
			return pbe; //qqqqqqqqqqqqq
		}
		
		return null;
	}

}
