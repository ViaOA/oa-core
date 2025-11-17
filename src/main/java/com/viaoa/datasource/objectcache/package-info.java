/**
 * Provides an in-memory {@link com.viaoa.datasource.OADataSource}
 * implementation and supporting iterators.
 * <p>
 * Classes in this package allow OA applications to operate without an external
 * database by storing objects directly in memory and serializing them to disk
 * when needed.
 *
 * <ul>
 *   <li>{@link com.viaoa.datasource.objectcache.OADataSourceObjectCache} —
 *       full in-memory data source with compressed save/load support.</li>
 *   <li>{@link com.viaoa.datasource.objectcache.ObjectCacheIterator} —
 *       streaming iterator for cache-based queries.</li>
 * </ul>
 */
package com.viaoa.datasource.objectcache;
