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

/**
 * Configuration container describing UI and behavioral permissions for
 * a model class of {@link OAObject}.
 *
 * <p>OAObjectModel is not persistent; it defines the user-interface and
 * workflow flags used by generated or runtime components (e.g. OA-Web,
 * OA-JFC) to determine what actions are available for a given object
 * type—add, edit, delete, search, etc.</p>
 *
 * <p><b>Primary Roles</b>:
 * <ul>
 *   <li>Maintain per-feature boolean flags (e.g. {@code allowAdd},
 *       {@code allowDelete}, {@code allowSearch}).</li>
 *   <li>Provide {@link #defaultAll(boolean)} to enable/disable everything
 *       in bulk.</li>
 *   <li>Hold display-name metadata used in UI captions.</li>
 *   <li>Support JFC-specific behaviors via {@code forJfc} flag.</li>
 * </ul>
 *
 * <p>It effectively acts as a “policy object” guiding OA’s generated UI layers.</p>
 */
//@OAClass (addToCache=false, localOnly=true, useDataSource=false)
public class OAObjectModel {
	//was: public class OAObjectModel extends OAObject {

	public static final String P_DisplayName = "DisplayName";
	public static final String P_DisplayNamePlural = "DisplayNamePlural";

	protected boolean bAllowGotoList;
	protected boolean bAllowGotoEdit;
	protected boolean bAllowSearch;
	protected boolean bAllowHubSearch;
	protected boolean bAllowMultiSelect;
	protected boolean bAllowTableFilter; // allow the table columns to include input filters
	protected boolean bAllowTableSorting;

	protected boolean bAllowAdd;
	protected boolean bAllowNew;
	// protected boolean bAllowAutoCreate;

	protected boolean bAllowSave;
	protected boolean bAllowRemove;
	protected boolean bAllowDelete;
	protected boolean bAllowClear; // set to null / set AO to null
	protected boolean bAllowRecursive;
	protected boolean bAllowFilter; // to include hub filters
	protected boolean bAllowDownload;
	protected boolean bAllowRefresh;

	protected boolean bAllowCut;
	protected boolean bAllowCopy;
	protected boolean bAllowPaste;
	protected boolean bViewOnly;
	protected boolean bCreateUI;
	protected boolean bAllowMove;

	// if  true, then use a splitPane to show children that are link=Many
	protected boolean bAllowChildrenSplitPanel;  
	
	
	protected String displayName;
	protected String pluralDisplayName;

	// flag to know if this model is used by JFC UI apps, in which case it can use Hubs that are loaded in swingWorker thread
	protected boolean bForJfc;
	
	/**
	 * Constructs a new model configuration instance and initializes all
	 * feature flags to their default values. These defaults enable common
	 * UI operations such as navigation, search, add, save, delete, copy/paste,
	 * and table filtering/sorting, while disabling actions like hub search
	 * and remove.
	 */
	public OAObjectModel() {
		// if (isLoading()) return;
		setAllowGotoList(true);
		setAllowGotoEdit(true);
		setAllowSearch(true);
		setAllowHubSearch(false);
		setAllowAdd(true);
		setAllowNew(true);
		setAllowRemove(false);
		setAllowSave(true);
		setAllowDelete(true);
		setAllowClear(true);
		setAllowCut(true);
		setAllowCopy(true);
		setAllowPaste(true);
		setAllowMultiSelect(false);
		setAllowTableFilter(true);
		setAllowTableSorting(true);
		setAllowFilter(true);
		setAllowDownload(false);
		setCreateUI(true);
	}

	/**
	 * Sets all configuration flags to the specified value. This provides a
	 * bulk-operation mechanism to enable or disable all supported UI and
	 * workflow features for the model.
	 *
	 * @param bOnOrOff the value to assign to every configuration flag
	 */
	public void defaultAll(boolean bOnOrOff) {
		setAllowGotoList(bOnOrOff);
		setAllowGotoEdit(bOnOrOff);
		setAllowSearch(bOnOrOff);
		setAllowHubSearch(bOnOrOff);
		setAllowAdd(bOnOrOff);
		setAllowNew(bOnOrOff);
		setAllowRemove(bOnOrOff);
		setAllowSave(bOnOrOff);
		setAllowDelete(bOnOrOff);
		setAllowClear(bOnOrOff);
		setAllowCut(bOnOrOff);
		setAllowCopy(bOnOrOff);
		setAllowPaste(bOnOrOff);
		setAllowMultiSelect(bOnOrOff);
		setAllowTableFilter(bOnOrOff);
		setAllowTableSorting(bOnOrOff);
		setAllowFilter(bOnOrOff);
		setAllowDownload(bOnOrOff);
		setCreateUI(bOnOrOff);
	}

	/**
	 * Returns the singular display name associated with this model, used
	 * by UI components for captions and labels.
	 *
	 * @return the display name, or {@code null} if unset
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Updates the singular display name for this model. The assignment
	 * replaces any prior value. No property-change event is fired.
	 *
	 * @param newValue the new display name
	 */
	public void setDisplayName(String newValue) {
		String old = displayName;
		this.displayName = newValue;
		//        firePropertyChange(P_DisplayName, old, this.displayName);
	}

	/**
	 * Returns the pluralized display name associated with this model, used
	 * by UI components when referring to collections of the object type.
	 *
	 * @return the plural display name, or {@code null} if unset
	 */
	public String getPluralDisplayName() {
		return pluralDisplayName;
	}

	/**
	 * Sets the plural display name for this model. The assignment replaces
	 * any previous value. No property-change event is fired.
	 *
	 * @param newValue the new plural display name
	 */
	public void setPluralDisplayName(String newValue) {
		String old = pluralDisplayName;
		this.pluralDisplayName = newValue;
		//        firePropertyChange(P_DisplayNamePlural, old, this.displayNamePlural);
	}

	/**
	 * Indicates whether UI components are permitted to navigate to a list
	 * view for this model type.
	 *
	 * @return {@code true} if list-view navigation is allowed
	 */
	public boolean getAllowGotoList() {
		return bAllowGotoList;
	}

	/**
	 * Enables or disables list-view navigation for this model type.
	 *
	 * @param b {@code true} to allow list-view navigation
	 */
	public void setAllowGotoList(boolean b) {
		bAllowGotoList = b;
	}

	/**
	 * Indicates whether UI components are permitted to navigate to an edit
	 * view for this model type.
	 *
	 * @return {@code true} if edit-view navigation is allowed
	 */
	public boolean getAllowGotoEdit() {
		return bAllowGotoEdit;
	}

	/**
	 * Enables or disables edit-view navigation for this model type.
	 *
	 * @param b {@code true} to allow edit-view navigation
	 */
	public void setAllowGotoEdit(boolean b) {
		bAllowGotoEdit = b;
	}

	/**
	 * Enables or disables edit-view navigation for this model type.
	 *
	 * @param b {@code true} to allow edit-view navigation
	 */
	public boolean getAllowSearch() {
		return bAllowSearch;
	}

	/**
	 * Enables or disables search operations for this model.
	 *
	 * @param b {@code true} to allow searching
	 */
	public void setAllowSearch(boolean b) {
		bAllowSearch = b;
	}

	/**
	 * Enables or disables search operations for this model.
	 *
	 * @param b {@code true} to allow searching
	 */
	public boolean getAllowHubSearch() {
		return bAllowHubSearch;
	}

	/**
	 * Enables or disables hub-based search operations for this model type.
	 *
	 * @param b {@code true} to allow hub searches
	 */
	public void setAllowHubSearch(boolean b) {
		bAllowHubSearch = b;
	}

	/**
	 * Indicates whether new instances of this model type may be created.
	 *
	 * @return {@code true} if object creation is allowed
	 */
	public boolean getAllowAdd() {
		return bAllowAdd;
	}

	/**
	 * Enables or disables the ability to create new instances of this
	 * model type.
	 *
	 * @param b {@code true} to permit additions
	 */
	public void setAllowAdd(boolean b) {
		bAllowAdd = b;
	}

	/**
	 * Returns whether creating a new instance of this model type is permitted.
	 * <p>
	 * This flag is typically consumed by UI or workflow layers (OA-Web, OA-JFC)
	 * to determine whether “New” actions should be enabled for the associated
	 * {@link OAObject} type.
	 * </p>
	 *
	 * @return {@code true} if the UI and workflow should allow creation of a new object;
	 *         {@code false} otherwise.
	 */
	public boolean getAllowNew() {
		return bAllowNew;
	}

	/**
	 * Sets whether creating a new instance of this model type is permitted.
	 * <p>
	 * UI and workflow layers (OA-Web, OA-JFC) use this flag to determine whether
	 * “New” actions should be enabled for the associated {@link OAObject} type.
	 * </p>
	 *
	 * @param b {@code true} to allow creation of new objects; {@code false} to disable it.
	 */
	public void setAllowNew(boolean b) {
		bAllowNew = b;
	}

	/**
	 * Returns whether save operations are permitted for this model type.
	 *
	 * @return {@code true} if saving is allowed
	 */
	public boolean getAllowSave() {
		return bAllowSave;
	}

	/**
	 * Enables or disables save operations for this model type.
	 *
	 * @param b {@code true} to allow saving
	 */
	public void setAllowSave(boolean b) {
		bAllowSave = b;
	}

	/**
	 * Enables or disables delete operations for objects of this model type.
	 *
	 * @param b {@code true} to permit deletion
	 */
	public boolean getAllowRemove() {
		return bAllowRemove;
	}

	/**
	 * Enables or disables remove operations for this model type.
	 *
	 * @param b {@code true} to allow removal
	 */
	public void setAllowRemove(boolean b) {
		bAllowRemove = b;
	}

	/**
	 * Indicates whether delete operations are permitted for objects of this
	 * model type.
	 *
	 * @return {@code true} if deletion is allowed
	 */
	public boolean getAllowDelete() {
		return bAllowDelete;
	}

	/**
	 * Enables or disables delete operations for objects of this model type.
	 *
	 * @param b {@code true} to permit deletion
	 */
	public void setAllowDelete(boolean b) {
		bAllowDelete = b;
	}

	public boolean getAllowClear() {
		return bAllowClear;
	}

	public void setAllowClear(boolean b) {
		bAllowClear = b;
	}

	public boolean getAllowRecursive() {
		return bAllowRecursive;
	}

	public void setAllowRecursive(boolean b) {
		bAllowRecursive = b;
	}

	/**
	 * Returns whether cut operations are permitted for this model type.
	 *
	 * @return {@code true} if cutting is allowed
	 */
	public boolean getAllowCut() {
		return bAllowCut;
	}

	/**
	 * Enables or disables cut operations for this model type.
	 *
	 * @param b {@code true} to permit cutting
	 */
	public void setAllowCut(boolean b) {
		bAllowCut = b;
	}

	/**
	 * Indicates whether copy operations are enabled for this model type.
	 *
	 * @return {@code true} if copying is allowed
	 */
	public boolean getAllowCopy() {
		return bAllowCopy;
	}

	/**
	 * Enables or disables copy operations for this model type.
	 *
	 * @param b {@code true} to allow copying
	 */
	public void setAllowCopy(boolean b) {
		bAllowCopy = b;
	}

	/**
	 * Returns whether paste operations are permitted for this model type.
	 *
	 * @return {@code true} if pasting is allowed
	 */
	public boolean getAllowPaste() {
		return bAllowPaste;
	}

	/**
	 * Enables or disables paste operations for this model type.
	 *
	 * @param b {@code true} to allow pasting
	 */
	public void setAllowPaste(boolean b) {
		bAllowPaste = b;
	}

	/**
	 * Returns whether this model is restricted to view-only mode.
	 * <p>
	 * When enabled, UI layers should suppress all editing actions and present
	 * the object type in a strictly read-only manner.
	 * </p>
	 *
	 * @return {@code true} if the model is view-only; {@code false} otherwise.
	 */
	public boolean getViewOnly() {
		return bViewOnly;
	}

	/**
	 * Sets whether this model should operate in view-only mode.
	 * <p>
	 * When enabled, UI layers must disable all editing, modification, and
	 * object-changing actions for this model type.
	 * </p>
	 *
	 * @param b {@code true} to make the model read-only; {@code false} to allow edits.
	 */
	public void setViewOnly(boolean b) {
		bViewOnly = b;
	}

	/**
	 * Indicates whether UI components should be created for this model type.
	 * <p>
	 * Generated UI frameworks use this flag to determine whether screens,
	 * editors, and related widgets should be instantiated.
	 * </p>
	 *
	 * @return {@code true} if UI creation is enabled; {@code false} otherwise.
	 */
	public boolean getCreateUI() {
		return bCreateUI;
	}

	/**
	 * Enables or disables creation of UI components for this model type.
	 * <p>
	 * When disabled, generated UI layers should not create screens or editors
	 * for the associated {@link OAObject} type.
	 * </p>
	 *
	 * @param b {@code true} to allow UI creation; {@code false} to disable it.
	 */
	public void setCreateUI(boolean b) {
		bCreateUI = b;
	}

	/**
	 * Returns whether multi-selection is permitted for this model type.
	 * <p>
	 * UI lists and tables use this flag to determine whether users may select
	 * multiple rows simultaneously.
	 * </p>
	 *
	 * @return {@code true} if multi-select is allowed; {@code false} otherwise.
	 */
	public boolean getAllowMultiSelect() {
		return bAllowMultiSelect;
	}

	/**
	 * Returns whether multi-selection is permitted for this model type.
	 * <p>
	 * UI lists and tables use this flag to determine whether users may select
	 * multiple rows simultaneously.
	 * </p>
	 *
	 * @return {@code true} if multi-select is allowed; {@code false} otherwise.
	 */
	public void setAllowMultiSelect(boolean b) {
		bAllowMultiSelect = b;
	}

	/**
	 * Indicates whether table-column filtering is enabled for this model type.
	 * <p>
	 * When enabled, UI tables may include inline filter controls for narrowing
	 * visible rows.
	 * </p>
	 *
	 * @return {@code true} if column filtering is permitted; {@code false} otherwise.
	 */
	public boolean getAllowTableFilter() {
		return bAllowTableFilter;
	}

	/**
	 * Enables or disables table-column filtering for this model type.
	 * <p>
	 * Affects whether UI table components expose filter widgets on columns.
	 * </p>
	 *
	 * @param b {@code true} to allow filtering; {@code false} to disable it.
	 */
	public void setAllowTableFilter(boolean b) {
		bAllowTableFilter = b;
	}

	/**
	 * Indicates whether table sorting is permitted for this model type.
	 * <p>
	 * When enabled, UI tables may allow users to click column headers to sort rows.
	 * </p>
	 *
	 * @return {@code true} if sorting is allowed; {@code false} otherwise.
	 */
	public boolean getAllowTableSorting() {
		return bAllowTableSorting;
	}

	/**
	 * Sets whether table sorting is permitted for this model type.
	 * <p>
	 * UI layers use this to enable or disable sortable table columns.
	 * </p>
	 *
	 * @param b {@code true} to allow sorting; {@code false} to turn off sorting.
	 */
	public void setAllowTableSorting(boolean b) {
		bAllowTableSorting = b;
	}

	/**
	 * Returns whether hub-level filtering is allowed for this model type.
	 * <p>
	 * This controls whether dynamic {@code HubFilter} instances may be applied
	 * when presenting or manipulating collections of {@link OAObject} instances.
	 * </p>
	 *
	 * @return {@code true} if filtering is allowed; {@code false} otherwise.
	 */
	public boolean getAllowFilter() {
		return bAllowFilter;
	}

	/**
	 * Enables or disables hub-level filtering for this model type.
	 * <p>
	 * When enabled, UI and workflow layers may apply {@code HubFilter}
	 * instances to refine visible or usable subsets of related objects.
	 * </p>
	 *
	 * @param b {@code true} to allow filtering; {@code false} to disable it.
	 */
	public void setAllowFilter(boolean b) {
		bAllowFilter = b;
	}

	/**
	 * Indicates whether this model is intended for use within OA-JFC UI environments.
	 * <p>
	 * When enabled, the UI layer may leverage JFC-specific behaviors, including
	 * the use of Hubs loaded within SwingWorker threads.
	 * </p>
	 *
	 * @return {@code true} if this model is used in JFC contexts; {@code false} otherwise.
	 */
	public boolean getForJfc() {
		return bForJfc;
	}

	/**
	 * Specifies whether this model will be used within OA-JFC UI environments.
	 * <p>
	 * This controls JFC-specific UI behaviors, such as background-loaded Hubs
	 * and Swing threading considerations.
	 * </p>
	 *
	 * @param b {@code true} if used for JFC; {@code false} otherwise.
	 */
	public void setForJfc(boolean b) {
		this.bForJfc = b;
	}

	/**
	 * Returns whether downloading/exporting data is permitted for this model type.
	 * <p>
	 * UI layers use this to determine whether features such as CSV or Excel export
	 * should be enabled.
	 * </p>
	 *
	 * @return {@code true} if download/export actions are allowed; {@code false} otherwise.
	 */
	public boolean getAllowDownload() {
		return bAllowDownload;
	}

	/**
	 * Sets whether downloading/exporting data is allowed for this model type.
	 * <p>
	 * When enabled, UI layers may expose export functions such as CSV, Excel,
	 * or other file-based outputs.
	 * </p>
	 *
	 * @param b {@code true} to allow downloads; {@code false} to disable them.
	 */
	public void setAllowDownload(boolean b) {
		bAllowDownload = b;
	}

	/**
	 * Indicates whether move operations are permitted for this model type.
	 * <p>
	 * Move operations typically involve reorganizing an object's position within
	 * a collection or hierarchy.
	 * </p>
	 *
	 * @return {@code true} if move operations are allowed; {@code false} otherwise.
	 */
	public boolean getAllowMove() {
		return bAllowMove;
	}

	/**
	 * Enables or disables move operations for this model type.
	 * <p>
	 * UI layers use this flag to determine whether objects may be repositioned
	 * within collections or hierarchical structures.
	 * </p>
	 *
	 * @param b {@code true} to allow move operations; {@code false} to disable them.
	 */
	public void setAllowMove(boolean b) {
		bAllowMove = b;
	}

	/**
	 * Returns whether refresh actions are permitted for this model type.
	 * <p>
	 * A refresh action typically reloads or synchronizes the underlying data
	 * for objects of this type.
	 * </p>
	 *
	 * @return {@code true} if refresh operations are permitted; {@code false} otherwise.
	 */
	public boolean getAllowRefresh() {
		return bAllowRefresh;
	}

	/**
	 * Enables or disables refresh operations for this model type.
	 * <p>
	 * When enabled, UI layers may expose “Refresh” commands to reload data
	 * or synchronize with a backing source.
	 * </p>
	 *
	 * @param b {@code true} to allow refresh operations; {@code false} to disable them.
	 */
	public void setAllowRefresh(boolean b) {
		bAllowRefresh = b;
	}
	
	/**
	 * Indicates whether child collections (link=Many) should be displayed in a split-panel layout.
	 * <p>
	 * When enabled, UI layers may use a split panel to separate parent and child lists
	 * for improved navigation and visibility.
	 * </p>
	 *
	 * @return {@code true} if split-panel child displays are allowed; {@code false} otherwise.
	 */
	public boolean getAllowChildrenSplitPanel() {
		return bAllowChildrenSplitPanel;
	}

	/**
	 * Sets whether child collections (link=Many) should be displayed using a split-panel layout.
	 * <p>
	 * When enabled, UI layers may present parent and child collections in a divided
	 * interface region for improved clarity and navigation.
	 * </p>
	 *
	 * @param b {@code true} to enable split-panel child displays; {@code false} to disable them.
	 */
	public void setAllowChildrenSplitPanel(boolean b) {
		bAllowChildrenSplitPanel = b;
	}
}
