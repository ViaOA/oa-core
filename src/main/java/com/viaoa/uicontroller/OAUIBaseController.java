package com.viaoa.uicontroller;

import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.util.OALogger;

/**
 * Base Controller used to have UI components interact with Hub and OAObjects.
 * <p>
 * @author vince
 */
public abstract class OAUIBaseController {
    private static final Logger LOG = OALogger.getLogger(OAUIBaseController.class);

    protected final Hub hub;

    private String title;
    private String description;
    private String confirmMessage;
    private String completedMessage;


    public OAUIBaseController(Hub hub) {
        this.hub = hub;
    }

    public Hub getHub() {
        return hub;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return this.title;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDescription() {
        return this.description;
    }
    
    public void setConfirmMessage(String msg) {
        this.confirmMessage = msg;
    }
    public String getConfirmMessage() {
        return this.confirmMessage;
    }

    public void setCompletedMessage(String msg) {
        this.completedMessage = msg;
    }
    public String getCompletedMessage() {
        return this.completedMessage;
    }
    
    public boolean isEnabled() {
        if (hub == null) return false;
        return hub.isValid();
    }

    public boolean isVisible() {
        return true;
    }
    
    
    /**
     * These allow for overwriting to handle user interactions.
     */
    protected boolean onConfirm(String confirmMessage, String title) {
        return true;
    }

    protected void onError(String errorMessage, String detailMessage) {
    }
    
    protected void onCompleted(String completedMessage, String title) {
    }
    
}
