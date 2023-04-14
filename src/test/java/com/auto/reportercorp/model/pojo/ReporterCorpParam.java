package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.time.*;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "name", scope = ReporterCorpParam.class)
public class ReporterCorpParam implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile Type type;
     
    public static enum Type {
        unknown;
    }
    protected volatile String name;
    protected volatile String value;

    // References to other objects
    // ReporterCorp
    protected volatile ReporterCorp reporterCorp;
     
    public ReporterCorpParam() {
        this.created = LocalDateTime.now();
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
     
    public String getValue() {
        return value;
    }
    public void setValue(String newValue) {
        this.value = newValue;
    }

    @JsonIgnore
    public ReporterCorp getReporterCorp() {
        return reporterCorp;
    }
    public void setReporterCorp(ReporterCorp newValue) {
        this.reporterCorp = newValue;
    }
     
    @Override
    public String toString() {
        return "ReporterCorpParam [" +
            "created=" + created +
            ", type=" + type +
            ", name=" + name +
            ", value=" + value +
        "]";
    }
}
 
