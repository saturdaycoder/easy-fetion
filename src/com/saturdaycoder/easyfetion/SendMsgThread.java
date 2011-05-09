package com.saturdaycoder.easyfetion;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;


public class SendMsgThread extends Thread{


    public SystemConfig sysConfig;
    private Crypto crypto;
    private SipcMessageParser parser = new SipcMessageParser();
    Handler uiHandler;
    Handler mHandler;
    FetionPictureVerification verification;
    public State state;
    public class ThreadCommand {
    	Command cmd;
    	Object arg;
    }
    public class ThreadState {
    	State state;
    	Object arg;
    }
    public void addCommand(Command cmd, Object obj) {
    	Message msg = mHandler.obtainMessage();
    	ThreadCommand tc = new ThreadCommand();
    	tc.cmd = cmd;
    	tc.arg = obj;
    	msg.obj = tc;
    	mHandler.sendMessage(msg);
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
    public SendMsgThread(SystemConfig sysConfig, Crypto crypto, 
    		Handler uiHandler){
    	this.sysConfig = sysConfig;
		this.uiHandler = uiHandler;
		this.crypto = crypto;
		//this.contactList = contactList;
		verification  = new FetionPictureVerification();
    }
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
    	
    	WAIT_MSG,
    	MSG_SENDING,
    	MSG_TRANSFERED,
    	MSG_TRANSFERED_SMS,
    	//MSG_RECEIVED,
    	MSG_FAILED,
    	THREAD_EXIT,
    	NETWORK_ERROR,
    }
    
    public enum Command {
    	REGISTER,
    	AUTHENTICATE,
    	SEND_MSG,
    	EXIT,
    }
    
    private class WorkHandler extends Handler {
    	@Override
        public void handleMessage(Message msg) 
		{
    		ThreadCommand tc = (ThreadCommand)msg.obj;
    		FetionMsg fm = (FetionMsg)tc.arg;
    		switch (tc.cmd) {
    		case SEND_MSG: {
    			notifyState(State.WAIT_REGISTER, fm);
    			
    	        RegisterSession reg = null;
    	        
    	        //Socket sipcSocket = null;
    	        
    	        Debugger.d( "SIPC = " + sysConfig.sipcProxyIp + ":" + sysConfig.sipcProxyPort);
    	        
    	        InputStream is = null;
    	        OutputStream os = null;
    	        
    	        try {
    	        	Network.closeSipcSocket();
    	        	Network.createSipcSocket(sysConfig.sipcProxyIp, sysConfig.sipcProxyPort);
    	        } catch (Exception e) {
    	        	Debugger.e( "error re-create sipc socket");
    	        	notifyState(State.NETWORK_ERROR, fm);
    	        	return;
    	        }
    	        
    	        try {
    	        	
    	        	is = Network.getSipcInputStream();
    	        	os = Network.getSipcOutputStream();
    	        } catch (Exception e) {
    	        	Debugger.e( "network error in: " + e.getMessage());
    	        	notifyState(State.NETWORK_ERROR, null);
    	        	return;
    	        }

    	        try {
    	        	reg = new RegisterSession(sysConfig, crypto, is, os);
    	        	notifyState(State.REGISTER_RUNNING, fm);
    	        	reg.send();
    	        	notifyState(State.REGISTER_RUNNING, fm);
    	        	reg.read();
    	        	notifyState(State.REGISTER_RUNNING, fm);
    	        	int statuscode = reg.response.getResponseCode();
    	        	notifyState(State.REGISTER_RUNNING, fm);
    	            switch(statuscode) {
    	            case 401:
    	            	reg.postprocess();
    	            	break;
    	            default:
    	            	notifyState(State.REGISTER_FAIL, fm);
    	            	//sipcSocket.close();
    	            	return;
    	            }
    	        } catch (Exception e) {
    	        	if (null != reg) {
    	        		Debugger.e( "error in register session: " + e.getMessage());
    	        		notifyState(State.REGISTER_FAIL, fm);
    	        		return;
    	        	}
    	        }
    	        
    	        notifyState(State.REGISTER_SUCC, fm);
    	        
    	        notifyState(State.WAIT_AUTHENTICATE, fm);
    	        boolean retry = false;
    	        AuthenticationSession auth = null;
    	        do {
    	        	retry = false;
    		        try {
    		        	auth = new AuthenticationSession(sysConfig, crypto, is, os);//sipcSocket);
    		        	notifyState(State.AUTHENTICATE_RUNNING, fm);
    		        	auth.send(verification);
    		        
    		        	notifyState(State.AUTHENTICATE_RUNNING, fm);
    		        	auth.read();
    		        	notifyState(State.AUTHENTICATE_RUNNING, fm);
    		        	int statuscode = auth.response.getResponseCode();
    		        	notifyState(State.AUTHENTICATE_RUNNING, fm);
    		            switch(statuscode) {
    		            case 200:
    		            	//auth.postprocessContacts(null);
    		            	Debugger.d( "Process a junk");
    		            	auth.postprocessJunk();
    		            	retry = false;
    		            	break;
    		            case 420:
    		            case 421:
    		            	auth.postprocessVerification(verification);
    		            	notifyState(State.AUTHENTICATE_NEED_CONFIRM, fm);
    		            	synchronized(this) {
    		            		Debugger.d( "thread is sleeping");
    		                	wait();
    		                }
    		            	Debugger.d( "thread is awaken");
    		            	retry = true;
    		            	break;
    		            default:
    		            	notifyState(State.AUTHENTICATE_FAIL, fm);
    		            	//sipcSocket.close();
    		            	retry = false;
    		            	return;
    		            }
    		        } catch (Exception e) {
    		        	if (null != auth) {
    		        		Debugger.e( "error in authenticate session: " + e.getMessage());
    		        		notifyState(State.AUTHENTICATE_FAIL, fm);
    		        		retry = false;
    		        		return;
    		        	}
    		        }
    	        } while (retry);
    	        notifyState(State.AUTHENTICATE_SUCC, fm);
    			
    	        
    	        
    	        // send message
    	        try {
    				SipcCommand sendMsgCmd = new SipcSendMsgCommand(sysConfig.sId, 
    						fm.contact.sipUri, fm.msg);
    				os.write(sendMsgCmd.toString().getBytes());
    				Debugger.d( "Sent command: " + sendMsgCmd.toString());
    			} catch (Exception e) {
    				Debugger.e( "sending command failed");
    				notifyState(State.NETWORK_ERROR, fm);
    				return;
    			}
    			
    			SipcResponse m = null;
    			
    			try {
    				m = (SipcResponse)parser.parse(is);
    			}catch (Exception e) {
    				
    				Debugger.e( "send online msg command failed:" + e.getMessage());
    				
    				notifyState(State.NETWORK_ERROR, fm);
    				
    			}
				if (m == null) {
					Debugger.e( "got an mal-formated message");
				}
				else if (m.getResponseCode() == 200) {
					Debugger.d( "succeeded sending msg: " + m.toString());
    				
					long date = System.currentTimeMillis();
					String strd = m.getHeaderValue("D");
					if (strd != null) {
						date = Date.parse(strd);
						Debugger.d( "received date is " + DateFormat.format("yyyy-MM-dd kk:mm:ss", date));
					}
					
					
	        		// write the sent message to sms database
	        		SmsDbAdapter.insertSentSms(fm.contact.getSmsNumber(),
	        						date, fm.msg);
	        		notifyState(State.MSG_TRANSFERED, fm);
	        		
				}
				else if (m.getResponseCode() == 280) {
					Debugger.d( "succeeded sending msg by SMS: " + m.toString());
					long date = System.currentTimeMillis();
					String strd = m.getHeaderValue("D");
					if (strd != null) {
						date = Date.parse(strd);
						Debugger.d( "received date is " + DateFormat.format("yyyy-MM-dd kk:mm:ss", date));
					}
	        		// write the sent message to sms database
	        		SmsDbAdapter.insertSentSms(fm.contact.getSmsNumber(),
	        						date, fm.msg);
	        		notifyState(State.MSG_TRANSFERED_SMS, fm);
	        		
				}
				else {
					notifyState(State.MSG_FAILED, fm);
					Debugger.d( "sending msg failed: errno=" + m.getResponseCode());
					
				}
   	        
    			
    			// send drop
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
    		}
    		default:
    			break;
    		}
		}
    }
    
    @Override 
	public void run() {
		Looper.prepare();
		
		mHandler = new WorkHandler();
		
		Looper.loop();
    }
}
