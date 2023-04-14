package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportInstance;
import com.auto.dev.reportercorp.model.oa.ReportVersion;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportInstancePP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAOne;
import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAArray;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAString;

@OAClass(useDataSource = false, localOnly = true)
public class ReportInstanceSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(ReportInstanceSearch.class.getName());

	public static final String P_Created = "Created";
	public static final String P_Created2 = "Created2";
	public static final String P_StoreNumber = "StoreNumber";
	public static final String P_StoreNumber2 = "StoreNumber2";
	public static final String P_FileName = "FileName";
	public static final String P_Report = "Report";
	public static final String P_UseReportSearch = "UseReportSearch";
	public static final String P_ReportVersion = "ReportVersion";
	public static final String P_UseReportVersionSearch = "UseReportVersionSearch";
	public static final String P_MaxResults = "MaxResults";

	protected OADateTime created;
	protected OADateTime created2;
	protected int storeNumber;
	protected int storeNumber2;
	protected String fileName;
	protected Report report;
	protected boolean useReportSearch;
	protected ReportSearch searchReport;
	protected ReportVersion reportVersion;
	protected boolean useReportVersionSearch;
	protected ReportVersionSearch searchReportVersion;
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

	public int getStoreNumber() {
		return storeNumber;
	}

	public void setStoreNumber(int newValue) {
		int old = storeNumber;
		fireBeforePropertyChange(P_StoreNumber, old, newValue);
		this.storeNumber = newValue;
		firePropertyChange(P_StoreNumber, old, this.storeNumber);
		if (isLoading()) {
			return;
		}
		if (storeNumber > storeNumber2) {
			setStoreNumber2(this.storeNumber);
		}
	}

	public int getStoreNumber2() {
		return storeNumber2;
	}

	public void setStoreNumber2(int newValue) {
		int old = storeNumber2;
		fireBeforePropertyChange(P_StoreNumber2, old, newValue);
		this.storeNumber2 = newValue;
		firePropertyChange(P_StoreNumber2, old, this.storeNumber2);
		if (isLoading()) {
			return;
		}
		if (storeNumber > storeNumber2) {
			setStoreNumber(this.storeNumber2);
		}
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String newValue) {
		String old = fileName;
		fireBeforePropertyChange(P_FileName, old, newValue);
		this.fileName = newValue;
		firePropertyChange(P_FileName, old, this.fileName);
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

	@OAOne
	public ReportVersion getReportVersion() {
		if (reportVersion == null) {
			reportVersion = (ReportVersion) getObject(P_ReportVersion);
		}
		return reportVersion;
	}

	public void setReportVersion(ReportVersion newValue) {
		ReportVersion old = this.reportVersion;
		this.reportVersion = newValue;
		firePropertyChange(P_ReportVersion, old, this.reportVersion);
	}

	public boolean getUseReportVersionSearch() {
		return useReportVersionSearch;
	}

	public void setUseReportVersionSearch(boolean newValue) {
		boolean old = this.useReportVersionSearch;
		this.useReportVersionSearch = newValue;
		firePropertyChange(P_UseReportVersionSearch, old, this.useReportVersionSearch);
	}

	public ReportVersionSearch getReportVersionSearch() {
		return this.searchReportVersion;
	}

	public void setReportVersionSearch(ReportVersionSearch newValue) {
		this.searchReportVersion = newValue;
	}

	public void reset() {
		setCreated(null);
		setCreated2(null);
		setStoreNumber(0);
		setNull(P_StoreNumber);
		setStoreNumber2(0);
		setNull(P_StoreNumber2);
		setFileName(null);
		setReport(null);
		setUseReportSearch(false);
		setReportVersion(null);
		setUseReportVersionSearch(false);
	}

	public boolean isDataEntered() {
		if (getCreated() != null) {
			return true;
		}
		if (!isNull(P_StoreNumber)) {
			return true;
		}
		if (getFileName() != null) {
			return true;
		}
		if (getReport() != null) {
			return true;
		}
		if (getUseReportSearch()) {
			return true;
		}
		if (getReportVersion() != null) {
			return true;
		}
		if (getUseReportVersionSearch()) {
			return true;
		}
		return false;
	}

	protected String extraWhere;
	protected Object[] extraWhereParams;
	protected OAFilter<ReportInstance> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<ReportInstance> f = new OAQueryFilter<ReportInstance>(ReportInstance.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<ReportInstance> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<ReportInstance> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<ReportInstance> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += ReportInstance.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + ReportInstance.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += ReportInstance.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		if (!isNull(P_StoreNumber)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_StoreNumber2) && storeNumber != storeNumber2) {
				sql += ReportInstance.P_StoreNumber + " >= ?";
				args = OAArray.add(Object.class, args, getStoreNumber());
				sql += " AND " + ReportInstance.P_StoreNumber + " <= ?";
				args = OAArray.add(Object.class, args, getStoreNumber2());
			} else {
				sql += ReportInstance.P_StoreNumber + " = ?";
				args = OAArray.add(Object.class, args, getStoreNumber());
			}
		}
		if (OAString.isNotEmpty(this.fileName)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(fileName);
			if (val.indexOf("%") >= 0) {
				sql += ReportInstance.P_FileName + " LIKE ?";
			} else {
				sql += ReportInstance.P_FileName + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (!useReportSearch && getReport() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstancePP.reportVersion().reportTemplate().report().pp + " = ?";
			args = OAArray.add(Object.class, args, getReport());
		}
		if (!useReportVersionSearch && getReportVersion() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstancePP.reportVersion().pp + " = ?";
			args = OAArray.add(Object.class, args, getReportVersion());
		}

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<ReportInstance> select = new OASelect<ReportInstance>(ReportInstance.class, sql, args, sortOrder);
		if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
			select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
		} else {
			select.setFilter(this.getCustomFilter());
		}
		select.setDataSourceFilter(this.getDataSourceFilter());
		if (getMaxResults() > 0) {
			select.setMax(getMaxResults());
		}
		if (useReportSearch && getReportSearch() != null) {
			getReportSearch().appendSelect(ReportInstancePP.reportVersion().reportTemplate().report().pp, select);
		}
		if (useReportVersionSearch && getReportVersionSearch() != null) {
			getReportVersionSearch().appendSelect(ReportInstancePP.reportVersion().pp, select);
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
				sql += prefix + ReportInstance.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + prefix + ReportInstance.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += prefix + ReportInstance.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		if (!isNull(P_StoreNumber)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_StoreNumber2) && storeNumber != storeNumber2) {
				sql += prefix + ReportInstance.P_StoreNumber + " >= ?";
				args = OAArray.add(Object.class, args, getStoreNumber());
				sql += " AND " + prefix + ReportInstance.P_StoreNumber + " <= ?";
				args = OAArray.add(Object.class, args, getStoreNumber2());
			} else {
				sql += prefix + ReportInstance.P_StoreNumber + " = ?";
				args = OAArray.add(Object.class, args, getStoreNumber());
			}
		}
		if (OAString.isNotEmpty(this.fileName)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(fileName);
			if (val.indexOf("%") >= 0) {
				sql += prefix + ReportInstance.P_FileName + " LIKE ?";
			} else {
				sql += prefix + ReportInstance.P_FileName + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (!useReportSearch && getReport() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstancePP.reportVersion().reportTemplate().report().pp + " = ?";
			args = OAArray.add(Object.class, args, getReport());
		}
		if (useReportSearch && getReportSearch() != null) {
			getReportSearch().appendSelect(prefix + ReportInstancePP.reportVersion().reportTemplate().report().pp, select);
		}
		if (!useReportVersionSearch && getReportVersion() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstancePP.reportVersion().pp + " = ?";
			args = OAArray.add(Object.class, args, getReportVersion());
		}
		if (useReportVersionSearch && getReportVersionSearch() != null) {
			getReportVersionSearch().appendSelect(prefix + ReportInstancePP.reportVersion().pp, select);
		}
		select.add(sql, args);
	}

	private OAFilter<ReportInstance> filterDataSourceFilter;

	public OAFilter<ReportInstance> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<ReportInstance>() {
			@Override
			public boolean isUsed(ReportInstance reportInstance) {
				return ReportInstanceSearch.this.isUsedForDataSourceFilter(reportInstance);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<ReportInstance> filterCustomFilter;

	public OAFilter<ReportInstance> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<ReportInstance>() {
			@Override
			public boolean isUsed(ReportInstance reportInstance) {
				boolean b = ReportInstanceSearch.this.isUsedForCustomFilter(reportInstance);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(ReportInstance searchReportInstance) {
		return true;
	}

	public boolean isUsedForCustomFilter(ReportInstance searchReportInstance) {
		return true;
	}
}
