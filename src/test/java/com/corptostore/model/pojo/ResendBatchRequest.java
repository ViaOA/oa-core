// generated by OABuilder
package com.corptostore.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
 
public class ResendBatchRequest implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected LocalDateTime created;
    protected LocalDateTime session;
    protected int beginSeq;
    protected int endSeq;
     
    public ResendBatchRequest() {
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public LocalDateTime getSession() {
        return session;
    }
    public void setSession(LocalDateTime newValue) {
        this.session = newValue;
    }
     
    public int getBeginSeq() {
        return beginSeq;
    }
    public void setBeginSeq(int newValue) {
        this.beginSeq = newValue;
    }
     
    public int getEndSeq() {
        return endSeq;
    }
    public void setEndSeq(int newValue) {
        this.endSeq = newValue;
    }
}
 