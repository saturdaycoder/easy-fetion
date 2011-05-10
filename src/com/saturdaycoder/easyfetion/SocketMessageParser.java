package com.saturdaycoder.easyfetion;
import java.io.*;
public abstract class SocketMessageParser 
{
	public abstract SocketMessage parse(InputStream is) throws IOException, java.net.SocketTimeoutException;
	public abstract SocketMessage parse(String str);
}
