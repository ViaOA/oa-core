package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportVersion;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportPP;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportVersionPP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAOne;
import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAArray;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAString;

@OAClass(useDataSource = false, localOnly = true)
public class ReportVersionSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(ReportVersionSearch.class.getName());

	public static final String P_Created = "Created";
	public static final String P_Report = "Report";
	public static final String P_UseReportSearch = "UseReportSearch";
	public static final String P_MaxResults = "MaxResults";

	protected OADateTime created;
	protected Report report;
	protected boolean useReportSearch;
	protected ReportSearch searchReport;
	protected int maxResults;

	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
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

	@OAOne
	public Report getReport() {
		if (report == null) {
			report = (Report) getObject(P_Report);
		}
		return report;
	}

	public void setReport(Report newValue) {
		Report old = this.report;
		this.report = newValue;
		firePropertyChange(P_Report, old, this.report);
	}

	public boolean getUseReportSearch() {
		return useReportSearch;
	}

	public void setUseReportSearch(boolean newValue) {
		boolean old = this.useReportSearch;
		this.useReportSearch = newValue;
		firePropertyChange(P_UseReportSearch, old, this.useReportSearch);
	}

	public ReportSearch getReportSearch() {
		return this.searchReport;
	}

	public void setReportSearch(ReportSearch newValue) {
		this.searchReport = newValue;
	}

	public void reset() {
		setCreated(null);
		setReport(null);
		setUseReportSearch(false);
	}

	public boolean isDataEntered() {
		if (getCreated() != null) {
			return true;
		}
		if (getReport() != null) {
			return true;
		}
		if (getUseReportSearch()) {
			return true;
		}
		return false;
	}

	protected String extraWhere;
	protected Object[] extraWhereParams;
	protected OAFilter<ReportVersion> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<ReportVersion> f = new OAQueryFilter<ReportVersion>(ReportVersion.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<ReportVersion> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<ReportVersion> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<ReportVersion> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];
		OAFinder finder = null;
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportVersion.P_Created + " = ?";
			args = OAArray.add(Object.class, args, this.created);
		}
		if (!useReportSearch && getReport() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportVersionPP.reportTemplate().report().pp + " = ?";
			args = OAArray.add(Object.class, args, getReport());
			String pp = ReportPP.reportTemplates().reportVersions().pp;
			finder = new OAFinder<Report, ReportVersion>(getReport(), pp);
		}

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<ReportVersion> select = new OASelect<ReportVersion>(ReportVersion.class, sql, args, sortOrder);
		if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
			select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
		} else {
			select.setFilter(this.getCustomFilter());
		}
		select.setDataSourceFilter(this.getDataSourceFilter());
		select.setFinder(finder);
		if (getMaxResults() > 0) {
			select.setMax(getMaxResults());
		}
		if (useReportSearch && getReportSearch() != null) {
			getReportSearch().appendSelect(ReportVersionPP.reportTemplate().report().pp, select);
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
			sql += prefix + ReportVersion.P_Created + " = ?";
			args = OAArray.add(Object.class, args, this.created);
		}
		if (!useReportSearch && getReport() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportVersionPP.reportTemplate().report().pp + " = ?";
			args = OAArray.add(Object.class, args, getReport());
		}
		if (useReportSearch && getReportSearch() != null) {
			getReportSearch().appendSelect(prefix + ReportVersionPP.reportTemplate().report().pp, select);
		}
		select.add(sql, args);
	}

	private OAFilter<ReportVersion> filterDataSourceFilter;

	public OAFilter<ReportVersion> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<ReportVersion>() {
			@Override
			public boolean isUsed(ReportVersion reportVersion) {
				return ReportVersionSearch.this.isUsedForDataSourceFilter(reportVersion);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<ReportVersion> filterCustomFilter;

	public OAFilter<ReportVersion> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<ReportVersion>() {
			@Override
			public boolean isUsed(ReportVersion reportVersion) {
				boolean b = ReportVersionSearch.this.isUsedForCustomFilter(reportVersion);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(ReportVersion searchReportVersion) {
		return true;
	}

	public boolean isUsedForCustomFilter(ReportVersion searchReportVersion) {
		return true;
	}
}
