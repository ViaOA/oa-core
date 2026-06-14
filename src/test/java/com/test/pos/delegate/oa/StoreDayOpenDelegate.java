package com.test.pos.delegate.oa;

import com.test.pos.model.oa.*;

public class StoreDayOpenDelegate {

	public static void createStoreSafeAudit(StoreDayOpen storeDayOpen) {
		if (storeDayOpen == null) return;
		
/*qqqqqqqqq		
		if (storeDayOpen.getStoreSafeAudit() != null) return;
		
		final StoreSafe storeSafe = storeDayOpen.getStoreSchedule().getStore().getStoreSafe();
		
		StoreSafeLedgerEntry ledgerEntry = new StoreSafeLedgerEntry();
		ledgerEntry.setTypeEnum(StoreSafeLedgerEntry.Type.StoreOpenAudit);
		
		StoreSafeAudit audit = new StoreSafeAudit();
		ledgerEntry.setStoreSafeAudit(audit);

		storeSafe.getStoreSafeLedgerEntries().add(ledgerEntry);

		storeDayOpen.setStoreSafeAudit(audit);
qqqqqqqqq*/		
	}

}
