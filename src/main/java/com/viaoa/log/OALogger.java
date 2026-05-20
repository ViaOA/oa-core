/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.text.IndentFormatter;

/*qqqqqqqqqqqqqqqqqqqqqqq
CODEX

3. src/main/java/com/viaoa/log/OALogger.java:142, src/main/java/com/viaoa/log/OALogger.java:171, src/main/java/com/
     viaoa/log/OALogUtil.java:82

  - Concrete bug: repeated setup adds duplicate ConsoleHandlers.
  - Runtime scenario: tests or runtime bootstrap call setupConsoleLogger, createIndentConsoleLogger, or
    consolePerformance more than once. Each call adds another handler to the same logger, causing each log record to
    print multiple times.
  - Why it violates OA logging semantics: diagnostic output becomes duplicated and misleading, especially for sync/
    remote/queue failure analysis.
  - Minimal fix direction: remove/close prior OA-installed console handlers or make setup idempotent per logger/
    formatter/level.
  - Suggested CODEX comment location: before each log.addHandler(ch).

  4. src/main/java/com/viaoa/log/OALogUtil.java:107

  - Concrete bug: consoleOnly(Level,String) has setup-before-commit behavior that can disable existing logging before
    validating replacement inputs.
  - Runtime scenario: consoleOnly(null, "com.viaoa") or consoleOnly(level, null) first removes/disables current
    handlers, then throws from Handler.setLevel(null) or Logger.getLogger(null).
  - Why it violates OA logging semantics: a failed logging reconfiguration can leave the JVM with logging disabled and
    no caller-visible recovery state beyond the thrown exception.
  - Minimal fix direction: validate level and name before mutating global logger state; construct replacement handler
    before removing old handlers.
  - Suggested CODEX comment location: start of consoleOnly(Level,String).



*/

/*
 * Sets up Logging environment for complete application.  Root package level has Log Handlers
 * for console and log file.
 * <br>
 * The messages used are stored in the Resouce Bundle "values.properties".
 * <br>
 * NOTE: All logging uses the values.properties resouce bundle file for messages.
 *  * <p>
 * The following are the rules for using the different log levels.
 * <ul>
 * <li>SEVERE - fatal, adds to ERROR_* log file, program will exit
 * <li>BUG* - defined in this class
 * <li>ERROR* - defined in this class, popup error, "option to exit app", adds to ERROR_* log file  (int value = WARNING+2)
 * <li>SERVERERROR* - defined in this class, from server, adds to ERROR_* log file  (int value = WARNING+1)
 * <li>CLIENTERROR* - defined in this class, from workstation, adds to ERROR_* log file  (int value = WARNING+1)
 * <li>WARNING - popup error for user
 * <li>INFO - popup for user
 * <li>CONFIG - show in console and status bar
 * <li>FINE   - debug level 1 - console, use this for Entry and Return in the methods
 * <li>FINER  - debug level 2 - console
 * <li>FINEST - debug level 3, testing mode - console
 * </ul>
 */
public class OALogger extends Logger {

	// Log Level used to distingish between a SEVERE and WARNING.
	/**
	 * Custom log level representing a bug condition.
	 */
	public static final Level BUG = new MyLevel("Bug", Level.WARNING.intValue() + 4);
	
	/**
	 * Custom log level representing an application error condition.
	 */
	public static final Level ERROR = new MyLevel("Error", Level.WARNING.intValue() + 3);
	
	/**
	 * Custom log level representing a server-side error condition.
	 */
	public static final Level SERVERERROR = new MyLevel("ServerError", Level.WARNING.intValue() + 2);
	
	/**
	 * Custom log level representing a client-side error condition.
	 */
	public static final Level CLIENTERROR = new MyLevel("ClientError", Level.WARNING.intValue() + 1);

	// used for IndentFormatter to "act" the same as LOG.entering() and LOG.exiting()
	/**
	 * Marker string used to indicate method entry.
	 */
	public static final String Enter = "ENTRY";
	
	/**
	 * Marker string used to indicate method exit.
	 */
	public static final String Exit = "RETURN";

	/**
	 * Custom {@link Level} implementation used to define application-specific log levels.
	 */
	static class MyLevel extends Level {
		/**
		 * Creates a new custom logging level.
		 *
		 * @param name the name of the logging level
		 * @param value the integer value of the logging level
		 */
		public MyLevel(String name, int value) {
			super(name, value);
		}
	};

	/**
	 * Creates a new logger with the specified name and resource bundle.
	 *
	 * @param name the logger name
	 * @param resourceBundleName the resource bundle name
	 */
	protected OALogger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
	}

	/*
	 * @return Logger for class name that uses the resource bundle file values.properties. see Format#getResourceBundleFileName
	 */
	/**
	 * Returns a logger for the given class name.
	 *
	 * @param c the class for which a logger is requested
	 * @return the logger instance, or null if the class is null
	 */
	public static Logger getLogger(Class c) {
		if (c == null) {
			return null;
		}
		//qqqq ToDo:		if (rbFileName == null) rbFileName = Resource.getResourceBundleFileName();
		//return Logger.getLogger(c.getName(), rbFileName);
		return Logger.getLogger(c.getName());
	}

	/*
	 * Create console logging for ClassPath.
	 * 
	 * @param classPath    root path for messages
	 * @param defaultLevel of messages to display.
	 */
	/**
	 * Creates a console logger with indentation formatting for the specified class path.
	 *
	 * @param classPath the root class path for logging
	 * @param defaultLevel the default logging level
	 */
	public static void createIndentConsoleLogger(String classPath, Level defaultLevel) {
		if (defaultLevel == null) {
			defaultLevel = Level.CONFIG;
		}

		// turn off top level logger
		Logger.getLogger("").setLevel(Level.OFF);
		Handler[] hs = Logger.getLogger("").getHandlers();
		for (int i = 0; hs != null && i < hs.length; i++) {
			hs[i].setLevel(Level.OFF);
		}

		Logger log = Logger.getLogger(classPath);
		log.setLevel(defaultLevel);

		// create Console message Handler
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(defaultLevel);
		ch.setFormatter(new IndentFormatter());

		log.addHandler(ch);
	}

	/**
	 * Sets up a basic console logger for the specified class path.
	 *
	 * @param classPath the root class path for logging
	 * @param defaultLevel the default logging level
	 */
	public static void setupConsoleLogger(String classPath, Level defaultLevel) {
		if (defaultLevel == null) {
			defaultLevel = Level.CONFIG;
		}

		// turn off top level logger
		Logger log = Logger.getLogger("");
		log.setLevel(Level.OFF);
		Handler[] hs = log.getHandlers();
		for (int i = 0; hs != null && i < hs.length; i++) {
			hs[i].setLevel(Level.OFF);
		}

		// create Console message Handler
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(defaultLevel);

		log = Logger.getLogger(classPath);
		log.setLevel(defaultLevel);
		log.addHandler(ch);
	}

}
