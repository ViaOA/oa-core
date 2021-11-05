package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
 
public class AppUserError implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime dateTime;
    protected String message;
    protected String stackTrace;
    protected LocalDate reviewed;
    protected String reviewNote;
     
    // References to other objects.
     
     
    public AppUserError() {
    }
     
    public AppUserError(int id) {
        this();
        setId(id);
    }
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        this.id = newValue;
    }
     
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    public void setDateTime(LocalDateTime newValue) {
        this.dateTime = newValue;
    }
     
    public String getMessage() {
        return message;
    }
    public void setMessage(String newValue) {
        this.message = newValue;
    }
     
    public String getStackTrace() {
        return stackTrace;
    }
    public void setStackTrace(String newValue) {
        this.stackTrace = newValue;
    }
     
    public LocalDate getReviewed() {
        return reviewed;
    }
    public void setReviewed(LocalDate newValue) {
        this.reviewed = newValue;
    }
     
    public String getReviewNote() {
        return reviewNote;
    }
    public void setReviewNote(String newValue) {
        this.reviewNote = newValue;
    }
     
}
 
