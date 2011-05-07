package com.saturdaycoder.easyfetion;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.saturdaycoder.easyfetion.LoginThread.Command;
import com.saturdaycoder.easyfetion.LoginThread.State;
import com.saturdaycoder.easyfetion.LoginThread.ThreadCommand;
import java.io.IOException;

public class RefreshThread extends Thread{
	private static final String TAG = "EasyFetion";
	private SystemConfig sysConfig;
	private Handler uiHandler;
	private Crypto crypto;
    private Handler workHandler;
    private Map<String, FetionContact> contactList;
    public FetionPictureVerification verification;
    public State state;
    public enum State {
    	INIT,
    	
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

    	THREAD_EXIT,
    	
    	NETWORK_DOWN,
    }
    public enum Command {
    	EXIT,
    }
    public class ThreadCommand {
    	Command cmd;
    	Object arg;
    }

    public void addCommand(Command cmd, Object obj) {
    	Message msg = workHandler.obtainMessage();
    	ThreadCommand tc = new ThreadCommand();
    	tc.cmd = cmd;
    	tc.arg = obj;
    	msg.obj = tc;
    	workHandler.sendMessage(msg);
    }
    
    
    
    protected void notifyState(State state){//, Object arg) {
    	this.state = state;
    	//ThreadState ts = new ThreadState();
    	Message msg = uiHandler.obtainMessage();
    	//msg.state = state;
    	//ts.arg = arg;
    	msg.obj = state;
    	uiHandler.sendMessage(msg);
    }
    public RefreshThread(SystemConfig sysConfig, Crypto crypto, 
    		Map<String, FetionContact> contactList, Handler uiHandler)
	{
		this.sysConfig = sysConfig;
		this.uiHandler = uiHandler;
		this.crypto = crypto;
		this.contactList = contactList;
		verification  = new FetionPictureVerification();
	}
    
    @Override
	public void run() {
    	notifyState(State.WAIT_REGISTER);
        
        RegisterSession reg = null;
        //Socket sipcSocket = null;
        InputStream is = null;
        OutputStream os = null;
        try {
        	Network.closeSipcSocket();
        	Network.createSipcSocket(sysConfig.sipcProxyIp, sysConfig.sipcProxyPort);
        	is = Network.getSipcInputStream();
        	os = Network.getSipcOutputStream();
        } catch (Exception e) {
        	Log.e(TAG, "error re-create sipc socket");
        	notifyState(State.NETWORK_DOWN);
        	return;
        }
        
        Log.d(TAG, "SIPC = " + sysConfig.sipcProxyIp + ":" + sysConfig.sipcProxyPort);
        
        try {
        	reg = new RegisterSession(sysConfig, crypto, is, os);
        	notifyState(State.REGISTER_RUNNING);
        	reg.send();
        	notifyState(State.REGISTER_RUNNING);
        	reg.read();
        	notifyState(State.REGISTER_RUNNING);
        	int statuscode = reg.response.getResponseCode();
        	notifyState(State.REGISTER_RUNNING);
            switch(statuscode) {
            case 401:
            	
            	reg.postprocess();
            	break;
            default:
            	notifyState(State.REGISTER_FAIL);
            	//sipcSocket.close();
            	return;
            }
        } catch (Exception e) {
        	if (null != reg) {
        		Log.e(TAG, "error in register session: " + e.getMessage());
        		notifyState(State.REGISTER_FAIL);
        		//try {
        			//sipcSocket.close();
        		//} catch (Exception ex) {
        			
        		//}
        		return;
        	}
        }
        
        notifyState(State.REGISTER_SUCC);
        
        notifyState(State.WAIT_AUTHENTICATE);
        boolean retry = false;
        AuthenticationSession auth = null;
        do {
        	retry = false;
	        try {
	        	auth = new AuthenticationSession(sysConfig, crypto, is, os);//sipcSocket);
	        	notifyState(State.AUTHENTICATE_RUNNING);
	        	auth.send(verification);
	        
	        	notifyState(State.AUTHENTICATE_RUNNING);
	        	auth.read();
	        	notifyState(State.AUTHENTICATE_RUNNING);
	        	int statuscode = auth.response.getResponseCode();
	        	notifyState(State.AUTHENTICATE_RUNNING);
	            switch(statuscode) {
	            case 200:
	            	auth.postprocessContacts(contactList);
	            	Log.d(TAG, "Process a junk");
	            	auth.postprocessJunk();
	            	retry = false;
	            	break;
	            case 420:
	            case 421:
	            	auth.postprocessVerification(verification);
	            	notifyState(State.AUTHENTICATE_NEED_CONFIRM);
	            	synchronized(this) {
	                	wait();
	                }
	            	//if (!verification.code.equals("")) {
	            		//retry = true;
	            	retry = true;
	            	//}
	            	break;
	            default:
	            	notifyState(State.AUTHENTICATE_FAIL);
	            	//sipcSocket.close();
	            	retry = false;
	            	return;
	            }
	        } catch (Exception e) {
	        	if (null != auth) {
	        		Log.e(TAG, "error in authenticate session: " + e.getMessage());
	        		notifyState(State.AUTHENTICATE_FAIL);
	        		retry = false;
	        		return;
	        	}
	        }
        } while (retry);
        notifyState(State.AUTHENTICATE_SUCC);
        
        
        notifyState(State.CONTACT_GETTING);
        
        
        SipcMessageParser parser = new SipcMessageParser();
        
        
        Iterator<String> iter = contactList.keySet().iterator();
        
        while (iter.hasNext())
        {
        	String uri = iter.next();
        	try {
        		/*if (fetionDb.hasContactByUri(contactList.get(uri).sipUri)) {
        			Log.d(TAG, "contact already in database, skip");
        			continue;
        		}*/
        		
        		SipcContactInfoCommand cmd = new SipcContactInfoCommand(sysConfig.sId, 
        				contactList.get(uri).sipUri);
        		os.write(cmd.toString().getBytes());
        		SipcResponse res = (SipcResponse)parser.parse(is);
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
        			//fetionDb.setContact(contactList.get(uri));
        			//Log.d(TAG, "save contact into database");
        		}
        	} catch (Exception e) {
        		Log.e(TAG, "error happened getting detail[" + contactList.get(uri).sipUri 
        				+ "]: " + e.getMessage());
        	}
        }
        
        notifyState(State.CONTACT_GET_SUCC);

        boolean dretry = false;
        do {
	        SipcDropCommand cmd = new SipcDropCommand(sysConfig.sId);
	        try {
	        	Log.d(TAG, "send drop command: " + cmd.toString());
				os.write(cmd.toString().getBytes());
				SipcResponse res = (SipcResponse)parser.parse(is);
				if (res == null) {
					dretry = true;
					continue;
				}
				Log.d(TAG, "received response: " + res.toString());
				if (res.getResponseCode() == 200) {
					dretry = false;
					break;
				}
				else {
					dretry = true;
					continue;
				}
	        } catch (IOException e) {
	        	dretry = true;
				continue;
	        }
        } while (dretry);
        
                
        try {
        	Network.closeSipcSocket();
        } catch (Exception e) {
        	
        }
        notifyState(State.THREAD_EXIT);
    }
}
