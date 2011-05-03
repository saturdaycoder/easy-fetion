package com.saturdaycoder.easyfetion;
import java.util.*;
import java.io.*;
import android.util.Log;
public class SipcMessageParser extends SocketMessageParser
{
	//private static String TAG = "EasyFetion";
	
	
	
	public SocketMessage parse(InputStream is) 
	{	
		
		byte output[] = new byte[2048];
		String str = "";
		int len  = 0;
		
		try {
			boolean reparse = false;
			//Log.d(TAG, "SipcFactory start read");
			len = is.read(output);
			
			//Log.d(TAG, "SipcFactory read " + len + "bytes");
			str = new String(output, 0, len);
			SipcMessage resp1 = (SipcMessage)this.parse(str);
			//Log.d(TAG, "sipc parser read: \"" + str + "\", len = " + len);
			
			String headerL = resp1.getHeaderValue("L");
			
			if (headerL == null) {
				//Log.d(TAG, "no header L is read");
				return resp1;
			}
						
			int totallen = Integer.parseInt(headerL);
			int headerlen = str.indexOf("\r\n\r\n") + 4;
			
			while (len < totallen + headerlen) {
				
				int rc = (totallen + headerlen - len > 2048)? 2048: (totallen + headerlen - len);
				int l = is.read(output, 0, rc);
				
				str += new String(output, 0, l);
				len += l;
				if (!reparse && len < totallen + headerlen)
					reparse = true;
			}
			//Log.d(TAG, "len = " + len);
			
			if (reparse) 
			{
				return this.parse(str);
			}
			else {
				return resp1;
			}
			
			
		} catch (Exception e) {
			Log.e(TAG, "error parsing input stream");
			return null;
		}

	}
	public SocketMessage parse(String str) {
		String tmp = str;

		
		SipcMessage msg;
		
		// parse command line
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
