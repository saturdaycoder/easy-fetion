package com.saturdaycoder.easyfetion;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Map;
import android.util.Log;

public class AuthenticationSession {
	private static final String TAG = "EasyFetion";
	Crypto crypto;
	SystemConfig sysConfig;
	//Socket socket;
	InputStream is;
	OutputStream os;
	public SipcResponse response;
	//public FetionPictureVerification verification;
	
	
	
	public AuthenticationSession(SystemConfig sysConfig, Crypto crypto, 
			InputStream is, OutputStream os)//Socket socket)
			throws IOException
	{
		this.sysConfig = sysConfig;
		//this.socket = socket;
		this.is = is;
		this.os = os;
		this.crypto = crypto;
	}
	
	public void send(FetionPictureVerification pv) throws IOException
	{
		char rsa[] = crypto.computeResponse(sysConfig.userId, sysConfig.userPassword);
		SipcCommand authMsg = new SipcAuthenticateCommand(sysConfig, 
				new String(rsa), pv);
		os.write(authMsg.toString().getBytes());
	}
	
	public void read() throws IOException
	{
		SipcMessageParser parser = new SipcMessageParser();
		this.response = (SipcResponse)parser.parse(is);
		Log.d(TAG, "auth response=\"" + response + "\"");
	}
	
	public void postprocessJunk()
	{
		SipcMessageParser parser = new SipcMessageParser();
		SipcMessage msg = (SipcMessage)parser.parse(is);
	}
	
	public void postprocessContacts(Map<String, FetionContact> ca)
	{
		//ArrayList<FetionContact> tmp = new ArrayList<FetionContact>();
		//ca.clear();
		//ArrayList<FetionContact> ca = new ArrayList<FetionContact>();
    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
		try {
			DocumentBuilder db = dbf.newDocumentBuilder(); //ParserConfigurationException
			Document document = db.parse(new ByteArrayInputStream(response.body.getBytes())); // SAXException/IOException
			Node node = document.getFirstChild();
			//Log.d(TAG, "rootnode name is " + node.getNodeName());
			
			NodeList nl = node.getChildNodes();
			Log.d(TAG, "rootnode name is " + node.getNodeName());
			
			
			for (int j = 0; j < nl.getLength(); ++j) 
			{
				
				Node nj = nl.item(j);
				
				// user info
				if (nj.getNodeName().equals("user-info"))
				{
					Log.d(TAG, "found user-info");
					NodeList njl = nj.getChildNodes();
					for (int k = 0; k < njl.getLength(); ++k) 
					{
						Node nk = njl.item(k);
						
						// contact-list
						if (nk.getNodeName().equals("contact-list"))
						{
							Log.d(TAG, "found contact-list");
							sysConfig.contactVersion = nk.getAttributes().getNamedItem("version").getNodeValue();
							
							NodeList nkl = nk.getChildNodes();
							for (int l = 0; l < nkl.getLength(); ++l) 
							{
								Node nl2 = nkl.item(l);
								
								// buddies
								if (nl2.getNodeName().equals("buddies"))
								{
									Log.d(TAG, "found buddies");
									NodeList nll = nl2.getChildNodes();
									for (int m = 0; m < nll.getLength(); ++m)
									{
										Node b = nll.item(m);
										FetionContact c = new FetionContact();
										c.userId = b.getAttributes().getNamedItem("i").getNodeValue();
										c.sipUri = b.getAttributes().getNamedItem("u").getNodeValue();
										c.localName = b.getAttributes().getNamedItem("n").getNodeValue();
										c.groupId = Integer.parseInt(b.getAttributes().getNamedItem("l").getNodeValue());
										c.relationStatus = Integer.parseInt(b.getAttributes().getNamedItem("r").getNodeValue());
										c.identity = b.getAttributes().getNamedItem("p").getNodeValue();
										
										//tmp.add(c);
										if (!ca.containsKey(c.sipUri))
											ca.put(c.sipUri, c);
										
										Log.d(TAG, "added contact:" + c.sipUri
												+ "," + c.userId 
												+ "," + c.identity);
									}
									
									
									break;
								}
							}
							
							break;
						}
					}
					break;
				}
			}
			//return ca;
		} catch (Exception e) {
			Log.e(TAG, "error parsing xml " + e.getMessage());
			//return null;
			//throw new IOException("error parsing contacts");
			ca.clear();
		}	
		
	}
	
	public void postprocessVerification(FetionPictureVerification pv)
	{
		//FetionPictureVerification pv = new FetionPictureVerification();
		String w = response.getHeaderValue("W");
		pv.algorithm = w.substring(w.indexOf("algorithm=\"") + 11);
		pv.algorithm = pv.algorithm.substring(0, pv.algorithm.indexOf('\"'));
		pv.type = w.substring(w.indexOf("type=\"") + 6);
		pv.type = pv.type.substring(0, pv.type.indexOf('\"'));
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
		try {
			DocumentBuilder db = dbf.newDocumentBuilder(); //ParserConfigurationException
			Document document = db.parse(new ByteArrayInputStream(response.body.getBytes())); // SAXException/IOException
			Node node = document.getFirstChild();
			//Log.d(TAG, "rootnode name is " + node.getNodeName());
			
			NodeList nl = node.getChildNodes();
			
			// servers version
			for (int j = 0; j < nl.getLength(); ++j) 
			{
				if (nl.item(j).getNodeName().equals("reason"))
				{
					pv.text = nl.item(j).getAttributes().getNamedItem("text").getNodeValue();
					
					pv.tips = nl.item(j).getAttributes().getNamedItem("tips").getNodeValue();
					
					break;
				}
			}
			//return pv;
		} catch (Exception e) {
			Log.e(TAG, "error parsing xml " + e.getMessage());
			//return null;
			pv.algorithm = "";
			pv.type = "";
			pv.guid = "";
			pv.text = "";
			pv.tips = "";
			pv.code = "";
		}
	}
}
