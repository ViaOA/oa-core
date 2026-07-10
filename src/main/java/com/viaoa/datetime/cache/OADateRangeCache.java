/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
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
package com.viaoa.datetime.cache;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.datetime.OADate;


/**
 * Helper for caching data that is naturally grouped by date ranges. The cache
 * tracks a collection of {@link DateRange} entries, each with a begin and end
 * {@link OADate} and an optional list of objects of type {@code T}. <p>
 *
 * A typical usage pattern:
 * <ol>
 *   <li>Call {@link #findMissingGaps(OADate, OADate)} to determine which
 *       portions of a requested date range are not yet present in the cache.</li>
 *   <li>For each missing gap, load data from the underlying datasource and
 *       call {@link #add(OADate, OADate, java.util.List)}.</li>
 *   <li>Call {@link #getCacheItems(OADate, OADate)} to retrieve all cached
 *       objects whose dates fall within the requested range.</li>
 * </ol>
 *
 * The cache operates on {@link OADate} ranges, but selection of individual
 * objects is driven by the abstract {@link #getDate(Object)} method, which
 * subclasses implement to expose the date associated with each cached object.
 * <p>
 *
 * This class is not synchronized; callers must provide external concurrency
 * control if it is shared across threads.
 *
 * @param <T> the type of object stored in each cached range.
 */
public abstract class OADateRangeCache<T> {

	final List<DateRange<T>> alCache = new ArrayList<>();

	/**
	 * This is used to find out the missing date range gaps when comparing the cached dateRanged items with a begin and end date range.
	 *
	 * @return list of DateRanges that are not in the catch.
	 */
	public List<DateRange<T>> findMissingGaps(DateRange dateRange) {
		if (dateRange == null) {
			return null;
		}
		return findMissingGaps(dateRange.beginDate, dateRange.endDate);
	}

	/**
	 * Used to find missing dateRanges in the cache.
	 *
	 * @return list of dateRanges that are not in the cache.
	 */
	public List<DateRange<T>> findMissingGaps(OADate beginDate, OADate endDate) {
		if (beginDate == null || endDate == null) {
			return null;
		}
		if (endDate.before(beginDate)) {
			return null;
		}

		List<DateRange<T>> alMissing = new ArrayList<>();
		alMissing.add(new DateRange(beginDate, endDate));

		boolean bStartOver = true;
		for (; bStartOver;) {
			bStartOver = false;

			for (DateRange rangeCache : alCache) {

				for (DateRange rangeMissing : alMissing) {
					if (rangeMissing.endDate.before(rangeCache.beginDate)) {
						continue;
					}
					if (rangeMissing.beginDate.after(rangeCache.endDate)) {
						continue;
					}

					int x = rangeMissing.beginDate.compare(rangeCache.beginDate);

					if (x == 0) {
						alMissing.remove(rangeMissing);

						x = rangeMissing.endDate.compare(rangeCache.endDate);
						if (x > 0) {
							alMissing.add(new DateRange((OADate) rangeCache.endDate.plusDay(), rangeMissing.endDate));
						}
						bStartOver = true;
						break;

					} else if (x < 0) {
						alMissing.add(new DateRange(rangeMissing.beginDate, (OADate) rangeCache.beginDate.minusDay()));
						alMissing.remove(rangeMissing);

						x = rangeMissing.endDate.compare(rangeCache.endDate);
						if (x > 0) {
							alMissing.add(new DateRange((OADate) rangeCache.endDate.plusDay(), rangeMissing.endDate));
						}
						bStartOver = true;
						break;
					} else if (x > 0) {
						alMissing.remove(rangeMissing);

						x = rangeMissing.endDate.compare(rangeCache.endDate);
						if (x > 0) {
							alMissing.add(new DateRange((OADate) rangeCache.endDate.plusDay(), rangeMissing.endDate));
						}
						bStartOver = true;
						break;
					}

					if (bStartOver) {
						break;
					}
				}
				if (bStartOver) {
					break;
				}
			}
		}
		return alMissing;
	}

	/**
	 * Add dateRange with list of objects <T> to the cache.
	 */
	public void add(OADate beginDate, OADate endDate, List<T> list) {
		alCache.add(new DateRange(beginDate, endDate, list));
	}

	/**
	 * Add dateRange with list of objects <T> to the cache.
	 */
	public void add(DateRange dateRange) {
		alCache.add(dateRange);
	}

	/**
	 * Find all of the items in cache for a dateRange.
	 *
	 * @see #findMissingGaps(DateRange) to first find and add any missing dateRange gaps.
	 */
	public List<T> getCacheItems(final OADate beginDate, final OADate endDate) {
		if (beginDate == null || endDate == null) {
			return null;
		}

		final List<T> al = new ArrayList<>();
		for (DateRange<T> rangeCache : alCache) {
			if (endDate.before(rangeCache.beginDate)) {
				continue;
			}
			if (beginDate.after(rangeCache.endDate)) {
				continue;
			}

			List<T> alx = rangeCache.getList();
			if (alx == null) {
				continue;
			}

			for (T obj : alx) {
				OADate date = getDate(obj);
				if (date == null) {
					continue;
				}
				if (date.before(beginDate)) {
					continue;
				}
				if (date.after(endDate)) {
					continue;
				}
				if (!al.contains(obj)) {
					al.add(obj);
				}
			}
		}
		return al;
	}

	/**
	 * Clears all cached date ranges and their associated items.
	 */
	public void clearCache() {
		alCache.clear();
	}

	/**
	 * Inclusive date range and optional cached items for that range.
	 *
	 * @param <T> item type stored for the range
	 */
	public static class DateRange<T> {
		/**
		 * Inclusive begin and end dates for this range.
		 */
		protected OADate beginDate, endDate;

		/**
		 * Cached items associated with this range.
		 */
		protected List<T> list;

		/**
		 * Creates a date range without associated cached items.
		 *
		 * @param beginDate inclusive begin date
		 * @param endDate inclusive end date
		 */
		public DateRange(OADate beginDate, OADate endDate) {
			this.beginDate = beginDate;
			this.endDate = endDate;
		}

		/**
		 * Creates a date range with associated cached items.
		 *
		 * @param beginDate inclusive begin date
		 * @param endDate inclusive end date
		 * @param list cached items for this range
		 */
		public DateRange(OADate beginDate, OADate endDate, List<T> list) {
			this(beginDate, endDate);
			this.list = list;
		}

		/**
		 * Returns cached items associated with this range.
		 *
		 * @return cached items, or {@code null}
		 */
		public List<T> getList() {
			return this.list;
		}

		/**
		 * Sets cached items associated with this range.
		 *
		 * @param list cached items for this range
		 */
		public void setList(List<T> list) {
			this.list = list;
		}
	}

	/**
	 * This is used when calling getCacheItems to filter out objects in the cache.
	 *
	 * @return data value used for this object in the cache.
	 */
	protected abstract OADate getDate(T obj);
}
