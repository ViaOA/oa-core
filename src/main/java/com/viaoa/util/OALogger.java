/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	public static final Level BUG = new MyLevel("Bug", Level.WARNING.intValue() + 4);
	public static final Level ERROR = new MyLevel("Error", Level.WARNING.intValue() + 3);
	public static final Level SERVERERROR = new MyLevel("ServerError", Level.WARNING.intValue() + 2);
	public static final Level CLIENTERROR = new MyLevel("ClientError", Level.WARNING.intValue() + 1);

	// used for IndentFormatter to "act" the same as LOG.entering() and LOG.exiting()
	public static final String Enter = "ENTRY";
	public static final String Exit = "RETURN";

	static class MyLevel extends Level {
		public MyLevel(String name, int value) {
			super(name, value);
		}
	};

	protected OALogger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
	}

	/**
	 * @return Logger for class name that uses the resource bundle file values.properties. see Format#getResourceBundleFileName
	 */
	public static Logger getLogger(Class c) {
		if (c == null) {
			return null;
		}
		//qqqq ToDo:		if (rbFileName == null) rbFileName = Resource.getResourceBundleFileName();
		//return Logger.getLogger(c.getName(), rbFileName);
		return Logger.getLogger(c.getName());
	}

	/**
	 * Create console logging for ClassPath.
	 * 
	 * @param classPath    root path for messages
	 * @param defaultLevel of messages to display.
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
