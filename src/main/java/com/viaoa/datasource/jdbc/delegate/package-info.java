/**
 * Internal delegate layer for the OA JDBC subsystem.
 * <p>
 * Classes in this package encapsulate focused responsibilities used by
 * {@link com.viaoa.datasource.jdbc.OADataSourceJDBC}: SQL generation,
 * vendor metadata wiring, autonumber assignment, DDL utilities, logging
 * and recovery parsing, and query execution helpers.
 * The design keeps the main DataSource small while enabling shared,
 * package-scoped access to JDBC/db internals.
 * </p>
 *
 * <h2>Key components</h2>
 * <ul>
 *   <li>Autonumber assignment and verification</li>
 *   <li>Value conversion and SQL literal handling</li>
 *   <li>Vendor metadata synchronization</li>
 *   <li>DDL generation helpers</li>
 *   <li>INSERT/SELECT/DELETE execution utilities</li>
 *   <li>Structured DB logging and recovery parsing</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * Not intended for direct application use; leveraged internally by the JDBC DataSource.
 *
 * @since OA 4.0
 */
package com.viaoa.datasource.jdbc.delegate;
