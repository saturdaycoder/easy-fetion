package com.saturdaycoder.easyfetion;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

public class FetionHttpMessageParser extends SocketMessageParser {
	public SocketMessage parse(InputStream is)
	{	
		
		byte output[] = new byte[2048];
		String str = "";
		int len  = 0;
		
		try {
			boolean reparse = false;
			//Log.d(TAG, "FetionHttpFactory start read");
			len = is.read(output);
			
			//Log.d(TAG, "SipcFactory read " + len + "bytes");
			str = new String(output, 0, len);
			FetionHttpMessage resp1 = (FetionHttpMessage)this.parse(str);
			String headerL = resp1.getHeaderValue("Content-Length");
			
			if (headerL == null) {
				Log.d(TAG, "no header Content-Length is read");
				return resp1;
			}
			
			int totallen = Integer.parseInt(headerL);
			int headerlen = str.indexOf("\r\n\r\n") + 4;
			
			while (len < totallen + headerlen) {
				int rc = (totallen + headerlen - len > 2048)? 2048: (totallen + headerlen - len);
				int l = is.read(output, 0, rc);
				
				str += new String(output, 0, l);
				len += l;
				if (!reparse)
					reparse = true;
			}
			Log.d(TAG, "len = " + len);
			
			if (reparse) 
			{
				return this.parse(str);
			}
			else 
				return resp1;
			
			
		} catch (Exception e) {
			Log.e(TAG, "error parsing input stream");
			return null;
		}

	}
	public SocketMessage parse(String str)
	{
		String tmp = str;

		
		FetionHttpMessage msg;
		
		// parse command line
		String strcommand = tmp.substring(0, tmp.indexOf("\r\n"));
		
		if (strcommand.startsWith("HTTP/1.1")) {
			msg = new FetionHttpResponse();
			msg.setCmdLine(strcommand);
		}
		else {//if (strcommand.endsWith("HTTP/1.1")){
			msg = new FetionHttpMessage();
			msg.setCmdLine(strcommand);
		}
		//else {
		//	return null;
		//}
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
