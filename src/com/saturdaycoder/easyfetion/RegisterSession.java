package com.saturdaycoder.easyfetion;

import java.io.*;

public class RegisterSession {
	private SystemConfig sysConfig;
	private Crypto crypto;
	//private Socket sipcSocket;
	private InputStream is;
	private OutputStream os;
	public SipcResponse response;
	
	
	public RegisterSession(SystemConfig sysConfig, Crypto crypto,InputStream is,
			OutputStream os)//Socket sipcSocket)
	{
		this.sysConfig = sysConfig;
		this.crypto = crypto;
		this.is = is;
		this.os = os;
		//this.sipcSocket = sipcSocket;
	}
	
	public void send() throws IOException 
	{
		SipcRegisterCommand regMsg = new SipcRegisterCommand(sysConfig.sId,
				crypto.cnonce, SystemConfig.protocolVersion);
		Debugger.debug( "sent: " + regMsg.toString());
		os.write(regMsg.toString().getBytes());
	}
	
	public void read() throws IOException 
	{
		SipcMessageParser parser = new SipcMessageParser();
		SocketMessage m = parser.parse(is);
		if (m == null) {
			Debugger.error("can not read message from socket");
			throw new IOException ("null SIPC message");
		}
		this.response = (SipcResponse)m;
		Debugger.debug( "received: " + response.toString());
	}
	
	public void postprocess()
	{
		String str = response.getHeaderValue("W");
		
		String strnonce = str.substring(str.indexOf("nonce=\"") + 7);
		this.crypto.nonce = strnonce.substring(0, strnonce.indexOf('\"'));
		
		String strkey = str.substring(str.indexOf("key=\"") + 5);
		this.crypto.key = strkey.substring(0, strkey.indexOf('\"'));
		
		String strsignature = str.substring(str.indexOf("signature=\"") + 7);
		this.crypto.signature = strsignature.substring(0, strsignature.indexOf('\"'));
		
		Debugger.debug( "nonce = \"" + this.crypto.nonce + "\"");
		Debugger.debug( "key = \"" + this.crypto.key + "\"");
		Debugger.debug( "signature = \"" + this.crypto.signature + "\"");
	}
}
