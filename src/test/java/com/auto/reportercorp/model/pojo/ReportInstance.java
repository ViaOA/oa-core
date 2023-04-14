package com.auto.reportercorp.model.pojo;
 
import java.util.*;

import com.auto.reportercorp.util.EmbeddedJsonStringDeserializer;
import com.fasterxml.jackson.annotation.*;
import java.util.concurrent.CopyOnWriteArrayList;
import com.fasterxml.jackson.databind.annotation.*;

import java.time.*;
 
@JsonIdentityInfo(generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = ReportInstance.class)
public class ReportInstance implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile long id;
    protected volatile LocalDateTime created;
    protected volatile int storeNumber;
    protected volatile String fileName;
    protected volatile String title;
    protected volatile int compositePos;
    protected volatile String data;

    // References to other objects
    // ParentCompositeReportInstance
    protected volatile ReportInstance parentCompositeReportInstance;
    protected volatile Long parentCompositeReportInstanceId; // fkey: parentCompositeReportInstance.id
    // PypeReportMessage
    protected volatile PypeReportMessage pypeReportMessage;
    protected volatile Integer pypeReportMessageStore; // importMatch: pypeReportMessage.store
    protected volatile String pypeReportMessageFilename; // importMatch: pypeReportMessage.filename
    // ReportVersion
    protected volatile ReportVersion reportVersion;
    protected volatile Integer reportVersionId; // fkey: reportVersion.id
    // CompositeReportInstances
    protected volatile CopyOnWriteArrayList<ReportInstance> alCompositeReportInstances = new CopyOnWriteArrayList<>();
     
    public ReportInstance() {
        this.created = LocalDateTime.now();
    }

    public ReportInstance(long id) {
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
     
    public int getStoreNumber() {
        return storeNumber;
    }
    public void setStoreNumber(int newValue) {
        this.storeNumber = newValue;
    }
     
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String newValue) {
        this.fileName = newValue;
    }
     
    public String getTitle() {
        return title;
    }
    public void setTitle(String newValue) {
        this.title = newValue;
    }
     
    public int getCompositePos() {
        return compositePos;
    }
    public void setCompositePos(int newValue) {
        this.compositePos = newValue;
    }
     
    @JsonRawValue
    @JsonDeserialize(using = EmbeddedJsonStringDeserializer.class)
    public String getData() {
        return data;
    }
    public void setData(String newValue) {
        this.data = newValue;
    }

    @JsonIgnore
    public ReportInstance getParentCompositeReportInstance() {
        return parentCompositeReportInstance;
    }
    public void setParentCompositeReportInstance(ReportInstance newValue) {
        this.parentCompositeReportInstance = newValue;
    }
    public Long getParentCompositeReportInstanceId() {
        ReportInstance reportInstance = this.getParentCompositeReportInstance();
        if (reportInstance != null) {
            return reportInstance.getId();
        }
        return this.parentCompositeReportInstanceId;
    }
    public void setParentCompositeReportInstanceId(Long newValue) {
        this.parentCompositeReportInstanceId = newValue;
        if (newValue == null) setParentCompositeReportInstance(null);
        else {
            if (this.parentCompositeReportInstance != null) {
                if (parentCompositeReportInstance.getId() != newValue.longValue()) {
                    this.setParentCompositeReportInstance(null);
                }
            }
        }
    }

    @JsonIgnore
    public PypeReportMessage getPypeReportMessage() {
        return pypeReportMessage;
    }
    public void setPypeReportMessage(PypeReportMessage newValue) {
        this.pypeReportMessage = newValue;
    }
    public Integer getPypeReportMessageStore() {
        PypeReportMessage pypeReportMessage = this.getPypeReportMessage();
        if (pypeReportMessage != null) {
            return pypeReportMessage.getStore();
        }
        return this.pypeReportMessageStore;
    }
    public void setPypeReportMessageStore(Integer newValue) {
        this.pypeReportMessageStore = newValue;
        if (newValue == null) setPypeReportMessage(null);
        else {
            if (this.pypeReportMessage != null) {
                if (pypeReportMessage.getStore() != newValue.intValue()) {
                    this.setPypeReportMessage(null);
                }
            }
        }
    }
    public String getPypeReportMessageFilename() {
        PypeReportMessage pypeReportMessage = this.getPypeReportMessage();
        if (pypeReportMessage != null) {
            return pypeReportMessage.getFilename();
        }
        return this.pypeReportMessageFilename;
    }
    public void setPypeReportMessageFilename(String newValue) {
        this.pypeReportMessageFilename = newValue;
        if (newValue == null) setPypeReportMessage(null);
        else {
            if (this.pypeReportMessage != null) {
                if (!newValue.equals(this.pypeReportMessage.getFilename())) {
                    this.setPypeReportMessage(null);
                }
            }
        }
    }

    @JsonIgnore
    public ReportVersion getReportVersion() {
        return reportVersion;
    }
    public void setReportVersion(ReportVersion newValue) {
        this.reportVersion = newValue;
    }
    public Integer getReportVersionId() {
        ReportVersion reportVersion = this.getReportVersion();
        if (reportVersion != null) {
            return reportVersion.getId();
        }
        return this.reportVersionId;
    }
    public void setReportVersionId(Integer newValue) {
        this.reportVersionId = newValue;
        if (newValue == null) setReportVersion(null);
        else {
            if (this.reportVersion != null) {
                if (reportVersion.getId() != newValue.intValue()) {
                    this.setReportVersion(null);
                }
            }
        }
    }

    @JsonIdentityReference
    public CopyOnWriteArrayList<ReportInstance> getCompositeReportInstances() {
        return alCompositeReportInstances;
    }
    public void setCompositeReportInstances(List<ReportInstance> list) {
        if (list == null) this.alCompositeReportInstances.clear();
        else this.alCompositeReportInstances = new CopyOnWriteArrayList<>(list);
    }
     
    @Override
    public String toString() {
        return "ReportInstance [" +
            "id=" + id +
            ", created=" + created +
            ", storeNumber=" + storeNumber +
            ", fileName=" + fileName +
            ", title=" + title +
            ", compositePos=" + compositePos +
            ", parentCompositeReportInstanceId=" + parentCompositeReportInstanceId +
            ", pypeReportMessageStore=" + pypeReportMessageStore +
            ", pypeReportMessageFilename=" + pypeReportMessageFilename +
            ", reportVersionId=" + reportVersionId +
        "]";
    }
}
 
