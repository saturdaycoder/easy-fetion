package com.saturdaycoder.easyfetion;

public class SipcStartChatCommand extends SipcCommand {
	public SipcStartChatCommand(String sId) {
		setCmdLine("S fetion.com.cn SIP-C/4.0");
		addHeader("F", sId);
		addHeader("I", "1");
		addHeader("Q", "2 S");
		//addHeader("T", contacturi);
		addHeader("N", "StartChat");
		
		body = "";
		//addHeader("L", String.valueOf(body.getBytes().length));
	}
}
