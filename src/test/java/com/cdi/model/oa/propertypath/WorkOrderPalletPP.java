// Generated by OABuilder
package com.cdi.model.oa.propertypath;
 
import com.cdi.model.oa.*;
 
public class WorkOrderPalletPP {
    private static PalletPPx pallet;
    private static WorkOrderPPx workOrder;
     

    public static PalletPPx pallet() {
        if (pallet == null) pallet = new PalletPPx(WorkOrderPallet.P_Pallet);
        return pallet;
    }

    public static WorkOrderPPx workOrder() {
        if (workOrder == null) workOrder = new WorkOrderPPx(WorkOrderPallet.P_WorkOrder);
        return workOrder;
    }

    public static String id() {
        String s = WorkOrderPallet.P_Id;
        return s;
    }

    public static String amount() {
        String s = WorkOrderPallet.P_Amount;
        return s;
    }
}
 
