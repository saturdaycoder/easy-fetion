package com.saturdaycoder.easyfetion;

public class FetionLoginHttpRequest extends FetionHttpMessage
{
	public FetionLoginHttpRequest(String userId, String password, 
			String mobileno, FetionPictureVerification pv)//, Crypto crypto)
	{
		char hashedPassword[] = Crypto.getHashedPassword(userId, password);
		String noUri = "mobileno=" + mobileno;
		int passwordType = 1;
		String verifyUri = "";
		if (pv != null && !pv.algorithm.equals("")
					&& !pv.type.equals("") 
					&& !pv.code.equals("")
					&& !pv.guid.equals("")) {
			verifyUri = "&pid=" +pv.guid+ "&pic=" +pv.code 
					+ "&algorithm=" + pv.algorithm;
		}
		
		this.setCmdLine("GET /ssiportal/SSIAppSignInV4.aspx?" + noUri
				+ "&domains=fetion.com.cn" + verifyUri + "&v4digest-type="
				+ passwordType + "&v4digest=" + new String(hashedPassword));
		this.addHeader("User-Agent", "IIC2.0/pc " + SystemConfig.protocolVersion);
		this.addHeader("Host", SystemConfig.ssiHostName);
		this.addHeader("Cache-Control", "private");
		this.addHeader("Connection", "Keep-Alive");
	}
}
