package com.viaoa.remote.rest.info;

import java.util.ArrayList;
import java.util.List;

/**
 * Used by OARestClient to manage remote Java interfaces that can have their methods automatically invoke methods using HTTP(S) on another
 * server, either as a webserver, webservice, REST API, or as a Java2Java remote method call to the Implementation class on the server.
 * <p>
 * <p>
 * The OARestClassInfo is the metadata about a Java interface that has been annotated to allow OARestClient to create an instance of the
 * interface. <br>
 * OARestClient will then manage each method call to use HTTP(S) to invoke and get the return value from another server.
 * <p>
 *
 * @author vvia
 */
public class OARestClassInfo {

	public Class interfaceClass;
	public ArrayList<OARestMethodInfo> alMethodInfo = new ArrayList();
	public String contextName;

	public OARestClassInfo(Class clazz) {
		this.interfaceClass = clazz;
	}

	public ArrayList<String> verify() {
		ArrayList<String> alErrors = new ArrayList();

		for (OARestMethodInfo mi : alMethodInfo) {
			List<String> al = mi.verify();
			alErrors.addAll(al);
		}
		return alErrors;
	}

}
