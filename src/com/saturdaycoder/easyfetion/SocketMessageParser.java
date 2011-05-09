package com.saturdaycoder.easyfetion;
import java.io.*;
public abstract class SocketMessageParser 
{
	protected static final String TAG = "EasyFetion";
	public abstract SocketMessage parse(InputStream is);
	public abstract SocketMessage parse(String str);
}
