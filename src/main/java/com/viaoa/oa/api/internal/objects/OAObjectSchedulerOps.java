package com.viaoa.oa.api.internal.objects;

import com.viaoa.datetime.OADate;
import com.viaoa.object.OAObject;
import com.viaoa.schedule.OAScheduler;

/**
 * Internal access to scheduler metadata for date-based OAObject properties.
 */
public interface OAObjectSchedulerOps {

	/**
	 * Returns the scheduler for a date-based object property.
	 *
	 * @param oaObj the target object
	 * @param property the scheduled property name
	 * @param date the date to schedule
	 * @return the scheduler
	 */
	public OAScheduler getScheduler(OAObject oaObj, String property, OADate date);

}
