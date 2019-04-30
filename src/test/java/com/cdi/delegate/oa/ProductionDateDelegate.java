package com.cdi.delegate.oa;

import java.util.*;
import java.util.logging.Logger;

import com.cdi.model.oa.*;
import com.viaoa.hub.Hub;
import com.viaoa.sync.OASync;
import com.viaoa.util.*;

public class ProductionDateDelegate {
	private static Logger LOG = Logger.getLogger(ProductionDateDelegate.class.getName());
	
	private static final int[][] HOLIDAYS = new int[][] { new int[] {1,1}, {7,4}, {12,25}}; 

/*	
	public static void onSetDate(ProductionDate pd, OADate date) {
	    
		if (pd == null || date == null) return;
		if (!OASync.isServer()) return; // only process on server, and send events to clients (even if this is OAThreadClient)
		try {
		    OASync.sendMessages(true);
    		int m = date.getMonth()+1;
    		int d = date.getDay();
    
    		int type = ProductionDate.TYPE_REGULAR;
    		for (int i=0; i<HOLIDAYS.length; i++) {
    			int[] ints = HOLIDAYS[i];
    			if (ints[0] == m && ints[1] == d) {
    				type = ProductionDate.TYPE_HOLIDAY;
    				break;
    			}
    		}
    		if (type == ProductionDate.TYPE_REGULAR) {
    			int dow = date.getDayOfWeek();
    			if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) type = ProductionDate.TYPE_WEEKEND;
    		}		
    		pd.setType(type);
		}
		finally {
            OASync.sendMessages(false);
		}
	}
	
	public static int getProductionQuantity(ProductionDate pd, Mold mold, WOItem woiSkip) {
	    if (pd == null) return 0;
		int qty = 0;
		Hub h = pd.getWOItemProductions();
		for (int i=0; ;i++) {
			WOItemProduction woip = (WOItemProduction) h.getAt(i);
			if (woip == null) break;
			if (woip.getWOItem() == woiSkip) continue;
			Mold moldx = WOItemProductionDelegate.getMold(woip);
			if (moldx == mold) qty += woip.getQuantity();
		}
		return qty;
	}
*/	
}
