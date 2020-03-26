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
import java.util.ArrayList;
import java.util.logging.Logger;

import com.viaoa.ds.jdbc.db.*;
import com.viaoa.ds.jdbc.*;
import com.viaoa.util.OAString;

/**
 * Methods to validate that OAObject database metadata matches database scheme.  
 * @author vvia
 *
 */
public class VerifyDelegate {
    private static Logger LOG = Logger.getLogger(VerifyDelegate.class.getName());
    OADataSourceJDBC ds;
    
    public static boolean verify(OADataSourceJDBC ds) throws Exception {
        final ArrayList<String> alError = new ArrayList<>();
        Connection connection = null;
        try {
            connection = ds.getConnection(true);
            boolean b = _verify(ds, connection, alError);

            for (String s : alError) {
                System.out.println(s);
            }
            return b;
        }
        catch (Exception e) {
            return false;
        }
        finally {
            if (connection != null) {
                ds.releaseConnection(connection);
            }
        }
    }
    /** 
	    Verifies Tables, Columns and Indexes.  Prints to console window.
	    @returns true if all tables, columns and indexes exist, else returns false if any are missing.
	*/
	private static boolean _verify(OADataSourceJDBC ds, Connection connection, final ArrayList<String> alError) throws Exception {
	    final DatabaseMetaData dbmd = connection.getMetaData();
        final DBMetaData dbx = ds.getDBMetaData();
	    ResultSet rs;
	    boolean bResult = true;
	    Table[] tables = ds.getDatabase().getTables();
	    for (int i=0; i<tables.length; i++) {
	        Table t = tables[i];
	        rs = dbmd.getTables(null,null, convertDBName(dbx, t.name), null);
	        boolean b = rs.next();
	        rs.close();
	        if (!b) {
	           bResult = false;
	           String s = "DB ERROR: Table not found: "+t.name;
	           LOG.warning(s);
	           alError.add(s);
	           continue;
	        }
	        
	        Column[] columns = t.getColumns();

	        for (int j=0; j<columns.length; j++) {
	            Column c = columns[j];
	            rs = dbmd.getColumns(null,null,convertDBName(dbx, t.name), convertDBName(dbx, c.columnName));
	            b = rs.next();
	            if (b) {
	    	        int iType = rs.getInt(5);
	    	        String sType = rs.getString(6);
	    	        int iSize = rs.getInt(7);

                    if (iType == java.sql.Types.NVARCHAR && dbx != null && dbx.databaseType == dbx.SQLSERVER) {
                        iType = java.sql.Types.VARCHAR;
                    }
	    	        
                    if (iType == java.sql.Types.FLOAT || iType == java.sql.Types.DOUBLE || iType == java.sql.Types.DECIMAL) {
                        int x = c.getSqlType();
                        if (x == java.sql.Types.FLOAT || x == java.sql.Types.DOUBLE || x == java.sql.Types.DECIMAL) {
                            iType = x;
                        }
                    }
                    
                    
	    	        if (c.getSqlType() == 0) {
	    	        	if (c.propertyName != null && c.propertyName.trim().length() != 0) {
	    	        	    String s = "DB WARNING: Column missing TYPE "+t.name+"."+c.columnName+" property: " + c.propertyName;
	    	        	    alError.add(s);
	    	        	    LOG.warning(s);
	    	        	}
	    	        }
	    	        else if (iType == c.getSqlType()) {
                        // check size
	    	            if (OAString.isEmpty(c.propertyName)) {
	                        // could be fkey, which will not have a maxLength
	    	                
	    	            }
	    	            else if (iType == java.sql.Types.VARCHAR) {
	    	        		if (iSize != c.maxLength) {
	    	        		    String s = "DB NOTE: Column SIZE mismatch: "+t.name+"."+c.columnName+" ds:"+c.maxLength+" != db:"+iSize;
	    	        		    LOG.warning(s);
	                            alError.add(s);
	
	    	        		    /*
		    	        		if (c.maxLength > iSize) {
		    	        			String s = DDLDelegate.getAlterColumnSQL(ds.getDBMetaData(), t.name, c.columnName, "VARCHAR(" + c.maxLength + ")");
		    	        	        ds.execute(s);
		    	        	        LOG.warning("-- resized Column: "+t.name+"."+c.columnName+" from " + iSize + " to " + c.maxLength+ " : " + s);
		    	        		}
		    	        		else {
		    	        			c.maxLength = iSize;
		    	        			LOG.warning("-- will use column size of " + c.maxLength);
		    	        		}
		    	        		*/
		    	        		continue;
	    	        		}
	    	        	}
	    	        }
	    	        else {
	    	        	b = false;
	    	        	// see if CLOB or LONGVARCHAR match
	    	        	if (iType == java.sql.Types.LONGVARCHAR || iType == java.sql.Types.CLOB) {
		    	        	if (c.getSqlType() == java.sql.Types.LONGVARCHAR || c.getSqlType() == java.sql.Types.CLOB) {
		    	        		b = true;
		    	        	}
	    	        	}
                        if (!b && iType == java.sql.Types.VARCHAR && dbx != null && dbx.databaseType == dbx.SQLSERVER) {
                            if (c.getSqlType() == java.sql.Types.CLOB && iSize > Math.pow(10, 9)) {
                                b = true;
                            }
                        }
                        if (!b && iType == java.sql.Types.VARBINARY && dbx != null && dbx.databaseType == dbx.SQLSERVER) {
                            if (c.getSqlType() == java.sql.Types.BLOB) {
                                b = true;
                            }
                        }
                        
	    	        	if (iType == java.sql.Types.BIT || iType == java.sql.Types.BOOLEAN) {
		    	        	if (c.getSqlType() == java.sql.Types.BIT || c.getSqlType() == java.sql.Types.BOOLEAN) {
		    	        		b = true;
		    	        	}
	    	        	}
	    	        	if (iType == java.sql.Types.SMALLINT || iType == java.sql.Types.BOOLEAN || iType == java.sql.Types.CHAR || iType == java.sql.Types.BIT) {
		    	        	if (c.getSqlType() == java.sql.Types.BIT || c.getSqlType() == java.sql.Types.BOOLEAN) {
		    	        		b = true;
		    	        	}
	    	        	}
	    	        	if (!b && (iType == java.sql.Types.REAL || iType == java.sql.Types.DOUBLE)) {
		    	        	if (c.getSqlType() == java.sql.Types.REAL || c.getSqlType() == java.sql.Types.DOUBLE) {
		    	        		b = true;
		    	        	}
	    	        	}
	    	        	if (!b && (iType == java.sql.Types.TIMESTAMP)) {
		    	        	if (c.getSqlType() == java.sql.Types.DATE || c.getSqlType() == java.sql.Types.TIME) {
		    	        		b = true;
		    	        	}
	    	        	}
	    	        	if (!b) {
	    	        	    String s = "DB ERROR: Column TYPE mismatch: "+t.name+"."+c.columnName+" ds:"+c.getSqlType()+" != db:"+iType;
	    	        	    LOG.warning(s);
                            alError.add(s);
	    	        		continue;
	    	        	}
	    	        }
	            }
	            rs.close();
	            if (!b) {
	                bResult = false;
	                String s = "DB ERROR: Column not found: "+t.name+"."+c.columnName;
	                LOG.warning(s);
                    alError.add(s);
	                continue;
	            }
	
	            if (dbx.getCaseSensitive() && c.columnLowerName != null && c.columnLowerName.length() > 0) {
		            rs = dbmd.getColumns(null,null,convertDBName(dbx, t.name),convertDBName(dbx, c.columnLowerName));
		            b = rs.next();
		            rs.close();
		            if (!b) {
		                bResult = false;
		                String s = "DB ERROR: Column not found: "+t.name+"."+c.columnLowerName;
		                LOG.warning(s);
	                    alError.add(s);
		                continue;
		            }
	            }
	
	            if (c.primaryKey) {
	            	rs = dbmd.getPrimaryKeys(null, null, convertDBName(dbx, t.name));
		            b = rs.next();
                    String colname=null;
                    String pkname=null;
		            if (b) {
		            	colname = rs.getString(4); // column name
		            	pkname = rs.getString(6); // pk name
	            		b = pkname.equalsIgnoreCase("PK"+t.name);
                        b = b || pkname.equalsIgnoreCase("PK_"+t.name);
		            }	            	
		            rs.close();
		            if (!b) {
		                String s = "DB ERROR: PK missing: "+" PK" + t.name+" "+t.name+"."+c.columnName + " - FOUND: pkName="+pkname+", colname="+colname;
		                LOG.warning(s);
                        alError.add(s);
		            }
	            }
	        	// public static String convert(DBMetaData dbmd, Column column, Object value) {
	            try {
	            	ConverterDelegate.convert(ds.getDBMetaData(), c, "1");
	            }
	            catch (Exception e) {
	                String s = "DB ERROR: ConverterDelegate wont be able to convert column type: Table:" + t.name + " Column:" + c.columnName;
	                LOG.warning(s);
                    alError.add(s);
	            	bResult = false;
	            }
	        }
            Index[] indexes = t.getIndexes();

	        for (int j=0; j<indexes.length; j++) {
	    		Index ind = indexes[j];
	    		if (ind.fkey) {
	    		    if (dbx.getFkeysAutoCreateIndex()) continue; 
	    		}

	    		rs = dbmd.getIndexInfo(null, null, convertDBName(dbx, t.name), false, false);
	            int foundCnt = 0;
	            boolean bNameMatch = false;
	            for ( ;b = rs.next(); ) {
	            	String name = rs.getString(6); // index name
                    bNameMatch |= (name != null && name.equalsIgnoreCase(ind.name));
	            	
	            	name = rs.getString(9); // column name
	            	for (int k=0; name != null && k < ind.columns.length; k++) {
	            		if (name.equalsIgnoreCase(ind.columns[k]) || name.equalsIgnoreCase(ind.columns[k]+"lower")) {
                            foundCnt++;
	            		}
	            	}
	            }
	            rs.close();

	            if (!bNameMatch || foundCnt < ind.columns.length) {
	                String s = "DB ERROR: Index not in database: "+t.name+"."+ind.name;
                    LOG.warning(s);
                    alError.add(s);
	            }
	        }
            
            
	        // check to see if there are "extra" indexes
            rs = dbmd.getIndexInfo(null, null, convertDBName(dbx, t.name), false, false);
            for ( ;b = rs.next(); ) {
                boolean bFound = false;
                String name = rs.getString(6); // index name
                if (OAString.isEmpty(name)) continue;
                for (int j=0; j<indexes.length; j++) {
                    if (name != null && name.equalsIgnoreCase(indexes[j].name)) {
                        bFound = true;
                        break;
                    }
                }
                if (!bFound) {
                    // could be pkey or fkey 
                    String s = rs.getString(9); // column name
                    for (int j=0; j<columns.length; j++) {
                        Column c = columns[j];
                        if (c.columnName.equalsIgnoreCase(s)) {
                            if (c.primaryKey) bFound = true;
                            if (c.foreignKey) {
                                bFound = true;
                            }
                        }
                    }
                    if (!bFound) {
                        s = "DB warning: DB Index not in datasource: table="+t.name+", index="+name;
                        LOG.warning(s);
                        alError.add(s);
                    }
                }
            }	        
            rs.close();
	        if (!verifyLinks(t, dbmd, ds, alError)) bResult = false;
	    }
	    return bResult;
	}

	public static boolean verifyLinks(Table t, DatabaseMetaData dbmd, OADataSourceJDBC ds, final ArrayList<String> alError) throws Exception {
		boolean bError = false;
        final DBMetaData dbx = ds.getDBMetaData();
        // verify Links
        Link[] links = t.getLinks();
        for (int i=0; links!= null && i<links.length; i++) {
        	Link link = links[i];
            Column[] cols = link.fkeys;
        	Table toTable = link.toTable;
        	Link revLink = link.getReverseLink();
        	if (revLink == null) continue;
            Column[] revCols = revLink.fkeys;
            if ((cols == null && revCols != null) || (cols != null && revCols == null)) {
                String s = "DB ERROR: key columns for link do not match: "+t.name+"."+link.propertyName;
                LOG.warning(s);
                alError.add(s);
                bError = true;
            }
            if (cols == null) continue;
            if (cols.length != revCols.length) {
                LOG.warning("DB ERROR: key columns for link do not match: "+t.name+"."+link.propertyName);
                bError = true;
            }
            for (int j=0; j<cols.length; j++) {
            	int t1 = cols[j].type;
            	int t2 = revCols[j].type;
            	if (t1 == 0) {
            		cols[j].type = t1 = t2;
            	}
            	if (t1 != t2) {
            	    String s = "DB ERROR: key columns for link do not match types: "+t.name+"."+link.propertyName;
            	    LOG.warning(s);
                    alError.add(s);
                    bError = true;
            	}
            }
            
            // check fkey index
            ResultSet rs = dbmd.getIndexInfo(null, null, convertDBName(dbx, t.name), false, false);
            boolean bFound = false;
            for ( ; rs.next(); ) {
                String name = rs.getString(6); // index name
                if (OAString.isEmpty(name)) continue;
                String s = rs.getString(9); // column name
                for (int j=0; j<cols.length; j++) {
                    Column c = cols[j];
                    if (c.columnName.equalsIgnoreCase(s)) {
                        bFound = true;
                        break;
                    }
                }
            }           
            rs.close();
            if (!bFound && !ds.getDBMetaData().getFkeysAutoCreateIndex()) {
                String s = "DB warning: Index for fkey not in Database, table="+t.name+", fkey column="+cols[0].columnName;
                LOG.warning(s);
                alError.add(s);
                bError = true;
            }
        }
        return bError;
	}
	
	protected static String convertDBName(DBMetaData dbmd, String name) {
	    if (name != null && dbmd != null) {
	        if (dbmd.databaseType == dbmd.POSTGRES) name = name.toLowerCase(); 
	        else name = name.toUpperCase();
	    }
	    return name;
	}
	
}
