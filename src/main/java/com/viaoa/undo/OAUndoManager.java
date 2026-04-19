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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import com.viaoa.graph.OAGraphInternal;
import com.viaoa.runtime.OARemoteThreadService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;

/**
 * OA-specific extension of {@link javax.swing.undo.UndoManager} providing
 * undo/redo support for Hub and OAObject operations.
 * <p>
 * {@code OAUndoManager} centralizes undo logic for OA-based applications and
 * supplies additional features on top of Swing's {@code UndoManager}:
 * <ul>
 *   <li>global, shared undo manager via {@link #createUndoManager()},</li>
 *   <li>compound edits for grouping multiple operations,</li>
 *   <li>integration with {@code OAThreadLocalDelegate} for automatic
 *       property-change capture,</li>
 *   <li>thread-local ignore counters to suppress recursive undo capture,</li>
 *   <li>optional verbose logging,</li>
 *   <li>ability to disable all undo tracking temporarily.</li>
 * </ul>
 *
 * <h2>Compound Edits</h2>
 * {@link #startCompoundEdit(String)} and {@link #endCompoundEdit()} allow
 * multiple related edits to be grouped into a single user-visible undo item.
 * This is particularly useful for:
 * <ul>
 *   <li>bulk modifications,</li>
 *   <li>multi-step UI operations,</li>
 *   <li>property-change sequences initiated by controllers.</li>
 * </ul>
 *
 * <h2>Property-Change Capture</h2>
 * Methods {@link #startCompoundEditForPropertyChanges(String)} and
 * {@link #endCompoundEditForPropertyChanges()} work with the OA thread-local
 * undo delegate to capture OAObject property changes automatically.
 *
 * <h2>Ignore Logic</h2>
 * {@code setIgnore()} and related methods maintain a per-thread counter that
 * suppresses undo tracking for internal operations, ensuring that undo events
 * are only generated for user actions.
 *
 * <h2>Integration with Sync and Remote Threads</h2>
 * Remote sync threads are automatically ignored via
 * {@link com.viaoa.sync.OASyncDelegate#callSyncIsSingleUser()} and
 * {@link com.viaoa.remote.OARemoteThreadDelegate#isRemoteThread()},
 * preventing remote updates from being recorded as undoable user actions.
 *
 * <p>
 * This class forms the backbone of all UI-level undo/redo in OA applications.
 */
public class OAUndoManager extends UndoManager {

	private static Logger LOG = Logger.getLogger(OAUndoManager.class.getName());

	/**
	 * Thread-local ignore counter used to suppress undo tracking.
	 * <p>
	 * Each thread maintains a counter indicating how many times undo capture
	 * has been disabled. A value greater than zero causes undo events to be ignored
	 * for that thread.
	 */
	protected static final Map<Thread, Integer> hmThreadCounter = new HashMap(); 
	
	/**
	 * Singleton instance of the global {@code OAUndoManager}.
	 * <p>
	 * This shared instance centralizes undo/redo behavior across the application.
	 */
	protected static OAUndoManager undoManager;
	
	/**
	 * Flag indicating whether verbose logging is enabled.
	 * <p>
	 * When enabled, undoable edits added to the manager are printed
	 * to standard output for debugging purposes.
	 */
	protected static boolean bVerbose;
	
	/**
	 * Global flag to temporarily ignore all undo events.
	 * <p>
	 * When set to {@code true}, all undo tracking is disabled regardless
	 * of thread-local ignore counters.
	 */
	protected static boolean bIgnoreAll;
	
	/**
	 * The currently active compound edit, if any.
	 * <p>
	 * Compound edits group multiple undoable edits into a single
	 * user-visible undo operation.
	 */
	private static CompoundEdit compoundEdit;
	
	/**
	 * Reference to the last undoable edit that was added.
	 * <p>
	 * This field was historically used for duplicate suppression
	 * and is retained for backward compatibility.
	 */
	private static UndoableEdit lastEdit;

	/**
	 * Protected constructor to enforce controlled creation.
	 * <p>
	 * Instances are created through {@link #createUndoManager()}
	 * to ensure a single shared undo manager.
	 */
	protected OAUndoManager() {

	}

	/**
	 * Creates or returns the global {@code OAUndoManager} instance.
	 * <p>
	 * This method lazily initializes the undo manager and should be
	 * called during application startup.
	 *
	 * @return the global {@code OAUndoManager}
	 */
	public static OAUndoManager createUndoManager() {
		if (undoManager == null) {
			undoManager = new OAUndoManager();
		}
		return undoManager;
	}

	/**
	 * Returns the global {@code OAUndoManager} instance.
	 *
	 * @return the current {@code OAUndoManager}, or {@code null} if not created
	 */
	public static OAUndoManager getUndoManager() {
		return undoManager;
	}

	/**
	 * Enables or disables verbose undo logging.
	 *
	 * @param b {@code true} to enable verbose logging, {@code false} to disable
	 */
	public static void setVerbose(boolean b) {
		bVerbose = b;
	}

	/**
	 * Indicates whether verbose undo logging is enabled.
	 *
	 * @return {@code true} if verbose logging is enabled
	 */
	public static boolean getVerbose() {
		return bVerbose;
	}

	/**
	 * Starts a compound edit with a default presentation name.
	 * <p>
	 * All subsequent undoable edits are grouped into a single
	 * compound edit until {@link #endCompoundEdit()} is called.
	 */
	public static void startCompoundEdit() {
		startCompoundEdit("", true);
	}

	/**
	 * Starts a compound edit with the specified presentation name.
	 *
	 * @param presentationName the name displayed for the compound undo operation
	 */
	public static void startCompoundEdit(final String presentationName) {
		startCompoundEdit(presentationName, true);
	}

	/**
	 * Starts capturing OAObject property changes into a compound undoable edit.
	 * <p>
	 * This delegates to {@link OAThreadLocalDelegate} to automatically
	 * generate undoable property-change edits.
	 *
	 * @param presentationName the name displayed for the compound undo operation
	 */
	public static void startCompoundEditForPropertyChanges(final String presentationName) {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		srvcOAThreadLocal.startUndoable(presentationName);
		//startCompoundEdit(presentationName);
		//OARuntime.threadLocals().setCreateUndoablePropertyChanges(true);
	}

	/**
	 * Ends automatic capture of OAObject property changes.
	 * <p>
	 * This completes the compound undoable edit initiated by
	 * {@link #startCompoundEditForPropertyChanges(String)}.
	 */
	public static void endCompoundEditForPropertyChanges() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		srvcOAThreadLocal.endUndoable();
		//endCompoundEdit();
		//OARuntime.threadLocals().setCreateUndoablePropertyChanges(false);
	}

	/**
	 * Starts a compound edit with control over redo availability.
	 * <p>
	 * If a compound edit is already active, it will be ended
	 * before starting the new one.
	 *
	 * @param presentationName the name displayed for the compound undo operation
	 * @param bCanRedoThis {@code true} if redo should be allowed for this compound edit
	 */
	public static void startCompoundEdit(final String presentationName, final boolean bCanRedoThis) {
		if (undoManager == null) {
			return;
		}
		if (getIgnore()) {
			return;
		}
		if (compoundEdit != null) {
			LOG.warning("compoundEdit is not null, presentationName=" + compoundEdit.getPresentationName()
					+ ", will end before starting this new compoundEdit=" + presentationName);
			endCompoundEdit();
		}

		compoundEdit = new CompoundEdit() {
			public String getPresentationName() {
				return presentationName;
			}

			@Override
			public String getUndoPresentationName() {
				return "Undo " + presentationName;
			}

			@Override
			public String getRedoPresentationName() {
				return "Redo " + presentationName;
			}

			@Override
			public boolean canRedo() {
				return bCanRedoThis;
			}
		};
	}

	/**
	 * Ends the current compound edit and adds it to the undo manager.
	 * <p>
	 * If the compound edit is significant, it becomes a single
	 * user-visible undo operation.
	 */
	public static void endCompoundEdit() {
		if (undoManager == null || compoundEdit == null) {
			return;
		}
		if (getIgnore()) {
			return;
		}
		compoundEdit.end();

		if (compoundEdit.isSignificant()) {
			undoManager.addEdit(compoundEdit);
		}
		compoundEdit = null;
	}

	/**
	 * Indicates whether a compound edit is currently active.
	 *
	 * @return {@code true} if a compound edit is in progress
	 */
	public static boolean isInCompoundEdit() {
		if (getIgnore()) {
			return false;
		}
		return (undoManager != null && compoundEdit != null);
	}

	/**
	 * Cancels the current compound edit without adding it to the undo manager.
	 * <p>
	 * This discards any grouped undoable edits collected so far.
	 */
	public static void cancelCompoundEdit() {
		compoundEdit = null;
	}

	/**
	 * Adds a single undoable edit to the undo manager.
	 * <p>
	 * If no undo manager exists, or the edit is {@code null},
	 * the request is ignored.
	 *
	 * @param anEdit the undoable edit to add
	 */
	public static void add(UndoableEdit anEdit) {
		if (anEdit == null || undoManager == null) {
			return;
		}
		undoManager.addEdit(anEdit);
	}

	/* *qqqqqqq 20100124 not used?
	public static void add(UndoableEdit anEdit, boolean bIgnoreDuplicate) {
	    if (anEdit == null || undoManager == null) return;
	    if (bIgnoreDuplicate && anEdit.equals(lastEdit)) return;
	    lastEdit = anEdit;
	    undoManager.addEdit(anEdit);
	}
	**/

	/**
	 * Adds an array of undoable edits as a grouped operation.
	 * <p>
	 * If a compound edit is active, each edit is added to it.
	 * Otherwise, the edits are wrapped into a new {@link CompoundEdit}
	 * and added as a single undoable action.
	 *
	 * @param anEdits the array of undoable edits to add
	 */
	public static void add(UndoableEdit[] anEdits) {
		if (getIgnore()) {
			return;
		}
		if (anEdits != null && undoManager != null && anEdits.length > 0) {
			if (compoundEdit != null) {
				for (int i = 0; i < anEdits.length; i++) {
					undoManager.compoundEdit.addEdit(anEdits[i]);
				}
			} else {
				CompoundEdit ce = new CompoundEdit();
				for (int i = 0; i < anEdits.length; i++) {
					ce.addEdit(anEdits[i]);
				}
				ce.end();
				undoManager.addEdit(ce);
			}
		}
	}

	/**
	 * Adds an undoable edit to the manager, respecting ignore and compound logic.
	 * <p>
	 * When a compound edit is active, the edit is added to it instead
	 * of directly to the undo manager.
	 *
	 * @param anEdit the undoable edit to add
	 * @return {@code true} if the edit was accepted
	 */
	public synchronized boolean addEdit(UndoableEdit anEdit) {
		if (getIgnore()) {
			return false;
		}
		if (bVerbose) {
			System.out.println("OAUndoManager.addEdit " + anEdit.getPresentationName());
		}

		if (compoundEdit != null && anEdit != compoundEdit) {
			compoundEdit.addEdit(anEdit);
			return true;
		}
		return super.addEdit(anEdit);
	}

	/**
	 * Enables or disables undo tracking for the current thread.
	 * <p>
	 * This method increments or decrements the thread-local ignore counter.
	 *
	 * @param b {@code true} to ignore undo tracking, {@code false} to resume
	 */
	public static void setIgnore(boolean b) {
		setIgnore(b, false);
	}

	/**
	 * Enables or disables undo tracking for the current thread with reset control.
	 * <p>
	 * The thread-local ignore counter can optionally be reset before
	 * applying the new ignore state.
	 *
	 * @param b {@code true} to ignore undo tracking, {@code false} to resume
	 * @param bResetToZero {@code true} to reset the counter before updating
	 */
	public static void setIgnore(boolean b, boolean bResetToZero) {
		if (undoManager != null) {
			int i = 0;
			Thread t = Thread.currentThread();
			if (!bResetToZero) {
				Integer ii = (Integer) hmThreadCounter.get(t);
				if (ii != null) {
					i = ii.intValue();
				}
			}
			if (b) {
				i++;
			} else {
				i--;
			}

			if (i > 0) {
				hmThreadCounter.put(t, i);
			} else {
				hmThreadCounter.remove(t);
			}
		}
	}

	/**
	 * Convenience method to ignore undo tracking for the current thread.
	 * <p>
	 * Equivalent to calling {@link #setIgnore(boolean)} with {@code true}.
	 */
	public static void ignore() {
		setIgnore(true);
	}

	/**
	 * Convenience method to enable or disable undo tracking.
	 * <p>
	 * Equivalent to calling {@link #setIgnore(boolean)}.
	 *
	 * @param b {@code true} to ignore undo tracking, {@code false} otherwise
	 */
	public static void ignore(boolean b) {
		setIgnore(b);
	}

	/**
	 * Determines whether undo tracking is currently ignored.
	 * <p>
	 * Undo tracking is suppressed if:
	 * <ul>
	 *   <li>no undo manager exists,</li>
	 *   <li>global ignore-all is enabled,</li>
	 *   <li>the current thread has a positive ignore counter,</li>
	 *   <li>the current thread is a remote or sync thread.</li>
	 * </ul>
	 *
	 * @return {@code true} if undo tracking should be ignored
	 */
	public static boolean getIgnore() {
		if (undoManager == null) {
			return true;
		}
		if (bIgnoreAll) {
			return true;
		}

		int i = 0;
		Thread t = Thread.currentThread();
		Integer ii = (Integer) hmThreadCounter.get(t);
		if (ii != null) {
			i = ii.intValue();
		}
		if (i > 0) {
			return true;
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph();
		if (!og.syncInternal().isSingleUser()) {
			final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
			if (srvcOARemoteThread.isRemoteThread()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Enables or disables global suppression of all undo events.
	 *
	 * @param b {@code true} to ignore all undo events
	 */
	public static void setIgnoreAll(boolean b) {
		bIgnoreAll = b;
	}

	/**
	 * Undoes the most recent significant edit.
	 * <p>
	 * All undo tracking is temporarily disabled while the undo
	 * operation is performed to prevent recursive capture.
	 *
	 * @throws CannotUndoException if no undoable edit is available
	 */
	@Override
	public synchronized void undo() throws CannotUndoException {
		try {
			bIgnoreAll = true;
			super.undo();
		} finally {
			bIgnoreAll = false;
		}
	}
}
