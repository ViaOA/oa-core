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
package com.viaoa.ui.grid;


import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAStr;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;


// See: OATemplate when populating a html Table /grid 

//qqqqqqqqqqqqqqqqqq stack column

//qqqqqqqqqq add sort PPs
// group by range (ex: between dates)  ...  addGroupByRangeColumn
// perform:  sum, count, arith func, etc
// subtotals, final totals
// stack rows
// empty rows
// stack text 

/**
 * Framework component that constructs a two-dimensional grid of
 * {@link OAObject} references derived from one or more linked
 * {@link com.viaoa.hub.Hub}s.
 *
 * <p>Each {@code Column} represents a property path that can point to
 * a link property, detail hub, or group-by relationship.  The grid then
 * materializes a relational view (rows × columns) by traversing these
 * property paths, effectively building a dynamic in-memory table.</p>
 *
 * <p><b>Capabilities</b>:
 * <ul>
 *   <li>Add detail, group-by, or link columns programmatically.</li>
 *   <li>Generate the full matrix of objects via {@link #createGrid()}.</li>
 *   <li>Retrieve individual cell objects using {@link #getObject(int,int)}.</li>
 *   <li>Support nested hierarchies with recursive traversal.</li>
 * </ul>
 *
 * <p>Used by templating and reporting tools (e.g. OATemplate) to
 * generate complex HTML tables or exportable datasets from live OA
 * object graphs.</p>
 */
public class OAObjectGrid {
	
	/**
	 * The list of column definitions that make up this grid. Each entry
	 * defines a property path or relationship used when constructing the
	 * two-dimensional object matrix.
	 */
    private final List<Column> alColumn = new ArrayList<>(); 
    
    /**
     * The cached materialized grid. Each element represents a row containing
     * objects assigned to each column. Lazily created and cleared whenever
     * column definitions change.
     */
    private List<Object[]> alGrid;

    public static class Column<TYPE extends OAObject> {
    	/**
    	 * Parent column from which this column is derived. Used to represent
    	 * hierarchical or nested property-path traversal when building the grid.
    	 */
        Column colFrom;

        /**
         * Optional Hub serving as the data source for this column. Root columns
         * typically have a hub, while detail or group-by columns may not.
         */
        Hub hub;
        
        /**
         * Optional single OAObject assigned to this column. Used instead of a hub
         * when a specific object is the starting point for row generation.
         */
        OAObject object;
        
        /**
         * The property path used to traverse from the parent column’s object to
         * the objects represented by this column.
         */
        String pp;

        /**
         * Flag indicating whether this column performs a group-by operation as
         * opposed to a standard detail traversal.
         */
        boolean bGroupBy;

        /**
         * Property name used during group-by evaluation to match parent objects
         * against objects in the group-by hub.
         */
        String matchPropName;
        
        /**
         * Returns the property path assigned to this column. This path is
         * used to traverse from the root object to the referenced link when
         * populating the grid.
         *
         * @return the property path for this column, or {@code null} if unset
         */
        public String getPropertyPath() {
            return pp;
        }
        
        /**
         * Returns the parent column from which this column originates.
         * Parent columns represent earlier traversal steps when building
         * nested or hierarchical grid structures.
         *
         * @return the parent column, or {@code null} if this column is a root
         */
        public Column getFromColumn() {
            return colFrom;
        }
    }
    
    
    /**
     * Adds a new root column to the grid using the supplied hub as the
     * data source. Each object in the hub contributes one or more rows
     * when the grid is materialized.
     *
     * @param hub the hub providing objects for this column
     * @return the newly created column
     * @throws IllegalArgumentException if {@code hub} is {@code null}
     */
    public Column addColumn(Hub hub) {
        if (hub == null) {
            throw new IllegalArgumentException("hub can not be null");
        }
        Column col = new Column();
        col.hub = hub;
        
        alColumn.add(col);
        clearGrid();
        return col;
    }

    /**
     * Adds a detail column originating from another column. The supplied
     * property path must point to a link property beneath the originating
     * column's object or hub type. Each detail object discovered through
     * the property path expands the grid vertically.
     *
     * @param colFrom the column representing the parent object
     * @param pp      the link property path used to locate detail objects
     * @return the newly created detail column, or {@code null} if inputs
     *         are invalid
     * @throws RuntimeException if the property path is not a valid link
     */
    public Column addDetailColumn(OAObjectGrid.Column colFrom, String pp) {
        if (colFrom == null) return null;
        if (OAStr.isEmpty(pp)) return null;
        Column colRoot = getRootColumn(colFrom);
        String fpp = getPropertyPathFromRoot(colFrom, pp);
        if (!verifyLinkProperty(colRoot.hub == null ? colRoot.object.getClass() : colRoot.hub.getObjectClass(), fpp)) {
            throw new RuntimeException("invalid propertyPath, must be for a link property, pp="+pp);
        }
        
        Column col = new Column();
        col.colFrom = colFrom;
        col.pp = pp;
        alColumn.add(col);
        clearGrid();
        return col;
    }

    /**
     * Adds a group-by column that performs a left-join style match
     * between the parent column and a supplied hub. Objects from the
     * hub whose {@code matchPropName} value matches the parent object
     * are included in the grid under this column.
     *
     * @param colLeft       the parent column for group-by alignment
     * @param hub           the hub supplying objects to join
     * @param pp            the link property path for traversal
     * @param matchPropName property used to match the parent object
     * @return the created group-by column, or {@code null} if invalid
     * @throws RuntimeException if the property path is not a valid link
     */
    public Column addGroupByColumn(OAObjectGrid.Column colLeft, Hub hub, String pp, String matchPropName) {
        if (colLeft == null) return null;
        
        //Column colRoot = getRootColumn(colLeft);
        if (!verifyLinkProperty(colLeft.hub == null ? colLeft.object.getClass() : colLeft.hub.getObjectClass(), pp)) {
            throw new RuntimeException("invalid propertyPath, must be for a link property, pp="+pp);
        }
        
        
        Column col = new Column();
        col.bGroupBy = true;
        col.colFrom = colLeft;
        col.hub = hub;
        col.pp = pp;
        col.matchPropName = matchPropName;
        alColumn.add(col);
        clearGrid();
        
        return col;
    }
    

    /*qqqqqqqqq
    public Column addGroupByColumn(OAObjectGrid.Column colLeft, OAObject obj, String pp, String matchPropName) {
        if (colLeft == null) return null;
        
        Column colRoot = getRootColumn(colLeft);
        String fpp = getPropertyPathFromRoot(colLeft, pp);
        if (!verifyLinkProperty(colRoot.hub == null ? colRoot.object.getClass() : colRoot.hub.getObjectClass(), fpp)) {
            throw new RuntimeException("invalid propertyPath, must be for a link property, pp="+pp);
        }
        
        Column col = new Column();
        col.bGroupBy = true;
        col.colFrom = colLeft;
        col.object = obj;
        col.pp = pp;
        col.matchPropName = matchPropName;
        alColumn.add(col);
        clearGrid();
        
        return col;
    }
    */
    
    
    /**
     * Returns the top-level root column for the supplied column by
     * following its {@code colFrom} chain. Root columns are the
     * starting points for grid row expansion.
     *
     * @param col the column whose root is requested
     * @return the root column, or {@code null} if {@code col} is null
     */
    public Column getRootColumn(Column col) {
        if (col == null) return null;
        if (col.colFrom == null) return col;
        return getRootColumn(col.colFrom);
    }

    /**
     * Computes the full property path from the root column to the
     * supplied path by recursively prepending parent column paths
     * when present.
     *
     * @param colParent the parent column
     * @param pp        the property path to extend
     * @return the full property path relative to the root column
     */
    public String getPropertyPathFromRoot(Column colParent, String pp) {
        if (colParent == null) return pp;
        if (OAStr.isNotEmpty(colParent.pp)) {
            pp = OAStr.prepend(pp, colParent.pp, ".");
        }
        if (colParent.colFrom == null) return pp;
        return getPropertyPathFromRoot(colParent.colFrom, pp);
    }

    /**
     * Verifies that the supplied property path resolves to a link
     * property for the given class. This is required for detail and
     * group-by columns, which must traverse link relationships.
     *
     * @param classFrom the starting class for the property path
     * @param pp        the property path to validate
     * @return {@code true} if the path resolves to a link, otherwise false
     */
    public static boolean verifyLinkProperty(Class classFrom, String pp) {
        OAPath oapp = new OAPath(classFrom, pp);
        OALinkInfo li = oapp.getEndLinkInfo();
        return (li != null);
    }
    
    /**
     * Returns the column at the specified index.
     *
     * @param pos zero-based column position
     * @return the column at the given position, or {@code null} if out of range
     */
    public Column getColumn(int pos) {
        if (pos < 0 || pos >= alColumn.size()) return null;
        return alColumn.get(pos);
    }
    
    /**
     * Returns the total number of columns currently defined in the grid.
     *
     * @return the number of columns
     */
    public int getColumnCount() {
        return alColumn.size();
    }

    /**
     * Returns the list of all {@link Column} definitions that make up
     * this grid. Each column corresponds to a property path or linked
     * relationship used when building the two-dimensional structure.
     *
     * @return the internal list of column definitions.
     */
    public List<Column> getColumns() {
        return alColumn;
    }

    /**
     * Lazily initializes and returns the materialized grid.
     * If the grid has not yet been created, {@link #createGrid()}
     * is invoked to populate all rows and columns.
     *
     * @return the list of row arrays representing the grid.
     */
    public List<Object[]> getGrid() {
        if (alGrid == null) {
            alGrid = createGrid();
        }
        return alGrid;
    }
    
    /**
     * Clears the cached grid so that it will be rebuilt the
     * next time {@link #getGrid()} is invoked. This is called
     * when column definitions change.
     */
    public void clearGrid() {
        alGrid = null;
    }
 
    /**
     * Retrieves the object at the specified row and column.
     * Performs bounds checking and delegates to the protected
     * {@link #getObject(int, Column, List, boolean)} helper.
     *
     * @param row index of the row.
     * @param col index of the column.
     * @return the object for the cell, or {@code null} if out of bounds.
     */
    public Object getObject(int row, int col) {
        final List<Object[]> al = getGrid();
        if (row < 0 || al == null || row >= al.size() || col >= alColumn.size()) return null;
        Column column = alColumn.get(col);
        Object obj = getObject(row, column, al, false);
        return obj;
    }

    /**
     * Returns the “real” object for a cell, resolving cases where
     * repeated/expanded rows result in null entries. If a cell is
     * propagated downward due to child-column expansion, this method
     * walks upward to find the originating non-null value.
     *
     * @param row index of the row.
     * @param col index of the column.
     * @return the resolved non-null object, or {@code null}.
     */
    public Object getRealObject(int row, int col) {
        final List<Object[]> al = getGrid();
        if (row < 0 || al == null || row >= al.size()) return null;
        if (col < 0 || col >= alColumn.size()) return null;
        Column column = alColumn.get(col);
        Object obj = getObject(row, column, al, true);
        return obj;
    }
    
    /**
     * Core handler for retrieving an object from the grid.
     * Performs bounds checks, looks up the stored cell value,
     * and when {@code bGetRealObject} is true, resolves null
     * entries caused by child-row expansion by scanning upward
     * in the same column.
     *
     * @param rowPos       row index.
     * @param column       column definition.
     * @param al           grid rows.
     * @param bGetRealObject true to resolve propagated nulls.
     * @return the cell object or a resolved ancestor value.
     */
    protected Object getObject(final int rowPos, final Column column, final List<Object[]> al, final boolean bGetRealObject) {
        if (column == null || al == null) return null;
        if (rowPos >= al.size()) return null;
        final int colPos = alColumn.indexOf(column);
        if (colPos < 0) return null;
    
        final Object[] objs = al.get(rowPos);
        Object obj = objs[colPos];
        if (obj != null || !bGetRealObject) return obj;
        
        // see if child column "pushed" it to be empty
        if (hasChildRow(column, rowPos, al)) {
            for (int rp=rowPos-1; obj == null && rp >= 0; rp--) {
                Object[] objxs = al.get(rp);
                obj = objxs[colPos];
            }
        }
        return obj;
    }
    
    /**
     * Determines whether the specified column has a child column
     * value occupying the same row. Used for resolving repeated
     * rows and identifying when null propagation occurs.
     *
     * @param column column to evaluate.
     * @param row    row index.
     * @param al     grid rows.
     * @return true if a descendant column occupies this row.
     */
    protected boolean hasChildRow(Column column, int row, final List<Object[]> al) {
    	if (column == null) return false;
        if (al == null) return false;
        if (row >= al.size()) return false;
        
        Object[] objxs = al.get(row);
        int col = alColumn.indexOf(column);
        if (objxs[col] != null) return true;
        
        for (final Column colx : alColumn) {
            if (colx.colFrom != column) continue;
            if (hasChildRow(colx, row, al)) return true;
        }
        return false;
    }
    
    
    /**
     * Returns the number of rows in the current grid.
     * If the grid has not been built yet, returns zero.
     *
     * @return the number of populated grid rows.
     */
    public int getRowCount() {
        if (alGrid != null) return alGrid.size();
        return 0;
    }
    
    /**
     * Computes the total number of rows produced for the
     * specified root column by recursively counting all
     * descendant rows. Iterates the underlying hub or
     * object chain to accumulate counts.
     *
     * @param col the root column.
     * @return number of rows the column contributes.
     */
    public int getRowCount(Column col) {
        int cnt = 0;
       
        // start with parent
        Column colRoot = col;
        for ( ; col.colFrom != null; colRoot = colRoot.colFrom) {
        }
        
        if (colRoot.hub == null) {
        	if (colRoot.object != null) {
	            int x = getRowCount(colRoot, colRoot.object);
	            cnt += x;
        	}
        }
        else {
	        for (Object obj : colRoot.hub) {
	            int x = getRowCount(colRoot, (OAObject) obj);
	            cnt += x;
	        }
        }
        return cnt;
    }

    
    /**
     * Constructs the full grid by iterating over all
     * top-level (root) columns and recursively populating
     * row entries using {@link #_populateGridRows(Column,int,OAObject,List)}.
     *
     * @return the newly created list of grid rows.
     */
    public List<Object[]> createGrid() {
        alGrid = new ArrayList();
        int max = 0;
        for (Column col : alColumn) {
            int row = 0;
            if (col.colFrom != null) continue;
            if (col.object != null) {
                row = _populateGridRows(col, row, (OAObject) col.object, alGrid);
            }
            else {
                for (Object obj : col.hub) {
                    row = _populateGridRows(col, row, (OAObject) obj, alGrid);
                }
            }
            max = Math.max(max, row);
        }
        return alGrid;
    }
    
    /**
     * Recursively populates the grid for the specified column and object.
     * Ensures that a row exists for the given position, assigns the object
     * to its column cell, then processes all child columns. Each child column
     * expands downward, increasing row usage as nested relationships are
     * traversed.
     *
     * @param column  the column being populated.
     * @param rowPos  the starting row position.
     * @param object  the object assigned to the current cell.
     * @param alGrid  the accumulating grid.
     * @return the next available row index after population.
     */
    protected int _populateGridRows(final Column column, int rowPos, final OAObject object, final List<Object[]> alGrid) {
        int colPos = alColumn.indexOf(column);
        for (int row=alGrid.size(); row <= rowPos; row++) {
            alGrid.add(new Object[getColumnCount()]);
        }
        Object[] rowObjs = alGrid.get(rowPos);
        rowObjs[colPos] = object; 
        
        int rowNext = rowPos + 1;
        for (final Column col : alColumn) {
            if (col.colFrom != column) continue;

            if (!col.bGroupBy) {
                final AtomicInteger ai = new AtomicInteger(rowPos);
                OAFinder<OAObject, OAObject> f = new OAFinder(col.pp) {
                    @Override
                    protected boolean isUsed(OAObject obj) {
                        int x = _populateGridRows(col, ai.get(), obj, alGrid);
                        ai.set(Math.max(x, ai.get()));
                        return false;
                    }
                };
                f.find(object);
                rowNext = Math.max(rowNext, ai.get());
                continue;
            }                
            
            if (col.hub == null) continue;
            
            final AtomicInteger ai = new AtomicInteger(rowPos);
            OAFinder<OAObject, OAObject> f = new OAFinder(col.pp) {
                @Override
                protected boolean isUsed(OAObject obj) {
                    Object objx = obj.getProperty(col.matchPropName);
                    if (object == objx) {
                        int x = _populateGridRows(col, ai.get(), obj, alGrid);
                        ai.set(Math.max(x, ai.get()));
                    }
                    return false;
                }
            };
            if (col.object != null) f.find(col.object);
            else f.find(col.hub);
            rowNext = Math.max(rowNext, ai.get());
        }
        return rowNext;
    }
    
    /**
     * Calculates the number of rows contributed by the given object
     * for the specified column. Traverses child columns and recursively
     * aggregates their row counts. Ensures a minimum count of one when
     * no children produce rows.
     *
     * @param column the column whose row count is calculated.
     * @param object the object to evaluate.
     * @return number of rows allocated for the object.
     */
    public int getRowCount(final Column column, final OAObject object) {
        int max = 0;
        for (final Column col : alColumn) {
            if (col.colFrom != column) continue;

            if (!col.bGroupBy) {
                final AtomicInteger ai = new AtomicInteger();
                OAFinder<OAObject, OAObject> f = new OAFinder(col.pp) {
                    @Override
                    protected boolean isUsed(OAObject obj) {
//System.out.println(++cntx + ") " + obj.getClass().getSimpleName());//qqqqqqq                        
                        int x = getRowCount(col, (OAObject) obj);
                        ai.addAndGet(x);
                        return false;
                    }
                };
                f.find(object);
                max = Math.max(max, ai.get());
                continue;
            }                
            
            if (col.hub == null) continue;
            
            final AtomicInteger ai = new AtomicInteger();
            OAFinder<OAObject, OAObject> f = new OAFinder(col.pp) {
                @Override
                protected boolean isUsed(OAObject obj) {
                    Object objx = obj.getProperty(col.matchPropName);
                    if (object == objx) {
//System.out.println(++cntx + ") " + obj.getClass().getSimpleName());//qqqqqqq                        
                        int x = getRowCount(col, (OAObject) obj);
                        ai.addAndGet(x);
                    }
                    return false;
                }
            };
            if (col.object != null) f.find(col.object);
            else f.find(col.hub);
            max = Math.max(max, ai.get());
        }
        int cnt = Math.max(1, max);
        return cnt;
    }
}


