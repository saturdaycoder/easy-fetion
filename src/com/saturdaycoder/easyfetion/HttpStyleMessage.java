package com.saturdaycoder.easyfetion;

import java.util.ArrayList;

public abstract class HttpStyleMessage extends SocketMessage
{
	public abstract int getContentLength();
	
	public class Header
	{
		String name;
		String value;
		public Header(String n, String v)
		{
			this.name = n;
			this.value = v;
		}
	}
	public String cmdline;
	public ArrayList<Header> headers;
	public String body = "";
	
	public HttpStyleMessage()
	{
		this.headers = new ArrayList<Header>();
	
	}
	
	public void setCmdLine(String cmdline)
	{
		this.cmdline = cmdline;
	}
	
	public void addHeader(String name, String value)
	{
		this.headers.add(new Header(name, value));
		//return this;
	}
	
	@Override
	public String toString()
	{
		String str = this.cmdline;
		str += "\r\n";
		
		for (int i = 0; i < this.headers.size(); ++i) 
		{
			str += this.headers.get(i).name + ": " + this.headers.get(i).value + "\r\n";
		}
		
		str += "\r\n";
		str += body;
		return str;
	}

	public String getHeaderValue(String key) 
	{
		for (int i = 0; i < this.headers.size(); ++i) 
		{
			if (this.headers.get(i).name.equals(key)) {
				return this.headers.get(i).value;
				
			}
			
		}
		return null;
	}
	
	public String getNthHeaderValue(String key, int n) 
	{
		int nn = 0;
		for (int i = 0; i < this.headers.size(); ++i) 
		{
			if (this.headers.get(i).name.equals(key)) {
				++nn;
				if (nn == n) {
					return this.headers.get(i).value;
				}
			}
			
		}
		return null;
	}
}
