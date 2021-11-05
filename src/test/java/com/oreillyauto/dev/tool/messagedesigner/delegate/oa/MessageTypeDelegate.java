package com.oreillyauto.dev.tool.messagedesigner.delegate.oa;

import java.util.HashSet;

import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageGroup;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageRecord;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageTypeRecord;
import com.viaoa.util.OAString;

public class MessageTypeDelegate {

	public static String getPojoCode(final MessageType messageType) {

		StringBuilder sbProperties = new StringBuilder(1024 * 2);
		StringBuilder sbMethods = new StringBuilder(1024 * 4);

		if (messageType.getMessageRecords().size() == 1) {
			// return null;
		}

		HashSet<MessageGroup> hsMessageGroup = new HashSet();

		// properties
		for (MessageRecord mr : messageType.getMessageRecords()) {

			MessageTypeRecord rec = mr.getMessageTypeRecord();

			if (messageType.getMessageRecords().size() > 1 && (mr.getRelationshipType() == MessageRecord.RELATIONSHIPTYPE_One
					|| mr.getRelationshipType() == MessageRecord.RELATIONSHIPTYPE_ZeroOrOne)) {

				sbProperties.append(OAString.pad("", 4, true, ' '));
				sbProperties.append("protected " + OAString.mfcu(rec.getName()) + " " + OAString.mfcl(rec.getName()) + "; // code="
						+ rec.getCode() + "\n");

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

			MessageGroup grp = mr.getMessageGroup();
			if (grp != null) {
				if (hsMessageGroup.contains(grp)) {
					continue;
				}
				hsMessageGroup.add(grp);

				String s = null;
				for (MessageRecord mrx : grp.getMessageRecords()) {
					s = OAString.append(s, mrx.getMessageTypeRecord().getName() + "(" + mrx.getMessageTypeRecord().getCode() + ")", ", ");
				}
				s = String.format("  // Group: %s", s);

				sbProperties.append(OAString.pad("", 4, true, ' '));
				sbProperties.append("protected List<" + OAString.mfcu(grp.getName()) + "> list" + OAString.mfcu(grp.getName()) + "; " + s
						+ "\n");

				sbMethods.append("    \n");
				sbMethods.append("    public List<" + OAString.mfcu(grp.getName()) + "> get"
						+ OAString.mfcu(grp.getName()) + "List() {\n");

				sbMethods.append("        if (this.list" + OAString.mfcu(grp.getName()) + " == null) this.list"
						+ OAString.mfcu(grp.getName()) + " = new ArrayList();\n");

				sbMethods.append("        return this.list" + OAString.mfcu(grp.getName()) + ";\n");
				sbMethods.append("    }\n");

				sbMethods.append("    public void set" + OAString.mfcu(grp.getName()) + "List(List<" + OAString.mfcu(grp.getName())
						+ "> value) {\n");
				sbMethods.append("        this.list" + OAString.mfcu(grp.getName()) + " = value;\n");
				sbMethods.append("    }\n");

				continue;
			}

			sbProperties.append(OAString.pad("", 4, true, ' '));
			sbProperties
					.append("protected List<" + OAString.mfcu(rec.getName()) + "> list" + OAString.mfcu(rec.getName()) + "; // code="
							+ rec.getCode() + "\n");

			sbMethods.append("    \n");
			sbMethods.append("    public List<" + OAString.mfcu(rec.getName()) + "> get"
					+ OAString.mfcu(rec.getName()) + "List() {\n");
			sbMethods.append("        if (this.list" + OAString.mfcu(rec.getName()) + " == null) this.list" + OAString.mfcu(rec.getName())
					+ " = new ArrayList<"
					+ OAString.mfcu(rec.getName()) + ">();\n");
			sbMethods.append("        return this.list" + OAString.mfcu(rec.getName()) + ";\n");
			sbMethods.append("    }\n");

			sbMethods.append("    public void set" + OAString.mfcu(rec.getName()) + "List(List<" + OAString.mfcu(rec.getName())
					+ "> list) {\n");
			sbMethods.append("        this.list" + OAString.mfcu(rec.getName()) + " = list;\n");
			sbMethods.append("    }\n");
		}

		String className = OAString.mfcu(messageType.getCalcApiName());
		StringBuilder sb = new StringBuilder(1024 * 4);
		sb.append("public class " + OAString.mfcu(className) + " {\n");
		// sb.append("    // generated: " + (new OADateTime()) + "\n");
		sb.append("    public static final String MessageName = \"" + messageType.getName() + "\";\n\n");

		boolean b = false;
		if (messageType.getMessageRecords().size() < 2) {
			MessageRecord mr = messageType.getMessageRecords().get(0);
			if (mr == null) {
				sb.append("    // ERROR: no Records have been added to this Message Type\n");
			} else if (mr.getRelationshipType() == mr.RELATIONSHIPTYPE_One) {
				String code = mr.getMessageTypeRecord().getPojoCode();
				sb = new StringBuilder(code);
				sb.append("\n// this is the pojo for message record type: " + mr.getMessageTypeRecord().getCode() + " "
						+ mr.getMessageTypeRecord().getName() + "\n");
				b = true;
			}
		}
		if (!b) {
			sb.append(sbProperties.toString());
			sb.append(sbMethods.toString());
			sb.append("}\n");
		}
		return sb.toString();
	}
}
