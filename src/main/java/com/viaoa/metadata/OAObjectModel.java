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
package com.viaoa.metadata;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.copy.HubCopy;
import com.viaoa.hub.view.HubCombined;
import com.viaoa.hub.view.HubFlattened;
import com.viaoa.hub.view.HubGroupBy;
import com.viaoa.metadata.OAObjectSearchModel.Type;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

//qqqqqq todo: add hubNewObjectWizard

//qqqqqqqqqq have displayName use linkPropertyName qqqqqqqqqqqqqq

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
/**
 * UI and tooling metadata for an OAObject type.
 * <p>
 * This model describes display labels, table columns, search behavior, view
 * options, and other generated-application configuration associated with an
 * OAObject class. It complements {@link OAObjectInfo}, which owns the core
 * runtime metadata for identity, properties, links, rules, and persistence
 * behavior.
 * </p>
 */
public class OAObjectModel<T extends OAObject> {
    final OAObjectModelIR<T> ir;
    
    private Hub<T> hubView; // shared with active hub

    protected OAObjectSearchModel<T> searchModel;
    private HubListener<T> hlHubContext; 

    protected Hub<T> hubMultiSelect; 
    
    protected OAFinder<?, T> finder; 
    
    protected List<OAObjectSearchModel<T>> alSearchModel;
    
    /**
     * Constant property-name identifier for the singular display-name field. Used
     * by UI layers and reflective components to reference the property without
     * relying on hard-coded string literals.
     */
    public static final String P_DisplayName = "DisplayName";

    /**
     * Constant property-name identifier for the plural display-name field. Enables
     * UI frameworks and model-driven components to reference the plural form
     * without using literal strings.
     */
    public static final String P_DisplayNamePlural = "DisplayNamePlural";

    /**
     * Indicates whether navigation to a list view is permitted for this model type.
     * Used by UI frameworks to enable or disable list access.
     */
    protected boolean bAllowGotoList;

    /**
     * Determines whether navigation to an edit view is allowed for this model type.
     * Guides UI routing and workflow actions.
     */
    protected boolean bAllowGotoEdit;

    /**
     * Controls whether standard search operations are allowed for this model type.
     * Affects UI search widgets and workflow queries.
     */
    protected boolean bAllowSearch;

    /**
     * Indicates whether hub-based search is permitted. Used for deeper,
     * relationship-driven search operations across linked OA models.
     */
    protected boolean bAllowHubSearch;

    /**
     * Specifies whether UI lists and tables may allow multi-row selection for this
     * model type.
     */
    protected boolean bAllowMultiSelect;

    /**
     * Controls whether UI table components may include inline filter controls for
     * this model type.
     */
    protected boolean bAllowTableFilter;

    /**
     * Determines whether table-column sorting is enabled for this model type within
     * UI components.
     */
    protected boolean bAllowTableSorting;

    /**
     * Indicates whether adding new instances of this model type is allowed. Used to
     * enable or disable “Add” or “New” UI actions.
     */
    protected boolean bAllowAdd;

    /**
     * Determines whether creating a brand-new instance of this model type is
     * permitted. Consumed by UI and workflow layers.
     */
    protected boolean bAllowNew;
    // protected boolean bAllowAutoCreate;

    /**
     * Specifies whether save operations are allowed for this model type. Affects
     * editor components and workflow persistence.
     */
    protected boolean bAllowSave;

    /**
     * Controls whether remove operations (non-delete removal from a hub or
     * relationship) are allowed for this model type.
     */
    protected boolean bAllowRemove;

    /**
     * Indicates whether delete operations are permitted for objects of this model
     * type.
     */
    protected boolean bAllowDelete;

    /**
     * Determines whether clearing a reference—setting an active object or property
     * to null—is allowed for this model type.
     */
    protected boolean bAllowClear;

    /**
     * Indicates whether recursive navigation or recursive UI behaviors are
     * permitted for this model type.
     */
    protected boolean bAllowRecursive;

    /**
     * Controls whether hub-level filtering operations are allowed for this model
     * type, enabling or disabling dynamic HubFilter usage.
     */
    protected boolean bAllowFilter;

    /**
     * Specifies whether download or export actions (CSV, Excel, etc.) are permitted
     * for this model type.
     */
    protected boolean bAllowDownload;

    /**
     * Determines whether refresh operations—such as reloading or syncing data—are
     * enabled for this model type.
     */
    protected boolean bAllowRefresh;

    /**
     * Indicates whether UI or workflow layers should allow cut operations for
     * objects of this model type.
     */
    protected boolean bAllowCut;

    /**
     * Indicates whether copy operations are permitted for this model type. Used by
     * UI layers to enable or disable “Copy” actions.
     */
    protected boolean bAllowCopy;

    /**
     * Specifies whether paste operations are allowed for this model type.
     * Determines whether copied objects or values may be inserted.
     */
    protected boolean bAllowPaste;

    /**
     * When true, all editing and modification actions for this model type should be
     * disabled, placing the model into read-only mode.
     */
    protected boolean bViewOnly;

    /**
     * Determines whether UI components should be generated or displayed for this
     * model type. When false, UI layers skip creating editors/screens.
     */
    protected boolean bCreateUI;

    /**
     * Controls whether move operations are permitted for this model type, typically
     * used when repositioning objects within collections.
     */
    protected boolean bAllowMove;

    /**
     * Indicates whether child collections (link=Many) should be displayed in a
     * split-panel layout in UI frameworks for improved navigation.
     */
    protected boolean bAllowChildrenSplitPanel;

    /**
     * Singular display name used by UI components for captions, titles, and labels
     * representing this model type.
     */
    protected String displayName;

    /**
     * Plural form of the display name, used when UI layers reference collections or
     * lists of this model type.
     */
    protected String pluralDisplayName;

    /**
     * Indicates whether this model is configured for use in OA-JFC (Swing)
     * applications, enabling JFC-specific behaviors such as background-loaded Hubs.
     */
    protected boolean bForJfc;

    public OAObjectModel(T obj) {
        this(null, new Hub<T>(obj), (Hub<T>) null);
    }
    public OAObjectModel(Class<T> ctype) {
        this(ctype, new Hub<T>(ctype), (Hub<T>) null);
    }
    public OAObjectModel(Class<T> ctype, boolean bUseForSearch) {
        this(ctype, null, null, null, null, bUseForSearch);
    }
    
    public OAObjectModel(Class<T> ctype, Hub<T> hubContext) {
        this(ctype, hubContext, null, (Hub) null, null, false);
    }
    public OAObjectModel(Class<T> ctype, Hub<T> hubContext, Hub<T> hubChoices) {
        this(ctype, hubContext, hubChoices, (Hub) null, null, false);
    }
    public OAObjectModel(final OAObjectModel<?> modelFrom, final String linkPropertyName) {
        this(null, null, null, modelFrom==null?null:modelFrom.getContextHub(), linkPropertyName, false);
    }
    public OAObjectModel(Class<T> ctype, final OAObjectModel<?> modelFrom, final String linkPropertyName) {
        this(ctype, null, null, modelFrom==null?null:modelFrom.getContextHub(), linkPropertyName, false);
    }
    public OAObjectModel(Hub<T> hubChoices, final OAObjectModel<?> modelFrom, final String linkPropertyName) {
        this(null, null, hubChoices, modelFrom==null?null:modelFrom.getContextHub(), linkPropertyName, false);
    }
    
    public OAObjectModel(Class<T> ctype, Hub<T> hubChoices, final OAObjectModel<?> modelFrom, final String linkPropertyName) {
        this(ctype, null, hubChoices, modelFrom==null?null:modelFrom.getContextHub(), linkPropertyName, false);
    }
    
    public OAObjectModel( Class<T> ctype, Hub<T> hubContext, Hub<T> hubChoices, OAObjectModel<?> modelFrom, String linkPropertyName) {
        this(ctype, hubContext, hubChoices, modelFrom==null?null:modelFrom.getContextHub(), linkPropertyName, false);
    }
    
    public OAObjectModel(Class<T> ctype, final Hub<?> hubFrom, final String linkPropertyName) {
        this(ctype, null, null, hubFrom, linkPropertyName, false);
    }
    
    public OAObjectModel(
        Class<T> ctype, 
        Hub<T> hubContext, Hub<T> hubChoices, 
        final Hub<?> hubFrom, final String linkPropertyName,
        boolean bUseForSearch
    ) {
        ir = new OAObjectModelIR<>(ctype, hubContext, hubChoices, hubFrom, linkPropertyName, bUseForSearch);
        init();
    }
    
    protected void init() {
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

    public Hub<T> getChoicesHub() {
        return ir.hubChoices;
    }

    public Hub<T> getContextHub() {
        return ir.hubContext;
    }

    public Hub<T> getHub() {
        return ir.hubContext;
    }
    
    public Hub<T> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<T>(ir.ctype);
            hlHubContext = new HubListenerAdapter<>(ir.hubContext) {
                @Override
                public void afterRemove(HubEvent<T> e) {
                    hubMultiSelect.remove(e.getObject());
                }
                @Override
                public void afterRemoveAll(HubEvent<T> e) {
                    hubMultiSelect.clear();
                }
                @Override
                public void afterNewList(HubEvent<T> e) {
                    hubMultiSelect.clear();
                }
            };
            ir.hubContext.addListener(hlHubContext);
        }
        return hubMultiSelect;
    }

    public boolean getUsesFilters() {
        return ir.bFiltered;
    }

    public boolean getUsesGroupBy() {
        return ir.bGroupBy;
    }

    public boolean getUsesRecursive() {
        return ir.bRecursive;
    }

    public Hub<T> getViewHub() {
        if (hubView == null) {
            hubView = new Hub<T>(ir.ctype);
            if (ir.bFiltered) hubView.setSharedHub(getFilterShareHub(), true);
            else if (ir.bGroupBy) hubView.setSharedHub(getGroupByShareHub(), true);
            else if (ir.bRecursive) hubView.setSharedHub(getRecursiveShareHub(), true);
            else hubView.setSharedHub(ir.hubContext, true);
        }
        return hubView;
    }

    public Hub<T> getRecursiveShareHub() {
        if (ir.bRecursive && ir.hubRecursiveShare == null) {
            ir.hubRecursiveShare = new Hub<T>(ir.ctype);
            ir.hubRecursiveShare.setSharedHub(getRecursiveRootHub(), true);
        }
        return ir.hubRecursiveShare;
    }

    public Hub<T> getRecursiveRootHub() {
        if (!ir.bRecursive) return null;
        if (ir.hubRecursiveRoot == null) {
            ir.hubRecursiveRoot = new Hub<T>(ir.ctype);
            ir.hubRecursiveRoot.setSharedHub(ir.hubContext, true);
        }
        return ir.hubRecursiveRoot;
    }

    public Hub<T> getRecursiveHub() {
        if (ir.bRecursive && ir.hubRecursive == null) {
            ir.hubRecursive = new Hub<T>(ir.ctype);
            ir.hubRecursive.setSharedHub(getRecursiveRootHub(), true);
        }
        return ir.hubRecursive;
    }

    public Hub<T> getRecursiveFlattenedHub() {
        if (!ir.bRecursive) return null;
        if (ir.hubRecursiveFlat == null) {
            ir.hubRecursiveFlat = new Hub<T>(ir.ctype);
            HubFlattened hf = new HubFlattened(ir.hubContext, ir.hubRecursiveFlat);
        }
        return ir.hubRecursiveFlat;
    }

    public void useRecursiveRootHub() {
        if (!ir.bRecursive) return;
        getRecursiveShareHub().setSharedHub(getRecursiveRootHub(), true);
    }

    public void useRecursiveFlattenedHub() {
        if (!ir.bRecursive) return;
        getRecursiveShareHub().setSharedHub(getRecursiveFlattenedHub(), true);
    }

    public Hub<T> getGroupByShareHub() {
        if (ir.bGroupBy && ir.hubGroupByShare == null) {
            ir.hubGroupByShare = new Hub<T>(ir.ctype);
            useGroupByHub(null);
        }
        return ir.hubGroupByShare;
    }

    public Hub<T> getGroupByHub(int pos) {
        if (!ir.bGroupBy || pos < 0 || pos >= ir.hubGroupBys.length) return null;

        if (ir.hubGroupBys[pos] == null) {
            final OA oa = OARuntime.oa(ir.ctype);
            OAObjectInfo oi = oa.info(ir.ctype);
            String[] ss = oi.getGroupByPaths();

            Hub<T> hx = ir.bRecursive ? getRecursiveShareHub() : getContextHub();
            HubGroupBy<T, ?> hgb = new HubGroupBy<>(hx, ss[pos]);
            ir.hubGroupBys[pos] = hgb;
        }
        return ir.hubGroupBys[pos].getDetailHub();
    }

    public void useGroupByHub(Hub<T> hub) {
        if (!ir.bGroupBy) return;
        if (hub == null) {
            if (ir.bRecursive) getGroupByShareHub().setSharedHub(getRecursiveShareHub(), true);
            else getGroupByShareHub().setSharedHub(getContextHub(), true);
        }
        else {
            for (HubGroupBy<T, ?> hgb : ir.hubGroupBys) {
                if (hgb == null) continue;
                Hub<T> hx = hgb.getDetailHub();
                if (hx != hub) continue;
                getGroupByShareHub().setSharedHub(hub, true);
                break;
            }
        }
    }

    public Hub<?> getGroupedByHub(int pos) {
        if (!ir.bGroupBy) return null;
        if (ir.hubGroupBys == null || pos < 0 || pos >= ir.hubGroupBys.length) return null;
        HubGroupBy<T, ?> hgb = ir.hubGroupBys[pos];
        if (hgb == null) {
            getGroupByHub(pos);
            hgb = ir.hubGroupBys[pos];
        }
        return hgb.getMasterHub();
    }

    public Hub<T> getHubToFilter() {
        Hub<T> h = null;
        if (ir.bGroupBy) h = getGroupByShareHub();
        else if (ir.bRecursive) h = getRecursiveShareHub();
        else h = getContextHub();
        return h;
    }

    public Hub<T> getFilterShareHub() {
        if (ir.bFiltered && ir.hubFilterShare == null) {
            ir.hubFilterShare = new Hub<T>(ir.ctype);
            useFilterHub(null);
        }
        return ir.hubFilterShare;
    }

    public Hub<T> getFilterShareHub(int pos) {
        if (!ir.bFiltered || pos < 0) return null;
        if (ir.hubFilters == null || pos >= ir.hubFilters.length) return null;
        return ir.hubFilters[pos];
    }

    public void useFilterHub(Hub<T> hub) {
        if (!ir.bFiltered) return;
        if (hub == null) {
            getFilterShareHub().setSharedHub(getHubToFilter(), true);
        }
        else {
            for (Hub<T> h : ir.hubFilters) {
                if (h != hub) continue;
                getFilterShareHub().setSharedHub(hub, true);
                break;
            }
        }
    }

    public OAFinder<?, T> getFinder() {
        return finder;
    }
    public <F extends OAObject> void setFinder(OAFinder<F, T> finder) {
        this.finder = finder;
    }
    
    
    public T getViewAO() {
        return getViewHub().getAO();
    }

    public HubCopy<T> createHubCopy() {
        Hub<T> hubx = new Hub<>(ir.ctype);
        HubCopy<T> hc = new HubCopy<>(getContextHub(), hubx, true);
        return hc;
    }

    public OAObjectModel<T> createCopy() {
        OAObjectModel<T> mod = new OAObjectModel(ir.ctype, createHubCopy().getHub(), (Hub<T>) null);
        return mod;
    }

    /**
     * Sets all configuration flags to the specified value. This provides a
     * bulk-operation mechanism to enable or disable all supported UI and workflow
     * features for the model.
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
     * Returns the singular display name associated with this model, used by UI
     * components for captions and labels.
     *
     * @return the display name, or {@code null} if unset
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Updates the singular display name for this model. The assignment replaces any
     * prior value. No property-change event is fired.
     *
     * @param newValue the new display name
     */
    public void setDisplayName(String newValue) {
        String old = displayName;
        this.displayName = newValue;
        // firePropertyChange(P_DisplayName, old, this.displayName);
    }

    /**
     * Returns the pluralized display name associated with this model, used by UI
     * components when referring to collections of the object type.
     *
     * @return the plural display name, or {@code null} if unset
     */
    public String getPluralDisplayName() {
        return pluralDisplayName;
    }

    /**
     * Sets the plural display name for this model. The assignment replaces any
     * previous value. No property-change event is fired.
     *
     * @param newValue the new plural display name
     */
    public void setPluralDisplayName(String newValue) {
        String old = pluralDisplayName;
        this.pluralDisplayName = newValue;
        // firePropertyChange(P_DisplayNamePlural, old, this.displayNamePlural);
    }

    /**
     * Indicates whether UI components are permitted to navigate to a list view for
     * this model type.
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
     * Indicates whether UI components are permitted to navigate to an edit view for
     * this model type.
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
     * Enables or disables the ability to create new instances of this model type.
     *
     * @param b {@code true} to permit additions
     */
    public void setAllowAdd(boolean b) {
        bAllowAdd = b;
    }

    /**
     * Returns whether creating a new instance of this model type is permitted.
     * <p>
     * This flag is typically consumed by UI or workflow layers (OA-Web, OA-JFC) to
     * determine whether “New” actions should be enabled for the associated
     * {@link OAObject} type.
     * </p>
     *
     * @return {@code true} if the UI and workflow should allow creation of a new
     *         object; {@code false} otherwise.
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
     * @param b {@code true} to allow creation of new objects; {@code false} to
     *          disable it.
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
     * Indicates whether delete operations are permitted for objects of this model
     * type.
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

    /**
     * Indicates whether generated UI/search behavior should allow clearing values.
     *
     * @return {@code true} if clear operations are allowed
     */
    public boolean getAllowClear() {
        return bAllowClear;
    }

    /**
     * Sets whether generated UI/search behavior should allow clearing values.
     *
     * @param b {@code true} to allow clear operations
     */
    public void setAllowClear(boolean b) {
        bAllowClear = b;
    }

    /**
     * Indicates whether recursive navigation/search behavior is allowed.
     *
     * @return {@code true} if recursive behavior is allowed
     */
    public boolean getAllowRecursive() {
        return bAllowRecursive;
    }

    /**
     * Sets whether recursive navigation/search behavior is allowed.
     *
     * @param b {@code true} to allow recursive behavior
     */
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
     * When enabled, UI layers should suppress all editing actions and present the
     * object type in a strictly read-only manner.
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
     * @param b {@code true} to make the model read-only; {@code false} to allow
     *          edits.
     */
    public void setViewOnly(boolean b) {
        bViewOnly = b;
    }

    /**
     * Indicates whether UI components should be created for this model type.
     * <p>
     * Generated UI frameworks use this flag to determine whether screens, editors,
     * and related widgets should be instantiated.
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
     * When disabled, generated UI layers should not create screens or editors for
     * the associated {@link OAObject} type.
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
     * @return {@code true} if column filtering is permitted; {@code false}
     *         otherwise.
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
     * This controls whether dynamic {@code HubFilter} instances may be applied when
     * presenting or manipulating collections of {@link OAObject} instances.
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
     * When enabled, UI and workflow layers may apply {@code HubFilter} instances to
     * refine visible or usable subsets of related objects.
     * </p>
     *
     * @param b {@code true} to allow filtering; {@code false} to disable it.
     */
    public void setAllowFilter(boolean b) {
        bAllowFilter = b;
    }

    /**
     * Indicates whether this model is intended for use within OA-JFC UI
     * environments.
     * <p>
     * When enabled, the UI layer may leverage JFC-specific behaviors, including the
     * use of Hubs loaded within SwingWorker threads.
     * </p>
     *
     * @return {@code true} if this model is used in JFC contexts; {@code false}
     *         otherwise.
     */
    public boolean getForJfc() {
        return bForJfc;
    }

    /**
     * Specifies whether this model will be used within OA-JFC UI environments.
     * <p>
     * This controls JFC-specific UI behaviors, such as background-loaded Hubs and
     * Swing threading considerations.
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
     * @return {@code true} if download/export actions are allowed; {@code false}
     *         otherwise.
     */
    public boolean getAllowDownload() {
        return bAllowDownload;
    }

    /**
     * Sets whether downloading/exporting data is allowed for this model type.
     * <p>
     * When enabled, UI layers may expose export functions such as CSV, Excel, or
     * other file-based outputs.
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
     * Move operations typically involve reorganizing an object's position within a
     * collection or hierarchy.
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
     * @param b {@code true} to allow move operations; {@code false} to disable
     *          them.
     */
    public void setAllowMove(boolean b) {
        bAllowMove = b;
    }

    /**
     * Returns whether refresh actions are permitted for this model type.
     * <p>
     * A refresh action typically reloads or synchronizes the underlying data for
     * objects of this type.
     * </p>
     *
     * @return {@code true} if refresh operations are permitted; {@code false}
     *         otherwise.
     */
    public boolean getAllowRefresh() {
        return bAllowRefresh;
    }

    /**
     * Enables or disables refresh operations for this model type.
     * <p>
     * When enabled, UI layers may expose “Refresh” commands to reload data or
     * synchronize with a backing source.
     * </p>
     *
     * @param b {@code true} to allow refresh operations; {@code false} to disable
     *          them.
     */
    public void setAllowRefresh(boolean b) {
        bAllowRefresh = b;
    }

    /**
     * Indicates whether child collections (link=Many) should be displayed in a
     * split-panel layout.
     * <p>
     * When enabled, UI layers may use a split panel to separate parent and child
     * lists for improved navigation and visibility.
     * </p>
     *
     * @return {@code true} if split-panel child displays are allowed; {@code false}
     *         otherwise.
     */
    public boolean getAllowChildrenSplitPanel() {
        return bAllowChildrenSplitPanel;
    }

    /**
     * Sets whether child collections (link=Many) should be displayed using a
     * split-panel layout.
     * <p>
     * When enabled, UI layers may present parent and child collections in a divided
     * interface region for improved clarity and navigation.
     * </p>
     *
     * @param b {@code true} to enable split-panel child displays; {@code false} to
     *          disable them.
     */
    public void setAllowChildrenSplitPanel(boolean b) {
        bAllowChildrenSplitPanel = b;
    }

    public boolean performCallback() {
        OA oa = OARuntime.oa(getViewHub());
//qqqqvvvvvvvvvvvvvvvvv need to adjust for O2M2O        
        OALinkInfo li = oa.internal().hubs().detail().getLinkInfoFromDetailToMaster(getContextHub());
        if (li == null) {
            Hub<?> hx = hubView.getLinkHub(false);
            if (hx == null) return false;
            String name = oa.internal().hubs().link().getLinkToProperty(hubView);
            oa.internal().objects().rules().onObjectCallbackModel(hx.getObjectClass(), name, this);
        }
        else {
            Class<? extends OAObject> type = li.getToClass();
            oa.internal().objects().rules().onObjectCallbackModel(type, li.getReverseName(), this);
        }
        return true;
    }

    public boolean isFromSameMaster() {
        Hub h = getContextHub().getMasterHub();
        if (h == null) return false;
        return OARuntime.oa(h).internal().hubs().detail().getIsFromSameMasterHub(h, getContextHub());
    }

    public <L extends OAObject> Hub<L> createCombinedHub(Hub<L> hubList, Hub<L> hubSelected) {
        if (hubList == null || hubSelected == null) return null;
        Class<L> type = hubList.getObjectClass();
        if (type == null) type = hubSelected.getObjectClass();
        Hub<L> hub = new Hub<>(type);
        HubCombined<L> hubCombined = new HubCombined(hub, hubList, hubSelected);
        return hub;
    }

    /**
     * Add this to Hub searching.
     */
    public String getExtraWhere() {
        return ir.extraWhere;
    }
    /**
     * Add this to Hub searching.
     */
    public OAFilter<T> getExtraFilter() {
        return ir.extraFilter;
    }
    public Hub<?> getSelectWhereHub() {
        return ir.hubSelectWhere;
    }
    public String getSelectWhereHubPath() {
        return ir.selectWhereHubPath;
    }

    public Hub<?> getFromHub() {
        return ir.hubFrom;
    }
    public String getFromLinkProperty() {
        return ir.fromLinkProperty;
    }

    protected OAObjectSearchModel<T> getSearchModel(Type type) {
        if (alSearchModel == null) {
            alSearchModel = new ArrayList<>();
        }
        else {
            for (OAObjectSearchModel<T> sm : alSearchModel) {
                if (sm.type == type) return sm;
            }
        }
        OAObjectSearchModel<T> searchModel = createSearchModel(type);
        alSearchModel.add(searchModel);
        return searchModel;
    }

    protected OAObjectSearchModel<T> createSearchModel(Type type) {
        OAObjectSearchModel<T> searchModel = new OAObjectSearchModel<>(this, type);
        return searchModel;
    }    
    
    
    public OAObjectSearchModel<T> getSearchModel() {
        OAObjectSearchModel<T> searchModel = getSearchModel(OAObjectSearchModel.Type.DefaultSearch);
        return searchModel;
    }
    public OAObjectSearchModel<T> getInnerSearchModel() {
        OAObjectSearchModel<T> searchModel = getSearchModel(OAObjectSearchModel.Type.InnerHubContextSearch);
        return searchModel;
    }
    public OAObjectSearchModel<T> getSharedSearchModel() { 
        OAObjectSearchModel<T> searchModel = getSearchModel(OAObjectSearchModel.Type.HubContextSharedWithSearchResults);
        return searchModel;
    }
    
    
  //vvvvqqqqq needs to remove any other wiring/listeners
    public void close() {
        if (hlHubContext != null) {
            ir.hubContext.removeListener(hlHubContext);
            hlHubContext = null;
        }
    }

    public OAFilter<T> getFilter() {
        return ir.extraFilter;
    }
    public void setFilter(OAFilter<T> filter) {
        ir.extraFilter = filter;
    }
    
}
