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
package com.viaoa.xml;

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.viaoa.util.OAString;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParser;

/**
    XMLReader using a SAXParser to parse and load into a hierarchy of name/value, where value is a hashmap 
    or an ArrayList of values or hashmaps.
    
    NOTE: all name/values are put in uppercase
    see: SimpleXMLReaderTest
*/
public class SimpleXMLReader extends DefaultHandler {
    private Stack<Node> stack = new Stack<>();
    private Node node;

    static class Node {
        String name;
        String text;
        ArrayList<Node> alChildNode = new ArrayList<>();
        ArrayList<Attrib> alAttrib = new ArrayList<>();
        
        public Node(String name) {
            this.name = name;
        }
        
        public void add(Node node) {
            alChildNode.add(node);
        }
        public void add(Attrib attrib) {
            alAttrib.add(attrib);
        }
    }
    static class Attrib {
        String name;
        String value;
        Attrib(String name, String val) {
            this.value = val;
            this.name = name;
        }
    }
    
    public SimpleXMLReader() {
    }

    /**
     * @param xml text to parse.
     * @return hashmap of name/values, where values is either a HashMap or an ArrayList of HashMaps
     */
    public Node parse(String xml) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        StringReader sr = new StringReader(xml);
        InputSource is = new InputSource(sr);
        saxParser.parse(is, this);
        return node;
    }
    public Node parse(File file) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        
//qqqqqqqqq        
//        saxParser.setPreserveWhitespace(false);
        saxParser.parse(file, this);
        
        return node;
    }
    
    
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) throws SAXException {
        String eName = sName; // element name
        if ("".equals(eName)) eName = qName; // not namespaceAware

        Node nodePrev = node;
        
        node = new Node(eName);
        stack.push(node);
        if (nodePrev != null) {
            nodePrev.add(node);
            nodePrev.text = "";
        }

        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                String aName = attrs.getLocalName(i); // Attr name
                if ("".equals(aName)) aName = attrs.getQName(i);
                String aValue = attrs.getValue(i);
                Attrib attrib = new Attrib(aName, aValue);
                node.add(attrib);
            }
        }
    }

    public void endElement(String namespaceURI, String sName, String qName) throws SAXException {
        String eName = sName; // element name
        if ("".equals(eName)) eName = qName; // not namespaceAware
        
        node = stack.pop();
        if (stack.size() > 0) {
            node = stack.peek();
            node.text = "";
        }
    }
    
    public void characters(char buf[], int offset, int len) throws SAXException {
        String s = new String(buf, offset, len);
        if (node.alChildNode.size()==0) node.text = OAString.concat(node.text, OAString.decodeIllegalXML(s), "");
        else node.text = "";
    }

    
    public void ignorableWhitespace (char ch[], int start, int length) throws SAXException {
        int x = 4;
        x++;
    }


    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }
    
    public void display() {
        display(node, 0);
    }
    protected void display(Node node, final int indent) {
        if (node == null) return;
        String s = "";
        for (Attrib a : node.alAttrib) {
            s = OAString.append(s, a.name+"="+a.value, ", ");
        }
        System.out.println(OAString.indent(node.name+ (OAString.isEmpty(s)?"":s) + (OAString.isEmpty(node.text)?"":(" > \""+node.text+"\"")), indent));
        for (Node nodex : node.alChildNode) {
            display(nodex, indent+1);
        }
    }
    
    public static void main(String[] args) throws Exception {
        SimpleXMLReader sr = new SimpleXMLReader();
        File file = new File("C:\\Users\\vvia\\Documents\\HudsonMX\\TVB3.2\\schema\\test.xsd");
        //File file = new File("C:\\Users\\vvia\\Documents\\HudsonMX\\TVB3.2\\schema\\TVB_Common_3.2.xsd");
        sr.parse(file);
        sr.display();

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        URL xsdURL = sr.getClass().getResource("/xsd/abc.xsd");
        Schema schema = sf.newSchema(xsdURL);
/*qqq        
        Unmarshaller um = (getJAXBContext(NotificationReponseEnum.NOTIFICATION, notificationWrapper.getEnteteNotification().getTypeNotification()))
                .createUnmarshaller();
            um.setSchema(schema);        
*/        
        int xx = 4;
        xx++;
        
    }
    
}
