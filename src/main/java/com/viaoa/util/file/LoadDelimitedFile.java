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
package com.viaoa.util.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import com.viaoa.util.*;

/**
 * Utility class for reading a delimited text file line by line and converting
 * each row into an array of column values. Subclasses implement
 * {@link #process(String[], int)} to receive parsed rows. <p>
 *
 * The {@link #read(File, String, boolean)} method streams the file using a
 * {@link java.io.BufferedReader}, parses each line using
 * {@link com.viaoa.util.OAString#parseLine(String, char, boolean)}, and
 * invokes the process callback for each successfully parsed row. Column
 * parsing supports optional quoted fields and single-character delimiters. <p>
 *
 * This class does not perform trimming or type conversion and does not manage
 * multi-character delimiters. It is intended for simple CSV/TSV-style formats
 * where each line corresponds to a logical record.
 */
public abstract class LoadDelimitedFile {
    
    /**
     * Read text file, where each line uses "\\r\\n", and parse based on delimiter.
     * @param file
     * @param sep
     * @param bQuoted if column data could have quotes around it
     * @throws Exception
     */
    public void read(File file, char sep, boolean bQuoted) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (int i=1;;i++) {
            String line = reader.readLine();
            if (line == null) break;
            String[] ss = parse(line, sep, bQuoted, i);
            if (ss != null) process(ss, i);
        }
        reader.close();
    }
    
    
    // parse line into columns
    public String[] parse(String line, char sep, boolean bQuoted, int lineNumber) {
        String[] flds = OAString.parseLine(line, sep, bQuoted);
        return flds;
    }
        
    public abstract void process(String[] columns, int lineNumber);
}
    
    


