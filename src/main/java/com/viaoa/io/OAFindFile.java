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

import java.io.*;
import java.util.*;
import java.util.zip.*;


/**
 * Utility for recursively searching a directory tree for files with a specific
 * name. The search descends into all subdirectories and also inspects archive
 * files ({@code .zip}, {@code .jar}, {@code .war}, {@code .ear}) by scanning
 * their entries with a {@link ZipInputStream}. <p>
 *
 * When a match is found inside an archive, the returned path is composed of the
 * archive's absolute path followed by '!' and the entry name. The search is
 * case-insensitive with respect to the target filename. <p>
 *
 * This class performs a best-effort search; ZIP errors are caught and ignored.
 * Instances are not thread-safe and should not be shared across threads.
 */
public class OAFindFile {
	
	/**
	 * File extensions that are treated as ZIP-compatible archives during search.
	 */
	static final String[] ZIP_EXTENSIONS = { ".zip", ".jar", ".war", ".ear" };

	/**
	 * The target file name to search for.
	 */
	private String findFileName;
	
	/**
	 * Accumulates matched file paths during the search.
	 */
	private ArrayList<String> list;

	/**
	 * Searches recursively starting from the given root path for files with the specified name.
	 *
	 * @param rootFile the root directory path to start searching from
	 * @param findFileName the file name to search for
	 * @return an array of matched file paths
	 * @throws IOException if an I/O error occurs
	 */
	public String[] findAll(String rootFile, String findFileName) throws IOException {
		if (rootFile == null || rootFile.trim().length() == 0) rootFile = ".";
		return findAll(new File(rootFile), findFileName);
	}
	
	/**
	 * Searches recursively starting from the given root file for files with the specified name.
	 *
	 * @param rootFile the root file or directory to start searching from
	 * @param findFileName the file name to search for
	 * @return an array of matched file paths
	 * @throws IOException if an I/O error occurs
	 */
	public String[] findAll(File rootFile, String findFileName) throws IOException {
		if (findFileName == null || findFileName.trim().length() == 0) return new String[0];
		this.findFileName = findFileName;
		this.list = new ArrayList<String>();
		findFile(rootFile);
		String[] ss = new String[list.size()];
		this.list.toArray(ss);
		this.list = null;
		return ss;
	}
	
	/**
	 * Recursively inspects the given file or directory, adding matches to the result list.
	 *
	 * @param file the file or directory to inspect
	 * @throws IOException if an I/O error occurs
	 */
	protected void findFile(File file) throws IOException {
		if (file.isDirectory()) {
	        // System.out.println("checking "+file);         
			File[] files = file.listFiles();
			if (files != null) {
    			for (int i = 0; i < files.length; i++) {
    				findFile(files[i]);
    			}
			}
		} 
		else {
			// if ((cnt%100) == 0) System.out.println("Status: " + cnt+") "+file.getAbsolutePath());			
			String fileName = file.getName();
			if (fileName.equalsIgnoreCase(findFileName)) {
				list.add(file.getAbsolutePath());
				//System.out.println("Found #"+(list.size()) + " = " + file);		
			}
			else {
				for (int i = 0; i < ZIP_EXTENSIONS.length; i++) {
					if (fileName.toLowerCase().endsWith(ZIP_EXTENSIONS[i])) {
						//System.out.println("Compressed= "+file.getAbsolutePath());			
						try {
						    findZip(file);
						}
						catch (Exception e) {
						    System.out.println("Error with zip file:" +fileName+", "+e);
						}
						break;
					}
				}
			}
		}
	}

	/**
	 * Searches a ZIP-compatible archive file for entries matching the target file name.
	 *
	 * @param file the archive file to search
	 * @throws IOException if an I/O error occurs
	 */
	protected void findZip(File file) throws IOException {
        //System.out.println("checking zip "+file);         
		InputStream in = new FileInputStream(file);
		ZipInputStream zin = new ZipInputStream(in);

		ZipEntry en;
		while ((en = zin.getNextEntry()) != null) {
			if (en.isDirectory()) continue;
			String fn = en.getName();
			int pos = fn.lastIndexOf('/');
			if (pos < 0) pos = fn.lastIndexOf('\\');
			if (pos >= 0) fn = fn.substring(pos+1);
			if (fn.equalsIgnoreCase(findFileName)) {
				String s = file.getAbsolutePath() + "!" + en.getName();
				list.add(s);
				// System.out.println("Found #"+(list.size()) + " = " + file + "!" + en);		
			}
		}
	}
	
}

