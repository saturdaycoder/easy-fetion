package com.saturdaycoder.easyfetion;

public class SipcDropCommand extends SipcCommand {
	public SipcDropCommand(String sId) {
		setCmdLine("R fetion.com.cn SIP-C/4.0");
		addHeader("F", sId);
		addHeader("I", String.valueOf(generateCallId()));
		addHeader("Q", "1 R");
		
		addHeader("X", "0");
		
		body = "";
	}

}
