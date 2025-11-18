/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import java.io.*;
import java.text.*;

import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Utility wrapper around Swing's {@link HTMLEditorKit} and
 * {@link HTMLDocument} that parses a fragment of HTML text and provides simple
 * inspection and manipulation helpers. The class supports obtaining the length
 * of the rendered text (excluding markup), inserting plain text at a document
 * position, and extracting a substring of the underlying HTML including the
 * associated markup. <p>
 *
 * The HTML is parsed lazily on first access using a default
 * {@link HTMLEditorKit}. All parsing errors are silently ignored and the
 * document is left uninitialized if parsing fails. Operations such as
 * {@link #insert(String, int)} and {@link #substring(int, int)} operate on the
 * internal {@link HTMLDocument} rather than the original raw HTML string, and
 * the class is not thread-safe. It is intended as a lightweight helper for
 * simple HTML manipulation tasks.
 */
public class OAHtml {
    private String htmlText;
    private boolean bInit;
    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private Reader reader;

    public OAHtml() {
        
    }
    
    public OAHtml(String htmlText) {
        setText(htmlText);
    }
    
    /**
     * Set html text
     */
    public void setText(String htmlText) {
        this.htmlText = htmlText;
        bInit = false;
    }

    /**
     * @return length of text, without markup tags.  Returns -1 if error.
     */
    public int getLength() {
        if (!init()) return -1;
        return doc.getLength();
    }
    /**
     * @return length of text, without markup tags.  Returns -1 if error.
     */
    public int length() {
        return getLength();
    }
    
    
    /**
     * 
     * @return html text
     */
    public String getText() {
        return htmlText;
    }
    
    private boolean init() {
        if (bInit || htmlText == null) return bInit;
        
        reader = new StringReader(htmlText);
        if (kit == null) kit = new HTMLEditorKit();

        doc = (HTMLDocument) kit.createDefaultDocument();
        
        try {
            kit.read(reader, doc, 0);
            bInit = true;
        }
        catch (Exception e) {
        }
        
        return bInit;
    }

    /**
     * Insert a string at a given position.  Use substring to get html document with the inserted text.
     * @param text text to insert, should not include tags.
     * @param pos
     */
    public void insert(String text, int pos) {
        if (!init()) return;
        try {
            doc.insertString(pos, text, null);
        }
        catch (Exception e) {
        }
    }
    
    
    /*
     * Get substring of html document text, which will then include html tags/attributes.
     * example: if the substring(1,3) for html doc '<html><body>abced<body><html>' will return '<html><body>bc<body><html>'
     */
    public String substring(int beginPos, int endPos) {
        if (!init()) return null;
        
        StringWriter w = new StringWriter(); 
        try {
            w = new StringWriter(); 
            kit.write(w, doc, beginPos+1, (endPos-beginPos)+1);
        }
        catch (Exception e) {
        }
        return w.toString();
    }
    
}
