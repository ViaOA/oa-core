// Generated by OABuilder
package com.cdi.model.oa.search;

import java.util.logging.*;
import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.datasource.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.filter.OAQueryFilter;
import com.cdi.delegate.ModelDelegate;

@OAClass(useDataSource=false, localOnly=true)
public class AppUserSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(AppUserSearch.class.getName());
    public static final String P_Id = "Id";
    public static final String P_Id2 = "Id2";
    public static final String P_LoginId = "LoginId";
    public static final String P_LoginIdUseNull = "LoginIdUseNull";
    public static final String P_LoginIdUseNotNull = "LoginIdUseNotNull";
    public static final String P_FirstName = "FirstName";
    public static final String P_FirstNameUseNull = "FirstNameUseNull";
    public static final String P_FirstNameUseNotNull = "FirstNameUseNotNull";
    public static final String P_LastName = "LastName";
    public static final String P_LastNameUseNull = "LastNameUseNull";
    public static final String P_LastNameUseNotNull = "LastNameUseNotNull";
    public static final String P_MaxResults = "MaxResults";

    protected int id;
    protected int id2;
    protected String loginId;
    protected boolean loginIdUseNull;
    protected boolean loginIdUseNotNull;
    protected String firstName;
    protected boolean firstNameUseNull;
    protected boolean firstNameUseNotNull;
    protected String lastName;
    protected boolean lastNameUseNull;
    protected boolean lastNameUseNotNull;
    protected int maxResults;

    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
        if (isLoading()) return;
        if (id > id2) setId2(this.id);
    } 
    public int getId2() {
        return id2;
    }
    public void setId2(int newValue) {
        int old = id2;
        fireBeforePropertyChange(P_Id2, old, newValue);
        this.id2 = newValue;
        firePropertyChange(P_Id2, old, this.id2);
        if (isLoading()) return;
        if (id > id2) setId(this.id2);
    }

    public String getLoginId() {
        return loginId;
    }
    public void setLoginId(String newValue) {
        String old = loginId;
        fireBeforePropertyChange(P_LoginId, old, newValue);
        this.loginId = newValue;
        firePropertyChange(P_LoginId, old, this.loginId);
    }
      
    public boolean getLoginIdUseNull() {
        return loginIdUseNull;
    }
    public void setLoginIdUseNull(boolean newValue) {
        boolean old = this.loginIdUseNull;
        this.loginIdUseNull = newValue;
        firePropertyChange(P_LoginIdUseNull, old, this.loginIdUseNull);
    }
    public boolean getLoginIdUseNotNull() {
        return loginIdUseNotNull;
    }
    public void setLoginIdUseNotNull(boolean newValue) {
        boolean old = this.loginIdUseNotNull;
        this.loginIdUseNotNull = newValue;
        firePropertyChange(P_LoginIdUseNotNull, old, this.loginIdUseNotNull);
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String newValue) {
        String old = firstName;
        fireBeforePropertyChange(P_FirstName, old, newValue);
        this.firstName = newValue;
        firePropertyChange(P_FirstName, old, this.firstName);
    }
      
    public boolean getFirstNameUseNull() {
        return firstNameUseNull;
    }
    public void setFirstNameUseNull(boolean newValue) {
        boolean old = this.firstNameUseNull;
        this.firstNameUseNull = newValue;
        firePropertyChange(P_FirstNameUseNull, old, this.firstNameUseNull);
    }
    public boolean getFirstNameUseNotNull() {
        return firstNameUseNotNull;
    }
    public void setFirstNameUseNotNull(boolean newValue) {
        boolean old = this.firstNameUseNotNull;
        this.firstNameUseNotNull = newValue;
        firePropertyChange(P_FirstNameUseNotNull, old, this.firstNameUseNotNull);
    }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String newValue) {
        String old = lastName;
        fireBeforePropertyChange(P_LastName, old, newValue);
        this.lastName = newValue;
        firePropertyChange(P_LastName, old, this.lastName);
    }
      
    public boolean getLastNameUseNull() {
        return lastNameUseNull;
    }
    public void setLastNameUseNull(boolean newValue) {
        boolean old = this.lastNameUseNull;
        this.lastNameUseNull = newValue;
        firePropertyChange(P_LastNameUseNull, old, this.lastNameUseNull);
    }
    public boolean getLastNameUseNotNull() {
        return lastNameUseNotNull;
    }
    public void setLastNameUseNotNull(boolean newValue) {
        boolean old = this.lastNameUseNotNull;
        this.lastNameUseNotNull = newValue;
        firePropertyChange(P_LastNameUseNotNull, old, this.lastNameUseNotNull);
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
        setId(0);
        setNull(P_Id);
        setId2(0);
        setNull(P_Id2);
        setLoginId(null);
        setLoginIdUseNull(false);
        setLoginIdUseNotNull(false);
        setFirstName(null);
        setFirstNameUseNull(false);
        setFirstNameUseNotNull(false);
        setLastName(null);
        setLastNameUseNull(false);
        setLastNameUseNotNull(false);
    }

    public boolean isDataEntered() {

        if (getLoginId() != null) return true;
        if (getLoginIdUseNull()) return true;if (getLoginIdUseNotNull()) return true;
        if (getLoginIdUseNull()) return true;
        if (getLoginIdUseNotNull()) return true;
        if (getFirstName() != null) return true;
        if (getFirstNameUseNull()) return true;if (getFirstNameUseNotNull()) return true;
        if (getFirstNameUseNull()) return true;
        if (getFirstNameUseNotNull()) return true;
        if (getLastName() != null) return true;
        if (getLastNameUseNull()) return true;if (getLastNameUseNotNull()) return true;
        if (getLastNameUseNull()) return true;
        if (getLastNameUseNotNull()) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<AppUser> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (!OAString.isEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<AppUser> f = new OAQueryFilter<AppUser>(AppUser.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<AppUser> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<AppUser> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<AppUser> getSelect() {
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (!isNull(P_Id)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Id2) && id != id2) {
                sql += AppUser.P_Id + " >= ?";
                args = OAArray.add(Object.class, args, getId());
                sql += " AND " + AppUser.P_Id + " <= ?";
                args = OAArray.add(Object.class, args, getId2());
            }
            else {
                sql += AppUser.P_Id + " = ?";
                args = OAArray.add(Object.class, args, getId());
            }
        }
        if (loginIdUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + AppUser.P_LoginId + " = null OR " + AppUser.P_LoginId + " == '')";
        }
        else if (loginIdUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += AppUser.P_LoginId + " != null";
        }
        else if (!OAString.isEmpty(this.loginId)) {
            if (sql.length() > 0) sql += " AND ";
            String value = this.loginId.replace("*", "%");
            if (!value.endsWith("%")) value += "%";
            if (value.indexOf("%") >= 0) {
                sql += AppUser.P_LoginId + " LIKE ?";
            }
            else {
                sql += AppUser.P_LoginId + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (firstNameUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + AppUser.P_FirstName + " = null OR " + AppUser.P_FirstName + " == '')";
        }
        else if (firstNameUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += AppUser.P_FirstName + " != null";
        }
        else if (!OAString.isEmpty(this.firstName)) {
            if (sql.length() > 0) sql += " AND ";
            String value = this.firstName.replace("*", "%");
            if (!value.endsWith("%")) value += "%";
            if (value.indexOf("%") >= 0) {
                sql += AppUser.P_FirstName + " LIKE ?";
            }
            else {
                sql += AppUser.P_FirstName + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (lastNameUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + AppUser.P_LastName + " = null OR " + AppUser.P_LastName + " == '')";
        }
        else if (lastNameUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += AppUser.P_LastName + " != null";
        }
        else if (!OAString.isEmpty(this.lastName)) {
            if (sql.length() > 0) sql += " AND ";
            String value = this.lastName.replace("*", "%");
            if (!value.endsWith("%")) value += "%";
            if (value.indexOf("%") >= 0) {
                sql += AppUser.P_LastName + " LIKE ?";
            }
            else {
                sql += AppUser.P_LastName + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }

        if (!OAString.isEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<AppUser> select = new OASelect<AppUser>(AppUser.class, sql, args, sortOrder);
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFilter(this.getCustomFilter());
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (!isNull(P_Id)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Id2) && id != id2) {
                sql += AppUser.P_Id + " >= ?";
                args = OAArray.add(Object.class, args, getId());
                sql += " AND " + AppUser.P_Id + " <= ?";
                args = OAArray.add(Object.class, args, getId2());
            }
            else {
                sql += AppUser.P_Id + " = ?";
                args = OAArray.add(Object.class, args, getId());
            }
        }
        if (loginIdUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + AppUser.P_LoginId + " = null OR " + AppUser.P_LoginId + " == '')";
        }
        else if (loginIdUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += AppUser.P_LoginId + " != null";
        }
        else if (!OAString.isEmpty(this.loginId)) {
            if (sql.length() > 0) sql += " AND ";
            String value = this.loginId.replace("*", "%");
            if (!value.endsWith("%")) value += "%";
            if (value.indexOf("%") >= 0) {
                sql += AppUser.P_LoginId + " LIKE ?";
            }
            else {
                sql += AppUser.P_LoginId + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (firstNameUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + AppUser.P_FirstName + " = null OR " + AppUser.P_FirstName + " == '')";
        }
        else if (firstNameUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += AppUser.P_FirstName + " != null";
        }
        else if (!OAString.isEmpty(this.firstName)) {
            if (sql.length() > 0) sql += " AND ";
            String value = this.firstName.replace("*", "%");
            if (!value.endsWith("%")) value += "%";
            if (value.indexOf("%") >= 0) {
                sql += AppUser.P_FirstName + " LIKE ?";
            }
            else {
                sql += AppUser.P_FirstName + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (lastNameUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += "(" + AppUser.P_LastName + " = null OR " + AppUser.P_LastName + " == '')";
        }
        else if (lastNameUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += AppUser.P_LastName + " != null";
        }
        else if (!OAString.isEmpty(this.lastName)) {
            if (sql.length() > 0) sql += " AND ";
            String value = this.lastName.replace("*", "%");
            if (!value.endsWith("%")) value += "%";
            if (value.indexOf("%") >= 0) {
                sql += AppUser.P_LastName + " LIKE ?";
            }
            else {
                sql += AppUser.P_LastName + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        select.add(sql, args);
    }

    private OAFilter<AppUser> filterDataSourceFilter;
    public OAFilter<AppUser> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<AppUser>() {
            @Override
            public boolean isUsed(AppUser appUser) {
                return AppUserSearch.this.isUsedForDataSourceFilter(appUser);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<AppUser> filterCustomFilter;
    public OAFilter<AppUser> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<AppUser>() {
            @Override
            public boolean isUsed(AppUser appUser) {
                boolean b = AppUserSearch.this.isUsedForCustomFilter(appUser);
                if (b && filterExtraWhere != null) b = filterExtraWhere.isUsed(appUser);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(AppUser searchAppUser) {
        if (!isNull(P_Id2)) {
            if (!OACompare.isEqualOrBetween(searchAppUser.getId(), id, id2)) return false;
        }
        if (loginIdUseNull) {
            if (OACompare.isNotEmpty(searchAppUser.getLoginId())) return false;
        }
        else if (loginIdUseNotNull) {
            if (OACompare.isEmpty(searchAppUser.getLoginId())) return false;
        }
        else if (loginId != null) {
            String s = getLoginId();
            if (s != null && s.indexOf('*') < 0 && s.indexOf('%') < 0) s += '*';
            if (!OACompare.isLike(searchAppUser.getLoginId(), s)) return false;
        }
        if (firstNameUseNull) {
            if (OACompare.isNotEmpty(searchAppUser.getFirstName())) return false;
        }
        else if (firstNameUseNotNull) {
            if (OACompare.isEmpty(searchAppUser.getFirstName())) return false;
        }
        else if (firstName != null) {
            String s = getFirstName();
            if (s != null && s.indexOf('*') < 0 && s.indexOf('%') < 0) s += '*';
            if (!OACompare.isLike(searchAppUser.getFirstName(), s)) return false;
        }
        if (lastNameUseNull) {
            if (OACompare.isNotEmpty(searchAppUser.getLastName())) return false;
        }
        else if (lastNameUseNotNull) {
            if (OACompare.isEmpty(searchAppUser.getLastName())) return false;
        }
        else if (lastName != null) {
            String s = getLastName();
            if (s != null && s.indexOf('*') < 0 && s.indexOf('%') < 0) s += '*';
            if (!OACompare.isLike(searchAppUser.getLastName(), s)) return false;
        }
        return true;
    }
    public boolean isUsedForCustomFilter(AppUser searchAppUser) {
        return true;
    }
}
