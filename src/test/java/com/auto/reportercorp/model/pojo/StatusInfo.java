package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import com.fasterxml.jackson.annotation.*;
import java.time.*;
 
public class StatusInfo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile String status;
    protected volatile LocalDateTime lastStatus;
    protected volatile int statusCount;
    protected volatile String alert;
    protected volatile LocalDateTime lastAlert;
    protected volatile int alertCount;
    protected volatile String activity;
    protected volatile LocalDateTime lastActivity;
    protected volatile int activityCount;
    protected volatile String console;

    // References to other objects
    // StatusInfoActivityMessages
    protected volatile CopyOnWriteArrayList<StatusInfoMessage> alStatusInfoActivityMessages = new CopyOnWriteArrayList<>();
    // StatusInfoAlertMessages
    protected volatile CopyOnWriteArrayList<StatusInfoMessage> alStatusInfoAlertMessages = new CopyOnWriteArrayList<>();
    // StatusInfoMessages
    protected volatile CopyOnWriteArrayList<StatusInfoMessage> alStatusInfoMessages = new CopyOnWriteArrayList<>();
     
    public StatusInfo() {
        this.created = LocalDateTime.now();
    }
     
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public String getStatus() {
        return status;
    }
    public void setStatus(String newValue) {
        this.status = newValue;
    
        if (this.status == null || this.status.length() == 0) {
            return;
        }
        setLastStatus(LocalDateTime.now());
        int x = getStatusCount() + 1;
        setStatusCount(x);
    
        StatusInfoMessage sim = new StatusInfoMessage();
        sim.setCreated(LocalDateTime.now());
        sim.setMessage(newValue);
        sim.setType(StatusInfoMessage.Type.status);
        sim.setCounter(x);
        getStatusInfoMessages().add(sim);
        if (getStatusInfoMessages().size() > 100) {
            getStatusInfoMessages().remove(0);
        }
    }
     
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy/MM/dd hh:mm:ss")
    public LocalDateTime getLastStatus() {
        return lastStatus;
    }
    public void setLastStatus(LocalDateTime newValue) {
        this.lastStatus = newValue;
    }
     
    public int getStatusCount() {
        return statusCount;
    }
    public void setStatusCount(int newValue) {
        this.statusCount = newValue;
    }
     
    public String getAlert() {
        return alert;
    }
    public void setAlert(String newValue) {
        setAlert(newValue, null);
    }
    public void setAlert(String newValue, Exception e) {
        this.alert = newValue;
    
        if (this.alert == null || this.alert.length() == 0) {
            return;
        }
        setLastAlert(LocalDateTime.now());
        int x = getAlertCount() + 1;
        setAlertCount(x);
    
        StatusInfoMessage sim = new StatusInfoMessage();
        sim.setCreated(LocalDateTime.now());
        sim.setMessage(newValue);
        sim.setType(StatusInfoMessage.Type.alert);
        sim.setCounter(x);
    
        if (e != null) {
            String s = e.getMessage();
            for (StackTraceElement ste : e.getStackTrace()) {
                s += "\n" + ste;
            }
            sim.setException(s);
        }
    
        getStatusInfoAlertMessages().add(sim);
        if (getStatusInfoAlertMessages().size() > 250) {
            getStatusInfoAlertMessages().remove(0);
        }
        setStatus("Alert: " + newValue);
    }
     
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy/MM/dd hh:mm:ss")
    public LocalDateTime getLastAlert() {
        return lastAlert;
    }
    public void setLastAlert(LocalDateTime newValue) {
        this.lastAlert = newValue;
    }
     
    public int getAlertCount() {
        return alertCount;
    }
    public void setAlertCount(int newValue) {
        this.alertCount = newValue;
    }
     
    public String getActivity() {
        return activity;
    }
    public void setActivity(String newValue) {
        this.activity = newValue;
    
        if (this.activity == null || this.activity.length() == 0) {
            return;
        }
        setLastActivity(LocalDateTime.now());
        int x = getActivityCount() + 1;
        setActivityCount(x);
    
        StatusInfoMessage sim = new StatusInfoMessage();
        sim.setCreated(LocalDateTime.now());
        sim.setMessage(newValue);
        sim.setType(StatusInfoMessage.Type.activity);
        sim.setCounter(x);
        getStatusInfoActivityMessages().add(sim);
        if (getStatusInfoActivityMessages().size() > 250) {
            getStatusInfoActivityMessages().remove(0);
        }
        setStatus("Activity: " + newValue);
    }
     
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy/MM/dd hh:mm:ss")
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    public void setLastActivity(LocalDateTime newValue) {
        this.lastActivity = newValue;
    }
     
    public int getActivityCount() {
        return activityCount;
    }
    public void setActivityCount(int newValue) {
        this.activityCount = newValue;
    }
     
    public String getConsole() {
        return console;
    }
    public void setConsole(String newValue) {
        this.console = newValue;
    }

    public CopyOnWriteArrayList<StatusInfoMessage> getStatusInfoActivityMessages() {
        return alStatusInfoActivityMessages;
    }
    public void setStatusInfoActivityMessages(List<StatusInfoMessage> list) {
        if (list == null) this.alStatusInfoActivityMessages.clear();
        else this.alStatusInfoActivityMessages = new CopyOnWriteArrayList<>(list);
    }

    public CopyOnWriteArrayList<StatusInfoMessage> getStatusInfoAlertMessages() {
        return alStatusInfoAlertMessages;
    }
    public void setStatusInfoAlertMessages(List<StatusInfoMessage> list) {
        if (list == null) this.alStatusInfoAlertMessages.clear();
        else this.alStatusInfoAlertMessages = new CopyOnWriteArrayList<>(list);
    }

    public CopyOnWriteArrayList<StatusInfoMessage> getStatusInfoMessages() {
        return alStatusInfoMessages;
    }
    public void setStatusInfoMessages(List<StatusInfoMessage> list) {
        if (list == null) this.alStatusInfoMessages.clear();
        else this.alStatusInfoMessages = new CopyOnWriteArrayList<>(list);
    }
     
    @Override
    public String toString() {
        return "StatusInfo [" +
            "created=" + created +
            ", status=" + status +
            ", lastStatus=" + lastStatus +
            ", statusCount=" + statusCount +
            ", lastAlert=" + lastAlert +
            ", alertCount=" + alertCount +
            ", activity=" + activity +
            ", lastActivity=" + lastActivity +
            ", activityCount=" + activityCount +
            ", console=" + console +
        "]";
    }
}
 
