package com.saturdaycoder.easyfetion;

public class SipcSubscribeCommand extends SipcCommand {
	public SipcSubscribeCommand(String sId)
	{
		setCmdLine("SUB fetion.com.cn SIP-C/4.0");
		addHeader("F", sId);
		addHeader("I", String.valueOf(generateCallId()));
		addHeader("Q", "1 SUB");
		addHeader("N", "PresenceV4");
		
		body = "<args><subscription self=\"v4default;mail-count\" buddy=\"v4default\" version=\"0\" /></args>";
		addHeader("L", String.valueOf(body.getBytes().length));
	}
}
