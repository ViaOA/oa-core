package com.test.pos.delegate.oa;

import com.test.pos.model.oa.*;

public class RegisterDelegate {

	public static StoreSafeLedgerEntry transferCashToSafe(Till till) {
		// TODO Auto-generated method stub
		if (till == null) return null;
		
		StoreSafeLedgerEntry sle = new  StoreSafeLedgerEntry();
		sle.setType(StoreSafeLedgerEntry.TYPE_TillCashToSafe);
		
		//dont add, let calling code (or wizard) do it:
		// register.getSafeTransactions().add(st);

		
		
		// qqqqqqq ??? dont have it added to Hub until completed!=null
		
		// qqqqqq JFC generator needs to create command that will then popup wizard 
		
		return sle;
	}

}
