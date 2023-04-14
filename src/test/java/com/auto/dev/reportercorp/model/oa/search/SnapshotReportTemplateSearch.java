package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.SnapshotReport;
import com.auto.dev.reportercorp.model.oa.SnapshotReportTemplate;
import com.auto.dev.reportercorp.model.oa.propertypath.SnapshotReportTemplatePP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAOne;
import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAArray;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAString;

@OAClass(useDataSource = false, localOnly = true)
public class SnapshotReportTemplateSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(SnapshotReportTemplateSearch.class.getName());

	public static final String P_Id = "Id";
	public static final String P_Id2 = "Id2";
	public static final String P_Created = "Created";
	public static final String P_Created2 = "Created2";
	public static final String P_Md5hash = "Md5hash";
	public static final String P_Verified = "Verified";
	public static final String P_Report = "Report";
	public static final String P_UseReportSearch = "UseReportSearch";
	public static final String P_MaxResults = "MaxResults";

	protected int id;
	protected int id2;
	protected OADateTime created;
	protected OADateTime created2;
	protected String md5hash;
	protected OADate verified;
	protected SnapshotReport report;
	protected boolean useReportSearch;
	protected SnapshotReportSearch searchReport;
	protected int maxResults;

	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		int old = id;
		fireBeforePropertyChange(P_Id, old, newValue);
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
		if (isLoading()) {
			return;
		}
		if (id > id2) {
			setId2(this.id);
		}
	}

	public int getId2() {
		return id2;
	}

	public void setId2(int newValue) {
		int old = id2;
		fireBeforePropertyChange(P_Id2, old, newValue);
		this.id2 = newValue;
		firePropertyChange(P_Id2, old, this.id2);
		if (isLoading()) {
			return;
		}
		if (id > id2) {
			setId(this.id2);
		}
	}

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

	public String getMd5hash() {
		return md5hash;
	}

	public void setMd5hash(String newValue) {
		String old = md5hash;
		fireBeforePropertyChange(P_Md5hash, old, newValue);
		this.md5hash = newValue;
		firePropertyChange(P_Md5hash, old, this.md5hash);
	}

	public OADate getVerified() {
		return verified;
	}

	public void setVerified(OADate newValue) {
		OADate old = verified;
		fireBeforePropertyChange(P_Verified, old, newValue);
		this.verified = newValue;
		firePropertyChange(P_Verified, old, this.verified);
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
		setId(0);
		setNull(P_Id);
		setId2(0);
		setNull(P_Id2);
		setCreated(null);
		setCreated2(null);
		setMd5hash(null);
		setVerified(null);
		setReport(null);
		setUseReportSearch(false);
	}

	public boolean isDataEntered() {
		if (!isNull(P_Id)) {
			return true;
		}
		if (getCreated() != null) {
			return true;
		}
		if (getMd5hash() != null) {
			return true;
		}
		if (getVerified() != null) {
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
	protected OAFilter<SnapshotReportTemplate> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<SnapshotReportTemplate> f = new OAQueryFilter<SnapshotReportTemplate>(SnapshotReportTemplate.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<SnapshotReportTemplate> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<SnapshotReportTemplate> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<SnapshotReportTemplate> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];
		OAFinder finder = null;
		if (!isNull(P_Id)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Id2) && id != id2) {
				sql += SnapshotReportTemplate.P_Id + " >= ?";
				args = OAArray.add(Object.class, args, getId());
				sql += " AND " + SnapshotReportTemplate.P_Id + " <= ?";
				args = OAArray.add(Object.class, args, getId2());
			} else {
				sql += SnapshotReportTemplate.P_Id + " = ?";
				args = OAArray.add(Object.class, args, getId());
			}
		}
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += SnapshotReportTemplate.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + SnapshotReportTemplate.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += SnapshotReportTemplate.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		if (OAString.isNotEmpty(this.md5hash)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(md5hash);
			if (val.indexOf("%") >= 0) {
				sql += SnapshotReportTemplate.P_Md5hash + " LIKE ?";
			} else {
				sql += SnapshotReportTemplate.P_Md5hash + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (verified != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += SnapshotReportTemplate.P_Verified + " = ?";
			args = OAArray.add(Object.class, args, this.verified);
		}
		if (!useReportSearch && getReport() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += SnapshotReportTemplatePP.snapshotReport().pp + " = ?";
			args = OAArray.add(Object.class, args, getReport());
			finder = new OAFinder<SnapshotReport, SnapshotReportTemplate>(getReport(), SnapshotReport.P_SnapshotReportTemplates);
		}

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<SnapshotReportTemplate> select = new OASelect<SnapshotReportTemplate>(SnapshotReportTemplate.class, sql, args, sortOrder);
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
			getReportSearch().appendSelect(SnapshotReportTemplatePP.snapshotReport().pp, select);
		}
		return select;
	}

	public void appendSelect(final String fromName, final OASelect select) {
		final String prefix = fromName + ".";
		String sql = "";
		Object[] args = new Object[0];
		if (!isNull(P_Id)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Id2) && id != id2) {
				sql += prefix + SnapshotReportTemplate.P_Id + " >= ?";
				args = OAArray.add(Object.class, args, getId());
				sql += " AND " + prefix + SnapshotReportTemplate.P_Id + " <= ?";
				args = OAArray.add(Object.class, args, getId2());
			} else {
				sql += prefix + SnapshotReportTemplate.P_Id + " = ?";
				args = OAArray.add(Object.class, args, getId());
			}
		}
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += prefix + SnapshotReportTemplate.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + prefix + SnapshotReportTemplate.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += prefix + SnapshotReportTemplate.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		if (OAString.isNotEmpty(this.md5hash)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(md5hash);
			if (val.indexOf("%") >= 0) {
				sql += prefix + SnapshotReportTemplate.P_Md5hash + " LIKE ?";
			} else {
				sql += prefix + SnapshotReportTemplate.P_Md5hash + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (verified != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + SnapshotReportTemplate.P_Verified + " = ?";
			args = OAArray.add(Object.class, args, this.verified);
		}
		if (!useReportSearch && getReport() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + SnapshotReportTemplatePP.snapshotReport().pp + " = ?";
			args = OAArray.add(Object.class, args, getReport());
		}
		if (useReportSearch && getReportSearch() != null) {
			getReportSearch().appendSelect(prefix + SnapshotReportTemplatePP.snapshotReport().pp, select);
		}
		select.add(sql, args);
	}

	private OAFilter<SnapshotReportTemplate> filterDataSourceFilter;

	public OAFilter<SnapshotReportTemplate> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<SnapshotReportTemplate>() {
			@Override
			public boolean isUsed(SnapshotReportTemplate snapshotReportTemplate) {
				return SnapshotReportTemplateSearch.this.isUsedForDataSourceFilter(snapshotReportTemplate);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<SnapshotReportTemplate> filterCustomFilter;

	public OAFilter<SnapshotReportTemplate> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<SnapshotReportTemplate>() {
			@Override
			public boolean isUsed(SnapshotReportTemplate snapshotReportTemplate) {
				boolean b = SnapshotReportTemplateSearch.this.isUsedForCustomFilter(snapshotReportTemplate);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(SnapshotReportTemplate searchSnapshotReportTemplate) {
		return true;
	}

	public boolean isUsedForCustomFilter(SnapshotReportTemplate searchSnapshotReportTemplate) {
		return true;
	}
}
