package com.saturdaycoder.easyfetion;
import android.net.*;
import java.net.*;
import android.app.*;
import android.net.wifi.*;
import android.util.Log;
import android.content.*;
import java.io.IOException;
import java.io.*;
public class Network {

	private static final String TAG="EasyFetion";
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
	public static void createSipcSocket(String ip, int port) throws IOException
	{
		
		Log.d(TAG, "SIPC socket is CREATED");
		
		if (sipcSocket == null) {
			sipcSocket = new Socket(ip, port);
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
		Log.d(TAG, "SIPC socket is CLOSED");
		if (sipcSocket != null) {
			sipcSocket.close();
			sipcSocket = null;
			is = null;
			os = null;
		}
	}
	
	public static void setActivity(Activity activity) {
		
		Network.activity = activity;
	}
	public static String getWifiMacAddr() {
		WifiManager wifi = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) {
        
        	return "ffffffffffff";
        }
        
        WifiInfo info = wifi.getConnectionInfo();
        if (info == null) {
        
        	return "ffffffffffff";
        }
        String macAddr = info.getMacAddress().replace(":", "");
        
        return macAddr;
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
