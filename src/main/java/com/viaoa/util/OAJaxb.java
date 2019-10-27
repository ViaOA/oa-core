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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller.Listener;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.jaxb.MarshallerProperties;

import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAThreadLocalDelegate;

/**
 * Uses JAXB & Moxy to automate how OAObjects, and Hubs are converted to/from XML & Json.
 * Java+OAobject+Hub  <->  XML or JSON
 *
 * OAObject references have extra methods and annotations so that they work with JAXB.
 * 
 * Allows for controlling which references are included.  Designed to handle "deep" graphs without circular references. 
 *  
 * There are methods added to OAObject link properties for JAXB to work.  These methods are named "*Jaxb*".
 * 
 * One Links - 
 * 0: getEmployee will be annotated with @XmlTransient so that it is ignored by jaxb
 *  * only one of the following will be used when writing XML, others will return a null. All will be defined in xsd.  Other systems will need to be able to use/process each.
 * 1: getJaxbEmployee will return the xml for the full object, if shouldInclude=true and if the object is not already in the graph.
 * 2: getJaxbRefEmployee - used only if the refence object has already been included, uses an @XmlIDREF. 
 * 3: getJaxbEmployeeId  - used when the full object is not needed (shouldIncludeProperty=false, and it is not already in the graph.
 * 
 * Many (Hub) Linkes -
 * 0: getEmployees will be annotated with @XmlTransient so that it is ignored by jaxb
 * 1: getJaxbStations will return a list<Station> of the stations that are not already in the graph.
 * 2: getJaxbRefStations will return a list<Station> of the stations that are already in the graph.
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
            context = hmJAXBContext.get(clazz);
            if (context == null) {
                context = JAXBContext.newInstance(HubWrapper.class, clazz); 
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
//qqqqqqqqqqqqqqqqqqqq was false            
            marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
            marshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);         
        }
        
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(obj, stringWriter);
        String xml = stringWriter.toString();
        return xml;
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
            OAThreadLocalDelegate.setOAJaxb(this);
            return _convert(hub, rootName, bToXML);
        }
        finally {
            OAThreadLocalDelegate.setOAJaxb(null);
        }
    }
    protected String _convert(Hub<TYPE> hub, String rootName, boolean bToXML) throws Exception {
        reset();

        HubWrapper<TYPE> wrapper = new HubWrapper<TYPE>(hub);
        JAXBElement jaxbElement = new JAXBElement(new QName(rootName), HubWrapper.class, wrapper);
        
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
        marshaller.marshal(jaxbElement, stringWriter);
        String xml = stringWriter.toString();
        return xml;
    }
    
    
    public TYPE convertFromXML(String xml) throws Exception {
        OAThreadLocalDelegate.setLoading(true);
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            StringReader reader = new StringReader(xml);
            
            TYPE objx = (TYPE) unmarshaller.unmarshal(reader);
            return objx;
        }
        finally {
            OAThreadLocalDelegate.setLoading(false);
        }
    }
    public TYPE convertFromJSON(String xml) throws Exception {
        OAThreadLocalDelegate.setLoading(true);
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
            unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
            unmarshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);         

            StringReader reader = new StringReader(xml);
            
            TYPE objx = (TYPE) unmarshaller.unmarshal(reader);
            return objx;
        }
        finally {
            OAThreadLocalDelegate.setLoading(false);
        }
    }
    
    public Hub<TYPE> convertHubFromXML(String xml) throws Exception {
        OAThreadLocalDelegate.setLoading(true);
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            
            StreamSource streamSource = new StreamSource(new StringReader(xml));
            
            JAXBElement<HubWrapper> hubWrapper = unmarshaller.unmarshal(streamSource, HubWrapper.class);
            Hub<TYPE> hub = new Hub<TYPE>(clazz);
            
            for (Object obj : hubWrapper.getValue().getList()) {
                hub.add((TYPE) obj);
            }
            return hub;
        }
        finally {
            OAThreadLocalDelegate.setLoading(false);
        }
    }
    public Hub<TYPE> convertHubFromJSON(String xml) throws Exception {
        OAThreadLocalDelegate.setLoading(true);
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
            unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
            unmarshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);         
            
            StreamSource streamSource = new StreamSource(new StringReader(xml));
            
            JAXBElement<HubWrapper> hubWrapper = unmarshaller.unmarshal(streamSource, HubWrapper.class);
            Hub<TYPE> hub = new Hub<TYPE>(clazz);
            
            for (Object obj : hubWrapper.getValue().getList()) {
                hub.add((TYPE) obj);
            }
            return hub;
        }
        finally {
            OAThreadLocalDelegate.setLoading(false);
        }
    }

    
    /**
     * Used by OAObject when serializing
     */
    public SendRefType getSendRefType(final OAObject objThis, final String propertyName) {
        Object objx = OAObjectPropertyDelegate.getProperty(objThis, propertyName, true, true);
        if (objx instanceof OANotExist) return SendRefType.empty;
        
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
        return false;
    }
    
    
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


