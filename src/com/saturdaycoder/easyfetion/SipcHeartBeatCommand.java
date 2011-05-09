package com.saturdaycoder.easyfetion;

public class SipcHeartBeatCommand extends SipcCommand{
	public SipcHeartBeatCommand(String sId) {
		setCmdLine("R fetion.com.cn SIP-C/4.0");
		addHeader("F", sId);
		addHeader("I", "1");
		addHeader("Q", String.valueOf(generateHeartBeat()) + " R");
		
		addHeader("N", "KeepAlive");

		body = "<args><credentials domains=\"fetion.com.cn;m161.com.cn;www.ikuwa.cn;games.fetion.com.cn\" /></args>";

		addHeader("L", String.valueOf(body.getBytes().length));
	}
	private int generateHeartBeat()
	{
		int h = 0;
		synchronized(SipcHeartBeatCommand.class){
			h = ++heartBeat;
		}
		return h;
	}
	private static int heartBeat = 0;
}
