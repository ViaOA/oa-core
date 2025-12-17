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
package com.viaoa.util;

import java.util.logging.*;

/*
 * This works with Logger.entering() and Logger.exiting() to create an indentation output.
   Note: you can use the following instead of entering() or exiting() methods
  		LOG.finest("ENTRY");  // or ">", "START", "BEGIN"
        LOG.finest("RETURN"); // or "<", "END"
 */
/**
 * {@code IndentFormatter} is a custom {@link java.util.logging.Formatter} that
 * produces indented log output to visually represent call nesting and execution flow.
 * <p>
 * This formatter is designed to work with {@link java.util.logging.Logger#entering}
 * and {@link java.util.logging.Logger#exiting}, as well as with explicit log messages
 * that signal entry and exit semantics. Recognized entry markers include
 * {@code "ENTRY"}, {@code "BEGIN"}, and {@code ">"}. Recognized exit markers include
 * {@code "RETURN"}, {@code "END"}, and {@code "<"}.
 * <p>
 * When an entry marker is encountered, the formatter increases the indentation
 * level for subsequent log messages. When an exit marker is encountered, the
 * indentation level is decreased. Indentation is rendered using a repeating
 * {@code "|  "} prefix to clearly show call depth.
 * <p>
 * For entry and exit messages, the formatter appends the originating class and
 * method name when available. If a {@link Throwable} is associated with the
 * {@link java.util.logging.LogRecord}, it is included in the output. Log records
 * with a severity higher than {@link java.util.logging.Level#INFO} are additionally
 * highlighted to make elevated log levels more visible.
 * <p>
 * This formatter is useful for debugging and tracing complex execution paths,
 * particularly in deeply nested or recursive code, where understanding the
 * flow of method calls is critical.
 */
public class IndentFormatter extends SimpleFormatter {
	
	/**
	 * Current indentation depth used when formatting log output.
	 * <p>
	 * This value is incremented on entry-style log messages and decremented
	 * on exit-style log messages to visually represent call nesting.
	 */
	int indent = 0;
	
	/**
	 * Cached indentation prefix string corresponding to the current indent level.
	 * <p>
	 * This string is rebuilt whenever the indentation level changes and is
	 * prepended to each formatted log message.
	 */
	String strIndent = "";
    
	/**
	 * Platform-specific line separator used to terminate formatted log entries.
	 */
	public static final String NL = System.getProperty("line.separator");     
	
	/**
	 * Formats a {@link LogRecord} with visual indentation to represent call nesting.
	 * <p>
	 * This formatter recognizes entry and exit markers in the log message
	 * (such as {@code "ENTRY"}, {@code "RETURN"}, {@code "BEGIN"}, {@code "END"},
	 * {@code ">"}, and {@code "<"}) and adjusts indentation accordingly.
	 * <p>
	 * When available, the source class and method names are appended for
	 * entry and exit messages. Thrown exceptions and elevated log levels
	 * are also included in the formatted output.
	 *
	 * @param record the log record to be formatted
	 * @return the formatted log message with indentation and metadata
	 */
	public String format(LogRecord record) {
		String s = record.getMessage();
		if (s == null) s = "";
		boolean bEntry = false;
		if (s.equals("ENTRY") || s.equals(">") || s.equals("BEGIN")) {
			bEntry = true;
			s = "+";
		}
		boolean bExit = false;
		if (s.equals("RETURN") || s.equals("<") || s.equals("END")) {
			bExit = true;
			s = "+";
			indent--;
			if (indent < 0) indent = 0;
			strIndent = "";
			for (int i=0; i<indent;i++) strIndent += "|  ";
		}

		s = strIndent + s;
		
		boolean b = false;
		if (bEntry || bExit) {
			if (record.getSourceClassName() != null) {
				b = true;
			    String s2 = record.getSourceClassName();
			    int pos = s2.lastIndexOf('.');
			    if (pos > 0 && pos < s2.length()) s2 = s2.substring(pos+1);
			    s += s2;
			}
			if (record.getSourceMethodName() != null) {
				if (b) s += ".";
			    s += (record.getSourceMethodName());
			}
		}
		if (record.getThrown() != null) {
			s += ("  EXCEPTION: " + record.getThrown());
		}
		
		if (record.getLevel().intValue() > Level.INFO.intValue()) s += (" *** >>>>" + record.getLevel().getName()).toUpperCase() + "<<<< ***";
		
		s += NL;

		if (bEntry && indent < 10) {
			indent++;
			strIndent = "";
			for (int i=0; i<indent;i++) strIndent += "|  ";
		}
		
		return s;
	}
}

