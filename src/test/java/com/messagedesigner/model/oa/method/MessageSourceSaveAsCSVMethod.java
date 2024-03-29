// Generated by OABuilder
package com.messagedesigner.model.oa.method;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.object.OAObject;

@OAClass(useDataSource = false, localOnly = true)
public class MessageSourceSaveAsCSVMethod extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(MessageSourceSaveAsCSVMethod.class.getName());
	public static final String P_CsvFileName = "CsvFileName";

	protected String csvFileName;

	public String getCsvFileName() {
		return csvFileName;
	}

	public void setCsvFileName(String newValue) {
		String old = csvFileName;
		fireBeforePropertyChange(P_CsvFileName, old, newValue);
		this.csvFileName = newValue;
		firePropertyChange(P_CsvFileName, old, this.csvFileName);
	}

	public void reset() {
		setCsvFileName(null);
	}
}
