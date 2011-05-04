package com.saturdaycoder.easyfetion;

public class SipcContactInfoCommand extends SipcCommand {
	public SipcContactInfoCommand(String sId, String uri)
	{
		setCmdLine("S fetion.com.cn SIP-C/4.0");
		addHeader("F", sId);
		addHeader("I", String.valueOf(generateCallId()));
		addHeader("Q", "2 S");
		addHeader("N", "GetContactInfoV4");
		
		body = "<args><contact uri=\"" + uri + "\"/></args>";
		addHeader("L", String.valueOf(body.getBytes().length));
	}
}
