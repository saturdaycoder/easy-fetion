package com.saturdaycoder.easyfetion;

import java.io.*;
import java.util.ArrayList;
public class SipcMessageParser extends SocketMessageParser
{
	public SocketMessage parse(InputStream is) throws IOException, java.net.SocketTimeoutException
	{	
		byte output[] = new byte[2048];
		String str = "";
		int len  = 0;
		
		boolean reparse = false;
		len = is.read(output);
		
		if (len == -1)
			return null;
		
		str = new String(output, 0, len, "UTF-8");
		SipcMessage resp1 = (SipcMessage)this.parse(str);
		String headerL = resp1.getHeaderValue("L");
		
		if (headerL == null) {
			return resp1;
		}	
		
		int totallen = Integer.parseInt(headerL);
		int headerlen = str.indexOf("\r\n\r\n") + 4;
				
		while (len < totallen + headerlen) {
			
			
			int rc = (totallen + headerlen - len > 2048)? 2048: (totallen + headerlen - len);
			int l = is.read(output, 0, rc);
			
			if (l == -1)
				break;
			
			str += new String(output, 0, l, "UTF-8");
			len += l;
			Debugger.info("0: len=" + len + "," + totallen);
			if (!reparse && len < totallen + headerlen)
				reparse = true;
			
		}
				
		if (reparse) 
		{
			
			SipcResponse resp2 = (SipcResponse)this.parse(str);
			Debugger.info("2: len=" + totallen + "," + (resp2.body.getBytes().length-headerlen));
			
			return resp2;
		}
		else {
			Debugger.info("1: len=" + totallen + "," + (resp1.body.getBytes().length-headerlen));
			return resp1;
		}
	}
	
	public SocketMessage parse(InputStream is, ArrayList<Byte> pendingBytes) 
			throws IOException, java.net.SocketTimeoutException
	{	
		// handle pending bytes first
		SipcMessage m = (SipcMessage)this.parse(pendingBytes);
		if (m != null) 
			return m;
		
		// error parsing SIpcMessage because of not enough bytes
		byte output[] = new byte[2048];
		
		do {
			int len = is.read(output);
			for (int i = 0; i < len; ++i)
				pendingBytes.add(output[i]);
			
			m = (SipcMessage)this.parse(pendingBytes);
		} while (m == null);
		
		return m;
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
	
	public SocketMessage parse(ArrayList<Byte> bytearray)
	{
		int consumedbytes = 0;
		byte[] b = new byte[bytearray.size()];
		for (int i = 0; i < bytearray.size(); ++i)
			b[i] = bytearray.get(i);
		String tmp = new String(b);
		SipcMessage msg = null;
		
		int cind = tmp.indexOf("\r\n");
		if (cind == -1)
			return null;
		
		String strcommand = tmp.substring(0, cind);
		
		if (strcommand.startsWith("SIP-C/4.0")) {
			msg = new SipcResponse();
			msg.setCmdLine(strcommand);
		}
		else if (strcommand.endsWith("SIP-C/4.0")){
			msg = new SipcCommand();
			msg.setCmdLine(strcommand);
		}
		else {
			Debugger.error("Unable to recognize such kind of message, or maybe not enough bytes:");
		//	Debugger.error(tmp);
			return null;
		}
		
		tmp = tmp.substring(tmp.indexOf("\r\n") + 2);
		consumedbytes += msg.cmdline.getBytes().length + 2;
		
		do {
			int ind = tmp.indexOf("\r\n");
			if (ind == -1)
				return null; // unable to find a complete header
			
			String strtmp = tmp.substring(0, ind);
			if (strtmp.equals(""))
				break; // found the end of headers
			
			msg.addHeader(strtmp.substring(0, strtmp.indexOf(':')), 
							strtmp.substring(strtmp.indexOf(": ") + 2));
			tmp = tmp.substring(tmp.indexOf("\r\n") + 2);
			
			consumedbytes += strtmp.getBytes().length + 2;
		} while (true);
		
		tmp = tmp.substring(tmp.indexOf("\r\n") + 2);
		consumedbytes += 2;
		
		String length = msg.getHeaderValue("L");
		if (length == null) {
			msg.body = "";
		}
		else {
			int l = Integer.parseInt(length);
			if (l > tmp.getBytes().length)
				return null;// not enough to compose a body
			
			// because of UTF-8 encoding, should not use substring() here
			msg.body = new String(b, consumedbytes, l);//tmp.substring(0, l);
			consumedbytes += l;
		}
		Debugger.debug("consumed " + consumedbytes + " bytes, remaining " 
				+ (bytearray.size()-consumedbytes));
		
		// remove consumedbytes from array
		for (int j = 0; j < consumedbytes; ++j) {
			bytearray.remove(0);
		}
		
		return msg;
	
	}
	
}
