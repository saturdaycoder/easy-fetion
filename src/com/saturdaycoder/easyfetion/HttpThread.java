package com.saturdaycoder.easyfetion;
import java.io.InputStream;
import java.util.Iterator;
import java.io.OutputStream;
import java.net.Socket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.Map;
public class HttpThread extends Thread {
	private SystemConfig sysConfig;
	private Handler uiHandler;
    private Handler mHandler;
    public FetionPictureVerification verification;
    public State state;
    public State lastState;
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

    	WAIT_GET_PORTRAIT,
    	GET_PORTRAIT_GETTING,
    	GET_PORTRAIT_SUCC,
    	GET_PORTRAIT_FAIL,
    	
    	THREAD_EXIT,
    	
    	NETWORK_DOWN,
    }
    public enum Command {
    	//INIT,
    	LOGIN,
    	DOWNLOAD_CONFIG,
    	GET_PORTRAIT,
    	EXIT,
    }
    public class ThreadCommand {
    	Command cmd;
    	Object arg;
    }

    public void addCommand(Command cmd, Object arg){
    	Message msg = mHandler.obtainMessage();
    	ThreadCommand tc = new ThreadCommand();
    	tc.cmd = cmd;
    	tc.arg = arg;
    	msg.obj = tc;
    	mHandler.sendMessage(msg);
    }
    
    private void doGetPortraits(Map<String, FetionContact> contactList) {
    	notifyState(State.WAIT_GET_PORTRAIT);
    	Socket pSocket = null;
        InputStream pIs = null;
        OutputStream pOs = null;
        try { 
	        pSocket = new Socket(sysConfig.portraitServersName, 80);
	        pSocket.setSoTimeout (2000);
	        pIs = pSocket.getInputStream();
	        pOs = pSocket.getOutputStream();
	    } catch (Exception e) {
	    	notifyState(State.GET_PORTRAIT_FAIL);
	    	Debugger.e( "creating socket for loading portraits failed: " + e.getMessage());
	    	return;
	    }
	    Iterator<String> iter = contactList.keySet().iterator();
	    //FetionHttpMessageParser p = new FetionHttpMessageParser();
        while (iter.hasNext())
        {
        	notifyState(State.GET_PORTRAIT_GETTING);
        	String uri = iter.next();
        	FetionContact fc = contactList.get(uri);
        	doGetPortrait(fc, pIs, pOs);
        }
        try {
        	pSocket.close();
        } catch (Exception e) {
        	
        }
        notifyState(State.GET_PORTRAIT_SUCC);
    }
    
    private void doGetPortrait(FetionContact fc, InputStream is, OutputStream os) {
    	FetionHttpMessageParser p = new FetionHttpMessageParser();
    	FetionHttpMessage req = new FetionLoadPortraitHttpRequest(
    			"/" + sysConfig.portraitServersPath + "/getportrait.aspx", fc.sipUri, 
    			Network.encodeUril(sysConfig.ssic),
    			sysConfig.portraitServersName);
    	try {
    		os.write(req.toString().getBytes());
    		Debugger.d( "sent request: " + req.toString());
        	FetionHttpResponse res = (FetionHttpResponse)p.parse(is);
        	if (res != null) {
        		Debugger.d( "received response: " + res.toString());
        	}
        	if (res != null && res.getResponseCode() == 200) {
        		//fc.portrait = res.body;
        		byte[] b = res.bodybytes;
				java.math.BigInteger bi = new java.math.BigInteger(1, b);
			    String hex = String.format("%0" + (b.length << 1) + "X", bi);
				Debugger.d("hex is " + hex);
				
        		FetionDatabase.getInstance().savePortrait(fc.userId + ".JPG", res.bodybytes);
        		Debugger.d( "successfully got portrait for " + fc.sipUri);
        		
        	}
        	else if (res != null && res.getResponseCode() == 404) {
        		//fc.portrait = null;
        		Debugger.d( "no portrait for " + fc.sipUri);
        	}
        	else {
        		Debugger.d( "portrait for " + fc.sipUri + " ERROR");
        	}
    	} catch (Exception e) {
    		Debugger.e( "loading portrait for " + fc.sipUri + " failed: " + e.getMessage());
    	}
        
        
    }
    
    
    
    private class WorkHandler extends Handler {
    	@Override
        public void handleMessage(Message msg) 
		{

    		ThreadCommand tc = (ThreadCommand)msg.obj;
    		
    		switch (tc.cmd) {
    		//case INIT:
    		//	notifyState(State.INIT);
    		//	break;
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
    		case GET_PORTRAIT:
    			
    			try {
    				Map<String, FetionContact> cl = (Map<String, FetionContact>)tc.arg;
    				if (cl != null)
    					doGetPortraits(cl);
    				else {
    					Debugger.e("can not extract arguments for geting portrait");
        				notifyState(State.GET_PORTRAIT_FAIL);
    				}
    			} catch (Exception e) {
    				Debugger.e("can not extract arguments for geting portrait: " + e.getMessage());
    				notifyState(State.GET_PORTRAIT_FAIL);
    			}
    			break;
    		case EXIT: {
    			break;
    		}
    		default:{
    			break;
    		}
    		}
    	}
    }
    
    protected void notifyState(State state){
    	this.lastState = this.state;
    	this.state = state;
    	Message msg = uiHandler.obtainMessage();

    	msg.obj = state;
    	uiHandler.sendMessage(msg);
    }
    
	public HttpThread(SystemConfig sysConfig, Handler uiHandler)
	{
		this.sysConfig = sysConfig;
		this.uiHandler = uiHandler;
		verification  = new FetionPictureVerification();
		verification.clear();
		
	}
	

	
	@Override
	public void run() {
		Looper.prepare();
		mHandler = new WorkHandler();
        Looper.loop();

	}
}
