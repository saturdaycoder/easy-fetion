package com.saturdaycoder.easyfetion;

public class FetionHttpMessage extends HttpStyleMessage
{
	public byte[] bodybytes;
	public int getContentLength()
	{
		int c = -1;
		String s;
		if ((s = this.getHeaderValue("Content-Length")) != null) {
			c = Integer.parseInt(s);
		}
		return c;
	}
}
