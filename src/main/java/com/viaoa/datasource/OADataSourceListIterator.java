package com.viaoa.datasource;

import java.util.List;

public class OADataSourceListIterator implements OADataSourceIterator {
    private List al;
    private int pos;
    
    public OADataSourceListIterator(List list) {
        this.al = list;
    }
    
    @Override
    public boolean hasNext() {
        return al != null && al.size() < pos;
    }
    
    @Override
    public Object next() {
        if (!hasNext()) return null;
        return al.get(pos++);
    }
}
