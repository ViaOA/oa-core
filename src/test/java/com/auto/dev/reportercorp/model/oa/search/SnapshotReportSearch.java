package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.SnapshotReport;
import com.viaoa.annotation.OAClass;
import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAArray;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAString;

@OAClass(useDataSource = false, localOnly = true)
public class SnapshotReportSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(SnapshotReportSearch.class.getName());

	public static final String P_Name = "Name";
	public static final String P_Created = "Created";
	public static final String P_Created2 = "Created2";
	public static final String P_FileName = "FileName";
	public static final String P_Title = "Title";
	public static final String P_Verified = "Verified";
	public static final String P_MaxResults = "MaxResults";

	protected String name;
	protected OADateTime created;
	protected OADateTime created2;
	protected String fileName;
	protected String title;
	protected OADate verified;
	protected int maxResults;

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		String old = name;
		fireBeforePropertyChange(P_Name, old, newValue);
		this.name = newValue;
		firePropertyChange(P_Name, old, this.name);
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

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String newValue) {
		String old = fileName;
		fireBeforePropertyChange(P_FileName, old, newValue);
		this.fileName = newValue;
		firePropertyChange(P_FileName, old, this.fileName);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String newValue) {
		String old = title;
		fireBeforePropertyChange(P_Title, old, newValue);
		this.title = newValue;
		firePropertyChange(P_Title, old, this.title);
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

	public void reset() {
		setName(null);
		setCreated(null);
		setCreated2(null);
		setFileName(null);
		setTitle(null);
		setVerified(null);
	}

	public boolean isDataEntered() {
		if (getName() != null) {
			return true;
		}
		if (getCreated() != null) {
			return true;
		}
		if (getFileName() != null) {
			return true;
		}
		if (getTitle() != null) {
			return true;
		}
		if (getVerified() != null) {
			return true;
		}
		return false;
	}

	protected String extraWhere;
	protected Object[] extraWhereParams;
	protected OAFilter<SnapshotReport> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<SnapshotReport> f = new OAQueryFilter<SnapshotReport>(SnapshotReport.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<SnapshotReport> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<SnapshotReport> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<SnapshotReport> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];
		if (OAString.isNotEmpty(this.name)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(name);
			if (val.indexOf("%") >= 0) {
				sql += SnapshotReport.P_Name + " LIKE ?";
			} else {
				sql += SnapshotReport.P_Name + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += SnapshotReport.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + SnapshotReport.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += SnapshotReport.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		if (OAString.isNotEmpty(this.fileName)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(fileName);
			if (val.indexOf("%") >= 0) {
				sql += SnapshotReport.P_FileName + " LIKE ?";
			} else {
				sql += SnapshotReport.P_FileName + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (OAString.isNotEmpty(this.title)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(title);
			if (val.indexOf("%") >= 0) {
				sql += SnapshotReport.P_Title + " LIKE ?";
			} else {
				sql += SnapshotReport.P_Title + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (verified != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += SnapshotReport.P_Verified + " = ?";
			args = OAArray.add(Object.class, args, this.verified);
		}

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<SnapshotReport> select = new OASelect<SnapshotReport>(SnapshotReport.class, sql, args, sortOrder);
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
		if (OAString.isNotEmpty(this.name)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(name);
			if (val.indexOf("%") >= 0) {
				sql += prefix + SnapshotReport.P_Name + " LIKE ?";
			} else {
				sql += prefix + SnapshotReport.P_Name + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += prefix + SnapshotReport.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + prefix + SnapshotReport.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += prefix + SnapshotReport.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		if (OAString.isNotEmpty(this.fileName)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(fileName);
			if (val.indexOf("%") >= 0) {
				sql += prefix + SnapshotReport.P_FileName + " LIKE ?";
			} else {
				sql += prefix + SnapshotReport.P_FileName + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (OAString.isNotEmpty(this.title)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(title);
			if (val.indexOf("%") >= 0) {
				sql += prefix + SnapshotReport.P_Title + " LIKE ?";
			} else {
				sql += prefix + SnapshotReport.P_Title + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (verified != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + SnapshotReport.P_Verified + " = ?";
			args = OAArray.add(Object.class, args, this.verified);
		}
		select.add(sql, args);
	}

	private OAFilter<SnapshotReport> filterDataSourceFilter;

	public OAFilter<SnapshotReport> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<SnapshotReport>() {
			@Override
			public boolean isUsed(SnapshotReport snapshotReport) {
				return SnapshotReportSearch.this.isUsedForDataSourceFilter(snapshotReport);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<SnapshotReport> filterCustomFilter;

	public OAFilter<SnapshotReport> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<SnapshotReport>() {
			@Override
			public boolean isUsed(SnapshotReport snapshotReport) {
				boolean b = SnapshotReportSearch.this.isUsedForCustomFilter(snapshotReport);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(SnapshotReport searchSnapshotReport) {
		return true;
	}

	public boolean isUsedForCustomFilter(SnapshotReport searchSnapshotReport) {
		return true;
	}
}
