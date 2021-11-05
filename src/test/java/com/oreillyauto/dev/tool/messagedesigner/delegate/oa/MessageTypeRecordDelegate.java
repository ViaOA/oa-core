package com.oreillyauto.dev.tool.messagedesigner.delegate.oa;

import java.io.BufferedReader;
import java.io.StringReader;

import com.oreillyauto.dev.tool.messagedesigner.delegate.ModelDelegate;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.JsonType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageRecord;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageSource;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageTypeColumn;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageTypeRecord;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.RpgType;
import com.viaoa.object.OAFinder;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

public class MessageTypeRecordDelegate {

	public static String getPojoCode(final MessageTypeRecord messageTypeRecord) {
		StringBuilder sbProperties = new StringBuilder(1024 * 2);
		StringBuilder sbMethods = new StringBuilder(1024 * 4);

		MessageRecord mr = messageTypeRecord.getMessageRecords().getAt(0);
		if (mr == null || mr.getRelationshipType() == MessageRecord.RELATIONSHIPTYPE_One) {
			// return null;
		}

		MessageSource messageSource = messageTypeRecord.getMessageSource();
		final boolean bIsJcomm = (messageSource.getSource() == MessageSource.SOURCE_Jcomm);

		for (MessageTypeColumn col : messageTypeRecord.getMessageTypeColumns()) {

			sbProperties.append(OAString.pad("", 4, true, ' '));

			RpgType rpgType = col.getRpgType();
			if (rpgType == null) {
				sbProperties.append("Invalid, no rpgType for column: " + OAString.mfcu(col.getName()) + "\n");
				continue;
			}
			JsonType jsonType = rpgType.getJsonType();
			if (jsonType == null) {
				sbProperties.append("Invalid, no jsonType for column: " + OAString.mfcu(col.getName()) + "\n");
				continue;
			}

			String s1 = "";
			if (!bIsJcomm && col.getFromPos() == 1 && col.getToPos() == 2 && col.getName().toLowerCase().indexOf("code") >= 0) {
				s1 = " = \"" + messageTypeRecord.getCode() + "\"";
			}

			String s2 = "protected " + col.getDefaultJavaClassType() + " " + OAString.mfcl(col.getName()) + s1 + ";";
			int max = s2.length();
			if (max < 45) {
				max = 45;
			}

			String s = String.format(	"%s, %d-%d, %s, %d", col.getRpgName(),
										col.getFromPos(), col.getToPos(),
										col.getRpgType().getName(), col.getDecimalPlaces());

			sbProperties.append(OAString.format(s2, max + " L") + "// "
					+ s + " - \"" + OAString.notNull(col.getDocType()) + "\"\n");

			sbMethods.append("    \n");
			s = null;
			if (col.getDecimalPlaces() > 0) {
				s = OAString.concat(s, "decimalPlaces = " + col.getDecimalPlaces(), ", ");
			}
			if (OAString.isNotEmpty(col.getFormat())) {
				s2 = col.getFormat();
				String s3 = col.getRpgType().getDefaultFormat();
				if (OAString.isNotEmpty(s3) && s3.equalsIgnoreCase(s2)) {
					s2 = s3;
				}
				s = OAString.concat(s, "format = \"" + s2 + "\"", ", ");
			}
			if (s != null) {
				sbMethods.append("    // @MessageInfo(" + s + ")\n");
			}

			sbMethods.append("    public " + col.getDefaultJavaClassType() + " get" + OAString.mfcu(col.getName()) + "() {\n");
			sbMethods.append("        return this." + OAString.mfcl(col.getName()) + ";\n");
			sbMethods.append("    }\n");

			sbMethods.append("    public void set" + OAString.mfcu(col.getName()) + "(" + col.getDefaultJavaClassType() + " "
					+ OAString.mfcl(col.getName()) + ") {\n");

			sbMethods.append("        this." + OAString.mfcl(col.getName()) + " = " + OAString.mfcl(col.getName()) + ";\n");
			sbMethods.append("    }\n");
		}

		if (bIsJcomm) {
			/*
			sbProperties.append("    protected String messageCode = \"" + messageTypeRecord.getCode() + "\";\n");

			sbMethods.append("    \n");
			sbMethods.append("    public String getMessageCode() {\n");
			sbMethods.append("        return this.messageCode;\n");
			sbMethods.append("    }\n");
			sbMethods.append("    public void setMessageCode(String messageCode) {\n");
			sbMethods.append("        // this.messageCode = messageCode;\n");
			sbMethods.append("    }\n");
			*/
		}

		String className = OAString.mfcu(messageTypeRecord.getName());
		if (OAString.isEmpty(className)) {
			className = "MessageCode" + messageTypeRecord.getCode();
		}

		StringBuilder sb = new StringBuilder(1024 * 4);
		sb.append("public class " + OAString.mfcu(className) + " {\n");
		// sb.append("    // last generated: " + (new OADateTime()) + "\n");
		sb.append("    public static final String MessageName = \"" + messageTypeRecord.getName() + "\"; //  messageCode="
				+ messageTypeRecord.getCode() + "\n");
		// sb.append("    public static final String MessageCode = \"" + messageTypeRecord.getCode() + "\";\n\n");

		sb.append(sbProperties.toString());
		sb.append(sbMethods.toString());
		sb.append("}\n");

		return sb.toString();

	}

	public static void convertLayoutToColumns(final MessageTypeRecord messageTypeRecord) {
		if (messageTypeRecord == null) {
			return;
		}

		messageTypeRecord.getMessageTypeColumns().deleteAll();

		String layout = messageTypeRecord.getLayout();
		if (OAString.isEmpty(layout)) {
			return;
		}

		StringReader sr = new StringReader(layout);
		BufferedReader br = new BufferedReader(sr);

		for (int i = 0;; i++) {
			String line = null;
			try {
				line = br.readLine();
			} catch (Exception e) {
			}
			if (line == null) {
				break;
			}
			try {
				convertLineToColumn(messageTypeRecord, line);
			} catch (Exception e) {
				//qqqqqqqqqq
			}
		}
	}

	public static void fixLayout(final MessageTypeRecord messageTypeRecord) {
		if (messageTypeRecord == null) {
			return;
		}
		String s = messageTypeRecord.getLayout();
		if (OAString.isEmpty(s)) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		StringBuilder sbMsg = new StringBuilder();

		StringReader sr = new StringReader(s);
		BufferedReader br = new BufferedReader(sr);

		for (int i = 0;; i++) {
			String line = null;
			try {
				line = br.readLine();
			} catch (Exception e) {
			}
			if (line == null) {
				break;
			}
			line = line.replace('\t', '|');

			// remove trailing '|'
			for (int j = line.length() - 1; j >= 0; j--) {
				char c = line.charAt(j);
				if (c == '|') {
					continue;
				}
				if (j < line.length() - 1) {
					line = line.substring(0, j + 1);
				}
				break;
			}

			/*
			Expected format:
			rpgName|fromPos|toPos|type|decimals|format|jsonName

			type="char,string,zone,packed"
			*/

			String[] ss = line.split("\\|");
			boolean bRebuild = false;
			if (ss.length > 4) {

				if (OAString.isEmpty(ss[0])) {
					sbMsg.append("line #" + (i + 1) + " no rpg name\n");
				}
				if (OAString.isEmpty(ss[1])) {
					sbMsg.append("line #" + (i + 1) + " no from pos\n");
				}
				if (OAString.isEmpty(ss[2])) {
					sbMsg.append("line #" + (i + 1) + " no to pos\n");
				}
				if (OAString.isEmpty(ss[3])) {
					sbMsg.append("line #" + (i + 1) + " type pos\n");
				}
				if (ss.length > 7) {
					sbMsg.append("line #" + (i + 1) + " columns.size=" + ss.length + "\n");
				}

				for (int j = 1; j < 3; j++) {
					s = ss[j];
					for (int k = 0; k < s.length(); k++) {
						char ch = s.charAt(k);
						if (!Character.isDigit(ch)) {
							s = OAString.field(ss[j], ch, 1);
							ss[j] = s;
							bRebuild = true;
							break;
						}
					}
				}

				// decimals [4]
				if (OAString.isNotEmpty(ss[4])) {
					s = OAString.notNull(OAString.field(ss[4], ',', 2));
					ss[4] = s.trim();
					bRebuild = true;
				}

				// type [3]
				if (OAString.isNotEmpty(ss[3])) {
					s = OAString.notNull(OAString.field(ss[3], '(', 2));
					if (OAString.isNotEmpty(s)) {
						s = OAString.notNull(OAString.field(s, ')', 1));
						if (OAString.isNotEmpty(s)) {
							s = OAString.notNull(OAString.field(s, ',', 2)).trim();
							if (OAString.isNotEmpty(s)) {
								ss[4] = s; // deci places
							}
						}
						s = OAString.notNull(OAString.field(ss[3], '(', 1));
						ss[3] = s.trim();
						bRebuild = true;
					}
				}

				bRebuild |= (ss.length > 7);

				if (bRebuild) {
					String newLine = "";
					for (int i2 = 0; i2 < Math.min(ss.length, 7); i2++) {
						if (i2 > 0) {
							newLine += "|";
						}
						newLine += ss[i2];
					}
					line = newLine;
				}
			}

			sb.append(line);
			sb.append("\n");
		}

		if (sbMsg.length() > 0) {
			sb.append(sbMsg.toString());
			sb.append("\n");
		}

		messageTypeRecord.setLayout(sb.toString());
	}

	/*
		Expected format:
		rpgName|fromPos|toPos|type|decimals|format|jsonName

		type="char,string,zone,packed"
	 */
	protected static void convertLineToColumn(final MessageTypeRecord messageTypeRecord, final String lineOrig) {
		if (messageTypeRecord == null) {
			return;
		}
		if (OAString.isEmpty(lineOrig)) {
			return;
		}

		final String line = lineOrig.replace('\t', '|');

		String[] ss = line.split("\\|");
		if (ss.length < 7) {
			return;
		}

		String s = ss[6];
		if (OAString.isEmpty(s)) {
			return;
		}
		if (s.toLowerCase().indexOf("not needed") >= 0) {
			return;
		}

		final String rpgName = ss[0];

		OAFinder<MessageTypeRecord, MessageTypeColumn> finder = new OAFinder<>(MessageTypeRecord.P_MessageTypeColumns);
		finder.addLikeFilter(MessageTypeColumn.P_RpgName, rpgName);
		MessageTypeColumn mc = finder.findFirst(messageTypeRecord);

		if (mc == null) {
			mc = new MessageTypeColumn();
			mc.setRpgName(rpgName);
			messageTypeRecord.getMessageTypeColumns().add(mc);
		}
		mc.setDocType(line);
		mc.setName(ss[6]);

		int fromPos = OAConv.toInt(ss[1]);
		mc.setFromPos(fromPos);

		int toPos = OAConv.toInt(ss[2]);
		mc.setToPos(toPos);

		mc.setSize((toPos - fromPos) + 1);

		if (OAString.isNotEmpty(ss[4])) {
			int x = OAConv.toInt(ss[4].trim());
			mc.setDecimalPlaces(x);
		}

		String fmt = null;
		if (OAString.isNotEmpty(ss[5])) {
			fmt = ss[5];
			mc.setFormat(ss[5]);
		}

		String type = ss[3];
		if (type == null) {
			type = "string";
		}
		type = type.toLowerCase();

		if (type.indexOf("string") >= 0 || type.indexOf("char") >= 0) {
			if (OAString.isNotEmpty(fmt)) {
				mc.setRpgType(findRpgType(RpgType.EncodeType.None, fmt));
			} else {
				mc.setRpgType(getStringRpgType());
			}
		} else if ((type.indexOf("zon") >= 0) || (type.indexOf("numeric") >= 0)) {
			mc.setRpgType(findRpgType(RpgType.EncodeType.ZonedDecimal, fmt));
		} else if (type.indexOf("pack") >= 0 || type.indexOf("deci") >= 0) {
			mc.setRpgType(findRpgType(RpgType.EncodeType.PackedDecimal, fmt));
		}

	}

	public static RpgType getStringRpgType() {
		for (RpgType rt : ModelDelegate.getRpgTypes()) {
			if (rt.getEncodeTypeEnum() != RpgType.EncodeType.None) {
				continue;
			}
			JsonType jt = rt.getJsonType();
			if (jt == null) {
				continue;
			}
			if (jt.getTypeEnum() != JsonType.Type.String) {
				continue;
			}
			return rt;
		}
		return null;
	}

	protected static RpgType findRpgType(RpgType.EncodeType encodeType, String fmt) {
		RpgType rpgType = null;
		for (RpgType rt : ModelDelegate.getRpgTypes()) {
			if (rt.getEncodeTypeEnum() != encodeType) {
				continue;
			}
			if (OAString.isEmpty(fmt)) {
				if (OAString.isNotEmpty(rt.getDefaultFormat())) {
					continue;
				}
			} else {
				if (!fmt.equalsIgnoreCase(rt.getDefaultFormat())) {
					continue;
				}
			}
			rpgType = rt;
			break;
		}
		return rpgType;
	}

}
