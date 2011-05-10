package com.saturdaycoder.easyfetion;
import java.io.*;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.os.Bundle;
import android.os.Message;
import android.widget.Toast;
import android.widget.AdapterView.*;
import android.widget.AdapterView;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.HashMap;
import android.view.MenuItem;
import android.view.Menu;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.saturdaycoder.easyfetion.HttpThread.Command;

import android.os.Handler;
import android.content.res.*;
import android.content.*;
public class EasyFetion extends Activity 
{
	private static String TAG = "EasyFetion";
	
	private String lastLoginAcc = "";
	//private String lastPasswd = "";

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
    
    private HttpThread loginThread;
    private SipcThread refreshThread;
    
    //private boolean pendingLogin = false;

    //final EasyFetionThread worker = new EasyFetionThread();;
    
    //private ArrayList<FetionContact> selectedContacts = new ArrayList<FetionContact>();
    
    private void showerr(String TAG, String msg)
    {
    	popNotify(msg);
    	Debugger.e( msg);
    }
    
    private void popNotify(String msg)
    {
        Toast.makeText(EasyFetion.this, msg,
                Toast.LENGTH_LONG).show();
    }


    private class RefreshUiHandler extends Handler {
        
    	@Override
        public void handleMessage(Message msg) 
		{
    		SipcThread.State state = (SipcThread.State)msg.obj;
    		Debugger.v( "received reports of state: " + state.toString());
    		switch (state) {
    		case INIT:
    		case CONNECTING_SIPC:
    			break;
    		case CONNECTING_SUCC:
	    		refreshThread.addCommand(SipcThread.Command.REGISTER, null);
	    		break;
    		case CONNECTING_FAIL:
    			dismissDialog(DIALOG_REFRESH_PROGRESS);
    			showerr(TAG, "Pls check your network connection");
    			break;
    		case DISCONNECTING_SIPC:
    		case DISCONNECTING_SUCC:
    		case DISCONNECTING_FAIL:
    			
    		case WAIT_REGISTER:
    		case REGISTER_SENDING:
    		case REGISTER_READING:
    		case REGISTER_POSTPROCESSING:
    		case REGISTER_FAIL:
    			break;
    		case REGISTER_SUCC:

	    		refreshThread.addCommand(SipcThread.Command.AUTHENTICATE, null);
	    		break;
        	
    		case WAIT_AUTHENTICATE:
    		case AUTHENTICATE_SENDING:
    		case AUTHENTICATE_READING:
    		case AUTHENTICATE_POSTPROCESSING:
    			break;
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
    		case AUTHENTICATE_SUCC:

	    		refreshThread.addCommand(SipcThread.Command.GET_CONTACTS, null);
	    		break;
    		case AUTHENTICATE_FAIL:
    			dismissDialog(DIALOG_REFRESH_PROGRESS);
				showerr(TAG, "authenticate failed. try again");
				break;
    		case WAIT_GET_CONTACT:
    		case CONTACT_GETTING:
    			break;
    		case CONTACT_GET_SUCC: {
				dismissDialog(DIALOG_REFRESH_PROGRESS);
				FetionDatabase.getInstance().setUserInfo(sysConfig);
				
				loadContactList();
				Iterator<String> iter = contactList.keySet().iterator();
        		while (iter.hasNext()) {
        			String uri = iter.next();
        			FetionContact c = contactList.get(uri);
        			FetionDatabase.getInstance().setContact(c);
        		}
        		try{
					FileOutputStream fos = openFileOutput(".nomedia", Context.MODE_PRIVATE);
					fos.write("1".getBytes(), 0, "1".getBytes().length);
					fos.close();
					//Debugger.d( "portrait " + c.sId + " wrote succeeded");
			    } catch(Exception e) {
			       //e.printStackTrace();
			       Debugger.e( ".nomedia mark wrote failed: " + e.getMessage());
			    }
				showerr(TAG, "Congratulations! contact list ok!!");
				break;
			}
    		case CONTACT_GET_FAIL:
    			dismissDialog(DIALOG_REFRESH_PROGRESS);
    			showerr(TAG, "Failed to get contact list");
    			break;
    		case WAIT_DROP:
    		case DROP_SENDING:
    		case DROP_READING:
    		case DROP_POSTPROCESSING:
    		case DROP_FAIL:
    		case DROP_SUCC:
    			break;
    		case THREAD_EXIT:
    			break;
    		case NETWORK_DOWN:
    			dismissDialog(DIALOG_REFRESH_PROGRESS);
    			showerr(TAG, "Pls check your network connection");
    			break;
    		case NETWORK_TIMEOUT:
    			dismissDialog(DIALOG_REFRESH_PROGRESS);
    			showerr(TAG, "Network connection timed out");
    			break;
			default:
				break;
			/*case PORTRAIT_GET_SUCC: {

				loadContactList();
				Iterator<String> iter = contactList.keySet().iterator();
        		while (iter.hasNext()) {
        			String uri = iter.next();
        			FetionContact c = contactList.get(uri);
        			//FetionDatabase.getInstance().setContact(c);
        			if (!c.portrait.equals("")) {
        				try{
        					FileOutputStream fos = openFileOutput(
        							c.userId + ".JPG", Context.MODE_PRIVATE);
        					fos.write(c.portrait.getBytes(), 0, c.portrait.getBytes().length);
        					//fw.flush();
        					fos.close();
        					Debugger.d( "portrait " + c.sId + " wrote succeeded");
        			    } catch(Exception e) {
        			       e.printStackTrace();
        			       Debugger.e( "portrait " + c.sId + " wrote failed: " + e.getMessage());
        			    }
        			} else {
        				Debugger.d( "portrait " + c.sId + " not exist");
        			}
        		}
				break;
			}*/
			}
		}
	}
    
    private class LoginUiHandler extends Handler {
    	@Override
        public void handleMessage(Message msg) 
		{
    		HttpThread.State state = (HttpThread.State)msg.obj;
			
			Debugger.v( "received reports of state: " + state.toString());
			
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

	    		loginThread.addCommand(Command.DOWNLOAD_CONFIG, null);
				break;	
			case CONFIG_DOWNLOAD_SUCC:
				dismissDialog(INTENT_ACC_SET_DIALOG);
				FetionDatabase.getInstance().setUserInfo(sysConfig);
				showerr(TAG, "Congratulations! Your ass is mine!!");
				
				showDialog(DIALOG_REFRESH_PROGRESS);
				sysConfig.contactVersion = "0";
	    		
				refreshThread.addCommand(SipcThread.Command.CONNECT_SIPC, null);
				break;
			case CONFIG_DOWNLOAD_FAIL:
				dismissDialog(INTENT_ACC_SET_DIALOG);
				showerr(TAG, "Your login failed");
				break;
			case NETWORK_DOWN:
				showerr(TAG, "Network error!!");
				break;
			default:
				break;
			}
			
		}
    }
    
    
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	Debugger.d( "QUICKFETION ONCREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //btnSend = (Button)findViewById(R.id.buttonSend);
        //editMsg = (EditText)findViewById(R.id.editMessage);
        //spinContacts = (Spinner)findViewById(R.id.spinContacts);
        lvContacts = (ListView)findViewById(R.id.lvContacts);
        

        Network.setActivity(this);
        
        /*if (!Network.isNetworkAvailable()) {
        	showerr(TAG, "Give me your WIFI/3G's ass. Or I get nothing to fuck");

        	finish();
        	return;
        }*/
        
        FetionDatabase.setInstance(this);
        SmsDbAdapter.setContext(this);
        
        //sysConfig = new SystemConfig();
        sysConfig = SystemConfig.getInstance();
        refreshUiHandler = new RefreshUiHandler();
        loginUiHandler = new LoginUiHandler();
        contactList = new LinkedHashMap<String, FetionContact>();
        crypto = Crypto.getInstance();
        
        loginThread = new HttpThread(sysConfig, loginUiHandler);
        refreshThread = new SipcThread(sysConfig, crypto,
				contactList, refreshUiHandler);

        loginThread.start();
        refreshThread.start();
        
        
        lvContacts.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> a, View v, int position, long id) 
        	{
        		Debugger.d( "lvcontacts onitemclick");
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
    	Debugger.d( "onActivityResult: " + requestCode + ", " + resultCode );
    	switch (requestCode) {
    	case INTENT_ACC_SET_DIALOG: {
    		if (resultCode == RESULT_OK) {
	    		Bundle bundle = data.getExtras();
	    		if (!bundle.containsKey("mobileno")) {
	    			Debugger.e( "not contain mobileno");
	    		}
	    		sysConfig.mobileNumber = bundle.getString("mobileno");
	    		sysConfig.userPassword = bundle.getString("passwd");
	    		lastLoginAcc = sysConfig.mobileNumber;
	    		if (sysConfig.mobileNumber == null || sysConfig.userPassword == null) {
	    			Debugger.e( "retrieving user account:" + 
	    					sysConfig.mobileNumber + ", " + sysConfig.userPassword);
	    		}
	
	    		showDialog(DIALOG_LOGIN_PROGRESS);
	    		
				loginThread.addCommand(Command.LOGIN, null);
	    		Debugger.v( "Dialog created and returned");
    		}
    		else {
    			Debugger.d( "dialog canceled");
    		}
    		break;
    	}
    	case INTENT_PIC_VERIFY_DIALOG_FOR_LOGIN: {
    		if (loginThread != null && (loginThread.state == HttpThread.State.LOGIN_NEED_CONFIRM))
    		{
    			switch (resultCode) {
    			case RESULT_OK: {
		    		Bundle bundle = data.getExtras();
		    		loginThread.verification.code = bundle.getString("code"); 
		    		loginThread.addCommand(Command.LOGIN, null);
		    		break;
    			}
		    	default:
		    		Debugger.e( "pic verify destroyed");
		    		dismissDialog(DIALOG_LOGIN_PROGRESS);
		    		break;
    			}
    		}
    		break;
    	}
    	case INTENT_PIC_VERIFY_DIALOG_FOR_AUTHENTICATE: {
    		Debugger.d( "refreshThread state = " + refreshThread.state.ordinal());
    		if (refreshThread != null && (refreshThread.state == SipcThread.State.AUTHENTICATE_NEED_CONFIRM)) {
    			switch (resultCode) {
    			case RESULT_OK: {
		    		Bundle bundle = data.getExtras();
		    		refreshThread.verification.code = bundle.getString("code"); 
		    		refreshThread.addCommand(SipcThread.Command.AUTHENTICATE, null);
		    		break;
    			}
		    	default:
		    		dismissDialog(DIALOG_REFRESH_PROGRESS);
		    		showerr(TAG,  "User gave up authentication");
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
    	Debugger.i( "QUICKFETION ONSTART");
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
    		Debugger.d( "SIPC = " + sysConfig.sipcProxyIp + ":" + sysConfig.sipcProxyPort);
    		if (sysConfig.sipcProxyIp == "" || sysConfig.sipcProxyPort == -1) {
    			//Debugger.e( "error getting sipc proxy from db");
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
			FetionContact c = contactList.get(uri);
			String nn = c.getDisplayName();
			
			String mn = c.mobileNumber;
			if (mn == null || mn.equals("")) {
				mn = getString(R.string.mobileno_invalid);
			}
			
			
		    HashMap<String, Object> map = new HashMap<String, Object>(); 
		    
		    String potraitfile = c.userId + ".JPG";
		    Debugger.d( "portrait filename is " + potraitfile);
		    FileInputStream fis = null; 
		    try {
		    	fis = openFileInput(potraitfile);
		    } catch (FileNotFoundException e) {
		    	//Debugger.e( "contact " + c.sipUri + " has no portrait");
		    }
		    if (fis == null) {
		    	
		    	map.put("FetionImage", R.drawable.icon);
		    	Debugger.d( "contact " + c.sipUri + " has no portrait");
		    } else {
		    	Bitmap bmp = BitmapFactory.decodeFile(potraitfile);
		    	ImageView i = new ImageView(this);
		    	i.setImageBitmap(bmp);
		    	//map.put("FetionImage", bmp);
		    	//map.put("FetionImage", i);
		    	//map.put("FetionImage", "file:///data/data/com.saturdaycoder.quickfetion/files/" + potraitfile);
		    	map.put("FetionImage", R.drawable.icon);
		    	Debugger.e( "contact " + c.sipUri + " HAS portrait");
		    }
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
    	Debugger.i( "QUICKFETION ONRESTART");
    	super.onRestart();
    }
    @Override
    protected void onResume()
    {
    	Debugger.i( "QUICKFETION ONRESUME");
    	super.onResume();

    }
    @Override
    protected void onPause()
    {
    	Debugger.i( "QUICKFETION ONPAUSE");
    	super.onPause();
    }
    @Override
    protected void onStop()
    {
    	Debugger.i( "QUICKFETION ONSTOP");
    	super.onStop();
    }
    @Override
    protected void onDestroy()
    {

    	Debugger.i( "QUICKFETION ONDESTROY");
    	super.onDestroy();
    	
    	if (loginThread.isAlive())
    		loginThread.stop();
    	
    	if (refreshThread.isAlive())
    		refreshThread.stop();
    	
    	try {
    		Network.closeSipcSocket();
    	} catch (Exception e) {
    		
    	}
    }
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    	Debugger.i( "QUICKFETION ONSAVEINSTANCESTATE");
    	super.onSaveInstanceState(outState);
    }
    @Override 
    public void onConfigurationChanged(Configuration newConfig) {
    	Debugger.i( "QUICKFETION ONCONFIGURATIONCHANGED: " + newConfig.toString());
    	super.onConfigurationChanged(newConfig); 

    }
    
    //@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
    	Debugger.d( "onMeasure(" + widthMeasureSpec + "," + heightMeasureSpec + ")");
    	
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
	    		if (!refreshThread.isAlive()) {
	    			refreshThread.start();
				}
	    		refreshThread.addCommand(SipcThread.Command.CONNECT_SIPC, null);
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
                dialog.setCancelable(false);
                return dialog;
            }
            case DIALOG_REFRESH_PROGRESS: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("Refresh");
                dialog.setMessage("Please wait while refreshing...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                return dialog;
            }
        }
        return null;
    }
    
    /*@Override 
    protected void onPrepareDialog(int id, Dialog dialog) { 
        switch(id){ 
        case DIALOG_LOGIN_PROGRESS: 
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){ 
                @Override 
                public void onDismiss(DialogInterface dialog) { 
                	showerr(TAG, "login is canceled");
                	//if (loginThread != null)
                	loginThread.stop();
                } 
            }); 
            break;
        case DIALOG_REFRESH_PROGRESS:
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){ 
                @Override 
                public void onDismiss(DialogInterface dialog) { 
                	showerr(TAG, "refresh is canceled");
                	if (refreshThread != null)
                		refreshThread.stop();
                	try {
                		Network.closeSipcSocket();
                	} catch (Exception e) {
                		Debugger.e( "error closing SIPC socket: " + e.getMessage());
                	}
                } 
            }); 
            break;
        default:
        	break;
        } 
    } */
}
        
    
    


