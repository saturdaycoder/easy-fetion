package com.saturdaycoder.easyfetion;

public class FetionLoadPortraitHttpRequest extends FetionHttpMessage{

	public FetionLoadPortraitHttpRequest (String portraitPath,
			String encodedSipuri, String encodedSsic, String server) {
		this.setCmdLine("GET " + portraitPath + "?Uri=" + encodedSipuri 
				+ "&Size=120&c=" + encodedSsic + " HTTP/1.1");
		this.addHeader("User-Agent", "IIC2.0/PC " + SystemConfig.protocolVersion);

		this.addHeader("Host", server);
		this.addHeader("Accept", "*/*");
		this.addHeader("Connection", "Keep-Alive");
		
		this.body = "";
	}
}
