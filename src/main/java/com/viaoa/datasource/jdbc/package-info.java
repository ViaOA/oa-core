/**
 * Provides the JDBC implementation of the OADataSource abstraction.
 * <p>
 * Classes in this package translate OA object queries and filters into native SQL commands
 * and manage all JDBC interactions — including connection pooling, statement preparation,
 * transaction management, and schema generation.
 * <p>
 * The central class {@link com.viaoa.datasource.jdbc.OADataSourceJDBC} implements the
 * {@link com.viaoa.datasource.OADataSourceInterface}, serving as the concrete bridge between
 * the OA Object Graph and relational databases.
 * <p>
 * Subpackages include:
 * <ul>
 *   <li>{@code query} — query tokenization and parsing utilities</li>
 *   <li>{@code meta} — database metadata reflection and schema helpers</li>
 * </ul>
 * <p>
 * For developers implementing custom persistence layers, this package
 * illustrates how OA’s object-centric API can be bound to standard JDBC.
 *
 * @author Vince Via
 * @since OA 4.0
 */
package com.viaoa.datasource.jdbc;
