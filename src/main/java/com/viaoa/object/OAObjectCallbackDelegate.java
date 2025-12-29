/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.object;

import java.util.logging.Logger;

import javax.swing.JLabel;

import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubChangeListener;
import com.viaoa.hub.HubEvent;
import com.viaoa.runtime.OARuntime;

/**
 * Rule and permission engine for {@link OAObject} interactions.
 * <p>
 * This delegate evaluates whether a user action on an object or hub is
 * permitted, visible, enabled, confirmable, or requires further UI messaging.
 * It unifies rule sources from annotations, object-level callback methods,
 * Hub listeners, user context, and metadata from {@code OAObjectInfo}.
 *
 * <h3>Primary Responsibilities</h3>
 * <ul>
 *   <li>Resolve enable/visible rules for properties, links, and methods</li>
 *   <li>Check add/remove/delete/save permissions for Hub operations</li>
 *   <li>Provide UI data (tooltips, labels, formatting)</li>
 *   <li>Execute annotation-driven and {@code callback*} methods on objects</li>
 *   <li>Propagate errors, responses, or confirmation titles/messages</li>
 *   <li>Traverse owner/master hierarchy to enforce inherited rules</li>
 * </ul>
 *
 * <h3>Evaluation Sources</h3>
 * <ul>
 *   <li>Metadata from {@code @OAObjCallback} on class/props/links/methods</li>
 *   <li>Domain logic in {@code callback*}(..) methods</li>
 *   <li>Hub listeners associated with UI contexts</li>
 *   <li>Context-driven {@link com.viaoa.context.OAUserAccess} constraints</li>
 *   <li>Process/edit flags on objects and hubs</li>
 * </ul>
 *
 * <h3>Correctness Guarantees</h3>
 * <ul>
 *   <li>No side effects on object identity, lifecycle, or loading</li>
 *   <li>All rule failures optionally provide actionable UI feedback</li>
 *   <li>User/role checks are always respected before UI updates</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * Invoked automatically by:
 * <ul>
 *   <li>{@link OAObject} mutation (before-change routing)</li>
 *   <li>{@link Hub} add/remove operations</li>
 *   <li>UI controllers to dynamically update visual state</li>
 * </ul>
 *
 * Application code should not call this directly except to query allowed actions.
 *
 * @see OAObjectCallback
 * @see com.viaoa.annotation.OAObjCallback
 */
public class OAObjectCallbackDelegate {
	private static Logger LOG = Logger.getLogger(OAObjectCallbackDelegate.class.getName());

	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	/**
	 * Returns whether the specified property or link is visible by evaluating
	 * the associated {@link OAObjectCallback}. The visibility decision is
	 * determined by {@code getAllowVisibleObjectCallback(...)}.
	 *
	 * @param hub  the hub providing contextual visibility rules, or {@code null}
	 * @param obj  the target object, or {@code null}
	 * @param name the property or link name, or {@code null}
	 * @return {@code true} if visibility is allowed; otherwise {@code false}
	 */
	public static boolean getAllowVisible(Hub hub, OAObject obj, String name) {
		OAGraph g = getGraph(hub, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getAllowVisible(hub, obj, name);
	}

	
	/**
	 * Returns whether the specified property, link, or command is enabled
	 * by evaluating the associated {@link OAObjectCallback}. The enabled
	 * state is determined by {@code getAllowEnabledObjectCallback(...)}.
	 *
	 * @param checkType the bitmask of checking options
	 * @param hub       the hub providing contextual rules, or {@code null}
	 * @param obj       the target object, or {@code null}
	 * @param name      the property, link, or method name, or {@code null}
	 * @return {@code true} if the action is enabled; otherwise {@code false}
	 */
	public static boolean getAllowEnabled(int checkType, Hub hub, OAObject obj, String name) {
		OAGraph g = getGraph(hub, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getAllowEnabled(checkType, hub, obj, name);
	}

	/**
	 * Returns whether the specified object can be copied by evaluating the
	 * associated {@link OAObjectCallback}. If the object is {@code null},
	 * copying is not allowed.
	 *
	 * @param oaObj the object to evaluate
	 * @return {@code true} if copying is allowed; otherwise {@code false}
	 */
	public static boolean getAllowCopy(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getAllowCopy(oaObj);
	}

	/**
	 * Returns a copy of the specified object using the {@link OAObjectCallback}
	 * associated with copy behavior. If the callback does not provide a copy
	 * value and copying is allowed, {@code createCopy()} is invoked.
	 *
	 * @param oaObj the source object to copy
	 * @return the copied object, or {@code null} if copying is not allowed
	 */
	public static OAObject getCopy(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getCopy(oaObj);
	}
	/*
	public static void afterCopy(OAObject oaObj, OAObject oaObjCopy) {
	    if (oaObj == null || oaObjCopy == null) return;
	    getAfterCopyObjectCallback(oaObj, oaObjCopy);
	}
	*/

	/**
	 * Returns whether the specified property change is permitted by evaluating
	 * the associated {@link OAObjectCallback} for verification.
	 *
	 * @param checkType    the bitmask of checking options
	 * @param obj          the target object
	 * @param propertyName the property being changed
	 * @param oldValue     the previous value
	 * @param newValue     the proposed new value
	 * @return {@code true} if the property change is allowed; otherwise {@code false}
	 */
	public static boolean getVerifyPropertyChange(int checkType, OAObject obj, String propertyName, Object oldValue, Object newValue) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getVerifyPropertyChange(checkType, obj, propertyName, oldValue, newValue);
	}

	/**
	 * Returns whether the specified object can be added to the given hub by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub receiving the object
	 * @param obj       the object being added
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if the add operation is allowed; otherwise {@code false}
	 */
	public static boolean getAllowAdd(Hub hub, OAObject obj, int checkType) {
		OAGraph g = getGraph(hub, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getAllowAdd(hub, obj, checkType);
	}

	/**
	 * Returns whether adding the specified object to the given hub passes
	 * verification by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub receiving the object
	 * @param obj       the object being added
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public static boolean getVerifyAdd(Hub hub, OAObject obj, int checkType) {
		OAGraph g = getGraph(hub, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getVerifyAdd(hub, obj, checkType);
	}

	/**
	 * Returns whether the specified object can be removed from the given hub by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub from which the object may be removed
	 * @param obj       the object being removed
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if the remove operation is allowed; otherwise {@code false}
	 */
	public static boolean getAllowRemove(Hub hub, OAObject obj, int checkType) {
		OAGraph g = getGraph(hub, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getAllowRemove(hub, obj, checkType);
	}

	/**
	 * Returns whether removing the specified object from the given hub passes
	 * verification by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub from which the object is being removed
	 * @param obj       the object being removed
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public static boolean getVerifyRemove(Hub hub, OAObject obj, int checkType) {
		OAGraph g = getGraph(hub, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getVerifyRemove(hub, obj, checkType);
	}

	/**
	 * Returns whether all objects may be removed from the given hub by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub whose contents may be removed
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if removing all objects is allowed; otherwise {@code false}
	 */
	public static boolean getAllowRemoveAll(Hub hub, int checkType) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getAllowRemoveAll(hub, checkType);
	}

	/**
	 * Returns whether removing all objects from the given hub passes
	 * verification by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub whose objects may be removed
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public static boolean getVerifyRemoveAll(Hub hub, int checkType) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getVerifyRemoveAll(hub, checkType);
	}

	/**
	 * Returns whether the specified object may be deleted within the context
	 * of the given hub by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub the hub providing contextual rules
	 * @param obj the object to delete
	 * @return {@code true} if deletion is allowed; otherwise {@code false}
	 */
	public static boolean getAllowDelete(Hub hub, OAObject obj) {
		OAGraph g = getGraph(hub, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getAllowDelete(hub, obj);
	}

	/**
	 * Returns whether deleting the specified object passes verification by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub providing contextual rules
	 * @param obj       the object to delete
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public static boolean getVerifyDelete(Hub hub, OAObject obj, int checkType) {
		OAGraph g = getGraph(hub, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getVerifyDelete(hub, obj, checkType);
	}

	/**
	 * Returns whether the specified object may be saved by evaluating
	 * the associated {@link OAObjectCallback}.
	 *
	 * @param obj       the object to save
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if saving is allowed; otherwise {@code false}
	 */
	public static boolean getAllowSave(OAObject obj, int checkType) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getAllowSave(obj, checkType);
	}

	/**
	 * Returns whether saving the specified object passes verification by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param obj       the object to save
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public static boolean getVerifySave(OAObject obj, int checkType) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return false;
		return g.objects().getOAObjectCallbackService().getVerifySave(obj, checkType);
	}

	/**
	 * Creates an {@link OAObjectCallback} used to determine whether the
	 * specified object satisfies all required submit-time rules. The callback
	 * is initialized with {@code Type.AllowSubmit} and evaluated across the
	 * object's properties and owned links.
	 *
	 * @param obj the object to evaluate
	 * @return the resulting callback, or {@code null} if the object is {@code null}
	 */
	public static OAObjectCallback getAllowSubmitObjectCallback(OAObject obj) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowSubmitObjectCallback(obj);
	}


	/**
	 * Returns the formatting string for the specified object and property by
	 * evaluating a {@link OAObjectCallback} of type {@code GetFormat}. The
	 * default format is set first, then evaluated again after assigning the
	 * property name.
	 *
	 * @param obj           the target object
	 * @param propertyName  the property whose format is requested
	 * @param defaultFormat the initial format value
	 * @return the resulting format string
	 */
	public static String getFormat(OAObject obj, String propertyName, String defaultFormat) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getFormat(obj, propertyName, defaultFormat);
	}

	
	/**
	 * Returns the tooltip text for the specified object and property by
	 * evaluating a {@link OAObjectCallback} of type {@code GetToolTip}. The
	 * default tooltip is assigned first, then evaluated again after assigning
	 * the property name.
	 *
	 * @param obj           the target object
	 * @param propertyName  the property whose tooltip is requested
	 * @param defaultToolTip the initial tooltip value
	 * @return the resulting tooltip text
	 */
	public static String getToolTip(OAObject obj, String propertyName, String defaultToolTip) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getToolTip(obj, propertyName, defaultToolTip);
	}

	/**
	 * Evaluates a {@link OAObjectCallback} of type {@code RenderLabel} to allow
	 * callback logic to update the given label used for rendering a component.
	 * Evaluation occurs once before and once after assigning the property name.
	 *
	 * @param obj          the target object
	 * @param propertyName the property associated with the label
	 * @param label        the label to be updated
	 */
	public static void renderLabel(OAObject obj, String propertyName, JLabel label) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectCallbackService().renderLabel(obj, propertyName, label);
	}

	/**
	 * Evaluates a {@link OAObjectCallback} of type {@code UpdateLabel} to allow
	 * callback logic to modify the label associated with a component.
	 *
	 * @param obj          the target object
	 * @param propertyName the property associated with the label
	 * @param label        the label to update
	 */
	public static void updateLabel(OAObject obj, String propertyName, JLabel label) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectCallbackService().updateLabel(obj, propertyName, label);
	}

	/**
	 * Creates and returns an {@link OAObjectCallback} of type
	 * {@code AllowVisible} for the given hub by delegating to the overloaded
	 * method with {@code null} object and property name.
	 *
	 * @param hub the hub providing visibility context
	 * @return the resulting callback
	 */
    public static OAObjectCallback getAllowVisibleObjectCallback(Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowVisibleObjectCallback(hub);
    }
	
    /**
     * Creates and evaluates an {@link OAObjectCallback} of type
     * {@code AllowVisible} for the specified hub, object, and property or link
     * name. When the object is {@code null}, the appropriate master object is
     * resolved from the hub.
     *
     * @param hub   the hub providing contextual rules
     * @param oaObj the target object, or {@code null}
     * @param name  the property or link name, or {@code null}
     * @return the resulting callback, or {@code null} if hub and object are both null
     */
	public static OAObjectCallback getAllowVisibleObjectCallback(Hub hub, OAObject oaObj, String name) {
		OAGraph g = getGraph(hub, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowVisibleObjectCallback(hub, oaObj, name);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowEnabled} using the specified hub, object, property name,
	 * and check-type mask. When the object is {@code null}, the appropriate
	 * master object is resolved from the hub.
	 *
	 * @param checkType the bitmask of checking options
	 * @param hub       the hub providing contextual rules
	 * @param oaObj     the target object, or {@code null}
	 * @param name      the property, link, or method name, or {@code null}
	 * @return the resulting callback, or {@code null} if hub and object are both null
	 */
	public static OAObjectCallback getAllowEnabledObjectCallback(final int checkType, final Hub hub, OAObject oaObj, String name) {
		OAGraph g = getGraph(hub, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowEnabledObjectCallback(checkType, hub, oaObj, name);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowEnabled} for the given hub. If the hub has a master
	 * object, the callback is evaluated against that object; otherwise,
	 * only hub listeners are processed.
	 *
	 * @param hub the hub providing contextual rules
	 * @return the resulting callback
	 */
	public static OAObjectCallback getAllowEnabledObjectCallback(Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowEnabledObjectCallback(hub);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowCopy} for the specified object.
	 *
	 * @param oaObj the object being copied
	 * @return the resulting callback
	 */
	public static OAObjectCallback getAllowCopyObjectCallback(final OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowCopyObjectCallback(oaObj);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code GetCopy}
	 * to obtain a copy of the specified object or to allow callback logic to
	 * supply an alternate value.
	 *
	 * @param oaObj the object to copy
	 * @return the resulting callback
	 */
	public static OAObjectCallback getCopyObjectCallback(final OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getCopyObjectCallback(oaObj);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code AfterCopy}
	 * for the specified source object and its copy.
	 *
	 * @param oaObj     the original object
	 * @param oaObjCopy the copied object
	 * @return the resulting callback
	 */
	public static OAObjectCallback getAfterCopyObjectCallback(final OAObject oaObj, final OAObject oaObjCopy) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAfterCopyObjectCallback(oaObj, oaObjCopy);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifyPropertyChange} to validate a property update.
	 * The old value is assigned before processing.
	 *
	 * @param checkType    the bitmask of checking options
	 * @param oaObj        the target object
	 * @param propertyName the property being changed
	 * @param oldValue     the previous value
	 * @param newValue     the proposed new value
	 * @return the resulting callback
	 */
	public static OAObjectCallback getVerifyPropertyChangeObjectCallback(final int checkType, final OAObject oaObj,
			final String propertyName,
			final Object oldValue, final Object newValue) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getVerifyPropertyChangeObjectCallback(checkType, oaObj, propertyName, oldValue, newValue);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifyCommand} to validate invocation of the specified method.
	 *
	 * @param oaObj      the target object
	 * @param methodName the method to verify
	 * @param checkType  the bitmask of checking options
	 * @return the resulting callback
	 */
	public static OAObjectCallback getVerifyCommandObjectCallback(final OAObject oaObj, final String methodName, int checkType) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getVerifyCommandObjectCallback(oaObj, methodName, checkType);
	}

	/**
	 * Updates the callback to disallow editing when context rules
	 * indicate that processed-state editing is not permitted.
	 *
	 * @param objectCallback the callback to update
	 */
	public static void updateEditProcessed(OAObjectCallback objectCallback) {
		if (objectCallback == null) {
			return;
		}
		OAGraph g = getGraph(objectCallback.getHub(), objectCallback.getObject());
		if (g == null) return;
		g.objects().getOAObjectCallbackService().updateEditProcessed(objectCallback);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code AllowAdd}
	 * to determine whether the specified object may be added to the given hub.
	 * Hub listener rules or reverse-link rules may be applied depending on the
	 * hub's metadata.
	 *
	 * @param hub       the hub receiving the object
	 * @param objAdd    the object being added
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public static OAObjectCallback getAllowAddObjectCallback(final Hub hub, OAObject objAdd, final int checkType) {
		OAGraph g = getGraph(hub, objAdd);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowAddObjectCallback(hub, objAdd, checkType);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code VerifyAdd}
	 * to verify whether the specified object may be added to the given hub.
	 * Hub listener rules or reverse-link rules may be applied depending on
	 * the hub's metadata.
	 *
	 * @param hub       the hub receiving the object
	 * @param oaObj     the object being added
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public static OAObjectCallback getVerifyAddObjectCallback(final Hub hub, final OAObject oaObj, final int checkType) {
		OAGraph g = getGraph(hub, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getVerifyAddObjectCallback(hub, oaObj, checkType);
	}

	/**
	 * Creates an {@link OAObjectCallback} of type {@code AllowNew} to determine
	 * whether a new instance of the specified class may be created. Context and
	 * processed-state rules are evaluated before returning.
	 *
	 * @param clazz the class to evaluate
	 * @return the resulting callback, or {@code null} if the class is {@code null}
	 */
	public static OAObjectCallback getAllowNewObjectCallback(final Class clazz) {
		if (clazz == null) return null;
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowNewObjectCallback(clazz);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code AllowNew}
	 * to determine whether a new object may be created for the given hub. When
	 * applicable, reverse-link rules or hub listeners are evaluated.
	 *
	 * @param hub the hub providing contextual rules
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public static OAObjectCallback getAllowNewObjectCallback(final Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowNewObjectCallback(hub);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code AllowRemove}
	 * to determine whether the specified object may be removed from the hub.
	 * Hub listener rules or reverse-link rules may be applied depending on
	 * metadata.
	 *
	 * @param hub       the hub from which the object may be removed
	 * @param objRemove the object being removed
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public static OAObjectCallback getAllowRemoveObjectCallback(final Hub hub, final OAObject objRemove, final int checkType) {
		OAGraph g = getGraph(hub, objRemove);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowRemoveObjectCallback(hub, objRemove, checkType);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code VerifyRemove}
	 * to verify whether the specified object may be removed from the hub. Hub
	 * listener rules or reverse-link rules may be applied depending on metadata.
	 *
	 * @param hub       the hub from which the object is being removed
	 * @param objRemove the object being removed
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public static OAObjectCallback getVerifyRemoveObjectCallback(final Hub hub, final OAObject objRemove, final int checkType) {
		OAGraph g = getGraph(hub, objRemove);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getVerifyRemoveObjectCallback(hub, objRemove, checkType);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowRemoveAll} to determine whether all objects may be
	 * removed from the specified hub. Hub listener rules or reverse-link
	 * rules may be applied depending on metadata.
	 *
	 * @param hub       the hub whose objects may be removed
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public static OAObjectCallback getAllowRemoveAllObjectCallback(final Hub hub, final int checkType) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowRemoveAllObjectCallback(hub, checkType);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifyRemoveAll} to verify whether all objects may be removed
	 * from the specified hub. Hub listener rules or reverse-link rules
	 * may be applied depending on metadata.
	 *
	 * @param hub       the hub whose objects may be removed
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public static OAObjectCallback getVerifyRemoveAllObjectCallback(final Hub hub, final int checkType) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getVerifyRemoveAllObjectCallback(hub, checkType);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowSave} to determine whether the specified object may
	 * be saved.
	 *
	 * @param oaObj     the object to save
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback
	 */
	public static OAObjectCallback getAllowSaveObjectCallback(final OAObject oaObj, final int checkType) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowSaveObjectCallback(oaObj, checkType);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifySave} to verify whether the specified object may
	 * be saved.
	 *
	 * @param oaObj     the object to save
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback
	 */
	public static OAObjectCallback getVerifySaveObjectCallback(final OAObject oaObj, final int checkType) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getVerifySaveObjectCallback(oaObj, checkType);
	}

	
	/**
	 * Creates an {@link OAObjectCallback} of type {@code AllowDelete}
	 * to determine whether the specified object may be deleted. Context
	 * and processed-state rules are evaluated before returning.
	 *
	 * @param objDelete the object to delete
	 * @return the resulting callback, or {@code null} if the object or its class is null
	 */
	public static OAObjectCallback getAllowDeleteObjectCallback(final OAObject objDelete) {
		OAGraph g = getGraph(null, objDelete);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowDeleteObjectCallback(objDelete);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowDelete} to determine whether the specified object
	 * may be deleted within the context of the given hub. Hub listener
	 * rules or reverse-link rules may be applied depending on metadata.
	 *
	 * @param hub       the hub providing contextual rules
	 * @param objDelete the object to delete
	 * @return the resulting callback, or {@code null} if the hub or object is {@code null}
	 */
	public static OAObjectCallback getAllowDeleteObjectCallback(final Hub hub, final OAObject objDelete) {
		OAGraph g = getGraph(hub, objDelete);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getAllowDeleteObjectCallback(hub, objDelete);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifyDelete} to verify whether the specified object may
	 * be deleted from the given hub. Hub listener rules or reverse-link
	 * rules may be applied depending on metadata.
	 *
	 * @param hub       the hub providing contextual rules
	 * @param objDelete the object to delete
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback
	 */
	public static OAObjectCallback getVerifyDeleteObjectCallback(final Hub hub, final OAObject objDelete, final int checkType) {
		OAGraph g = getGraph(hub, objDelete);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getVerifyDeleteObjectCallback(hub, objDelete, checkType);
	}


	/**
	 * Creates a confirmation {@link OAObjectCallback} for a property change
	 * using an unknown future value. This delegates to
	 * {@code getConfirmPropertyChangeObjectCallback} with the value set to
	 * {@code OAUnknownObject.instance}.
	 *
	 * @param oaObj          the target object
	 * @param property       the property being changed
	 * @param confirmMessage the message to present for confirmation
	 * @param confirmTitle   the title to present for confirmation
	 * @return the resulting callback
	 */
    public static OAObjectCallback getPreConfirmPropertyChangeObjectCallback(final OAObject oaObj, String property, 
            String confirmMessage, String confirmTitle) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getPreConfirmPropertyChangeObjectCallback(oaObj, property, confirmMessage, confirmTitle);
    }

    /**
     * Creates a confirmation {@link OAObjectCallback} for a property change
     * using an unknown future value. This delegates to
     * {@code getConfirmPropertyChangeObjectCallback} with the value set to
     * {@code OAUnknownObject.instance}.
     *
     * @param oaObj          the target object
     * @param property       the property being changed
     * @param confirmMessage the message to present for confirmation
     * @param confirmTitle   the title to present for confirmation
     * @return the resulting callback
     */
    public static OAObjectCallback getConfirmPropertyChangeObjectCallback(final OAObject oaObj, String property, Object newValue,
			String confirmMessage, String confirmTitle) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getConfirmPropertyChangeObjectCallback(oaObj, property, newValue, confirmMessage, confirmTitle);
	}

    /**
     * Creates and evaluates an {@link OAObjectCallback} of type
     * {@code SetConfirmForCommand} to supply confirmation text for
     * invoking the specified method.
     *
     * @param oaObj          the target object
     * @param methodName     the method requiring confirmation
     * @param confirmMessage the confirmation message
     * @param confirmTitle   the confirmation title
     * @return the resulting callback
     */
	public static OAObjectCallback getConfirmCommandObjectCallback(final OAObject oaObj, String methodName, String confirmMessage,
			String confirmTitle) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getConfirmCommandObjectCallback(oaObj, methodName, confirmMessage, confirmTitle);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code SetConfirmForSave} to supply confirmation text for saving
	 * the specified object. The confirmation message and title are assigned
	 * to the callback before it is processed.
	 *
	 * @param oaObj          the object being saved
	 * @param confirmMessage the confirmation message to assign
	 * @param confirmTitle   the confirmation title to assign
	 * @return the resulting callback
	 */
	public static OAObjectCallback getConfirmSaveObjectCallback(final OAObject oaObj, String confirmMessage, String confirmTitle) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getConfirmSaveObjectCallback(oaObj, confirmMessage, confirmTitle);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code SetConfirmForDelete} to supply confirmation text for deleting
	 * the specified object. The confirmation message and title are assigned
	 * to the callback before processing.
	 *
	 * @param oaObj          the object to delete
	 * @param confirmMessage the confirmation message to assign
	 * @param confirmTitle   the confirmation title to assign
	 * @return the resulting callback
	 */
	public static OAObjectCallback getConfirmDeleteObjectCallback(final OAObject oaObj, String confirmMessage, String confirmTitle) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getConfirmDeleteObjectCallback(oaObj, confirmMessage, confirmTitle);
	}

	/**
	 * Creates an {@link OAObjectCallback} of type {@code SetConfirmForRemove}
	 * to supply confirmation text for removing the specified object from the
	 * given hub. When a master object exists, the property name for the
	 * master-to-detail link is assigned before processing.
	 *
	 * @param hub            the hub providing contextual rules
	 * @param oaObj          the object being removed
	 * @param confirmMessage the confirmation message to assign
	 * @param confirmTitle   the confirmation title to assign
	 * @return the resulting callback
	 */
	public static OAObjectCallback getConfirmRemoveObjectCallback(final Hub hub, final OAObject oaObj, String confirmMessage,
			String confirmTitle) {
		OAGraph g = getGraph(hub, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getConfirmRemoveObjectCallback(hub, oaObj, confirmMessage, confirmTitle);
	}


	/**
	 * Creates an {@link OAObjectCallback} of type {@code SetConfirmForRemoveAll}
	 * to supply confirmation text for removing all objects from the given hub.
	 * When a master object exists, the master-to-detail property name is
	 * assigned before processing.
	 *
	 * @param hub            the hub whose objects may be removed
	 * @param confirmMessage the confirmation message to assign
	 * @param confirmTitle   the confirmation title to assign
	 * @return the resulting callback
	 */
    public static OAObjectCallback getConfirmRemoveAllObjectCallback(final Hub hub, String confirmMessage,
            String confirmTitle) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getConfirmRemoveAllObjectCallback(hub, confirmMessage, confirmTitle);
    }
	
	
    /**
     * Creates an {@link OAObjectCallback} of type {@code SetConfirmForAdd}
     * to supply confirmation text for adding the specified object to the
     * given hub. When a master object exists, the master-to-detail property
     * name is assigned before processing.
     *
     * @param hub            the hub receiving the object
     * @param oaObj          the object being added
     * @param confirmMessage the confirmation message to assign
     * @param confirmTitle   the confirmation title to assign
     * @return the resulting callback
     */
	public static OAObjectCallback getConfirmAddObjectCallback(final Hub hub, final OAObject oaObj, String confirmMessage,
			String confirmTitle) {
		OAGraph g = getGraph(hub, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectCallbackService().getConfirmAddObjectCallback(hub, oaObj, confirmMessage, confirmTitle);
	}

	/*qqqqqqq
	protected static void processObjectCallback(OAObjectCallback objectCallback, final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue) {
	    processObjectCallback(objectCallback, null, null, oaObj, propertyName, oldValue, newValue, false);
	}
	protected static void processObjectCallback(OAObjectCallback objectCallback, final Class<? extends OAObject> clazz, final String propertyName, final Object oldValue, final Object newValue) {
	    processObjectCallback(objectCallback, null, clazz, null, propertyName, oldValue, newValue, false);
	}
	*/

	
	/**
	 * Processes the supplied callback by delegating to the internal
	 * {@code _processObjectCallback} method. After processing, the
	 * callback is updated to allow all operations when the demo flag
	 * is enabled, or when the current user is a super-admin.
	 *
	 * @param objectCallback the callback to process
	 */
	protected static void processObjectCallbackXX(OAObjectCallback objectCallback) {
		//_processObjectCallback(objectCallback);
		OAGraph g = getGraph(objectCallback.getHub(), objectCallback.getObject());
		if (g == null) return;
		// g.objects().getOAObjectCallbackService().processObjectCallback(objectCallback);
	}

	/**
	 * Global demo-mode flag that forces all OAObjectCallback evaluations
	 * to succeed. When enabled, callback failures are overridden to allow
	 * all operations, useful for demos or testing scenarios.
	 */
	private static boolean DEMO_AllowAllToPass;

	/**
	 * Enables or disables demo mode for allowing all callbacks to pass.
	 * When enabled, warning messages are logged and printed to standard
	 * output. This flag affects subsequent callback processing.
	 *
	 * @param b {@code true} to allow all callbacks to pass; otherwise {@code false}
	 */
	public static void demoAllowAllToPass(boolean b) {
		OAObjectCallbackDelegate.DEMO_AllowAllToPass = b;
	}

	/**
	 * Processes the supplied callback by evaluating rules based on hub context,
	 * class metadata, object state, property-level definitions, user-access
	 * settings, and any callback methods defined on the object. This method
	 * updates the callback’s allowed state and response message as needed
	 * and invokes hub listeners when applicable.
	 *
	 * @param objectCallback the callback being evaluated
	 */
	protected static void _processObjectCallbackXX(final OAObjectCallback objectCallback) {
		OAGraph g = getGraph(objectCallback.getHub(), objectCallback.getObject());
		if (g == null) return;
		// g.objects().getOAObjectCallbackService()._processObjectCallback(objectCallback);
	}

	/**
	 * Evaluates visibility or enabled-state rules for the specified object
	 * and all of its owners by delegating to the recursive
	 * {@code _ownerHierProcess} method starting at depth {@code 0}.
	 *
	 * @param objectCallback the callback being updated
	 * @param oaObj          the target object
	 * @param propertyName   the property or link name associated with the callback
	 */
	protected static void ownerHierProcessXX(OAObjectCallback objectCallback, final OAObject oaObj, final String propertyName) {
		//_ownerHierProcess(objectCallback, oaObj, propertyName, null, 0);
	}

	/**
	 * Recursively evaluates visibility rules for the specified object and its
	 * owner hierarchy. Starting from the topmost owner, class-level and
	 * context-level visibility settings are applied, followed by invocation
	 * of any object-level callback method. The callback’s allowed state and
	 * response message may be updated based on these evaluations.
	 *
	 * @param objectCallback the callback being updated
	 * @param oaObj          the current object being evaluated
	 * @param propertyName   the property or link name associated with the callback
	 * @param li             the link used when navigating the owner hierarchy
	 * @param cnter          the recursion depth counter
	 */
	protected static void _ownerHierProcessXX(OAObjectCallback objectCallback, final OAObject oaObj, final String propertyName,
			final OALinkInfo li, final int cnter) {
		OAGraph g = getGraph(objectCallback.getHub(), oaObj);
		if (g == null) return;
		// g.objects().getOAObjectCallbackService()._ownerHierProcess(objectCallback, oaObj, propertyName, li, cnter);
	}


	/**
	 * Notifies hub-level listeners of a callback event by creating or updating
	 * a {@link HubEvent} and invoking the appropriate listener method based
	 * on the callback type. The callback’s allowed state and response message
	 * may be updated according to listener results. Any thrown exception marks
	 * the callback as not allowed.
	 *
	 * @param objectCallback the callback being evaluated
	 * @param hub            the hub providing listeners
	 * @param oaObj          the target object
	 * @param propertyName   the property or link associated with the callback
	 * @param oldValue       the previous value for property-change callbacks
	 * @param newValue       the new value for property-change callbacks
	 */
	protected static void processObjectCallbackForHubListenersXX(OAObjectCallback objectCallback, final Hub hub, final OAObject oaObj,
			final String propertyName, final Object oldValue, final Object newValue) {
		OAGraph g = getGraph(hub, oaObj);
		if (g == null) return;
		// g.objects().getOAObjectCallbackService().processObjectCallbackForHubListeners(objectCallback, hub, oaObj, propertyName, oldValue, newValue);
	}

	/**
	 * Internal helper used to notify hub listeners of a callback event. A
	 * {@link HubEvent} is created if needed, and each listener is invoked
	 * according to the callback type. Listener results update the callback’s
	 * allowed state and response message. Exceptions mark the callback as not
	 * allowed.
	 *
	 * @param objectCallback the callback being evaluated
	 * @param hub            the hub providing listeners
	 * @param oaObj          the target object
	 * @param propertyName   the property or link associated with the callback
	 * @param oldValue       the previous property value
	 * @param newValue       the new property value
	 */
	protected static void _processObjectCallbackForHubListenersXX(OAObjectCallback objectCallback, final Hub hub, final OAObject oaObj,
			final String propertyName, final Object oldValue, final Object newValue) {
		OAGraph g = getGraph(hub, oaObj);
		if (g == null) return;
		// g.objects().getOAObjectCallbackService()._processObjectCallbackForHubListeners(objectCallback, hub, oaObj);
	}

	/**
	 * Invokes the callback method on the object referenced by the supplied
	 * {@link OAObjectCallback}. The method is resolved using the object's
	 * metadata and executed with the callback instance. If no method exists,
	 * the call is ignored.
	 *
	 * @param em the callback whose associated object method is invoked
	 */
	protected static void callObjectCallbackMethodXX(final OAObjectCallback em) {
		OAGraph g = getGraph(em.getHub(), em.getObject());
		if (g == null) return;
		// g.objects().getOAObjectCallbackService().callObjectCallbackMethod(em);
	}

	/**
	 * Resolves and invokes the object-level callback method associated with
	 * the given property. If the resolved method exists, it is executed with
	 * the supplied callback instance. Any exception marks the callback as not
	 * allowed and stores the throwable.
	 *
	 * @param object        the target object on which the method may be invoked
	 * @param propertyName  the property whose callback method is requested
	 * @param em            the callback instance passed to the invoked method
	 */
	protected static void callObjectCallbackMethodXX(final Object object, String propertyName, final OAObjectCallback em) {
		OAGraph g = getGraph(em.getHub(), em.getObject());
		if (g == null) return;
		//g.objects().getOAObjectCallbackService().callObjectCallbackMethod(object, propertyName, em);
	}

	/**
	 * Invokes a model-level callback method for the specified class and
	 * property. The method is resolved from the class metadata and must
	 * accept an {@link OAObjectModel} parameter. If found, the method is
	 * invoked statically with the supplied model instance.
	 *
	 * @param clazz    the class defining the callback
	 * @param property the property name used to locate the callback method
	 * @param model    the model instance passed to the callback
	 */
	public static void onObjectCallbackModel(Class clazz, String property, OAObjectModel model) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return;
		g.objects().getOAObjectCallbackService().onObjectCallbackModel(clazz, property, model);
	}

	/**
	 * Registers change listeners on properties that influence the visibility
	 * or enabled state evaluated by an {@link OAObjectCallback}. Class-level
	 * and dependent properties are added to the supplied listener using the
	 * provided prefix.
	 *
	 * @param hub            the hub whose objects are monitored
	 * @param cz             the class whose metadata defines dependent properties
	 * @param prop           the property associated with the callback
	 * @param ppPrefix       an optional prefix for dependent property paths
	 * @param changeListener the listener to register dependencies with
	 * @param bEnabled       true to use enabled dependencies; false for visible
	 */
	public static void addObjectCallbackChangeListeners(final Hub hub, final Class cz, final String prop, String ppPrefix,
			final HubChangeListener changeListener, final boolean bEnabled) {
		OAGraph g = OARuntime.get().graph(cz);
		if (g == null) return;
		g.objects().getOAObjectCallbackService().addObjectCallbackChangeListeners(hub, cz, prop, ppPrefix, changeListener, bEnabled);
	}

	/**
	 * Adds dependent property paths to the supplied {@link HubChangeListener}.
	 * View-level, context-level, and processed-dependent properties are added
	 * using the provided prefix. Null or empty property arrays are ignored.
	 *
	 * @param hub                       the hub whose objects are monitored
	 * @param prefix                    optional property-path prefix
	 * @param viewDependentProperties   properties affecting visibility
	 * @param contextDependentProperties properties affecting context visibility or enabled state
	 * @param bProcessed                true to include processed-dependent properties
	 * @param changeListener            the listener that receives dependent paths
	 */
	protected static void addDependentPropsXX(Hub hub, String prefix, String[] viewDependentProperties, String[] contextDependentProperties,
			boolean bProcessed, HubChangeListener changeListener) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return;
		//g.objects().getOAObjectCallbackService().addDependentProps(hub, prefix, viewDependentProperties,
		//	contextDependentProperties, bProcessed, changeListener); 
	}
}
