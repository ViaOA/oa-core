package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.PypeReportMessage;
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
public class PypeReportMessageSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(PypeReportMessageSearch.class.getName());

	public static final String P_Store = "Store";
	public static final String P_Store2 = "Store2";
	public static final String P_Filename = "Filename";
	public static final String P_Created = "Created";
	public static final String P_Created2 = "Created2";
	public static final String P_MaxResults = "MaxResults";

	protected int store;
	protected int store2;
	protected String filename;
	protected OADateTime created;
	protected OADateTime created2;
	protected int maxResults;

	public int getStore() {
		return store;
	}

	public void setStore(int newValue) {
		int old = store;
		fireBeforePropertyChange(P_Store, old, newValue);
		this.store = newValue;
		firePropertyChange(P_Store, old, this.store);
		if (isLoading()) {
			return;
		}
		if (store > store2) {
			setStore2(this.store);
		}
	}

	public int getStore2() {
		return store2;
	}

	public void setStore2(int newValue) {
		int old = store2;
		fireBeforePropertyChange(P_Store2, old, newValue);
		this.store2 = newValue;
		firePropertyChange(P_Store2, old, this.store2);
		if (isLoading()) {
			return;
		}
		if (store > store2) {
			setStore(this.store2);
		}
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String newValue) {
		String old = filename;
		fireBeforePropertyChange(P_Filename, old, newValue);
		this.filename = newValue;
		firePropertyChange(P_Filename, old, this.filename);
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
		setStore(0);
		setNull(P_Store);
		setStore2(0);
		setNull(P_Store2);
		setFilename(null);
		setCreated(null);
		setCreated2(null);
	}

	public boolean isDataEntered() {
		if (!isNull(P_Store)) {
			return true;
		}
		if (getFilename() != null) {
			return true;
		}
		if (getCreated() != null) {
			return true;
		}
		return false;
	}

	protected String extraWhere;
	protected Object[] extraWhereParams;
	protected OAFilter<PypeReportMessage> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<PypeReportMessage> f = new OAQueryFilter<PypeReportMessage>(PypeReportMessage.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<PypeReportMessage> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<PypeReportMessage> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<PypeReportMessage> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];
		if (!isNull(P_Store)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Store2) && store != store2) {
				sql += PypeReportMessage.P_Store + " >= ?";
				args = OAArray.add(Object.class, args, getStore());
				sql += " AND " + PypeReportMessage.P_Store + " <= ?";
				args = OAArray.add(Object.class, args, getStore2());
			} else {
				sql += PypeReportMessage.P_Store + " = ?";
				args = OAArray.add(Object.class, args, getStore());
			}
		}
		if (OAString.isNotEmpty(this.filename)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(filename);
			if (val.indexOf("%") >= 0) {
				sql += PypeReportMessage.P_Filename + " LIKE ?";
			} else {
				sql += PypeReportMessage.P_Filename + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += PypeReportMessage.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + PypeReportMessage.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += PypeReportMessage.P_Created + " = ?";
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

		OASelect<PypeReportMessage> select = new OASelect<PypeReportMessage>(PypeReportMessage.class, sql, args, sortOrder);
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
		if (!isNull(P_Store)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Store2) && store != store2) {
				sql += prefix + PypeReportMessage.P_Store + " >= ?";
				args = OAArray.add(Object.class, args, getStore());
				sql += " AND " + prefix + PypeReportMessage.P_Store + " <= ?";
				args = OAArray.add(Object.class, args, getStore2());
			} else {
				sql += prefix + PypeReportMessage.P_Store + " = ?";
				args = OAArray.add(Object.class, args, getStore());
			}
		}
		if (OAString.isNotEmpty(this.filename)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			String val = OAString.convertToLikeSearch(filename);
			if (val.indexOf("%") >= 0) {
				sql += prefix + PypeReportMessage.P_Filename + " LIKE ?";
			} else {
				sql += prefix + PypeReportMessage.P_Filename + " = ?";
			}
			args = OAArray.add(Object.class, args, val);
		}
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += prefix + PypeReportMessage.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + prefix + PypeReportMessage.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += prefix + PypeReportMessage.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		select.add(sql, args);
	}

	private OAFilter<PypeReportMessage> filterDataSourceFilter;

	public OAFilter<PypeReportMessage> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<PypeReportMessage>() {
			@Override
			public boolean isUsed(PypeReportMessage pypeReportMessage) {
				return PypeReportMessageSearch.this.isUsedForDataSourceFilter(pypeReportMessage);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<PypeReportMessage> filterCustomFilter;

	public OAFilter<PypeReportMessage> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<PypeReportMessage>() {
			@Override
			public boolean isUsed(PypeReportMessage pypeReportMessage) {
				boolean b = PypeReportMessageSearch.this.isUsedForCustomFilter(pypeReportMessage);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(PypeReportMessage searchPypeReportMessage) {
		return true;
	}

	public boolean isUsedForCustomFilter(PypeReportMessage searchPypeReportMessage) {
		return true;
	}
}
