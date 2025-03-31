package com.viaoa.object;


import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.hub.Hub;
import com.viaoa.util.*;


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
 * Create a two dimensional (cols & rows) of data.
 * Each column is given a property path to a link property, which can be type one or many.
 * <br> 
 * Columns can be populated with:
 * <ul>
 * <li>Hub
 * <li>single OAObject
 * <li>property path from another column (master/detail)
 * <li>Hub of Objects that can be grouped by another column.
 * </ul>
 * The rows are then created to form a data grid (/table).
 *  
 * @author vvia
 */
public class OAObjectGrid {
    private final List<Column> alColumn = new ArrayList<>(); 
    private List<Object[]> alGrid;

    public static class Column<TYPE extends OAObject> {
        Column colFrom;
        Hub hub;
        OAObject object;
        
        String pp;
        boolean bGroupBy;
        String matchPropName;
        
        public String getPropertyPath() {
            return pp;
        }
        public Column getFromColumn() {
            return colFrom;
        }
    }
    
    
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
     * Create a LeftJoin relationship with another column (the "left").
     * @param colLeft that is the linkLeft for hub to match to.
     * @param pp propertyPath to link hub/object.
     * @param matchPropName property in hub that links to colLeft.hub
     */
    public Column addGroupByColumn(OAObjectGrid.Column colLeft, Hub hub, String pp, String matchPropName) {
        if (colLeft == null) return null;
        
        Column colRoot = getRootColumn(colLeft);
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
    
    
    public Column getRootColumn(Column col) {
        if (col == null) return null;
        if (col.colFrom == null) return col;
        return getRootColumn(col.colFrom);
    }

    public String getPropertyPathFromRoot(Column colParent, String pp) {
        if (colParent == null) return pp;
        if (OAStr.isNotEmpty(colParent.pp)) {
            pp = OAStr.prepend(pp, colParent.pp, ".");
        }
        if (colParent.colFrom == null) return pp;
        return getPropertyPathFromRoot(colParent.colFrom, pp);
    }
    /** all column propertyPaths need to be for a link */
    public static boolean verifyLinkProperty(Class classFrom, String pp) {
        OAPropertyPath oapp = new OAPropertyPath(classFrom, pp);
        OALinkInfo li = oapp.getEndLinkInfo();
        return (li != null);
    }
    
    
    
    public Column getColumn(int pos) {
        if (pos < 0 || pos >= alColumn.size()) return null;
        return alColumn.get(pos);
    }
    public int getColumnCount() {
        return alColumn.size();
    }
    public List<Column> getColumns() {
        return alColumn;
    }

    public List<Object[]> getGrid() {
        if (alGrid == null) {
            alGrid = createGrid();
        }
        return alGrid;
    }
    
    public void clearGrid() {
        alGrid = null;
    }
 
    public Object getObject(int row, int col) {
        final List<Object[]> al = getGrid();
        if (row < 0 || al == null || row >= al.size() || col >= alColumn.size()) return null;
        Column column = alColumn.get(col);
        Object obj = getObject(row, column, al, false);
        return obj;
    }

    /**
     * Find the object for a row,col.  Since a row,col can be null when a cell is repeating (/expanded) to 
     * multiple rows.
     */
    public Object getRealObject(int row, int col) {
        final List<Object[]> al = getGrid();
        if (row < 0 || al == null || col >= al.size()) return null;
        Column column = alColumn.get(col);
        Object obj = getObject(row, column, al, true);
        return obj;
    }
    
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
    
    protected boolean hasChildRow(Column column, int row, final List<Object[]> al) {
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
    
    
    public int getRowCount() {
        if (alGrid != null) return alGrid.size();
        return 0;
    }
    
    public int getRowCount(Column col) {
        int cnt = 0;
       
        // start with parent
        Column colRoot = col;
        for ( ; col.colFrom != null; colRoot = colRoot.colFrom) {
        }
        
        for (Object obj : colRoot.hub) {
            int x = getRowCount(colRoot, (OAObject) obj);
            cnt += x;
        }
        return cnt;
    }

    
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


