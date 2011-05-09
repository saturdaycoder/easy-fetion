package com.saturdaycoder.easyfetion;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import java.io.*;
import org.xmlpull.v1.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.util.Log;

public class SipcAuthenticateCommand extends SipcCommand 
{
	private static final String TAG = "EasyFetion";
	
	public SipcAuthenticateCommand (SystemConfig sc, String response, 
									FetionPictureVerification pv)
	{
		setCmdLine("R fetion.com.cn SIP-C/4.0");
		addHeader("F", sc.sId);
		addHeader("I", String.valueOf(generateCallId()));
		addHeader("Q", "2 R");
		
		addHeader("A", "Digest response=\"" + response + "\",algorithm=\"SHA1-sess-v4\"");
		addHeader("AK", "ak-value");
		
		if (pv != null && !pv.algorithm.equals("")
				&& !pv.type.equals("")
				&& !pv.guid.equals("")
				&& !pv.code.equals("")) 
		{
			addHeader("A", "Verify response=\"" + pv.code 
					+ "\",algorithm=\"" + pv.algorithm 
					+ "\",type=\"" + pv.type 
					+ "\",chid=\"" + pv.guid + "\"");
		}
		
		body = generateAuthBody(sc);
		addHeader("L", String.valueOf(body.getBytes().length));
	}
	
	private String generateAuthBody(SystemConfig sc)
	{
		try 
		{
			StringWriter sw = new StringWriter();
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();  
	        XmlSerializer xs = factory.newSerializer();  
	        
	        xs.setOutput(sw);
	        
	        xs.startDocument("utf-8", true); 
			
	        xs.startTag(null, "args");
			
	        xs.startTag(null, "device");
	        
	        String machinecode = "";
	        try {
	        	machinecode = Network.getWifiMacAddr();
	        } catch (Exception e) {
	        	Debugger.e( "error getting WIFI MAC address: " + e.getMessage());
	        }
	        
	        if (machinecode == null || machinecode.equals("")) {
	        	try {
	        		machinecode = Network.getDeviceId();
	        	} catch (Exception e) {
	        		Debugger.e( "error getting Device ID: " + e.getMessage());
	        	}
	        } 
	        
	        if (machinecode == null || machinecode.equals("")) {
	        	machinecode = "FFFFFFFFFFFF";
	        }
	        
	        xs.attribute(null, "machine-code", machinecode);
	        xs.endTag(null, "device");

	        xs.startTag(null, "caps");
	        xs.attribute(null, "value", "1ff");
	        xs.endTag(null, "caps");

	        xs.startTag(null, "events");
	        xs.attribute(null, "value", "7f");
	        xs.endTag(null, "events");

	        xs.startTag(null, "user-info");
	        xs.attribute(null, "mobile-no", sc.mobileNumber);
	        xs.attribute(null, "user-id", sc.userId);

	        xs.startTag(null, "personal");
	        xs.attribute(null, "version", sc.personalVersion);
	        xs.attribute(null, "attributes", "v4default");
	        xs.endTag(null, "personal");

	        xs.startTag(null, "custom-config");
	        xs.attribute(null, "version", sc.customConfigVersion);
	        xs.endTag(null, "custom-config");

	        xs.startTag(null, "contact-list");
	        xs.attribute(null, "version", sc.contactVersion);
	        xs.attribute(null, "buddy-attributes", "v4default");
	        xs.endTag(null, "contact-list");
			
			xs.endTag(null, "user-info");

			xs.startTag(null, "presence");
			xs.startTag(null, "basic");
			xs.attribute(null, "value", String.valueOf(sc.state));
			xs.attribute(null, "desc", "");
			xs.endTag(null, "basic");
			xs.endTag(null, "presence");
			
			xs.endTag(null, "args");
			
			xs.endDocument();
			
			return sw.toString();
		} catch (Exception e) {
			Debugger.e( "error generating auth xml: " + e.getMessage());
			return null;
		}
	
	}
}
