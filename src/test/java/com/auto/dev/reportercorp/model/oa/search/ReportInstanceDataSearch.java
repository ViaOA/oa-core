package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportInstanceData;
import com.viaoa.annotation.OAClass;
import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAArray;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAString;

@OAClass(useDataSource = false, localOnly = true)
public class ReportInstanceDataSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(ReportInstanceDataSearch.class.getName());

	public static final String P_Created = "Created";
	public static final String P_Created2 = "Created2";
	public static final String P_MaxResults = "MaxResults";

	protected OADateTime created;
	protected OADateTime created2;
	protected int maxResults;

	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
		if (isLoading()) {
			return;
		}
		if (created != null) {
			if (created2 == null) {
				setCreated2(this.created.addDays(1));
			} else if (created.compareTo(created2) > 0) {
				setCreated2(this.created.addDays(1));
			}
		}
	}

	public OADateTime getCreated2() {
		return created2;
	}

	public void setCreated2(OADateTime newValue) {
		OADateTime old = created2;
		fireBeforePropertyChange(P_Created2, old, newValue);
		this.created2 = newValue;
		firePropertyChange(P_Created2, old, this.created2);
		if (created != null && created2 != null) {
			if (created.compareTo(created2) > 0) {
				setCreated(this.created2);
			}
		}
	}

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
		setCreated(null);
		setCreated2(null);
	}

	public boolean isDataEntered() {
		if (getCreated() != null) {
			return true;
		}
		return false;
	}

	protected String extraWhere;
	protected Object[] extraWhereParams;
	protected OAFilter<ReportInstanceData> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<ReportInstanceData> f = new OAQueryFilter<ReportInstanceData>(ReportInstanceData.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<ReportInstanceData> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<ReportInstanceData> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<ReportInstanceData> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += ReportInstanceData.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + ReportInstanceData.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += ReportInstanceData.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<ReportInstanceData> select = new OASelect<ReportInstanceData>(ReportInstanceData.class, sql, args, sortOrder);
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
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += prefix + ReportInstanceData.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + prefix + ReportInstanceData.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += prefix + ReportInstanceData.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		select.add(sql, args);
	}

	private OAFilter<ReportInstanceData> filterDataSourceFilter;

	public OAFilter<ReportInstanceData> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<ReportInstanceData>() {
			@Override
			public boolean isUsed(ReportInstanceData reportInstanceData) {
				return ReportInstanceDataSearch.this.isUsedForDataSourceFilter(reportInstanceData);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<ReportInstanceData> filterCustomFilter;

	public OAFilter<ReportInstanceData> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<ReportInstanceData>() {
			@Override
			public boolean isUsed(ReportInstanceData reportInstanceData) {
				boolean b = ReportInstanceDataSearch.this.isUsedForCustomFilter(reportInstanceData);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(ReportInstanceData searchReportInstanceData) {
		return true;
	}

	public boolean isUsedForCustomFilter(ReportInstanceData searchReportInstanceData) {
		return true;
	}
}
