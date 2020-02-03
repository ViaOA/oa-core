package com.viaoa.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller.Listener;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MOXySystemProperties;

import com.viaoa.annotation.OAProperty;
import com.viaoa.ds.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;

/**
 * Uses JAXB & Moxy to automate how OAObjects, and Hubs are converted to/from XML & Json.
 * Java+OAobject+Hub  <->  XML or JSON
 *
 * OAObject references have extra methods and annotations so that they work with JAXB.
 * 
 * Allows for controlling which references are included.  Designed to handle "deep" graphs without circular references. 
 *  
 * There are added methods and annotations to OAObject link properties for JAXB to work.  These methods are named "*Jaxb*".
 * 
 * One Links - 
 * 0: getEmployee will be annotated with @XmlTransient so that it is ignored by jaxb
 *  * only one of the following will be used when writing XML, others will return a null. All will be defined in xsd.  Other systems will need to be able to use/process each.
 * 1: getJaxbEmployee will return the xml for the full object, if shouldInclude=true and if the object is not already in the graph.
 * 2: getJaxbRefEmployee - used only if the refence object has already been included, uses an @XmlIDREF. 
 * 3: getJaxbEmployeeId - used when the full object is not needed (shouldIncludeProperty=false, and it is not already in the graph.
 * 
 * Many (Hub) Linkes -
 * 0: getEmployees will be annotated with @XmlTransient so that it is ignored by jaxb
 * 1: getJaxbEmployees will return a list<Station> of the stations that are not already in the graph.
 * 2: getJaxbRefEmployees will return a list<Station> of the stations that are already in the graph.
 * when the xml/json is unmarshelled, the Hub will have the combined objects.  
 * @author vvia
 */
public class OAJaxb<TYPE extends OAObject> {
    private OACascade cascade;
    private JAXBContext context;
    private Class<TYPE> clazz;

    private Stack<Object> stackObject;
    private Stack<HashSet<String>> stackHashSet;
    private Stack<HashMap<String, ArrayList<OAObject>>> stackHmRefsOnly;

    private HashSet<String> hsCurrent;
    private HashMap<String, ArrayList<OAObject>> hmCurrentRefsOnly; 

    private boolean bIsMarshelling;
    
    private static HashMap<Class<OAObject>, JAXBContext> hmJAXBContext = new HashMap<>();
    
    public OAJaxb(Class<TYPE> c) {
        this.clazz = c;
    }

    public int getStackSize() {
        if (stackObject == null) return 0;
        return stackObject.size();
    }
    
    public JAXBContext getJAXBContext() throws Exception {
        if (context == null) {
            
            /*
                The JAXB spec says that XmlId must always be type string
                MOXY allows it to be otherwise, by doing one of the following:
                A: set system property at startup.  Has to before MOXySystemProperties is initialized, since it will store at classload 
                    System.setProperty(MOXySystemProperties.XML_ID_EXTENSION, "true"); 
                B: add this annotation of Id property 
                    @org.eclipse.persistence.oxm.annotations.XmlIDExtension

                https://www.eclipse.org/eclipselink/api/2.7/org/eclipse/persistence/jaxb/MOXySystemProperties.html
                https://stackoverflow.com/questions/29564627/does-moxy-support-non-string-xmlid-in-version-2-6-0        
             */
            
            context = hmJAXBContext.get(clazz);
            if (context == null) {

                // MOXY 
                HashMap hm = new HashMap<>();
                hm.put(MOXySystemProperties.XML_ID_EXTENSION, Boolean.TRUE);  // ?????? dont think this is needed
                context = JAXBContextFactory.createContext(new Class[] {clazz}, hm);
                //was: context = JAXBContextFactory.createContext(new Class[] {HubWrapper.class, clazz}, hm);
                
                boolean bx = (context instanceof org.eclipse.persistence.jaxb.JAXBContext);
                
                // default
                // context = JAXBContext.newInstance(HubWrapper.class, clazz); 
                hmJAXBContext.put((Class<OAObject>) clazz, context);
            }
        }
        return context;
    }

    protected void reset() {
        cascade = new OACascade();
        stackObject = new Stack<>();
        stackHashSet = new Stack<>();
        hsCurrent = null;
        stackHmRefsOnly = new Stack<>();
        hmCurrentRefsOnly = null;
    }
    
    public void createXsdFile(final String directoryName) throws Exception {
        getJAXBContext().generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceURI, String suggestedFileName) throws IOException {
                String dn;
                if (OAString.isNotEmpty(directoryName)) dn = OAString.convertFileName(directoryName + "/");
                else dn = "";
                
                File file = new File(dn + suggestedFileName);
                StreamResult result = new StreamResult(file);
                result.setSystemId(file.toURI().toURL().toString());
                return result;
            }
        });        
    }

    public boolean isMarshelling() {
        return bIsMarshelling;
    }

    public String convertToXML(TYPE obj) throws Exception {
        String s = convert(obj, true);
        return s;
    }
    public String convertToJSON(TYPE obj) throws Exception {
        String s = convert(obj, false);
        return s;
    }
    
    /**
     * Convert OAObject to XML or JSON.
     * @param bToXML if true then XML, else JSON
     */
    public String convert(TYPE obj, boolean bToXML) throws Exception {
        try {
            reset();
            OAThreadLocalDelegate.setOAJaxb(this);
            bIsMarshelling = true;
            return _convert(obj, bToXML);
        }
        finally {
            bIsMarshelling = false;
            OAThreadLocalDelegate.setOAJaxb(null);
        }
    }

    
    protected String _convert(TYPE obj, boolean bToXML) throws Exception {
        Marshaller marshaller = getJAXBContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setListener(new OAJaxbListener());

        if (!bToXML) {
            // Output JSON - Based on Object Graph
            marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
            marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
            marshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
        }
        
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(obj, stringWriter);
        String result = stringWriter.toString();
        return result;
    }
    
    /*qqqqqqqqqqqqqqqqqq    
    public String testJackson(TYPE obj) throws Exception {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);  // XmlElementWrapper is included in method annotations

        ObjectMapper objectMapper = new XmlMapper(xmlModule);
        
        objectMapper.registerModule(new JaxbAnnotationModule());
        
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME);  // did not allow inside name to be a duplicate
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        / *        
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector(objectMapper.getTypeFactory());
        objectMapper.setAnnotationIntrospector(introspector);
        * /        
        String xml = objectMapper.writeValueAsString(obj);
        return xml;
    }
    */    
    
    public String convertToXML(Hub<TYPE> hub, String rootName) throws Exception {
        String s = convert(hub, rootName, true);
        return s;
    }
    public String convertToJSON(Hub<TYPE> hub, String rootName) throws Exception {
        String s = convert(hub, rootName, false);
        return s;
    }
    
    
    public String convert(Hub<TYPE> hub, String rootName, boolean bToXML) throws Exception {
        try {
            reset();
            OAThreadLocalDelegate.setOAJaxb(this);
            bIsMarshelling = true;
            for (OAObject obj : hub) {
                cascade.wasCascaded(obj, true);
            }
            return _convert(hub, rootName, bToXML);
        }
        finally {
            bIsMarshelling = false;
            OAThreadLocalDelegate.setOAJaxb(null);
        }
    }
    
    protected String _convert(Hub<TYPE> hub, String rootName, boolean bToXML) throws Exception {
        // qqqqqqqqqq take out: 
        //reset();

        Object obj;
        if (OAString.isEmpty(rootName)) {
            obj = hub;
        }
        else {
            //HubWrapper<TYPE> wrapper = new HubWrapper<TYPE>(hub);
            //JAXBElement jaxbElement = new JAXBElement(new QName(rootName), HubWrapper.class, wrapper);
            //obj = jaxbElement;
            obj = hub;
        }
        
        Marshaller marshaller = getJAXBContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setListener(new OAJaxbListener());
        
        if (!bToXML) {
            // Output JSON - Based on Object Graph
            marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
            /*
            marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);
            marshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);         
             */
marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
marshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);         
        }

        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(obj, stringWriter);
        String result = stringWriter.toString();
        return result;
    }
    
    
    public TYPE convertFromXML(String xml) throws Exception {
        OAThreadLocalDelegate.setLoading(true);
        OAThreadLocalDelegate.setOAJaxb(this);
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            StringReader reader = new StringReader(xml);
            
            TYPE objx = (TYPE) unmarshaller.unmarshal(reader);
            return objx;
        }
        finally {
            OAThreadLocalDelegate.setLoading(false);
            OAThreadLocalDelegate.setOAJaxb(null);
        }
    }
    
    
    
    public TYPE convertFromJSON(String json) throws Exception {
        OAThreadLocalDelegate.setLoading(true);
        OAThreadLocalDelegate.setOAJaxb(this);
        
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
/*qqqqqqqqqqq            
            unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
            unmarshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);         
*/
unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
unmarshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);         
            
            
            StringReader reader = new StringReader(json);
            
            TYPE objx = (TYPE) unmarshaller.unmarshal(reader);
            return objx;
        }
        finally {
            OAThreadLocalDelegate.setLoading(false);
            OAThreadLocalDelegate.setOAJaxb(null);
        }
    }
    
    public Hub<TYPE> convertHubFromXML(String xml) throws Exception {
        OAThreadLocalDelegate.setLoading(true);
        OAThreadLocalDelegate.setOAJaxb(this);
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            
            StreamSource streamSource = new StreamSource(new StringReader(xml));
//qqqqqq need to setLoading            
            /*was
            JAXBElement<HubWrapper> hubWrapper = unmarshaller.unmarshal(streamSource, HubWrapper.class);
            */

JAXBElement ele = unmarshaller.unmarshal(streamSource, clazz);
List lst = (List) ele.getValue();
Hub<TYPE> hub = new Hub<TYPE>(clazz);
            
            
            for (Object obj : lst) {
                hub.add((TYPE) obj);
            }
            return hub;
        }
        finally {
            OAThreadLocalDelegate.setLoading(false);
            OAThreadLocalDelegate.setOAJaxb(null);
        }
    }
    public Hub<TYPE> convertHubFromJSON(String xml) throws Exception {
//qqqqqqqqq this needs to be an option, since POST will need to not use have Loading==true        
        OAThreadLocalDelegate.setLoading(true);
        OAThreadLocalDelegate.setOAJaxb(this);
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
            unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
            unmarshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);         
            
            StreamSource streamSource = new StreamSource(new StringReader(xml));
            
preload(xml); //qqqqqqqqqqqqqqqqqq



JAXBElement ele = unmarshaller.unmarshal(streamSource, clazz);
//            JAXBElement<HubWrapper> hubWrapper = unmarshaller.unmarshal(streamSource, HubWrapper.class);
            
            List lst = (List) ele.getValue();
            Hub<TYPE> hub = new Hub<TYPE>(clazz);

            for (Object obj : lst) {
                hub.add((TYPE) obj);
            }
            
            return hub;
        }
        finally {
            OAThreadLocalDelegate.setLoading(false);
            OAThreadLocalDelegate.setOAJaxb(null);
        }
    }

//qqqqqqqqqqqqqqqqqqqqqqqq    
    
    private static class Record {
        Class clazz;
        Object id;
        OAPropertyInfo pi;
        public Record(Class c) {
            this.clazz = c;
        }
    }
    
//qqqqqqqqqqqqq need to create a warning listener qqqqqqqqqqq otherwise it's silent    

    /** used from preload data, by static oaObject.jaxbCreate(), to get the next oaObject */
    public OAObject getNextObject(Class clazz) {
        if (clazz == null) return null;

        if (stackRecord == null || stackRecord.isEmpty()) return null;
        Record rec = stackRecord.peek();
        if (!rec.clazz.equals(clazz)) return null;
        stackRecord.pop();
        
        Object id = rec.id;
        if (id == null) return null;
        
        OAObjectKey objKey = new OAObjectKey(id);
        Object ref = OAObjectCacheDelegate.get(clazz, objKey);
        
        if (ref instanceof OAObject) {
            return (OAObject) ref;
        }
        
        OASelect sel = new OASelect(clazz);
        sel.select(rec.pi.getName() + " = ?",  new Object[] {id} );
        ref = sel.next();
        if (ref instanceof OAObject) {
            return (OAObject) ref;
        }
        return null;
    }
    
    
    // used by preload to know the order of objects that will be needed.
    private Stack<Record> stackRecord;
    /**
     * Need to find all of the Object IDs, so that jaxb createObject can then find the 
     * correct oaObj to update.
     */
    protected void preload(final String sz) {
        final JsonParser parser = Json.createParser(new StringReader(sz));
        
        final Stack<Record> stack = new Stack();
        
        Class clazz = this.clazz;
        Record rec = new Record(clazz);
        stack.push(rec);
        
        stackRecord = new Stack();
        stackRecord.add(rec);
        
        String key = null;
        String value = null;
        while (parser.hasNext()) {
             final Event event = parser.next();

             if (event == Event.START_OBJECT) {
                 if (key != null) {
                     // find next clazz
                     OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
                     OALinkInfo li = oi.getLinkInfo(key);
                     if (li != null) {
                         clazz = li.getToClass();
                         rec = new Record(clazz);
                         stack.push(rec);
                         stackRecord.add(rec);
                     }
                 }
             }                 
             else if (event == Event.END_OBJECT) {
                 rec = stack.pop();
                 key = null;
                 if (stack.isEmpty()) {
                     clazz = this.clazz;
                     rec = new Record(clazz);
                     stack.push(rec);
                     stackRecord.add(rec);
                 }
                 else {
                     rec = stack.peek();
                     clazz = rec.clazz;
                 }
             }                 
             else if (event == Event.KEY_NAME) {
                 key = parser.getString();
             }
             else if (event == Event.VALUE_STRING || event == Event.VALUE_NUMBER) {
                 value = parser.getString();
                 OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
                 OAPropertyInfo pi = oi.getPropertyInfo(key);
                 if (pi != null && pi.getId()) {
                     value = parser.getString();
                     rec = stack.peek();
                     Object objx = OAConv.convert(pi.getClassType(), value);
                     rec.id = objx;
                     rec.pi = pi;
                 }
             }
        }
        parser.close();
    }
    
    
    /**
     * Used by OAObject when serializing
     */
    public SendRefType getSendRefType(final OAObject objThis, final String propertyName) {
        Object objx = OAObjectPropertyDelegate.getProperty(objThis, propertyName, true, true);
/*
        String objName = objThis.getClass().getSimpleName();
int x = getStackSize();
maxStackSize = 5;
*/
/*
if (propertyName == null) return SendRefType.empty;          
if (propertyName.equalsIgnoreCase("AppUser")) return SendRefType.empty;
if (objName.equalsIgnoreCase("Buyer")) return SendRefType.empty;
if (propertyName.equalsIgnoreCase("")) return SendRefType.empty;
if (propertyName.equalsIgnoreCase("")) return SendRefType.empty;
*/
        if (objx instanceof OANotExist) return SendRefType.empty;

//qqqqqqqqqqqqqqqqqqqqqq        
//if (true || false) return SendRefType.object;


        if (objx instanceof OAObject) {
            if (cascade != null) {
                if (cascade.wasCascaded((OAObject) objx, false)) {
                    if (hsCurrent != null && hsCurrent.contains(propertyName.toUpperCase())) return SendRefType.empty;
                    return SendRefType.ref;
                }
            }
            if (shouldIncludeProperty(objThis, propertyName)) {
                if (hsCurrent == null) hsCurrent = new HashSet<>();
                hsCurrent.add(propertyName.toUpperCase());
                return SendRefType.object;
            }
        }
        if (hsCurrent != null && hsCurrent.contains(propertyName.toUpperCase())) return SendRefType.empty;
        return SendRefType.id;
    }

    /**
     * Used by OAObject when serializing
     */
    public boolean isAlreadyIncluded(OAObject obj) {
        if (cascade != null) {
            if (cascade.wasCascaded(obj, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Internally used by OAObject jaxb methods to keep track of a hub property refs that need to be sent, from method getJaxbRef[Name]s 
     */
    public ArrayList<OAObject> getRefsOnlyList(String prop) {
        if (prop == null) return null;
        if (hmCurrentRefsOnly == null) return null;
        ArrayList<OAObject> list =  hmCurrentRefsOnly.get(prop.toUpperCase());
        return list;
    }
    public void setRefsOnlyList(String prop, ArrayList<OAObject> list) {
        if (prop == null || list == null) return;
        if (hmCurrentRefsOnly == null) hmCurrentRefsOnly = new HashMap<>();
        hmCurrentRefsOnly.put(prop.toUpperCase(), list);
    }

    
    /**
     * call back used to determine is an object reference should be included.  If false (default) then only the Id is included.
     * @param objThis
     * @param propertyName
     * @return
     */
    public boolean shouldIncludeProperty(final OAObject objThis, final String propertyName) {
        int x = getStackSize();
        
        return x < maxStackSize;
    }
//qqqqqqqqqq    
int maxStackSize = 8;
    
    class OAJaxbListener extends Listener {
        private final HashSet<String> hsDummy = new HashSet<>();
        private final HashMap<String, ArrayList<OAObject>> hmDummy = new HashMap<>();
        @Override
        public void beforeMarshal(Object source) {
            stackObject.push(source);
            
            stackHashSet.push(hsCurrent != null ? hsCurrent : hsDummy);
            hsCurrent = null;

            stackHmRefsOnly.push(hmCurrentRefsOnly != null ? hmCurrentRefsOnly : hmDummy);
            hmCurrentRefsOnly = null;

            if (source instanceof OAObject) {
                cascade.wasCascaded((OAObject) source, true);
            }
            else if (source instanceof Hub) {
                cascade.wasCascaded((Hub) source, true);
            }
        }
        @Override
        public void afterMarshal(Object source) {
            stackObject.pop();
            hsCurrent = stackHashSet.pop();
            if (hsCurrent == hsDummy) hsCurrent = null;
            hmCurrentRefsOnly = stackHmRefsOnly.pop();
            if (hmCurrentRefsOnly == hmDummy) hmCurrentRefsOnly = null;
        }
    }

    public enum SendRefType {
        empty, object, ref, id;
    }


    public void setValidateSchema(String fname) {
/*qqqqq todo:        
        SchemaFactory sf = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        Schema schema = sf.newSchema(new File("schema1.xsd"));        
        m.setSchema(schema);
*/  
    }
    
    public boolean willBeIncludedLater(OAObject oaObj) {
        if (oaObj == null) return false;
        if (cascade.wasCascaded(oaObj, false)) return false;  // already included

        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
        OALinkInfo li = oi.getOwnedByOne();
        if (li == null) {
            return false;
        }
        
        Object objx = OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true);
        if (objx == null) return false;
        if (!(objx instanceof OAObject)) return false;
        
        if (cascade.wasCascaded((OAObject) objx, false)) return false;
        
        // parent has not yet been written
        // now need to find if a parent is already include
        if (isAnyOwnerAlreadyIncluded((OAObject) objx)) return true;
        
        return false;
    }
    private boolean isAnyOwnerAlreadyIncluded(OAObject oaObj) {
        if (oaObj == null) return false;

        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
        OALinkInfo li = oi.getOwnedByOne();
        if (li == null) return false;

        Object objx = OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true);
        if (objx == null) return false;
        
        if (cascade.wasCascaded((OAObject)objx, false)) return true;
        
        return isAnyOwnerAlreadyIncluded((OAObject)objx);
    }

    
    
}

/**
 * Wraps a Hub so that it can be used as a root element.
 */
class HubWrapper<T> {
    private List<T> list;

    public HubWrapper() {
    }
    
    public HubWrapper(List<T> lst) {
        this.list = lst;
    }
    
    @XmlAnyElement(lax=true)
    public List<T> getList() {
        if (list == null) list = new ArrayList<T>();
        return list;
    }

}


