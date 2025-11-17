/**
 * Provides REST-based client/server data-source communication for OA.
 * <p>
 * The classes in this package define REST interfaces and implementations
 * that allow OA clients to interact with OA servers using JSON over HTTP.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.viaoa.datasource.rest.OADataSourceRestClient} — client-side proxy.</li>
 *   <li>{@link com.viaoa.datasource.rest.OADataSourceRestImpl} — server-side delegate.</li>
 *   <li>{@link com.viaoa.datasource.rest.RemoteRestClientInterface} — synchronization protocol.</li>
 *   <li>{@link com.viaoa.datasource.rest.RemoteRestClientImpl} — REST-aware base client.</li>
 * </ul>
 *
 * @since OA 4.0
 */
package com.viaoa.datasource.rest;
