package com.saturdaycoder.easyfetion;
import android.net.*;
import android.app.*;
import android.net.wifi.*;
import android.util.Log;
import android.content.*;
public class Network {

	private static Activity activity;
	
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
