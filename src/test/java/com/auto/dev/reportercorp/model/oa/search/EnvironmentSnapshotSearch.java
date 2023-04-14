package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.EnvironmentSnapshot;
import com.viaoa.annotation.OAClass;
import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAString;

@OAClass(useDataSource = false, localOnly = true)
public class EnvironmentSnapshotSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(EnvironmentSnapshotSearch.class.getName());

	public static final String P_MaxResults = "MaxResults";

	protected int maxResults;

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int newValue) {
		fireBeforePropertyChange(P_MaxResults, this.maxResults, newValue);
		int old = maxResults;
		this.maxResults = newValue;
		firePropertyChange(P_MaxResults, old, this.maxResults);
	}

	public void reset() {
	}

	public boolean isDataEntered() {
		return false;
	}

	protected String extraWhere;
	protected Object[] extraWhereParams;
	protected OAFilter<EnvironmentSnapshot> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<EnvironmentSnapshot> f = new OAQueryFilter<EnvironmentSnapshot>(EnvironmentSnapshot.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<EnvironmentSnapshot> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<EnvironmentSnapshot> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<EnvironmentSnapshot> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<EnvironmentSnapshot> select = new OASelect<EnvironmentSnapshot>(EnvironmentSnapshot.class, sql, args, sortOrder);
		if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
			select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
		} else {
			select.setFilter(this.getCustomFilter());
		}
		select.setDataSourceFilter(this.getDataSourceFilter());
		if (getMaxResults() > 0) {
			select.setMax(getMaxResults());
		}
		return select;
	}

	public void appendSelect(final String fromName, final OASelect select) {
		final String prefix = fromName + ".";
		String sql = "";
		Object[] args = new Object[0];
		select.add(sql, args);
	}

	private OAFilter<EnvironmentSnapshot> filterDataSourceFilter;

	public OAFilter<EnvironmentSnapshot> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<EnvironmentSnapshot>() {
			@Override
			public boolean isUsed(EnvironmentSnapshot environmentSnapshot) {
				return EnvironmentSnapshotSearch.this.isUsedForDataSourceFilter(environmentSnapshot);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<EnvironmentSnapshot> filterCustomFilter;

	public OAFilter<EnvironmentSnapshot> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<EnvironmentSnapshot>() {
			@Override
			public boolean isUsed(EnvironmentSnapshot environmentSnapshot) {
				boolean b = EnvironmentSnapshotSearch.this.isUsedForCustomFilter(environmentSnapshot);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(EnvironmentSnapshot searchEnvironmentSnapshot) {
		return true;
	}

	public boolean isUsedForCustomFilter(EnvironmentSnapshot searchEnvironmentSnapshot) {
		return true;
	}
}
