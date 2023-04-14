package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.viaoa.annotation.OAClass;
import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAString;

@OAClass(useDataSource = false, localOnly = true)
public class ReporterCorpSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(ReporterCorpSearch.class.getName());

	public static final String P_NodeName = "NodeName";
	public static final String P_MaxResults = "MaxResults";

	protected String nodeName;
	protected int maxResults;

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String newValue) {
		String old = nodeName;
		fireBeforePropertyChange(P_NodeName, old, newValue);
		this.nodeName = newValue;
		firePropertyChange(P_NodeName, old, this.nodeName);
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
		setNodeName(null);
	}

	public boolean isDataEntered() {
		if (getNodeName() != null) {
			return true;
		}
		return false;
	}

	protected String extraWhere;
	protected Object[] extraWhereParams;
	protected OAFilter<ReporterCorp> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<ReporterCorp> f = new OAQueryFilter<ReporterCorp>(ReporterCorp.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<ReporterCorp> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<ReporterCorp> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<ReporterCorp> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];
		if (OAString.isNotEmpty(this.nodeName)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(nodeName);
			if (val.indexOf("%") >= 0) {
				sql += ReporterCorp.P_NodeName + " LIKE ?";
			} else {
				sql += ReporterCorp.P_NodeName + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<ReporterCorp> select = new OASelect<ReporterCorp>(ReporterCorp.class, sql, args, sortOrder);
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
		if (OAString.isNotEmpty(this.nodeName)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(nodeName);
			if (val.indexOf("%") >= 0) {
				sql += prefix + ReporterCorp.P_NodeName + " LIKE ?";
			} else {
				sql += prefix + ReporterCorp.P_NodeName + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		select.add(sql, args);
	}

	private OAFilter<ReporterCorp> filterDataSourceFilter;

	public OAFilter<ReporterCorp> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<ReporterCorp>() {
			@Override
			public boolean isUsed(ReporterCorp reporterCorp) {
				return ReporterCorpSearch.this.isUsedForDataSourceFilter(reporterCorp);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<ReporterCorp> filterCustomFilter;

	public OAFilter<ReporterCorp> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<ReporterCorp>() {
			@Override
			public boolean isUsed(ReporterCorp reporterCorp) {
				boolean b = ReporterCorpSearch.this.isUsedForCustomFilter(reporterCorp);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(ReporterCorp searchReporterCorp) {
		return true;
	}

	public boolean isUsedForCustomFilter(ReporterCorp searchReporterCorp) {
		return true;
	}
}
