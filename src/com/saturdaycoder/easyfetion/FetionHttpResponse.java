package com.saturdaycoder.easyfetion;

public class FetionHttpResponse extends FetionHttpMessage 
{
	public int getResponseCode()
	{
		String str = this.cmdline.substring(this.cmdline.indexOf(' ') + 1);
		str = str.substring(0, str.indexOf(' '));
		return Integer.parseInt(str);
	}
}
