/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.ds.jdbc.query;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.PrintWriter;
import java.sql.*;

import com.viaoa.object.*;
import com.viaoa.transaction.OATransaction;
import com.viaoa.util.ClassModifier;
import com.viaoa.util.OAConverter;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAThrottle;
import com.viaoa.util.OATime;
import com.viaoa.ds.OADataSourceIterator;
import com.viaoa.ds.jdbc.*;
import com.viaoa.ds.jdbc.db.*;
import com.viaoa.hub.Hub;

public class ResultSetIterator implements OADataSourceIterator {
    private static Logger LOG = Logger.getLogger(ResultSetIterator.class.getName());
    
    OADataSourceJDBC ds;
    Class clazz;
    String query;
    Statement statement;
    PreparedStatement preparedStatement;
    OATransaction transaction; 
    ResultSet rs;
    Column[] columns;
    Object[] values;
    ColumnInfo[] columnInfos;
    volatile boolean bMore = false;
    int lastPkeyColumn;  // last column needed to be able to create an ObjectKey, to do a cache lookup
    int max;
    int cnter;

    // used when first selecting only the primary key column
    String query2;
    Statement statement2;
    ResultSet rs2;
    int idColumnCount;
    OAObjectInfo oi;
    Object[] pkeyValues;
    boolean bDatesIncludeTime;
    Object objectTrue, objectFalse;
    boolean bDirty;  
    volatile boolean bIsSelecting;
    volatile boolean bInit;
    DataAccessObject dataAccessObject; 
    DataAccessObject.ResultSetInfo resultSetInfo = new DataAccessObject.ResultSetInfo();
    Object[] arguments; // when using preparedStatement
    private boolean bUsePreparedStatement;

    public static final OAThrottle throttle = new OAThrottle(2500);
    
    public String getQuery() {
        return query;
    }
    public String getQuery2() {
        return query2;
    }
    
    class ColumnInfo {
        Column column;
        int pkeyPos=-1;
    }
    
    public ResultSetIterator(OADataSourceJDBC ds, Class clazz, DataAccessObject dataAccessObject, String query, String query2, int max) {
        this(ds, clazz, null, query, query2, max, dataAccessObject);
    }

    // 20121013 to use with preparedStatement
    public ResultSetIterator(OADataSourceJDBC ds, Class clazz, DataAccessObject dataAccessObject, String query, Object[] arguments) {
        this.ds = ds;
        this.clazz = clazz;
        this.dataAccessObject = dataAccessObject;
        this.query = query;
        this.arguments = arguments;
        bUsePreparedStatement = true;
    }
    public ResultSetIterator(OADataSourceJDBC ds, Class clazz, Column[] columns, String query, Object[] arguments, int max) {
        this(ds, clazz, columns, query, null, max, null);
        this.arguments = arguments;
        bUsePreparedStatement = true;
    }

    public ResultSetIterator(OADataSourceJDBC ds, Class clazz, Column[] columns, String query, int max) {
        this(ds, clazz, columns, query, null, max, null);
    }
    
    /**
     * @param query2 used if the first query only returns pkIds.  
     * Query2 will need to use ? to position where the id values will be inserted.
     * Query2 needs to be the correct SQL statement.
     */
    public ResultSetIterator(OADataSourceJDBC ds, Class clazz, Column[] columns, String query, String query2, int max) {
        this(ds, clazz, columns, query, query2, max, null);
    }
    
    private ResultSetIterator(OADataSourceJDBC ds, Class clazz, Column[] columns, String query, String query2, int max, DataAccessObject dataAccessObject) {
        // LOG.fine("query="+query+", query2="+query2+", columns.length="+columns.length+", max="+max);
        this.ds = ds;
        this.clazz = clazz;
        this.columns = columns;
        this.query = query;
        this.query2 = query2;
        this.max = max;
        this.dataAccessObject = dataAccessObject;
    }
    
    public void setDirty(boolean b) {
        this.bDirty = b;
    }
    public boolean getDirty() {
        return this.bDirty;
    }
    
    protected synchronized void init() {
        if (bInit) return;
        bInit = true;

        long ts = System.currentTimeMillis();
        _init();
        long msDiff = System.currentTimeMillis() - ts;
        
        if (throttle.check() || msDiff > 3000) {
            String txt = throttle.getCheckCount()+") ResultSetIterator: ";
            txt += msDiff+"ms";
            if (msDiff > 5000) txt += " ALERT";

            String s = query;
            int pos = s.toUpperCase().indexOf(" FROM ");
            if (pos > 0) s = s.substring(pos+1);
            pos = s.toUpperCase().indexOf("PASSWORD");
            if (pos > 0) s = s.substring(0, pos) + "****";
            txt += " query="+s;
            
            if (msDiff > 3000) OAPerformance.LOG.fine(txt);
            LOG.fine(txt);
            //if (OAObject.getDebugMode()) {
                System.out.println(txt);
            //}

        }
    }
    
    private void _init() {
        /*
        if ( (qqq%(DisplayMod*4)==0)) {        
            Vector v = OADataSource.getInfo();
            for (int i=0; i<v.size(); i++) {
                System.out.println("  "+v.elementAt(i));
            }
        }
        */
        // 20120227 add transaction
        //        transaction = new OATransaction(Connection.TRANSACTION_READ_UNCOMMITTED);
        //        transaction.start();

        
        this.oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
        
        DBMetaData dbmd = ds.getDBMetaData();
        this.bDatesIncludeTime = dbmd.getDatesIncludeTime();
        this.objectTrue = dbmd.getObjectTrue();
        this.objectFalse = dbmd.getObjectFalse();

        String[] pkeys = this.oi.getIdProperties();
        if (dataAccessObject != null) {
            // no-op needed
            idColumnCount = (pkeys == null) ? 0 : pkeys.length;
        }
        else {
            this.values = new Object[columns.length];
            this.columnInfos = new ColumnInfo[columns.length];
    
            this.pkeyValues = new Object[pkeys.length];
    
            // create column infos
            for (int i=0; i<columns.length; i++) {
                columnInfos[i] = new ColumnInfo();
                columnInfos[i].column = columns[i]; 
                if (columns[i].primaryKey) idColumnCount++;
                assert (columns[i].clazz != null);
                if (columns[i].propertyName == null) {
                    assert(columns[i].fkeyLink != null);
                    continue;
                }
                for (int j=0; j<pkeys.length; j++) {
                    if (pkeys[j].equalsIgnoreCase(columns[i].propertyName)) {
                        columnInfos[i].pkeyPos = j;
                        lastPkeyColumn = Math.max(lastPkeyColumn, i);
                    }
                }
            }
        }
        
        rs = null;
        try {
            bIsSelecting = true;
            if (bUsePreparedStatement) {
                preparedStatement = ds.getConnectionPool().getPreparedStatement(query, false);
                for (int i=0; arguments!=null && i < arguments.length; i++) {
                    preparedStatement.setObject(i+1, arguments[i]);
                }
                preparedStatement.setMaxRows( Math.max(0, max));
                rs = preparedStatement.executeQuery();
            }
            else if (statement == null && ds != null) {
                statement = ds.getStatement(query);
                statement.setMaxRows( Math.max(0, max));
                rs = statement.executeQuery(query);
            }
            
            bMore = rs != null && rs.next(); // goto first
            bIsSelecting = false;
            if (!bMore) _close();
        }
        catch (Exception e) {
            _close();
            throw new RuntimeException(e + ", query: "+query, e);
        }
        finally {
            bIsSelecting = false;
        }
    }
    
    public boolean hasNext() {
        if (!bInit) init();
        return (bMore || hubReadAhead != null);
    }

    
    // 20171222 add prefetch into hub, so that OAThreadLocalDelegate.setGetDetailHub could be used  
    private Hub hubReadAhead;
    private HashSet<Integer> hsObjectWasLoaded;
    private OASiblingHelper siblingHelper;
    
    public synchronized Object next() {
        if (!bInit) init();
        if (!bMore && hubReadAhead == null) return null;

        if (hubReadAhead == null) {
            hubReadAhead = new Hub();
            hsObjectWasLoaded = new HashSet<>(25, .75f);
        }

        hubReadAhead.remove(0);  // remove last one that was returned from next().  It stayed in hubReadAhead in case getSiblings is called
        for (int i=hubReadAhead.size(); bMore && i<100; i++) {
            _next();
        }
        
        OAObject obj = (OAObject) hubReadAhead.getAt(0);
        if (hsObjectWasLoaded.remove(obj.getGuid())) {
            if (siblingHelper == null) siblingHelper = new OASiblingHelper(this.hubReadAhead);
            boolean bx = OAThreadLocalDelegate.addSiblingHelper(siblingHelper); 
            try {
                obj.afterLoad();
            }
            finally {
                if (bx) OAThreadLocalDelegate.removeSiblingHelper(siblingHelper);
            }
        }
        if (!bMore && hubReadAhead.size() == 1) {
            close();
        }
        return obj;
    }
    
    @Override
    public OASiblingHelper getSiblingHelper() {
        return siblingHelper;
    }
    protected boolean _next() {
        if (!bInit) init();
        if (rs == null) return false;
        if (max > 0 && cnter > max) { 
            _close();
            return false;
        }
    
        boolean bDataSourceLoadingObject = true;
        OAObject oaObject = null;
        boolean bLoadedObject = false;
        boolean bSetChangedAndNew = false;
        try {
            ResultSet resultSet = rs;
            if (query2 != null) {  // need to do a seperate select to get data for each row
                if (statement2 == null && ds != null) {
                    statement2 = ds.getStatement(query);
                }
                for (;bMore;) {
                    String newQuery = query2;
                    int pos = 0;
                    for (int i=0; i<idColumnCount; i++) {
                        Object obj = rs.getObject(i+1);
                        String s;
                        if (rs.wasNull()) s = null;
                        else s = OAConverter.toString(obj);
                        pos = newQuery.indexOf('?', pos);
                        if (pos >= 0) {
                            newQuery = newQuery.substring(0,pos) + s + newQuery.substring(pos+1);
                            if (s == null) pos += 4;
                            else pos += s.length();
                        }
                        else throw new RuntimeException("parameter mismatch in query "+query2);
                    }
                    statement2.setMaxRows(0);
                    rs2 = statement2.executeQuery(newQuery);
                    if (rs2.next()) {
                        resultSet = rs2;
                        break;
                    }
                    bMore = rs.next();  // goto next
                    rs2.close();
                    if (!bMore) return false;
                }
            }

            if (!bDirty) {
                OAThreadLocalDelegate.setLoading(true);
            }
            else bDataSourceLoadingObject = false;
 
            if (!bDirty && dataAccessObject != null) {
                resultSetInfo.reset(resultSet);
                oaObject = dataAccessObject.getObject(resultSetInfo);
                bLoadedObject = !resultSetInfo.getFoundInCache();
                bSetChangedAndNew = true;
                
                if (bLoadedObject) {
                    OAObject objx = (OAObject) OAObjectCacheDelegate.add(oaObject, false, true);
                    if (objx != oaObject) {
                        oaObject = objx;
                    }
                }
            }
            else {
                for (int i=0; i < columnInfos.length; i++) {
                    if (columnInfos[i].column.clazz.equals(String.class)) {
                        values[i] =  resultSet.getString(i+1);
                    }
                    else if (columnInfos[i].column.clazz.equals(byte[].class)) {
                        // 20100514
                        Blob blob =  resultSet.getBlob(i+1);
                        if (blob != null) {
                            values[i] = blob.getBytes(1, (int) blob.length());
                        }
                        else values[i] = null;
                    }
                    else {
                        values[i] =  resultSet.getObject(i+1);
                        if (values[i] == null) {
                        }
                        else if (resultSet.wasNull()) {
                            values[i] = null;
                        }
                        else {
                            values[i] = convert(columnInfos[i].column.clazz, values[i]);                    
                        }
                    }
                    if (columnInfos[i].pkeyPos >= 0) {
                        pkeyValues[columnInfos[i].pkeyPos] = values[i];
                        if (i == lastPkeyColumn) {
                            // try to find existing object
                            oaObject = OAObjectCacheDelegate.get(clazz, new OAObjectKey(pkeyValues));
                            if (oaObject != null && !bDirty) break;
                        }
                    }
                }
                
                if (oaObject == null || bDirty) {
                    boolean bNew;
                    if (oaObject == null) {
                        bNew = true;
                        oaObject = (OAObject) OAObjectReflectDelegate.createNewObject(clazz);
                    }
                    else bNew = false;
                    
                    for (int i=0; i < columns.length; i++) {
                        if (!bNew && columnInfos[i].pkeyPos >= 0) continue;
                        if (columnInfos[i].pkeyPos >= 0 || columns[i].fkeyLink == null) { 
                            try {
                                oaObject.setProperty(columns[i].propertyName, values[i]);
                            }
                            catch (Exception e) {
                                if (bNew && columnInfos[i].pkeyPos >= 0) {
                                    OAObject objx = OAObjectCacheDelegate.get(clazz, new OAObjectKey(pkeyValues));
                                    if (objx != null) {
                                        LOG.log(Level.WARNING, "Error while setting property "+columns[i].propertyName+", object has been found in cache, so everything is good", e);
                                        oaObject = objx;
                                        bNew = false;
                                        if (!bDirty) break;
                                    }
                                    else {
                                        LOG.log(Level.WARNING, "Error while setting property "+columns[i].propertyName+", NOT found in cache as hoped :(  will continue anyway", e); 
                                    }
                                }
                                else {
                                    LOG.log(Level.WARNING, "Error while setting property "+columns[i].propertyName+", will continue anyway", e);
                                }
                            }
                        }
                        else {
                            // fkey
                            if (columns[i].fkeyLink.fkeys.length == 1) {
                                oaObject.setProperty(columns[i].fkeyLink.propertyName, values[i]);
                                continue;
                            }
                            
                            if (columns[i].fkeyLinkPos > 0) continue; // already loaded (in next code) 
                            Object[] ids = new Object[columns[i].fkeyLink.fkeys.length];
                            for (int j=i; j < columns.length; j++) {
                                if (columns[j].fkeyLink == columns[i].fkeyLink) {
                                    ids[columns[j].fkeyLinkPos] = values[j];
                                }
                            }
                            oaObject.setProperty(columns[i].fkeyLink.propertyName, new OAObjectKey(ids));
                        }
                    }

                    if (bNew && oi.getAddToCache()) { // 20110731 add to cache, OAThreadLocal.SkipObjectInitialize
                        oaObject = (OAObject) OAObjectCacheDelegate.add(oaObject, false, true);
                    }
                    
                    OAObjectDelegate.setNew(oaObject, false);
                    oaObject.setChanged(false);
                    bLoadedObject = true;
                    bSetChangedAndNew = true;
                }
            }

            ++cnter;
            
            if (bDataSourceLoadingObject) {
                OAThreadLocalDelegate.setLoading(false);
                bDataSourceLoadingObject = false;
            }

            if (bLoadedObject) {
                hsObjectWasLoaded.add(oaObject.getGuid());
            }


            if (rs != null) {
                bMore = rs.next();  // goto next
                if (!bMore) _close();
            }
            hubReadAhead.add(oaObject);

            return true;
        }
        catch (Exception e) {
            String s = String.format("Exception in next(), thread=%s, query=%s, bClosed=%b", Thread.currentThread().getName(), query, bClosed);
            LOG.log(Level.WARNING, s, e);
            throw new RuntimeException(e);
        }
        finally {
            if (bLoadedObject && !bSetChangedAndNew && oaObject != null) {
                OAObjectDelegate.setNew(oaObject, false);
                oaObject.setChanged(false);
            }
            if (bDataSourceLoadingObject) {
                OAThreadLocalDelegate.setLoading(false);
            }
        }
    }    
    
    private boolean bClosed;
    
    // part of iterator interface
    public void remove() {
        _close();
    }

    public void finalize() throws Throwable {
        super.finalize();
        close();
    }

    // 20110407 added synchronized, since OASelectManager could close iterator while it is performing next()
    public synchronized void close() {
        if (hubReadAhead != null) {
            hubReadAhead.clear();
            hubReadAhead = null;
        }
        if (siblingHelper != null) {
            siblingHelper = null;
        }
        bClosed = true;
        bMore = false;
        _close();
    }
    protected void _close() {
        boolean b = false;
        try {
            if (bIsSelecting) {
                try {                    
                    if (statement != null) {
                        statement.cancel();
                    }
                    if (statement2 != null) {
                        statement2.cancel();
                    }
                    if (preparedStatement != null) {
                        preparedStatement.cancel();
                    }
                }
                catch (Exception exx) {
                    int xx = 4;
                    xx++;
                }
            }
            
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (rs2 != null) {
                rs2.close();
                rs2 = null;
            }
            if (transaction != null) {
                transaction.commit();
            }

        }
        catch (Exception e) {
            // throw new OADataSourceException(OADataSourceJDBC.this, "OADataSource.getStatement() "+e);
        }
        finally {
            rs = null;
            rs2 = null;
            bMore = false;
            if (ds != null) {
                if (preparedStatement != null) {  // 20121013
                    ds.getConnectionPool().releasePreparedStatement(preparedStatement, true);
                }
                else {
                    ds.releaseStatement(statement);
                    if (statement2 != null) ds.releaseStatement(statement2);
                }
            }
            statement = null;
            statement2 = null;
            transaction = null;
            preparedStatement = null;
        }
    }

    private Object convert(Class paramType, Object obj) throws Exception {
        if (obj == null) return null;
        if (obj.getClass().equals(paramType)) return obj;

        if (obj instanceof Clob) {
            obj = ((Clob)obj).getSubString(1, (int) ((Clob)obj).length());
        }
        else if (obj.getClass().isArray()) {
            // 2006/06/01
            Class c = ClassModifier.getClassWrapper(paramType);
            if (Number.class.isAssignableFrom(c) ) {
                obj = new java.math.BigInteger((byte[]) obj);
            }
            else if (java.util.Date.class.isAssignableFrom(paramType) ) {
                obj = new java.util.Date(new java.math.BigInteger((byte[]) obj).longValue());
            }
            else if (paramType.equals(String.class) ) {  // 2006/11/08
                obj = new String((byte[]) obj);
            }
        }
        
        if (obj instanceof String) {
            String s = (String) obj;
            String fmt = null;
            if (paramType.equals(String.class)) {
                obj = repairSingleQuotes((String) obj);
            }
            else if (paramType.equals(int.class)) obj = Integer.valueOf(s);
            else if (paramType.equals(double.class)) obj = Double.valueOf(s);
            else if (paramType.equals(long.class)) obj = Long.valueOf(s);
            else if (paramType.equals(short.class)) obj = Short.valueOf(s);
            else if (paramType.equals(float.class)) obj = Float.valueOf(s);
            else if (paramType.equals(char.class)) obj = new Character(s.charAt(0));
            else {
                if ( java.util.Date.class.isAssignableFrom(paramType) ) {
                    if (bDatesIncludeTime) fmt = "yyyy-MM-dd hh:mm:ss.SSS"; // 1999-11-21 14:21:53.123
                    else {
                        fmt = "yyyy-MM-dd"; // 1999-11-21
                        if (paramType.equals(Time.class)) fmt = "hh:mm:ss.SSS"; // 14:21:53
                    }
                }
                else {
                    if (bDatesIncludeTime) fmt = "yyyy-MM-dd hh:mm:ss.SSS"; // 1999-11-21 14:21:53.123
                    else {
                        if (paramType.equals(OADate.class)) fmt = "yyyy-MM-dd";
                        else if (paramType.equals(OATime.class)) fmt = "hh:mm:ss.SSS";
                        else if (paramType.equals(OADateTime.class)) fmt = "yyyy-MM-dd hh:mm:ss.SSS";
                    }
                }
                obj = OAConverter.convert(paramType, (String) obj, fmt);
            }
        }
        else if (obj instanceof Number) {
            Number num = (Number) obj;
            if (paramType.equals(int.class)) obj = new Integer(num.intValue());
            else if (paramType.equals(boolean.class)) obj = new Boolean(num.intValue() != 0);
            else if (paramType.equals(double.class)) obj = new Double(num.doubleValue());
            else if (paramType.equals(String.class)) obj = num.toString();
            else if (paramType.equals(long.class)) obj = new Long(num.longValue());
            else if (paramType.equals(short.class)) obj = new Short(num.shortValue());
            else if (paramType.equals(float.class)) obj = new Float(num.floatValue());
            else if (paramType.equals(char.class)) obj = new Character((char) num.shortValue());
            else if (paramType.equals(java.awt.Color.class)) obj = new java.awt.Color(num.intValue());
        }
        else if (obj instanceof Double && paramType.equals(float.class)) {
            obj = new Float( ((Double)obj).floatValue() ) ;
        }
        else if (obj instanceof java.util.Date) {
            if (paramType.equals(Time.class)) obj = new Time( ((java.util.Date)obj).getTime() );
            else if (paramType.equals(java.sql.Timestamp.class)) obj = new Timestamp( ((java.util.Date)obj).getTime() );
            else if (paramType.equals(OADate.class)) obj = new OADate((java.util.Date)obj);
            else if (paramType.equals(OATime.class)) obj = new OATime( (java.util.Date)obj );
            else if (paramType.equals(OADateTime.class)) obj = new OADateTime( (java.util.Date)obj ); // 2006/11/08
        }
        else if (obj instanceof Boolean) {
            boolean b = ((Boolean) obj).booleanValue();
            if (paramType.equals(boolean.class));
            else if (paramType.equals(int.class)) obj = new Integer(b?1:0);
            else if (paramType.equals(double.class)) obj = new Double(b?1.0:0.0);
            else if (paramType.equals(String.class)) obj = obj.toString();
            else if (paramType.equals(long.class)) obj = new Long((long) (b?1:0));
            else if (paramType.equals(short.class)) obj = new Short((short)(b?1:0));
            else if (paramType.equals(float.class)) obj = new Float((float)(b?1.0f:0.0f));
            else if (paramType.equals(char.class)) obj = new Character((char) (b?'1':'0'));
        }

        if (paramType.equals(boolean.class)) {
            if (!(obj instanceof Boolean)) {
                if (objectTrue == null || objectFalse == null) {
                    if (obj instanceof Number) new Boolean( ((Number)obj).intValue()!=0);
                    //else throw new OADataSourceException(OADataSourceJDBC.this,"ResultSetIterator.next() "+" method "+method.getName()+" uses a boolean and database stores data as "+obj.getClass());
                }
                else {
                    if (obj.equals(objectTrue)) {
                        obj = new Boolean(true);
                    }
                    else if (obj.equals(objectFalse)) {
                        obj = new Boolean(false);
                    }
                    else {
                       // throw new OADataSourceException(OADataSourceJDBC.this,"ResultSetIterator.next() "+" method "+method.getName()+" cant convert "+obj+" to a boolean, it does not match objectTrue or objectFalse values");
                    }
                }
            }
        }
        return obj;
    }
    protected String repairSingleQuotes(String value) {
        return value;
    }
    
}    

