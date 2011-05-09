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
    //private Map<String, String> portraitList;
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
    	CONTACT_GET_FAIL,
    	
    	WAIT_GET_PORTRAIT,
    	PORTRAIT_GETTING,
    	PORTRAIT_GET_SUCC,
    	PORTRAIT_GET_FAIL,

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
        	Debugger.e( "error re-create sipc socket");
        	notifyState(State.NETWORK_DOWN);
        	return;
        }
        
        Debugger.d( "SIPC = " + sysConfig.sipcProxyIp + ":" + sysConfig.sipcProxyPort);
        
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
        		Debugger.e( "error in register session: " + e.getMessage());
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
	        	auth = new AuthenticationSession(sysConfig, crypto, is, os);
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
	            	Debugger.d( "Process a junk");
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
	            	Debugger.d( "refresh thread awakes");
	            	retry = true;
	            	break;
	            default:
	            	notifyState(State.AUTHENTICATE_FAIL);
	            	retry = false;
	            	return;
	            }
	        } catch (Exception e) {
	        	if (null != auth) {
	        		Debugger.e( "error in authenticate session: " + e.getMessage());
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
        			Debugger.d( "contact already in database, skip");
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
        			Debugger.d( "got user detail: nickname=" + nickname + " no = " + mobileno);
        			//fetionDb.setContact(contactList.get(uri));
        			//Debugger.d( "save contact into database");
        		}
        	} catch (Exception e) {
        		Debugger.e( "error happened getting detail[" + contactList.get(uri).sipUri 
        				+ "]: " + e.getMessage());
        	}
        }
        
        notifyState(State.CONTACT_GET_SUCC);

        boolean dretry = true;
        //do {
	        SipcDropCommand cmd = new SipcDropCommand(sysConfig.sId);
	        try {
	        	Debugger.d( "send drop command: " + cmd.toString());
				os.write(cmd.toString().getBytes());
				dretry = false;
				SipcResponse res = (SipcResponse)parser.parse(is);
				if (res == null) {
					
					//continue;
				}
				Debugger.d( "received response: " + res.toString());
				if (res.getResponseCode() == 200) {
					
					//break;
				}
				else {
					
					//continue;
				}
	        } catch (IOException e) {
	        	
	        	Debugger.e( "drop error: " + e.getMessage());
				//continue;
	        }
        //} while (dretry);

        try {
        	Network.closeSipcSocket();
        } catch (Exception e) {
        	
        }
        
        // update each contact's portrait
        Socket pSocket = null;
        InputStream pIs = null;
        OutputStream pOs = null;
        try { 
	        pSocket = new Socket(sysConfig.portraitServersName, 80);
	        pIs = pSocket.getInputStream();
	        pOs = pSocket.getOutputStream();
	    } catch (Exception e) {
	    	notifyState(State.PORTRAIT_GET_FAIL);
	    	Debugger.e( "creating socket for loading portraits failed: " + e.getMessage());
	    	return;
	    }
	    iter = contactList.keySet().iterator();
	    FetionHttpMessageParser p = new FetionHttpMessageParser();
        while (iter.hasNext())
        {
        	String uri = iter.next();
        	FetionContact fc = contactList.get(uri);
        	FetionHttpMessage req = new FetionLoadPortraitHttpRequest(
        			"/" + sysConfig.portraitServersPath + "/getportrait.aspx", fc.sipUri, 
        			Network.encodeUril(sysConfig.ssic),
        			sysConfig.portraitServersName);
        	try {
        		pOs.write(req.toString().getBytes());
        		Debugger.d( "sent request: " + req.toString());
	        	FetionHttpResponse res = (FetionHttpResponse)p.parse(pIs);
	        	if (res != null) {
	        		Debugger.d( "received response: " + res.toString());
	        	}
	        	if (res != null && res.getResponseCode() == 200) {
	        		fc.portrait = res.body;
	        		Debugger.d( "successfully got portrait for " + uri);
	        	}
        	} catch (Exception e) {
        		Debugger.e( "loading portrait for " + uri + " failed: " + e.getMessage());
        	}
        }
        
        notifyState(State.PORTRAIT_GET_SUCC);
        
        
                
        try {
        	pSocket.close();
        } catch (Exception e) {
        	
        }
        notifyState(State.THREAD_EXIT);
    }
}
