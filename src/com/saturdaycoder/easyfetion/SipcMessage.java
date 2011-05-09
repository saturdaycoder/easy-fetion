package com.saturdaycoder.easyfetion;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.*;
import java.io.*;
import android.util.Log;
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
