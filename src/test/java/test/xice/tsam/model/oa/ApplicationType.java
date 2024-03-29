// Generated by OABuilder
package test.xice.tsam.model.oa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OALinkTable;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATable;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;

@OAClass(shortName = "at", displayName = "Application Type", isLookup = true, isPreSelect = true, displayProperty = "code")
@OATable()
public class ApplicationType extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ApplicationType.class.getName());
	public static final String PROPERTY_Id = "Id";
	public static final String P_Id = "Id";
	public static final String PROPERTY_Code = "Code";
	public static final String P_Code = "Code";
	public static final String PROPERTY_Description = "Description";
	public static final String P_Description = "Description";
	public static final String PROPERTY_ServerTypeId = "ServerTypeId";
	public static final String P_ServerTypeId = "ServerTypeId";
	public static final String PROPERTY_Registered = "Registered";
	public static final String P_Registered = "Registered";
	public static final String PROPERTY_DefaultHostName = "DefaultHostName";
	public static final String P_DefaultHostName = "DefaultHostName";
	public static final String PROPERTY_UsesCron = "UsesCron";
	public static final String P_UsesCron = "UsesCron";
	public static final String PROPERTY_UsesPool = "UsesPool";
	public static final String P_UsesPool = "UsesPool";
	public static final String PROPERTY_UsesDns = "UsesDns";
	public static final String P_UsesDns = "UsesDns";
	public static final String PROPERTY_DnsName = "DnsName";
	public static final String P_DnsName = "DnsName";
	public static final String PROPERTY_DnsShortName = "DnsShortName";
	public static final String P_DnsShortName = "DnsShortName";
	public static final String PROPERTY_ClientPort = "ClientPort";
	public static final String P_ClientPort = "ClientPort";
	public static final String PROPERTY_WebPort = "WebPort";
	public static final String P_WebPort = "WebPort";
	public static final String PROPERTY_SslPort = "SslPort";
	public static final String P_SslPort = "SslPort";
	public static final String PROPERTY_VIPClientPort = "VIPClientPort";
	public static final String P_VIPClientPort = "VIPClientPort";
	public static final String PROPERTY_VIPWebPort = "VIPWebPort";
	public static final String P_VIPWebPort = "VIPWebPort";
	public static final String PROPERTY_VIPSSLPort = "VIPSSLPort";
	public static final String P_VIPSSLPort = "VIPSSLPort";
	public static final String PROPERTY_F5Port = "F5Port";
	public static final String P_F5Port = "F5Port";
	public static final String PROPERTY_HasClient = "HasClient";
	public static final String P_HasClient = "HasClient";
	public static final String PROPERTY_UserId = "UserId";
	public static final String P_UserId = "UserId";
	public static final String PROPERTY_UsesIDL = "UsesIDL";
	public static final String P_UsesIDL = "UsesIDL";
	public static final String PROPERTY_Directory = "Directory";
	public static final String P_Directory = "Directory";
	public static final String PROPERTY_JarDirectoryName = "JarDirectoryName";
	public static final String P_JarDirectoryName = "JarDirectoryName";
	public static final String PROPERTY_StartCommand = "StartCommand";
	public static final String P_StartCommand = "StartCommand";
	public static final String PROPERTY_SnapshotStartCommand = "SnapshotStartCommand";
	public static final String P_SnapshotStartCommand = "SnapshotStartCommand";
	public static final String PROPERTY_StopCommand = "StopCommand";
	public static final String P_StopCommand = "StopCommand";
	public static final String PROPERTY_ConnectsToMRAD = "ConnectsToMRAD";
	public static final String P_ConnectsToMRAD = "ConnectsToMRAD";
	public static final String PROPERTY_ShowInMRAD = "ShowInMRAD";
	public static final String P_ShowInMRAD = "ShowInMRAD";
	public static final String PROPERTY_ShowInDeploy = "ShowInDeploy";
	public static final String P_ShowInDeploy = "ShowInDeploy";

	public static final String PROPERTY_Applications = "Applications";
	public static final String P_Applications = "Applications";
	public static final String PROPERTY_ApplicationTypeCommands = "ApplicationTypeCommands";
	public static final String P_ApplicationTypeCommands = "ApplicationTypeCommands";
	public static final String PROPERTY_Developers = "Developers";
	public static final String P_Developers = "Developers";
	public static final String PROPERTY_ExcludeApplicationGroups = "ExcludeApplicationGroups";
	public static final String P_ExcludeApplicationGroups = "ExcludeApplicationGroups";
	public static final String PROPERTY_IncludeApplicationGroups = "IncludeApplicationGroups";
	public static final String P_IncludeApplicationGroups = "IncludeApplicationGroups";
	public static final String PROPERTY_PackageTypes = "PackageTypes";
	public static final String P_PackageTypes = "PackageTypes";
	public static final String PROPERTY_SiloConfigs = "SiloConfigs";
	public static final String P_SiloConfigs = "SiloConfigs";
	public static final String PROPERTY_SiloTypes = "SiloTypes";
	public static final String P_SiloTypes = "SiloTypes";

	protected int id;
	protected String code;
	protected String description;
	protected int serverTypeId;
	protected boolean registered;
	protected String defaultHostName;
	protected boolean usesCron;
	protected boolean usesPool;
	protected boolean usesDns;
	protected String dnsName;
	protected String dnsShortName;
	protected int clientPort;
	protected int webPort;
	protected int sslPort;
	protected int vipClientPort;
	protected int vipWebPort;
	protected int vipSSLPort;
	protected int f5Port;
	protected boolean hasClient;
	protected String userId;
	protected boolean usesIDL;
	protected String directory;
	protected String jarDirectoryName;
	protected String startCommand;
	protected String snapshotStartCommand;
	protected String stopCommand;
	protected boolean connectsToMRAD;
	protected boolean showInMRAD;
	protected boolean showInDeploy;

	// Links to other objects.
	protected transient Hub<ApplicationTypeCommand> hubApplicationTypeCommands;
	protected transient Hub<Developer> hubDevelopers;
	protected transient Hub<PackageType> hubPackageTypes;
	protected transient Hub<SiloType> hubSiloTypes;

	public ApplicationType() {
		if (!isLoading()) {
			setConnectsToMRAD(true);
			setShowInMRAD(true);
			setShowInDeploy(true);
		}
	}

	public ApplicationType(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, displayLength = 5)
	@OAId()
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		fireBeforePropertyChange(P_Id, this.id, newValue);
		int old = id;
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
	}

	@OAProperty(maxLength = 32, isUnique = true, displayLength = 5, columnLength = 7, importMatch = true)
	@OAColumn(maxLength = 32)
	public String getCode() {
		return code;
	}

	public void setCode(String newValue) {
		if (newValue != null) {
			newValue = newValue.toLowerCase();
		}
		fireBeforePropertyChange(P_Code, this.code, newValue);
		String old = code;
		this.code = newValue;
		firePropertyChange(P_Code, old, this.code);
	}

	@OAProperty(maxLength = 250, displayLength = 20, columnLength = 12)
	@OAColumn(maxLength = 250)
	public String getDescription() {
		return description;
	}

	public void setDescription(String newValue) {
		fireBeforePropertyChange(P_Description, this.description, newValue);
		String old = description;
		this.description = newValue;
		firePropertyChange(P_Description, old, this.description);
	}

	@OAProperty(displayName = "Server Type Id", displayLength = 3, columnLength = 2)
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getServerTypeId() {
		return serverTypeId;
	}

	public void setServerTypeId(int newValue) {
		fireBeforePropertyChange(P_ServerTypeId, this.serverTypeId, newValue);
		int old = serverTypeId;
		this.serverTypeId = newValue;
		firePropertyChange(P_ServerTypeId, old, this.serverTypeId);
	}

	@OAProperty(description = "is this on the list of registered server types", displayLength = 2)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	/**
	 * is this on the list of registered server types
	 */
	public boolean getRegistered() {
		return registered;
	}

	public void setRegistered(boolean newValue) {
		fireBeforePropertyChange(P_Registered, this.registered, newValue);
		boolean old = registered;
		this.registered = newValue;
		firePropertyChange(P_Registered, old, this.registered);
	}

	@OAProperty(displayName = "Default Host Name", maxLength = 18, displayLength = 8, columnLength = 6)
	@OAColumn(maxLength = 18)
	public String getDefaultHostName() {
		return defaultHostName;
	}

	public void setDefaultHostName(String newValue) {
		fireBeforePropertyChange(P_DefaultHostName, this.defaultHostName, newValue);
		String old = defaultHostName;
		this.defaultHostName = newValue;
		firePropertyChange(P_DefaultHostName, old, this.defaultHostName);
	}

	@OAProperty(displayName = "Uses Cron", description = "Is cron used to start this server?", displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	/**
	 * Is cron used to start this server?
	 */
	public boolean getUsesCron() {
		return usesCron;
	}

	public void setUsesCron(boolean newValue) {
		fireBeforePropertyChange(P_UsesCron, this.usesCron, newValue);
		boolean old = usesCron;
		this.usesCron = newValue;
		firePropertyChange(P_UsesCron, old, this.usesCron);
	}

	@OAProperty(displayName = "Uses Pool", description = "is there a pool of servers", displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	/**
	 * is there a pool of servers
	 */
	public boolean getUsesPool() {
		return usesPool;
	}

	public void setUsesPool(boolean newValue) {
		fireBeforePropertyChange(P_UsesPool, this.usesPool, newValue);
		boolean old = usesPool;
		this.usesPool = newValue;
		firePropertyChange(P_UsesPool, old, this.usesPool);
	}

	@OAProperty(displayName = "Uses Dns", displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getUsesDns() {
		return usesDns;
	}

	public void setUsesDns(boolean newValue) {
		fireBeforePropertyChange(P_UsesDns, this.usesDns, newValue);
		boolean old = usesDns;
		this.usesDns = newValue;
		firePropertyChange(P_UsesDns, old, this.usesDns);
	}

	@OAProperty(displayName = "DNS Long Name", maxLength = 75, displayLength = 12, columnLength = 8)
	@OAColumn(maxLength = 75)
	public String getDnsName() {
		return dnsName;
	}

	public void setDnsName(String newValue) {
		fireBeforePropertyChange(P_DnsName, this.dnsName, newValue);
		String old = dnsName;
		this.dnsName = newValue;
		firePropertyChange(P_DnsName, old, this.dnsName);
	}

	@OAProperty(displayName = "DNS Short Name", description = "template used to describe DNS name for this server", maxLength = 75, displayLength = 12, columnLength = 7)
	@OAColumn(maxLength = 75)
	/**
	 * template used to describe DNS name for this server
	 */
	public String getDnsShortName() {
		return dnsShortName;
	}

	public void setDnsShortName(String newValue) {
		fireBeforePropertyChange(P_DnsShortName, this.dnsShortName, newValue);
		String old = dnsShortName;
		this.dnsShortName = newValue;
		firePropertyChange(P_DnsShortName, old, this.dnsShortName);
	}

	@OAProperty(displayName = "Client Port", displayLength = 3)
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getClientPort() {
		return clientPort;
	}

	public void setClientPort(int newValue) {
		fireBeforePropertyChange(P_ClientPort, this.clientPort, newValue);
		int old = clientPort;
		this.clientPort = newValue;
		firePropertyChange(P_ClientPort, old, this.clientPort);
	}

	@OAProperty(displayName = "Web Port", displayLength = 3, columnLength = 2)
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getWebPort() {
		return webPort;
	}

	public void setWebPort(int newValue) {
		fireBeforePropertyChange(P_WebPort, this.webPort, newValue);
		int old = webPort;
		this.webPort = newValue;
		firePropertyChange(P_WebPort, old, this.webPort);
	}

	@OAProperty(displayName = "SSL Port", displayLength = 3, columnLength = 2)
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getSslPort() {
		return sslPort;
	}

	public void setSslPort(int newValue) {
		fireBeforePropertyChange(P_SslPort, this.sslPort, newValue);
		int old = sslPort;
		this.sslPort = newValue;
		firePropertyChange(P_SslPort, old, this.sslPort);
	}

	@OAProperty(displayName = "VIP Client Port", displayLength = 3, columnLength = 2)
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getVIPClientPort() {
		return vipClientPort;
	}

	public void setVIPClientPort(int newValue) {
		fireBeforePropertyChange(P_VIPClientPort, this.vipClientPort, newValue);
		int old = vipClientPort;
		this.vipClientPort = newValue;
		firePropertyChange(P_VIPClientPort, old, this.vipClientPort);
	}

	@OAProperty(displayName = "VIP Web Port", displayLength = 3, columnLength = 2)
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getVIPWebPort() {
		return vipWebPort;
	}

	public void setVIPWebPort(int newValue) {
		fireBeforePropertyChange(P_VIPWebPort, this.vipWebPort, newValue);
		int old = vipWebPort;
		this.vipWebPort = newValue;
		firePropertyChange(P_VIPWebPort, old, this.vipWebPort);
	}

	@OAProperty(displayName = "VIP SSL Port", displayLength = 3, columnLength = 2)
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getVIPSSLPort() {
		return vipSSLPort;
	}

	public void setVIPSSLPort(int newValue) {
		fireBeforePropertyChange(P_VIPSSLPort, this.vipSSLPort, newValue);
		int old = vipSSLPort;
		this.vipSSLPort = newValue;
		firePropertyChange(P_VIPSSLPort, old, this.vipSSLPort);
	}

	@OAProperty(displayName = "F5 Port", displayLength = 3, columnLength = 2)
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getF5Port() {
		return f5Port;
	}

	public void setF5Port(int newValue) {
		fireBeforePropertyChange(P_F5Port, this.f5Port, newValue);
		int old = f5Port;
		this.f5Port = newValue;
		firePropertyChange(P_F5Port, old, this.f5Port);
	}

	@OAProperty(displayName = "Has Client", displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getHasClient() {
		return hasClient;
	}

	public void setHasClient(boolean newValue) {
		fireBeforePropertyChange(P_HasClient, this.hasClient, newValue);
		boolean old = hasClient;
		this.hasClient = newValue;
		firePropertyChange(P_HasClient, old, this.hasClient);
	}

	@OAProperty(displayName = "User Id", maxLength = 25, displayLength = 8)
	@OAColumn(maxLength = 25)
	public String getUserId() {
		return userId;
	}

	public void setUserId(String newValue) {
		fireBeforePropertyChange(P_UserId, this.userId, newValue);
		String old = userId;
		this.userId = newValue;
		firePropertyChange(P_UserId, old, this.userId);
	}

	@OAProperty(displayName = "Uses IDL", displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getUsesIDL() {
		return usesIDL;
	}

	public void setUsesIDL(boolean newValue) {
		fireBeforePropertyChange(P_UsesIDL, this.usesIDL, newValue);
		boolean old = usesIDL;
		this.usesIDL = newValue;
		firePropertyChange(P_UsesIDL, old, this.usesIDL);
	}

	@OAProperty(maxLength = 254, displayLength = 14, columnLength = 12)
	@OAColumn(maxLength = 254)
	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String newValue) {
		fireBeforePropertyChange(P_Directory, this.directory, newValue);
		String old = directory;
		this.directory = newValue;
		firePropertyChange(P_Directory, old, this.directory);
	}

	@OAProperty(displayName = "Jar Directory Name", maxLength = 45, displayLength = 8, columnLength = 7)
	@OAColumn(maxLength = 45)
	public String getJarDirectoryName() {
		return jarDirectoryName;
	}

	public void setJarDirectoryName(String newValue) {
		fireBeforePropertyChange(P_JarDirectoryName, this.jarDirectoryName, newValue);
		String old = jarDirectoryName;
		this.jarDirectoryName = newValue;
		firePropertyChange(P_JarDirectoryName, old, this.jarDirectoryName);
	}

	@OAProperty(displayName = "Start Command", maxLength = 254, displayLength = 15, columnLength = 12)
	@OAColumn(maxLength = 254)
	public String getStartCommand() {
		return startCommand;
	}

	public void setStartCommand(String newValue) {
		fireBeforePropertyChange(P_StartCommand, this.startCommand, newValue);
		String old = startCommand;
		this.startCommand = newValue;
		firePropertyChange(P_StartCommand, old, this.startCommand);
	}

	@OAProperty(displayName = "Snapshot Start Command", maxLength = 254, displayLength = 15, columnLength = 12)
	@OAColumn(maxLength = 254)
	public String getSnapshotStartCommand() {
		return snapshotStartCommand;
	}

	public void setSnapshotStartCommand(String newValue) {
		fireBeforePropertyChange(P_SnapshotStartCommand, this.snapshotStartCommand, newValue);
		String old = snapshotStartCommand;
		this.snapshotStartCommand = newValue;
		firePropertyChange(P_SnapshotStartCommand, old, this.snapshotStartCommand);
	}

	@OAProperty(displayName = "Stop Command", maxLength = 254, displayLength = 15, columnLength = 12)
	@OAColumn(maxLength = 254)
	public String getStopCommand() {
		return stopCommand;
	}

	public void setStopCommand(String newValue) {
		fireBeforePropertyChange(P_StopCommand, this.stopCommand, newValue);
		String old = stopCommand;
		this.stopCommand = newValue;
		firePropertyChange(P_StopCommand, old, this.stopCommand);
	}

	@OAProperty(displayName = "Connects To MRAD", defaultValue = "true", displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getConnectsToMRAD() {
		return connectsToMRAD;
	}

	public void setConnectsToMRAD(boolean newValue) {
		fireBeforePropertyChange(P_ConnectsToMRAD, this.connectsToMRAD, newValue);
		boolean old = connectsToMRAD;
		this.connectsToMRAD = newValue;
		firePropertyChange(P_ConnectsToMRAD, old, this.connectsToMRAD);
	}

	@OAProperty(displayName = "Show In MRAD", defaultValue = "true", displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getShowInMRAD() {
		return showInMRAD;
	}

	public void setShowInMRAD(boolean newValue) {
		fireBeforePropertyChange(P_ShowInMRAD, this.showInMRAD, newValue);
		boolean old = showInMRAD;
		this.showInMRAD = newValue;
		firePropertyChange(P_ShowInMRAD, old, this.showInMRAD);
	}

	@OAProperty(displayName = "Show In Deploy", defaultValue = "true", displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getShowInDeploy() {
		return showInDeploy;
	}

	public void setShowInDeploy(boolean newValue) {
		fireBeforePropertyChange(P_ShowInDeploy, this.showInDeploy, newValue);
		boolean old = showInDeploy;
		this.showInDeploy = newValue;
		firePropertyChange(P_ShowInDeploy, old, this.showInDeploy);
	}

	@OAMany(toClass = Application.class, reverseName = Application.P_ApplicationType, createMethod = false)
	private Hub<Application> getApplications() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAMany(displayName = "Application Type Commands", toClass = ApplicationTypeCommand.class, owner = true, reverseName = ApplicationTypeCommand.P_ApplicationType, cascadeSave = true, cascadeDelete = true, matchProperty = ApplicationTypeCommand.P_Command)
	public Hub<ApplicationTypeCommand> getApplicationTypeCommands() {
		if (hubApplicationTypeCommands == null) {
			Hub<Command> hubMatch = test.xice.tsam.delegate.ModelDelegate.getCommands();
			hubApplicationTypeCommands = (Hub<ApplicationTypeCommand>) getHub(P_ApplicationTypeCommands, hubMatch);
		}
		return hubApplicationTypeCommands;
	}

	@OAMany(toClass = Developer.class, reverseName = Developer.P_ApplicationTypes)
	@OALinkTable(name = "DeveloperApplicationType", indexName = "DeveloperApplicationType", columns = { "ApplicationTypeId" })
	public Hub<Developer> getDevelopers() {
		if (hubDevelopers == null) {
			hubDevelopers = (Hub<Developer>) getHub(P_Developers);
		}
		return hubDevelopers;
	}

	@OAMany(displayName = "Application Groups", toClass = ApplicationGroup.class, reverseName = ApplicationGroup.P_ExcludeApplicationTypes, createMethod = false)
	@OALinkTable(name = "ApplicationGroupExcludeApplicationType", indexName = "ApplicationGroupExcludeApplicationType", columns = {
			"ApplicationTypeId" })
	private Hub<ApplicationGroup> getExcludeApplicationGroups() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAMany(displayName = "Include Application Groups", toClass = ApplicationGroup.class, reverseName = ApplicationGroup.P_IncludeApplicationTypes, createMethod = false)
	@OALinkTable(name = "ApplicationGroupIncludeApplicationType", indexName = "ApplicationGroupIncludeApplicationType", columns = {
			"ApplicationTypeId" })
	private Hub<ApplicationGroup> getIncludeApplicationGroups() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAMany(displayName = "Package Types", toClass = PackageType.class, reverseName = PackageType.P_ApplicationTypes)
	@OALinkTable(name = "ApplicationTypePackageType", indexName = "PackageTypeApplicationType", columns = { "ApplicationTypeId" })
	public Hub<PackageType> getPackageTypes() {
		if (hubPackageTypes == null) {
			hubPackageTypes = (Hub<PackageType>) getHub(P_PackageTypes);
		}
		return hubPackageTypes;
	}

	@OAMany(displayName = "Silo Configs", toClass = SiloConfig.class, reverseName = SiloConfig.P_ApplicationType, createMethod = false)
	private Hub<SiloConfig> getSiloConfigs() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAMany(displayName = "Silo Types", toClass = SiloType.class, reverseName = SiloType.P_ApplicationTypes)
	@OALinkTable(name = "SiloTypeApplicationType", indexName = "SiloTypeApplicationType", columns = { "ApplicationTypeId" })
	public Hub<SiloType> getSiloTypes() {
		if (hubSiloTypes == null) {
			hubSiloTypes = (Hub<SiloType>) getHub(P_SiloTypes);
		}
		return hubSiloTypes;
	}

	public void load(ResultSet rs, int id) throws SQLException {
		this.id = id;
		this.code = rs.getString(2);
		this.description = rs.getString(3);
		this.serverTypeId = (int) rs.getInt(4);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_ServerTypeId, true);
		}
		this.registered = rs.getBoolean(5);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_Registered, true);
		}
		this.defaultHostName = rs.getString(6);
		this.usesCron = rs.getBoolean(7);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_UsesCron, true);
		}
		this.usesPool = rs.getBoolean(8);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_UsesPool, true);
		}
		this.usesDns = rs.getBoolean(9);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_UsesDns, true);
		}
		this.dnsName = rs.getString(10);
		this.dnsShortName = rs.getString(11);
		this.clientPort = (int) rs.getInt(12);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_ClientPort, true);
		}
		this.webPort = (int) rs.getInt(13);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_WebPort, true);
		}
		this.sslPort = (int) rs.getInt(14);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_SslPort, true);
		}
		this.vipClientPort = (int) rs.getInt(15);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_VIPClientPort, true);
		}
		this.vipWebPort = (int) rs.getInt(16);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_VIPWebPort, true);
		}
		this.vipSSLPort = (int) rs.getInt(17);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_VIPSSLPort, true);
		}
		this.f5Port = (int) rs.getInt(18);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_F5Port, true);
		}
		this.hasClient = rs.getBoolean(19);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_HasClient, true);
		}
		this.userId = rs.getString(20);
		this.usesIDL = rs.getBoolean(21);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_UsesIDL, true);
		}
		this.directory = rs.getString(22);
		this.jarDirectoryName = rs.getString(23);
		this.startCommand = rs.getString(24);
		this.snapshotStartCommand = rs.getString(25);
		this.stopCommand = rs.getString(26);
		this.connectsToMRAD = rs.getBoolean(27);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_ConnectsToMRAD, true);
		}
		this.showInMRAD = rs.getBoolean(28);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_ShowInMRAD, true);
		}
		this.showInDeploy = rs.getBoolean(29);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, ApplicationType.P_ShowInDeploy, true);
		}
		if (rs.getMetaData().getColumnCount() != 29) {
			throw new SQLException("invalid number of columns for load method");
		}

		changedFlag = false;
		newFlag = false;
	}
}
