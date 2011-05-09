package com.saturdaycoder.easyfetion;
import java.io.*;
import java.net.*;  
import org.w3c.dom.*;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import javax.xml.parsers.*;
import java.util.ArrayList;

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
	
	private String TAG = "EasyFetion";
	
	private Socket navSocket;// = new Socket(navHostName, 80);
	
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
		/*DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
		DocumentBuilder builder;
		Document doc;
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.newDocument();
		} catch (Exception e) {
			return null;
		}
		Element root = doc.createElement("config");
		doc.appendChild(root);
		Element user = doc.createElement("user");
		user.setAttribute("mobile-no", this.mobileNumber);
		root.appendChild(user);
		Element client = doc.createElement("client");
		client.setAttribute("type", clientType);
		client.setAttribute("version", protocolVersion);
		client.setAttribute("platform", clientPlatform);
		root.appendChild(client);
		Element servers = doc.createElement("servers");
		servers.setAttribute("version", this.configServersVersion);
		root.appendChild(servers);
		Element parameters = doc.createElement("parameters");
		parameters.setAttribute("version", this.configParametersVersion);
		root.appendChild(parameters);
		Element hints = doc.createElement("hints");
		hints.setAttribute("version", this.configHintsVersion);
		root.appendChild(hints);*/
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
		/*try {
			StringWriter sw = new StringWriter();  				
	        Transformer serializer = TransformerFactory.newInstance().newTransformer();
	        serializer.transform(new DOMSource(doc), new StreamResult(sw));  
	        String ret = sw.toString();
			
	        ret = ret.substring(ret.indexOf("<config>"));
	        return ret;
		} catch (Exception e) {
			Debugger.e( "error generating body: " + e.getMessage());
			return null;
		}*/
		//return XmlConverter.xml2String(doc);
	}
	

	
	public void Download()//SystemConfig sysConfig)
	{
		String body = this.generateConfigRequestBody();
		try {
			//Socket socket = new Socket(navHostName, 80);
			Socket socket = new Socket(navHostIp, 80);
			String str = "POST /nav/getsystemconfig.aspx HTTP/1.1\r\n"
		           + "User-Agent: IIC2.0/PC " + protocolVersion + "\r\n"
		           + "Host: " + navHostName + "\r\n"
		           + "Connection: Close\r\n" 
		           + "Content-Length: " + body.length() + "\r\n\r\n" + body;
			
			Debugger.d( "config request = \"" + str + "\"");
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			
			os.write(str.getBytes());
			
			byte output[] = new byte[1024];
			
			int xml_len = -1;
			int xmllen_startpos = -1;
			int xmllen_endpos = -1;
			int total_len = 0;
			int header_len = -1;
			String response = "";
			/*byte respBytes[] = null;
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

			}while (xml_len == -1 || total_len < xml_len + header_len);*/
			
			FetionHttpMessageParser parser = new FetionHttpMessageParser();
			FetionHttpResponse resp = (FetionHttpResponse)parser.parse(is);
			
			Debugger.d( "config download response = \"" + resp.toString()+"\"");
			
			String xmlstr = resp.body;//response.substring(header_len);
			
			// parse xml
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
			try {
				DocumentBuilder db = dbf.newDocumentBuilder(); //ParserConfigurationException
				Document document = db.parse(new ByteArrayInputStream(xmlstr.getBytes())); // SAXException/IOException
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
				

			} catch (Exception e) {
				Debugger.e( "error happens: " + e.getClass().getName());
			}
			
			
		} catch (IOException e) {
		
		}
	}
}
