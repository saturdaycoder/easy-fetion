package com.saturdaycoder.easyfetion;
import java.io.*;
import java.net.*;  
import org.w3c.dom.*;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import javax.xml.parsers.*;
import java.util.ArrayList;
import org.xml.sax.*;
public class SystemConfig {
	public String publicIp = "";
	public String lastLoginIp = "";
	public String lastLoginPlace = "";
	public String lastLoginTime = "";
	
	public ArrayList<FetionContact> contacts = new ArrayList<FetionContact>();
	
	public String sId;// = "";
	public String userId;// = "";
	public String userUri;// = "";
	public String configServersVersion;// = "";
	public String configParametersVersion;// = "";
	public String configHintsVersion;// = "";
	public String sipcProxyIp;// = "";
	public int sipcProxyPort;// = -1;
	public String ssic;
	public static String clientType = "PC";
	public static String clientPlatform = "W5.1";
	public String portraitServersName;// = "";
	public String portraitServersPath;// = "";
	public String mobileNumber;// = "";
	public String userPassword;// = "";
	public static String ssiHostName = "uid.fetion.com.cn";
	public static String ssiHostIp = "221.130.45.212";
	public static String fetionDomainName = "fetion.com.cn:";
	public static String navHostName = "nav.fetion.com.cn";
	public static String navHostIp = "221.130.45.201";
	public static String protocolVersion = "4.0.2510";
	
	public String personalVersion = "";
	public String customConfigVersion = "";
	public String contactVersion = "";
	
	public int state = 0;
	
	
	//private Socket navSocket;// = new Socket(navHostName, 80);
	
	private static SystemConfig instance = null;//new SystemConfig();
	
	public static SystemConfig getInstance() {
		if (instance == null) {
			synchronized(SystemConfig.class) {
				if (instance == null) {
					instance = new SystemConfig();
				}
			}
			
		}
		return instance;
	}
	
	protected SystemConfig(){
		
		Debugger.d( "SYSTEMCONFIG CTOR");
		
		publicIp = "";
		lastLoginIp = "";
		lastLoginPlace = "";
		lastLoginTime = "";
		ssic = "";
		contacts = new ArrayList<FetionContact>();
		
		sId = "";
		userId = "";
		userUri = "";
		configServersVersion = "";
		configParametersVersion = "";
		configHintsVersion = "";
		sipcProxyIp = "";
		sipcProxyPort = -1;
		//clientType = "PC";
		//clientPlatform = "W5.1";
		portraitServersName = "";
		portraitServersPath = "";
		mobileNumber = "";
		userPassword = "";
		/*ssiHostName = "uid.fetion.com.cn";
		ssiHostIp = "221.130.45.212";
		fetionDomainName = "fetion.com.cn:";
		navHostName = "nav.fetion.com.cn";
		navHostIp = "221.130.45.201";
		protocolVersion = "4.0.2510";*/
		
		personalVersion = "";
		customConfigVersion = "";
		contactVersion = "";
		
		state = 0;
	}
	
	public void setUserInfo(Document userInfoXml)
	{
		
	}
	
	public void setSystemInfo(Document sysConfigXml)
	{
		
	}
	
	private String generateConfigRequestBody()
	{
		try {
			StringWriter sw = new StringWriter();
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();  
	        XmlSerializer xs = factory.newSerializer();  
	        xs.setOutput(sw);
	        xs.startDocument("utf-8", true);
	        
	        xs.startTag(null, "config");
	        
	        xs.startTag(null, "user");
	        xs.attribute(null,"mobile-no", this.mobileNumber);
	        xs.endTag(null, "user");
	        
	        xs.startTag(null, "client");
	        xs.attribute(null,"type", clientType);
			xs.attribute(null, "version", protocolVersion);
			xs.attribute(null,"platform", clientPlatform);
			xs.endTag(null, "client");
			
			xs.startTag(null,"servers");
			xs.attribute(null, "version", "0");//this.configServersVersion);
			xs.endTag(null, "servers");
			
			xs.startTag(null,"parameters");
			xs.attribute(null,"version", "0");//this.configParametersVersion);
			xs.endTag(null, "parameters");
			
			xs.startTag(null,"hints");
			xs.attribute(null, "version", "0");//this.configHintsVersion);
			xs.endTag(null, "hints");
			
			xs.endTag(null, "config");
			xs.endDocument();
			
			return sw.toString();
		} catch (Exception e) {
			Debugger.e( "error generating conf download xml: " + e.getMessage());
			return null;
		}

	}
	
	private Socket socket;
	private InputStream is;
	private OutputStream os;
	private FetionHttpResponse resp;
	public void initDownload() throws IOException 
	{
		socket = new Socket(navHostIp, 80);
		socket.setSoTimeout(5000);
		os = socket.getOutputStream();
		is = socket.getInputStream();
		resp = null;
	}
	public void closeDownload() {
		try {
			socket.close();
		} catch (Exception e) {
			Debugger.e("error closing config download socket: " + e.getMessage());
		}
	}
	public void sendDownload() throws IOException 
	{
		String body = this.generateConfigRequestBody();
		String str = "POST /nav/getsystemconfig.aspx HTTP/1.1\r\n"
	           + "User-Agent: IIC2.0/PC " + protocolVersion + "\r\n"
	           + "Host: " + navHostName + "\r\n"
	           + "Connection: Close\r\n" 
	           + "Content-Length: " + body.length() + "\r\n\r\n" + body;
		
		Debugger.d( "config request = \"" + str + "\"");
		
		os.write(str.getBytes());
	}
	
	public void readDownload() throws IOException 
	{
		FetionHttpMessageParser parser = new FetionHttpMessageParser();
		resp = (FetionHttpResponse)parser.parse(is);
		
		Debugger.d( "config download response = \"" + resp.toString()+"\"");
	}
	
	public void postprocessDownload() 
			throws IOException, ParserConfigurationException, SAXException
	{
		String xmlstr = resp.body;
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 

		DocumentBuilder db = dbf.newDocumentBuilder(); 
		Document document = db.parse(new ByteArrayInputStream(xmlstr.getBytes())); 
		Node node = document.getFirstChild();
		Debugger.d( "rootnode name is " + node.getNodeName());
		
		NodeList nl = node.getChildNodes();
		
		// servers version
		for (int j = 0; j < nl.getLength(); ++j) 
		{
			if (nl.item(j).getNodeName().equals("servers"))
			{
				this.configServersVersion = nl.item(j).getAttributes().getNamedItem("version").getNodeValue();
				Debugger.d( "servers version = " + this.configServersVersion);
				NodeList nlservers = nl.item(j).getChildNodes();
				Debugger.d( "servers node has " + nlservers.getLength() + " child nodes");
				for (int i = 0; i < nlservers.getLength(); ++i) 
				{
					Node n = nlservers.item(i);
					// sipc-proxy
					if (n.getNodeName().equals("sipc-proxy")) 
					{
						Debugger.d( "found sipc proxy node");
						String tmp = n.getFirstChild().getNodeValue();//n.getTextContent();
						Debugger.d( "found sipc proxy node value: " + tmp);
						this.sipcProxyIp = tmp.substring(0, tmp.indexOf(':'));
						Debugger.d( "sipcProxyIp = \"" + this.sipcProxyIp + "\"");
						this.sipcProxyPort = Integer.parseInt(tmp.substring(tmp.indexOf(':') + 1));
						Debugger.d( "sipcProxyPort = " + this.sipcProxyPort);
					}
					// get-uri
					if (n.getNodeName().equals("get-uri")) 
					{
						Debugger.d( "found get-uri node");
						String tmp = n.getFirstChild().getNodeValue();
						tmp = tmp.substring(tmp.indexOf("http://") + 7);
						int firstSlashPos = tmp.indexOf('/');
						//int secondSlashPos = tmp.substring(firstSlashPos + 1).indexOf('/');
						this.portraitServersName = tmp.substring(0, firstSlashPos);
						Debugger.d( "portraitServersName = \"" + this.portraitServersName + "\"");
						this.portraitServersPath = tmp.substring(firstSlashPos + 1);
						this.portraitServersPath = this.portraitServersPath.substring(0, this.portraitServersPath.indexOf('/'));
						Debugger.d( "portraitServersPath = \"" + this.portraitServersPath + "\"");
					}
				}
			}
			
			// parameters version
			if (nl.item(j).getNodeName().equals("parameters"))
			{
				//Node nParameters = nl.getNamedItem("parameters");
				this.configParametersVersion = nl.item(j).getAttributes().getNamedItem("version").getNodeValue();
				Debugger.d( "parameters version = " + this.configParametersVersion);
			}
			
			// hints version
			if (nl.item(j).getNodeName().equals("hints"))
			{
				//Node nHints = nl.getNamedItem("hints");
				this.configHintsVersion = nl.item(j).getAttributes().getNamedItem("version").getNodeValue();
				Debugger.d( "hints version = " + this.configHintsVersion);
			}
		}
	}
	

}
