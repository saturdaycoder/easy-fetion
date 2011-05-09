package com.saturdaycoder.easyfetion;
import com.saturdaycoder.easyfetion.SendMsgThread.ThreadCommand;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class LoginThread extends Thread {
	private SystemConfig sysConfig;
	private Handler uiHandler;
    private Handler mHandler;
    public FetionPictureVerification verification;
    public State state;
    public enum State {
    	INIT,
    	
    	WAIT_LOGIN,
    	LOGIN_SENDING,
    	LOGIN_READING,
    	LOGIN_POSTPROCESSING,
    	LOGIN_FAIL,
    	LOGIN_NEED_CONFIRM,
    	LOGIN_SUCC,
    	
    	WAIT_DOWNLOAD_CONFIG,
    	CONFIG_DOWNLOAD_SENDING,
    	CONFIG_DOWNLOAD_READING,
    	CONFIG_DOWNLOAD_POSTPROCESSING,
    	CONFIG_DOWNLOAD_SUCC,
    	CONFIG_DOWNLOAD_FAIL,

    	THREAD_EXIT,
    	
    	NETWORK_DOWN,
    }
    public enum Command {
    	LOGIN,
    	DOWNLOAD_CONFIG,
    	EXIT,
    }
    public class ThreadCommand {
    	Command cmd;
    	Object arg;
    }

    public void addCommand(Command cmd){//, Object obj) {
    	Message msg = mHandler.obtainMessage();
    	//ThreadCommand tc = new ThreadCommand();
    	//tc.cmd = cmd;
    	//tc.arg = obj;
    	msg.obj = cmd;//tc;
    	mHandler.sendMessage(msg);
    }
    
    private class WorkHandler extends Handler {
    	@Override
        public void handleMessage(Message msg) 
		{
    		//ThreadCommand tc = (ThreadCommand)msg.obj;
    		Command cmd = (Command)msg.obj;
    		//FetionMsg fm = (FetionMsg)tc.arg;
    		switch (cmd) {
    		case LOGIN: {
    	        Debugger.v("login thread starts");
    	        notifyState(State.WAIT_LOGIN);
    	        LoginSession login = null;
    	        try {
    	        	login = new LoginSession(sysConfig);
    	        } catch (Exception e) {
    	        	Debugger.e("unable to establish login SSL socket: " + e.getMessage());
    	        	notifyState(State.NETWORK_DOWN);
    	        	return;
    	        }
    	        notifyState(State.LOGIN_SENDING);
    	        Debugger.v("retry login");
    	        try {
    	        	login.send(verification);
    	        	verification.clear();
    	        } catch (Exception e) {
    	        	Debugger.e("unable to send login command thru SSL socket: " + e.getMessage());
    	        	login.close();
    	        	notifyState(State.NETWORK_DOWN);
    	        	return;	
    	        }
    	        
    	        notifyState(State.LOGIN_READING);
    	        try {
    	        	login.read();
    	        } catch (Exception e) {
    	        	Debugger.e("error retriving SSL response: " + e.getMessage());
    	        	login.close();
    	        	notifyState(State.NETWORK_DOWN);
    	        	return;	
    	        }
    	        Debugger.d("login response = \"" + login.response + "\"");
    	        
    	        notifyState(State.LOGIN_POSTPROCESSING);
    	        int statuscode = login.response.getResponseCode();
    	        switch(statuscode) {
    	        case 200:
    	        	try {
    	        		login.postprocess();
    	        		login.close();
    	            	notifyState(State.LOGIN_SUCC);
    	            	return;
    	        	} catch (Exception e) {
    	        		Debugger.e("error parsing XML in the response: " + e.getMessage());
    	            	login.close();
    	            	notifyState(State.LOGIN_FAIL);
    	            	return;	
    	        	}        	
    	        case 420:
    	        case 421:
    	        	login.postprocessVerification(verification);
    	        	
    	        	Debugger.d("login verify code=" + verification.code);
    	        	login.close();
    	        	notifyState(State.LOGIN_NEED_CONFIRM);
    	        	return;
    	        default:
    	        	
    	        	login.close();
    	        	notifyState(State.LOGIN_FAIL);
    	        	return;
    	        }	 			
    		}
    		case DOWNLOAD_CONFIG: {
    			notifyState(State.WAIT_DOWNLOAD_CONFIG);
    			try {
    				sysConfig.initDownload();
    			} catch (Exception e) {
    				Debugger.e("error creating config download socket: " + e.getMessage());
    				notifyState(State.NETWORK_DOWN);
    				return;
    			}
    			
    	        notifyState(State.CONFIG_DOWNLOAD_SENDING);
    	        try {
    	        	sysConfig.sendDownload();
    	        }catch (Exception e) {
    				sysConfig.closeDownload();
    				Debugger.e("error sending config download command: " + e.getMessage());
    				notifyState(State.NETWORK_DOWN);
    				return;
    			}
    	        
    	   		notifyState(State.CONFIG_DOWNLOAD_READING);
    	        try {
    	        	sysConfig.readDownload();
    	        }catch (Exception e) {
    				sysConfig.closeDownload();
    				Debugger.e("error reading the config response: " + e.getMessage());
    				notifyState(State.NETWORK_DOWN);
    				return;
    			}
    	   		notifyState(State.CONFIG_DOWNLOAD_POSTPROCESSING);
    	   		try {
    	   			sysConfig.postprocessDownload();
    	   		}catch (org.xml.sax.SAXException e) {
    				sysConfig.closeDownload();
    				Debugger.e("SAX error parsing config XML in the response: " + e.toString());
    				notifyState(State.CONFIG_DOWNLOAD_FAIL);
    				return;
    			}catch (javax.xml.parsers.ParserConfigurationException e) {
    				sysConfig.closeDownload();
    				Debugger.e("Parser error parsing config XML in the response: " + e.toString());
    				notifyState(State.CONFIG_DOWNLOAD_FAIL);
    				return;
    			}
    	   		catch (java.io.IOException e) {
    				sysConfig.closeDownload();
    				Debugger.e("IO error parsing config XML in the response: " + e.toString());
    				notifyState(State.CONFIG_DOWNLOAD_FAIL);
    				return;
    			}
    	   		sysConfig.closeDownload();
    	        notifyState(State.CONFIG_DOWNLOAD_SUCC);
    	        return;
    	        
    		}
    		case EXIT: {
    			break;
    		}
    		default:{
    			break;
    		}
    		}
    	}
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
    
	public LoginThread(SystemConfig sysConfig, Handler uiHandler)
	{
		this.sysConfig = sysConfig;
		this.uiHandler = uiHandler;
		verification  = new FetionPictureVerification();
		verification.clear();
		mHandler = new WorkHandler();
	}
	

	
	@Override
	public void run() {
		Looper.prepare();
		
        Looper.loop();

	}
}
