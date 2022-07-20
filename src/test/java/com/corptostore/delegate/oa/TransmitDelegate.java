package com.corptostore.delegate.oa;

import com.corptostore.model.oa.Transmit;
import com.viaoa.util.OAString;

public class TransmitDelegate {

	public static String getAsJson(final Transmit transmit) {
		if (transmit == null) {
			return null;
		}

		byte[] rpgRecord = transmit.getRpgRecord();

		/*
		RpgJsonConverter rjc = new RpgJsonConverter(MessageSource.CorpToStore);
		rjc.appendJcommRecordCode(rpgRecord, transmit.getMessageCode() + "");

		String json = rjc.convertToJson(rpgRecord);
		*/
		String json = "not working";
		return json;
	}

	public static String getBinaryDisplay(final Transmit transmit) {
		if (transmit == null) {
			return "";
		}
		byte[] bs = transmit.getRpgRecord();
		if (bs == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder(600);
		sb.append("<html><tt><pre>");

		int max = bs == null ? 128 : bs.length;

		sb.append(OAString.getVerticalNumberLines(1, max));
		sb.append("</pre>");
		sb.append("<b><pre>");
		if (bs == null) {
			sb.append("binary is null");
		} else {
			sb.append(OAString.getVerticalHex(bs));
		}

		sb.append("\n\n");
		String s = "";//was: RpgJsonConverter.getString(bs, 0, bs.length);
		int x = s.length();
		for (int i = 0; i < x; i++) {
			char c = s.charAt(i);
			int ic = (int) c;
			if (c < 32) {
				sb.append('?');
			} else if (c > 127) {
				sb.append('?');
			} else if (c == ' ') {
				sb.append('_');
			} else {
				sb.append(c);
			}
		}
		// sb.append("[end]");
		sb.append("</pre></b></tt>");
		return sb.toString();
	}

}
