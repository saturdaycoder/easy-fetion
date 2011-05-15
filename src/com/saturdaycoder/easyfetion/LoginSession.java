package com.saturdaycoder.easyfetion;
import java.net.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import javax.xml.parsers.*;
import java.io.*;
public class LoginSession {

	
	public FetionHttpResponse response;
	private SystemConfig sysConfig;
	private InputStream is;
	private OutputStream os;
	private SSLSocket socket;
	public LoginSession(SystemConfig sysConfig)
			throws UnknownHostException, IOException
	{
		this.sysConfig = sysConfig;
		SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
		socket = (SSLSocket)sslsf.createSocket(SystemConfig.ssiHostIp, 443);
		socket.setSoTimeout(10000);
		os = socket.getOutputStream();
		is = socket.getInputStream();
	}
	
	public void close()// throws IOException 
	{
		try {
			socket.close();
		} catch (Exception e) {
			Debugger.error("error closing SSI connection: " + e.getMessage());
		}
	}
	
	public void send(FetionPictureVerification pv)
			throws IOException
	{
		FetionHttpMessage loginRequest = new FetionLoginHttpRequest(sysConfig.userId,
				sysConfig.userPassword, sysConfig.mobileNumber, pv);

		Debugger.debug( "sending login request: " + loginRequest.toString());
		os.write(loginRequest.toString().getBytes());
	}
	public void read() throws IOException 
	{
		FetionHttpMessageParser parser = new FetionHttpMessageParser();
		response = (FetionHttpResponse)parser.parse(is);
	}
	public void postprocess()
			throws SAXException, IOException, ParserConfigurationException
	{
		
		String cookie = response.getHeaderValue("Set-Cookie");
		if (cookie != null) {
			int ind1 = cookie.indexOf("ssic=");
			if (ind1 != -1) {
				String ssic = cookie.substring(ind1 + 5);
				ssic = ssic.substring(0, ssic.indexOf(';'));
				sysConfig.ssic = ssic;
			}
				
		}
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
		DocumentBuilder db = dbf.newDocumentBuilder(); 
		Document document = db.parse(new ByteArrayInputStream(response.body.getBytes()));
		Node nodeRes = document.getFirstChild();
		Node nodeUser = nodeRes.getFirstChild();
		NamedNodeMap nnmUser = nodeUser.getAttributes();
		Node nodeSid = nnmUser.getNamedItem("uri");
		String useruri = nodeSid.getNodeValue();
		sysConfig.userUri = useruri;
		sysConfig.sId = useruri.substring(useruri.indexOf("sip:") + 4, useruri.indexOf('@'));
		Debugger.debug( "user's sId = \"" + sysConfig.sId + "\"");
		Node nodeUserId = nnmUser.getNamedItem("user-id");
		sysConfig.userId = nodeUserId.getNodeValue();		
		Debugger.debug( "user's userId = \"" + sysConfig.userId + "\"");
	}
	
	public void postprocessVerification(FetionPictureVerification pv)
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
		try {
			DocumentBuilder db = dbf.newDocumentBuilder(); //ParserConfigurationException
			Document document = db.parse(new ByteArrayInputStream(response.body.getBytes())); // SAXException/IOException
			Node node = document.getFirstChild();
			
			Node vn = node.getFirstChild();
			pv.algorithm = vn.getAttributes().getNamedItem("algorithm").getNodeValue();
			pv.type = vn.getAttributes().getNamedItem("type").getNodeValue();
			pv.text = vn.getAttributes().getNamedItem("text").getNodeValue();
			pv.tips = vn.getAttributes().getNamedItem("tips").getNodeValue();
		} catch (Exception e) {
			Debugger.error( "error parsing xml " + e.getMessage());
			pv.algorithm = "";
			pv.type = "";
			pv.guid = "";
			pv.text = "";
			pv.tips = "";
			pv.code = "";
		}
	}
}
