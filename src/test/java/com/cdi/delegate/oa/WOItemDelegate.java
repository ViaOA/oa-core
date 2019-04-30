package com.cdi.delegate.oa;

import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.cdi.model.oa.*;
import com.viaoa.util.*;


//qqqq need to have some of these only ran on server

public class WOItemDelegate {

	private static Logger LOG = Logger.getLogger(WOItemDelegate.class.getName());	
    
	public static Mold getMold(WOItem woi) {
		LOG.finer("called");					
		if (woi == null) return null;
		OrderItem oi = woi.getOrderItem();
		if (oi == null) return null;
		Item item = oi.getItem();
		if (item == null) return null;
		Mold mold = item.getMold();
		return mold;
	}

}
    
    
    

