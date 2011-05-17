package com.saturdaycoder.easyfetion;
import android.app.*;
import android.media.*;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.os.*;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.text.format.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import android.telephony.gsm.SmsMessage;
import android.content.IntentFilter;
import android.os.Vibrator;

import com.saturdaycoder.easyfetion.SipcThread.Command;
import com.saturdaycoder.easyfetion.SipcThread.State;
import com.saturdaycoder.easyfetion.SipcThread.ThreadState;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.ContextMenu.*;
import android.view.Menu;
import android.text.ClipboardManager;
public class MsgHistory extends Activity
{
	private Intent intent;
	private Bundle bundle;
	private SmsReceiver receiver;
	private Button btnSend;
	private EditText editMsgText;
	private ListView lvMsgList;
	private String mobileno = null; 
	private String msgno = null;
	private Crypto crypto;
	private SystemConfig sysConfig;
	private Handler uiHandler;
	private SipcThread thread;
	private static final int INTENT_PIC_VERIFY_DIALOG = 1;
	private ArrayList<AndroidSms> smsList = null;
	private String nickname = null;
	private String sipuri = null;
	
	private boolean isSending = false;
	
	private Vibrator vb = null;
	private SoundPool sp = null;
	private int newsmshit = -1;
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.msghistoryactivity);
		//getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.contactlistitem);
		
		
		Debugger.warn("MsgListHistory.onCreate");
		
		if (!SmsDbAdapter.isInit())
			SmsDbAdapter.setContext(this);
		if (!FetionDatabase.isInit())
        	FetionDatabase.setInstance(this);
		
		sysConfig = SystemConfig.getInstance();
		if (sysConfig.sipcProxyIp.equals("")) {
	    	FetionDatabase.getInstance().getAccount(sysConfig);
	    	FetionDatabase.getInstance().getUserInfo(sysConfig);
	    	
	    	
		}
		
		intent = this.getIntent();
		bundle = intent.getExtras();
		mobileno = bundle.getString("mobileno");
		msgno = bundle.getString("msgno");
		nickname = bundle.getString("nickname");
		
		int inputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        inputMode=inputMode|WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
        getWindow().setSoftInputMode(inputMode);
		
		//TextView title = (TextView)findViewById(R.id.contactListItemName); 
		//title.setText(nickname);
		//ImageView icon = (ImageView)findViewById(R.id.contactListItemIcon);
		//icon.setImageResource(R.drawable.icon);
		//TextView number = (TextView)findViewById(R.id.contactListItemNumber); 
		//number.setText(msgno);
		
		
		this.setTitle(nickname);
		
		sipuri = bundle.getString("sipuri");
		
		btnSend = (Button)findViewById(R.id.btnSendMsg);
		editMsgText = (EditText)findViewById(R.id.editMsgText);
		String s = bundle.getString("msgtext");
		
		if (s != null) {
			editMsgText.setText(s);
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	    	Debugger.error("cancel notification STATUS BAR");
	    	nm.cancel(R.layout.msghistoryactivity);
		}
		
		lvMsgList = (ListView)findViewById(R.id.lvMsgList);
		
		
		crypto = Crypto.getInstance();
		
		receiver = new SmsReceiver();
		uiHandler = new SendMsgUiHandler();
		thread = new SipcThread(sysConfig, crypto, null, uiHandler);
		thread.start();
		
		// prepare notification
		vb = (Vibrator)getApplication().getSystemService(Service.VIBRATOR_SERVICE);
		sp = new SoundPool(1, AudioManager.STREAM_SYSTEM, 10);
		newsmshit = sp.load(this, R.raw.new_message, 0);
		//
		
		
		if (mobileno == null || nickname == null) {
			
			Debugger.debug( "can't get correct parameter");
			
			return;
		}
		
		btnSend.setOnClickListener(new Button.OnClickListener()
        {
        	//@Override
        	public void onClick(View v) {
        		if (editMsgText.getText().toString().equals(""))
        			return;
        		
        		isSending = true;
        		
        		btnSend.setClickable(false);
        		editMsgText.setEnabled(false);
        		
        		FetionMsg fm = new FetionMsg();
        		FetionContact fc = FetionDatabase.getInstance().getContactByUri(sipuri);
        		fm.contact = fc;
        		fm.msg = editMsgText.getText().toString();
        		thread.pendingMsg = true;
        		thread.addCommand(Command.CONNECT_SIPC, fm);
        	}
        });
		
		ListView.OnCreateContextMenuListener MenuLis=new ListView.OnCreateContextMenuListener(){
			  @Override
			  public void onCreateContextMenu(ContextMenu menu, View v,
					  ContextMenuInfo menuInfo) {
				  menu.add(Menu.NONE,Menu_Item1,0,"复制");
			  }
		};
		lvMsgList.setOnCreateContextMenuListener(MenuLis);
	}
	
	public boolean onContextItemSelected(MenuItem item){
		//关键代码在这里
        AdapterView.AdapterContextMenuInfo menuInfo;
        menuInfo =(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        //输出position
        
        smsList = SmsDbAdapter.getSmsList(mobileno);
        AndroidSms sms = smsList.get(menuInfo.position);
        Toast.makeText(MsgHistory.this, "消息已复制到剪贴板", 
    		 Toast.LENGTH_LONG).show();
        
        ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cm.setText (sms.body);
        editMsgText.requestFocus();
        return super.onContextItemSelected(item); 
 
    }
	protected static final int Menu_Item1=Menu.FIRST;
	
	private class SendMsgUiHandler extends Handler {
		@Override
        public void handleMessage(Message msg) 
		{
			ThreadState ss = (ThreadState)msg.obj;
			Debugger.debug( "sendmsgthread reports " + ss.state.toString());
			switch (ss.state) {
			case CONNECTING_SIPC: 
				break;
			case CONNECTING_SUCC: {
				FetionMsg fm = (FetionMsg)ss.arg;
				thread.addCommand(Command.REGISTER, fm);
				break;
			}
			case CONNECTING_FAIL: {
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify("不好意思，网络连接失败或超时，请重试。。。");
				showStatusBarNotification(fm);
				btnSend.setClickable(true);
				editMsgText.setText(fm.msg);
        		editMsgText.setEnabled(true);
        		isSending = false;
				break;
			}
			case DISCONNECTING_SIPC:
				break;
			case DISCONNECTING_SUCC:
			case DISCONNECTING_FAIL: {
				btnSend.setClickable(true);
				editMsgText.setEnabled(true);
				isSending = false;
				break;
			}
			case WAIT_REGISTER:
			case REGISTER_SENDING:
			case REGISTER_READING:
			case REGISTER_POSTPROCESSING:
				break;
			case REGISTER_FAIL: {
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify("给 " + fm.contact.nickName
						+ "的消息发送失败了 :(  请重试。。。");
				showStatusBarNotification(fm);
				thread.addCommand(Command.DISCONNECT_SIPC, fm);
				editMsgText.setText(fm.msg);
				isSending = false;
				break;
			}
			case REGISTER_SUCC: {
				FetionMsg fm = (FetionMsg)ss.arg;
				thread.addCommand(Command.AUTHENTICATE, fm);
				break;
			}
			case WAIT_AUTHENTICATE:
			case AUTHENTICATE_SENDING:
			case AUTHENTICATE_READING:
			case AUTHENTICATE_POSTPROCESSING:
				break;
			case AUTHENTICATE_NEED_CONFIRM:
				Intent intent = new Intent();
				intent.setClass(MsgHistory.this, PictureVerifyDialog.class);
				Bundle bundle = new Bundle();
				bundle.putByteArray("picture", thread.verification.getPicture());
				bundle.putString("text", thread.verification.text);
				bundle.putString("tips", thread.verification.tips);
				intent.putExtras(bundle);
				startActivityForResult(intent, INTENT_PIC_VERIFY_DIALOG);
				break;
			case AUTHENTICATE_SUCC: {
				FetionMsg fm = (FetionMsg)ss.arg;
				thread.addCommand(Command.SEND_MSG, fm);
				break;
			}
			case AUTHENTICATE_FAIL: {
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify("给 " + fm.contact.nickName
						+ "的消息发送失败了 :(  请重试。。。");
				showStatusBarNotification(fm);
				thread.addCommand(Command.DISCONNECT_SIPC, fm);
				editMsgText.setText(fm.msg);
				isSending = false;
				break;
			}
			case WAIT_SEND_MSG:
			case SEND_MSG_SENDING:
			case SEND_MSG_READING:
			case SEND_MSG_POSTPROCESSING:
				break;
			case SEND_MSG_SUCC_ONLINE: {
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify(fm.contact.nickName
						+ "的客户端成功接收了消息");
				loadMsgList();
				thread.addCommand(Command.DROP, fm);
				editMsgText.setText("");
				break;
			}
			case SEND_MSG_SUCC_SMS: {
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify(fm.contact.nickName
						+ "的手机成功接收了消息（短信形式）");
				loadMsgList();
				editMsgText.setText("");
				thread.addCommand(Command.DROP, fm);
				break;
			}
			case SEND_MSG_FAIL: {
				FetionMsg fm = (FetionMsg)ss.arg;
				loadMsgList();
				popNotify("给 " + fm.contact.nickName
						+ "的消息发送失败了 :(  请重试。。。");
				showStatusBarNotification(fm);
				thread.addCommand(Command.DROP, fm);
				editMsgText.setText(fm.msg);
				isSending = false;
				break;
			}
			case SEND_MSG_RESPONSE_TIMEOUT: {
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify("给 " + fm.contact.nickName
						+ "的消息成功发送给了服务器但没得到对方接收确认");
				thread.addCommand(Command.DROP, fm);
				editMsgText.setText(fm.msg);
				break;
			}
			case WAIT_DROP:
			case DROP_SENDING:
			case DROP_READING:
			case DROP_POSTPROCESSING:
				break;
			case DROP_FAIL:
			case DROP_SUCC: {
				FetionMsg fm = (FetionMsg)ss.arg;
				thread.addCommand(Command.DISCONNECT_SIPC, fm);
				break;
			}
			case NETWORK_DOWN:
			{
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify("不好意思，网络连接失败，请重试。。。");
				showStatusBarNotification(fm);
				btnSend.setClickable(true);
				editMsgText.setText(fm.msg);
        		editMsgText.setEnabled(true);
        		isSending = false;
				break;
			}
			case NETWORK_TIMEOUT:
			{
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify("不好意思，网络连接超时，请重试。。。");
				showStatusBarNotification(fm);
				btnSend.setClickable(true);
				editMsgText.setText(fm.msg);
        		editMsgText.setEnabled(true);
        		isSending = false;
				break;
			}
			case THREAD_EXIT: 
				//popNotify("程序完全退出");
				break;
			default:
				break;
			}
		}
	}
	
    private void popNotify(String msg)
    {
        Toast.makeText(MsgHistory.this, msg,
                Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onResume() {


    	loadMsgList();
    	editMsgText.requestFocus();
    	super.onResume();
    	
    }
    
    @Override
    protected void onPause() {

    	super.onPause();
    }
	
	@Override
	protected void onStop() {
    	unregisterReceiver(receiver);
    	Debugger.debug( "RECEIVER UNREGISTERED");
    	super.onStop();
	}
	
	protected void showStatusBarNotification(FetionMsg fm) {
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		Notification notification = new Notification(R.drawable.icon, "发送信息失败",
                System.currentTimeMillis());
		
		PendingIntent contentIntent = null;
		if (fm != null) {
			contentIntent = PendingIntent.getActivity(this, 0,
						new Intent(this, MsgHistory.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("mobileno", fm.contact.getSmsNumber())
                        .putExtra("msgno", fm.contact.getMsgNumber())
                        .putExtra("nickname", fm.contact.getDisplayName())
                        .putExtra("msgtext", fm.msg)
                        .putExtra("sipuri", fm.contact.sipUri),
                        PendingIntent.FLAG_UPDATE_CURRENT);
				
	        notification.setLatestEventInfo(this, "发送给" + fm.contact.getDisplayName() + "的信息发送失败",
	                       "飞信随手发", contentIntent);
	
	        nm.notify(R.layout.msghistoryactivity, notification);
		}
		else {
			
		}
	}
	
	@Override
	protected void onDestroy() {
    	//unregisterReceiver(receiver);
    	//Debugger.d( "RECEIVER UNREGISTERED");
		//thread.addCommand(Command.DROP, null);
		//thread.addCommand(Command.DISCONNECT_SIPC, null);
		//thread.addCommand(Command.EXIT_AFTER_SEND, null);
    	super.onDestroy();
	}
	
	/*@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {

		if (keyCode == KeyEvent.KEYCODE_BACK && isSending) {
			return true;
		}
		else {
			return super.onKeyDown(keyCode, msg);
		}
	}*/
	
	@Override
	protected void onStart() {
		super.onStart();
		
		Debugger.debug( "loading sms list of " + mobileno);
		
    	IntentFilter filter = new IntentFilter();
    	filter.addAction("android.provider.Telephony.SMS_RECEIVED");
    	filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
    	registerReceiver(receiver, filter);
    	Debugger.debug( "RECEIVER REGISTERED");
		
		
		loadMsgList();
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	switch (requestCode) {
    	case INTENT_PIC_VERIFY_DIALOG: {
    		if (resultCode == RESULT_OK) {
    			Bundle bundle = data.getExtras();
	    		thread.verification.code = bundle.getString("code"); 
    			thread.addCommand(Command.AUTHENTICATE, null);
    		}
    		break;
    	}
    	
    	default: 
    		break;
    	}
    }
	
	private void loadMsgList() 
	{

		smsList = SmsDbAdapter.getSmsList(mobileno);
		ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>(); 
		ArrayList<Integer> backcolor = new ArrayList<Integer>();
		
		for (int i = 0; i < smsList.size(); ++i) {
			int type = smsList.get(i).type;
			long date = smsList.get(i).date;
			String body = smsList.get(i).body;
			
			String smstext = ((type == 1)? "": "我: " )
					+body;
			String timetext = DateFormat.format("yy-MM-dd kk:mm:ss", date).toString();
			
		    HashMap<String, Object> map = new HashMap<String, Object>(); 
		    
		    map.put("MsgText", smstext); 
		    map.put("TimeText", timetext); 
		    listItem.add(map); 
		    
		    backcolor.add((type == 1)? R.drawable.skyblue: R.drawable.grey);
		} 
		MsgListAdapter listItemAdapter = new MsgListAdapter(MsgHistory.this,
			listItem,
			
			
			backcolor,
			
		    R.layout.msglistitem,
		            
		    new String[] {
				"MsgText", 
				"TimeText"},  
		    
		    new int[] {
				R.id.msgListItemBody,
				R.id.msgListItemTime} 
		); 
		
		
		lvMsgList.setAdapter(listItemAdapter);  
	}
	
	public class SmsReceiver extends BroadcastReceiver 
	{
		private static final String mReceiveAction = "android.provider.Telephony.SMS_RECEIVED";
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Debugger.debug( "SmsReceiver.onReceive");
			if (intent.getAction().equals(mReceiveAction)) 
			{
				Debugger.debug( "SmsReceiver.onReceive SMS_RECEIVED");
				
				//StringBuilder sb = new StringBuilder();
				
				Bundle bundle = intent.getExtras();
				
				
				boolean hasThisContact = false;
				boolean hasOtherContact = false;
				if (bundle != null) {
					Object[] objpdus = (Object[])bundle.get("pdus");
					
					ArrayList<Object> newobjpdus = new ArrayList<Object>();
					for (int i = 0; i < objpdus.length; ++i) {
						SmsMessage sms = SmsMessage.createFromPdu((byte[])objpdus[i]);
						String addr = sms.getOriginatingAddress();
						long date = sms.getTimestampMillis();
						String body = sms.getMessageBody();
						Debugger.debug( "received sms from " + addr);
						if (/*addr.equals(mobileno) || */addr.equals(msgno)) {
							hasThisContact = true;

							SmsDbAdapter.insertReceivedSms(mobileno, date, body);
							try {
								vb.vibrate(new long[] {
										100, 10, 100, 1000	
										}, -1);
								 sp.play(newsmshit, 2, 1, 0, 0, (float)1.0);
								
							} catch (Exception e) {
								Debugger.error( "can not play sound:" + e.getMessage());
							}
					    }
						else {
							hasOtherContact = true;
						}
					}

				}
				
				if (hasThisContact) {
					loadMsgList();
				}
				if (!hasOtherContact) {
					Debugger.error( "do not stop sms propagation because of other contact");
					abortBroadcast();
			        setResultData(null);
				}
			}
		}
	}
}
