package com.saturdaycoder.easyfetion;
import java.io.*;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import java.net.*;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

import android.widget.ImageView;
import android.util.Log;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.*;
import android.widget.AdapterView;
import android.view.View;
import android.widget.EditText;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.net.UnknownHostException;
import java.io.IOException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.HashMap;
import android.view.MenuItem;
import android.view.Menu;
import com.saturdaycoder.easyfetion.EasyFetionThread.State;
//import com.saturdaycoder.easyfetion.LoginThread.ThreadState;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Looper;
import android.content.res.*;

public class EasyFetion extends Activity 
{
	private static String TAG = "EasyFetion";
	
	private String lastLoginAcc = "";
	private String lastPasswd = "";

	private static final int MENU_SET_ACC_ID = Menu.FIRST;  
	private static final int MENU_REFRESH_ID = Menu.FIRST + 1;  
	private static final int MENU_ABOUT_ID = Menu.FIRST + 2;  
	
	private static final int DIALOG_LOGIN_PROGRESS = 0;
	private static final int DIALOG_REFRESH_PROGRESS = 1;
	
	private static final int INTENT_ACC_SET_DIALOG = 0;
	private static final int INTENT_PIC_VERIFY_DIALOG_FOR_LOGIN = 1;
	private static final int INTENT_PIC_VERIFY_DIALOG_FOR_AUTHENTICATE = 2;
	
	private ListView lvContacts;
	private SystemConfig sysConfig;
    private Handler loginUiHandler;
    private Handler refreshUiHandler;
    private Crypto crypto;
    private Map<String, FetionContact> contactList;
    
    
    
    //private FetionPictureVerification verification;
    
    private LoginThread loginThread;
    private RefreshThread refreshThread;
    
    private boolean pendingLogin = false;

    //final EasyFetionThread worker = new EasyFetionThread();;
    
    private ArrayList<FetionContact> selectedContacts = new ArrayList<FetionContact>();
    
    private void showerr(String TAG, String msg)
    {
    	popNotify(msg);
    	Log.e(TAG, msg);
    }
    
    private void popNotify(String msg)
    {
        Toast.makeText(EasyFetion.this, msg,
                Toast.LENGTH_LONG).show();
    }
    private void statusbarNotify(String msg)
    {
    	
    }

    private class RefreshUiHandler extends Handler {
        
    	@Override
        public void handleMessage(Message msg) 
		{
    		RefreshThread.State state = (RefreshThread.State)msg.obj;
    		Log.v(TAG, "received reports of state: " + state.toString());
    		switch (state) {
			case AUTHENTICATE_NEED_CONFIRM: {					
				Intent intent = new Intent();
				intent.setClass(EasyFetion.this, PictureVerifyDialog.class);
				Bundle bundle = new Bundle();
				bundle.putByteArray("picture", refreshThread.verification.getPicture());
				bundle.putString("text", refreshThread.verification.text);
				bundle.putString("tips", refreshThread.verification.tips);
				intent.putExtras(bundle);
				startActivityForResult(intent, INTENT_PIC_VERIFY_DIALOG_FOR_AUTHENTICATE);
				
				break;
			}
			case CONTACT_GET_SUCC:
				dismissDialog(DIALOG_REFRESH_PROGRESS);
				FetionDatabase.getInstance().setUserInfo(sysConfig);
				
				loadContactList();
				
				
				Iterator<String> iter = contactList.keySet().iterator();
        		while (iter.hasNext()) {
        			String uri = iter.next();
        			FetionContact c = contactList.get(uri);
        			FetionDatabase.getInstance().setContact(c);
        		}
				
				showerr(TAG, "Congratulations! contact list ok!!");
				break;
			case AUTHENTICATE_FAIL:
				dismissDialog(DIALOG_REFRESH_PROGRESS);
				showerr(TAG, "authenticate failed. try again");
				break;
			default:
				break;
			}
		}
	}
    
    private class LoginUiHandler extends Handler {
    	@Override
        public void handleMessage(Message msg) 
		{
    		LoginThread.State state = (LoginThread.State)msg.obj;
			
			Log.v(TAG, "received reports of state: " + state.toString());
			
			switch (state) {
			case LOGIN_NEED_CONFIRM: {			
				Intent intent = new Intent();
				intent.setClass(EasyFetion.this, PictureVerifyDialog.class);
				Bundle bundle = new Bundle();
				bundle.putByteArray("picture", loginThread.verification.getPicture());
				bundle.putString("text", loginThread.verification.text);
				bundle.putString("tips", loginThread.verification.tips);
				intent.putExtras(bundle);
				startActivityForResult(intent, INTENT_PIC_VERIFY_DIALOG_FOR_LOGIN);
				
				break;
			}
			case LOGIN_FAIL: 
				dismissDialog(INTENT_ACC_SET_DIALOG);
				showerr(TAG, "Login failed. input your account again");
				break;
			case LOGIN_SUCC: 
				FetionDatabase.getInstance().setAccount(sysConfig);
				FetionDatabase.getInstance().clearContacts();
				break;	
			case CONFIG_DOWNLOADING:
				break;
			case CONFIG_DOWNLOAD_SUCC:
				dismissDialog(INTENT_ACC_SET_DIALOG);
				FetionDatabase.getInstance().setUserInfo(sysConfig);
				
    			
				showerr(TAG, "Congratulations! Your ass is mine!!");
				
				showDialog(DIALOG_REFRESH_PROGRESS);
				sysConfig.contactVersion = "0";
	    		
				refreshThread = new RefreshThread(sysConfig, crypto,
						contactList, refreshUiHandler);
				refreshThread.start();
				
				break;
			case CONFIG_DOWNLOAD_FAIL:
				break;
			}
			
		}
    }
    
    
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	Log.d(TAG, "QUICKFETION ONCREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //btnSend = (Button)findViewById(R.id.buttonSend);
        //editMsg = (EditText)findViewById(R.id.editMessage);
        //spinContacts = (Spinner)findViewById(R.id.spinContacts);
        lvContacts = (ListView)findViewById(R.id.lvContacts);
        

        Network.setActivity(this);
        
        if (!Network.isNetworkAvailable()) {
        	showerr(TAG, "Give me your WIFI/3G's ass. Or I get nothing to fuck");

        	finish();
        	return;
        }
        
        FetionDatabase.setInstance(this);
        SmsDbAdapter.setContext(this);
        
        //sysConfig = new SystemConfig();
        sysConfig = SystemConfig.getInstance();
        refreshUiHandler = new RefreshUiHandler();
        loginUiHandler = new LoginUiHandler();
        contactList = new LinkedHashMap<String, FetionContact>();
        crypto = Crypto.getInstance();

        lvContacts.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> a, View v, int position, long id) 
        	{
        		Log.d(TAG, "lvcontacts onitemclick");
        		//selectedContacts.clear();
        		Iterator<String> iter = contactList.keySet().iterator();
        		int i = -1;
        		String uri = "";
        		while (i != position && iter.hasNext()) {
        			uri = iter.next();
        			i++;
        		}
        		if (i != position) {
        			showerr(TAG, "FATAL error selecting contact");
        			return;
        		}
        		
        		Intent intent = new Intent();
    			intent.setClass(EasyFetion.this, MsgHistory.class);
    			Bundle bundle = new Bundle();
    			bundle.putString("mobileno", contactList.get(uri).getSmsNumber());
    			bundle.putString("nickname", contactList.get(uri).getDisplayName());
    			bundle.putString("sipuri", contactList.get(uri).sipUri);
    			intent.putExtras(bundle);
    			startActivity(intent);
        		
        		//selectedContacts.add(worker.contactList.get(uri));
        		//showerr(TAG, "selected contact " + worker.contactList.get(uri).nickName);
        		//}
        	}
        });

    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	Log.d(TAG, "onActivityResult: " + requestCode + ", " + resultCode );
    	switch (requestCode) {
    	case INTENT_ACC_SET_DIALOG: {
    		if (resultCode == RESULT_OK) {
	    		Bundle bundle = data.getExtras();
	    		if (!bundle.containsKey("mobileno")) {
	    			Log.e(TAG, "not contain mobileno");
	    		}
	    		sysConfig.mobileNumber = bundle.getString("mobileno");
	    		sysConfig.userPassword = bundle.getString("passwd");
	    		lastLoginAcc = sysConfig.mobileNumber;
	    		if (sysConfig.mobileNumber == null || sysConfig.userPassword == null) {
	    			Log.e(TAG, "retrieving user account:" + 
	    					sysConfig.mobileNumber + ", " + sysConfig.userPassword);
	    		}
	
	    		showDialog(DIALOG_LOGIN_PROGRESS);
	    		
				loginThread = new LoginThread(sysConfig, loginUiHandler);
				loginThread.start();
	    		
	    		Log.v(TAG, "Dialog created and returned");
    		}
    		else {
    			Log.d(TAG, "dialog canceled");
    		}
    		break;
    	}
    	case INTENT_PIC_VERIFY_DIALOG_FOR_LOGIN: {
    		if (loginThread != null && (loginThread.state == LoginThread.State.LOGIN_NEED_CONFIRM))
    		{
    			switch (resultCode) {
    			case RESULT_OK: {
		    		Bundle bundle = data.getExtras();
		    		loginThread.verification.code = bundle.getString("code"); 
		    		synchronized(loginThread) {
		    			loginThread.notify();
		    		}
		    		break;
    			}
		    	default:
		    		Log.e(TAG, "pic verify destroyed");
		    		loginThread.stop();
		    		break;
    			}
    		}
    		break;
    	}
    	case INTENT_PIC_VERIFY_DIALOG_FOR_AUTHENTICATE: {
    		Log.d(TAG, "refreshThread state = " + refreshThread.state.ordinal());
    		if (refreshThread != null && (refreshThread.state == RefreshThread.State.AUTHENTICATE_NEED_CONFIRM)) {
    			switch (resultCode) {
    			case RESULT_OK: {
		    		Bundle bundle = data.getExtras();
		    		refreshThread.verification.code = bundle.getString("code"); 
		    		synchronized(refreshThread) {
		    			refreshThread.notify();
		    		}
		    		break;
    			}
		    	default:
		    		Log.e(TAG, "pic verify destroyed");
		    		refreshThread.stop();
		    		break;
    			}
    		}
    		break;
    	}
    	default: 
    		break;
    	}
    }
    
    @Override
    protected void onStart()
    {
    	Log.i(TAG, "QUICKFETION ONSTART");
    	super.onStart();
    	
    	sysConfig = SystemConfig.getInstance();//new SystemConfig();
    	
    	FetionDatabase.getInstance().getAccount(sysConfig);
    	
    	if (sysConfig.sId == "") {
    		lastLoginAcc = "";
        	Intent intent = new Intent();
			intent.setClass(EasyFetion.this, AccountSettingDialog.class);
			Bundle bundle = new Bundle();
			bundle.putString("lastlogin", lastLoginAcc);
			intent.putExtras(bundle);
			startActivityForResult(intent, INTENT_ACC_SET_DIALOG);
    	}
    	else {
    		lastLoginAcc = sysConfig.mobileNumber;
    		FetionDatabase.getInstance().getUserInfo(sysConfig);
    		Log.d(TAG, "SIPC = " + sysConfig.sipcProxyIp + ":" + sysConfig.sipcProxyPort);
    		if (sysConfig.sipcProxyIp == "" || sysConfig.sipcProxyPort == -1) {
    			//Log.e(TAG, "error getting sipc proxy from db");
    			// download config
    		}
    		else {
    			
    			FetionContact contacts[] = FetionDatabase.getInstance().getContacts();
    			for (FetionContact c: contacts) {
    				contactList.put(c.sipUri, c);
    			}
    			
    			loadContactList();
    		}
    	}
    }
    
    private void loadContactList() {
    	ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>(); 
		//for(int i=0;i<worker.contactList.size();i++)
		Iterator<String> iter = contactList.keySet().iterator();
		while(iter.hasNext())
		{ 
			String uri = iter.next();
			String nn = contactList.get(uri).getDisplayName();
			
			String mn = contactList.get(uri).mobileNumber;
			if (mn == null || mn.equals("")) {
				mn = getString(R.string.mobileno_invalid);
			}
			
			
		    HashMap<String, Object> map = new HashMap<String, Object>(); 
		    map.put("FetionImage", R.drawable.icon); 
		    map.put("FetionNickName", nn); 
		    map.put("FetionMobileNo", mn); 
		    listItem.add(map); 
		} 
		SimpleAdapter listItemAdapter = new SimpleAdapter(EasyFetion.this,
			listItem,
		    R.layout.contactlistitem,
		            
		    new String[] {"FetionImage",
				"FetionNickName", 
				"FetionMobileNo"},  
		    
		    new int[] {R.id.contactListItemIcon,
				R.id.contactListItemName,
				R.id.contactListItemNumber} 
		); 
		
		
		lvContacts.setAdapter(listItemAdapter);  
    }
    
    @Override
    protected void onRestart()
    {
    	Log.i(TAG, "QUICKFETION ONRESTART");
    	super.onRestart();
    }
    @Override
    protected void onResume()
    {
    	Log.i(TAG, "QUICKFETION ONRESUME");
    	super.onResume();

    }
    @Override
    protected void onPause()
    {
    	Log.i(TAG, "QUICKFETION ONPAUSE");
    	super.onPause();
    }
    @Override
    protected void onStop()
    {
    	Log.i(TAG, "QUICKFETION ONSTOP");
    	super.onStop();
    }
    @Override
    protected void onDestroy()
    {

    	Log.i(TAG, "QUICKFETION ONDESTROY");
    	super.onDestroy();
    	
    	if (loginThread != null)
    		loginThread.stop();
    	
    	if (refreshThread != null)
    		refreshThread.stop();
    	
    	try {
    		Network.closeSipcSocket();
    	} catch (Exception e) {
    		
    	}
    }
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    	Log.i(TAG, "QUICKFETION ONSAVEINSTANCESTATE");
    	super.onSaveInstanceState(outState);
    }
    @Override 
    public void onConfigurationChanged(Configuration newConfig) {
    	Log.i(TAG, "QUICKFETION ONCONFIGURATIONCHANGED: " + newConfig.toString());
    	super.onConfigurationChanged(newConfig); 

    }
    
    //@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
    	Log.d(TAG, "onMeasure(" + widthMeasureSpec + "," + heightMeasureSpec + ")");
    	
    }
    @Override
    public boolean onCreateOptionsMenu (Menu aMenu) {  
        
        super.onCreateOptionsMenu(aMenu);  
        aMenu.add(0, MENU_SET_ACC_ID, 0, R.string.menu_set_account);  
        aMenu.add(0, MENU_REFRESH_ID, 0, R.string.menu_refresh_contact_list);  
        aMenu.add(0, MENU_ABOUT_ID, 0, R.string.menu_about);  
        return true;  
          
    }  
    @Override
    public boolean onOptionsItemSelected (MenuItem aMenuItem) {  
        
        switch (aMenuItem.getItemId()) {  
            case MENU_SET_ACC_ID:  
            	Intent intent = new Intent();
				intent.setClass(EasyFetion.this, AccountSettingDialog.class);
				Bundle bundle = new Bundle();
				bundle.putString("lastlogin", lastLoginAcc);
				intent.putExtras(bundle);
				
				sysConfig.mobileNumber = "";
				sysConfig.userPassword = "";
				sysConfig.sId = "";
				sysConfig.userId = "";
				
				
				startActivityForResult(intent, INTENT_ACC_SET_DIALOG);
                break;  
            case MENU_REFRESH_ID:  
				showDialog(DIALOG_REFRESH_PROGRESS);
				FetionDatabase.getInstance().clearContacts();
	    		sysConfig.contactVersion = "0";
				refreshThread = new RefreshThread(sysConfig, crypto,
						contactList, refreshUiHandler);
				refreshThread.start();
                break;  
            case MENU_ABOUT_ID:  
                showerr(TAG, "This client is fucking awesome!");  
                break;  
        }  
        return super.onOptionsItemSelected(aMenuItem);
    }  
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_LOGIN_PROGRESS: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("Login");
                dialog.setMessage("Please wait while loging in...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(true);
                return dialog;
            }
            case DIALOG_REFRESH_PROGRESS: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("Refresh");
                dialog.setMessage("Please wait while refreshing...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(true);
                return dialog;
            }
        }
        return null;
    }
}
        
    
    


