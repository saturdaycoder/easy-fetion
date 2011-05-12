package com.saturdaycoder.easyfetion;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
public class FetionHttpMessageParser extends SocketMessageParser {
	public SocketMessage parse(InputStream is) throws IOException, SocketTimeoutException
	{	
		ArrayList<Byte> ba = new ArrayList<Byte>();
		//Debugger.e("Start parsing http response");
		byte output[] = new byte[2048];
		String str = "";
		int len  = 0;
		

		boolean reparse = false;
		
		len = is.read(output);

		
		str = new String(output, 0, len);
		FetionHttpMessage resp1 = (FetionHttpMessage)this.parse(str);
		String headerL = resp1.getHeaderValue("Content-Length");
		
		if (headerL == null) {
			//Debugger.e( "no header Content-Length is read");
			resp1.bodybytes = null;
			return resp1;
		}
		
		
		
		int totallen = Integer.parseInt(headerL);
		int headerlen = str.indexOf("\r\n\r\n") + 4;
		//Debugger.e( "header Content-Length is read: " + totallen);
		//Debugger.e( "header len: " + headerlen);
		
		/*byte[] tmp = new byte[len - headerlen];
		System.arraycopy(output, headerlen, tmp, 0, len - headerlen);
		java.math.BigInteger bi = new java.math.BigInteger(1, tmp);
	    String hex = String.format("%0" + (tmp.length << 1) + "X", bi);
		Debugger.e(hex);
	*/
		for (int k = headerlen; k < len; ++k) {
			ba.add(output[k]);
		}
		
		while (len < totallen + headerlen) {
			int rc = (totallen + headerlen - len > 2048)? 2048: (totallen + headerlen - len);
			int l = is.read(output, 0, rc);
			/*byte[] tmp1 = new byte[l];
			System.arraycopy(output, 0, tmp1, 0, l);
			java.math.BigInteger bi1 = new java.math.BigInteger(1, tmp1);
		    String hex1 = String.format("%0" + (tmp1.length << 1) + "X", bi1);
			Debugger.e(hex1);*/
			
			for (int k = 0; k < l; ++k) {
				ba.add(output[k]);
			}
			
			str += new String(output, 0, l);
			len += l;
			if (!reparse)
				reparse = true;
		}
		Debugger.d( "len = " + len);
		
		if (reparse) 
		{
			FetionHttpResponse resp2 = (FetionHttpResponse)this.parse(str);
			byte[] bb = new byte[ba.size()];
			for (int m = 0; m < bb.length; ++m) {
				bb[m] = ba.get(m);
			}
			resp2.bodybytes = bb;
			return resp2;
		}
		else 
			return resp1;
			

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
