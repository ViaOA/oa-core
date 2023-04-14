package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.*;
import java.time.LocalDate;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = ReportVersion.class)
public class ReportVersion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile int id;
    protected volatile LocalDateTime created;
    protected volatile LocalDate verified;

    // References to other objects
    // ParentReportVersion
    protected volatile ReportVersion parentReportVersion;
    protected volatile Integer parentReportVersionId; // fkey: parentReportVersion.id
    // ReportTemplate
    protected volatile ReportTemplate reportTemplate;
    protected volatile Integer reportTemplateId; // fkey: reportTemplate.id
    // SubReportVersions
    protected volatile CopyOnWriteArrayList<ReportVersion> alSubReportVersions = new CopyOnWriteArrayList<>();
     
    public ReportVersion() {
        this.created = LocalDateTime.now();
    }

    public ReportVersion(int id) {
        this();
        setId(id);
    }
     
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        this.id = newValue;
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public LocalDate getVerified() {
        return verified;
    }
    public void setVerified(LocalDate newValue) {
        this.verified = newValue;
    }

    @JsonIgnore
    public ReportVersion getParentReportVersion() {
        return parentReportVersion;
    }
    public void setParentReportVersion(ReportVersion newValue) {
        this.parentReportVersion = newValue;
    }
    public Integer getParentReportVersionId() {
        ReportVersion reportVersion = this.getParentReportVersion();
        if (reportVersion != null) {
            return reportVersion.getId();
        }
        return this.parentReportVersionId;
    }
    public void setParentReportVersionId(Integer newValue) {
        this.parentReportVersionId = newValue;
        if (newValue == null) setParentReportVersion(null);
        else {
            if (this.parentReportVersion != null) {
                if (parentReportVersion.getId() != newValue.intValue()) {
                    this.setParentReportVersion(null);
                }
            }
        }
    }

    @JsonIgnore
    public ReportTemplate getReportTemplate() {
        return reportTemplate;
    }
    public void setReportTemplate(ReportTemplate newValue) {
        this.reportTemplate = newValue;
    }
    public Integer getReportTemplateId() {
        ReportTemplate reportTemplate = this.getReportTemplate();
        if (reportTemplate != null) {
            return reportTemplate.getId();
        }
        return this.reportTemplateId;
    }
    public void setReportTemplateId(Integer newValue) {
        this.reportTemplateId = newValue;
        if (newValue == null) setReportTemplate(null);
        else {
            if (this.reportTemplate != null) {
                if (reportTemplate.getId() != newValue.intValue()) {
                    this.setReportTemplate(null);
                }
            }
        }
    }

    @JsonIdentityReference
    public CopyOnWriteArrayList<ReportVersion> getSubReportVersions() {
        return alSubReportVersions;
    }
    public void setSubReportVersions(List<ReportVersion> list) {
        if (list == null) this.alSubReportVersions.clear();
        else this.alSubReportVersions = new CopyOnWriteArrayList<>(list);
    }
     
    @Override
    public String toString() {
        return "ReportVersion [" +
            "id=" + id +
            ", created=" + created +
            ", verified=" + verified +
            ", parentReportVersionId=" + parentReportVersionId +
            ", reportTemplateId=" + reportTemplateId +
        "]";
    }
}
 
