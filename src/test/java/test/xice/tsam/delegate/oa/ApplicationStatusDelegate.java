package test.xice.tsam.delegate.oa;

import java.awt.Color;

import test.xice.tsam.delegate.ModelDelegate;
import test.xice.tsam.model.oa.ApplicationStatus;

public class ApplicationStatusDelegate {

    
    public static ApplicationStatus getApplicationStatus(int type) {
        ApplicationStatus ss = ModelDelegate.getApplicationStatuses().find(ApplicationStatus.P_Type, type);
        return ss;
    }
 
    public static void setDefaultColors() {
        for (ApplicationStatus ss : ModelDelegate.getApplicationStatuses()) {
            ss.getColor();
        }
    }
    
}
