// Copied from OATemplate project by OABuilder 11/16/22 01:55 PM
package com.auto.dev.reportercorp.resource;

import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import com.viaoa.object.OAObject;
import com.viaoa.util.OAConv;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAEncryption;
import com.viaoa.util.OALogger;
import com.viaoa.util.OAProperties;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;

/**
 * Used to setup Locale specific name/value pairs from runtime properties, System.properties, server.ini, client.ini, and resourceBundle.
 * see: values.properties file for list of name/value pairs. Note: as a convention, directory paths should not end with a pathSeparator
 * character. any value that is a file path should be converted to correct pathSeparator
 */
public class Resource {
	private static Logger LOG = OALogger.getLogger(Resource.class);

	public static final String DateFormat = "yyyyMMdd";
	public static final String DateTimeFormat = "yyyyMMdd hh:mm:ss";

	// type of resource
	public static final int TYPE_Server = 0; // server.ini
	public static final int TYPE_Client = 1; // client.ini
	public static final int TYPE_Runtime = 2; // main args[]
	public static final int TYPE_Webserver = 3; // webserver.ini
	public static final int TYPE_Single = 4; // single.ini

	// startup type
	public static final int RUNTYPE_Client = 0;
	public static final int RUNTYPE_JWSClient = 1;
	public static final int RUNTYPE_Server = 2;
	public static final int RUNTYPE_Service = 3;
	public static final int RUNTYPE_Webserver = 4;
	public static final int RUNTYPE_Single = 5;
	private static int runType;
	public static final String[] STARTUP_TYPES = new String[] { "CLIENT", "JWSCLIENT", "SERVER", "SERVICE", "WEBSERVER", "SINGLE" };

	private static OAProperties propsRuntime;
	private static OAProperties propsSystem;
	private static OAProperties propsServer;
	private static OAProperties propsClient;
	private static OAProperties propsSingle;
	private static OAProperties propsEnvironment;
	private static OAProperties propsBundle;
	private static OAProperties propsWebserver;
	private static ResourceBundle resourceBundle;

	private static boolean bServerPropChanged;
	private static boolean bWebserverPropChanged;
	private static boolean bClientPropChanged;
	private static boolean bSinglePropChanged;
	private static String rootDirectory;

	protected static Locale locale;
	protected static NumberFormat numberFormat, currencyFormat, decimalFormat, integerFormat;

	private static String runTimeName;

	// RESOURCE Bundle names
	// NOTE: These need to match up with values.properties
	public static final String APP_ApplicationName = "ApplicationName";
	public static final String APP_ServerApplicationName = "ServerApplicationName";
	public static final String APP_ClientApplicationName = "ClientApplicationName";
	public static final String APP_SingleApplicationName = "SingleApplicationName";

	public static final String APP_ShortName = "ShortName";
	public static final String APP_Version = "Version";
	public static final String APP_Demo = "Demo";
	public static final String APP_DataVersion = "DataVersion";
	public static final String APP_Release = "Release";
	public static final String APP_ReleaseDate = "ReleaseDate";
	public static final String APP_ClassPath = "ClassPath";
	public static final String APP_RootDirectory = "RootDirectory";
	public static final String APP_JWSRootDirectory = "JWSRootDirectory";
	public static final String APP_LogsDirectory = "LogsDirectory";
	public static final String APP_TempDirectory = "TempDirectory";

	public static final String APP_Env = "Env";
	public static final String APP_HostName = "HostName"; // this computer
	public static final String APP_HostIPAddress = "HostIPAddress"; // this computer
	public static final String APP_HostIPAddresses = "HostIPAddresses"; // this computer
	public static final String APP_Server = "Server"; // actual server host
	public static final String APP_ServerPort = "ServerPort";
	public static final String APP_ServerDisplayName = "ServerDisplayName";
	public static final String APP_JavaKeyStore = "JavaKeyStore";

	public static final String APP_JettyServer = "JettyServer";
	public static final String APP_JettyPort = "JettyPort";
	public static final String APP_JettySSLPort = "JettySSLPort";
	public static final String APP_JettyWSPort = "JettyWSPort";
	public static final String APP_JettyDirectory = "JettyDirectory"; // "webcontent"
	public static final String APP_JettyLibDirectory = "JettyLibDirectory"; // for jnlp to find jar files
	public static final String APP_Website = "Website"; // url
	public static final String APP_WelcomePage = "WelcomePage";
	public static final String APP_MainCardPanel = "MainCardPanel"; // for client UI

	public static final String APP_JarImageDirectory = "JarImageDirectory";
	public static final String APP_JarImageDirectory2 = "JarImageDirectory2";
	public static final String APP_FileImageDirectory = "FileImageDirectory";
	public static final String APP_ReportDirectory = "ReportDirectory";
	public static final String APP_Copyright = "Copyright";
	public static final String APP_JarFileName = "JarFileName";
	public static final String APP_Welcome = "Welcome";
	public static final String APP_GoodBye = "GoodBye";
	public static final String APP_JDKVersion = "JDKVersion";
	public static final String APP_LogFileName = "LogFileName";
	public static final String APP_LogLevel = "LogLevel";
	public static final String APP_ConsoleLogLevel = "ConsoleLogLevel";
	public static final String APP_LogErrorDays = "LogErrorDays"; // days to keep Error log files
	public static final String APP_LogRegularDays = "LogRegularDays"; // days to keep regular log files
	public static final String APP_HtmlOutputDirectory = "HtmlOutputDirectory"; // where to write HTML text to.
	public static final String APP_HelpPath1 = "HelpPath1";
	public static final String APP_HelpPath2 = "HelpPath2";
	public static final String APP_HelpJarPath = "HelpJarPath";
	public static final String APP_LookAndFeel = "LookAndFeel";
	public static final String APP_IntegerFormat = "IntegerFormat";
	public static final String APP_DecimalFormat = "DecimalFormat";
	public static final String APP_MoneyFormat = "MoneyFormat";
	public static final String APP_BooleanFormat = "BooleanFormat";
	public static final String APP_DateTimeFormat = "DateTimeFormat";
	public static final String APP_DateFormat = "DateFormat";
	public static final String APP_TimeFormat = "TimeFormat";
	public static final String APP_PhoneFormat = "PhoneFormat";
	public static final String APP_ZipCodeFormat = "ZipCodeFormat";
	public static final String APP_AppUpdateInterval = "AppUpdateInterval";
	public static final String APP_DictionaryFileName = "DictionaryFileName"; // original dict of words
	public static final String APP_DictionaryFileName2 = "DictionaryFileName2"; // new approved words
	public static final String APP_NewWordsFileName = "NewWordsFileName";
	public static final String APP_DataDirectory = "DataDirectory";

	// ini file properties
	public static final String INI_AutoLogin = "AutoLogin";
	public static final String INI_AutoLogout = "AutoLogout";
	public static final String INI_StoreLogin = "StoreLogin";
	public static final String INI_LanguageCode = "LanguageCode";
	public static final String INI_CountryCode = "CountryCode";
	public static final String INI_LogLevel = "LogLevel";
	public static final String INI_Location = "Location";
	public static final String INI_User = "User";
	public static final String INI_Password = "Password";
	public static final String INI_StorePassword = "StorePassword";
	public static final String INI_License = "License";
	public static final String INI_Debug = "Debug";
	public static final String INI_DBDebug = "DBDebug";
	public static final String INI_UIDebug = "UIDebug";
	public static final String INI_SuperAdmin = "SuperAdmin";
	public static final String INI_SaveDataToDatabase = "SaveDataToDatabase";
	public static final String INI_SaveDataToFile = "SaveDataToFile";
	public static final String INI_SaveDataToJsonFile = "SaveDataToJsonFile";
	public static final String INI_SaveDataToXmlFile = "SaveDataToXmlFile";
	public static final String INI_EnableShutdownHook = "EnableShutdownHook";

	public static final String DB_Enabled = "db.enabled";
	public static final String DB_JDBC_Driver = "db.jdbcDriver";
	public static final String DB_JDBC_URL = "db.jdbcUrl";
	public static final String DB_User = "db.user";
	public static final String DB_Password = "db.password";
	public static final String DB_Password_Base64 = "db.passwordBase64";
	public static final String DB_DBMD_Type = "db.dbmdType";
	public static final String DB_MinConnections = "db.minConnections";
	public static final String DB_MaxConnections = "db.maxConnections";
	public static final String DB_BackupDirectory = "db.backupDirectory";
	public static final String DB_CheckForCorruption = "db.checkForCorruption";
	public static final String DB_CacheFileName = "db.cacheFileName";
	public static final String DB_Verify = "db.verify";
	public static final String DB_MaxWaitForSelects = "db.maxWaitForSelects"; // seconds
	public static final String DB_IgnoreWrites = "db.ignoreWrites";

	public static final String SERVER_RecordFileName = "Server.RecordFileName";
	public static final String SERVER_PlayBackFileName = "Server.PlaybackFileName";
	public static final String SERVER_PreloadData = "Server.PreloadData";
	public static final String SERVER_PreloadDataMaxThreadCount = "Server.PreloadDataMaxThreadCount";

	// number of minutes to allow for AWTThread response before close client application.
	public static final String CLIENT_CheckAWTThreadMinutes = "Client.CheckAWTThreadMinutes";

	// Images
	public static final String IMG_CSHelp = "Image.CSHelp";
	public static final String IMG_Exit = "Image.Exit";
	public static final String IMG_Help = "Image.Help";
	public static final String IMG_AppServerIcon = "Image.AppServerIcon";
	public static final String IMG_AppClientIcon = "Image.AppClientIcon";
	public static final String IMG_PrintPreview = "Image.PrintPreview";
	public static final String IMG_Print = "Image.Print";
	public static final String IMG_Save = "Image.Save";
	public static final String IMG_Splash = "Image.Splash";
	public static final String IMG_Login = "Image.Login";
	public static final String IMG_Undo = "Image.Redo";
	public static final String IMG_Redo = "Image.Undo";
	public static final String IMG_Pdf = "Image.Pdf";
	public static final String IMG_PdfBig = "Image.PdfBig";
	public static final String IMG_ImageNotFound = "Image.ImageNotFound";
	public static final String IMG_Search = "Image.search";
	public static final String IMG_Goto = "Image.Goto";
	public static final String IMG_GoBack = "Image.GoBack";
	public static final String IMG_Open = "Image.Open";
	public static final String IMG_OpenFile = "Image.OpenFile";
	public static final String IMG_Filter = "Image.Filter";
	public static final String IMG_NoFilter = "Image.NoFilter";
	public static final String IMG_GroupBy = "Image.GroupBy";
	public static final String IMG_Wizard = "Image.Wizard";
	public static final String IMG_Download = "Image.Download";

	// Report
	public static final String RPT_PaperWidth = "report.PaperWidth"; // => "estimate.report.PaperWidth"
	public static final String RPT_PaperHeight = "report.PaperHeight";
	public static final String RPT_X = "report.x";
	public static final String RPT_Y = "report.y";
	public static final String RPT_Width = "report.width";
	public static final String RPT_Height = "report.height";
	public static final String RPT_Orientation = "report.orientation";

	// Email
	public static final String EMAIL_SMTP_Host = "email.smtp.host";
	public static final String EMAIL_SMTP_Port = "email.smtp.port";
	public static final String EMAIL_UserId = "email.userId";
	public static final String EMAIL_Password = "email.password";

	public static final String MSG_InvalidJVM = "msg.InvalidJVM";
	public static final String MSG_StartupError = "msg.StartupError";

	/**
	 * Location of internally used images, uses getResourceVaue(MSG_JarImagePath)
	 */
	public static String getJarImageDirectory() {
		return getValue(APP_JarImageDirectory);
	}

	/**
	 * Location of images stored by User, uses getResourceVaue(MSG_FileImagePath) Note: does not convert PathSep chars
	 */
	public static String getFileImageDirectory() {
		return getValue(APP_FileImageDirectory);
	}

	public static void setRunTimeName(String name) {
		runTimeName = name;
	}

	public static String getRunTimeName() {
		if (runTimeName == null) {
			runTimeName = getValue(APP_ApplicationName);
		}
		return runTimeName;
	}

	public static void setRunType(int x) {
		runType = x;
	}

	public static int getRunType() {
		return runType;
	}

	/**
	 * This will return a directory path that is correct for OS and ends in a directory separator. public static String getDirectory(String
	 * name) { String s = getValue(name, (String) null); if (s != null) { if (!s.endsWith("/") && !s.endsWith("\\")) s += "/"; s =
	 * OAString.convertFileName(s); } else s = ""; return s; }
	 */

	public static boolean getBoolean(String name) {
		return OAConv.toBoolean(getValue(name, "false"));
	}

	public static boolean getBoolean(String name, boolean bDefault) {
		return OAConv.toBoolean(getValue(name, new Boolean(bDefault).toString()));
	}

	public static int getInt(String name) {
		return OAConv.toInt(getValue(name, "0"));
	}

	public static int getInt(String name, int i) {
		String s = getValue(name, i + "");
		if (s == null) {
			return i;
		}
		return OAConv.toInt(s);
	}

	public static double getDouble(String name) {
		return OAConv.toDouble(getValue(name, "0.0"));
	}

	public static double getDouble(String name, double d) {
		String s = Resource.getValue(name, (String) null);
		if (s == null) {
			return d;
		}
		return OAConv.toDouble(s);
	}

	public static String getValue(String name, Object[] args) {
		return getValue(name, args, null);
	}

	public static String getValue(String name, String defaultValue) {
		return getValue(name, null, defaultValue);
	}

	public static String getValue(String name) {
		return getValue(name, null, null);
	}

	/**
	 * Check to see if name is defined.
	 */
	public static boolean isDefined(String name) {
		String s = getValue(name, null, null);
		return s != null;
	}

	private static final String EncryptPrefix = "b64E";

	/**
	 * Case-insensitive way to get name/value. Checks runtime args, system properties, client.ini, server.ini, values.properties
	 */
	protected static String getValue(String name, Object[] args, String defaultValue) {
		String value = _getValue(name, args, defaultValue);
		if (value != null) {
			if (value.startsWith(EncryptPrefix)) {
				try {
					value = OAEncryption.decrypt(value.substring(EncryptPrefix.length()));
				} catch (Exception e) {
					LOG.log(Level.WARNING, "error while calling decrypt for " + name + ", value=" + value + ", will return value", e);
				}
			}
			value = value.trim();
			if (args != null && args.length > 0) {
				value = MessageFormat.format(value, args);
			}
		} else {
			value = defaultValue;
		}
		return value;
	}

	/**
	 * Utility method to create encrypted value for use by Resource.class, that can then be used as a value in properties/ini name/value
	 * file.
	 */
	public static String getEncryptedValue(String value) {
		if (value == null) {
			return value;
		}
		try {
			value = OAEncryption.encrypt(value);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "error while calling encrypt value=" + value + ", will return value", e);
			return value;
		}
		return EncryptPrefix + value;
	}

	private static String _getValue(String name, Object[] args, String defaultValue) {
		if (name == null) {
			return null;
		}
		String value;

		value = getRuntimeProperties().getString(name);
		if (value != null) {
			return value;
		}

		value = getSystemProperties().getString(name);
		if (value != null) {
			return value;
		}

		if (runType == RUNTYPE_Server || runType == RUNTYPE_Service) {
			value = getServerProperties().getString(name);
			if (value != null) {
				return value;
			}
		} else if (runType == RUNTYPE_Webserver) {
			value = getWebserverProperties().getString(name);
			if (value != null) {
				return value;
			}
		} else if (runType == RUNTYPE_Client) {
			value = getClientProperties().getString(name);
			if (value != null) {
				return value;
			}
		} else if (runType == RUNTYPE_Single) {
			value = getSingleProperties().getString(name);
			if (value != null) {
				return value;
			}
		}

		if (!name.equalsIgnoreCase(APP_Env)) {
			value = getEnvironementProperties().getString(name);
			if (value != null) {
				return value;
			}

			value = getBundleProperties().getString(name);
			if (value != null) {
				return value;
			}
		}

		return value;
	}

	/**
	 * load server.ini
	 */
	public static OAProperties getServerProperties() {
		if (propsServer == null) {
			propsServer = new OAProperties();
			String s = getRootDirectory() + "/server.ini";
			s = OAString.convertFileName(s);
			propsServer.setFileName(s);
			propsServer.load();
			resetLocale();
		}
		return propsServer;
	}

	/**
	 * load webserver.ini
	 */
	public static OAProperties getWebserverProperties() {
		if (propsWebserver == null) {
			propsWebserver = new OAProperties();
			String s = getRootDirectory() + "/webserver.ini";
			s = OAString.convertFileName(s);
			propsWebserver.setFileName(s);
			propsWebserver.load();
			resetLocale();
		}
		return propsWebserver;
	}

	/**
	 * load client.ini
	 */
	public static OAProperties getClientProperties() {
		if (propsClient == null) {
			propsClient = new OAProperties();
			String s = getRootDirectory() + "/client.ini";
			s = OAString.convertFileName(s);
			propsClient.setFileName(s);
			propsClient.load();
			resetLocale();
		}
		return propsClient;
	}

	/**
	 * load single.ini
	 */
	public static OAProperties getSingleProperties() {
		if (propsSingle == null) {
			propsSingle = new OAProperties();
			String s = getRootDirectory() + "/single.ini";
			s = OAString.convertFileName(s);
			propsSingle.setFileName(s);
			propsSingle.load();
			resetLocale();
		}
		return propsSingle;
	}

	/**
	 * Load System.properties to be used case-insensitive
	 */
	public static OAProperties getSystemProperties() {
		if (propsSystem == null) {
			propsSystem = new OAProperties();
			Enumeration enumx = System.getProperties().keys();
			for (; enumx.hasMoreElements();) {
				String key = (String) enumx.nextElement();
				String value = System.getProperties().getProperty(key);
				if (value != null) {
					propsSystem.put(key, value);
				}
			}
			resetLocale();
		}
		return propsSystem;
	}

	/**
	 * runtime arguments.
	 */
	public static OAProperties getRuntimeProperties() {
		if (propsRuntime == null) {
			propsRuntime = new OAProperties();
		}
		return propsRuntime;
	}

	// load args[] into properties
	public static void loadArguments(String[] args) {
		for (int i = 0; args != null && i < args.length; i++) {
			String name = args[i];
			if (name.length() < 2) {
				continue;
			}
			if (name.charAt(0) == '-') {
				name = name.substring(1);
			}
			String value = null;
			int pos = name.indexOf('=');
			if (pos > 0) {
				value = name.substring(pos + 1);
				name = name.substring(0, pos);
			} else {
				value = "true";
			}
			getRuntimeProperties().put(name, value);
			LOG.config("args[" + i + "] name=" + name + ", value=" + value);
		}
		resetLocale();
	}

	public static OAProperties getBundleProperties() {
		if (propsBundle != null) {
			return propsBundle;
		}
		propsBundle = new OAProperties();

		String fname = getResourceBundleFileName();

		Locale loc = locale;
		if (loc == null) {
			loc = Locale.getDefault();
		}

		resourceBundle = null;
		ResourceBundle rb = getResourceBundle();
		// use this for UTF-8
		// resourceBundle = Utf8ResourceBundle.getBundle(fname, loc, Resource.class.getClassLoader());

		for (String key : rb.keySet()) { // now try for case insensitive match
			String value = resourceBundle.getString(key);
			propsBundle.put(key, value);
		}

		// use POM data for jars
		propsBundle.setProperty("OAVersion", OAObject.getOAVersion());

		try {
			InputStream resourceAsStream = OAObject.class
					.getResourceAsStream("/META-INF/maven/com.auto.dev.reportercorp/oaReporterCorp/pom.properties");
			Properties props = new Properties();
			props.load(resourceAsStream);

			// String g = props.getProperty("groupId");
			// String a = props.getProperty("artifactId");
			String v = props.getProperty("version");
			propsBundle.setProperty("Version", v);
		} catch (Exception e) {
		}

		return propsBundle;
	}

	public static String getResourceBundleFileName() {
		String fname = Resource.class.getName();
		int x = fname.lastIndexOf('.');
		if (x >= 0) {
			fname = fname.substring(0, x);
		}
		fname = fname.replace('.', '/');
		fname += "/values.properties";
		return fname;
	}

	public static ResourceBundle getResourceBundle() {
		if (resourceBundle == null) {
			String fname = getResourceBundleFileName();
			fname = fname.substring(0, fname.lastIndexOf('.'));
			fname = fname.replace('/', '.');
			Locale loc = locale;
			if (loc == null) {
				loc = Locale.getDefault();
			}
			try {
				resourceBundle = ResourceBundle.getBundle(fname, loc, Resource.class.getClassLoader());
			} catch (Throwable t) {
				loc = new Locale("en", "US");
				resourceBundle = ResourceBundle.getBundle(fname, loc, Resource.class.getClassLoader());
			}
		}
		return resourceBundle;
	}

	private static boolean bEnvironmentProperties;

	public static OAProperties getEnvironementProperties() {
		if (bEnvironmentProperties && propsEnvironment != null) {
			return propsEnvironment;
		}
		if (propsEnvironment == null) {
			propsEnvironment = new OAProperties();
		}

		String env = getValue(APP_Env);
		if (OAString.isEmpty(env)) {
			return propsEnvironment;
		}

		bEnvironmentProperties = true;

		String fname = Resource.class.getName();
		int x = fname.lastIndexOf('.');
		if (x >= 0) {
			fname = fname.substring(0, x);
		}
		fname = fname.replace('.', '/');
		fname += "/" + env.toLowerCase() + "-values.properties";

		fname = fname.substring(0, fname.lastIndexOf('.'));
		fname = fname.replace('/', '.');
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle(fname);
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "error while loading environment resouce bundle, env=" + env, t);
		}
		if (rb != null) {
			for (String key : rb.keySet()) {
				String value = rb.getString(key);
				propsEnvironment.put(key, value);
			}
		}
		return propsEnvironment;
	}

	/**
	 * If the server or client *.ini file has been change, then it will be saved.
	 */
	public static void save() {
		if (bServerPropChanged) {
			propsServer.save();
			bServerPropChanged = false;
		}
		if (bWebserverPropChanged) {
			propsWebserver.save();
		}
		if (bClientPropChanged) {
			propsClient.save();
			bClientPropChanged = false;
		}
		if (bSinglePropChanged) {
			propsSingle.save();
			bSinglePropChanged = false;
		}
	}

	public static boolean isSuperAdmin() {
		return OAConv.toBoolean(getValue(INI_SuperAdmin));
	}

	/**
	 * Get image from jar file. Uses getDirectory(APP_JarImagePath)
	 *
	 * @param name of image. Use IMG_xxx names.
	 * @return
	 */
	public static ImageIcon getJarIcon(String imageName) {
		String fullName = getValue(APP_JarImageDirectory) + "/" + imageName;

		URL url = Resource.class.getResource(fullName);
		if (url == null) {
			fullName = getValue(APP_JarImageDirectory2) + "/" + imageName;
			url = Resource.class.getResource(fullName);
			if (url == null) {
				fullName = getValue(APP_JarImageDirectory) + "/" + getValue(imageName);
				url = Resource.class.getResource(fullName);
				if (url == null) {
					fullName = getValue(APP_JarImageDirectory2) + "/" + getValue(imageName);
					url = Resource.class.getResource(fullName);
				}
			}
		}

		if (url == null) {
			LOG.warning("Image not found, name=" + imageName);
			imageName = getValue(IMG_ImageNotFound);
			fullName = getValue(APP_JarImageDirectory) + "/" + imageName;
			url = Resource.class.getResource(fullName);
		}
		if (url == null) {
			return null;
		}

		ImageIcon ii = new ImageIcon(url);
		return ii;
	}

	/**
	 * Update a property in an *.ini file.
	 *
	 * @param type  either TYPE_Server, Client, Runtime
	 * @param name
	 * @param value if null then value will be removed.
	 * @see com.RemoteDelegate.delegate.ServerDelegate#setResourceValue(int, String, String) as a delegate that will call this method and
	 *      also update other computers (clients/server).
	 */
	public static void setValue(int type, String name, String value) {
		if (name == null) {
			return;
		}
		if (type == TYPE_Server) {
			if (getServerProperties() != null) {
				if (value == null) {
					if (getServerProperties().remove(name) != null) {
						bServerPropChanged = true;
					}
				} else {
					Object objx = getServerProperties().getProperty(name);
					if (objx == null || !objx.equals(value)) {
						getServerProperties().setProperty(name, value);
						bServerPropChanged = true;
					}
				}
			}
		} else if (type == TYPE_Webserver) {
			if (getWebserverProperties() != null) {
				if (value == null) {
					if (getWebserverProperties().remove(name) != null) {
						bWebserverPropChanged = true;
					}
				} else {
					Object objx = getWebserverProperties().getProperty(name);
					if (objx == null || !objx.equals(value)) {
						getWebserverProperties().setProperty(name, value);
						bWebserverPropChanged = true;
					}
				}
			}
		} else if (type == TYPE_Client) {
			if (getClientProperties() != null) {
				if (value == null) {
					if (getClientProperties().remove(name) != null) {
						bClientPropChanged = true;
					}
				} else {
					Object objx = getClientProperties().getProperty(name);
					if (objx == null || !objx.equals(value)) {
						getClientProperties().setProperty(name, value);
						bClientPropChanged = true;
					}
				}
			}
		} else if (type == TYPE_Single) {
			if (getSingleProperties() != null) {
				if (value == null) {
					if (getSingleProperties().remove(name) != null) {
						bSinglePropChanged = true;
					}
				} else {
					Object objx = getSingleProperties().getProperty(name);
					if (objx == null || !objx.equals(value)) {
						getSingleProperties().setProperty(name, value);
						bSinglePropChanged = true;
					}
				}
			}
		} else {
			if (value == null) {
				getRuntimeProperties().remove(name);
			} else {
				getRuntimeProperties().put(name, value);
			}
		}
	}

	public static void setValue(int type, String name, int val) {
		setValue(type, name, "" + val);
	}

	public static void setValue(int type, String name, boolean b) {
		setValue(type, name, b + "");
	}

	/**
	 * Note: this is set by StartupController
	 */
	public static String getRootDirectory() {
		if (rootDirectory == null) {
			rootDirectory = ".";
			rootDirectory = getValue(APP_RootDirectory, ".");
			rootDirectory = OAString.convertFileName(rootDirectory);
		}
		return rootDirectory;
	}

	public static void setRootDirectory(String dir) {
		rootDirectory = dir;
		setValue(Resource.TYPE_Runtime, Resource.APP_RootDirectory, dir);
		// need to have ini and resource files reloaded
		propsClient = null;
		propsServer = null;
		propsWebserver = null;
		propsBundle = null;
		resourceBundle = null;
	}

	public static String getLogsDirectory() {
		String s = getRootDirectory();
		s += "/" + getValue(APP_LogsDirectory, "logs");
		s = OAString.convertFileName(s);
		return s;
	}

	private static boolean bLocaleManuallySet;

	public static void setLocale(Locale loc) {
		if (loc == null) {
			loc = Locale.getDefault();
		}
		locale = loc;
		bLocaleManuallySet = true;
		resourceBundle = null;
		propsBundle = null;
		resetLocaleDefaults();
	}

	// called when any properties are loaded
	protected static void resetLocale() {
		if (!bLocaleManuallySet && resourceBundle != null) {
			locale = null;
			getLocale();
		}
	}

	public static Locale getLocale() {
		if (locale != null) {
			return locale;
		}

		locale = Locale.getDefault();
		resourceBundle = null;
		propsBundle = null;

		// Setup default Locale
		String strLang = _getValue(Resource.INI_LanguageCode, null, null);
		String strCountry = _getValue(Resource.INI_CountryCode, null, null);
		if (strLang != null || strCountry != null) {
			locale = new Locale(strLang, strCountry); // Ex: ("en","GB") or ("en", "US")
		}
		resetLocaleDefaults();
		return locale;
	}

	protected static void resetLocaleDefaults() {
		if (locale == null) {
			return;
		}
		String s = Resource.getValue(Resource.APP_DateTimeFormat);
		if (s == null || s.length() == 0) {
			OADateTime.setLocale(locale);
		} else {
			OADateTime.setGlobalOutputFormat(s);
		}

		s = Resource.getValue(Resource.APP_DateFormat);
		if (s == null || s.length() == 0) {
			OADate.setLocale(locale);
		} else {
			OADate.setGlobalOutputFormat(s);
		}

		s = Resource.getValue(Resource.APP_TimeFormat);
		if (s == null || s.length() == 0) {
			OADate.setLocale(locale);
		} else {
			OATime.setGlobalOutputFormat(s);
		}

		numberFormat = null;
		getNumberFormatter(); // initialize to fit Locale

		// OAConverter setup
		s = Resource.getValue(Resource.APP_IntegerFormat);
		if (s == null || s.length() == 0) {
			s = ("#,###");
		}
		OAConv.setIntegerFormat(s);

		s = Resource.getValue(Resource.APP_DecimalFormat);
		if (s == null || s.length() == 0) {
			s = ("#,##0.00");
		}
		OAConv.setDecimalFormat(s);
		OAConv.setBigDecimalFormat(s);

		// \u00A4 � = $symbol, see javadoc DecimalFormat.  OAConverterNumber will allow for '$' and will substitute '\u00A4' when converting
		s = Resource.getValue(Resource.APP_MoneyFormat);
		if (s == null || s.length() == 0) {
			s = ("\u00A4#,##0.00");
		}
		OAConv.setMoneyFormat(s);

		s = Resource.getValue(Resource.APP_BooleanFormat);
		if (s == null || s.length() == 0) {
			s = ("true;false;null");
		}
		OAConv.setBooleanFormat(s);
	}

	public static NumberFormat getNumberFormatter() {
		if (numberFormat == null) {
			numberFormat = NumberFormat.getInstance(getLocale());
			numberFormat.setCurrency(Currency.getInstance(getLocale()));

			integerFormat = NumberFormat.getIntegerInstance(getLocale());
			// integerFormat.setMaximumFractionDigits(0);

			decimalFormat = NumberFormat.getCurrencyInstance(getLocale());

			currencyFormat = numberFormat.getCurrencyInstance(getLocale());
		}
		return numberFormat;
	}

	public static NumberFormat getIntegerFormatter() {
		if (integerFormat == null) {
			getNumberFormatter();
		}
		return integerFormat;
	}

	public static NumberFormat getDecimalFormatter() {
		if (decimalFormat == null) {
			getNumberFormatter();
		}
		return decimalFormat;
	}

	public static NumberFormat getCurrencyFormatter() {
		if (currencyFormat == null) {
			getNumberFormatter();
		}
		return currencyFormat;
	}

	public static void main(String[] args) {
		String s = Resource.getEncryptedValue("Enter Password here");
		s = null;
	}
}
