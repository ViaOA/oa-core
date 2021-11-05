// Copied from OATemplate project by OABuilder 02/13/19 10:11 AM
package com.oreillyauto.dev.tool.messagedesigner.delegate;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.oreillyauto.dev.tool.messagedesigner.model.oa.AppServer;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.AppUser;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.AppUserError;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.AppUserLogin;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.JsonType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.Message;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageConfig;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageSource;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageTypeColumn;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageTypeRecord;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.RpgMessage;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.RpgProgram;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.RpgType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.cs.ClientRoot;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.cs.ServerRoot;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAODelegate;

/**
 * This is used to access all of the Root level Hubs. This is so that they will not have to be passed into and through the models. After
 * client login, the Hubs will be shared with the Hubs in the ServerRoot object from the server.
 *
 * @author vincevia
 * @see SingleController#initializeClientModel
 */
public class ModelDelegate {
	private static Logger LOG = Logger.getLogger(ModelDelegate.class.getName());

	private static final Hub<AppUserLogin> hubLocalAppUserLogin = new Hub<AppUserLogin>(AppUserLogin.class);
	private static final Hub<AppUser> hubLocalAppUser = new Hub<AppUser>(AppUser.class);

	/*$$Start: ModelDelegate1 $$*/
	// lookups, preselects
	private static final Hub<AppUser> hubAppUsers = new Hub<AppUser>(AppUser.class);
	private static final Hub<JsonType> hubJsonTypes = new Hub<JsonType>(JsonType.class);
	private static final Hub<MessageSource> hubMessageSources = new Hub<MessageSource>(MessageSource.class);
	private static final Hub<RpgProgram> hubRpgPrograms = new Hub<RpgProgram>(RpgProgram.class);
	private static final Hub<RpgType> hubRpgTypes = new Hub<RpgType>(RpgType.class);
	// autoCreateOne
	private static final Hub<AppServer> hubCreateOneAppServer = new Hub<AppServer>(AppServer.class);
	private static final Hub<MessageConfig> hubCreateOneMessageConfig = new Hub<MessageConfig>(MessageConfig.class);
	// filters
	private static final Hub<MessageType> hubNotVerifiedMessageTypes = new Hub<MessageType>(MessageType.class);
	private static final Hub<MessageTypeColumn> hubInvalidRpgTypeMessageTypeColumns = new Hub<MessageTypeColumn>(MessageTypeColumn.class);
	// UI containers
	private static final Hub<Message> hubJsonSearchMessages = new Hub<Message>(Message.class);
	private static final Hub<RpgMessage> hubSearchRpgMessages = new Hub<RpgMessage>(RpgMessage.class);
	private static final Hub<MessageType> hubSearchMessageTypes = new Hub<MessageType>(MessageType.class);
	private static final Hub<MessageTypeRecord> hubSearchMessageTypeRecords = new Hub<MessageTypeRecord>(MessageTypeRecord.class);
	private static final Hub<MessageTypeColumn> hubSearchMessageTypeColumns = new Hub<MessageTypeColumn>(MessageTypeColumn.class);
	private static final Hub<MessageType> hubChangedMessageTypes = new Hub<MessageType>(MessageType.class);
	private static final Hub<AppUserLogin> hubAppUserLogins = new Hub<AppUserLogin>(AppUserLogin.class);
	private static final Hub<AppUserError> hubAppUserErrors = new Hub<AppUserError>(AppUserError.class);
	/*$$End: ModelDelegate1 $$*/

	public static void initialize(ServerRoot rootServer, ClientRoot rootClient) {
		LOG.fine("selecting data");

		/*$$Start: ModelDelegate2 $$*/
		// lookups, preselects
		setSharedHub(getAppUsers(), rootServer.getAppUsers());
		setSharedHub(getJsonTypes(), rootServer.getJsonTypes());
		setSharedHub(getMessageSources(), rootServer.getMessageSources());
		setSharedHub(getRpgPrograms(), rootServer.getRpgPrograms());
		setSharedHub(getRpgTypes(), rootServer.getRpgTypes());
		// autoCreateOne
		setSharedHub(getCreateOneAppServerHub(), rootServer.getCreateOneAppServerHub());
		setSharedHub(getCreateOneMessageConfigHub(), rootServer.getCreateOneMessageConfigHub());
		// filters
		setSharedHub(getNotVerifiedMessageTypes(), rootServer.getNotVerifiedMessageTypes());
		setSharedHub(getInvalidRpgTypeMessageTypeColumns(), rootServer.getInvalidRpgTypeMessageTypeColumns());
		// UI containers
		if (rootClient != null) {
			setSharedHub(getJsonSearchMessages(), rootClient.getJsonSearchMessages());
		}
		if (rootClient != null) {
			setSharedHub(getSearchRpgMessages(), rootClient.getSearchRpgMessages());
		}
		if (rootClient != null) {
			setSharedHub(getSearchMessageTypes(), rootClient.getSearchMessageTypes());
		}
		if (rootClient != null) {
			setSharedHub(getSearchMessageTypeRecords(), rootClient.getSearchMessageTypeRecords());
		}
		if (rootClient != null) {
			setSharedHub(getSearchMessageTypeColumns(), rootClient.getSearchMessageTypeColumns());
		}
		getChangedMessageTypes().setSharedHub(rootServer.getChangedMessageTypes());
		getAppUserLogins().setSharedHub(rootServer.getAppUserLogins());
		getAppUserErrors().setSharedHub(rootServer.getAppUserErrors());
		/*$$End: ModelDelegate2 $$*/

		for (int i = 0; i < 120; i++) {
			if (aiExecutor.get() == 0) {
				break;
			}
			if (i > 5) {
				LOG.fine(i + "/120 seconds) waiting on initialize to finish sharing hubs");
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		if (executorService != null) {
			executorService.shutdown();
			executorService = null;
			queExecutorService = null;
		}
		LOG.fine("completed selecting data");
	}

	public static Hub<AppUser> getLocalAppUserHub() {
		return hubLocalAppUser;
	}

	public static AppUser getLocalAppUser() {
		return getLocalAppUserHub().getAO();
	}

	public static void setLocalAppUser(AppUser user) {
		getLocalAppUserHub().add(user);
		getLocalAppUserHub().setAO(user);
	}

	public static Hub<AppUserLogin> getLocalAppUserLoginHub() {
		return hubLocalAppUserLogin;
	}

	public static AppUserLogin getLocalAppUserLogin() {
		return getLocalAppUserLoginHub().getAO();
	}

	public static void setLocalAppUserLogin(AppUserLogin userLogin) {
		getLocalAppUserLoginHub().add(userLogin);
		getLocalAppUserLoginHub().setAO(userLogin);
		if (userLogin != null) {
			setLocalAppUser(userLogin.getAppUser());
		}
	}

	/*$$Start: ModelDelegate3 $$*/
	public static Hub<AppUser> getAppUsers() {
		return hubAppUsers;
	}

	public static Hub<JsonType> getJsonTypes() {
		return hubJsonTypes;
	}

	public static Hub<MessageSource> getMessageSources() {
		return hubMessageSources;
	}

	public static Hub<RpgProgram> getRpgPrograms() {
		return hubRpgPrograms;
	}

	public static Hub<RpgType> getRpgTypes() {
		return hubRpgTypes;
	}

	// autoCreateOne
	public static Hub<AppServer> getCreateOneAppServerHub() {
		return hubCreateOneAppServer;
	}

	public static AppServer getAppServer() {
		return hubCreateOneAppServer.getAt(0);
	}

	public static Hub<MessageConfig> getCreateOneMessageConfigHub() {
		return hubCreateOneMessageConfig;
	}

	public static MessageConfig getMessageConfig() {
		return hubCreateOneMessageConfig.getAt(0);
	}

	public static Hub<MessageType> getNotVerifiedMessageTypes() {
		return hubNotVerifiedMessageTypes;
	}

	public static Hub<MessageTypeColumn> getInvalidRpgTypeMessageTypeColumns() {
		return hubInvalidRpgTypeMessageTypeColumns;
	}

	public static Hub<Message> getJsonSearchMessages() {
		return hubJsonSearchMessages;
	}

	public static Hub<RpgMessage> getSearchRpgMessages() {
		return hubSearchRpgMessages;
	}

	public static Hub<MessageType> getSearchMessageTypes() {
		return hubSearchMessageTypes;
	}

	public static Hub<MessageTypeRecord> getSearchMessageTypeRecords() {
		return hubSearchMessageTypeRecords;
	}

	public static Hub<MessageTypeColumn> getSearchMessageTypeColumns() {
		return hubSearchMessageTypeColumns;
	}

	public static Hub<MessageType> getChangedMessageTypes() {
		return hubChangedMessageTypes;
	}

	public static Hub<AppUserLogin> getAppUserLogins() {
		return hubAppUserLogins;
	}

	public static Hub<AppUserError> getAppUserErrors() {
		return hubAppUserErrors;
	}
	/*$$End: ModelDelegate3 $$*/

	// thread pool for initialize
	private static ThreadPoolExecutor executorService;
	private static LinkedBlockingQueue<Runnable> queExecutorService;
	private static final AtomicInteger aiExecutor = new AtomicInteger();

	private static void setSharedHub(final Hub h1, final Hub h2) {
		HubAODelegate.warnOnSettingAO(h1);
		if (executorService == null) {
			queExecutorService = new LinkedBlockingQueue<Runnable>(Integer.MAX_VALUE);
			// min/max must be equal, since new threads are only created when queue is full
			executorService = new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, queExecutorService);
			executorService.allowCoreThreadTimeOut(true); // ** must have this
		}

		aiExecutor.incrementAndGet();
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					h1.setSharedHub(h2, false);
				} finally {
					aiExecutor.decrementAndGet();
				}
			}
		});
	}
}
