package com.saturdaycoder.easyfetion;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import android.util.Log;

public class FetionPictureVerification 
{
	private static final String TAG = "EasyFetion";
	public String guid = "";
	public String algorithm = "";
	public String type = "";
	public String code = "";
	public String text = "";
	public String tips = "";
	private String pic_base64 = "";
	//private SystemConfig sysConfig;
	public FetionPictureVerification(/*SystemConfig sc*/) 
	{
		//this.sysConfig = sc;
	}
	
	public byte[] getPicture()
	{
		try {
			Socket socket = new Socket(SystemConfig.navHostName, 80);
			String str = "GET /nav/GetPicCodeV4.aspx?algorithm=" + this.algorithm + " HTTP/1.1\r\n"				
					+ "Host: " + SystemConfig.navHostName + "\r\n"
					+ "User-Agent: IIC2.0/PC " + SystemConfig.protocolVersion + "\r\n"
					+ "Connection: Close\r\n\r\n" ;
			
			//Debugger.d( "config request = \"" + str + "\"");
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			os.write(str.getBytes());
			//Debugger.d( "sent auth pic \"" +  str + "\"");
			byte output[] = new byte[1024];
			int xml_len = -1;
			int xmllen_startpos = -1;
			int xmllen_endpos = -1;
			int total_len = 0;
			int header_len = -1;
			String response = "";
			byte respBytes[] = null;
			do 
			{
				int len = is.read(output);
				
				if (len == -1)
					break;

				total_len += len;
				
				response += new String(output, 0, len);				
				
				if (xml_len == -1) 
				{
					int start = response.indexOf("Content-Length: ");
					int end = response.indexOf("\r\n\r\n");
					if (start != -1) xmllen_startpos = start;
					if (end != -1) xmllen_endpos = end;
					if (xmllen_startpos != -1 && xmllen_endpos != -1) 
					{
						xml_len = Integer.parseInt(response.substring(xmllen_startpos + 16, xmllen_endpos));
						header_len = xmllen_endpos + 4;
					}
				}
			}while (xml_len == -1 || total_len < xml_len + header_len);
			//Debugger.d( "response = \"" + response + "\"");
			String xmlstr = response.substring(header_len);
			
			// check http status code
			int s = response.indexOf(' ') + 1;
			int e = response.indexOf(' ', s);
			String stateCode = response.substring(s, e);
			//Debugger.d( "status code = \"" + stateCode + "\"");
			if (!stateCode.equals("200"))
				return null;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
			DocumentBuilder db = dbf.newDocumentBuilder(); //ParserConfigurationException
			Document document = db.parse(new ByteArrayInputStream(xmlstr.getBytes())); // SAXException/IOException
			Node node = document.getFirstChild();
			
			Node n = node.getFirstChild();
			//Debugger.d( "first node = \"" + n.getNodeName() + "\"");
			// servers version
			Node nId = n.getAttributes().getNamedItem("id");
			Node nPic = n.getAttributes().getNamedItem("pic");
			if (nId == null || nPic == null) {
				Debugger.e( "error getting element id and pic");
				return null;
			}
			this.guid = nId.getNodeValue();
			this.pic_base64 = nPic.getNodeValue();					
			
			//Debugger.d( "guid = \"" + this.verifyGuid + "\"");
			//Debugger.d( "pic = \"" + this.verifyPic + "\"");
	
			return this.pic_base64.getBytes();
		} catch (Exception e) {
			Debugger.e( "error geting auth pic: " + e.getMessage());
			return null;
		}
	}
}
