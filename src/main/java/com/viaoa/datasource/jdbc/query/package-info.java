/**
 * Provides the query translation and result-streaming layer for the OA JDBC data source.
 * <p>
 * Classes in this package convert OA object-graph queries into SQL and iterate over JDBC
 * {@link java.sql.ResultSet}s to construct OAObjects. They form the bridge between the
 * object-model abstraction and relational database access.
 * <ul>
 *   <li>{@link com.viaoa.datasource.jdbc.query.QueryConverter} — Parses and builds SQL from OA queries.</li>
 *   <li>{@link com.viaoa.datasource.jdbc.query.ResultSetIterator} — Executes SQL and streams results into OAObjects.</li>
 *   <li>{@link com.viaoa.datasource.jdbc.query.FreeTextConverter} — Handles database-specific full-text syntax.</li>
 * </ul>
 * This layer is internal to OA-JDBC and typically used indirectly via
 * {@link com.viaoa.datasource.jdbc.db.DataAccessObject}.
 */
package com.viaoa.datasource.jdbc.query;
