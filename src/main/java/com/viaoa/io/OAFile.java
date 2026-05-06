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
package com.viaoa.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.viaoa.lang.OAString;

/**
 * Extension of {@link java.io.File} that adds common utility methods used
 * throughout OA for path normalization, directory creation, copying, and basic
 * text-file handling. All paths passed to this class are automatically
 * normalized to the platform's file separator conventions. <p>
 *
 * The class provides helpers for obtaining file names, directory names, and
 * extensions, as well as static methods for recursively creating or deleting
 * directory trees. Additional methods support copying files, copying resources
 * from the classpath to the filesystem, and reading or writing text files. <p>
 *
 * This class performs only basic error handling and does not use
 * try-with-resources; callers that require stronger guarantees around partial
 * writes or error reporting should wrap the I/O operations as needed.
 */
public class OAFile extends java.io.File {
	static final long serialVersionUID = 1L;

	/**
	 * File separator string for the current platform.
	 */
	public static final String FS = File.separator;

	/**
	 * Line separator string for the current platform.
	 */
	public static final String NL = System.getProperty("line.separator");

	/**
	 * Creates a new file instance using a normalized file name.
	 *
	 * @param fname the file name or path
	 */
	public OAFile(String fname) {
		super(OAString.convertFileName(fname));
	}

	/**
	 * Copies this file to the specified file name.
	 *
	 * @param fileNameTo the destination file name
	 * @return true if the copy succeeded, false otherwise
	 */
	public boolean copyTo(String fileNameTo) {
		try {
			copy(this.getPath(), fileNameTo);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Copies this file to the specified destination file.
	 *
	 * @param fileTo the destination file
	 * @return true if the copy succeeded, false otherwise
	 */
	public boolean copyTo(OAFile fileTo) {
		if (fileTo == null) {
			return false;
		}
		try {
			copy(this.getPath(), fileTo.getPath());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Creates all required directories for this file.
	 */
	public void mkdirsForFile() {
		mkdirsForFile(getPath());
	}

	/**
	 * Converts a file path to use the platform-specific file separator.
	 *
	 * @param fileName the file path to convert
	 * @return the converted file path
	 */
	public static String convertFileName(String fileName) {
		return convertFileName(fileName, false);
	}

	/**
	 * Converts a file path to use the platform-specific file separator and
	 * optionally ensures it ends with a separator.
	 *
	 * @param fileName the file path to convert
	 * @param bEndWithSlashChar true to ensure the path ends with a separator
	 * @return the converted file path
	 */
	public static String convertFileName(String fileName, boolean bEndWithSlashChar) {
		if (fileName == null) {
			return null;
		}

		char ch = File.separatorChar;
		if (ch == '/') {
			fileName = fileName.replace('\\', '/');
			//was:  fileName = OAString.convert(fileName, "//", "/");
		} else {
			fileName = fileName.replace('/', '\\');
			//was: fileName = OAString.convert(fileName, "\\\\", "\\"); // bug: if using \\comp\c$\abc\xyz
		}
		if (bEndWithSlashChar && !fileName.endsWith(FS)) {
			fileName += File.separatorChar;
		}
		return fileName;
	}

	/**
	 * Extracts the file name from a file path.
	 *
	 * @param filePath the full file path
	 * @return the file name portion of the path
	 */
	public static String getFileName(String filePath) {
		filePath = filePath.replace('\\', '/');

		int x = filePath.lastIndexOf('/');
		if (x >= 0) {
			filePath = filePath.substring(x + 1);
		}
		filePath = convertFileName(filePath);
		return filePath;
	}

	/**
	 * Returns the directory path portion of a file path.
	 *
	 * @param filePath the full file path
	 * @return the directory path
	 */
	public static String getDirectoryName(String filePath) {
		filePath = filePath.replace('\\', '/');
		String dir = ".";

		int x = filePath.lastIndexOf('/');
		if (x >= 0) {
			dir = filePath.substring(0, x);
		}
		dir = convertFileName(dir);
		return dir;
	}

	/**
	 * Returns the file extension for the given file.
	 *
	 * @param file the file whose extension is returned
	 * @return the file extension, or null if file is null
	 */
	public static String getExtension(File file) {
		if (file == null) {
			return null;
		}
		return getExtension(file.getName());
	}

	/**
	 * Returns the file extension from a file path.
	 *
	 * @param filePath the file path
	 * @return the file extension, or an empty string if none exists
	 */
	public static String getExtension(String filePath) {
		String ext;
		int x = filePath.lastIndexOf('.');
		if (x >= 0) {
			ext = filePath.substring(x + 1);
		} else {
			ext = "";
		}
		return ext;
	}

	/*
	 * Create directories for fileName.<br>
	 * Compared to the method in the File.class, "File.mkdirs()" which creates a directory using the full fileName, where fileName itself
	 * will end up being a directory. This method assumes that the fileName is for a file and will then create the directories needed so
	 * that the file can be saved.
	 */
	/**
	 * Creates all required directories for the given file name.
	 *
	 * @param fileName the file path
	 */
	public static void mkdirsForFile(String fileName) {
		if (fileName == null) {
			return;
		}
		fileName = OAString.convertFileName(fileName);
		int pos = fileName.lastIndexOf(File.separatorChar);
		if (pos > 0) {
			File f = new File(fileName.substring(0, pos));
			f.mkdirs();
		}
	}

	/**
	 * Creates all required directories for the given file.
	 *
	 * @param file the file whose parent directories are created
	 */
	public static void mkdirsForFile(File file) {
		if (file == null) {
			return;
		}
		String fileName = file.getAbsolutePath();
		fileName = OAString.convertFileName(fileName);
		int pos = fileName.lastIndexOf(File.separatorChar);
		if (pos > 0) {
			File f = new File(fileName.substring(0, pos));
			f.mkdirs();
		}
	}

	/**
	 * Renames this file to the specified file name, creating directories if needed.
	 *
	 * @param fileName the new file name
	 */
	public void renameTo(String fileName) {
		if (fileName != null) {
			File f1 = new File(OAString.convertFileName(this.getPath()));
			if (f1.exists()) {
				OAFile.mkdirsForFile(fileName);
				File f2 = new File(OAString.convertFileName(fileName));
				f1.renameTo(f2);
			}
		}
	}

	/**
	 * Copies one file to another file by file name.
	 * NOTE: if the fileNameTo already exists, it will be overwritten.
	 *
	 * @param fileNameFrom the source file name
	 * @param fileNameTo the destination file name
	 * @throws Exception if the copy fails
	 */
	public static void copy(String fileNameFrom, String fileNameTo) throws Exception {
		if (fileNameFrom == null || fileNameTo == null) {
			return;
		}
		fileNameFrom = OAString.convertFileName(fileNameFrom);
		File fileFrom = new File(fileNameFrom);

		fileNameTo = OAString.convertFileName(fileNameTo);
		File fileTo = new File(fileNameTo);

		copy(fileFrom, fileTo);
	}

	/**
	 * Copies one file to another file.
	 *
	 * @param file the source file
	 * @param fileTo the destination file
	 * @throws Exception if the copy fails
	 */
	public static void copy(File file, File fileTo) throws Exception {
		if (file == null || fileTo == null) {
			return;
		}

		if (!file.exists()) {
			throw new Exception("File " + file.getAbsolutePath() + " not found");
		}

		mkdirsForFile(fileTo);
		if (fileTo.exists()) {
			fileTo.delete();
		}

		InputStream is = new FileInputStream(file);
		OutputStream os = new FileOutputStream(fileTo);

		int bufferSize = 32 * 1024;
		byte[] bs = new byte[bufferSize];

		for (int i = 0;; i++) {
			int x = is.read(bs, 0, bufferSize);
			if (x < 0) {
				break;
			}
			os.write(bs, 0, x);
		}
		is.close();
		os.close();
	}

	/*
	 * Copy a file from class/jar to file.
	 *
	 * @param c
	 * @param resourceName class path name for file to read param fname file name to save as param estimatedSize
	 * @return true if successful, false if resource did not exist
	 */
	/**
	 * Copies a resource from the classpath to a file.
	 *
	 * @param c the class used to locate the resource
	 * @param resourceName the classpath resource name
	 * @param fname the destination file name
	 * @return true if successful, false if the resource was not found
	 * @throws Exception if copying fails
	 */
	public static boolean copyResourceToFile(Class c, String resourceName, String fname) throws Exception {
		if (fname == null) {
			return false;
		}
		fname = OAString.convertFileName(fname);

		InputStream is = c.getResourceAsStream(resourceName);
		if (is == null) {
			is = ClassLoader.getSystemResourceAsStream(resourceName);
			if (is == null) {
				return false;
			}
		}

		mkdirsForFile(fname);
		File fileTo = new File(fname);

		OutputStream os = new FileOutputStream(fileTo);

		int bufferSize = 1024 * 2;
		byte[] bs = new byte[bufferSize];

		for (int i = 0;; i++) {
			int x = is.read(bs, 0, bufferSize);
			if (x < 0) {
				break;
			}
			os.write(bs, 0, x);
		}
		is.close();
		os.close();
		return true;
	}

	/*
	 * Read the contents of a text file from a specific class location. This will read from a jar file. param fname '/' seperated file name,
	 * located from the class. If fname begins with '/' then the file will go to the root directory.
	 */
	/**
	 * Reads a text resource from the classpath into an array of strings.
	 *
	 * @param c the class used to locate the resource
	 * @param resourceName the classpath resource name
	 * @return an array of text lines, or null if the resource was not found
	 * @throws Exception if reading fails
	 */
	public static String[] readResourceTextFile(Class c, String resourceName) throws Exception {
		InputStream is = c.getResourceAsStream(resourceName);
		if (is == null) {
			is = ClassLoader.getSystemResourceAsStream(resourceName);
			if (is == null) {
				return null;
			}
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		ArrayList<String> al = new ArrayList<String>(120);
		for (int i = 0;; i++) {
			String s = reader.readLine();
			if (s == null) {
				break;
			}
			al.add(s);
		}
		is.close();
		return al.toArray(new String[0]);
	}

	/*
	 * Read the contents of a text file from a specific class location. This will read from a jar file.
	 *
	 * @param fname '/' seperated file name, located from the class. If fname begins with '/' then the file will go to the root directory.
	 */
	/**
	 * Reads the contents of a text resource from the classpath into a string.
	 *
	 * @param c the class used to locate the resource
	 * @param fname the classpath resource name
	 * @param estimatedSize the estimated size of the content
	 * @return the text content, or null if the resource was not found
	 * @throws Exception if reading fails
	 */
	public static String readTextFile(Class c, String fname, int estimatedSize) throws Exception {
		if (fname == null) {
			return null;
		}
		// fname = OAString.convertFileName(fname); // dont convert, this reads
		// from class and should be using '/'
		InputStream is = c.getResourceAsStream(fname);
		if (is == null) {
			is = ClassLoader.getSystemResourceAsStream(fname);
			if (is == null) {
				return null;
			}
		}

		BufferedInputStream bis = new BufferedInputStream(is);
		StringBuilder sb = new StringBuilder(estimatedSize);
		for (;;) {
			int x = bis.read();
			if (x < 0) {
				break;
			}
			sb.append((char) x);
		}
		is.close();
		return new String(sb);
	}

	/**
	 * Reads the contents of a text file into a string.
	 *
	 * @param file the file to read
	 * @param estimatedSize the estimated size of the content
	 * @return the text content
	 * @throws Exception if reading fails
	 */
	public static String readTextFile(File file, int estimatedSize) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		StringBuffer sb = new StringBuffer(estimatedSize);
		for (;;) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			sb.append(line);
			sb.append(NL);
		}
		reader.close();
		return new String(sb);
	}

	/**
	 * Reads the contents of a text file specified by name into a string.
	 *
	 * @param fname the file name to read
	 * @param estimatedSize the estimated size of the content
	 * @return the text content, or null if the file name is null
	 * @throws Exception if reading fails
	 */
	public static String readTextFile(String fname, int estimatedSize) throws Exception {
		if (fname == null) {
			return null;
		}
		fname = OAString.convertFileName(fname);
		BufferedReader reader = new BufferedReader(new FileReader(fname));
		if (estimatedSize < 100) {
			estimatedSize = 1024 * 4;
		}
		StringBuffer sb = new StringBuffer(estimatedSize);
		for (;;) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			sb.append(line);
			sb.append(NL);
		}
		reader.close();
		return new String(sb);
	}

	/**
	 * Reads the contents of a text file and adds each line to the given list.
	 *
	 * @param fname the file name to read
	 * @param lst the list to populate with lines from the file
	 * @throws Exception if reading fails
	 */
	public static void readTextFile(String fname, final List<String> lst) throws Exception {
		if (fname == null || lst == null) {
			return;
		}
		fname = OAString.convertFileName(fname);
		BufferedReader reader = new BufferedReader(new FileReader(fname));
		for (;;) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			lst.add(line);
		}
		reader.close();
	}

	/**
	 * Writes text data to a file specified by name.
	 *
	 * @param fname the file name to write
	 * @param data the text data to write
	 * @return true if the write succeeded, false otherwise
	 * @throws Exception if writing fails
	 */
	public static boolean writeTextFile(String fname, String data) throws Exception {
		if (fname == null) {
			return false;
		}
		fname = OAString.convertFileName(fname);

		mkdirsForFile(fname);
		File fileTo = new File(fname);

		OutputStream os = new FileOutputStream(fileTo);

		if (data != null) {
			os.write(data.getBytes());
		}

		os.close();
		return true;
	}

	/**
	 * Writes text data to the specified file.
	 *
	 * @param file the file to write
	 * @param data the text data to write
	 * @return true if the write succeeded, false otherwise
	 * @throws Exception if writing fails
	 */
	public static boolean writeTextFile(File file, String data) throws Exception {
		if (file == null) {
			return false;
		}

		mkdirsForFile(file);
		OutputStream os = new FileOutputStream(file);

		if (data != null) {
			os.write(data.getBytes());
		}

		os.close();
		return true;
	}

	/**
	 * Removes a directory and all of its contents.
	 *
	 * @param f the directory to remove
	 * @throws IOException if removal fails
	 */
	public static void rmDir(File f) throws IOException {
		delTree(f);
	}

	/**
	 * Removes a directory and all of its contents.
	 *
	 * @param f the directory to remove
	 * @throws IOException if removal fails
	 */
	public static void removeDir(File f) throws IOException {
		delTree(f);
	}

	/**
	 * Recursively deletes a file or directory tree.
	 *
	 * @param f the file or directory to delete
	 * @throws IOException if deletion fails
	 */
	public static void delTree(File f) throws IOException {
		if (f == null || !f.exists()) {
			return;
		}
		if (f.isDirectory()) {
			File[] fs = f.listFiles();
			if (fs != null) {
				for (File c : fs) {
					delTree(c);
				}
			}
		}
		f.delete();
	}
}
