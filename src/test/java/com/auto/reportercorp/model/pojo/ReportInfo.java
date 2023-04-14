package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.*;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "reportId", scope = ReportInfo.class)
public class ReportInfo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile int receivedCount;
    protected volatile int processedCount;
    protected volatile int fixedJsonCount;
    protected volatile int processingErrorCount;

    // References to other objects
    // Report
    protected volatile Report report;
    protected volatile Integer reportId; // fkey: report.id
    // ReportInstanceProcessorInfo
    protected volatile ReportInstanceProcessorInfo reportInstanceProcessorInfo;
    // ReportInstanceProcesses
    protected volatile CopyOnWriteArrayList<ReportInstanceProcess> alReportInstanceProcesses = new CopyOnWriteArrayList<>();
     
    public ReportInfo() {
        this.created = LocalDateTime.now();
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public int getReceivedCount() {
        return receivedCount;
    }
    public void setReceivedCount(int newValue) {
        this.receivedCount = newValue;
    }
    public int incrReceivedCount() {
        return ++this.receivedCount;
    }
     
    public int getProcessedCount() {
        return processedCount;
    }
    public void setProcessedCount(int newValue) {
        this.processedCount = newValue;
    }
    public int incrProcessedCount() {
        return ++this.processedCount;
    }
     
    public int getFixedJsonCount() {
        return fixedJsonCount;
    }
    public void setFixedJsonCount(int newValue) {
        this.fixedJsonCount = newValue;
    }
    public int incrFixedJsonCount() {
        return ++fixedJsonCount;
    }
     
    public int getProcessingErrorCount() {
        return processingErrorCount;
    }
    public void setProcessingErrorCount(int newValue) {
        this.processingErrorCount = newValue;
    }
    public int incrProcessingErrorCount() {
        return ++this.processingErrorCount;
    }

    @JsonIgnore
    public Report getReport() {
        return report;
    }
    public void setReport(Report newValue) {
        this.report = newValue;
    }
    public Integer getReportId() {
        Report report = this.getReport();
        if (report != null) {
            return report.getId();
        }
        return this.reportId;
    }
    public void setReportId(Integer newValue) {
        this.reportId = newValue;
        if (newValue == null) setReport(null);
        else {
            if (this.report != null) {
                if (report.getId() != newValue.intValue()) {
                    this.setReport(null);
                }
            }
        }
    }

    @JsonIgnore
    public ReportInstanceProcessorInfo getReportInstanceProcessorInfo() {
        return reportInstanceProcessorInfo;
    }
    public void setReportInstanceProcessorInfo(ReportInstanceProcessorInfo newValue) {
        this.reportInstanceProcessorInfo = newValue;
    }

    @JsonIdentityReference
    public CopyOnWriteArrayList<ReportInstanceProcess> getReportInstanceProcesses() {
        return alReportInstanceProcesses;
    }
    public void setReportInstanceProcesses(List<ReportInstanceProcess> list) {
        if (list == null) this.alReportInstanceProcesses.clear();
        else this.alReportInstanceProcesses = new CopyOnWriteArrayList<>(list);
    }
     
    @Override
    public String toString() {
        return "ReportInfo [" +
            "created=" + created +
            ", receivedCount=" + receivedCount +
            ", processedCount=" + processedCount +
            ", fixedJsonCount=" + fixedJsonCount +
            ", processingErrorCount=" + processingErrorCount +
            ", reportId=" + reportId +
        "]";
    }
}
 
