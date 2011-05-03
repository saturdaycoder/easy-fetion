package com.saturdaycoder.easyfetion;

public class SipcContactInfoCommand extends SipcCommand {
	public SipcContactInfoCommand(String sId, String uri)
	{
		setCmdLine("S fetion.com.cn SIP-C/4.0");
		addHeader("F", sId);
		addHeader("I", "1");
		addHeader("Q", "2 S");
		addHeader("N", "GetContactInfoV4");
		
		//addHeader("A", "Digest response=\"" + response + "\",algorithm=\"SHA1-sess-v4\"");
		//addHeader("AK", "ak-value");
		
		/*if (pv != null && !pv.algorithm.equals("")
				&& !pv.type.equals("")
				&& !pv.guid.equals("")
				&& !pv.code.equals("")) 
		{
			addHeader("A", "Verify response=\"" + pv.code 
					+ "\",algorithm=\"" + pv.algorithm 
					+ "\",type=\"" + pv.type 
					+ "\",chid=\"" + pv.guid + "\"");
		}*/
		
		body = "<args><contact uri=\"" + uri + "\"/></args>";
		addHeader("L", String.valueOf(body.getBytes().length));
	}
}
