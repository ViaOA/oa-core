package com.viaoa.graph.api.internal.objects;

import com.viaoa.datetime.OADate;
import com.viaoa.object.OAObject;
import com.viaoa.schedule.OAScheduler;

public interface OAObjectSchedulerOps {

	public OAScheduler getScheduler(OAObject oaObj, String property, OADate date);

}
