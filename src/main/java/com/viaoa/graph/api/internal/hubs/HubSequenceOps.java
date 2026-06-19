package com.viaoa.graph.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.auto.HubAutoSequence;

public interface HubSequenceOps {

 	public HubAutoSequence getAutoSequence(Hub<?> hub);	
	public void setAutoSequence(Hub<?> hub, String property, int startNumber, boolean bKeepSeq);
	public void resequence(Hub<?> hub);

}
