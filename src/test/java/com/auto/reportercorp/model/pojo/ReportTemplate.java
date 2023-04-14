package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.*;
import java.time.LocalDate;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = ReportTemplate.class)
public class ReportTemplate implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile int id;
    protected volatile LocalDateTime created;
    protected volatile String md5hash;
    protected volatile byte[] template;
    protected volatile LocalDate verified;

    // References to other objects
    // Report
    protected volatile Report report;
    protected volatile Integer reportId; // fkey: report.id
    // ReportVersions
    protected volatile CopyOnWriteArrayList<ReportVersion> alReportVersions = new CopyOnWriteArrayList<>();
     
    public ReportTemplate() {
        this.created = LocalDateTime.now();
    }

    public ReportTemplate(int id) {
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
     
    public String getMd5hash() {
        return md5hash;
    }
    public void setMd5hash(String newValue) {
        this.md5hash = newValue;
    }
     
    public byte[] getTemplate() {
        return template;
    }
    public void setTemplate(byte[] newValue) {
        this.template = newValue;
    }
     
    public LocalDate getVerified() {
        return verified;
    }
    public void setVerified(LocalDate newValue) {
        this.verified = newValue;
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

    @JsonIdentityReference
    public CopyOnWriteArrayList<ReportVersion> getReportVersions() {
        return alReportVersions;
    }
    public void setReportVersions(List<ReportVersion> list) {
        if (list == null) this.alReportVersions.clear();
        else this.alReportVersions = new CopyOnWriteArrayList<>(list);
    }
     
    @Override
    public String toString() {
        return "ReportTemplate [" +
            "id=" + id +
            ", created=" + created +
            ", md5hash=" + md5hash +
            ", verified=" + verified +
            ", reportId=" + reportId +
        "]";
    }
}
 
