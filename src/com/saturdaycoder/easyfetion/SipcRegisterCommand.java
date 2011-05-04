package com.saturdaycoder.easyfetion;

public class SipcRegisterCommand extends SipcCommand 
{
	public SipcRegisterCommand(String sId, String cnonce, String protocolVer)
	{
		setCmdLine("R fetion.com.cn SIP-C/4.0");
		addHeader("F", sId);
		addHeader("I", String.valueOf(generateCallId()));
		addHeader("Q", "2 R");
		addHeader("CN", cnonce);
		addHeader("CL", "type=\"pc\" ,version=\"" + protocolVer + "\"");
	}
}
