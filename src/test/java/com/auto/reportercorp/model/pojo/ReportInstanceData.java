package com.auto.reportercorp.model.pojo;
 
import java.util.*;

import com.auto.reportercorp.util.EmbeddedJsonStringDeserializer;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;

import java.time.*;
import java.time.LocalDate;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = ReportInstanceData.class)
public class ReportInstanceData implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile long id;
    protected volatile LocalDateTime created;
    protected volatile String type;
    protected volatile boolean printNow;
    protected volatile String printerId;
    protected volatile String reportData;
    protected volatile String reportName;
    protected volatile int storeNumber;
    protected volatile LocalDate businessDate;
    protected volatile LocalDate processingDate;
    protected volatile String additionalParameters;
     
    public ReportInstanceData() {
        this.created = LocalDateTime.now();
    }

    public ReportInstanceData(long id) {
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
     
    public String getType() {
        return type;
    }
    public void setType(String newValue) {
        this.type = newValue;
    }
     
    public boolean getPrintNow() {
        return printNow;
    }
    public void setPrintNow(boolean newValue) {
        this.printNow = newValue;
    }
     
    public String getPrinterId() {
        return printerId;
    }
    public void setPrinterId(String newValue) {
        this.printerId = newValue;
    }
     
    @JsonRawValue
    @JsonDeserialize(using = EmbeddedJsonStringDeserializer.class)
    public String getReportData() {
        return reportData;
    }
    public void setReportData(String newValue) {
        this.reportData = newValue;
    }
     
    public String getReportName() {
        return reportName;
    }
    public void setReportName(String newValue) {
        this.reportName = newValue;
    }
     
    public int getStoreNumber() {
        return storeNumber;
    }
    public void setStoreNumber(int newValue) {
        this.storeNumber = newValue;
    }
     
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    public LocalDate getBusinessDate() {
        return businessDate;
    }
    public void setBusinessDate(LocalDate newValue) {
        this.businessDate = newValue;
    }
     
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    public LocalDate getProcessingDate() {
        return processingDate;
    }
    public void setProcessingDate(LocalDate newValue) {
        this.processingDate = newValue;
    }
     
    @JsonRawValue
    @JsonDeserialize(using = EmbeddedJsonStringDeserializer.class)
    public String getAdditionalParameters() {
        return additionalParameters;
    }
    public void setAdditionalParameters(String newValue) {
        this.additionalParameters = newValue;
    }
     
    @Override
    public String toString() {
        return "ReportInstanceData [" +
            "id=" + id +
            ", created=" + created +
            ", type=" + type +
            ", printNow=" + printNow +
            ", printerId=" + printerId +
            ", reportName=" + reportName +
            ", storeNumber=" + storeNumber +
            ", businessDate=" + businessDate +
            ", processingDate=" + processingDate +
        "]";
    }
}
 
