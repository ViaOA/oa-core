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
package com.viaoa.ds.jdbc;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.*;

import com.viaoa.object.*;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;
import com.viaoa.ds.*;
import com.viaoa.ds.jdbc.db.*;
import com.viaoa.ds.jdbc.delegate.*;
import com.viaoa.ds.jdbc.connection.*;

/**
    OADataSource object that uses JDBC and connection pools to communicate with Relational Databases.
    Uses settings to know what type of database that is being used so that SQL statements can
    be customized to fit the particular database.
    <p>
    OADataSourceJDBC has a lot of settings that can be used to set up how the JDBC Driver and SQL needs to
    work.  By setting the type of Database, these values will automatically be set.
*/
public class OADataSourceJDBC extends OADataSource {
    
    private static Logger LOG = Logger.getLogger(OADataSourceJDBC.class.getName());

    protected DBMetaData dbmd;
    protected Database database;
    protected ConnectionPool connectionPool;

    /**
        Create new OADataSourceJDBC using a Database mapping object.
    */
    public OADataSourceJDBC(Database database, DBMetaData dbmd) {
        this.database = database;
        this.dbmd = dbmd;
        connectionPool = new ConnectionPool(dbmd);
    }

    /**
        Returns database mapping object.
    */
    public Database getDatabase() {
        return database;
    }
    public DBMetaData getDBMetaData() {
    	return dbmd;
    }
    public void setDBMetaData(DBMetaData dbmd) {
        this.dbmd = dbmd;
    }
    public ConnectionPool getConnectionPool() {
    	return connectionPool;
    }

    
    /**
        Returns true, this datasource supports selecting/storing/deleting
    */
    public @Override boolean supportsStorage() {
        return true;
    }

    public @Override boolean isAvailable() {
    	boolean b = true;
    	try {
    		return connectionPool.isDatabaseAvailable();
    	}
    	catch (Exception e) {
    	    System.out.println("OADataSourceJDBC.isAvailable error: "+e);
    		return false;
    	}
    }

    /** returns false */
    public @Override boolean getAllowIdChange() {
        return false;
    }
    
    /**
        Returns true if Database has Table that is mapped to Class.
    */
    public @Override boolean isClassSupported(Class clazz, OAFilter filter) {
        boolean b = (database.getTable(clazz) != null);
        return b;
    }

    
    /**
        Adds Strings to Vector, listing information about DataSource.
    */
    public void getInfo(Vector vec) {
        connectionPool.getInfo(vec);
    }
    
    
    /**
	    Set all properties that are mapped to a column to NULL
	*/
	public @Override void assignId(OAObject object) {
	    if (!bAssignNumberOnCreate) return;
        try {
            OAObjectDSDelegate.setAssigningId(object, true);
            _assignId(object);
        }
        finally {
            OAObjectDSDelegate.setAssigningId(object, false);
        }
	}
    private void _assignId(OAObject object) {
	    Class clazz = object.getClass();
	    for (;;) {
	        Table table = database.getTable(clazz);
	        if (table == null) return;
	
	        Column[] columns = table.getColumns();
	        for (int i=0; columns != null && i < columns.length; i++) {
	            Column column = columns[i];
	            if (dbmd.supportsAutoAssign && column.primaryKey && column.assignNextNumber) {
	            	if (!column.assignedByDatabase) {
	            		AutonumberDelegate.assignNumber(this, object, table, column);
	            	}
	            }
	        }
	        clazz = clazz.getSuperclass();
	        if (clazz == null || clazz.equals(OAObject.class)) break;
	    }
	}
    
	public void setNextNumber(Class c, int nextNumberToUse) {
	    Table table = database.getTable(c);
	    AutonumberDelegate.setNextNumber(this, table, nextNumberToUse);
	}
	
    public @Override void update(OAObject object, String[] includeProperties, String[] excludeProperties) {
        OAObjectKey key = OAObjectKeyDelegate.getKey(object);
        LOG.finer("object="+object.getClass()+", key="+key);
    	UpdateDelegate.update(this, object, includeProperties, excludeProperties);
    }

    public @Override void insert(OAObject object) {
        OAObjectKey key = OAObjectKeyDelegate.getKey(object);
        LOG.finer("object="+object.getClass()+", key="+key+", isNew="+object.isNew());
        InsertDelegate.insert(this, object);
        /*was
                try {
                    InsertDelegate.insert(this, object);
                }
                catch (RuntimeException e) {
                    LOG.log(Level.WARNING, "OADataSourceJDBC.insert ERROR: object="+object.getClass().getSimpleName()+", key="+key+", isNew="+object.isNew(), e);
                    throw e;
                }
        */        
    }

    public @Override void insertWithoutReferences(OAObject obj) {
        OAObjectKey key = OAObjectKeyDelegate.getKey(obj);
        LOG.fine("object="+obj.getClass()+", key="+key+", isNew="+obj.isNew());
    	InsertDelegate.insertWithoutReferences(this, obj);
    }

    
    public @Override void delete(OAObject object) {
        OAObjectKey key = OAObjectKeyDelegate.getKey(object);
        LOG.fine("object="+object.getClass().getSimpleName()+", key="+key);
        DeleteDelegate.delete(this, object);
    }

	public @Override void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster) {
        OAObjectKey key = OAObjectKeyDelegate.getKey(masterObject);
        LOG.finer("object="+masterObject.getClass().getSimpleName()+", key="+key);
		UpdateDelegate.updateMany2ManyLinks(this, masterObject, adds, removes, propFromMaster);
	}


    /**
        Called by OAObject.getRequiredProperties() to find required properties that are unassigned.
        @return true if the datasource will set the property value before saving.
    */
    public @Override boolean willCreatePropertyValue(OAObject object, String propertyName) {
        if (object == null) return false;
        Class clazz = object.getClass();
        if (propertyName == null) return false;
        Table table = database.getTable(clazz);
        if (table == null) return false;

        Column[] columns = table.getColumns();
        for (int i=0; columns != null && i < columns.length; i++) {
            Column column = columns[i];
            if (propertyName.equalsIgnoreCase(column.propertyName)) {
                if (column.primaryKey && column.assignNextNumber) return true;
            }
        }
        return false;
    }


    /**
     * 
     * @param filter not used, since the jdbc ds supports queries.
     */
    @Override
    public OADataSourceIterator select(Class selectClass, 
        String queryWhere, Object[] params, String queryOrder, 
        OAObject whereObject, String propertyFromWhereObject, String extraWhere, 
        int max, OAFilter filter, boolean bDirty
    )
    {
        // 20200219 need to convert whereObject/properyFromWhereObject to part of query if it's using a propertyPath 
        if (whereObject != null && propertyFromWhereObject != null && propertyFromWhereObject.indexOf(".") >= 0) {
            OAPropertyPath pp = new OAPropertyPath(whereObject.getClass(), propertyFromWhereObject);
            pp = pp.getReversePropertyPath();
            if (OAString.isNotEmpty(queryWhere)) queryWhere += " AND ";
            else if (queryWhere == null) queryWhere = "";
            queryWhere += pp.getPropertyPath() + " == ?";
            params = OAArray.add(Object.class, params, whereObject);
            whereObject = null;
            propertyFromWhereObject = null;
        }
        
        if (whereObject != null) {
            return SelectDelegate.select(this, selectClass, 
                    whereObject, extraWhere, params, propertyFromWhereObject,
                    queryOrder, max,  bDirty);
            
        }
        return SelectDelegate.select(this, selectClass, 
                queryWhere, params, queryOrder,
                max, bDirty);
    }

    
    public OADataSourceIterator selectPassthru(Class selectClass, 
        String queryWhere, String queryOrder, 
        int max, OAFilter filter, boolean bDirty
    )
    {
        return SelectDelegate.selectPassthru(this, selectClass, queryWhere, queryOrder, max, bDirty);
    }

    
    public @Override Object execute(String command) {
        return SelectDelegate.execute(this,command);
    }
    
    
    @Override
    public int count(Class selectClass, 
        String queryWhere, Object[] params,   
        OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max)
    {
        if (whereObject != null) {
            return SelectDelegate.count(this, selectClass, whereObject, extraWhere, params, propertyFromWhereObject, max);
        }
        return SelectDelegate.count(this, selectClass, queryWhere, params, max);
    }

    @Override
    public int countPassthru(Class selectClass, String queryWhere, int max) {
        return SelectDelegate.countPassthru(this, queryWhere, max);
    }
    


    
    /**
        Returns a JDBC Statement from connection pool.
        Note: you must call releaseStatement() to return the Statement to the Connection Pool
    */
    public Statement getStatement() {
        return getStatement("OADataSourceJDBC.getStatement()");
    }

    /**
        Returns a JDBC Statement from connection pool.
        Note: you must call releaseStatement() to return the Statement to the Connection Pool
    */
    public Statement getStatement(String message) {
        try {
            return connectionPool.getStatement(message);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** release a JDBC Statement from connection pool. */
    public void releaseStatement(Statement statement) {
        if (statement != null) connectionPool.releaseStatement(statement);
    }
    
    
    
    /**
        Returns a JDBC PreparedStatement from connection pool.
        Note: you must call releasePreparedStatement() to return the PreparedStatement to the Connection Pool
    */
    public PreparedStatement getPreparedStatement(String sql) {
        return getPreparedStatement(sql, false);
    }
    public PreparedStatement getPreparedStatement(String sql, boolean bHasAutoGenerated) {
        try {
            return connectionPool.getPreparedStatement(sql, bHasAutoGenerated);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
        Releases a JDBC PreparedStatement from connection pool.
    */
    public void releasePreparedStatement(PreparedStatement ps) {
        if (ps != null) connectionPool.releasePreparedStatement(ps, false);
    }
    public void releasePreparedStatement(PreparedStatement ps, boolean bCanBeReused) {
        if (ps != null) connectionPool.releasePreparedStatement(ps, bCanBeReused);
    }


    /**
        Close all Connections.
    */
    public void close() {
        super.close(); // remove from list of available datasources
        if (connectionPool != null) {
            connectionPool.close();
        }
        DBMetaDataDelegate.close(dbmd);
    }

    @Override
    public void reopen(int pos) {
        super.reopen(pos);
        if (connectionPool != null) {
            connectionPool.open();
        }
    }
    /**
        Close all Connections.
    */
    public void closeAllConnections() {
        if (connectionPool != null) connectionPool.closeAllConnections();
    }

    /**
        Get a jdbc Connection from Connection Pool.
    */
    public Connection getConnection() throws Exception {
        return connectionPool.getConnection(false);
    }
    public Connection getConnection(boolean bExclusive) throws Exception {
        return connectionPool.getConnection(bExclusive);
    }
    public void releaseConnection(Connection connection) {
        if (connection != null) connectionPool.releaseConnection(connection);
    }
    
    public boolean verify() throws Exception {
        System.out.println("");
        return VerifyDelegate.verify(this);
    }
 
    @Override
    public byte[] getPropertyBlobValue(OAObject obj, String propertyName) {
        byte[] result = null;
        try {
            result = SelectDelegate.getPropertyBlobValue(this, obj, propertyName);
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "error getting blob value", e);
        }
        return result;
    }

    
    @Override
    public Object getObject(OAObjectInfo oi, Class clazz, OAObjectKey key, boolean bDirty) {
        Object obj = null;
        try {
            Iterator it = SelectDelegate.selectObject(this, clazz, key, bDirty);
            if (it == null) {
                return super.getObject(oi, clazz, key, bDirty);
            }
            obj = it.next();
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "error getting object, class="+clazz, e);
        }
        return obj;
    }

    @Override
    public int getMaxLength(Class c, String propertyName) {
        int x = Delegate.getPropertyMaxLength(this, c, propertyName);
        return x;
    }
    
    @Override
    public void setGuid(String guid) {
        super.setGuid(guid);
        getDBMetaData().guid = guid;
    }

    /**
     * Select the objectKeys from a link table.
     */
    public ArrayList<ManyToMany> getManyToMany(OALinkInfo linkInfo) {
        if (linkInfo == null) return null;
        ArrayList<ManyToMany> al = SelectDelegate.getManyToMany(this, linkInfo);
        return al;
    }

    /**
     * used to set the next number for databases that support auto assigned (seq) pkeys.
     */
    public void updateAutoSequence(Class<? extends OAObject> clazz) {
        if (dbmd == null || clazz == null) return;
        
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
        Table table = database.getTable(clazz);
        
        Column[] cols = table.getPrimaryKeyColumns();
        if (cols == null || cols.length == 0) return;
        if (cols[0].assignedByDatabase) {
            Connection connection = null;
            Statement st = null;
            try {
                connection = getConnection();
                st = connection.createStatement();

                if (dbmd.getDatabaseType() == dbmd.POSTGRES) {
                    String sql = "SELECT setval('"+table.name+"_id_seq', (SELECT MAX(id) FROM "+table.name+"))";
                    LOG.fine(sql);
                    st.execute(sql);
                    st.close();
                }
            }
            catch (Exception e) {
                LOG.log(Level.WARNING, "exception while updating seq", e);
            }
            finally {
                releaseConnection(connection);
            }
        }
    }
    
}


