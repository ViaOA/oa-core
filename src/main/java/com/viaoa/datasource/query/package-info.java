/**
 * Provides parsing and lexical analysis utilities for OA's object query language.
 * <p>
 * The {@code com.viaoa.datasource.query} package converts high-level, object-based
 * query strings (such as {@code "customer.lastName = 'Smith' and active = true"})
 * into structured {@link com.viaoa.datasource.query.OAQueryToken} sequences.
 * These tokens are then translated by the active {@link com.viaoa.datasource.OADataSource}
 * implementation into the appropriate native query language — typically SQL,
 * REST parameters, or distributed filter expressions.
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Provide a unified, lightweight parser for OA's object query syntax.</li>
 *   <li>Enable model-driven queries without requiring SQL or REST syntax knowledge.</li>
 *   <li>Serve as the first step in query transformation pipelines used by
 *       {@link com.viaoa.datasource.jdbc.OADataSourceJDBC} and other DataSources.</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.viaoa.datasource.query.OAQueryTokenizer} —
 *       converts query text into token streams.</li>
 *   <li>{@link com.viaoa.datasource.query.OAQueryTokenManager} —
 *       performs lexical scanning and classification of query symbols.</li>
 *   <li>{@link com.viaoa.datasource.query.OAQueryToken} —
 *       represents an individual token (operator, literal, or keyword).</li>
 *   <li>{@link com.viaoa.datasource.query.OAQueryTokenType} —
 *       defines the constants used for token categorization.</li>
 * </ul>
 *
 * <h2>Supported Query Features</h2>
 * <ul>
 *   <li>Comparison operators: =, !=, &lt;, &lt;=, &gt;, &gt;=</li>
 *   <li>Logical operators: AND, OR, NOT</li>
 *   <li>String literals, escape sequences, and quoted identifiers</li>
 *   <li>Parentheses and nested sub-expressions</li>
 *   <li>Pattern matching and set membership ({@code LIKE}, {@code IN (...)})</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * String query = "customer.age >= 18 and (status = 'A' or creditLimit > 500)";
 * OAQueryTokenizer qt = new OAQueryTokenizer();
 * Vector<OAQueryToken> tokens = qt.convertToTokens(query);
 * }</pre>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Lightweight, no external dependencies.</li>
 *   <li>Non-thread-safe by design — instantiate per query.</li>
 *   <li>Used internally by OADataSource implementations for dynamic query translation.</li>
 * </ul>
 *
 * @see com.viaoa.datasource.OADataSource
 * @see com.viaoa.datasource.jdbc.OADataSourceJDBC
 * @see com.viaoa.datasource.query.OAQueryTokenizer
 */
package com.viaoa.datasource.query;
