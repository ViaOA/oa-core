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
package com.viaoa.ds.jdbc.delegate;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.ds.jdbc.*;
import com.viaoa.ds.jdbc.db.*;
import com.viaoa.object.*;

/**
 * Used to get seq numbers to assign for new object ids.
 * 
 * Logging: all are set to "finer"
 * 
  */
public class AutonumberDelegate {
    private static Logger LOG = Logger.getLogger(AutonumberDelegate.class.getName());

    private static final ConcurrentHashMap<String, AtomicInteger> hashNext = new ConcurrentHashMap<String, AtomicInteger>(39, .75f);  // Table.name.upper, Integer
    private static final Object LOCK = new Object();
	
    /**
	    Assigns autonumber properties.
	    If guid is being used, then it will prefix the autonumber.
	*/
	public static void assignNumber(OADataSourceJDBC ds, OAObject object, Table table, Column column) {
        // LOG.finer("table="+table.name+", column="+column.columnName);
	    int id = getNextNumber(ds, table, column, true);
        // LOG.finer("table="+table.name+", column="+column.columnName+", nextId="+id);
	    DBMetaData dbmd = ds.getDBMetaData();
	    Object value;
	    
	    if (column.guid && dbmd.guid != null) value = dbmd.guid + "-"+id;
	    else value = new Integer(id);
	    
	    try {
	        OAObjectDSDelegate.setAssigningId(object, true);
	        OAObjectReflectDelegate.setProperty(object, column.propertyName, value, null);
	    }
	    finally {
            OAObjectDSDelegate.setAssigningId(object, false);
	    }
	}
	
	/**
	 * This is used to determine if an assigned ID needs to change the autoNextNumber ID
	 */
	public static void verifyNumberUsed(OADataSourceJDBC ds, OAObject object, Table table, Column column, final int id) {
	    if (table == null || table.name == null || column == null) return;
        // LOG.finer("table="+table.name+", column="+column.columnName+", verifyId="+id);
        for (;;) {
            int idNext = getNextNumber(ds, table, column, false);
            if (id < idNext) break;
            AtomicInteger ai = hashNext.get(table.name.toUpperCase());
            if (ai == null || ai.compareAndSet(idNext, id+1)) break; // else need to try again
        }
	}

	public static void setNextNumber(OADataSourceJDBC ds, Table table, int nextNumberToUse) {
        if (table == null || table.name == null) return;
        LOG.fine("table="+table.name+", nextNumberToUse="+nextNumberToUse);
        Column[] columns = table.getColumns();
        for (int i=0; columns != null && i < columns.length; i++) {
            Column column = columns[i];
            if (column.primaryKey) {
                verifyNumberUsed(ds, null, table, column, nextNumberToUse);
                break;
            }
        }
	}

    protected static int getNextNumber(final OADataSourceJDBC ds, final Table table, final Column pkColumn, final boolean bAutoIncrement) {
        int x = _getNextNumber(ds, table, pkColumn, bAutoIncrement);
        //LOG.finer("table="+table+", name="+table.name+", bAutoIncrement="+bAutoIncrement+", returning="+x);
        return x;
    }	
    //========================= Utilities ===========================
    private static int _getNextNumber(final OADataSourceJDBC ds, final Table table, final Column pkColumn, final boolean bAutoIncrement) {
        if (table == null || table.name == null || pkColumn == null) return -1;
        // LOG.finer("table="+table.name+", column="+pkColumn.columnName+", bAutoIncrement="+bAutoIncrement);
        
        int max = 0;
        final String hashId = table.name.toUpperCase();
        AtomicInteger ai = hashNext.get(hashId);
        if (ai == null) {
            synchronized(LOCK) {
                ai = hashNext.get(hashId);
                if (ai == null) {
                    if (ds == null) {
                        max = 1;
                    }
                    else {
                        DBMetaData dbmd = ds.getDBMetaData();
                        String query = "";
                        if (pkColumn.guid && dbmd.guid != null && dbmd.guid.length() > 0) {
                        	query = getMaxGuidQuery(dbmd, table, pkColumn);
                        }
                        else {
                        	query = getMaxIdQuery(dbmd, table, pkColumn);
                        }
    
                        Statement statement = null;
                        try {
                            statement = ds.getStatement(query);
                            ResultSet rs = statement.executeQuery(query);
                            if (rs.next()) max = (rs.getInt(1) + 1);
                            rs.close();
                            LOG.fine("table="+table.name+", column="+pkColumn.columnName+", max="+max+", query="+query+", hash="+hashNext);
                        }
                        catch (Exception e) {
                            throw new RuntimeException("OADataSource.getNextNumber() failed for "+table.name+" Query:"+query, e);
                        }
                        finally {
                            if (statement != null) ds.releaseStatement(statement);
                        }
                    }
                    ai = new AtomicInteger(max);
                	hashNext.put(hashId, ai);
                }
            }
        }
        
        if (bAutoIncrement) {
            max = ai.getAndIncrement();
        }
        else {
            max = ai.get();
        }
        //qqqqqqqqqqqqqqqq        
        //LOG.warning("table="+table.name+", column="+pkColumn.columnName+", max="+max+", ai="+ai+", bAutoIncrement="+bAutoIncrement);
        return max;
    }
    

	protected static String getMaxGuidQuery(DBMetaData dbmd, Table table, Column dbcolumn) {
        // ACCESS Version to get string value of seq number
        String column = dbcolumn.columnName;
		String s;
		String from = " from "+dbmd.leftBracket+table.name+dbmd.rightBracket;
		String where;
		if (dbmd.databaseType == dbmd.ACCESS) {
            s = "select max(val(right$("+column+", len("+column+")-"+(dbmd.guid.length()+1)+") ))";
            where = " WHERE "+column+" like '"+ dbmd.guid + "-%'";
        }
        else if (dbmd.databaseType == dbmd.DERBY) {
        	s = "select max(integer(substr("+column+", " + (dbmd.guid.length()+2) +")))";
            where = " WHERE "+column+" like '"+ dbmd.guid + "-%'";
        }
        else if (dbmd.databaseType == dbmd.SQLSERVER) {
            s = "select max(right("+column+", len("+column+")-"+(dbmd.guid.length()+1)+"))";
            where = " WHERE "+column+" like '"+ dbmd.guid + "-%'";
        }
        else {
            // MYSQL
            s = "select max(convert(right("+column+", length("+column+")-"+(dbmd.guid.length()+1)+"), UNSIGNED INTEGER))";
            where = " WHERE "+column+" like '"+ dbmd.guid + "-%'";
        }
		s = s + from + where;;
        LOG.fine("table="+table.name+", column="+dbcolumn.columnName+", query="+s);
		
		return s;
	}
		
	protected static String getMaxIdQuery(DBMetaData dbmd, Table table, Column dbcolumn) {
        String column = dbcolumn.columnName;
		String s;
        if (dbmd.databaseType == dbmd.ACCESS) {
            // ACCESS Version to get string value of seq number
            s = "select max(val("+column+"))";
        }
        else if (dbmd.databaseType == dbmd.MYSQL) {
            s = "SELECT MAX(CONVERT("+column+", UNSIGNED INTEGER))";
        }
        else {
            s = "select max("+column+")";
        }

        s += " FROM " +table.name;
        LOG.fine("table="+table.name+", column="+dbcolumn.columnName+", query="+s);
        return s;
	}
}
