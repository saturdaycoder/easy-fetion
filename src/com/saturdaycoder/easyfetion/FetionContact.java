package com.saturdaycoder.easyfetion;

public class FetionContact {
	//public String id;
	public String userId = "";
	public String sId = "";
	public String sipUri = "";
	public String localName = "";
	public String nickName = "";
	public String impression = "";
	public String mobileNumber = "";
	public String deviceType = "";
	public String portraitCrc = "";
	public String birthday = "";
	public String country = "";
	public String province = "";
	public String city = "";
	public String identity = "";
	public int scoreLevel = -1;
	public int serviceStatus = -1;
	public int carrierStatus = -1;
	public int relationStatus = -1;
	public String carrier = "";
	public int state = -1;
	public int groupId = -1;
	public int gender = -1;
	public int imageChanged = -1;
	public int dirty = -1;
	public String version = "";
	public String portrait = "";
	
	public String getFetionNumber() {
		int sipbegin = sipUri.indexOf("sip:");
		if (sipbegin == -1) {
			int telbegin = sipUri.indexOf("tel:");
			if (telbegin == -1) {
				return sipUri.substring(telbegin + 4);
			}
			return "";
		}
		
		int sipend = sipUri.indexOf('@');
		return sipUri.substring(sipbegin + 4, sipend);
	}
	
	public String getDisplayName() {
		if (localName.equals("")) {
			if (nickName.equals("")) {
				return "No name";
			}
			else {
				return nickName;
			}
		}
		else {
			return localName;
		}
	}
	
	public String getSmsNumber() {
		if (mobileNumber.equals("")) {
			return "12520" + getFetionNumber();
		}
		else { 
			return /*"12520" + */mobileNumber;
		}
	}
	public String getMsgNumber() {
		if (mobileNumber.equals("")) {
			return "12520" + getFetionNumber();
		}
		else { 
			return "12520" + mobileNumber;
		}
	}
	
}
