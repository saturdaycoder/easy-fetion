package com.saturdaycoder.easyfetion;

public abstract class SipcMessage extends HttpStyleMessage 
{
	public enum Type {
		TYPE_REQUEST,
		TYPE_RESPONSE,
		TYPE_UNKNOWN,
	}
	public byte[] bodybytes = null;
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
	public Type getType() {
		if (this.getClass().getName().contains("SipcCommand")) {
			return Type.TYPE_REQUEST;
		}
		else if (this.getClass().getName().contains("SipcResponse")) {
			return Type.TYPE_RESPONSE;
		}
		else {
			return Type.TYPE_UNKNOWN;
		}
	}
}
