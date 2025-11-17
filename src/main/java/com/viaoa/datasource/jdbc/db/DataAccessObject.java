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
package com.viaoa.datasource.jdbc.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.viaoa.object.OAObject;

/**
 * Defines the contract for classes that populate {@link com.viaoa.object.OAObject}
 * instances from JDBC {@link java.sql.ResultSet} data.
 * <p>
 * Implementations of {@code DataAccessObject} handle mapping result set
 * columns to object properties, guided by {@link Table} and {@link Column}
 * metadata. Each {@code ResultSetInfo} tracks caching state and the
 * current cursor position.
 * </p>
 *
 * @see Table
 * @see com.viaoa.datasource.jdbc.OADataSourceJDBC
 */
public interface DataAccessObject {

    public class ResultSetInfo {
        ResultSet rs;
        boolean foundInCache;
        public void reset(ResultSet rs) {
            this.rs = rs;
            foundInCache = false;
        }
        public boolean getFoundInCache() {
            return this.foundInCache;
        }
        public void setFoundInCache(boolean b) {
            this.foundInCache = b;
        }
        public ResultSet getResultSet() {
            return rs;
        }
    }
    
    public OAObject getObject(ResultSetInfo rsi) throws SQLException;
    public String getPkeySelectColumns();
    public String getSelectColumns();
    
}
