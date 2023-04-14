package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.*;
import java.time.LocalDate;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Report.class)
public class Report implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile int id;
    protected volatile LocalDateTime created;
    protected volatile String name;
    protected volatile String title;
    protected volatile String description;
    protected volatile boolean composite;
    protected volatile String fileName;
    protected volatile Type type;
     
    public static enum Type {
        DayEnd, MonthEnd, YearEnd, AdHoc, NightlyTransmit, AnnualInventory, SubReport, Hub, Other;
    }
    protected volatile String packet;
    protected volatile LocalDate verified;
    protected volatile String display;
    protected volatile int category;
    protected volatile int retentionDays;
    protected volatile int subReportSeq;

    // References to other objects
    // ParentReport
    protected volatile Report parentReport;
    protected volatile Integer parentReportId; // fkey: parentReport.id
    // ReportTemplates
    protected volatile CopyOnWriteArrayList<ReportTemplate> alReportTemplates = new CopyOnWriteArrayList<>();
    // SubReports
    protected volatile CopyOnWriteArrayList<Report> alSubReports = new CopyOnWriteArrayList<>();
     
    public Report() {
        this.created = LocalDateTime.now();
    }

    public Report(int id) {
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
     
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        this.name = newValue;
    }
     
    public String getTitle() {
        return title;
    }
    public void setTitle(String newValue) {
        this.title = newValue;
    }
     
    public String getDescription() {
        return description;
    }
    public void setDescription(String newValue) {
        this.description = newValue;
    }
     
    public boolean getComposite() {
        return composite;
    }
    public void setComposite(boolean newValue) {
        this.composite = newValue;
    }
     
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String newValue) {
        this.fileName = newValue;
    }
     
    public Type getType() {
        return type;
    }
    public void setType(Type newType) {
        this.type = newType;
    }
     
    public String getPacket() {
        return packet;
    }
    public void setPacket(String newValue) {
        this.packet = newValue;
    }
     
    public LocalDate getVerified() {
        return verified;
    }
    public void setVerified(LocalDate newValue) {
        this.verified = newValue;
    }
     
    public String getDisplay() {
        return display;
    }
    public void setDisplay(String newValue) {
        this.display = newValue;
    }
     
    public int getCategory() {
        return category;
    }
    public void setCategory(int newValue) {
        this.category = newValue;
    }
     
    public int getRetentionDays() {
        return retentionDays;
    }
    public void setRetentionDays(int newValue) {
        this.retentionDays = newValue;
    }
     
    public int getSubReportSeq() {
        return subReportSeq;
    }
    public void setSubReportSeq(int newValue) {
        this.subReportSeq = newValue;
    }

    @JsonIgnore
    public Report getParentReport() {
        return parentReport;
    }
    public void setParentReport(Report newValue) {
        this.parentReport = newValue;
    }
    public Integer getParentReportId() {
        Report report = this.getParentReport();
        if (report != null) {
            return report.getId();
        }
        return this.parentReportId;
    }
    public void setParentReportId(Integer newValue) {
        this.parentReportId = newValue;
        if (newValue == null) setParentReport(null);
        else {
            if (this.parentReport != null) {
                if (parentReport.getId() != newValue.intValue()) {
                    this.setParentReport(null);
                }
            }
        }
    }

    @JsonIdentityReference
    public CopyOnWriteArrayList<ReportTemplate> getReportTemplates() {
        return alReportTemplates;
    }
    public void setReportTemplates(List<ReportTemplate> list) {
        if (list == null) this.alReportTemplates.clear();
        else this.alReportTemplates = new CopyOnWriteArrayList<>(list);
    }

    @JsonIdentityReference
    public CopyOnWriteArrayList<Report> getSubReports() {
        return alSubReports;
    }
    public void setSubReports(List<Report> list) {
        if (list == null) this.alSubReports.clear();
        else this.alSubReports = new CopyOnWriteArrayList<>(list);
    }
     
    @Override
    public String toString() {
        return "Report [" +
            "id=" + id +
            ", created=" + created +
            ", name=" + name +
            ", title=" + title +
            ", description=" + description +
            ", composite=" + composite +
            ", fileName=" + fileName +
            ", type=" + type +
            ", packet=" + packet +
            ", verified=" + verified +
            ", display=" + display +
            ", category=" + category +
            ", retentionDays=" + retentionDays +
            ", subReportSeq=" + subReportSeq +
            ", parentReportId=" + parentReportId +
        "]";
    }
}
 
