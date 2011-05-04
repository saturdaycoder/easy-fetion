package com.saturdaycoder.easyfetion;

public class SipcSendSmsCommand extends SipcCommand {
	public SipcSendSmsCommand(String sId, String contacturi, String msg) {
		setCmdLine("M fetion.com.cn SIP-C/4.0");
		addHeader("F", sId);
		addHeader("I", String.valueOf(generateCallId()));
		addHeader("Q", "2 M");
		addHeader("T", contacturi);
		addHeader("N", "SendCatSMS");
		
		body = msg;
		addHeader("L", String.valueOf(body.getBytes().length));
	}
}
