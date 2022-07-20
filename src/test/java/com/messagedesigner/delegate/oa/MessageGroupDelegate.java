package com.messagedesigner.delegate.oa;

import com.messagedesigner.model.oa.MessageGroup;
import com.messagedesigner.model.oa.MessageRecord;
import com.messagedesigner.model.oa.MessageTypeRecord;
import com.viaoa.util.OAString;

public class MessageGroupDelegate {

	public static String getPojoCode(final MessageGroup messageGroup) {
		StringBuilder sbProperties = new StringBuilder(1024 * 2);
		StringBuilder sbMethods = new StringBuilder(1024 * 4);

		for (MessageRecord mr : messageGroup.getMessageRecords()) {
			MessageTypeRecord rec = mr.getMessageTypeRecord();

			sbProperties.append(OAString.pad("", 4, true, ' '));
			sbProperties.append("protected " + OAString.mfcu(rec.getName()) + " " + OAString.mfcl(rec.getName()) + ";\n");

			sbMethods.append("    \n");
			sbMethods.append("    public " + OAString.mfcu(rec.getName()) + " get"
					+ OAString.mfcu(rec.getName()) + "() {\n");
			sbMethods.append("        return this." + OAString.mfcl(rec.getName()) + ";\n");
			sbMethods.append("    }\n");

			sbMethods.append("    public void set" + OAString.mfcu(rec.getName()) + "(" + OAString.mfcu(rec.getName())
					+ " value) {\n");
			sbMethods.append("        this." + OAString.mfcl(rec.getName()) + " = value;\n");
			sbMethods.append("    }\n");

			continue;
		}
		String className = OAString.mfcu(messageGroup.getName());
		StringBuilder sb = new StringBuilder(1024 * 4);
		sb.append("public class " + OAString.mfcu(className) + " {\n");
		// sb.append("    // generated: " + (new OADateTime()) + "\n");
		sb.append(sbProperties.toString());
		sb.append(sbMethods.toString());
		sb.append("}\n");

		return sb.toString();
	}
}
