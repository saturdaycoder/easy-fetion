package com.saturdaycoder.easyfetion;
import java.net.Socket;
import java.io.*;
//import java.util.ConcurrentLinkedQueue;
import java.util.LinkedList;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.os.Message;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashMap;

public class EasyFetionThread extends Thread 
{
	public Queue<FetionMsg> pendingSmsQueue;
	
	private static final String TAG = "EasyFetion";

    public SystemConfig sysConfig;
    private Crypto crypto;
    public SmsDbWriter smsDbWriter;
    public FetionDatabase fetionDb;

    Handler uiHandler;
    Handler workHandler;
    
    public boolean toBeExited = false;
    
    public State state = State.INIT;
    
    // data (model)
    public Map<String, FetionContact> contactList;
    public FetionPictureVerification verification;
	
    public EasyFetionThread()
    {
    	
    }
    public void init(Handler uiHandler) {
    	this.uiHandler = uiHandler;
    	verification  = new FetionPictureVerification();
    	contactList = new LinkedHashMap<String, FetionContact>();
    	pendingSmsQueue = new LinkedList<FetionMsg>();
    	
    }
    
    public class ThreadCommand {
    	Command cmd;
    	Object arg;
    }
    public class ThreadState {
    	State state;
    	Object arg;
    }
    public void addCommand(Command cmd, Object obj) {
    	Message msg = workHandler.obtainMessage();
    	ThreadCommand tc = new ThreadCommand();//obj;
    	tc.cmd = cmd;
    	tc.arg = obj;
    	msg.obj = tc;
    	workHandler.sendMessage(msg);
    }
    
    protected void notifyState(State state, Object arg) {
    	this.state = state;
    	ThreadState ts = new ThreadState();
    	Message msg = uiHandler.obtainMessage();
    	ts.state = state;
    	ts.arg = arg;
    	msg.obj = ts;
    	uiHandler.sendMessage(msg);
    }
    
    public enum State {
    	INIT,
    	WAIT_LOGIN,
    	LOGIN_RUNNING,
    	LOGIN_FAIL,
    	LOGIN_NEED_CONFIRM,
    	LOGIN_SUCC,
    	WAIT_DOWNLOAD_CONFIG,
    	CONFIG_DOWNLOADING,
    	CONFIG_DOWNLOAD_SUCC,
    	CONFIG_DOWNLOAD_FAIL,
    	WAIT_REGISTER,
    	REGISTER_RUNNING,
    	REGISTER_FAIL,
    	REGISTER_SUCC,
    	WAIT_AUTHENTICATE,
    	AUTHENTICATE_RUNNING,
    	AUTHENTICATE_FAIL,
    	AUTHENTICATE_NEED_CONFIRM,
    	AUTHENTICATE_SUCC,
    	AUTHENTICATE_POSTPROCESS,
    	WAIT_GET_CONTACT,
    	CONTACT_GETTING,
    	CONTACT_GET_SUCC,
    	
    	WAIT_MSG,
    	
    	MSG_SENDING,
    	MSG_TRANSFERED,
    	MSG_RECEIVED,
    	MSG_FAILED,
    	
    	THREAD_EXIT
    }
    
    public enum Command {
    	//START,
    	//CANCEL,
    	//VERIFY,
    	SEND_MSG,
    	STOP,
    	STOP_NOW,
    	EXIT,
    	EXIT_NOW,
    }
    
	@Override 
	public void run() {
		
		Looper.prepare();
		
		
		
        sysConfig = new SystemConfig();
        
        // get saved contact list
        FetionContact contacts[] = fetionDb.getContacts();
        Log.d(TAG, "got saved contacts " + contacts.length + " people");
        for (int i = 0; i <contacts.length; ++i)
        	contactList.put(contacts[i].sipUri, contacts[i]);
        
        notifyState(State.CONTACT_GET_SUCC, null);
        
        fetionDb.getAccount(sysConfig);
        

        
        Log.d(TAG, "work thread starts");
        
       
        boolean lretry = false;
        boolean lreinput = true;
        do {
            if ((sysConfig.mobileNumber == null || sysConfig.mobileNumber.equals(""))
            		&& lreinput) {
            	notifyState(State.WAIT_LOGIN, null);
            	try {
            		synchronized(this) {
            			wait();
            		}
            	} catch (Exception e) {
            		Log.e(TAG, "error waiting: " + e.getMessage());
            	}
            }
            
	        LoginSession login = null;
	        try {
	        	login = new LoginSession(sysConfig);
	        	notifyState(State.LOGIN_RUNNING, null);
	        	Log.d(TAG, "retry login");
	        	login.send(verification);
	        	
	        	login.read();
	        	
	        	int statuscode = login.response.getResponseCode();
	            
	        	Log.d(TAG, "login response = \"" + login.response + "\"");
	        	
	            switch(statuscode) {
	            case 200:
	            	login.postprocess();
	            	login.close();
	            	lreinput = false;
	            	lretry = false;
	            	break;
	            case 420:
	            case 421:
	            	login.postprocessVerification(verification);
	            	notifyState(State.LOGIN_NEED_CONFIRM, null);
	            	synchronized(this) {
	                	wait();
	                }
	            	Log.d(TAG, "login verify code=" + verification.code);
	            	
	            	lretry = true;
	            	
	            	lreinput = false;
	            	login.close();
	            	break;
	            	
	            default:
	            	notifyState(State.LOGIN_FAIL, null);
	            	login.close();
	            	sysConfig.mobileNumber = "";
	            	lretry = true;
	            	lreinput = true;
	            	break;
	            	
	            }	
	        } catch (Exception e) {
	        	Log.e(TAG, "error in login: " + e.getMessage());
	        	notifyState(State.LOGIN_FAIL, null);
	        	if (null != login) 
	        		login.close();
	        	
	        	sysConfig.mobileNumber = "";
	        	lretry = true;
	        	break;
	        }
	        
	        
        } while (lretry);
        notifyState(State.LOGIN_SUCC, null);
        fetionDb.setAccount(sysConfig);
        
        notifyState(State.WAIT_DOWNLOAD_CONFIG, null);
        notifyState(State.CONFIG_DOWNLOADING, null);
        
        fetionDb.getUserInfo(sysConfig);
        sysConfig.Download();
        fetionDb.setUserInfo(sysConfig);
        
        notifyState(State.CONFIG_DOWNLOAD_SUCC, null);
        
        
        notifyState(State.WAIT_REGISTER, null);
        
        RegisterSession reg = null;
        
        crypto = new Crypto();
        
        Socket sipcSocket = null;
        
        try {
        	sipcSocket = new Socket(sysConfig.sipcProxyIp, sysConfig.sipcProxyPort);
        } catch (Exception e) {
        	Log.e(TAG, "fatal error: creating sipc socket: " + e.getMessage());
        }
        InputStream is = null;
        OutputStream os = null;
        try {
        	//sipcSocket = new Socket(sysConfig.sipcProxyIp, sysConfig.sipcProxyPort);
        	is = sipcSocket.getInputStream();
        	os = sipcSocket.getOutputStream();
        } catch (Exception e) {
        	Log.e(TAG, "fatal error: getting i/o stream from sipc socket: " + e.getMessage());
        }

        try {
        	reg = new RegisterSession(sysConfig, crypto, is, os);//sipcSocket);
        	notifyState(State.REGISTER_RUNNING, null);
        	reg.send();
        	notifyState(State.REGISTER_RUNNING, null);
        	reg.read();
        	notifyState(State.REGISTER_RUNNING, null);
        	int statuscode = reg.response.getResponseCode();
        	notifyState(State.REGISTER_RUNNING, null);
            switch(statuscode) {
            case 401:
            	
            	reg.postprocess();
            	break;
            default:
            	notifyState(State.REGISTER_FAIL, null);
            	sipcSocket.close();
            	return;
            }
        } catch (Exception e) {
        	if (null != reg) {
        		Log.e(TAG, "error in register session: " + e.getMessage());
        		notifyState(State.REGISTER_FAIL, null);
        		return;
        	}
        }
        
        notifyState(State.REGISTER_SUCC, null);
        
        notifyState(State.WAIT_AUTHENTICATE, null);
        boolean retry = false;
        AuthenticationSession auth = null;
        do {
        	retry = false;
	        try {
	        	auth = new AuthenticationSession(sysConfig, crypto, is, os);//sipcSocket);
	        	notifyState(State.AUTHENTICATE_RUNNING, null);
	        	auth.send(verification);
	        
	        	notifyState(State.AUTHENTICATE_RUNNING, null);
	        	auth.read();
	        	notifyState(State.AUTHENTICATE_RUNNING, null);
	        	int statuscode = auth.response.getResponseCode();
	        	notifyState(State.AUTHENTICATE_RUNNING, null);
	            switch(statuscode) {
	            case 200:
	            	auth.postprocessContacts(contactList);
	            	Log.d(TAG, "Process a junk");
	            	auth.postprocessJunk();
	            	break;
	            case 420:
	            case 421:
	            	auth.postprocessVerification(verification);
	            	notifyState(State.AUTHENTICATE_NEED_CONFIRM, null);
	            	synchronized(this) {
	                	wait();
	                }
	            	if (!verification.code.equals("")) {
	            		retry = true;
	            	}
	            	break;
	            default:
	            	notifyState(State.AUTHENTICATE_FAIL, null);
	            	sipcSocket.close();
	            	return;
	            }
	        } catch (Exception e) {
	        	if (null != auth) {
	        		Log.e(TAG, "error in authenticate session: " + e.getMessage());
	        		notifyState(State.AUTHENTICATE_FAIL, null);
	        		return;
	        	}
	        }
        } while (retry);
        notifyState(State.AUTHENTICATE_SUCC, null);
        
        
        notifyState(State.CONTACT_GETTING, null);
        
        
        SipcMessageParser parser = new SipcMessageParser();
        
        
        Iterator<String> iter = contactList.keySet().iterator();
        
        while (iter.hasNext())
        {
        	String uri = iter.next();
        	try {
        		if (fetionDb.hasContactByUri(contactList.get(uri).sipUri)) {
        			Log.d(TAG, "contact already in database, skip");
        			continue;
        		}
        		
        		SipcContactInfoCommand cmd = new SipcContactInfoCommand(sysConfig.sId, 
        				contactList.get(uri).sipUri);
        		sipcSocket.getOutputStream().write(cmd.toString().getBytes());
        		SipcResponse res = (SipcResponse)parser.parse(sipcSocket.getInputStream());
        		if (res.getResponseCode() == 200) {
        			int mobilenoind = res.body.indexOf("mobile-no=\"");
        			int nicknameind = res.body.indexOf("nickname=\"");
        			String mobileno, nickname;
        			if (mobilenoind != -1) {
        				mobileno = res.body.substring(mobilenoind + 11);
        				mobileno = mobileno.substring(0, mobileno.indexOf("\""));
        			} else {
        				mobileno = "";//unknown";
        			}
        			contactList.get(uri).mobileNumber = mobileno;
        			if (nicknameind != -1) {
        				nickname = res.body.substring(nicknameind + 10);
        				nickname = nickname.substring(0, nickname.indexOf("\""));
        			} else {
        				nickname = "";//unknown";
        			}
        			contactList.get(uri).nickName = nickname;
        			Log.d(TAG, "got user detail: nickname=" + nickname + " no = " + mobileno);
        			fetionDb.setContact(contactList.get(uri));
        			Log.d(TAG, "save contact into database");
        		}
        	} catch (Exception e) {
        		Log.e(TAG, "error happened getting detail[" + contactList.get(uri).sipUri 
        				+ "]: " + e.getMessage());
        	}
        }
        
        notifyState(State.CONTACT_GET_SUCC, null);

        
        
        
        
        
        while (true) {
        	if (pendingSmsQueue.size() > 0) {
        		Log.e(TAG, "detected pending sms queue size = " + pendingSmsQueue.size());
        		FetionMsg msg = pendingSmsQueue.peek();
        		boolean online = true;
        		if (!online) { // send direct SMS
	        		try {
		        		SipcSendSmsCommand cmd = new SipcSendSmsCommand(sysConfig.sId,
		        				msg.contact.sipUri, msg.msg);
		        		//		sysConfig.userUri, msg.msg);
		        		os.write(cmd.toString().getBytes());
		        		Log.e(TAG, "send msg sent: \"" + cmd.toString() + "\"");
		        		notifyState(State.MSG_TRANSFERED, null);
		        		pendingSmsQueue.poll();
	        		} catch (Exception e) {
	        			Log.e(TAG, "work thread fails to send sms: " + e.getMessage());
	        			notifyState(State.MSG_FAILED, null);
	        			continue;
	        		}
	        		try {
	        			SipcResponse res = (SipcResponse)parser.parse(is);
	        			if (res == null) {
	        				Log.e(TAG, "error receiving response");
	        				notifyState(State.MSG_FAILED, null);
	        			}
		        		Log.e(TAG, "send msg received: \"" + res.toString() + "\"");
		        		if (res.getResponseCode() == 280) {
		        			notifyState(State.MSG_RECEIVED, null);
		        		} else {
		        			notifyState(State.MSG_FAILED, null);
		        		}
	
	        		} catch (Exception e) {
	        			Log.e(TAG, "work thread fails to read sms response: " + e.getMessage());
	        		}
        		}
        		else { // send online message

        			try {
        				//sleep(10000);
        				SipcCommand startChatCmd = new SipcStartChatCommand(sysConfig.sId);
        				os.write(startChatCmd.toString().getBytes());
        				Log.e(TAG, "Sent command: " + startChatCmd.toString());
        				SipcResponse response = (SipcResponse)parser.parse(is);
        				Log.e(TAG, "succeeded starting chat: " + response.toString());
        			} catch (Exception e) {
        				Log.e(TAG, "send start chat command failed");
        				continue;
        			}
        			
        			try {
        				SipcCommand sendMsgCmd = new SipcSendMsgCommand(sysConfig.sId, 
        						msg.contact.sipUri, msg.msg);
        				os.write(sendMsgCmd.toString().getBytes());
        				Log.e(TAG, "Sent command: " + sendMsgCmd.toString());
        				SipcMessage m = (SipcMessage)parser.parse(is);
        				Log.e(TAG, "succeeded sending msg: " + m.toString());
        				notifyState(State.MSG_TRANSFERED, msg);
		        		pendingSmsQueue.poll();
		        		// write the sent message to sms database
		        		if (msg.contact.mobileNumber != null 
		        				&& !msg.contact.mobileNumber.equals("")) {
		        			smsDbWriter.insertSentSms(msg.contact.mobileNumber,
		        						System.currentTimeMillis(), msg.msg);
		        		}
		        		else {
		        			//notifyState(State.MSG_FAILED, msg);
		        			Log.e(TAG, "the mobileno of the receiver is invalid");
		        		}
        			} catch (Exception e) {
        				notifyState(State.MSG_FAILED, msg);
        				Log.e(TAG, "send online msg command failed");
        				continue;
        			}
        		}
        		
        	}
        	else {
        		Log.e(TAG, "detected pending sms queue size = " + pendingSmsQueue.size());
        		if (toBeExited) {
        			Log.e(TAG, "worker thread exits");
        			notifyState(State.THREAD_EXIT, null);
        			
        			return;
        		}
        		
        		try {
        			synchronized(this) {
        				notifyState(State.WAIT_MSG, null);
        				
        				wait();
        			}
        		} catch (Exception e) {
        			Log.e(TAG, "work thread fails to wait: " + e.getMessage());
        		}
        	}
        }

	}

}
