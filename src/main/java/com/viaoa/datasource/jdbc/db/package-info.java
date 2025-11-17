/**
 * Provides the database schema-reflection model for the OA JDBC subsystem.
 * <p>
 * Classes in this package describe the logical and physical structure of
 * relational databases, including tables, columns, indexes, relationships,
 * and vendor-specific metadata.  They are used internally by
 * {@link com.viaoa.datasource.jdbc.OADataSourceJDBC} to translate
 * OAObject graphs into SQL statements.
 * </p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Represent database tables, columns, and indexes as Java objects.</li>
 *   <li>Model one-to-many, many-to-one, and many-to-many relationships.</li>
 *   <li>Expose vendor-specific behavior and keywords through {@link DBMetaData}.</li>
 *   <li>Provide metadata to {@link com.viaoa.datasource.jdbc.db.DataAccessObject}
 *       for result-set population.</li>
 * </ul>
 *
 * <h2>Thread-Safety</h2>
 * Schema models are immutable after load and safe for concurrent read access.
 *
 * @author Vince Via
 * @since OA 4.0
 */
package com.viaoa.datasource.jdbc.db;