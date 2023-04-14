package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Report;
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
public class ReportSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(ReportSearch.class.getName());

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
	protected OAFilter<Report> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<Report> f = new OAQueryFilter<Report>(Report.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<Report> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<Report> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<Report> getSelect() {
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
				sql += Report.P_Name + " LIKE ?";
			} else {
				sql += Report.P_Name + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += Report.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + Report.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += Report.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		if (OAString.isNotEmpty(this.fileName)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(fileName);
			if (val.indexOf("%") >= 0) {
				sql += Report.P_FileName + " LIKE ?";
			} else {
				sql += Report.P_FileName + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (OAString.isNotEmpty(this.title)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(title);
			if (val.indexOf("%") >= 0) {
				sql += Report.P_Title + " LIKE ?";
			} else {
				sql += Report.P_Title + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (verified != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += Report.P_Verified + " = ?";
			args = OAArray.add(Object.class, args, this.verified);
		}

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<Report> select = new OASelect<Report>(Report.class, sql, args, sortOrder);
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
				sql += prefix + Report.P_Name + " LIKE ?";
			} else {
				sql += prefix + Report.P_Name + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += prefix + Report.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + prefix + Report.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += prefix + Report.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		if (OAString.isNotEmpty(this.fileName)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(fileName);
			if (val.indexOf("%") >= 0) {
				sql += prefix + Report.P_FileName + " LIKE ?";
			} else {
				sql += prefix + Report.P_FileName + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (OAString.isNotEmpty(this.title)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(title);
			if (val.indexOf("%") >= 0) {
				sql += prefix + Report.P_Title + " LIKE ?";
			} else {
				sql += prefix + Report.P_Title + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (verified != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + Report.P_Verified + " = ?";
			args = OAArray.add(Object.class, args, this.verified);
		}
		select.add(sql, args);
	}

	private OAFilter<Report> filterDataSourceFilter;

	public OAFilter<Report> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<Report>() {
			@Override
			public boolean isUsed(Report report) {
				return ReportSearch.this.isUsedForDataSourceFilter(report);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<Report> filterCustomFilter;

	public OAFilter<Report> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<Report>() {
			@Override
			public boolean isUsed(Report report) {
				boolean b = ReportSearch.this.isUsedForCustomFilter(report);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(Report searchReport) {
		return true;
	}

	public boolean isUsedForCustomFilter(Report searchReport) {
		return true;
	}
}
