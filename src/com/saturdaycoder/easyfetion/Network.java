package com.saturdaycoder.easyfetion;
import android.net.*;
import java.lang.IllegalAccessException;
import java.net.*;
import android.app.*;
import android.net.wifi.*;
import android.content.*;
import java.io.*;
import android.telephony.*;
public class Network {

	private static Activity activity;
	
	private static Socket sipcSocket = null;
	private static InputStream is = null;
	private static OutputStream os = null;
	
	public static Socket getSipcSocket() throws IOException 
	{
		if (sipcSocket == null)
			throw new IOException ("SIPC is null");
		if (sipcSocket.isClosed())
			throw new IOException("SIPC is closed");
		if (!sipcSocket.isConnected())
			throw new IOException ("SIPC not connected");
		return sipcSocket;
	}
	public static String encodeUril(String original) {
		String encoded = original;
		encoded = encoded.replace("/", "%2f").replace("@", "%40").replace("=", "%3d");
		encoded = encoded.replace(":", "%3a").replace(";", "%3b").replace("+", "%2b");
		return encoded;
	}
	
	public static void createSipcSocket(String ip, int port) throws IOException
	{
		
		Debugger.d( "SIPC socket is CREATED");
		
		if (sipcSocket == null) {
			sipcSocket = new Socket(ip, port);
			sipcSocket.setSoTimeout (10000);
			is = sipcSocket.getInputStream();
			os = sipcSocket.getOutputStream();
		}
	}
	public static InputStream getSipcInputStream() throws IOException
	{
		getSipcSocket();
		return is;
	}
	public static OutputStream getSipcOutputStream() throws IOException
	{
		getSipcSocket();
		return os;
	}
	public static void closeSipcSocket() throws IOException 
	{
		
		if (sipcSocket != null && sipcSocket.isConnected()) {
			Debugger.d("closing SIPC socket");
			sipcSocket.close();
			sipcSocket = null;
			is = null;
			os = null;
		}
		Debugger.d( "SIPC socket is CLOSED");
	}
	
	public static void setActivity(Activity activity) {
		
		Network.activity = activity;
	}
	public static String getWifiMacAddr() throws IllegalAccessException {
		WifiManager wifi = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
		if (wifi == null) {
			throw new IllegalAccessException("Can not access WIFI service");
		}
        
        WifiInfo info = wifi.getConnectionInfo();
        if (info == null) {
        	throw new IllegalAccessException("Unable to get WIFI information");
        }
        
        String macAddr = info.getMacAddress();
        if (macAddr == null || macAddr.equals("")) {
        	throw new IllegalAccessException("Illegal WIFI MAC address");
        }
        macAddr = macAddr.replace(":", "");
        
        return macAddr;
	}
	public static String getPhoneNumber() throws IllegalAccessException {
		TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm == null) {
			throw new IllegalAccessException("Can not access TELEPHONY service");
		}
		
		String line1Number = tm.getLine1Number();
		if (line1Number == null || line1Number.equals("")) {
			throw new IllegalAccessException("Illegal Line 1 Number");
		}
		
		return line1Number;
	}
	
	public static String getDeviceId() throws IllegalAccessException {
		TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm == null) {
			throw new IllegalAccessException("Can not access TELEPHONY service");
		}
        
        String devid = tm.getDeviceId();
        if (devid == null || devid.equals("")) {
        	throw new IllegalAccessException("Illegal Device ID");
        }
        
        return devid;
	}
	public static boolean isNetworkAvailable() { 
	    Context context = activity.getApplicationContext();
	    ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    if (connectivity == null) {    
	      return false;
	    } else {  
	        NetworkInfo[] info = connectivity.getAllNetworkInfo();    
	        if (info != null) {        
	            for (int i = 0; i < info.length; i++) {           
	                if (info[i].getState() == NetworkInfo.State.CONNECTED) {              
	                    return true; 
	                }        
	            }     
	        } 
	    }   
	    return false;
	}
}
