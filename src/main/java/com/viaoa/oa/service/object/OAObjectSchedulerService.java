package com.viaoa.oa.service.object;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import com.viaoa.datetime.OADate;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.*;
import com.viaoa.path.OAPath;
import com.viaoa.schedule.OAScheduler;

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
    public <T extends OAObject> OAScheduler<T> getScheduler(T objThis, String property, OADate date) {
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
     *       dot-notation {@link OAPath}.</li>
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
    public <T extends OAObject> OAScheduler<T> getScheduler(T objThis, String property, OAObject objSearch, OADate date) {
        if (objThis == null || OAString.isEmpty(property)) return null;

        OAObjectInfo oi = callInfoGetObjectInfo(objThis);
        if (oi == null) return null;
        OALinkInfo li = oi.getLinkInfo(property);
        if (li == null) {
            if (property.indexOf(".") < 0) return null;
            OAPath pp = new OAPath(objThis.getClass(), property);
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
    public <T extends OAObject> void invokeCallback(OAScheduler<T> scheduler, T objThis, String property) {
        if (scheduler == null || objThis == null || OAString.isEmpty(property)) return;

        OAObjectInfo oi = callInfoGetObjectInfo(objThis);
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

	public abstract OAObjectInfo callInfoGetObjectInfo(OAObject obj); 

}
