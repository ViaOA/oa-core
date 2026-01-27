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
package com.viaoa.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import com.viaoa.graph.OAGraphImpl;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAString;

/**
 * Implementation of {@link javax.swing.undo.UndoableEdit} that captures a
 * reversible change to an OA Hub or OAObject.
 * <p>
 * An {@code OAUndoableEdit} instance records all data needed to undo or redo
 * a specific operation. Supported edit types include:
 * <ul>
 *   <li>{@link #ADD} – add object to Hub,</li>
 *   <li>{@link #REMOVE} – remove object from Hub,</li>
 *   <li>{@link #MOVE} – reorder an object within a Hub,</li>
 *   <li>{@link #INSERT} – insert object at specific position,</li>
 *   <li>{@link #CHANGEAO} – change active object of a Hub,</li>
 *   <li>{@link #PROPCHANGE} – change an OAObject property,</li>
 *   <li>{@link #HOLDER} – placeholder for custom logic.</li>
 * </ul>
 *
 * <h2>Undo / Redo Semantics</h2>
 * Undo and redo operations perform symmetric Hub or OAObject changes, ensuring
 * full reversibility:
 * <ul>
 *   <li>Add → Remove</li>
 *   <li>Remove → Insert</li>
 *   <li>Move → Move (reverse positions)</li>
 *   <li>Insert → Remove</li>
 *   <li>Active Object change → restore previous AO</li>
 *   <li>Property change → restore previous value</li>
 * </ul>
 *
 * <h2>Presentation Names</h2>
 * Presentation names are used for menus and UI components:
 * <ul>
 *   <li>{@link #getPresentationName()}</li>
 *   <li>{@link #getUndoPresentationName()}</li>
 *   <li>{@link #getRedoPresentationName()}</li>
 * </ul>
 * Names can be automatically generated when not provided.
 *
 * <h2>Replacement Logic</h2>
 * Edits can optionally replace previous edits of the same type, object, and
 * property. This is controlled by {@link #setAllowReplace(boolean)} and is
 * tested using {@link #equals(Object)}.
 *
 * <h2>Usage</h2>
 * Edit instances are created through static factory methods such as:
 * <ul>
 *   <li>{@link #createUndoableAdd(String, Hub, Object)}</li>
 *   <li>{@link #createUndoableRemove(String, Hub, Object, int)}</li>
 *   <li>{@link #createUndoablePropertyChange(String, Object, String, Object, Object)}</li>
 * </ul>
 *
 * <p>
 * This class is used extensively by OA GUI controllers and
 * {@link com.viaoa.undo.OAUndoManager} to provide application-level undo/redo.
 */
public class OAUndoableEdit implements UndoableEdit {

	/**
	 * Identifies the type of undoable operation represented by this edit.
	 * <p>
	 * The value corresponds to one of the predefined constants such as
	 * {@link #ADD}, {@link #REMOVE}, {@link #MOVE}, {@link #INSERT},
	 * {@link #CHANGEAO}, {@link #PROPCHANGE}, or {@link #HOLDER}.
	 * This field controls the logic executed during {@link #undo()} and {@link #redo()}.
	 */
	int type;
	
	/**
	 * The {@link Hub} associated with this undoable edit.
	 * <p>
	 * This is used for operations that affect hub membership, ordering,
	 * or active object selection, such as add, remove, move, insert,
	 * and active object changes.
	 */
	Hub hub;
	
	/**
	 * The name of the property being modified for property change edits.
	 * <p>
	 * This value is only applicable when {@link #type} is {@link #PROPCHANGE}
	 * and identifies the OAObject property whose value is being changed.
	 */
	String propertyName;
	
	/**
	 * The previous and new values associated with this undoable edit.
	 * <p>
	 * These values are primarily used for property change and active object
	 * change operations to restore or reapply state during undo and redo.
	 */
	Object prevValue, newValue;
	
	/**
	 * The target object affected by this undoable edit.
	 * <p>
	 * This may be an {@link OAObject} for property changes or a hub member
	 * object for add, remove, insert, or move operations.
	 */
	Object object;
	
	/**
	 * The human-readable name used to describe this edit in the UI.
	 * <p>
	 * This value is displayed in undo and redo menu items and may be
	 * automatically generated when not explicitly provided.
	 */
	String presentationName;
	
	/**
	 * Indicates whether this edit can currently be undone.
	 * <p>
	 * This flag is updated as undo and redo operations are performed
	 * to enforce correct undo/redo sequencing.
	 */
	boolean bCanUndo = true;
	
	/**
	 * Stores the previous and new positions for move and insert operations.
	 * <p>
	 * These values are used to reverse or reapply ordering changes
	 * within a {@link Hub}.
	 */
	int prevPos, newPos;
	
	/**
	 * Flag indicating whether this edit is allowed to replace a previous edit.
	 * <p>
	 * Replacement is determined by {@link #equals(Object)} and is used
	 * to coalesce similar edits into a single undoable action.
	 */
	boolean bAllowReplace;
	
	/**
	 * Controls whether redo operations are permitted for this edit.
	 * <p>
	 * When set to {@code false}, the edit will not allow redo even
	 * after a successful undo.
	 */
	boolean bAllowRedo = true;
	
	/**
	 * Indicates whether the target {@link OAObject} was previously marked as changed.
	 * <p>
	 * This is used during property change undo operations to restore
	 * the original changed state when appropriate.
	 */
	boolean wasChanged;

	/**
	 * Edit type constant representing the addition of an object to a {@link Hub}.
	 * <p>
	 * Undoing this edit removes the object, while redoing it adds the object back.
	 */
	public static final int ADD = 0;

	/**
	 * Edit type constant representing the removal of an object from a {@link Hub}.
	 * <p>
	 * Undoing this edit reinserts the object at its original position.
	 */
	public static final int REMOVE = 1;
	
	/**
	 * Edit type constant representing a reordering of an object within a {@link Hub}.
	 * <p>
	 * Undo and redo operations swap the previous and new positions.
	 */
	public static final int MOVE = 2;
	
	/**
	 * Edit type constant representing insertion of an object at a specific position.
	 * <p>
	 * Undoing this edit removes the object from the hub.
	 */
	public static final int INSERT = 3;
	
	/**
	 * Edit type constant representing a change to a hub's active object.
	 * <p>
	 * Undo restores the previous active object, while redo reapplies the new one.
	 */
	public static final int CHANGEAO = 4;
	
	/**
	 * Edit type constant representing a property value change on an {@link OAObject}.
	 * <p>
	 * Undo restores the previous property value, and redo reapplies the new value.
	 */
	public static final int PROPCHANGE = 5;
	
	/**
	 * Alternate constant for {@link #PROPCHANGE}.
	 * <p>
	 * This alias exists for backward compatibility and semantic clarity.
	 */
	public static final int PROPERTYCHANGE = 5;
	
	/**
	 * Edit type constant representing a placeholder or custom undoable action.
	 * <p>
	 * This type performs no default undo or redo logic and can be used
	 * for application-specific behaviors.
	 */
	public static final int HOLDER = 6;

	/**
	 * Private constructor to enforce controlled creation of undoable edits.
	 * <p>
	 * Instances are created exclusively through static factory methods,
	 * ensuring correct initialization based on edit type.
	 */
	private OAUndoableEdit() {
	}

	/**
	 * Creates an undoable edit representing the addition of an object to a Hub.
	 * <p>
	 * Undoing this edit will remove the object from the hub, while redoing
	 * it will add the object back. If no presentation name is supplied,
	 * a default name based on the object type is generated.
	 *
	 * @param presentationName the UI presentation name, or {@code null} to auto-generate
	 * @param hub the hub to which the object is added
	 * @param obj the object being added to the hub
	 * @return a configured {@code OAUndoableEdit} instance
	 */
	public static OAUndoableEdit createUndoableAdd(String presentationName, Hub hub, Object obj) {
		OAUndoableEdit oe = new OAUndoableEdit();
		oe.type = ADD;
		oe.hub = hub;
		oe.object = obj;
		if (presentationName == null) {
			presentationName = "Add " + oe.getClassName();
		}
		oe.presentationName = presentationName;
		return oe;
	}

	/**
	 * Creates an undoable edit representing a change to a hub's active object.
	 * <p>
	 * Undoing this edit restores the previous active object, while redoing
	 * it sets the new active object. When the presentation name is not provided,
	 * it is automatically generated using hub and link metadata.
	 *
	 * @param presentationName the UI presentation name, or {@code null} to auto-generate
	 * @param hub the hub whose active object is being changed
	 * @param prevObject the previously active object
	 * @param newObject the new active object
	 * @return a configured {@code OAUndoableEdit} instance
	 */
	public static OAUndoableEdit createUndoableChangeAO(String presentationName, Hub hub, Object prevObject, Object newObject) {
		OAUndoableEdit oe = new OAUndoableEdit();
		oe.type = CHANGEAO;
		oe.hub = hub;
		oe.newValue = newObject;
		oe.prevValue = prevObject;
		if (presentationName == null) {
			Class c = hub.getObjectClass();
			String s = OAString.convertToHungarian(c.getSimpleName());

			Hub h2 = hub.getLinkHub(true);
			if (h2 != null) {
				final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(hub);
				c = h2.getObjectClass();
				s = OAString.convertToHungarian(c.getSimpleName());
				String s2 = og.getHubService().getHubLinkService().getLinkToProperty(hub);
				presentationName = "change to " + s + " " + s2;
			} else {
				presentationName = "change selected " + s;
			}
		}
		oe.presentationName = presentationName;
		return oe;
	}

	/**
	 * Creates an undoable edit representing insertion of an object into a Hub
	 * at a specific position.
	 * <p>
	 * Undoing this edit removes the object from the hub, while redoing it
	 * reinserts the object at the specified position.
	 *
	 * @param presentationName the UI presentation name, or {@code null} to auto-generate
	 * @param hub the hub receiving the inserted object
	 * @param obj the object being inserted
	 * @param pos the target position for insertion
	 * @return a configured {@code OAUndoableEdit} instance
	 */
	public static OAUndoableEdit createUndoableInsert(String presentationName, Hub hub, Object obj, int pos) {
		OAUndoableEdit oe = new OAUndoableEdit();
		oe.type = INSERT;
		oe.hub = hub;
		oe.object = obj;
		oe.newPos = pos;
		if (presentationName == null) {
			presentationName = "Insert " + oe.getClassName();
		}
		oe.presentationName = presentationName;
		return oe;
	}

	/**
	 * Creates an undoable edit representing removal of an object from a Hub.
	 * <p>
	 * Undoing this edit reinserts the object at its original position,
	 * while redoing it removes the object again.
	 *
	 * @param presentationName the UI presentation name, or {@code null} to auto-generate
	 * @param hub the hub from which the object is removed
	 * @param obj the object being removed
	 * @param pos the original position of the object
	 * @return a configured {@code OAUndoableEdit} instance
	 */
	public static OAUndoableEdit createUndoableRemove(String presentationName, Hub hub, Object obj, int pos) {
		OAUndoableEdit oe = new OAUndoableEdit();
		oe.type = REMOVE;
		oe.hub = hub;
		oe.object = obj;
		oe.prevPos = pos;
		if (presentationName == null) {
			presentationName = "Remove " + oe.getClassName();
		}
		oe.presentationName = presentationName;
		return oe;
	}

	/**
	 * Creates an undoable edit representing a move operation within a Hub.
	 * <p>
	 * Undoing this edit moves the object back to its previous position,
	 * while redoing it moves the object to the new position.
	 *
	 * @param presentationName the UI presentation name, or {@code null} to auto-generate
	 * @param hub the hub whose contents are being reordered
	 * @param prevPos the original position of the object
	 * @param newPos the new position of the object
	 * @return a configured {@code OAUndoableEdit} instance
	 */
	public static OAUndoableEdit createUndoableMove(String presentationName, Hub hub, int prevPos, int newPos) {
		OAUndoableEdit oe = new OAUndoableEdit();
		oe.type = MOVE;
		oe.hub = hub;
		oe.prevPos = prevPos;
		oe.newPos = newPos;
		if (presentationName == null) {
			presentationName = "Move " + oe.getClassName();
		}
		oe.presentationName = presentationName;
		return oe;
	}

	/**
	 * Creates an undoable edit representing a property change on an OAObject.
	 * <p>
	 * This convenience method assumes the object was marked as changed
	 * and delegates to the full property change factory method.
	 *
	 * @param presentationName the UI presentation name, or {@code null} to auto-generate
	 * @param obj the object whose property is being changed
	 * @param prop the name of the property being modified
	 * @param prevValue the previous property value
	 * @param newValue the new property value
	 * @return a configured {@code OAUndoableEdit} instance
	 */
	public static OAUndoableEdit createUndoablePropertyChange(String presentationName, Object obj, String prop, Object prevValue,
			Object newValue) {
		return createUndoablePropertyChange(presentationName, obj, prop, prevValue, newValue, true);
	}

	/**
	 * Creates an undoable edit representing a property change on an OAObject.
	 * <p>
	 * Undoing this edit restores the previous property value and optionally
	 * restores the original changed-state flag of the object.
	 *
	 * @param presentationName the UI presentation name, or {@code null} to auto-generate
	 * @param obj the object whose property is being changed
	 * @param prop the name of the property being modified
	 * @param prevValue the previous property value
	 * @param newValue the new property value
	 * @param wasChanged the original changed-state flag of the object
	 * @return a configured {@code OAUndoableEdit} instance
	 */
	public static OAUndoableEdit createUndoablePropertyChange(String presentationName, Object obj, String prop, Object prevValue,
			Object newValue, boolean wasChanged) {
		OAUndoableEdit oe = new OAUndoableEdit();
		oe.type = PROPCHANGE;
		oe.object = obj;
		oe.propertyName = prop;
		oe.prevValue = prevValue;
		oe.newValue = newValue;
		if (presentationName == null) {
			String s = oe.getClassName();
			s += " " + OAString.convertToHungarian(prop);
			presentationName = "Change to " + s;
		}
		oe.presentationName = presentationName;
		oe.wasChanged = wasChanged;
		return oe;
	}

	/**
	 * Creates a placeholder undoable edit with no default undo or redo behavior.
	 * <p>
	 * This edit type can be used for grouping or custom undo logic
	 * defined externally by the application.
	 *
	 * @param presentationName the UI presentation name
	 * @return a configured {@code OAUndoableEdit} instance
	 */
	public static OAUndoableEdit createUndoable(String presentationName) {
		OAUndoableEdit oe = new OAUndoableEdit();
		oe.type = HOLDER;
		oe.presentationName = presentationName;
		return oe;
	}

	/**
	 * Determines the human-readable class name for this edit.
	 * <p>
	 * The name is derived from the affected object's class or the hub's
	 * object class and converted to a user-friendly format.
	 *
	 * @return a formatted class name, or {@code null} if unavailable
	 */
	private String getClassName() {
		Class c = null;
		String s = null;
		if (object != null) {
			c = object.getClass();
		} else if (hub != null) {
			c = hub.getObjectClass();
		}
		if (c != null) {
			s = c.getSimpleName();
			s = OAString.convertToHungarian(s);
		}
		return s;
	}

	/**
	 * Sets the presentation name used to describe this edit in the UI.
	 *
	 * @param name the presentation name to assign
	 */
	public void setName(String name) {
		presentationName = name;
	}

	/**
	 * Returns the presentation name for this undoable edit.
	 * <p>
	 * This name is used by UI components to describe the edit
	 * in undo and redo menu items.
	 *
	 * @return the presentation name
	 */
	public String getName() {
		return presentationName;
	}

	/**
	 * Sets the presentation name used for UI display of this edit.
	 *
	 * @param name the presentation name to assign
	 */
	public void setPresentationName(String name) {
		presentationName = name;
	}

	/**
	 * Returns the presentation name used for UI display.
	 *
	 * @return the presentation name
	 */
	public String getPresentationName() {
		return presentationName;
	}

	/**
	 * Indicates whether this edit can currently be undone.
	 *
	 * @return {@code true} if undo is allowed, otherwise {@code false}
	 */
	public boolean canUndo() {
		return bCanUndo;
	}

	/**
	 * Undoes the operation represented by this edit.
	 * <p>
	 * The undo logic performed depends on the edit {@link #type}
	 * and restores the previous state of the hub or object.
	 *
	 * @throws CannotUndoException if the undo operation is not permitted
	 */
	public void undo() throws CannotUndoException {
		bCanUndo = false;
		switch (type) {
		case HOLDER:
			break;
		case ADD:
			hub.remove(object);
			break;
		case REMOVE:
			hub.insert(object, prevPos);
			break;
		case MOVE:
			hub.move(newPos, prevPos);
			break;
		case INSERT:
			hub.remove(object);
			break;
		case CHANGEAO:
			hub.setAO(prevValue);
			break;
		case PROPCHANGE:
			((OAObject) object).setProperty(propertyName, prevValue);
			if (!wasChanged && object instanceof OAObject) {
				((OAObject) object).setChanged(false);
			}
			break;
		}
	}

	/**
	 * Redoes the operation represented by this edit.
	 * <p>
	 * The redo logic performed depends on the edit {@link #type}
	 * and reapplies the change previously undone.
	 *
	 * @throws CannotRedoException if the redo operation is not permitted
	 */
	public void redo() throws CannotRedoException {
		bCanUndo = true;
		switch (type) {
		case HOLDER:
			break;
		case ADD:
			hub.add(object);
			break;
		case REMOVE:
			hub.remove(object);
			break;
		case MOVE:
			hub.move(prevPos, newPos);
			break;
		case INSERT:
			hub.insert(object, newPos);
			break;
		case CHANGEAO:
			hub.setAO(newValue);
			break;
		case PROPCHANGE:
			((OAObject) object).setProperty(propertyName, newValue);
			break;
		}
	}

	/**
	 * Indicates whether this edit can currently be redone.
	 *
	 * @return {@code true} if redo is allowed, otherwise {@code false}
	 */
	public boolean canRedo() {
		return !bCanUndo && bAllowRedo;
	}

	/**
	 * Returns the UI presentation name for undo operations.
	 *
	 * @return the undo presentation name
	 */
	public String getUndoPresentationName() {
		return "Undo " + presentationName;
	}

	/**
	 * Returns the UI presentation name for redo operations.
	 *
	 * @return the redo presentation name
	 */
	public String getRedoPresentationName() {
		return "Redo " + presentationName;
	}

	/**
	 * Indicates whether this edit is considered significant.
	 * <p>
	 * Significant edits are typically shown to the user
	 * in undo and redo UI components.
	 *
	 * @return {@code true}, indicating this edit is significant
	 */
	public boolean isSignificant() {
		return true;
	}

	/**
	 * Attempts to incorporate another edit into this edit.
	 * <p>
	 * This implementation does not combine edits and always
	 * returns {@code false}.
	 *
	 * @param anEdit the incoming edit to add
	 * @return {@code false}, indicating the edit was not added
	 */
	public boolean addEdit(UndoableEdit anEdit) {
		return false;
	}

	/**
	 * Marks this edit as no longer needed.
	 * <p>
	 * This implementation performs no cleanup and is provided
	 * to satisfy the {@link UndoableEdit} contract.
	 */
	public void die() {
	}

	/**
	 * Determines whether this edit should replace a previous edit.
	 * <p>
	 * Replacement is allowed when the incoming edit is an
	 * {@code OAUndoableEdit}, replacement is enabled, and both
	 * edits are considered equal.
	 *
	 * @param anEdit the edit to potentially replace
	 * @return {@code true} if the previous edit should be replaced
	 */
	public boolean replaceEdit(UndoableEdit anEdit) {
		return (anEdit != null && (anEdit instanceof OAUndoableEdit) && ((OAUndoableEdit) anEdit).bAllowReplace && this.equals(anEdit));
	}

	/**
	 * Enables or disables replacement of a previous matching edit.
	 *
	 * @param b {@code true} to allow replacement, {@code false} otherwise
	 */
	public void setAllowReplace(boolean b) {
		bAllowReplace = b;
	}

	/**
	 * Indicates whether replacement of a previous edit is allowed.
	 *
	 * @return {@code true} if replacement is enabled
	 */
	public boolean getAllowReplace() {
		return bAllowReplace;
	}

	/**
	 * Enables or disables redo operations for this edit.
	 *
	 * @param b {@code true} to allow redo, {@code false} otherwise
	 */
	public void setAllowRedo(boolean b) {
		bAllowRedo = b;
	}

	/**
	 * Indicates whether redo operations are allowed for this edit.
	 *
	 * @return {@code true} if redo is enabled
	 */
	public boolean getAllowRedo() {
		return bAllowRedo;
	}

	/**
	 * Compares this edit with another object for equality.
	 * <p>
	 * Edits are considered equal when they represent the same
	 * type of operation on the same target object and property.
	 *
	 * @param obj the object to compare against
	 * @return {@code true} if the edits are considered equal
	 */
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof OAUndoableEdit)) {
			return false;
		}
		OAUndoableEdit ue = (OAUndoableEdit) obj;
		if (this.type != ue.type) {
			return false;
		}
		if (object != ue.object) {
			return false;
		}
		/*
		if (this.presentationName != ue.presentationName) {
		    if (this.presentationName == null || !this.presentationName.equals(ue.presentationName)) return false;
		}
		*/
		if (this.propertyName != ue.propertyName) {
			if (this.propertyName == null || !this.propertyName.equals(ue.propertyName)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns a hash code for this undoable edit.
	 * <p>
	 * The hash code is derived from the edit type and target object
	 * to remain consistent with {@link #equals(Object)}.
	 *
	 * @return the hash code value for this edit
	 */
	@Override
	public int hashCode() {
		return (type + "." + object).hashCode();
	}
}
