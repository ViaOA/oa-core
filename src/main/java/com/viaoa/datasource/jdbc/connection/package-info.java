/**
 * Provides low-level JDBC connection pooling and statement management for OA's JDBC data source.
 * <p>
 * Classes in this package maintain and optimize database connections, manage
 * statement reuse, and coordinate transactional batching between the JDBC driver
 * and OA's transaction framework.
 * <ul>
 *   <li>{@link com.viaoa.datasource.jdbc.connection.ConnectionPool} — dynamic connection pool manager.</li>
 *   <li>{@link com.viaoa.datasource.jdbc.connection.OAConnection} — pooled connection wrapper with statement reuse.</li>
 * </ul>
 *
 * These classes are internal components of {@link com.viaoa.datasource.jdbc.OADataSourceJDBC}.
 *
 * @since OA 4.0
 */
package com.viaoa.datasource.jdbc.connection;
