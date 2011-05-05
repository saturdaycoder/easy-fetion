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
			/*DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
			DocumentBuilder builder;
			Document doc;
			try {
				builder = factory.newDocumentBuilder();
				doc = builder.newDocument();
			} catch (Exception e) {
				return null;
			}*/
			StringWriter sw = new StringWriter();
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();  
	        XmlSerializer xs = factory.newSerializer();  
	        // 设置输出流对象  
	        
	        xs.setOutput(sw);
	        
	        xs.startDocument("utf-8", true); 
			
			//Element root = doc.createElement("args");
			//doc.appendChild(root);
	        xs.startTag(null, "args");
			
			//Element device = doc.createElement("device");
			//device.setAttribute("machine-code", Network.macAddr);
			//root.appendChild(device);
	        xs.startTag(null, "device");
	        xs.attribute(null, "machine-code", Network.getWifiMacAddr());
	        xs.endTag(null, "device");
			
			//Element caps = doc.createElement("caps");
			//caps.setAttribute("value", "1ff");
			//root.appendChild(caps);
	        xs.startTag(null, "caps");
	        xs.attribute(null, "value", "1ff");
	        xs.endTag(null, "caps");
			
			//Element events = doc.createElement("events");
			//events.setAttribute("value", "7f");
			//root.appendChild(events);
	        xs.startTag(null, "events");
	        xs.attribute(null, "value", "7f");
	        xs.endTag(null, "events");
			
			//Element userinfo = doc.createElement("user-info");
			//userinfo.setAttribute("mobile-no", sc.mobileNumber);
			//userinfo.setAttribute("user-id", sc.userId);
			//root.appendChild(userinfo);
	        xs.startTag(null, "user-info");
	        xs.attribute(null, "mobile-no", sc.mobileNumber);
	        xs.attribute(null, "user-id", sc.userId);
	        
			
			//Element personal = doc.createElement("personal");
			//personal.setAttribute("version", sc.personalVersion);
			//personal.setAttribute("attributes", "v4default");
			//userinfo.appendChild(personal);
	        xs.startTag(null, "personal");
	        xs.attribute(null, "version", sc.personalVersion);
	        xs.attribute(null, "attributes", "v4default");
	        xs.endTag(null, "personal");
			
			//Element customconfig = doc.createElement("custom-config");
			//customconfig.setAttribute("version", sc.customConfigVersion);
			//userinfo.appendChild(customconfig);
	        xs.startTag(null, "custom-config");
	        xs.attribute(null, "version", sc.customConfigVersion);
	        xs.endTag(null, "custom-config");
			
			//Element contactlist = doc.createElement("contact-list");
			//contactlist.setAttribute("version", sc.contactVersion);
			//contactlist.setAttribute("buddy-attributes", "v4default");
			//userinfo.appendChild(contactlist);
	        xs.startTag(null, "contact-list");
	        xs.attribute(null, "version", sc.contactVersion);
	        xs.attribute(null, "buddy-attributes", "v4default");
	        xs.endTag(null, "contact-list");
			
			xs.endTag(null, "user-info");
			
			//Element credentials = doc.createElement("credentials");
			//credentials.setAttribute("domains", "fetion.com.cn");
			//root.appendChild(credentials);
			
			//Element presence = doc.createElement("presence");
			//root.appendChild(presence);
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
			Log.e(TAG, "error generating auth xml: " + e.getMessage());
			return null;
		}
		//Element basic = doc.createElement("basic");
		//basic.setAttribute("value", String.valueOf(sc.state));
		//basic.setAttribute("desc", "");
		//presence.appendChild(basic);
		
		/*try {
			StringWriter sw = new StringWriter();  				
	        Transformer serializer = TransformerFactory.newInstance().newTransformer();
	        serializer.transform(new DOMSource(doc), new StreamResult(sw));  
	        String ret = sw.toString();
	        ret = ret.substring(ret.indexOf("<args>")) + "\r\n";
	        return ret;
		} catch (Exception e) {
			Log.e(TAG, "error generating body");
			return null;
		}*/
		
	}
}
