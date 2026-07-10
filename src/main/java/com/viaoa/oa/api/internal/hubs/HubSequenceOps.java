package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.auto.HubAutoSequence;

/**
 * Internal automatic sequence-number operations for ordered Hub contents.
 */
public interface HubSequenceOps {

	/**
	 * Returns the auto-sequence controller for a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @return the auto-sequence controller, or {@code null}
	 */
 	public HubAutoSequence getAutoSequence(Hub<?> hub);	
	/**
	 * Configures automatic sequencing for a Hub property.
	 *
	 * @param hub the Hub to sequence
	 * @param property the sequence property name
	 * @param startNumber the first sequence number
	 * @param bKeepSeq {@code true} to keep sequence values maintained
	 */
	public void setAutoSequence(Hub<?> hub, String property, int startNumber, boolean bKeepSeq);
	/**
	 * Recomputes sequence values for a Hub.
	 *
	 * @param hub the Hub to resequence
	 */
	public void resequence(Hub<?> hub);

}
