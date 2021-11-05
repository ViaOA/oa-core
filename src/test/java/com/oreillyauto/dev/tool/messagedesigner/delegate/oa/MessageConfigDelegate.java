package com.oreillyauto.dev.tool.messagedesigner.delegate.oa;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oreillyauto.dev.tool.messagedesigner.delegate.ModelDelegate;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.JsonType;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.MessageConfig;
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
import com.viaoa.util.OAString;

public class MessageConfigDelegate {

	public static void saveAsJsonResource(MessageConfig messageConfig, String fileName) {

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

		for (MessageSource messageSource : ModelDelegate.getMessageSources()) {

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
							cd.setFromPos(col.getFromPos());
							cd.setToPos(col.getToPos());
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

			int xx = 4;
			xx++;
		} catch (Exception e) {
			System.out.println("Exception: " + e);
			e.printStackTrace();
			json = "Exception: " + e;
		}

	}

}
