// generated by OABuilder
package com.corptostore.model.pojo;
 
import java.util.*;

import com.corptostore.model.pojo.CorpToStore;

import java.time.LocalDateTime;
import java.awt.Color;
 
 
public class Environment implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected LocalDateTime created;
    protected Type type;
    protected String name;
    protected String urlTemplate;
    protected int nodeCount;
    protected Color color;
    protected boolean enableDashboard;
     
    public static enum Type {
        Unknown,Local,Dev,Test,Prod;
    }

    // References to other objects.
    // CorpToStores
    protected List<CorpToStore> alCorpToStores;
     
    public Environment() {
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public Type getType() {
        return type;
    }
    public void setType(Type newType) {
        this.type = newType;
    }
     
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        this.name = newValue;
    }
     
    public String getUrlTemplate() {
        return urlTemplate;
    }
    public void setUrlTemplate(String newValue) {
        this.urlTemplate = newValue;
    }
     
    public int getNodeCount() {
        return nodeCount;
    }
    public void setNodeCount(int newValue) {
        this.nodeCount = newValue;
    }
     
    public Color getColor() {
        return color;
    }
    public void setColor(Color newValue) {
        this.color = newValue;
    }
     
    public boolean getEnableDashboard() {
        return enableDashboard;
    }
    public void setEnableDashboard(boolean newValue) {
        this.enableDashboard = newValue;
    }
     
    public List<CorpToStore> getCorpToStores() {
        if (alCorpToStores == null) {
            alCorpToStores = new ArrayList<CorpToStore>();
        }
        return alCorpToStores;
    }
    public void setCorpToStores(List<CorpToStore> list) {
        this.alCorpToStores = list;
    }
}
 
