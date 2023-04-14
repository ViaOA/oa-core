package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.*;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "counter", scope = ReportInstanceProcess.class)
public class ReportInstanceProcess implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile int counter;
    protected volatile LocalDateTime completed;
    protected volatile boolean fixedJson;
    protected volatile boolean pypeError;
    protected volatile boolean processingError;

    // References to other objects
    // PypeReportMessage
    protected volatile PypeReportMessage pypeReportMessage;
    protected volatile Long pypeReportMessageId; // fkey: pypeReportMessage.id
    // ReportInfo
    protected volatile ReportInfo reportInfo;
    protected volatile Integer reportId; // selectFrom=reportInstanceProcessorInfo.reportInfos, uniqueLink=report, fkey: reportInfo.report.id
    // ReportInstanceProcessorInfo
    protected volatile ReportInstanceProcessorInfo reportInstanceProcessorInfo;
    // StatusInfo
    protected volatile StatusInfo statusInfo;
    // StoreInfo
    protected volatile StoreInfo storeInfo;
    protected volatile Integer storeInfoStoreNumber; // selectFrom=reportInstanceProcessorInfo.reporterCorp.storeInfos, uniqueLink=storeInfo, link w/unique property: storeInfo.storeNumber
    // ThreadInfo
    protected volatile ThreadInfo threadInfo;
    protected volatile String threadInfoName; // selectFrom=reportInstanceProcessorInfo.reporterCorp.threadInfos, uniqueLink=threadInfo, link w/unique property: threadInfo.name
    // ReportInstanceProcessSteps
    protected volatile CopyOnWriteArrayList<ReportInstanceProcessStep> alReportInstanceProcessSteps = new CopyOnWriteArrayList<>();
     
    public ReportInstanceProcess() {
        this.created = LocalDateTime.now();
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public int getCounter() {
        return counter;
    }
    public void setCounter(int newValue) {
        this.counter = newValue;
    }
     
    public LocalDateTime getCompleted() {
        return completed;
    }
    public void setCompleted(LocalDateTime newValue) {
        this.completed = newValue;
    }
     
    public boolean getFixedJson() {
        return fixedJson;
    }
    public void setFixedJson(boolean newValue) {
        this.fixedJson = newValue;
    }
     
    public boolean getPypeError() {
        return pypeError;
    }
    public void setPypeError(boolean newValue) {
        this.pypeError = newValue;
    }
     
    public boolean getProcessingError() {
        return processingError;
    }
    public void setProcessingError(boolean newValue) {
        this.processingError = newValue;
    }

    @JsonIgnore
    public PypeReportMessage getPypeReportMessage() {
        return pypeReportMessage;
    }
    public void setPypeReportMessage(PypeReportMessage newValue) {
        this.pypeReportMessage = newValue;
    }
    public Long getPypeReportMessageId() {
        PypeReportMessage pypeReportMessage = this.getPypeReportMessage();
        if (pypeReportMessage != null) {
            return pypeReportMessage.getId();
        }
        return this.pypeReportMessageId;
    }
    public void setPypeReportMessageId(Long newValue) {
        this.pypeReportMessageId = newValue;
        if (newValue == null) setPypeReportMessage(null);
        else {
            if (this.pypeReportMessage != null) {
                if (pypeReportMessage.getId() != newValue.longValue()) {
                    this.setPypeReportMessage(null);
                }
            }
        }
    }

    @JsonIgnore
    public ReportInfo getReportInfo() {
        return reportInfo;
    }
    public void setReportInfo(ReportInfo newValue) {
        this.reportInfo = newValue;
    }
    public Integer getReportId() {
        ReportInfo reportInfo = this.getReportInfo();
        if (reportInfo != null) {
            Report report = reportInfo.getReport();
            if (report != null) {
                return report.getId();
            }
        }
        return this.reportId;
    }
    public void setReportId(Integer newValue) {
        this.reportId = newValue;
        if (newValue == null) setReportInfo(null);
        else {
            if (this.reportInfo != null) {
                Report report = reportInfo.getReport();
                if (report == null) setReportInfo(null);
                else {
                    if (report.getId() != newValue.intValue()) {
                        this.setReportInfo(null);
                    }
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

    public StatusInfo getStatusInfo() {
        if (statusInfo == null) statusInfo = new StatusInfo();
        return statusInfo;
    }
    public void setStatusInfo(StatusInfo newValue) {
        this.statusInfo = newValue;
    }

    @JsonIgnore
    public StoreInfo getStoreInfo() {
        return storeInfo;
    }
    public void setStoreInfo(StoreInfo newValue) {
        this.storeInfo = newValue;
    }
    public Integer getStoreInfoStoreNumber() {
        StoreInfo storeInfo = this.getStoreInfo();
        if (storeInfo != null) {
            return storeInfo.getStoreNumber();
        }
        return this.storeInfoStoreNumber;
    }
    public void setStoreInfoStoreNumber(Integer newValue) {
        this.storeInfoStoreNumber = newValue;
        if (newValue == null) setStoreInfo(null);
        else {
            if (this.storeInfo != null) {
                if (storeInfo.getStoreNumber() != newValue.intValue()) {
                    this.setStoreInfo(null);
                }
            }
        }
    }

    @JsonIgnore
    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }
    public void setThreadInfo(ThreadInfo newValue) {
        this.threadInfo = newValue;
    }
    public String getThreadInfoName() {
        ThreadInfo threadInfo = this.getThreadInfo();
        if (threadInfo != null) {
            return threadInfo.getName();
        }
        return this.threadInfoName;
    }
    public void setThreadInfoName(String newValue) {
        this.threadInfoName = newValue;
        if (newValue == null) setThreadInfo(null);
        else {
            if (this.threadInfo != null) {
                if (!newValue.equals(this.threadInfo.getName())) {
                    this.setThreadInfo(null);
                }
            }
        }
    }

    public CopyOnWriteArrayList<ReportInstanceProcessStep> getReportInstanceProcessSteps() {
        return alReportInstanceProcessSteps;
    }
    public void setReportInstanceProcessSteps(List<ReportInstanceProcessStep> list) {
        if (list == null) this.alReportInstanceProcessSteps.clear();
        else this.alReportInstanceProcessSteps = new CopyOnWriteArrayList<>(list);
    }
     
    @Override
    public String toString() {
        return "ReportInstanceProcess [" +
            "created=" + created +
            ", counter=" + counter +
            ", completed=" + completed +
            ", fixedJson=" + fixedJson +
            ", pypeError=" + pypeError +
            ", processingError=" + processingError +
            ", pypeReportMessageId=" + pypeReportMessageId +
            ", reportId=" + reportId +
            ", storeInfoStoreNumber=" + storeInfoStoreNumber +
            ", threadInfoName=" + threadInfoName +
        "]";
    }
}
 
