package com.viaoa.jaxb;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Marshaller.Listener;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;

import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAConv;
import com.viaoa.util.OANotExist;
import com.viaoa.util.OAString;

// see: https://howtodoinjava.com/jaxb/convert-json-to-java-object-moxy/

/**
 * Uses JAXB & Moxy to automate how OAObjects, Hubs and other java classes are converted to/from XML & Json.<br>
 * Java+OAObject+Hub <-> XML or JSON
 * <p>
 * OAObject classes can be generated to include extra methods (names with "jaxb") and annotations so that they work with JAXB and allow for
 * sending references as object, Id only, or as internal "ref" to another object in output.<br>
 * This allows for controlling which objects and references are included in the json output.<br>
 * Designed to handle "deep" graphs without circular references. <br>
 * <p>
 * OAObject class methods that are used by jaxb.<br>
 * One Links -<br>
 * 0: getEmployee will be annotated with @XmlTransient so that it is ignored by jaxb only one of the following will be used when writing
 * XML, others will return a null. All will be defined in xsd. Other systems will need to be able to use/process each.<br>
 * 1: getJaxbEmployee will return the json/xml for the full object, if shouldInclude=true and if the object is not already in the graph.<br>
 * 2: getJaxbRefEmployee - used only if the reference object has already been included, uses an @XmlIDREF.<br>
 * 3: getJaxbEmployeeId - used when the full object is not needed (shouldIncludeProperty=false, and it is not already in the graph.<br>
 * <p>
 * Many (Hub) Links -<br>
 * 0: getEmployees will be annotated with @XmlTransient so that it is ignored by jaxb<br>
 * 1: getJaxbEmployees will return a list<Station> of the stations that are not already in the graph.<br>
 * 2: getJaxbRefEmployees will return a list<Station> of the stations that are already in the graph.<br>
 * <p>
 * When the xml/json is unmarshalled, the Hub will have the combined objects.<br>
 * <p>
 * Note: if setUseReferences=false, then getJaxbRef will not be used and instead will always use the object.<br>
 * <p>
 * Example:<br>
 * <code>
 * OAJaxb jaxb = new OAJaxb<>(Company.class);
 * jaxb.setUseReferences(false);
 * jaxb.setIncludeGuids(false);
 * jaxb.addPropertyPath(CompanyPP.clients().products().pp);
 * jaxb.addPropertyPath(CompanyPP.locations().buyers().pp);
 * jsonOutput = jaxb.convertToJSON(hub);
 * </code>
 *
 * @see OAJson for working directory with Json object graphs, without loading into Java objects.
 * @author vvia
 */
public class OAJaxb<TYPE extends OAObject> {
	private OACascade cascade;
	private JAXBContext context;
	private Class<TYPE> clazz;

	// each object in the tree
	private Stack<Object> stackObject;

	// keeps hsCurrent for objs in stack
	private Stack<HashSet<String>> stackHashSet;

	// keeps a list of refs for the current object hub properties
	private Stack<HashMap<String, ArrayList<OAObject>>> stackHmRefsOnly;

	// list of link properties for the current object that have responded with SendRefType.object
	//    so that the other oaObj.getJaxb methods for the property will not be used.
	private HashSet<String> hsCurrentLinkObjectsSent;

	// keeps track objects in a Hub that are already in the output (obj graph)
	private HashMap<String, ArrayList<OAObject>> hmCurrentRefsOnly;

	private boolean bIsMarshelling;

	private boolean bUseReferences = true;

	private static HashMap<Class<OAObject>, JAXBContext> hmJAXBContext = new HashMap<>();

	private ArrayList<String> alPropertyPath = new ArrayList<>();

	public OAJaxb(Class<TYPE> c) {
		this.clazz = c;
	}

	public int getStackSize() {
		if (stackObject == null) {
			return 0;
		}
		return stackObject.size();
	}

	public void addPropertyPath(String pp) {
		alPropertyPath.add(pp);
	}

	public void clearPropertyPaths() {
		alPropertyPath.clear();
	}

	private boolean bIncludeGuids;

	public boolean getIncludeGuids() {
		return bIncludeGuids;
	}

	public void setIncludeGuids(boolean b) {
		this.bIncludeGuids = b;
	}

	/**
	 * Flag to know if references are permitted (default: true). References are used when an object is already in graph, and the reference
	 * will point to this object. If set to false, then an object that is included more then once will be repeated, causing duplicates for
	 * the object. Note: if false and a circular reference is detected, then a referece will be used anyway to avoid exception.
	 */
	public boolean getUseReferences() {
		return this.bUseReferences;
	}

	public void setUseReferences(boolean b) {
		this.bUseReferences = b;
	}

	/**
	 * Get the context for this.clazz
	 */
	public JAXBContext getJAXBContext() throws Exception {
		if (context != null) {
			return context;
		}

		/* Note: we are currently following the jaxb spec, where Id/RefId are treated as String.
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
			// hm.put(MOXySystemProperties.XML_ID_EXTENSION, Boolean.TRUE);  // ?????? dont think this is needed right now

			// create using Moxy Factory
			context = JAXBContextFactory.createContext(new Class[] { HubWrapper.class, clazz }, hm);
			boolean bx = (context instanceof org.eclipse.persistence.jaxb.JAXBContext);

			// default way for creating
			// context = JAXBContext.newInstance(HubWrapper.class, clazz);

			hmJAXBContext.put((Class<OAObject>) clazz, context);
		}
		return context;
	}

	protected void reset() {
		cascade = new OACascade();
		stackObject = new Stack<>();
		stackHashSet = new Stack<>();
		hsCurrentLinkObjectsSent = null;
		stackHmRefsOnly = new Stack<>();
		hmCurrentRefsOnly = null;
		stackMarshalLinkInfo = new Stack<MarshalInfo>();
		hsDontUpdateGuids = null;
	}

	public void createXsdFile(final String directoryName) throws Exception {
		getJAXBContext().generateSchema(new SchemaOutputResolver() {
			@Override
			public Result createOutput(String namespaceURI, String suggestedFileName) throws IOException {
				String dn;
				if (OAString.isNotEmpty(directoryName)) {
					dn = OAString.convertFileName(directoryName + "/");
				} else {
					dn = "";
				}

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
		String result = convertTo(obj, true);
		return result;
	}

	public String convertToXml(TYPE obj) throws Exception {
		String result = convertTo(obj, true);
		return result;
	}

	public String convertToJSON(TYPE obj) throws Exception {
		String result = convertTo(obj, false);
		return result;
	}

	public String convertToJson(TYPE obj) throws Exception {
		String result = convertTo(obj, false);
		return result;
	}

	public String convertTo(TYPE obj, boolean bToXML) throws Exception {
		if (obj == null) {
			return null;
		}
		StringWriter writer = new StringWriter();
		convert(obj, bToXML, writer);
		String result = writer.toString();
		return result;
	}

	public void saveAsXML(TYPE obj, File file) throws Exception {
		saveAs(obj, true, file);
	}

	public void saveAsXml(TYPE obj, File file) throws Exception {
		saveAs(obj, true, file);
	}

	public void saveAsJSON(TYPE obj, File file) throws Exception {
		saveAs(obj, false, file);
	}

	public void saveAsJson(TYPE obj, File file) throws Exception {
		saveAs(obj, false, file);
	}

	public void saveAs(TYPE obj, boolean bToXML, File file) throws Exception {
		Writer writer = new FileWriter(file);
		convert(obj, bToXML, writer);
		writer.close();
	}

	/**
	 * Main method for converting an object to a writer.
	 *
	 * @param bToXML if true then XML, else JSON
	 */
	public void convert(TYPE obj, boolean bToXML, Writer writer) throws Exception {
		try {
			reset();
			OAThreadLocalDelegate.setOAJaxb(this);
			bIsMarshelling = true;
			_convert(obj, bToXML, writer);
		} finally {
			bIsMarshelling = false;
			OAThreadLocalDelegate.setOAJaxb(null);
		}
	}

	protected void _convert(TYPE obj, boolean bToXML, Writer writer) throws Exception {
		if (obj == null || writer == null) {
			return;
		}
		Marshaller marshaller = getJAXBContext().createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setListener(new OAJaxbListener());

		if (!bToXML) {
			// Output JSON - Based on Object Graph
			marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
			marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
			marshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
		}

		marshaller.marshal(obj, writer);
	}

	public static String convertToJsonString(Object obj) throws Exception {
		if (obj == null) {
			return null;
		}

		HashMap hm = new HashMap<>();
		JAXBContext context = JAXBContextFactory.createContext(new Class[] { obj.getClass() }, hm);

		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
		marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
		marshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);

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
		String s = convertTo(hub, rootName, true);
		return s;
	}

	public String convertToXml(Hub<TYPE> hub, String rootName) throws Exception {
		String s = convertTo(hub, rootName, true);
		return s;
	}

	public String convertToJSON(Hub<TYPE> hub) throws Exception {
		String s = convertTo(hub, null, false);
		return s;
	}

	public String convertToJson(Hub<TYPE> hub) throws Exception {
		String s = convertTo(hub, null, false);
		return s;
	}

	public String convertTo(Hub<TYPE> hub, String rootName, boolean bToXML) throws Exception {
		StringWriter writer = new StringWriter();
		convert(hub, rootName, bToXML, writer);
		String result = writer.toString();
		return result;
	}

	public void saveAsXML(Hub<TYPE> hub, String rootName, File file) throws Exception {
		Writer writer = new FileWriter(file);
		convert(hub, rootName, true, writer);
	}

	public void saveAsXml(Hub<TYPE> hub, String rootName, File file) throws Exception {
		Writer writer = new FileWriter(file);
		convert(hub, rootName, true, writer);
	}

	public void saveAsJSON(Hub<TYPE> hub, File file) throws Exception {
		Writer writer = new FileWriter(file);
		convert(hub, null, false, writer);
	}

	public void saveAsJson(Hub<TYPE> hub, File file) throws Exception {
		Writer writer = new FileWriter(file);
		convert(hub, null, false, writer);
	}

	/**
	 * Main method for converting Hub.
	 */
	public void convert(Hub<TYPE> hub, String rootName, boolean bToXML, Writer writer) throws Exception {
		try {
			reset();
			OAThreadLocalDelegate.setOAJaxb(this);
			bIsMarshelling = true;
			for (OAObject obj : hub) {
				// put root objects in list of cascade so that inner references to them will be used
				cascade.wasCascaded(obj, true);
			}
			_convert(hub, rootName, bToXML, writer);
		} finally {
			bIsMarshelling = false;
			OAThreadLocalDelegate.setOAJaxb(null);
		}
	}

	protected void _convert(Hub<TYPE> hub, String rootName, boolean bToXML, Writer writer) throws Exception {
		Object obj;
		if (!bToXML || OAString.isEmpty(rootName)) {
			obj = hub;
		} else {
			HubWrapper<TYPE> wrapper = new HubWrapper<TYPE>(hub);
			JAXBElement jaxbElement = new JAXBElement(new QName(rootName), HubWrapper.class, wrapper);
			obj = jaxbElement;
		}

		Marshaller marshaller = getJAXBContext().createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setListener(new OAJaxbListener());

		if (!bToXML) {
			marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
			marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
			marshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
		}

		marshaller.marshal(obj, writer);
	}

	public TYPE convertFromXML(String xml) throws Exception {
		// OAThreadLocalDelegate.setLoading(true);
		OAThreadLocalDelegate.setOAJaxb(this);
		try {
			Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
			StringReader reader = new StringReader(xml);

			TYPE objx = (TYPE) unmarshaller.unmarshal(reader);
			return objx;
		} finally {
			// OAThreadLocalDelegate.setLoading(false);
			OAThreadLocalDelegate.setOAJaxb(null);
		}
	}

	/**
	 * Set what type of loading can be done.
	 */
	public static enum LoadingMode {
		/**
		 * Update existing objects or create new ones if there is not an existing.
		 */
		Default,

		/**
		 * Flag that will only allow a new Object and it's owned references to be created, and not to update existing objects. For example:
		 * an HTTP REST API POST method should only be used to create a new object.
		 */
		CreateNewRootOnly,

		/**
		 * Flag that will only allow the root Object(s) and it's owned references to be created, to ignore updating other objects included.
		 * For example: an HTTP REST API PUT method should only be used to update existing objects.
		 */
		UpdateRootOnly // including owned objects
	}

	private LoadingMode loadingMode;

	public LoadingMode getLoadingMode() {
		return loadingMode == null ? LoadingMode.Default : loadingMode;
	}

	public void setLoadingMode(LoadingMode lm) {
		this.loadingMode = lm;
	}

	/**
	 * Checks during loading into OAObjects to see if property should be loaded or ignored.
	 *
	 * @see OAObject#getAllowJaxbPropertyUpdate(String)
	 */
	public boolean getAllowJaxbPropertyUpdate(OAObject obj, String propertyName) {
		if (obj == null) {
			return false;
		}
		boolean b = hsCurrentLinkObjectsSent == null || !hsCurrentLinkObjectsSent.contains(obj.getGuid());

		if (b && getLoadingMode() == LoadingMode.CreateNewRootOnly && OAString.isNotEmpty(propertyName)) {
			// dont allow updating ID
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj);
			OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
			if (pi != null && pi.getId()) {
				b = false;
			}
		}

		return b;
	}

	/**
	 * Checks to see if property should be included in output. Default is true.
	 */
	public boolean getShouldInclude(OAObject obj, String propertyName) {
		return true;
	}

	//qqqqqqqqqqq
	//qqqqqq do same for loading Hub qqqqqqqqqqq
	//qqqqqqqqq do same for XML qqqqqqqqqqqqqqqqqqqqqqqqqq

	public void loadFromJSON(String json, TYPE objRoot) throws Exception {
		convertFromJSON(json, objRoot, true);
	}

	/**
	 */
	public TYPE convertFromJSON(String json) throws Exception {
		return convertFromJSON(json, null, true);
	}

	public TYPE convertFromJSON(String json, boolean bIncludeValidation) throws Exception {
		return convertFromJSON(json, null, bIncludeValidation);
	}

	public static Object convertJsonToGenericObject(String json, Class clazz) throws Exception {

		HashMap hm = new HashMap<>();
		JAXBContext context = JAXBContextFactory.createContext(new Class[] { clazz }, hm);

		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
		unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);

		StringReader reader = new StringReader(json);
		Source source = new StreamSource(reader);
		JAXBElement ele = unmarshaller.unmarshal(source, clazz);
		Object objx = ele.getValue();

		return objx;
	}

	public TYPE convertFromJSON(final String json, final TYPE objRoot) throws Exception {
		TYPE obj = convertFromJSON(json, objRoot, true);
		return obj;
	}

	public TYPE convertFromJson(final String json, final TYPE objRoot) throws Exception {
		TYPE obj = convertFromJSON(json, objRoot, true);
		return obj;
	}

	public TYPE convertFromJSON(final String json, final TYPE objRoot, final boolean bIncludeValidation) throws Exception {
		if (OAString.isEmpty(json)) {
			return null;
		}
		TYPE obj = convertFromJSON(objRoot, bIncludeValidation, new StringReader(json), new StringReader(json));
		return obj;
	}

	public TYPE convertFromJson(final String json, final TYPE objRoot, final boolean bIncludeValidation) throws Exception {
		TYPE obj = convertFromJSON(json, objRoot, bIncludeValidation);
		return obj;
	}

	public TYPE convertFromJSON(final TYPE objRoot, final boolean bIncludeValidation, File file) throws Exception {
		if (file == null) {
			return null;
		}
		TYPE obj = convertFromJSON(objRoot, bIncludeValidation, new FileReader(file), new FileReader(file));
		return obj;
	}

	public TYPE convertFromJson(final TYPE objRoot, final boolean bIncludeValidation, File file) throws Exception {
		return convertFromJSON(objRoot, bIncludeValidation, file);
	}

	/**
	 * Main method for converting JSON to object.
	 */
	public TYPE convertFromJSON(final TYPE objRoot, final boolean bIncludeValidation, Reader reader, Reader reader2) throws Exception {
		if (reader == null) {
			return objRoot;
		}

		boolean bUseIsLoading = false;
		TYPE objx = null;

		OAThreadLocalDelegate.setOAJaxb(this);
		try {
			preloadJSON(reader);
			reader = reader2;

			if (!queuePreloadNode.isEmpty()) {
				final Node node = queuePreloadNode.peek();
				if (objRoot != null) {
					node.oaObject = objRoot;
				}
			}

			if (!bIncludeValidation) {
				OAThreadLocalDelegate.setLoading(true);
				bUseIsLoading = true;
			}

			Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
			unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
			unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
			//qqqqqqqqqqqqqq NEW qqqqq
			unmarshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
			// unmarshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);

			/* Rules based on LoadingMode createNew & updateRootOnly
			  can only create new root and owned objects if createNew
			  dont allow updating of other objects
			  if they are new then reject (they should be created seperately)
			
			  only allow new (ignore/reject ID prop) for root and owned objects
			  other objects will not be updated,
			*/

			//qqqqqqqqqqqqqqqqqqqq see if first object (or collection is new object)

			if (getLoadingMode() == LoadingMode.UpdateRootOnly || getLoadingMode() == LoadingMode.CreateNewRootOnly) {
				int i = 0;
				for (Node node : queuePreloadNode) {
					Class cz = node.clazz;
					node.oaObject = getNextUnmarshalObject(node);
					if (i++ == 0) {
						if (node.oaObject == null) {
							OAObjectCallback eq = OAObjectCallbackDelegate.getAllowNewObjectCallback(cz);
							if (!eq.getAllowed()) {
								throw new Exception("User does not have permission to create new object, msg=" + eq.getResponse());
							}
						}
					}

					boolean bIsOwned = false;
					for (;;) {
						if (this.clazz.equals(cz)) {
							bIsOwned = true;
							break;
						}
						OAObjectInfo oi = OAObjectInfoDelegate.getObjectInfo(cz);
						OALinkInfo li = oi.getOwnedByOne();
						if (li == null) {
							break;
						}
						cz = li.getToClass();
					}
					node.bDisableUpdate = !bIsOwned; // dont allow updating real objects

					// check access
					if (node.oaObject == null) {
						if (getLoadingMode() == LoadingMode.UpdateRootOnly) {
							throw new Exception("can not create new Objects for " + node.clazz.getSimpleName() + ", id=" + node.id);
						} else if (!bIsOwned && getLoadingMode() == LoadingMode.CreateNewRootOnly) {
							throw new Exception("can not create new Objects for " + node.clazz.getSimpleName() + ", id=" + node.id);
						}

						if (bIsOwned) {
							OAObjectCallback eq = OAObjectCallbackDelegate.getAllowNewObjectCallback(cz);
							if (!eq.getAllowed()) {
								throw new Exception("User does not have permission to create new owned object, type=" + cz.getSimpleName()
										+ ", msg=" + eq.getResponse());
							}
						}
					}
				}
			}

			Source source = new StreamSource(reader);
			JAXBElement ele = unmarshaller.unmarshal(source, clazz);
			objx = (TYPE) ele.getValue();

		} finally {
			if (bUseIsLoading) {
				OAThreadLocalDelegate.setLoading(false);
			}
			OAThreadLocalDelegate.setOAJaxb(null);
		}
		return objx;
	}

	public Hub<TYPE> convertHubFromXML(String xml) throws Exception {
		//OAThreadLocalDelegate.setLoading(true);
		OAThreadLocalDelegate.setOAJaxb(this);
		try {
			Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();

			StreamSource streamSource = new StreamSource(new StringReader(xml));
			//qqqqqq need to setLoading
			/*was
			JAXBElement<HubWrapper> hubWrapper = unmarshaller.unmarshal(streamSource, HubWrapper.class);
			*/

			//qqqqqqqqqqqqqqqqqq get code from convertHubFromJSON ... call preload
			//qqqqqqqqqqqqqq test this with XML

			JAXBElement ele = unmarshaller.unmarshal(streamSource, clazz);
			List lst = (List) ele.getValue();
			Hub<TYPE> hub = new Hub<TYPE>(clazz);

			for (Object obj : lst) {
				//qqqqqqqqqqqqqqqq need to call:  OAObjectCacheDelegate.add(objNew);
				hub.add((TYPE) obj);
			}
			return hub;
		} finally {
			//OAThreadLocalDelegate.setLoading(false);
			OAThreadLocalDelegate.setOAJaxb(null);
		}
	}

	public Hub<TYPE> convertHubFromJSON(String xml) throws Exception {
		Hub<TYPE> hub = convertHubFromJSON(new StringReader(xml));
		return hub;
	}

	public Hub<TYPE> convertHubFromJson(String xml) throws Exception {
		Hub<TYPE> hub = convertHubFromJSON(new StringReader(xml));
		return hub;
	}

	public Hub<TYPE> convertHubFromJson(Reader reader) throws Exception {
		Hub<TYPE> hub = convertHubFromJSON(reader);
		return hub;
	}

	public Hub<TYPE> convertHubFromJSON(Reader reader) throws Exception {
		//qqqqqqqqq this needs to be an option, since POST will need to not use isLoading==true
		//qqq        OAThreadLocalDelegate.setLoading(true);
		OAThreadLocalDelegate.setOAJaxb(this);
		try {
			Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
			unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
			unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
			unmarshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);

			StreamSource streamSource = new StreamSource(reader);

			//qqqqqqqqqqq needs to check for boundaries on what's allowed to be changed qqqqqqq
			// preloadXml(xml); //qqqqqqqqqqqqqqqqqq

			JAXBElement ele = unmarshaller.unmarshal(streamSource, clazz);
			//            JAXBElement<HubWrapper> hubWrapper = unmarshaller.unmarshal(streamSource, HubWrapper.class);

			List lst = (List) ele.getValue();
			Hub<TYPE> hub = new Hub<TYPE>(clazz);

			for (Object obj : lst) {
				hub.add((TYPE) obj);
			}

			return hub;
		} finally {
			//qqqq            OAThreadLocalDelegate.setLoading(false);
			OAThreadLocalDelegate.setOAJaxb(null);
		}
	}

	// create nodes using preprocessor
	private static class Node {
		OALinkInfo li;
		String pp = "";
		Class clazz;
		Object id;
		OAPropertyInfo piId;
		boolean bIsArray;

		public Node(Class c) {
			this.clazz = c;
		}

		OAObject oaObject;
		boolean bDisableUpdate;
	}

	//qqqqqqqqqqqqq need to create a warning listener qqqqqqqqqqq otherwise it's silent

	// objects that should not have updates done to it.
	private HashSet<Integer> hsDontUpdateGuids;

	/** used from preload data, by static oaObject.jaxbCreate(), to get the next oaObject */
	public OAObject getNextUnmarshalObject(Class clazz) {
		if (clazz == null) {
			return null;
		}

		if (queuePreloadNode == null || queuePreloadNode.isEmpty()) {
			return null;
		}
		Node node = queuePreloadNode.peek();
		if (!node.clazz.equals(clazz)) {
			return null;
		}
		queuePreloadNode.remove();
		OAObject obj = getNextUnmarshalObject(node);
		if (node.bDisableUpdate) {
			if (hsDontUpdateGuids == null) {
				hsDontUpdateGuids = new HashSet<>();
			}
			hsDontUpdateGuids.add(obj.getGuid());
		}
		return obj;
	}

	protected OAObject getNextUnmarshalObject(Node node) {
		if (node.oaObject != null) {
			return node.oaObject;
		}

		Object id = node.id;
		if (id == null) {
			return null;
		}

		OAObjectKey objKey = new OAObjectKey(id);
		Object ref = OAObjectCacheDelegate.get(node.clazz, objKey);

		if (ref instanceof OAObject) {
			node.oaObject = (OAObject) ref;
			return (OAObject) ref;
		}
		//qqqqqqqqqqqqqqqq only if on server ... qqqqqqqqqqqqqqqq
		OASelect sel = new OASelect(node.clazz);
		sel.select(node.piId.getName() + " = ?", new Object[] { id });
		ref = sel.next();
		if (ref instanceof OAObject) {
			node.oaObject = (OAObject) ref;
			return (OAObject) ref;
		}
		return null;
	}

	// used by preload to know the order of objects that will be needed.
	private Queue<Node> queuePreloadNode;

	/**
	 * Need to find all of the Object IDs, so that jaxb createObject can then find the correct oaObj to update.
	 */
	protected void preloadJSON(Reader reader) {
		if (reader == null) {
			return;
		}
		final JsonParser parser = Json.createParser(reader);

		final Stack<Node> stackNode = new Stack();

		Class clazz = this.clazz;

		boolean bStartRootNode = true;

		queuePreloadNode = new ArrayDeque<>();

		Node node = null;
		String key = null;
		String value = null;
		while (parser.hasNext()) {
			final Event event = parser.next();

			if (event == Event.START_ARRAY) {
				if (bStartRootNode) {
					bStartRootNode = false;
					node = new Node(this.clazz);
					node.bIsArray = true;
					stackNode.push(node);
					//queuePreloadNode.add(node);
				} else {
					// find next clazz
					OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
					OALinkInfo li = oi.getLinkInfo(key);

					if (li != null) {
						clazz = li.getToClass();
						Node prev = node;
						node = new Node(clazz);
						node.bIsArray = true;
						node.li = li;
						if (OAString.isNotEmpty(prev.pp)) {
							node.pp = prev.pp + ".";
						}
						node.pp += li.getName();
						stackNode.push(node);
						// queuePreloadNode.add(node);
					} else {
						// array of refobjects
						Node prev = node;
						node = new Node(clazz);
						node.bIsArray = true;
						if (OAString.isNotEmpty(prev.pp)) {
							node.pp = prev.pp + ".";
						}
						node.pp += key;
						stackNode.push(node);
					}
				}
			} else if (event == Event.END_ARRAY) {
				node = stackNode.pop();
				key = null;
				if (stackNode.isEmpty()) {
					bStartRootNode = true;
				} else {
					node = stackNode.peek();
					clazz = node.clazz;
				}
			} else if (event == Event.START_OBJECT) {
				Node nodeArray = stackNode.isEmpty() ? null : stackNode.peek();
				if (nodeArray != null && !nodeArray.bIsArray) {
					nodeArray = null;
				}

				if (bStartRootNode) {
					bStartRootNode = false;
					node = new Node(this.clazz);
					stackNode.push(node);
					queuePreloadNode.add(node);
				} else if (nodeArray != null && nodeArray.li == null) {
					// outside array
					clazz = nodeArray.clazz;
					node = new Node(clazz);
					stackNode.push(node);
					// queuePreloadNode.add(node);
				} else if (nodeArray != null && nodeArray.li.getType() == OALinkInfo.TYPE_MANY) {
					// in an array
					clazz = nodeArray.clazz;
					node = new Node(clazz);
					node.li = nodeArray.li;
					node.pp = nodeArray.pp;

					stackNode.push(node);
					queuePreloadNode.add(node);
				} else if (key != null) { // Type_ONE
					// find next clazz
					OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
					OALinkInfo li = oi.getLinkInfo(key);

					if (li != null) {
						clazz = li.getToClass();
						Node prev = node;
						node = new Node(clazz);
						node.li = li;
						if (OAString.isNotEmpty(prev.pp)) {
							node.pp = prev.pp + ".";
						}
						node.pp += li.getName();
						stackNode.push(node);
						queuePreloadNode.add(node);
					} else {
						stackNode.push(new Node(clazz)); // dummy
					}
				}
			} else if (event == Event.END_OBJECT) {
				node = stackNode.pop();
				key = null;
				if (stackNode.isEmpty()) {
					bStartRootNode = true;
				} else {
					node = stackNode.peek();
					clazz = node.clazz;
				}
			} else if (event == Event.KEY_NAME) {
				key = parser.getString();
			} else if (event == Event.VALUE_STRING || event == Event.VALUE_NUMBER) {
				value = parser.getString();
				OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
				OAPropertyInfo pi = oi.getPropertyInfo(key);
				if (pi != null && pi.getId()) {
					value = parser.getString();
					node = stackNode.peek();
					Object objx = OAConv.convert(pi.getClassType(), value);
					node.id = objx;
					node.piId = pi;
				}
			} else {
				int xx = 4;
				xx++;
			}
		}
		parser.close();
	}

	public String getCurrentMarshalPropertyPath() {
		String pp = "";
		if (stackMarshalLinkInfo != null) {
			OALinkInfo liPrev = null;
			for (MarshalInfo mi : stackMarshalLinkInfo) {
				if (mi.li == liPrev) {
					continue; // recursive
				}
				if (pp.length() > 0) {
					pp += ".";
				}
				pp += mi.li.getName();
				liPrev = mi.li;
			}
		}
		return pp;
	}

	public boolean isInStack(OAObject obj) {
		if (stackObject == null) {
			return false;
		}
		for (Object objx : stackObject) {
			if (obj == objx) {
				return true;
			}
		}
		return false;
	}

	private OAObject lastGetSendRefObject;
	private String lastGetSendRefPropertyName;

	private boolean bIncludeOwned;

	public void setIncludeOwned(boolean b) {
		bIncludeOwned = b;
	}

	public boolean getIncludeOwned() {
		return bIncludeOwned;
	}

	private boolean bIncludeAll;

	public void setIncludeAll(boolean b) {
		bIncludeAll = b;
	}

	public boolean getIncludeAll() {
		return bIncludeAll;
	}

	/**
	 * Used by OAObject when serializing
	 */
	public SendRefType getSendRefType(final OAObject objThis, final String propertyName) {
		lastGetSendRefObject = objThis;
		lastGetSendRefPropertyName = propertyName;

		boolean bIsNeeded = getIncludeAll();
		boolean bMatchedPP = false;
		if (!bIsNeeded && alPropertyPath.size() > 0) {
			String ppCurrent = getCurrentMarshalPropertyPath();
			ppCurrent = OAString.concat(ppCurrent, propertyName, ".").toLowerCase();
			boolean bFound = false;
			for (String pp : alPropertyPath) {
				if (pp.toLowerCase().indexOf(ppCurrent) == 0) {
					bIsNeeded = true;
					bMatchedPP = true;
					break;
				}
			}
		}

		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(objThis);
		final OALinkInfo li = oi.getLinkInfo(propertyName);
		if (li == null) {
			return SendRefType.notNeeded;
		}

		Object objx = OAObjectPropertyDelegate.getProperty(objThis, propertyName, true, true);

		if (!bIsNeeded && getIncludeOwned() && li.getOwner()) {
			bIsNeeded = true;
		}

		if (!bIsNeeded && !shouldIncludeProperty(objThis, propertyName, false)) {
			if (li.getType() == li.MANY) {
				// if (objx instanceof Hub && ((Hub) objx).size() == 0) return SendRefType.object;
				return SendRefType.notNeeded;
			} else {
				if (objx instanceof OANotExist || objx == null) {
					return SendRefType.object; // send null
				}
				if (bUseReferences) {
					if (objx instanceof OAObject && cascade.wasCascaded((OAObject) objx, false)) {
						if (hsCurrentLinkObjectsSent != null && hsCurrentLinkObjectsSent.contains(propertyName.toUpperCase())) {
							return SendRefType.notNeeded;
						}
						return SendRefType.ref;
					}
				}
				return SendRefType.id;
			}
		}

		if (objx instanceof OANotExist) {
			if (li.getType() == li.ONE) {
				return SendRefType.object; // send null
			} else {
				return SendRefType.object; // send hub
			}
		}

		if (objx instanceof OAObjectKey) {
			Object objz = li.getValue(objThis);
			if (objz != null) {
				objx = objz;
			}
		}

		if (objx instanceof OAObject) {
			if (!bUseReferences) {
				if (!isInStack((OAObject) objx)) {
					if (shouldIncludeProperty(objThis, propertyName, true)) {
						if (hsCurrentLinkObjectsSent == null) {
							hsCurrentLinkObjectsSent = new HashSet<>();
						}
						hsCurrentLinkObjectsSent.add(propertyName.toUpperCase());
						return SendRefType.object;
					}
					return SendRefType.id;
				}
			}

			if (cascade != null) {
				if (cascade.wasCascaded((OAObject) objx, false)) {
					if (hsCurrentLinkObjectsSent != null && hsCurrentLinkObjectsSent.contains(propertyName.toUpperCase())) {
						return SendRefType.notNeeded;
					}
					return SendRefType.ref;
				}
			}
			if (shouldIncludeProperty(objThis, propertyName, true)) {
				if (hsCurrentLinkObjectsSent == null) {
					hsCurrentLinkObjectsSent = new HashSet<>();
				}
				hsCurrentLinkObjectsSent.add(propertyName.toUpperCase());
				return SendRefType.object;
			}
		}
		if (hsCurrentLinkObjectsSent != null && hsCurrentLinkObjectsSent.contains(propertyName.toUpperCase())) {
			return SendRefType.notNeeded;
		}
		if (objx == null) {
			return SendRefType.notNeeded;
		}
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
		if (prop == null) {
			return null;
		}
		if (hmCurrentRefsOnly == null) {
			return null;
		}
		ArrayList<OAObject> list = hmCurrentRefsOnly.get(prop.toUpperCase());
		return list;
	}

	public void setRefsOnlyList(String prop, ArrayList<OAObject> list) {
		if (prop == null || list == null) {
			return;
		}
		if (hmCurrentRefsOnly == null) {
			hmCurrentRefsOnly = new HashMap<>();
		}
		hmCurrentRefsOnly.put(prop.toUpperCase(), list);
	}

	/**
	 * call back used to determine is an object reference should be included. If false (default) then only the Id is included.
	 *
	 * @param objThis
	 * @param propertyName
	 * @return
	 */
	public boolean shouldIncludeProperty(final OAObject objThis, final String propertyName, final boolean bDefaultValue) {
		return bDefaultValue;
	}

	// stack of link objects from marshalling, that can be used to know the propertyPath
	private Stack<MarshalInfo> stackMarshalLinkInfo;

	private static class MarshalInfo {
		OALinkInfo li;
		OAObject lastGetSendRefObject;
		String lastGetSendRefPropertyName;

		MarshalInfo(OALinkInfo li, OAObject lastGetSendRefObject, String lastGetSendRefPropertyName) {
			this.li = li;
			this.lastGetSendRefObject = lastGetSendRefObject;
			this.lastGetSendRefPropertyName = lastGetSendRefPropertyName;
		}
	}

	class OAJaxbListener extends Listener {
		private final HashSet<String> hsDummy = new HashSet<>();
		private final HashMap<String, ArrayList<OAObject>> hmDummy = new HashMap<>();

		@Override
		public void beforeMarshal(Object source) {
			stackObject.push(source);

			if (lastGetSendRefObject != null && source instanceof OAObject) {
				OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(lastGetSendRefObject);
				OALinkInfo li = oi.getLinkInfo(lastGetSendRefPropertyName);
				if (li != null) {
					stackMarshalLinkInfo.push(new MarshalInfo(li, lastGetSendRefObject, lastGetSendRefPropertyName));
				}
			}

			stackHashSet.push(hsCurrentLinkObjectsSent != null ? hsCurrentLinkObjectsSent : hsDummy);
			hsCurrentLinkObjectsSent = null;

			stackHmRefsOnly.push(hmCurrentRefsOnly != null ? hmCurrentRefsOnly : hmDummy);
			hmCurrentRefsOnly = null;

			if (source instanceof OAObject) {
				cascade.wasCascaded((OAObject) source, true);
			} else if (source instanceof Hub) {
				cascade.wasCascaded((Hub) source, true);
			}
		}

		@Override
		public void afterMarshal(Object source) {
			if (stackMarshalLinkInfo.isEmpty()) {
				lastGetSendRefObject = null;
				lastGetSendRefPropertyName = null;
			} else {
				MarshalInfo mi = stackMarshalLinkInfo.pop();
				lastGetSendRefObject = mi.lastGetSendRefObject;
				lastGetSendRefPropertyName = mi.lastGetSendRefPropertyName;
			}

			stackObject.pop();
			hsCurrentLinkObjectsSent = stackHashSet.pop();
			if (hsCurrentLinkObjectsSent == hsDummy) {
				hsCurrentLinkObjectsSent = null;
			}
			hmCurrentRefsOnly = stackHmRefsOnly.pop();
			if (hmCurrentRefsOnly == hmDummy) {
				hmCurrentRefsOnly = null;
			}
		}
	}

	public enum SendRefType {
		notNeeded, object, ref, id;
	}

	public void setValidateSchema(String fname) {
		/*qqqqq todo:
		SchemaFactory sf = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
		Schema schema = sf.newSchema(new File("schema1.xsd"));
		m.setSchema(schema);
		*/
	}

	public boolean willBeIncludedLater(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		if (cascade.wasCascaded(oaObj, false)) {
			return false; // already included
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		OALinkInfo li = oi.getOwnedByOne();
		if (li == null) {
			return false;
		}

		Object objx = OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true);
		if (objx == null) {
			return false;
		}
		if (!(objx instanceof OAObject)) {
			return false;
		}

		if (cascade.wasCascaded((OAObject) objx, false)) {
			return false;
		}

		// parent has not yet been written
		// now need to find if a parent is already include
		if (isAnyOwnerAlreadyIncluded((OAObject) objx)) {
			return true;
		}

		return false;
	}

	private boolean isAnyOwnerAlreadyIncluded(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		OALinkInfo li = oi.getOwnedByOne();
		if (li == null) {
			return false;
		}

		Object objx = OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true);
		if (objx == null) {
			return false;
		}

		if (cascade.wasCascaded((OAObject) objx, false)) {
			return true;
		}

		return isAnyOwnerAlreadyIncluded((OAObject) objx);
	}

	//qqqqqqqqqqqqqqqqqqqqqqqq TEST with XML data ......qqqqqqqqqqqq  it might need to use HubWrapper qqqqqqqqqqqqqqqqqqqqq

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

	@XmlAnyElement(lax = true)
	public List<T> getList() {
		if (list == null) {
			list = new ArrayList<T>();
		}
		return list;
	}

}
