package com.saturdaycoder.easyfetion;

public class SipcSendMsgCommand extends SipcCommand{
	public SipcSendMsgCommand(String sId, String contactUri, String msg)
	{
		setCmdLine("M fetion.com.cn SIP-C/4.0");
		addHeader("F", sId);
		addHeader("I", String.valueOf(generateCallId()));
		addHeader("Q", "2 M");
		addHeader("T", contactUri);
		addHeader("C", "text/plain");
		addHeader("K", "SaveHistory");
		addHeader("N", "CatMsg");
		
		body = msg;
		addHeader("L", String.valueOf(body.getBytes().length));
	}
}
