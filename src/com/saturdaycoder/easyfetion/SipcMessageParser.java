package com.saturdaycoder.easyfetion;

import java.io.*;
import java.util.ArrayList;
public class SipcMessageParser extends SocketMessageParser
{
	public SocketMessage parse(InputStream is) throws IOException, java.net.SocketTimeoutException
	{	
		//ArrayList<Byte> ba = new ArrayList<Byte>();
		byte output[] = new byte[2048];
		String str = "";
		int len  = 0;
		
		boolean reparse = false;
		len = is.read(output);
		str = new String(output, 0, len);
		SipcMessage resp1 = (SipcMessage)this.parse(str);
		String headerL = resp1.getHeaderValue("L");
		
		if (headerL == null) {
			return resp1;
		}	
		
		int totallen = Integer.parseInt(headerL);
		int headerlen = str.indexOf("\r\n\r\n") + 4;
		
		byte[] bodybytes = new byte[totallen];
		resp1.bodybytes = bodybytes;
		System.arraycopy(output, 0, bodybytes, 0, len-headerlen);
		while (len < totallen + headerlen) {
			
			int rc = (totallen + headerlen - len > 2048)? 2048: (totallen + headerlen - len);
			int l = is.read(output, 0, rc);
			if (l == -1)
				break;
			System.arraycopy(output, 0, bodybytes, len-headerlen, l);
			str += new String(output, 0, l);
			len += l;
			if (!reparse && len < totallen + headerlen)
				reparse = true;
		}
		if (reparse) 
		{
			SipcResponse resp2 = (SipcResponse)this.parse(str);
			resp2.bodybytes = bodybytes;
			return resp2;
		}
		else {
			return resp1;
		}
	}
	public SocketMessage parse(String str)
	{
		String tmp = str;
		SipcMessage msg;
		
		String strcommand = tmp.substring(0, tmp.indexOf("\r\n"));
		
		if (strcommand.startsWith("SIP-C/4.0")) {
			msg = new SipcResponse();
			msg.setCmdLine(strcommand);
		}
		else if (strcommand.endsWith("SIP-C/4.0")){
			msg = new SipcCommand();
			msg.setCmdLine(strcommand);
		}
		else {
			Debugger.error("Unable to recognize such kind of message:");
			Debugger.error(str);
			return null;
		}
		tmp = tmp.substring(tmp.indexOf("\r\n") + 2);
		
		do {
			String strtmp = tmp.substring(0, tmp.indexOf("\r\n"));
			if (strtmp.equals(""))
				break;
			
			msg.addHeader(strtmp.substring(0, strtmp.indexOf(':')), 
							strtmp.substring(strtmp.indexOf(": ") + 2));
			tmp = tmp.substring(tmp.indexOf("\r\n") + 2);
		} while (true);
		msg.body = tmp.substring(tmp.indexOf("\r\n") + 2);
		return msg;
	
	}
}
