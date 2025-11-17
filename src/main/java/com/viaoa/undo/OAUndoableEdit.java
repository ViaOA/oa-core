/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubLinkDelegate;
import com.viaoa.object.OAObject;
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

	int type;
	Hub hub;
	String propertyName;
	Object prevValue, newValue;
	Object object;
	String presentationName;
	boolean bCanUndo = true;
	int prevPos, newPos;
	boolean bAllowReplace;
	boolean bAllowRedo = true;
	boolean wasChanged;

	public static final int ADD = 0;
	public static final int REMOVE = 1;
	public static final int MOVE = 2;
	public static final int INSERT = 3;
	public static final int CHANGEAO = 4;
	public static final int PROPCHANGE = 5;
	public static final int PROPERTYCHANGE = 5;
	public static final int HOLDER = 6;

	private OAUndoableEdit() {
	}

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
				c = h2.getObjectClass();
				s = OAString.convertToHungarian(c.getSimpleName());
				String s2 = HubLinkDelegate.getLinkToProperty(hub);
				presentationName = "change to " + s + " " + s2;
			} else {
				presentationName = "change selected " + s;
			}
		}
		oe.presentationName = presentationName;
		return oe;
	}

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

	public static OAUndoableEdit createUndoablePropertyChange(String presentationName, Object obj, String prop, Object prevValue,
			Object newValue) {
		return createUndoablePropertyChange(presentationName, obj, prop, prevValue, newValue, true);
	}

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

	public static OAUndoableEdit createUndoable(String presentationName) {
		OAUndoableEdit oe = new OAUndoableEdit();
		oe.type = HOLDER;
		oe.presentationName = presentationName;
		return oe;
	}

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

	public void setName(String name) {
		presentationName = name;
	}

	public String getName() {
		return presentationName;
	}

	public void setPresentationName(String name) {
		presentationName = name;
	}

	public String getPresentationName() {
		return presentationName;
	}

	public boolean canUndo() {
		return bCanUndo;
	}

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

	public boolean canRedo() {
		return !bCanUndo && bAllowRedo;
	}

	public String getUndoPresentationName() {
		return "Undo " + presentationName;
	}

	public String getRedoPresentationName() {
		return "Redo " + presentationName;
	}

	public boolean isSignificant() {
		return true;
	}

	public boolean addEdit(UndoableEdit anEdit) {
		return false;
	}

	public void die() {
	}

	public boolean replaceEdit(UndoableEdit anEdit) {
		return (anEdit != null && (anEdit instanceof OAUndoableEdit) && ((OAUndoableEdit) anEdit).bAllowReplace && this.equals(anEdit));
	}

	/** if true, an event can replace a previous matching event that is equals() */
	public void setAllowReplace(boolean b) {
		bAllowReplace = b;
	}

	public boolean getAllowReplace() {
		return bAllowReplace;
	}

	public void setAllowRedo(boolean b) {
		bAllowRedo = b;
	}

	public boolean getAllowRedo() {
		return bAllowRedo;
	}

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

	@Override
	public int hashCode() {
		return (type + "." + object).hashCode();
	}
}
