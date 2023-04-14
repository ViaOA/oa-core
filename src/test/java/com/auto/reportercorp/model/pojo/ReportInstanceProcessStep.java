package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.time.*;
 
public class ReportInstanceProcessStep implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile boolean used;
    protected volatile String result;
    protected volatile boolean success;

    // References to other objects
    // ProcessStep
    protected volatile ProcessStep processStep;
    protected volatile ProcessStep.Step processStepStep; // importMatch: processStep.step
    // ReportInstanceProcess
    protected volatile ReportInstanceProcess reportInstanceProcess;
    protected volatile Integer reportInstanceProcessCounter; // link w/unique property: reportInstanceProcess.counter
     
    public ReportInstanceProcessStep() {
        this.created = LocalDateTime.now();
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public boolean getUsed() {
        return used;
    }
    public void setUsed(boolean newValue) {
        this.used = newValue;
    }
     
    public String getResult() {
        return result;
    }
    public void setResult(String newValue) {
        this.result = newValue;
    }
     
    public boolean getSuccess() {
        return success;
    }
    public void setSuccess(boolean newValue) {
        this.success = newValue;
    }

    @JsonIgnore
    public ProcessStep getProcessStep() {
        return processStep;
    }
    public void setProcessStep(ProcessStep newValue) {
        this.processStep = newValue;
    }
    public ProcessStep.Step getProcessStepStep() {
        ProcessStep processStep = this.getProcessStep();
        if (processStep != null) {
            return processStep.getStep();
        }
        return this.processStepStep;
    }
    public void setProcessStepStep(ProcessStep.Step newValue) {
        this.processStepStep = newValue;
        if (newValue == null) setProcessStep(null);
        else {
            if (this.processStep != null) {
                if (processStep.getStep() != newValue) {
                    this.setProcessStep(null);
                }
            }
        }
    }

    @JsonIgnore
    public ReportInstanceProcess getReportInstanceProcess() {
        return reportInstanceProcess;
    }
    public void setReportInstanceProcess(ReportInstanceProcess newValue) {
        this.reportInstanceProcess = newValue;
    }
    public Integer getReportInstanceProcessCounter() {
        ReportInstanceProcess reportInstanceProcess = this.getReportInstanceProcess();
        if (reportInstanceProcess != null) {
            return reportInstanceProcess.getCounter();
        }
        return this.reportInstanceProcessCounter;
    }
    public void setReportInstanceProcessCounter(Integer newValue) {
        this.reportInstanceProcessCounter = newValue;
        if (newValue == null) setReportInstanceProcess(null);
        else {
            if (this.reportInstanceProcess != null) {
                if (reportInstanceProcess.getCounter() != newValue.intValue()) {
                    this.setReportInstanceProcess(null);
                }
            }
        }
    }
     
    @Override
    public String toString() {
        return "ReportInstanceProcessStep [" +
            "created=" + created +
            ", used=" + used +
            ", result=" + result +
            ", success=" + success +
            ", processStepStep=" + processStepStep +
            ", reportInstanceProcessCounter=" + reportInstanceProcessCounter +
        "]";
    }
}
 
