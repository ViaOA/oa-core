package com.viaoa.graph.service.object;


import java.lang.reflect.Method;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.object.*;
import com.viaoa.scheduler.OAScheduler;
import com.viaoa.util.OADate;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

public abstract class OAObjectSchedulerService {
	private final Logger LOG = Logger.getLogger(OAObjectSchedulerService.class.getName());


	public OAObjectSchedulerService() {
	}

	/**
	 * Delegates to {@link #getScheduler(OAObject, String, OAObject, OADate)} using a
	 * {@code null} search object. This provides a convenience overload for retrieving
	 * a scheduler for the specified property and date without supplying a search
	 * filter object.
	 *
	 * @param objThis the object whose scheduler callback method should be invoked
	 * @param property the name of the link property containing the scheduler callback
	 * @param date the date used to initialize the scheduler range
	 * @return the created scheduler, or {@code null} if required arguments are missing
	 */
    public OAScheduler getScheduler(OAObject objThis, String property, OADate date) {
        return getScheduler(objThis, property, null, date);
    }
    
    /**
     * Retrieves an {@link OAScheduler} for a linked property by invoking the scheduler
     * callback method defined in that property's {@link OALinkInfo}. The callback method,
     * if present, is invoked on {@code objThis} with a newly constructed scheduler
     * covering the supplied date.
     *
     * <p>This method performs several validations:</p>
     * <ul>
     *   <li>Ensures {@code objThis} and {@code property} are not null or empty.</li>
     *   <li>Locates the corresponding {@link OALinkInfo} either directly or via a
     *       dot-notation {@link OAPropertyPath}.</li>
     *   <li>Retrieves the scheduler callback {@link Method}, if defined.</li>
     * </ul>
     *
     * <p>If a callback method is found, it is invoked with a scheduler whose start and
     * end dates are both set to the supplied {@code date}. Exceptions during callback
     * execution are wrapped in a {@link RuntimeException}.</p>
     *
     * @param objThis    the object used to locate and invoke the scheduler callback
     * @param property   the link property or property path identifying the callback
     * @param objSearch  an optional object passed to the scheduler constructor
     * @param date       the date used to initialize the scheduler instance
     * @return the populated scheduler, or {@code null} if any required metadata is not found
     */
    public OAScheduler getScheduler(OAObject objThis, String property, OAObject objSearch, OADate date) {
        if (objThis == null || OAString.isEmpty(property)) return null;

        OAObjectInfo oi = getObjectInfo(objThis);
        if (oi == null) return null;
        OALinkInfo li = oi.getLinkInfo(property);
        if (li == null) {
            if (property.indexOf(".") < 0) return null;
            OAPropertyPath pp = new OAPropertyPath(objThis.getClass(), property);
            OALinkInfo[] lis = pp.getLinkInfos();
            if (lis == null || lis.length == 0) return null;
            li = lis[0];
        }
        
        Method method = li.getSchedulerMethod();
        if (method == null) return null;
        
        OAScheduler scheduler = new OAScheduler(objSearch, date, date);
        
        try {
            method.invoke(objThis, new Object[] {scheduler});
        }
        catch (Exception e) {
            throw new RuntimeException("exception while invoking scheduler callback method="+method+", for object="+objThis, e);
        }
        return scheduler;
    }
    
    /**
     * Invokes the scheduler callback method defined in the {@link OALinkInfo} for the
     * specified property, passing in the supplied {@link OAScheduler}. If any required
     * argument is missing or if the property lacks a scheduler callback, this method
     * returns silently.
     *
     * <p>The method performs the following steps:</p>
     * <ul>
     *   <li>Validates arguments and retrieves {@link OAObjectInfo} for the object.</li>
     *   <li>Locates the {@link OALinkInfo} for the property.</li>
     *   <li>Retrieves the scheduler callback {@link Method}, if defined.</li>
     *   <li>Invokes the callback, wrapping any exceptions in a {@link RuntimeException}.</li>
     * </ul>
     *
     * @param scheduler the scheduler instance passed to the callback method
     * @param objThis   the object whose callback method should be invoked
     * @param property  the property identifying which scheduler callback to execute
     */
    public void invokeCallback(OAScheduler scheduler, OAObject objThis, String property) {
        if (scheduler == null || objThis == null || OAString.isEmpty(property)) return;

        OAObjectInfo oi = getObjectInfo(objThis);
        if (oi == null) return;
        OALinkInfo li = oi.getLinkInfo(property);
        if (li == null) return;
        
        Method method = li.getSchedulerMethod();
        if (method == null) return;
        
        try {
            method.invoke(objThis, new Object[] {scheduler});
        }
        catch (Exception e) {
            throw new RuntimeException("exception while invoking scheduler callback method="+method+", for object="+objThis, e);
        }
    }

    
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getObjectInfo")
	public abstract OAObjectInfo getObjectInfo(OAObject obj); 

}
