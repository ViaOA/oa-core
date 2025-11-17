/**
 * Provides client-side implementations for OA's distributed data-source layer.
 * <p>
 * Classes in this package enable OA applications to access remote
 * {@link com.viaoa.datasource.OADataSource} instances hosted on OA servers.
 * Communication occurs via {@link com.viaoa.sync.remote.RemoteClientInterface}
 * and the OA synchronization framework.
 *
 * <h2>Key Component</h2>
 * <ul>
 *   <li>{@link com.viaoa.datasource.clientserver.OADataSourceClient} —
 *       client-side proxy for remote OADataSource operations.</li>
 * </ul>
 *
 * @since OA 4.0
 */
package com.viaoa.datasource.clientserver;
