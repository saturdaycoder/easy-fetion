package com.saturdaycoder.easyfetion;
import java.io.*;

public class SipcResponse extends SipcMessage{
    protected final static String SIPC_RESPONSE_CODE_INVITE_TRYING = "100";
    protected final static String SIPC_RESPONSE_CODE_OK = "200";
    protected final static String SIPC_RESPONSE_CODE_NOTEXIST = "404";
    protected final static String SIPC_RESPONSE_CODE_TEMPORARILYUNAVALIABLE = "480";
    protected final static String SIPC_RESPONSE_CODE_SENDSMS_OK = "280";
    protected final static String SIPC_RESPONSE_CODE_UNAUTHRIZED = "401";
    protected final static String SIPC_RESPONSE_CODE_NOSUBSCRIPTION = "522";
    
	public int getResponseCode()
	{
		String str = this.cmdline.substring(this.cmdline.indexOf(' ') + 1);
		str = str.substring(0, str.indexOf(' '));
		return Integer.parseInt(str);
	}

}
