// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.corptostore.model.oa.propertypath;

import com.corptostore.model.oa.propertypath.AppServerPPx;
import com.corptostore.model.oa.propertypath.AppUserErrorPPx;
import com.corptostore.model.oa.propertypath.AppUserLoginPPx;
import com.corptostore.model.oa.propertypath.AppUserPPx;
import com.corptostore.model.oa.propertypath.BatchPPx;
import com.corptostore.model.oa.propertypath.CorpToStorePPx;
import com.corptostore.model.oa.propertypath.DashboardLinePPx;
import com.corptostore.model.oa.propertypath.DashboardPPx;
import com.corptostore.model.oa.propertypath.EnvironmentPPx;
import com.corptostore.model.oa.propertypath.ImageStorePPx;
import com.corptostore.model.oa.propertypath.PurgeWindowPPx;
import com.corptostore.model.oa.propertypath.ReceivePPx;
import com.corptostore.model.oa.propertypath.ResendBatchRequestPPx;
import com.corptostore.model.oa.propertypath.SendPPx;
import com.corptostore.model.oa.propertypath.StatusInfoMessagePPx;
import com.corptostore.model.oa.propertypath.StatusInfoPPx;
import com.corptostore.model.oa.propertypath.StoreBatchPPx;
import com.corptostore.model.oa.propertypath.StoreInfoPPx;
import com.corptostore.model.oa.propertypath.StoreLockInfoPPx;
import com.corptostore.model.oa.propertypath.StoreLockServiceInfoPPx;
import com.corptostore.model.oa.propertypath.StorePPx;
import com.corptostore.model.oa.propertypath.StorePurgeInfoPPx;
import com.corptostore.model.oa.propertypath.StoreTransmitBatchPPx;
import com.corptostore.model.oa.propertypath.StoreTransmitInfoPPx;
import com.corptostore.model.oa.propertypath.TesterPPx;
import com.corptostore.model.oa.propertypath.TesterResultPPx;
import com.corptostore.model.oa.propertypath.TesterResultTypePPx;
import com.corptostore.model.oa.propertypath.TesterStepPPx;
import com.corptostore.model.oa.propertypath.TesterStepTypePPx;
import com.corptostore.model.oa.propertypath.TesterStorePPx;
import com.corptostore.model.oa.propertypath.ThreadInfoPPx;
import com.corptostore.model.oa.propertypath.TransmitBatchInfoPPx;
import com.corptostore.model.oa.propertypath.TransmitBatchPPx;
import com.corptostore.model.oa.propertypath.TransmitBatchServiceInfoPPx;
import com.corptostore.model.oa.propertypath.TransmitPPx;

/**
 * Used to build compiler safe property paths.
 * @author vvia
 */
public class PP {
    /*$$Start: PPInterface.code $$*/
    public static AppServerPPx appServer() {
        return new AppServerPPx("AppServer");
    }
    public static AppServerPPx appServers() {
        return new AppServerPPx("AppServers");
    }
    public static AppUserPPx appUser() {
        return new AppUserPPx("AppUser");
    }
    public static AppUserPPx appUsers() {
        return new AppUserPPx("AppUsers");
    }
    public static AppUserErrorPPx appUserError() {
        return new AppUserErrorPPx("AppUserError");
    }
    public static AppUserErrorPPx appUserErrors() {
        return new AppUserErrorPPx("AppUserErrors");
    }
    public static AppUserLoginPPx appUserLogin() {
        return new AppUserLoginPPx("AppUserLogin");
    }
    public static AppUserLoginPPx appUserLogins() {
        return new AppUserLoginPPx("AppUserLogins");
    }
    public static BatchPPx batch() {
        return new BatchPPx("Batch");
    }
    public static BatchPPx batches() {
        return new BatchPPx("Batches");
    }
    public static CorpToStorePPx corpToStore() {
        return new CorpToStorePPx("CorpToStore");
    }
    public static CorpToStorePPx corpToStores() {
        return new CorpToStorePPx("CorpToStores");
    }
    public static DashboardPPx dashboard() {
        return new DashboardPPx("Dashboard");
    }
    public static DashboardPPx dashboards() {
        return new DashboardPPx("Dashboards");
    }
    public static DashboardLinePPx dashboardLine() {
        return new DashboardLinePPx("DashboardLine");
    }
    public static DashboardLinePPx dashboardLines() {
        return new DashboardLinePPx("DashboardLines");
    }
    public static EnvironmentPPx environment() {
        return new EnvironmentPPx("Environment");
    }
    public static EnvironmentPPx environments() {
        return new EnvironmentPPx("Environments");
    }
    public static ImageStorePPx imageStore() {
        return new ImageStorePPx("ImageStore");
    }
    public static ImageStorePPx imageStores() {
        return new ImageStorePPx("ImageStores");
    }
    public static PurgeWindowPPx purgeWindow() {
        return new PurgeWindowPPx("PurgeWindow");
    }
    public static PurgeWindowPPx purgeWindows() {
        return new PurgeWindowPPx("PurgeWindows");
    }
    public static ReceivePPx receive() {
        return new ReceivePPx("Receive");
    }
    public static ReceivePPx receives() {
        return new ReceivePPx("Receives");
    }
    public static ResendBatchRequestPPx resendBatchRequest() {
        return new ResendBatchRequestPPx("ResendBatchRequest");
    }
    public static ResendBatchRequestPPx resendBatchRequests() {
        return new ResendBatchRequestPPx("ResendBatchRequests");
    }
    public static SendPPx send() {
        return new SendPPx("Send");
    }
    public static SendPPx sends() {
        return new SendPPx("Sends");
    }
    public static StatusInfoPPx statusInfo() {
        return new StatusInfoPPx("StatusInfo");
    }
    public static StatusInfoPPx statusInfos() {
        return new StatusInfoPPx("StatusInfos");
    }
    public static StatusInfoMessagePPx statusInfoMessage() {
        return new StatusInfoMessagePPx("StatusInfoMessage");
    }
    public static StatusInfoMessagePPx statusInfoMessages() {
        return new StatusInfoMessagePPx("StatusInfoMessages");
    }
    public static StorePPx store() {
        return new StorePPx("Store");
    }
    public static StorePPx stores() {
        return new StorePPx("Stores");
    }
    public static StoreBatchPPx storeBatch() {
        return new StoreBatchPPx("StoreBatch");
    }
    public static StoreBatchPPx storeBatches() {
        return new StoreBatchPPx("StoreBatches");
    }
    public static StoreInfoPPx storeInfo() {
        return new StoreInfoPPx("StoreInfo");
    }
    public static StoreInfoPPx storeInfos() {
        return new StoreInfoPPx("StoreInfos");
    }
    public static StoreLockInfoPPx storeLockInfo() {
        return new StoreLockInfoPPx("StoreLockInfo");
    }
    public static StoreLockInfoPPx storeLockInfos() {
        return new StoreLockInfoPPx("StoreLockInfos");
    }
    public static StoreLockServiceInfoPPx storeLockServiceInfo() {
        return new StoreLockServiceInfoPPx("StoreLockServiceInfo");
    }
    public static StoreLockServiceInfoPPx storeLockServiceInfos() {
        return new StoreLockServiceInfoPPx("StoreLockServiceInfos");
    }
    public static StorePurgeInfoPPx storePurgeInfo() {
        return new StorePurgeInfoPPx("StorePurgeInfo");
    }
    public static StorePurgeInfoPPx storePurgeInfos() {
        return new StorePurgeInfoPPx("StorePurgeInfos");
    }
    public static StoreTransmitBatchPPx storeTransmitBatch() {
        return new StoreTransmitBatchPPx("StoreTransmitBatch");
    }
    public static StoreTransmitBatchPPx storeTransmitBatches() {
        return new StoreTransmitBatchPPx("StoreTransmitBatches");
    }
    public static StoreTransmitInfoPPx storeTransmitInfo() {
        return new StoreTransmitInfoPPx("StoreTransmitInfo");
    }
    public static StoreTransmitInfoPPx storeTransmitInfos() {
        return new StoreTransmitInfoPPx("StoreTransmitInfos");
    }
    public static TesterPPx tester() {
        return new TesterPPx("Tester");
    }
    public static TesterPPx testers() {
        return new TesterPPx("Testers");
    }
    public static TesterResultPPx testerResult() {
        return new TesterResultPPx("TesterResult");
    }
    public static TesterResultPPx testerResults() {
        return new TesterResultPPx("TesterResults");
    }
    public static TesterResultTypePPx testerResultType() {
        return new TesterResultTypePPx("TesterResultType");
    }
    public static TesterResultTypePPx testerResultTypes() {
        return new TesterResultTypePPx("TesterResultTypes");
    }
    public static TesterStepPPx testerStep() {
        return new TesterStepPPx("TesterStep");
    }
    public static TesterStepPPx testerSteps() {
        return new TesterStepPPx("TesterSteps");
    }
    public static TesterStepTypePPx testerStepType() {
        return new TesterStepTypePPx("TesterStepType");
    }
    public static TesterStepTypePPx testerStepTypes() {
        return new TesterStepTypePPx("TesterStepTypes");
    }
    public static TesterStorePPx testerStore() {
        return new TesterStorePPx("TesterStore");
    }
    public static TesterStorePPx testerStores() {
        return new TesterStorePPx("TesterStores");
    }
    public static ThreadInfoPPx threadInfo() {
        return new ThreadInfoPPx("ThreadInfo");
    }
    public static ThreadInfoPPx threadInfos() {
        return new ThreadInfoPPx("ThreadInfos");
    }
    public static TransmitPPx transmit() {
        return new TransmitPPx("Transmit");
    }
    public static TransmitPPx transmits() {
        return new TransmitPPx("Transmits");
    }
    public static TransmitBatchPPx transmitBatch() {
        return new TransmitBatchPPx("TransmitBatch");
    }
    public static TransmitBatchPPx transmitBatches() {
        return new TransmitBatchPPx("TransmitBatches");
    }
    public static TransmitBatchInfoPPx transmitBatchInfo() {
        return new TransmitBatchInfoPPx("TransmitBatchInfo");
    }
    public static TransmitBatchInfoPPx transmitBatchInfos() {
        return new TransmitBatchInfoPPx("TransmitBatchInfos");
    }
    public static TransmitBatchServiceInfoPPx transmitBatchServiceInfo() {
        return new TransmitBatchServiceInfoPPx("TransmitBatchServiceInfo");
    }
    public static TransmitBatchServiceInfoPPx transmitBatchServiceInfos() {
        return new TransmitBatchServiceInfoPPx("TransmitBatchServiceInfos");
    }
    /*$$End: PPInterface.code $$*/
}
