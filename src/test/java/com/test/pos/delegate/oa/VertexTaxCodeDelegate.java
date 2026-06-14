package com.test.pos.delegate.oa;

import com.test.pos.model.oa.VertexTaxCode;
import com.test.pos.model.oa.VertexTaxCodeRate;
import com.viaoa.datetime.OADate;

public class VertexTaxCodeDelegate {

	public static VertexTaxCodeRate getCurrentVertexTaxCodeRate(final VertexTaxCode vertexTaxCode) {
		if (vertexTaxCode == null) return null;

		OADate date = new OADate(); 
		
		VertexTaxCodeRate tcrFound = null;
		
		
		for (VertexTaxCodeRate tcr : vertexTaxCode.getVertexTaxCodeRates()) {
			OADate bd = tcr.getBeginDate();
			OADate ed = tcr.getEndDate();
			
			if (bd == null) {
				if (ed == null) {
					if (tcrFound == null) tcrFound = tcr;
					continue;
				}
			}
			else if (ed == null) {
				if (ed.compare(date) >= 0) tcrFound = tcr;
			}
			else if (date.betweenOrEqual(bd, ed)) {
				tcrFound = tcr;
				break;
			}
		}
		
		return tcrFound;
	}

}
