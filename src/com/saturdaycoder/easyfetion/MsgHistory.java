package com.saturdaycoder.easyfetion;
import android.app.*;
import android.media.*;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.os.*;
import android.view.View;
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
import com.saturdaycoder.easyfetion.SendMsgThread.Command;


public class MsgHistory extends Activity
{
	private Intent intent;
	private Bundle bundle;
	private SmsReceiver receiver;
	private Button btnSend;
	private EditText editMsgText;
	private ListView lvMsgList;
	private String mobileno = null; 
	private Crypto crypto;
	private SystemConfig sysConfig;
	private Handler uiHandler;
	private SendMsgThread thread;
	private static final int INTENT_PIC_VERIFY_DIALOG = 1;
	private ArrayList<AndroidSms> smsList = null;
	private String nickname = null;
	private String sipuri = null;
	
	private Vibrator vb = null;
	private SoundPool sp = null;
	private int newsmshit = -1;
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.msghistoryactivity);
		
		intent = this.getIntent();
		bundle = intent.getExtras();
		mobileno = bundle.getString("mobileno");
		nickname = bundle.getString("nickname");
		sipuri = bundle.getString("sipuri");
		
		btnSend = (Button)findViewById(R.id.btnSendMsg);
		editMsgText = (EditText)findViewById(R.id.editMsgText);
		lvMsgList = (ListView)findViewById(R.id.lvMsgList);
		
		receiver = new SmsReceiver();
		
		
		// prepare notification
		vb = (Vibrator)getApplication().getSystemService(Service.VIBRATOR_SERVICE);
		sp = new SoundPool(1, AudioManager.STREAM_SYSTEM, 10);
		newsmshit = sp.load(this, R.raw.new_message, 0);
		//
		
		
		if (mobileno == null || nickname == null) {
			
			Debugger.d( "can't get correct parameter");
			
			return;
		}
		
		btnSend.setOnClickListener(new Button.OnClickListener()
        {
        	//@Override
        	public void onClick(View v) {
        		if (editMsgText.getText().toString().equals(""))
        			return;
        		
        		btnSend.setClickable(false);
        		editMsgText.setEnabled(false);
        		
        		FetionMsg fm = new FetionMsg();
        		FetionContact fc = FetionDatabase.getInstance().getContactByUri(sipuri);
        		fm.contact = fc;
        		fm.msg = editMsgText.getText().toString();
        		thread.addCommand(Command.SEND_MSG, fm);
        	}
        });
		
	}
	
	private class SendMsgUiHandler extends Handler {
		@Override
        public void handleMessage(Message msg) 
		{
			SendMsgThread.ThreadState ss = (SendMsgThread.ThreadState)msg.obj;
			Debugger.d( "sendmsgthread reports " + ss.state.toString());
			switch (ss.state) {
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
			case MSG_TRANSFERED: {
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify("successfully sent msg to " + fm.contact.nickName
						+ ": " + fm.msg);
				loadMsgList();
				btnSend.setClickable(true);
				editMsgText.setText("");
        		editMsgText.setEnabled(true);
				break;
			}
			case MSG_FAILED:{
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify("failed sent msg to " + fm.contact.nickName
						+ ": " + fm.msg);
				btnSend.setClickable(true);
        		editMsgText.setEnabled(true);
				break;
			}
			case NETWORK_ERROR:{
				FetionMsg fm = (FetionMsg)ss.arg;
				popNotify("msg failed to " + fm.contact.nickName
						+ ": " + fm.msg + ", because of network error");
				btnSend.setClickable(true);
        		editMsgText.setEnabled(true);
				break;
			}
			default:
				btnSend.setClickable(true);
        		editMsgText.setEnabled(true);
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
    	super.onResume();
    	
    }
    
    @Override
    protected void onPause() {

    	super.onPause();
    }
	
	@Override
	protected void onStop() {
    	unregisterReceiver(receiver);
    	Debugger.d( "RECEIVER UNREGISTERED");
    	super.onStop();
	}
	@Override
	protected void onStart() {
		super.onStart();
		crypto = Crypto.getInstance();
		Debugger.d( "loading sms list of " + mobileno);
		
    	IntentFilter filter = new IntentFilter();
    	filter.addAction("android.provider.Telephony.SMS_RECEIVED");
    	filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
    	registerReceiver(receiver, filter);
    	Debugger.d( "RECEIVER REGISTERED");
		
		
		loadMsgList();
		uiHandler = new SendMsgUiHandler();
		sysConfig = SystemConfig.getInstance();
		//Debugger.d( "SIPC = " + sysConfig.sipcProxyIp + ":" + sysConfig.sipcProxyPort);
		thread = new SendMsgThread(sysConfig, crypto, uiHandler);
		thread.start();
		
		
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	switch (requestCode) {
    	case INTENT_PIC_VERIFY_DIALOG: {
    		if (thread.state == SendMsgThread.State.AUTHENTICATE_NEED_CONFIRM)
    		{
    			Debugger.d( "thread state=" + thread.state.toString());
    			switch (resultCode) {
    			case RESULT_OK: {
		    		Bundle bundle = data.getExtras();
		    		thread.verification.code = bundle.getString("code");
		    		Debugger.d( "wake up thread");
		    		synchronized(thread) {
		    			thread.notify();
		    		}
		    		break;
    			}
		    	default:
		    		Debugger.e( "pic verify destroyed");
		    		thread.stop();
		    		break;
    			}
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
		ArrayList<Integer> frontcolor = new ArrayList<Integer>();
		for (int i = 0; i < smsList.size(); ++i) {
			int type = smsList.get(i).type;
			long date = smsList.get(i).date;
			String body = smsList.get(i).body;
			
			String smstext = ((type == 1)? "<0>": "<1>我: " )
					+body;
			String timetext = DateFormat.format("yy-MM-dd kk:mm:ss", date).toString();
			
		    HashMap<String, Object> map = new HashMap<String, Object>(); 
		    
		    map.put("MsgText", smstext); 
		    map.put("TimeText", timetext); 
		    listItem.add(map); 
		    frontcolor.add(0x00000000);
		    backcolor.add((type == 1)? 0x008080FF: 0x00FFFFFF);
		} 
		MsgListAdapter listItemAdapter = new MsgListAdapter(MsgHistory.this,
			listItem,
			
			frontcolor,
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
			Debugger.d( "SmsReceiver.onReceive");
			if (intent.getAction().equals(mReceiveAction)) 
			{
				Debugger.d( "SmsReceiver.onReceive SMS_RECEIVED");
				
				//StringBuilder sb = new StringBuilder();
				
				Bundle bundle = intent.getExtras();
				
				
				boolean hasThisContact = false;
				
				if (bundle != null) {
					Object[] objpdus = (Object[])bundle.get("pdus");
					
					ArrayList<Object> newobjpdus = new ArrayList<Object>();
					for (int i = 0; i < objpdus.length; ++i) {
						SmsMessage sms = SmsMessage.createFromPdu((byte[])objpdus[i]);
						String addr = sms.getOriginatingAddress();
						long date = sms.getTimestampMillis();
						String body = sms.getMessageBody();
						Debugger.d( "received sms from " + addr);
						if (addr.equals(mobileno)) {
							hasThisContact = true;
							//abortBroadcast();
					        //setResultData(null);
							SmsDbAdapter.insertReceivedSms(mobileno, date, body);
							try {
								vb.vibrate(new long[] {
										100, 10, 100, 1000	
										}, -1);
								 sp.play(newsmshit, 2, 1, 0, 0, (float)1.0);
								
							} catch (Exception e) {
								Debugger.e( "can not play sound:" + e.getMessage());
							}
					    }
						else {
							newobjpdus.add(objpdus[i]);
						}
					}

				}
				
				if (hasThisContact) {
					//bundle.put
					loadMsgList();
					//abortBroadcast();
			        //setResultData(null);
				}
			}
		}
	}
}
