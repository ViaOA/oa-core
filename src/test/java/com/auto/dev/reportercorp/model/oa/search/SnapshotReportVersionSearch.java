package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.SnapshotReport;
import com.auto.dev.reportercorp.model.oa.SnapshotReportVersion;
import com.auto.dev.reportercorp.model.oa.propertypath.SnapshotReportPP;
import com.auto.dev.reportercorp.model.oa.propertypath.SnapshotReportVersionPP;
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
public class SnapshotReportVersionSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(SnapshotReportVersionSearch.class.getName());

	public static final String P_Created = "Created";
	public static final String P_Report = "Report";
	public static final String P_UseReportSearch = "UseReportSearch";
	public static final String P_MaxResults = "MaxResults";

	protected OADateTime created;
	protected SnapshotReport report;
	protected boolean useReportSearch;
	protected SnapshotReportSearch searchReport;
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
	public SnapshotReport getReport() {
		if (report == null) {
			report = (SnapshotReport) getObject(P_Report);
		}
		return report;
	}

	public void setReport(SnapshotReport newValue) {
		SnapshotReport old = this.report;
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

	public SnapshotReportSearch getReportSearch() {
		return this.searchReport;
	}

	public void setReportSearch(SnapshotReportSearch newValue) {
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
	protected OAFilter<SnapshotReportVersion> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<SnapshotReportVersion> f = new OAQueryFilter<SnapshotReportVersion>(SnapshotReportVersion.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<SnapshotReportVersion> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<SnapshotReportVersion> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<SnapshotReportVersion> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];
		OAFinder finder = null;
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += SnapshotReportVersion.P_Created + " = ?";
			args = OAArray.add(Object.class, args, this.created);
		}
		if (!useReportSearch && getReport() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += SnapshotReportVersionPP.snapshotReportTemplate().snapshotReport().pp + " = ?";
			args = OAArray.add(Object.class, args, getReport());
			String pp = SnapshotReportPP.snapshotReportTemplates().snapshotReportVersions().pp;
			finder = new OAFinder<SnapshotReport, SnapshotReportVersion>(getReport(), pp);
		}

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<SnapshotReportVersion> select = new OASelect<SnapshotReportVersion>(SnapshotReportVersion.class, sql, args, sortOrder);
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
			getReportSearch().appendSelect(SnapshotReportVersionPP.snapshotReportTemplate().snapshotReport().pp, select);
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
			sql += prefix + SnapshotReportVersion.P_Created + " = ?";
			args = OAArray.add(Object.class, args, this.created);
		}
		if (!useReportSearch && getReport() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + SnapshotReportVersionPP.snapshotReportTemplate().snapshotReport().pp + " = ?";
			args = OAArray.add(Object.class, args, getReport());
		}
		if (useReportSearch && getReportSearch() != null) {
			getReportSearch().appendSelect(prefix + SnapshotReportVersionPP.snapshotReportTemplate().snapshotReport().pp, select);
		}
		select.add(sql, args);
	}

	private OAFilter<SnapshotReportVersion> filterDataSourceFilter;

	public OAFilter<SnapshotReportVersion> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<SnapshotReportVersion>() {
			@Override
			public boolean isUsed(SnapshotReportVersion snapshotReportVersion) {
				return SnapshotReportVersionSearch.this.isUsedForDataSourceFilter(snapshotReportVersion);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<SnapshotReportVersion> filterCustomFilter;

	public OAFilter<SnapshotReportVersion> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<SnapshotReportVersion>() {
			@Override
			public boolean isUsed(SnapshotReportVersion snapshotReportVersion) {
				boolean b = SnapshotReportVersionSearch.this.isUsedForCustomFilter(snapshotReportVersion);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(SnapshotReportVersion searchSnapshotReportVersion) {
		return true;
	}

	public boolean isUsedForCustomFilter(SnapshotReportVersion searchSnapshotReportVersion) {
		return true;
	}
}
