package com.oreillyauto.dev.tool.messagedesigner.delegate.oa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oreillyauto.dev.tool.messagedesigner.delegate.ModelDelegate;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.JsonType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageGroup;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageRecord;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageSource;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageTypeColumn;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageTypeRecord;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.RpgType;
import com.oreillyauto.dev.tool.messagedesigner.model.pojo.ColumnDef;
import com.oreillyauto.dev.tool.messagedesigner.model.pojo.JsonTypeDef;
import com.oreillyauto.dev.tool.messagedesigner.model.pojo.MessageDef;
import com.oreillyauto.dev.tool.messagedesigner.model.pojo.MessageGroupDef;
import com.oreillyauto.dev.tool.messagedesigner.model.pojo.MessageRecordDef;
import com.oreillyauto.dev.tool.messagedesigner.model.pojo.RecordDef;
import com.oreillyauto.dev.tool.messagedesigner.model.pojo.RpgTypeDef;
import com.viaoa.datasource.OASelect;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

public class MessageSourceDelegate {

	public static void createOneMessageTypeForRecords(MessageSource messageSource) {

		// use this to run on server (remote)
		int cnt = 0;
		for (MessageTypeRecord rec : messageSource.getMessageTypeRecords()) {
			boolean b = false;
			for (MessageRecord mr : rec.getMessageRecords()) {
				MessageType mt = mr.getMessageType();
				if (mt == null) {
					continue;
				}
				if (mt.getMessageRecords().size() == 1 && mt.getMessageRecords().getAt(0).getMessageTypeRecord() == rec) {
					b = true;
					break;
				}
			}
			if (b) {
				continue;
			}

			MessageType mt = new MessageType();
			messageSource.getMessageTypes().add(mt);

			MessageRecord mr = new MessageRecord();
			mr.setMessageType(mt);
			mr.setMessageTypeRecord(rec);
			// mt.setName(rec.getName());
			mr.setRelationshipType(mr.RELATIONSHIPTYPE_One);

			messageSource.setConsole(String.format((++cnt) + " creatednew MessageType=%s for Record.code=%s", mt.getName(), rec.getCode()));
		}
	}

	// only needed once, dont run again
	public static void update_1(MessageSource messageSource) {
		int cnt = 0;
		for (MessageType mt : messageSource.getMessageTypes()) {
			/*
			for (MessageTypeRecord rec : mt.getMessageTypeRecords()) {
				MessageRecord mr = new MessageRecord();
				mr.setMessageType(mt);
				mr.setMessageTypeRecord(rec);
			}
			*/
		}
	}

	// only needed once, dont run again
	public static void update_2(MessageSource messageSource) {
		for (MessageType mt : messageSource.getMessageTypes()) {
			for (MessageRecord mr : mt.getMessageRecords()) {
				mr.setRelationshipType(mr.getRelationshipType());
			}
		}
	}

	public static void update(MessageSource messageSource) {
		// future code ...
	}

	public static String getApiCode(MessageSource messageSource) {
		StringBuilder sb = new StringBuilder(1024 * 4);
		sb.append("public interface " + messageSource.getSourceString() + "Api {\n");

		sb.append("    // last generated: " + (new OADateTime()) + "\n");

		sb.append("    public static final String MessageType = \"" + messageSource.getSourceString().toLowerCase() + "\";\n");

		for (MessageType mt : messageSource.getMessageTypes()) {
			String s = "";
			for (MessageRecord mr : mt.getMessageRecords()) {
				s = OAString.append(s, mr.getMessageTypeRecord().getCode(), ", ");
			}

			sb.append("    \n");
			sb.append("    // message codes: " + s + "\n");

			String paramName = mt.getCalcApiName();
			if (mt.getMessageRecords().size() == 1) {
				MessageRecord mr = mt.getMessageRecords().get(0);
				if (mr.getRelationshipType() == mr.RELATIONSHIPTYPE_One) {
					paramName = mr.getMessageTypeRecord().getName();
				}
			}

			if (messageSource.getSource() == MessageSource.SOURCE_Jcomm) {
				sb.append("    public boolean " + OAString.mfcl(mt.getCalcApiName()) + "(" + OAString.mfcu(paramName) + " "
						+ OAString.mfcl(paramName) + ");\n");
			} else {
				sb.append("    public void " + OAString.mfcl(mt.getCalcApiName()) + "(" + OAString.mfcu(paramName) + " "
						+ OAString.mfcl(paramName) + ");\n");
			}
		}

		sb.append("    \n");
		sb.append("    // \"Raw\" messages\n");
		if (messageSource.getSource() == MessageSource.SOURCE_Jcomm) {
			sb.append("    public boolean rawMessage(RawMessage rawMessage);\n");
		} else {
			sb.append("    public void rawMessage(RawMessage rawMessage);\n");
		}

		sb.append("}\n");
		return sb.toString();
	}

	public static void saveAsJsonResource(final MessageSource messageSource, final String fileName) {
		if (OAString.isEmpty(fileName)) {
			return;
		}

		final boolean bIsJcomm = messageSource.getSource() == MessageSource.SOURCE_Jcomm;
		int jcommColumnId = 70000;

		final ArrayList<JsonTypeDef> alJsonType = new ArrayList<>();
		final HashMap<Integer, JsonTypeDef> hsJsonType = new HashMap<>();

		for (JsonType jt : ModelDelegate.getJsonTypes()) {
			JsonTypeDef jtd = new JsonTypeDef();
			alJsonType.add(jtd);
			jtd.setId(jt.getId());
			jtd.setName(jt.getName());
			jtd.setType(jt.getType());
			hsJsonType.put(jt.getId(), jtd);
		}

		final ArrayList<RpgTypeDef> alRpgType = new ArrayList<>();
		final HashMap<Integer, RpgTypeDef> hsRpgType = new HashMap<>();

		for (RpgType rt : ModelDelegate.getRpgTypes()) {
			RpgTypeDef rtd = new RpgTypeDef();
			alRpgType.add(rtd);
			rtd.setId(rt.getId());
			rtd.setName(rt.getName());
			rtd.setEncodeType(rt.getEncodeType());
			rtd.setDefaultSize(rt.getDefaultSize());
			rtd.setDefaultFormat(rt.getDefaultFormat());
			rtd.setNullValueType(rt.getNullValueType());
			hsRpgType.put(rt.getId(), rtd);

			JsonType jt = rt.getJsonType();
			if (jt != null) {
				rtd.setJsonType(hsJsonType.get(jt.getId()));
			}
		}

		final ArrayList<MessageDef> alMessageDef = new ArrayList<>();

		final HashMap<MessageTypeRecord, RecordDef> hmMessageTypeRecord = new HashMap<>();

		for (MessageType mt : messageSource.getMessageTypes()) {
			MessageDef md = new MessageDef();
			alMessageDef.add(md);

			md.setId(mt.getId());
			md.setSource(messageSource.getSource());

			// Custom Fix - make name lowercase to match methodName in generator for MessageAPI.java
			md.setName(OAString.mfcl(mt.getName()));

			md.setDescription(mt.getDescription());
			md.setCommonColumnCount(mt.getCommonColumnCount());

			final HashMap<MessageRecord, MessageRecordDef> hmMessageRecordDef = new HashMap<>();

			messageSource.setConsole(mt.getName());

			for (MessageRecord mr : mt.getMessageRecords()) {
				MessageTypeRecord rec = mr.getMessageTypeRecord();

				RecordDef rd = hmMessageTypeRecord.get(rec);
				if (rd == null) {
					rd = new RecordDef();
					hmMessageTypeRecord.put(rec, rd);
					rd.setId(rec.getId());
					rd.setCode(rec.getCode());
					rd.setSubCode(rec.getSubCode());
					rd.setName(rec.getName());

					rd.setRepeatingCount(rec.getRepeatingCount());

					for (MessageTypeColumn col : rec.getMessageTypeColumns()) {
						ColumnDef cd = new ColumnDef();
						rd.getColumnDefs().add(cd);

						cd.setId(col.getId());
						cd.setName(col.getName());
						cd.setRpgName(col.getRpgName());
						cd.setKeyPos(col.getKeyPos());
						cd.setSpecialType(col.getSpecialType());

						int pos = col.getFromPos();
						cd.setFromPos(pos);
						pos = col.getToPos();
						cd.setToPos(pos);

						cd.setSize(col.getSize());
						cd.setDescription(col.getDescription());
						cd.setDecimalPlaces(col.getDecimalPlaces());

						// Custom Fix
						String s = col.getFormat();
						if ("YYYY-MM-DD-HH.MM.SS.mmmmmm".equalsIgnoreCase(s)) {
							s = ("yyyy-MM-dd-HH.mm.ss.SSSSSS");
						}
						cd.setFormat(s);

						cd.setNotUsed(col.getNotUsed());
						cd.setNullValueType(col.getNullValueType());

						RpgType rt = col.getRpgType();

						cd.setRpgType(hsRpgType.get(rt.getId()));
					}

					if (bIsJcomm) {
						/*
						// need to add an ending column for the messageCode
						ColumnDef cd = new ColumnDef();
						rd.getColumnDefs().add(cd);

						cd.setId(jcommColumnId++);
						cd.setName("messageCode");
						cd.setRpgName("MSGC");

						cd.setFromPos(154);
						cd.setToPos(156);

						cd.setSize(3);
						cd.setDescription("message code manually appended for JCOMM");

						RpgType rt = RpgTypeDelegate.getStringRpgType();

						cd.setRpgType(hsRpgType.get(rt.getId()));
						*/
					}
				}

				MessageRecordDef mrd = new MessageRecordDef();

				hmMessageRecordDef.put(mr, mrd);

				mrd.setId(mr.getId());
				mrd.setRecordDef(rd);
				mrd.setRelationshipType(mr.getRelationshipType());

				md.getMessageRecordDefs().add(mrd);
			}

			// MessageGroups
			for (MessageGroup mg : mt.getMessageGroups()) {
				MessageGroupDef mgd = new MessageGroupDef();
				md.getMessageGroupDefs().add(mgd);
				mgd.setId(mg.getId());
				mgd.setName(mg.getName());
				for (MessageRecord mr : mg.getMessageRecords()) {
					MessageRecordDef mrd = hmMessageRecordDef.get(mr);
					mgd.getMessageRecordDefs().add(mrd);
				}
			}

			// Custom Fix - assign methodName for non-compound messageDefs
			if (OAString.isEmpty(md.getName())) {
				if (md.getMessageRecordDefs().size() == 1) {
					RecordDef rd = md.getMessageRecordDefs().get(0).getRecordDef();
					md.setName(OAString.mfcl(rd.getName()));
				}
			}
		}
		String json;
		ObjectMapper om = new ObjectMapper();
		om.setSerializationInclusion(Include.NON_NULL);
		om.setDefaultPropertyInclusion(Include.NON_DEFAULT);

		try {
			om.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), alMessageDef);

			Object obj = om.readValue(new File(fileName), MessageDef[].class);

			List<MessageDef> alx = om.readValue(new File(fileName), new TypeReference<List<MessageDef>>() {
			});
		} catch (Exception e) {
			System.out.println("Exception: " + e);
			e.printStackTrace();
			json = "Exception: " + e;
		}
	}

	public static String formatForCsv(String text) {
		if (text == null) {
			return "";
		}
		text = OAString.convert(text, "\"", "\\\"");
		return text;
	}

	public static void saveAsCsvFile(final MessageSource messageSource, final String fileName) {
		try {
			_saveAsCsvFile(messageSource, fileName);
		} catch (Exception e) {
			System.out.println("Exception: " + e);
			e.printStackTrace();
		}
	}

	protected static void _saveAsCsvFile(final MessageSource messageSource, final String fileName) throws Exception {
		if (OAString.isEmpty(fileName)) {
			return;
		}

		OASelect<MessageTypeRecord> sel = new OASelect(MessageTypeRecord.class);
		sel.select("", MessageTypeRecord.P_Code);

		File file = new File(fileName);
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);

		String s = "RECORD_CODE, TABLE_TEXT, COLUMN_NAME, ORDINAL_POSITION, KEY_POSITION, COLUMN_HEADING, DATA_TYPE, LENGTH, NUMERIC_SCALE, STORAGE, RPG_FORMAT, JSON_TYPE, JSON_FORMAT";
		bw.write(s + "\n");

		/*
				Hub<MessageTypeRecord> hubMessageTypeRecord = new Hub(MessageTypeRecord.class);
		
				hubMessageTypeRecord.select(MessageTypeRecord.P_MessageSource + " = ?", messageSource, MessageTypeRecord.P_Code);
				hubMessageTypeRecord.sort(MessageTypeRecord.P_Code);
		*/

		for (MessageTypeRecord rec : sel) {
			//		for (MessageTypeRecord rec : hubMessageTypeRecord) {
			messageSource.setConsole("record code " + rec.getCode());

			int cnt = 1;
			for (MessageTypeColumn col : rec.getMessageTypeColumns()) {
				String line = "";
				line = OAString.append(line, formatForCsv(rec.getCode()), ", ");
				line = OAString.append(line, formatForCsv(rec.getName()), ", ");
				line = OAString.append(line, formatForCsv(col.getRpgName()), ", ");
				line = OAString.append(line, "" + cnt++, ", ");
				line = OAString.append(line, "" + col.getKeyPos(), ", ");
				line = OAString.append(line, formatForCsv(col.getName()), ", ");

				RpgType rt = col.getRpgType();
				if (rt != null) {
					s = rt.getEncodeTypeString();
					if ("none".equals(s)) {
						s = "Char";
					}
				} else {
					s = "";
				}
				line = OAString.append(line, formatForCsv(s), ", ");

				int x = (col.getToPos() - col.getFromPos()) + 1;
				line = OAString.append(line, "" + x, ", ");

				line = OAString.append(line, "" + col.getDecimalPlaces(), ", ");

				line = OAString.append(line, "", ", ");

				line = OAString.append(line, formatForCsv(col.getFormat()), ", ");

				if (rt != null) {
					s = rt.getJsonType().getName();
				} else {
					s = "";
				}
				line = OAString.append(line, formatForCsv(s), ", ");

				if (rt != null) {
					s = OAString.notNull(rt.getDefaultFormat());
				} else {
					s = "";
				}
				line = OAString.append(line, formatForCsv(s), ", ");
				bw.write(line + "\n");
			}
		}

		//qqqqqqqqqqqqq add combined message info qqqqqqqqqqqqqq

		bw.close();
	}

	public static String getControllerCode(MessageSource messageSource) {
		StringBuilder sb = new StringBuilder(1024 * 4);

		for (MessageType mt : messageSource.getMessageTypes()) {
			String ln = mt.getCalcApiName();
			if (mt.getMessageRecords().size() == 1) {
				MessageRecord mr = mt.getMessageRecords().get(0);
				if (mr.getRelationshipType() == mr.RELATIONSHIPTYPE_One) {
					ln = mr.getMessageTypeRecord().getName();
				}
			}
			ln = OAString.mfcl(ln);

			sb.append("    @PostMapping(path = \"/" + ln + "\", consumes = MediaType.APPLICATION_JSON_VALUE)\n");
			sb.append("    @Override\n");
			sb.append("    public void " + ln + "(@RequestBody " + OAString.mfcu(mt.getCalcApiName()) + " " + ln + ") {\n");
			sb.append("        getApi()." + ln + "(" + ln + ");\n");
			sb.append("    }\n\n");
		}

		return sb.toString();
	}

}
