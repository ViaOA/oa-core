package com.viaoa.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a list of cached objects based on date ranges (begin and end dates).
 * <p>
 * Usage:
 * <ol>
 * <li>Get input dateRange (begin and end date).
 * <li>call findMissingGaps(dateRange), to get any dateRange(s) that are not already in the cache.
 * <li>for each missing gap (maybe 0), get data and then call add()
 * <li>call getCachesItems() with the same dateRange.
 * <ol>
 *
 * @author vvia
 * @param <T> class type being cached.
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
							alMissing.add(new DateRange((OADate) rangeCache.endDate.addDay(), rangeMissing.endDate));
						}
						bStartOver = true;
						break;

					} else if (x < 0) {
						alMissing.add(new DateRange(rangeMissing.beginDate, (OADate) rangeCache.beginDate.subtractDay()));
						alMissing.remove(rangeMissing);

						x = rangeMissing.endDate.compare(rangeCache.endDate);
						if (x > 0) {
							alMissing.add(new DateRange((OADate) rangeCache.endDate.addDay(), rangeMissing.endDate));
						}
						bStartOver = true;
						break;
					} else if (x > 0) {
						alMissing.remove(rangeMissing);

						x = rangeMissing.endDate.compare(rangeCache.endDate);
						if (x > 0) {
							alMissing.add(new DateRange((OADate) rangeCache.endDate.addDay(), rangeMissing.endDate));
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

	public void clearCache() {
		alCache.clear();
	}

	public static class DateRange<T> {
		protected OADate beginDate, endDate;
		protected List<T> list;

		public DateRange(OADate beginDate, OADate endDate) {
			this.beginDate = beginDate;
			this.endDate = endDate;
		}

		public DateRange(OADate beginDate, OADate endDate, List<T> list) {
			this(beginDate, endDate);
			this.list = list;
		}

		public List<T> getList() {
			return this.list;
		}

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
