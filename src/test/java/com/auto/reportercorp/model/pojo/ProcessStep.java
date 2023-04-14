package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.time.*;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "step", scope = ProcessStep.class)
public class ProcessStep implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile Step step;
     
    public static enum Step {
        begin, createReportInstance, getReport, getReportTemplate, subreports, checkCompositeReport, checkSubreports, processSubreports, getReportVersion, saveReportInstance, processCompoundReports, completed;
    }
     
    public ProcessStep() {
        this.created = LocalDateTime.now();
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public Step getStep() {
        return step;
    }
    public void setStep(Step newStep) {
        this.step = newStep;
    }
     
    @Override
    public String toString() {
        return "ProcessStep [" +
            "created=" + created +
            ", step=" + step +
        "]";
    }
}
 
