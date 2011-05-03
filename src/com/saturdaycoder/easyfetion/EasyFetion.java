package com.saturdaycoder.easyfetion;
import java.io.*;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import java.net.*;
import android.app.Activity;
import android.app.NotificationManager;
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

import com.saturdaycoder.easyfetion.EasyFetionThread.State;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Looper;
import android.content.res.*;

public class EasyFetion extends Activity 
{
	private static String TAG = "EasyFetion";
	
	private Button btnSend;
	private EditText editMsg;
	//private Spinner spinContacts;
	private ListView lvSelContact;
	private ListView lvContacts;
    private TextView textSelContact;
    private Handler uiHandler;

    final EasyFetionThread worker = new EasyFetionThread();;
    
    private ArrayList<FetionContact> selectedContacts = new ArrayList<FetionContact>();
    
    private void showerr(String TAG, String msg)
    {
    	popNotify(msg);
    	Log.e(TAG, msg);
    }
    
    private void popNotify(String msg)
    {
        Toast.makeText(EasyFetion.this, msg,
                Toast.LENGTH_SHORT).show();
    }
    private void statusbarNotify(String msg)
    {
    	
    }
    
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	Log.d(TAG, "QUICKFETION ONCREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btnSend = (Button)findViewById(R.id.buttonSend);
        editMsg = (EditText)findViewById(R.id.editMessage);
        //spinContacts = (Spinner)findViewById(R.id.spinContacts);
        lvContacts = (ListView)findViewById(R.id.lvContacts);
        textSelContact = (TextView)findViewById(R.id.textSelContact);
        
        
        
        
        
        
        
        Log.d(TAG, "prepare to get wifi manager");
        
        try {
        	WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (wifi == null) {
            	Log.e(TAG, "no wifi manager");
            	return;
            }
            
            WifiInfo info = wifi.getConnectionInfo();
            if (info == null) {
            	showerr(TAG, "no wifi info");
            }
            Network.macAddr = info.getMacAddress().replace(":", "");
            Log.d(TAG, "wifi mac = " + Network.macAddr);
        } catch (Exception e) {
        	Log.d(TAG, "calling getSystemService failed with: " + e.getMessage());
        }
        
        
		/*spinContacts.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override 
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				//Toast.makeText(main.this, Value[arg2],10).show();
				showerr(TAG, "selected contact " + worker.contactList.get(arg2).nickName);
				selectedContacts.clear();
				selectedContacts.add(worker.contactList.get(arg2));
			}
			
 
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				//Toast.makeText(main.this, "没选中",10).show();
				showerr(TAG, "no contact selected");
				selectedContacts.clear();
			}
 
		});*/
        lvContacts.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> a, View v, int position, long id) 
        	{
        		selectedContacts.clear();
        		Iterator<String> iter = worker.contactList.keySet().iterator();
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
        		selectedContacts.add(worker.contactList.get(uri));
        		showerr(TAG, "selected contact " + worker.contactList.get(uri).nickName);
        		EasyFetion.this.textSelContact.setText("选定联系人为：" + worker.contactList.get(uri).nickName);
        	}
        });
        
        //uiHandler);
        uiHandler = new Handler() 
        {
			@Override
            public void handleMessage(Message msg) 
			{
				EasyFetionThread.ThreadState ts = (EasyFetionThread.ThreadState)msg.obj;
				EasyFetionThread.State state = ts.state;
				FetionMsg fm = (FetionMsg)ts.arg;
				Log.d(TAG, "work thread reports " + state.toString());
				
				Intent intent;
				Bundle bundle;
				
				try {
					switch (state) 
					{
					case THREAD_EXIT:
						EasyFetion.this.finish();
						break;
					case WAIT_LOGIN: // popup dialog for input acc/pw
						intent = new Intent();
						intent.setClass(EasyFetion.this, AccountSettingDialog.class);
						bundle = new Bundle();
						intent.putExtras(bundle);
						startActivityForResult(intent, 0);

						break;
					case LOGIN_NEED_CONFIRM:
					case AUTHENTICATE_NEED_CONFIRM:
						intent = new Intent();
						intent.setClass(EasyFetion.this, PictureVerifyDialog.class);
						bundle = new Bundle();
						bundle.putByteArray("picture", worker.verification.getPicture());
						intent.putExtras(bundle);
						startActivityForResult(intent, 1);
						//showerr(TAG, "login needs picture verification which is not supported now, sorry");
						break;
					case LOGIN_FAIL: // input acc/pw again
						break;
					case LOGIN_SUCC: // save acc/pw into db
						break;
					case MSG_TRANSFERED:
						showerr(TAG, "message to " + fm.contact.nickName + " was successfully transfered");
						break;
					case MSG_FAILED:
						showerr(TAG, "message to " + fm.contact.nickName + " was failed");
						break;
					

					case CONTACT_GET_SUCC:
						/*ArrayList<String> strlist = new ArrayList<String>();
						for (int i = 0; i < worker.contactList.size(); ++i) {
							String nn = worker.contactList.get(i).nickName;
							if (nn == null || nn.equals("")) {
								nn = new String("无昵称");
							} 
							String mn = worker.contactList.get(i).mobileNumber;
							if (mn == null || mn.equals("")) {
								mn = new String("无手机号码");
							}
							
							strlist.add(nn + "(" + mn + ")");
						}
						FetionContact contacts[] = new FetionContact[worker.contactList.size()];
						contacts = worker.contactList.toArray(contacts);
						String strs[] = new String[strlist.size()];
						strs = strlist.toArray(strs);
						Log.d(TAG, "create adapter");
						ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				                EasyFetion.this, 
				                android.R.layout.simple_spinner_item,
				                strs);
				        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				        spinContacts.setAdapter(adapter);
						Log.d(TAG, "spinContacts set " + strlist.size() + " items");
*/
						
						// set list view
						ArrayList<HashMap<String, Object>> listItem 
								= new ArrayList<HashMap<String, Object>>(); 
				        //for(int i=0;i<worker.contactList.size();i++)
						Iterator<String> iter = worker.contactList.keySet().iterator();
						while(iter.hasNext())
				        { 
							String uri = iter.next();
							String nn = worker.contactList.get(uri).nickName;
							if (nn == null || nn.equals("")) {
								nn = worker.contactList.get(uri).localName;
								if (nn == null || nn.equals("")) {	
									nn = new String("无昵称");
								}
							} 
							String mn = worker.contactList.get(uri).mobileNumber;
							if (mn == null || mn.equals("")) {
								mn = new String("无手机号码");
							}
							
							
				            HashMap<String, Object> map = new HashMap<String, Object>(); 
				            map.put("FetionImage", R.drawable.icon); 
				            map.put("FetionNickName", nn); 
				            map.put("FetionMobileNo", mn); 
				            listItem.add(map); 
				        } 
				        //生成适配器的Item和动态数组对应的元素 
				        SimpleAdapter listItemAdapter = new SimpleAdapter(EasyFetion.this,
				        	listItem,//数据源  
				            R.layout.listview,//ListItem的XML实现 
				            //动态数组与ImageItem对应的子项         
				            new String[] {"FetionImage",
				        		"FetionNickName", 
				        		"FetionMobileNo"},  
				            //ImageItem的XML文件里面的一个ImageView,两个TextView ID 
				            new int[] {R.id.FetionImage,
				        		R.id.FetionNickName,
				        		R.id.FetionMobileNo} 
				        ); 
				        
				        //添加并且显示 
				        lvContacts.setAdapter(listItemAdapter);  
						break;
					default:
						break;
					}
				} catch (Exception e) {
					Log.e(TAG, "error filling contact list: " + e.getMessage());
				}
			}
        };
        
        
        
        btnSend.setOnClickListener(new Button.OnClickListener()
        {
        	@Override
        	public void onClick(View v) {
        		
        		String text = editMsg.getText().toString();
        		if (text.equals(""))
        			return;
        		
        		if (selectedContacts.size() == 0)
        			return;
        		
        		for (int i = 0; i < selectedContacts.size(); ++i) {
        			FetionMsg msg = new FetionMsg();
        			msg.contact = selectedContacts.get(i);
        			msg.msg = text;
        			msg.timestamp = System.currentTimeMillis();
        			worker.pendingSmsQueue.add(msg);
        			Log.d(TAG, "added new sms to pending queue");
        		}
        		
        		if (worker.state == State.WAIT_MSG) {
        			
        			synchronized(worker) 
    				{
        				Log.d(TAG, "awake work thread");
    					// set verification code
    					worker.notify();
    				}
    		        	
        		
        		} 
        		
				
        	}
        });
        
        worker.init(uiHandler);
        worker.smsDbWriter = new SmsDbWriter(this);
        worker.fetionDb = new FetionDatabase(this);
        worker.start();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	switch (requestCode) {
    	case 0: // from AccountSettingDialog
    		if (worker.state == State.WAIT_LOGIN) {
    			switch (resultCode) {
    			case RESULT_OK:
		    		Bundle bundle = data.getExtras();
		    		worker.sysConfig.mobileNumber = bundle.getString("mobileno");
		    		worker.sysConfig.userPassword = bundle.getString("passwd");
		    		Log.d(TAG, "input user passwd is " + worker.sysConfig.userPassword);
		    		synchronized(worker) {
		    			worker.notify();
		    		}
		    		break;
		    	default:
		    		Log.e(TAG, "setting dialog destroyed");
		    		worker.stop();
		    		break;
    			}
    		}
    		break;
    	case 1: // from PictureVerifyDialog
    		if (worker.state == State.LOGIN_NEED_CONFIRM
    				|| worker.state == State.AUTHENTICATE_NEED_CONFIRM)
    		{
    			switch (resultCode) {
    			case RESULT_OK:
		    		Bundle bundle = data.getExtras();
		    		worker.verification.code = bundle.getString("code"); 
		    		synchronized(worker) {
		    			worker.notify();
		    		}
		    		break;
		    	default:
		    		Log.e(TAG, "pic verify destroyed");
		    		worker.stop();
		    		break;
    			}
    		}
    		break;
    	default: 
    		break;
    	}
    }
    
    @Override
    protected void onStart()
    {
    	Log.i(TAG, "QUICKFETION ONSTART");
    	super.onStart();
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
    	//Log.i(TAG, "worker thread state is " + worker.isInterrupted());
    	worker.toBeExited = true;
    	if (worker.state == State.WAIT_MSG) {
    		Log.d(TAG, "tell a waiting thread to exit");
    		synchronized(worker) {
    			worker.notify();
    		}
    	}
    	else {
    		Log.d(TAG, "tell a running thread to exit");
    	}
    	Log.i(TAG, "QUICKFETION ONDESTROY");
    	super.onDestroy();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    	Log.i(TAG, "QUICKFETION ONSAVEINSTANCESTATE");
    }
    @Override 
    public void onConfigurationChanged(Configuration newConfig) {
    	Log.i(TAG, "QUICKFETION ONCONFIGURATIONCHANGED");
    	super.onConfigurationChanged(newConfig); 

    } 
}
        
    
    


