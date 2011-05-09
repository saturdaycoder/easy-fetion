package com.saturdaycoder.easyfetion;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class LoginThread extends Thread {
	private static final String TAG = "EasyFetion";
	private SystemConfig sysConfig;
	private Handler uiHandler;
    private Handler workHandler;
    public FetionPictureVerification verification;
    public State state;
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

    	THREAD_EXIT,
    	
    	NETWORK_DOWN,
    }
    public enum Command {
    	LOGIN,
    	DOWNLOAD,
    	EXIT,
    }
    public class ThreadCommand {
    	Command cmd;
    	Object arg;
    }
    /*public class ThreadState {
    	State state;
    	Object arg;
    }*/
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
    
	public LoginThread(SystemConfig sysConfig, Handler uiHandler)
	{
		this.sysConfig = sysConfig;
		this.uiHandler = uiHandler;
		verification  = new FetionPictureVerification();
	}
	
	private class WorkHandler extends Handler {
		
	}
	
	
	@Override
	public void run() {
		boolean lretry = false;
        boolean lreinput = true;
        Log.v(TAG, "login thread starts");
        do {
            
	        LoginSession login = null;
	        try {
	        	login = new LoginSession(sysConfig);
	        	notifyState(State.LOGIN_RUNNING);
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
	            	notifyState(State.LOGIN_SUCC);
	            	break;
	            case 420:
	            case 421:
	            	login.postprocessVerification(verification);
	            	notifyState(State.LOGIN_NEED_CONFIRM);
	            	synchronized(this) {
	                	wait();
	                }
	            	Log.d(TAG, "login verify code=" + verification.code);
	            	
	            	lretry = true;
	            	
	            	lreinput = false;
	            	login.close();
	            	break;
	            	
	            default:
	            	notifyState(State.LOGIN_FAIL);
	            	login.close();
	            	sysConfig.mobileNumber = "";
	            	lretry = true;
	            	lreinput = true;
	            	//break;
	            	return;
	            	
	            }	
	        } catch (Exception e) {
	        	Log.e(TAG, "error in login: " + e.getMessage());
	        	notifyState(State.LOGIN_FAIL);
	        	if (null != login) 
	        		login.close();
	        	
	        	sysConfig.mobileNumber = "";
	        	lretry = true;
	        	
	        	//break;
	        	return;
	        }
	        
	        
        } while (lretry);
        
        notifyState(State.CONFIG_DOWNLOADING);
        
   		sysConfig.Download();
        
        notifyState(State.CONFIG_DOWNLOAD_SUCC);
        
        notifyState(State.THREAD_EXIT);
	}
}
