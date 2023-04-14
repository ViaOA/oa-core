package com.auto.reportercorp.model.pojo;
 
import java.util.*;

import com.auto.reportercorp.util.EmbeddedJsonStringDeserializer;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;

import java.time.*;
import java.time.LocalDate;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = PypeReportMessage.class)
public class PypeReportMessage implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile long id;
    protected volatile LocalDateTime created;
    protected volatile int store;
    protected volatile LocalDate processingDate;
    protected volatile String title;
    protected volatile String template;
    protected volatile String filename;
    protected volatile String data;
    protected volatile String subreports;
    protected volatile LocalDateTime convertedDate;
    protected volatile String status;

    // References to other objects
    // ReportInstance
    protected volatile ReportInstance reportInstance;
    protected volatile Long reportInstanceId; // fkey: reportInstance.id
     
    public PypeReportMessage() {
        this.created = LocalDateTime.now();
    }

    public PypeReportMessage(long id) {
        this();
        setId(id);
    }
     
    public long getId() {
        return id;
    }
    public void setId(long newValue) {
        this.id = newValue;
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public int getStore() {
        return store;
    }
    public void setStore(int newValue) {
        this.store = newValue;
    }
     
    public LocalDate getProcessingDate() {
        return processingDate;
    }
    public void setProcessingDate(LocalDate newValue) {
        this.processingDate = newValue;
    }
     
    public String getTitle() {
        return title;
    }
    public void setTitle(String newValue) {
        this.title = newValue;
    }
     
    public String getTemplate() {
        return template;
    }
    public void setTemplate(String newValue) {
        this.template = newValue;
    }
     
    public String getFilename() {
        return filename;
    }
    public void setFilename(String newValue) {
        this.filename = newValue;
    }
     
    @JsonRawValue
    @JsonDeserialize(using = EmbeddedJsonStringDeserializer.class)
    public String getData() {
        return data;
    }
    public void setData(String newValue) {
        this.data = newValue;
    }
     
    @JsonRawValue
    @JsonDeserialize(using = EmbeddedJsonStringDeserializer.class)
    public String getSubreports() {
        return subreports;
    }
    public void setSubreports(String newValue) {
        this.subreports = newValue;
    }
     
    public LocalDateTime getConvertedDate() {
        return convertedDate;
    }
    public void setConvertedDate(LocalDateTime newValue) {
        this.convertedDate = newValue;
    }
     
    public String getStatus() {
        return status;
    }
    public void setStatus(String newValue) {
        this.status = newValue;
    }

    @JsonIgnore
    public ReportInstance getReportInstance() {
        return reportInstance;
    }
    public void setReportInstance(ReportInstance newValue) {
        this.reportInstance = newValue;
    }
    public Long getReportInstanceId() {
        ReportInstance reportInstance = this.getReportInstance();
        if (reportInstance != null) {
            return reportInstance.getId();
        }
        return this.reportInstanceId;
    }
    public void setReportInstanceId(Long newValue) {
        this.reportInstanceId = newValue;
        if (newValue == null) setReportInstance(null);
        else {
            if (this.reportInstance != null) {
                if (reportInstance.getId() != newValue.longValue()) {
                    this.setReportInstance(null);
                }
            }
        }
    }
     
    @Override
    public String toString() {
        return "PypeReportMessage [" +
            "id=" + id +
            ", created=" + created +
            ", store=" + store +
            ", processingDate=" + processingDate +
            ", title=" + title +
            ", template=" + template +
            ", filename=" + filename +
            ", convertedDate=" + convertedDate +
            ", status=" + status +
            ", reportInstanceId=" + reportInstanceId +
        "]";
    }
}
 
