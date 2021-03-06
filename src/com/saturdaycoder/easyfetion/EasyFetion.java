package com.saturdaycoder.easyfetion;
import java.io.*;
import android.view.WindowManager;
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
import android.telephony.TelephonyManager;
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
import android.net.*;

import com.saturdaycoder.easyfetion.HttpThread.Command;
import com.saturdaycoder.easyfetion.HttpThread.State;

import android.os.Handler;
import android.content.res.*;
import android.content.*;
public class EasyFetion extends Activity 
{
	private static String TAG = "EasyFetion";
	
	private String lastLoginAcc = "";

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
    private HttpThread loginThread;
    private SipcThread refreshThread;
    //private boolean needClearContactDb = false;
    private boolean clearContactDbOnLoginSucc = false;
    
    private void showerr(String TAG, String msg)
    {
    	popNotify(msg);
    	Debugger.error( msg);
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
    		SipcThread.ThreadState ts = (SipcThread.ThreadState)msg.obj;
    		SipcThread.State state = ts.state;
    		Debugger.verbose( "received reports of state: " + state.toString());
    		switch (state) {
    		case INIT:
    		case CONNECTING_SIPC:
    			break;
    		case CONNECTING_SUCC:
	    		refreshThread.addCommand(SipcThread.Command.REGISTER, null);
	    		break;
    		case CONNECTING_FAIL:
    			try {
    				dismissDialog(DIALOG_REFRESH_PROGRESS);
    			} catch (Exception e) {}
    			showerr(TAG, "网络连接出错。检查一下网络连接吧。");
    			break;
    		case DISCONNECTING_SIPC:
    		case DISCONNECTING_SUCC:
    		case DISCONNECTING_FAIL:
    			break;
    		case WAIT_REGISTER:
    		case REGISTER_SENDING:
    		case REGISTER_READING:
    		case REGISTER_POSTPROCESSING:
    			break;
    		case REGISTER_FAIL:
    			try {
					dismissDialog(DIALOG_REFRESH_PROGRESS);
				} catch (Exception e) {}
    			refreshThread.addCommand(SipcThread.Command.DISCONNECT_SIPC, null);
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
    		case AUTHENTICATE_SUCC: {
    			FetionDatabase.getInstance().clearContacts();
    			Iterator<String> iter = contactList.keySet().iterator();
        		while (iter.hasNext()) {
        			String uri = iter.next();
        			FetionContact c = contactList.get(uri);
        			FetionDatabase.getInstance().setContact(c);
        		}
        		loadContactList();
	    		refreshThread.addCommand(SipcThread.Command.GET_CONTACTS, null);
	    		break;
    		}
    		case AUTHENTICATE_FAIL: {
    			refreshThread.addCommand(SipcThread.Command.DISCONNECT_SIPC, null);
    			try {
					dismissDialog(DIALOG_REFRESH_PROGRESS);
				} catch (Exception e) {}
				showerr(TAG, "账号验证失败了。。。");
				break;
    		}
    		case WAIT_GET_CONTACT:
    		case CONTACT_GETTING:
    			break;
    		case CONTACT_GET_SUCC: {
				loginThread.addCommand(HttpThread.Command.GET_PORTRAIT, contactList);
				
				loadContactList();
				dismissDialog(DIALOG_REFRESH_PROGRESS);
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
			       Debugger.error( ".nomedia mark wrote failed: " + e.getMessage());
			    }
				showerr(TAG, "成功地获取到联系人列表啦！");
				break;
			}
    		case CONTACT_GET_FAIL:
    			refreshThread.addCommand(SipcThread.Command.DROP, null);
    			try {
    				dismissDialog(DIALOG_REFRESH_PROGRESS);
    			} catch (Exception e) {}
    			showerr(TAG, "获取联系人列表出错。");
    			break;
    		case WAIT_DROP:
    		case DROP_SENDING:
    		case DROP_READING:
    		case DROP_POSTPROCESSING:
    			break;
    		case DROP_FAIL:
    		case DROP_SUCC:
    			refreshThread.addCommand(SipcThread.Command.DISCONNECT_SIPC, null);
    			break;
    		case THREAD_EXIT:
    			break;
    		case NETWORK_DOWN:
    			try {
    				dismissDialog(DIALOG_REFRESH_PROGRESS);
    			} catch (Exception e) {}
    			showerr(TAG, "网络连接出错。检查一下网络连接吧。");
    			break;
    		case NETWORK_TIMEOUT:
    			try {
    				dismissDialog(DIALOG_REFRESH_PROGRESS);
    			} catch (Exception e) {}
    			showerr(TAG, "网络超时了。。。。。。");
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
    		HttpThread.State state = (HttpThread.State)msg.obj;
			
			Debugger.verbose( "received reports of state: " + state.toString());
			
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
				clearContactDbOnLoginSucc = false;
				// Roll back the saved account
				FetionDatabase.getInstance().getAccount(sysConfig);
				FetionDatabase.getInstance().getUserInfo(sysConfig);
				FetionContact contacts[] = FetionDatabase.getInstance().getContacts();
				loadContactList();
    			for (FetionContact c: contacts) {
    				contactList.put(c.sipUri, c);
    			}
    			loadContactList();
				
				try {
					dismissDialog(DIALOG_LOGIN_PROGRESS);
					showerr(TAG, "登录失败了，检查一下账号和密码有没有写错？");
				} catch (Exception e) {
					
				}
				try {
					dismissDialog(DIALOG_REFRESH_PROGRESS);
					showerr(TAG, "登录失败了，检查一下账号和密码有没有写错？");
				} catch (Exception e) {
					
				}
				break;
			case LOGIN_SUCC: 
				FetionDatabase.getInstance().setAccount(sysConfig);
				FetionDatabase.getInstance().clearUserInfo();
				if (clearContactDbOnLoginSucc) {
					FetionDatabase.getInstance().clearContacts();
				}
				clearContactDbOnLoginSucc = false;
	    		loginThread.addCommand(Command.DOWNLOAD_CONFIG, null);
				break;	
			case CONFIG_DOWNLOAD_SUCC:
				try {
					dismissDialog(DIALOG_LOGIN_PROGRESS);
					showerr(TAG, "账号配置已经成功下载完毕");
				} catch (IllegalArgumentException e) {
					
				}
				FetionDatabase.getInstance().setUserInfo(sysConfig);
				
				showDialog(DIALOG_REFRESH_PROGRESS);
				sysConfig.contactVersion = "0";
	    		
				refreshThread.addCommand(SipcThread.Command.CONNECT_SIPC, null);
				break;
			case CONFIG_DOWNLOAD_FAIL:
				//FetionDatabase.getInstance().setUserInfo(sysConfig);
				try { 
					dismissDialog(DIALOG_LOGIN_PROGRESS);
				} catch (Exception e) {}
				try {
					dismissDialog(DIALOG_REFRESH_PROGRESS);
				} catch (Exception e) {}
				showerr(TAG, "账号配置获取失败，请重试。。。");
				break;
			case NETWORK_DOWN:
				try { 
					dismissDialog(DIALOG_LOGIN_PROGRESS);
				} catch (Exception e) {}
				try {
					dismissDialog(DIALOG_REFRESH_PROGRESS);
				} catch (Exception e) {}
				showerr(TAG, "网络连接出错。检查一下网络连接吧。");
				break;
			case GET_PORTRAIT_FAIL:
				refreshThread.addCommand(SipcThread.Command.DROP, null);
				showerr(TAG, "联系人头像下载出错。。。");
				break;
			case GET_PORTRAIT_SUCC:
				refreshThread.addCommand(SipcThread.Command.DROP, null);
				loadContactList();
				showerr(TAG, "联系人头像下载完毕！");
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
    	Debugger.debug( "QUICKFETION ONCREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        lvContacts = (ListView)findViewById(R.id.lvContacts);
 
        Network.setActivity(this);
        
        if (!FetionDatabase.isInit())
        	FetionDatabase.setInstance(this);
        if (!SmsDbAdapter.isInit())
        	SmsDbAdapter.setContext(this);
        
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
        
        int inputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        inputMode=inputMode|WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
        getWindow().setSoftInputMode(inputMode);
        
        
        lvContacts.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> a, View v, int position, long id) 
        	{
        		Debugger.debug( "lvcontacts onitemclick");
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
    			bundle.putString("msgno", contactList.get(uri).getMsgNumber());
    			bundle.putString("nickname", contactList.get(uri).getDisplayName());
    			bundle.putString("sipuri", contactList.get(uri).sipUri);
    			intent.putExtras(bundle);
    			startActivity(intent);
        	}
        });
        
    	sysConfig = SystemConfig.getInstance();
    	
    	FetionDatabase.getInstance().getAccount(sysConfig);
    	
    	if (sysConfig.sId == "") {
    		lastLoginAcc = "";
        	Intent intent = new Intent();
			intent.setClass(EasyFetion.this, AccountSettingDialog.class);
			Bundle bundle = new Bundle();
			bundle.putString("lastlogin", lastLoginAcc);
			bundle.putInt("reason", 0);
			intent.putExtras(bundle);
			startActivityForResult(intent, INTENT_ACC_SET_DIALOG);
    	}
    	else {
    		lastLoginAcc = sysConfig.mobileNumber;
    		FetionDatabase.getInstance().getUserInfo(sysConfig);
    		Debugger.debug( "SIPC = " + sysConfig.sipcProxyIp + ":" + sysConfig.sipcProxyPort);
    		if (sysConfig.sipcProxyIp == "" || sysConfig.sipcProxyPort == -1) {

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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	Debugger.debug( "onActivityResult: " + requestCode + ", " + resultCode );
    	switch (requestCode) {
    	case INTENT_ACC_SET_DIALOG: {
    		if (resultCode == RESULT_OK) {
	    		Bundle bundle = data.getExtras();
	    		if (!bundle.containsKey("mobileno")) {
	    			Debugger.error( "not contain mobileno");
	    		}
	    		
	    		
	    		String m = bundle.getString("mobileno");
	    		String p = bundle.getString("passwd");
	    		lastLoginAcc = m;
	    		
	    		if (!m.equals(sysConfig.mobileNumber)) {
	    			clearContactDbOnLoginSucc = true;
	    		} else {
	    			clearContactDbOnLoginSucc = false;
	    		}
	    		
	    		sysConfig.reset();
	    		sysConfig.mobileNumber = m;
	    		sysConfig.userPassword = p;
				contactList.clear();
				loadContactList();
	    		
	    		if (sysConfig.mobileNumber == null || sysConfig.userPassword == null) {
	    			Debugger.error( "retrieving user account:" + 
	    					sysConfig.mobileNumber + ", " + sysConfig.userPassword);
	    		}
	
	    		showDialog(DIALOG_LOGIN_PROGRESS);
	    		
				loginThread.addCommand(Command.LOGIN, null);
	    		Debugger.verbose( "Dialog created and returned");
    		}
    		else {
    			Debugger.debug( "dialog canceled");
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
		    		Debugger.error( "pic verify destroyed");
		    		dismissDialog(DIALOG_LOGIN_PROGRESS);
		    		break;
    			}
    		}
    		break;
    	}
    	case INTENT_PIC_VERIFY_DIALOG_FOR_AUTHENTICATE: {
    		Debugger.debug( "refreshThread state = " + refreshThread.state.ordinal());
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
		    		showerr(TAG,  "用户中止了账号验证");
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
    	Debugger.info( "QUICKFETION ONSTART");
    	super.onStart();
    	

    }
    
    private void loadContactList() {
    	ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>(); 
        int reslist[] = new int[] {
        	//R.drawable.contact_default,
            R.drawable.contact_photo_1,
            R.drawable.contact_photo_2,
            R.drawable.contact_photo_3,
            R.drawable.contact_photo_4,
            
        };
        int photoindex = 0;
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
		    FileInputStream fis = null; 
		    try {
		    	fis = openFileInput(potraitfile);
		    } catch (FileNotFoundException e) {
		    }
		    if (fis == null) {
		    	//map.put("FetionImage", reslist[Integer.parseInt(c.userId) % reslist.length ]);
		    	map.put("FetionImage", reslist[photoindex]);
                photoindex = (photoindex + 1) % reslist.length;
		    } else {
		    	try {
		    		map.put("FetionImage", "/data/data/com.saturdaycoder.easyfetion/files/" + potraitfile);
		    	} catch (Exception e) {
		    		//map.put("FetionImage", R.drawable.contact_default);
		    		map.put("FetionImage", reslist[photoindex]);
	                photoindex = (photoindex + 1) % reslist.length;
		    	}
		    }
		    map.put("FetionNickName", nn + "(" + mn + ")"); 
		    map.put("FetionMobileNo", c.impression); 
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
    	Debugger.info( "QUICKFETION ONRESTART");
    	super.onRestart();
    }
    @Override
    protected void onResume()
    {
    	Debugger.info( "QUICKFETION ONRESUME");
    	super.onResume();

    }
    @Override
    protected void onPause()
    {
    	Debugger.info( "QUICKFETION ONPAUSE");
    	super.onPause();
    }
    @Override
    protected void onStop()
    {
    	Debugger.info( "QUICKFETION ONSTOP");
    	super.onStop();
    }
    @Override 
	public void onConfigurationChanged(Configuration newConfig) {
		Debugger.info( "QUICKFETION ONCONFIGURATIONCHANGED: " + newConfig.toString());
		super.onConfigurationChanged(newConfig); 
	
	}

	@Override
    protected void onDestroy()
    {

    	Debugger.info( "QUICKFETION ONDESTROY");
    	super.onDestroy();
    	
    	loginThread.stop();
    	
    	refreshThread.stop();
    	
    	try {
    	} catch (Exception e) {
    	}
    }
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    	Debugger.info( "QUICKFETION ONSAVEINSTANCESTATE");
    	super.onSaveInstanceState(outState);
    }
    //@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
    	Debugger.debug( "onMeasure(" + widthMeasureSpec + "," + heightMeasureSpec + ")");
    	
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
				bundle.putInt("reason", 1);
				intent.putExtras(bundle);
				
				/*sysConfig.mobileNumber = "";
				sysConfig.userPassword = "";
				sysConfig.sId = "";
				sysConfig.userId = "";*/
				
				
				startActivityForResult(intent, INTENT_ACC_SET_DIALOG);
                break;  
            case MENU_REFRESH_ID:  
            	clearContactDbOnLoginSucc = false;
            	String m = sysConfig.mobileNumber;
            	String p = sysConfig.userPassword;
            	sysConfig.reset();
            	sysConfig.mobileNumber = m;
            	sysConfig.userPassword = p;
				contactList.clear();
				loadContactList();
				
				showDialog(DIALOG_REFRESH_PROGRESS);
				
	    		//sysConfig.contactVersion = "0";
	    		
				loginThread.addCommand(Command.LOGIN, null);
                break;  
            case MENU_ABOUT_ID:  
                showerr(TAG, "这是一个为了方便发送飞信的小程序。每次发送完飞信消息就会离线，以节省流量。");  
                break;  
        }  
        return super.onOptionsItemSelected(aMenuItem);
    }  
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_LOGIN_PROGRESS: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("登录");
                dialog.setMessage("请稍候。。。");
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                return dialog;
            }
            case DIALOG_REFRESH_PROGRESS: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("刷新列表");
                dialog.setMessage("请稍候。。。");
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
        
    
    


