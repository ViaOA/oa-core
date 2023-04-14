package com.auto.reportercorp.model.pojo;
 
import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.time.*;
 
public class StatusInfoMessage implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
     
    protected volatile LocalDateTime created;
    protected volatile Type type;
     
    public static enum Type {
        status, activity, alert;
    }
    protected volatile String message;
    protected volatile int counter;
    protected volatile String exception;

    // References to other objects
    // StatusInfo
    protected volatile StatusInfo statusInfo;
     
    public StatusInfoMessage() {
        this.created = LocalDateTime.now();
    }
     
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd hh:mm:ss.S")
    public LocalDateTime getCreated() {
        return created;
    }
    public void setCreated(LocalDateTime newValue) {
        this.created = newValue;
    }
     
    public Type getType() {
        return type;
    }
    public void setType(Type newType) {
        this.type = newType;
    }
     
    public String getMessage() {
        return message;
    }
    public void setMessage(String newValue) {
        this.message = newValue;
    }
     
    public int getCounter() {
        return counter;
    }
    public void setCounter(int newValue) {
        this.counter = newValue;
    }
     
    public String getException() {
        return exception;
    }
    public void setException(String newValue) {
        this.exception = newValue;
    }

    @JsonIgnore
    public StatusInfo getStatusInfo() {
        return statusInfo;
    }
    public void setStatusInfo(StatusInfo newValue) {
        this.statusInfo = newValue;
    }
     
    @Override
    public String toString() {
        return "StatusInfoMessage [" +
            "created=" + created +
            ", type=" + type +
            ", message=" + message +
            ", counter=" + counter +
        "]";
    }
}
 
