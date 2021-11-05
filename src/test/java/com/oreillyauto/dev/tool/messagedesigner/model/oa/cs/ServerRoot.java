// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.oreillyauto.dev.tool.messagedesigner.model.oa.cs;

import com.oreillyauto.dev.tool.messagedesigner.model.oa.AppServer;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.AppUser;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.AppUserError;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.AppUserLogin;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.JsonType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageConfig;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageSource;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageTypeColumn;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.RpgProgram;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.RpgType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.propertypath.AppUserPP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubMerger;
import com.viaoa.object.OAObject;

/**
 * Root Object that is automatically updated between the Server and Clients. ServerController will do the selects for these objects. Model
 * will share these hubs after the application is started.
 */

@OAClass(useDataSource = false, displayProperty = "Id")
public class ServerRoot extends OAObject {
	private static final long serialVersionUID = 1L;

	public static final String PROPERTY_Id = "Id";
	public static final String P_Id = "Id";

	/*$$Start: ServerRoot1 $$*/
    // lookups, preselects
    public static final String P_AppServers = "AppServers";
    public static final String P_AppUsers = "AppUsers";
    public static final String P_JsonTypes = "JsonTypes";
    public static final String P_MessageSources = "MessageSources";
    public static final String P_RpgPrograms = "RpgPrograms";
    public static final String P_RpgTypes = "RpgTypes";
    // autoCreateOne
    public static final String P_CreateOneAppServerHub = "CreateOneAppServerHub";
    public static final String P_CreateOneMessageConfigHub = "CreateOneMessageConfigHub";
    // filters
    public static final String P_NotVerifiedMessageTypes = "NotVerifiedMessageTypes";
    public static final String P_InvalidRpgTypeMessageTypeColumns = "InvalidRpgTypeMessageTypeColumns";
    // UI containers
    public static final String P_ChangedMessageTypes = "ChangedMessageTypes";
    public static final String P_AppUserLogins = "AppUserLogins";
    public static final String P_AppUserErrors = "AppUserErrors";
/*$$End: ServerRoot1 $$*/

	protected int id;
	/*$$Start: ServerRoot2 $$*/
    // lookups, preselects
    protected transient Hub<AppServer> hubAppServers;
    protected transient Hub<AppUser> hubAppUsers;
    protected transient Hub<JsonType> hubJsonTypes;
    protected transient Hub<MessageSource> hubMessageSources;
    protected transient Hub<RpgProgram> hubRpgPrograms;
    protected transient Hub<RpgType> hubRpgTypes;
    // autoCreateOne
    protected transient Hub<AppServer> hubCreateOneAppServer;
    protected transient Hub<MessageConfig> hubCreateOneMessageConfig;
    // filters
    protected transient Hub<MessageType> hubNotVerifiedMessageTypes;
    protected transient Hub<MessageTypeColumn> hubInvalidRpgTypeMessageTypeColumns;
    // UI containers
    protected transient Hub<MessageType> hubChangedMessageTypes;
    protected transient Hub<AppUserLogin> hubAppUserLogins;
    protected transient Hub<AppUserError> hubAppUserErrors;
/*$$End: ServerRoot2 $$*/

	public ServerRoot() {
		setId(777);
	}

	@OAProperty(displayName = "Id")
	@OAId
	public int getId() {
		return id;
	}

	public void setId(int id) {
		int old = this.id;
		this.id = id;
		firePropertyChange(PROPERTY_Id, old, id);
	}

	/*$$Start: ServerRoot3 $$*/
    // lookups, preselects
    @OAMany(toClass = AppServer.class, cascadeSave = true)
    public Hub<AppServer> getAppServers() {
        if (hubAppServers == null) {
            hubAppServers = (Hub<AppServer>) super.getHub(P_AppServers);
        }
        return hubAppServers;
    }
    @OAMany(toClass = AppUser.class, cascadeSave = true)
    public Hub<AppUser> getAppUsers() {
        if (hubAppUsers == null) {
            hubAppUsers = (Hub<AppUser>) super.getHub(P_AppUsers);
        }
        return hubAppUsers;
    }
    @OAMany(toClass = JsonType.class, sortProperty = JsonType.P_Seq, cascadeSave = true)
    public Hub<JsonType> getJsonTypes() {
        if (hubJsonTypes == null) {
            hubJsonTypes = (Hub<JsonType>) super.getHub(P_JsonTypes, JsonType.P_Seq, true);
        }
        return hubJsonTypes;
    }
    @OAMany(toClass = MessageSource.class, cascadeSave = true)
    public Hub<MessageSource> getMessageSources() {
        if (hubMessageSources == null) {
            hubMessageSources = (Hub<MessageSource>) super.getHub(P_MessageSources);
        }
        return hubMessageSources;
    }
    @OAMany(toClass = RpgProgram.class, cascadeSave = true)
    public Hub<RpgProgram> getRpgPrograms() {
        if (hubRpgPrograms == null) {
            hubRpgPrograms = (Hub<RpgProgram>) super.getHub(P_RpgPrograms);
        }
        return hubRpgPrograms;
    }
    @OAMany(toClass = RpgType.class, sortProperty = RpgType.P_Seq, cascadeSave = true)
    public Hub<RpgType> getRpgTypes() {
        if (hubRpgTypes == null) {
            hubRpgTypes = (Hub<RpgType>) super.getHub(P_RpgTypes, RpgType.P_Seq, true);
        }
        return hubRpgTypes;
    }
    // autoCreatedOne
    @OAMany(toClass = AppServer.class, cascadeSave = true)
    public Hub<AppServer> getCreateOneAppServerHub() {
        if (hubCreateOneAppServer == null) {
            hubCreateOneAppServer = (Hub<AppServer>) super.getHub(P_CreateOneAppServerHub);
        }
        return hubCreateOneAppServer;
    }
    @OAMany(toClass = MessageConfig.class, cascadeSave = true)
    public Hub<MessageConfig> getCreateOneMessageConfigHub() {
        if (hubCreateOneMessageConfig == null) {
            hubCreateOneMessageConfig = (Hub<MessageConfig>) super.getHub(P_CreateOneMessageConfigHub);
        }
        return hubCreateOneMessageConfig;
    }
    // filters
    @OAMany(toClass = MessageType.class, sortProperty = MessageType.P_Display, cascadeSave = true)
    public Hub<MessageType> getNotVerifiedMessageTypes() {
        if (hubNotVerifiedMessageTypes == null) {
            hubNotVerifiedMessageTypes = (Hub<MessageType>) super.getHub(P_NotVerifiedMessageTypes);
        }
        return hubNotVerifiedMessageTypes;
    }
    @OAMany(toClass = MessageTypeColumn.class, cascadeSave = true)
    public Hub<MessageTypeColumn> getInvalidRpgTypeMessageTypeColumns() {
        if (hubInvalidRpgTypeMessageTypeColumns == null) {
            hubInvalidRpgTypeMessageTypeColumns = (Hub<MessageTypeColumn>) super.getHub(P_InvalidRpgTypeMessageTypeColumns);
        }
        return hubInvalidRpgTypeMessageTypeColumns;
    }
    // UI containers
    @OAMany(toClass = MessageType.class, sortProperty = MessageType.P_Display, cascadeSave = true)
    public Hub<MessageType> getChangedMessageTypes() {
        if (hubChangedMessageTypes == null) {
            hubChangedMessageTypes = (Hub<MessageType>) super.getHub(P_ChangedMessageTypes);
        }
        return hubChangedMessageTypes;
    }
    @OAMany(toClass = AppUserLogin.class, isCalculated = true, cascadeSave = true)
    public Hub<AppUserLogin> getAppUserLogins() {
        if (hubAppUserLogins == null) {
            hubAppUserLogins = (Hub<AppUserLogin>) super.getHub(P_AppUserLogins);
            String pp = AppUserPP.appUserLogins().lastDayFilter().pp;
            HubMerger hm = new HubMerger(this.getAppUsers(), hubAppUserLogins, pp, false, true);
        }
        return hubAppUserLogins;
    }
    @OAMany(toClass = AppUserError.class, isCalculated = true, cascadeSave = true)
    public Hub<AppUserError> getAppUserErrors() {
        if (hubAppUserErrors == null) {
            hubAppUserErrors = (Hub<AppUserError>) super.getHub(P_AppUserErrors);
            String pp = AppUserPP.appUserLogins().appUserErrors().pp;
            HubMerger hm = new HubMerger(this.getAppUsers(), hubAppUserErrors, pp, false, true);
        }
        return hubAppUserErrors;
    }
/*$$End: ServerRoot3 $$*/

}
