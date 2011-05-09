package com.saturdaycoder.easyfetion;

public abstract class SipcMessage extends HttpStyleMessage 
{
	protected static String TAG = "EasyFetion";
	public int getContentLength()
	{
		int c = -1;
		String s;
		if ((s = this.getHeaderValue("L")) != null) {
			c = Integer.parseInt(s);
		}
		return c;
	}

}
