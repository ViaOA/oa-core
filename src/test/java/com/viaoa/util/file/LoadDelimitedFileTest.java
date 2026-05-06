package com.viaoa.util.file;


import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.viaoa.OAUnitTest;
import com.viaoa.file.LoadDelimitedFile;

import test.xice.tsac3.model.oa.*;

public class LoadDelimitedFileTest extends OAUnitTest {

    @Test
    public void test() {
        
    }
    

    public static void main(String[] args) throws Exception {
        final ArrayList<String> alTable = new ArrayList<String>();
        alTable.add("ZITBAL");
        
        

        String s = "COLUMN_NAME,TABLE_NAME,TABLE_OWNER,ORDINAL_POSITION,DATA_TYPE,LENGTH,NUMERIC_SCALE,IS_NULLABLE,IS_UPDATABLE,LONG_COMMENT,"+
        "HAS_DEFAULT,COLUMN_HEADING,STORAGE,NUMERIC_PRECISION,CCSID,TABLE_SCHEMA,COLUMN_DEFAULT,CHARACTER_MAXIMUM_LENGTH,"+
        "CHARACTER_OCTET_LENGTH,NUMERIC_PRECISION_RADIX,DATETIME_PRECISION,COLUMN_TEXT,SYSTEM_COLUMN_NAME,"+
        "SYSTEM_TABLE_NAME,SYSTEM_TABLE_SCHEMA,USER_DEFINED_TYPE_SCHEMA,USER_DEFINED_TYPE_NAME,IS_IDENTITY,IDENTITY_GENERATION,"+
        "IDENTITY_START,IDENTITY_INCREMENT,IDENTITY_MINIMUM,IDENTITY_MAXIMUM,IDENTITY_CYCLE,IDENTITY_CACHE,IDENTITY_ORDER,"+
        "COLUMN_EXPRESSION,HIDDEN";

        /*                
        [0] column name
        [1] table name
        [4] dataType
        [5] length
        7-8 could be for key?
        [9] long comment
        [11] COLUMN_HEADING
        */                
        
        
        final ArrayList<String> alHeading = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(s, ",", false);
        for ( ;tok.hasMoreTokens(); ) {
            String word = tok.nextToken();
            alHeading.add(word);
        }
        
        
        LoadDelimitedFile ldf = new LoadDelimitedFile() {
            int cnt;
            @Override
            public void process(String[] columns, int lineNumber) {
                if (columns == null) return;
                int x = columns.length;
                if (x < 13) return;
                
                String s = columns[1];
                if (!alTable.contains(s)) return;
                
                System.out.println(lineNumber+"> "+(++cnt)+") "+columns[1]+", "+columns[0]+", "+columns[4]+", "+columns[5]+", "+columns[9]);
                /*
                int x2 = alHeading.size();
                for (int i=0; i<x2; i++) {
                    System.out.println("  "+i+") "+alHeading.get(i) + ") " + columns[i]);
                }
                */
            }
        };
        File file = new File("c:\\temp\\M3TABLES_Columns.csv"); 
//        ldf.read(file, ",", false);
    }
}

/*
OOHEAD - TF: Customer order
OCUSMA - MF: Customer
ZOHEAD - ?? not found
OCUSAD - MF: Customer address
OSYTXL - MF: Text, line
OOLINE - TF: Customer order lines
MWOHED - TF: Work order head
MITLOC - MF: Locations
MITBAL - MF: Warehouse/itemnumber stock and plan.valu2/(MB)
ZITBAL - ?? not found
MITFAC - MF: Facility/item balance and plan.value
MPDWCT - MF: Planning groups (workcenters)
MITMAS - MF: Items 
*/

/* 
108412 OCUSMA, OKCONO
  0) COLUMN_NAME) OKCONO
  1) TABLE_NAME) OCUSMA
  4) DATA_TYPE) DECIMAL
  5) LENGTH) 3
  6) NUMERIC_SCALE) 0
  7) IS_NULLABLE) N
  8) IS_UPDATABLE) Y
  9) LONG_COMMENT) Company
  21) COLUMN_TEXT) Company
*/


/* Sample
108412 OCUSMA, OKCONO
  0) COLUMN_NAME) OKCONO
  1) TABLE_NAME) OCUSMA
  2) TABLE_OWNER) M3SRVADM
  3) ORDINAL_POSITION) 1
  4) DATA_TYPE) DECIMAL
  5) LENGTH) 3
  6) NUMERIC_SCALE) 0
  7) IS_NULLABLE) N
  8) IS_UPDATABLE) Y
  9) LONG_COMMENT) Company
  10) HAS_DEFAULT) N
  11) COLUMN_HEADING) Cmp
  12) STORAGE) 2
  13) NUMERIC_PRECISION) 3
  14) CCSID) null
  15) TABLE_SCHEMA) M3DJDPRE
  16) COLUMN_DEFAULT) null
  17) CHARACTER_MAXIMUM_LENGTH) null
  18) CHARACTER_OCTET_LENGTH) null
  19) NUMERIC_PRECISION_RADIX) 10
  20) DATETIME_PRECISION) null
  21) COLUMN_TEXT) Company
  22) SYSTEM_COLUMN_NAME) OKCONO    
  23) SYSTEM_TABLE_NAME) OCUSMA    
  24) SYSTEM_TABLE_SCHEMA) M3DJDPRE  
  25) USER_DEFINED_TYPE_SCHEMA) null
  26) USER_DEFINED_TYPE_NAME) null
  27) IS_IDENTITY) NO
  28) IDENTITY_GENERATION) null
  29) IDENTITY_START) null
  30) IDENTITY_INCREMENT) null
  31) IDENTITY_MINIMUM) null
  32) IDENTITY_MAXIMUM) null
  33) IDENTITY_CYCLE) null
  34) IDENTITY_CACHE) null
  35) IDENTITY_ORDER) null
  36) COLUMN_EXPRESSION) null
  37) HIDDEN) N
*/


