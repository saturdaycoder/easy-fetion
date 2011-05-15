package com.saturdaycoder.easyfetion;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.saturdaycoder.easyfetion.SipcCommand.CommandType;
import com.saturdaycoder.easyfetion.SipcMessage.Type;



public class SipcThread extends Thread{
	InputStream is = null;
    OutputStream os = null;
	private SystemConfig sysConfig;
	private Handler uiHandler;
	private Crypto crypto;
    private Handler mHandler;
    private Map<String, FetionContact> contactList;
    //private Map<String, String> portraitList;
    public FetionPictureVerification verification;
    public State state;
    
    public boolean pendingMsg = false;
    
    public enum State {
    	INIT,
    	
    	CONNECTING_SIPC,
    	CONNECTING_SUCC,
    	CONNECTING_FAIL,
    	
    	DISCONNECTING_SIPC,
    	DISCONNECTING_SUCC,
    	DISCONNECTING_FAIL,
    	
    	WAIT_REGISTER,
    	REGISTER_SENDING,
    	REGISTER_READING,
    	REGISTER_POSTPROCESSING,
    	REGISTER_FAIL,
    	REGISTER_SUCC,
    	
    	
    	WAIT_AUTHENTICATE,
    	AUTHENTICATE_SENDING,
    	AUTHENTICATE_READING,
    	AUTHENTICATE_POSTPROCESSING,
    	AUTHENTICATE_NEED_CONFIRM,
    	AUTHENTICATE_SUCC,
    	AUTHENTICATE_FAIL,
    	
    	WAIT_GET_CONTACT,
    	CONTACT_GETTING,
    	CONTACT_GET_SUCC,
    	CONTACT_GET_FAIL,

        WAIT_DROP,
        DROP_SENDING,
        DROP_READING,
        DROP_POSTPROCESSING,
        DROP_FAIL,
        DROP_SUCC,
        
        WAIT_SEND_MSG,
        SEND_MSG_SENDING,
        SEND_MSG_READING,
        SEND_MSG_POSTPROCESSING,
        SEND_MSG_SUCC_ONLINE,
        SEND_MSG_SUCC_SMS,
        SEND_MSG_RESPONSE_TIMEOUT,
        SEND_MSG_FAIL,
        
    	THREAD_EXIT,
    	
    	NETWORK_DOWN,
    	NETWORK_TIMEOUT,
    }
    public enum Command {
    	CONNECT_SIPC,
    	DISCONNECT_SIPC,
    	REGISTER,
    	AUTHENTICATE,
    	GET_CONTACTS,
    	SEND_MSG,
    	SEND_SMS,
    	DROP,
    	EXIT,
    	EXIT_AFTER_SEND,
    }
    public class ThreadCommand {
    	Command cmd;
    	Object arg;
    }
    public class ThreadState {
    	State state;
    	Object arg;
    }

    private static SipcThread instance = null;
    
    public static SipcThread getInstance() {
    	return instance;
    }
    
    public void addCommand(Command cmd, Object arg) {
    	if (mHandler == null) {
    		Debugger.error("handler is null!!!!");
    		return;
    	}
    	Message msg = mHandler.obtainMessage();
    	ThreadCommand tc = new ThreadCommand();
    	tc.cmd = cmd;
    	tc.arg = arg;
    	msg.obj = tc;
    	mHandler.sendMessage(msg);
    }
    
    
    
    protected void notifyState(State state, Object arg){
    	this.state = state;

    	Message msg = uiHandler.obtainMessage();
    	ThreadState ts = new ThreadState();
    	ts.state = state;
    	ts.arg = arg;
    	msg.obj = ts;
    	uiHandler.sendMessage(msg);
    }
    public SipcThread(SystemConfig sysConfig, Crypto crypto, 
    		Map<String, FetionContact> contactList, Handler uiHandler)
	{
		this.sysConfig = sysConfig;
		this.uiHandler = uiHandler;
		this.crypto = crypto;
		this.contactList = contactList;
		verification  = new FetionPictureVerification();
		instance = this;
	}

    private void doConnectSipc(Object arg) {
    	notifyState(State.CONNECTING_SIPC, arg);
        try {
        	Network.closeSipcSocket();
        	Network.createSipcSocket(sysConfig.sipcProxyIp, sysConfig.sipcProxyPort);
        	is = Network.getSipcInputStream();
        	os = Network.getSipcOutputStream();
        	notifyState(State.CONNECTING_SUCC, arg);
        } catch (Exception e) {
        	Debugger.error( "error re-create sipc socket: " + e.getMessage());
        	notifyState(State.CONNECTING_FAIL, arg);
        }
    }
    private void doDisconnectSipc(Object arg) {
    	notifyState(State.DISCONNECTING_SIPC, arg);
		try {
        	Network.closeSipcSocket();
        	is = null;
        	os = null;
        	notifyState(State.DISCONNECTING_SUCC, arg);                	
        } catch (Exception e) {
        	Debugger.error( "error close sipc socket: " + e.getMessage());
        	notifyState(State.DISCONNECTING_FAIL, arg);
        }
    }
    
    private void doRegister(Object arg) {
    	notifyState(State.WAIT_REGISTER, arg);
        
        RegisterSession reg = new RegisterSession(sysConfig, crypto, is, os);
        notifyState(State.REGISTER_SENDING, arg);
        
        try {
        	reg.send();
        } catch (Exception e) {
        	Debugger.error("error sending reg command: " + e.getMessage());
        	notifyState(State.NETWORK_DOWN, arg);
        	return;
        }
        notifyState(State.REGISTER_READING, arg);
        try {
        	reg.read();
        } catch (java.net.SocketTimeoutException e) {
        	Debugger.error("timeout reading reg response: " + e.getMessage());
        	notifyState(State.NETWORK_TIMEOUT, arg);
        	return;
        } catch (Exception e) {
        	Debugger.error("error reading reg response: " + e.getMessage());
        	notifyState(State.REGISTER_FAIL, arg);
        	return;
        }
        notifyState(State.REGISTER_POSTPROCESSING, arg);
    	int statuscode = reg.response.getResponseCode();
    	
        switch(statuscode) {
        case 401:
        	reg.postprocess();
        	notifyState(State.REGISTER_SUCC, arg);
        	return;
        default:
        	notifyState(State.REGISTER_FAIL, arg);
        	return;
        }
    }
    
    private void doAuthenticate(Object arg) {
    	notifyState(State.WAIT_AUTHENTICATE, arg);
        
        AuthenticationSession auth = new AuthenticationSession(sysConfig, crypto, is, os);
        notifyState(State.AUTHENTICATE_SENDING, arg);
        try {
        	auth.send(verification);
        	verification.clear();
        } catch (Exception e) {
        	Debugger.error("error sending auth command: " + e.getMessage());
        	notifyState(State.NETWORK_DOWN, arg);
        }
        notifyState(State.AUTHENTICATE_READING, arg);
        try {
        	auth.read();
        } catch (java.net.SocketTimeoutException e) {
        	Debugger.error("timeout reading auth response: " + e.getMessage());
        	notifyState(State.NETWORK_TIMEOUT, arg);
        	return;
        } catch (Exception e) {
        	Debugger.error("error receiving auth response: " + e.getMessage());
        	notifyState(State.NETWORK_DOWN, arg);
        }
    	notifyState(State.AUTHENTICATE_POSTPROCESSING, arg);
    	int statuscode = auth.response.getResponseCode();
        switch(statuscode) {
        case 200:
        	auth.postprocessContacts(contactList);
        	Debugger.debug( "Process a junk");
        	auth.postprocessJunk();
        	notifyState(State.AUTHENTICATE_SUCC, arg);
        	break;
        case 420:
        case 421:
        	auth.postprocessVerification(verification);
        	notifyState(State.AUTHENTICATE_NEED_CONFIRM, arg);
        	break;
        default:
        	notifyState(State.AUTHENTICATE_FAIL, arg);
        	break;
        }
    }
    
    private void doSubContacts() {
    	notifyState(State.WAIT_GET_CONTACT, null);
    	SipcMessageParser parser = new SipcMessageParser();
    	SipcSubscribeCommand cmd = new SipcSubscribeCommand(sysConfig.sId);
    	try {
    		os.write(cmd.toString().getBytes());
    	} catch (Exception e) {
    		Debugger.error("error sending SUB command: " + e.getMessage());
    		notifyState(State.CONTACT_GET_FAIL, null);
    		return;
    	}
    	SipcResponse r = null;
    	try {
    		r = (SipcResponse)parser.parse(is);
    	} catch (Exception e) {
    		Debugger.error("error receiving SUB response: " + e.getMessage());
    		notifyState(State.CONTACT_GET_FAIL, null);
    		return;
    	}
    	if (r == null || r.getResponseCode() != 200) {
    		Debugger.error("SUB response not OK");
    		notifyState(State.CONTACT_GET_FAIL, null);
    		return;
    	}

    	int count = contactList.size() + 1;
    	int j = 0;
    	while (true) {
    		/*int available = -1;
    		try {
    			available = is.available();
    		} catch (Exception e) {
    			Debugger.error("error getting available bit count from SIPC inputstream: " + e.getMessage());
    			continue;
    		}
    		
    		if (available == 0) {
    			Debugger.info("no more available bits, exit loop");
    			break;
    		}*/
    		SipcCommand c = null;
        	try {
        		c = (SipcCommand)parser.parse(is);
        		Debugger.info(c.toString());
        		if (c.getCommandType() != CommandType.BENOTIFY 
        				|| !c.getHeaderValue("N").equals("PresenceV4")) {
        			Debugger.warn("non BENOTIFY or Presence command. ignore");
        			
        			continue;
        		}
        	} catch (java.net.SocketTimeoutException timeoute) {
        		Debugger.error("timeout receiving SUB response: " + timeoute.getMessage());
        		notifyState(State.CONTACT_GET_FAIL, null);
        		return;
        	} catch (IOException e) {
        		Debugger.error("error receiving SUB response: " + e.getMessage());
        		notifyState(State.CONTACT_GET_FAIL, null);
        		return;
        	}
        	
        	try {
        		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
        		DocumentBuilder db = dbf.newDocumentBuilder();
    			Document document = db.parse(new ByteArrayInputStream(c.body.getBytes()));
    			Node contactsnode = document.getFirstChild().getFirstChild().getFirstChild();
    			
    			NodeList nl = contactsnode.getChildNodes();
        		for (int i = 0; i < nl.getLength(); ++i) {
        			
        			Node cnode = nl.item(i);
	    			String userid = cnode.getAttributes().getNamedItem("id").getNodeValue();
	    			Node pnode = cnode.getFirstChild();
	    			String sipuri = pnode.getAttributes().getNamedItem("su").getNodeValue();
	    			FetionContact fc = null;
	    			if (contactList.containsKey(sipuri)) {
	    				fc = contactList.get(sipuri);
	    			}
	    			else {
	    				fc = new FetionContact();
	    				fc.sipUri = sipuri;
	    				contactList.put(sipuri, fc);
	    			}
	    			Debugger.warn("Got NOTIFY of contact " + sipuri);
	    			fc.userId = userid;
	    			fc.mobileNumber = pnode.getAttributes().getNamedItem("m").getNodeValue();
	    			fc.nickName = pnode.getAttributes().getNamedItem("n").getNodeValue();
	    			fc.localName = pnode.getAttributes().getNamedItem("i").getNodeValue();
	    			
	    			j++;
	        	}
        		if (j == count) {
        			Debugger.debug("all contact info are notified. exit loop");
        			break;
        		}
        		
        	} catch (Exception e) {
        		Debugger.error("error parsing SUB response: " + e.getMessage());
        		//notifyState(State.CONTACT_GET_FAIL, null);
        		//return;
        	}
	        
	        
    	}
           
        notifyState(State.CONTACT_GET_SUCC, null);
    }
    
    
    
    private boolean doGetContact(FetionContact fc) {
    	SipcMessageParser parser = new SipcMessageParser();
    	SipcContactInfoCommand cmd = new SipcContactInfoCommand(sysConfig.sId, 
				fc.sipUri);
    	try {
    		os.write(cmd.toString().getBytes());
    	} catch (Exception e) {
    		Debugger.error("error sending get contact command: " + e.getMessage());
    		return false;
    	}
    	SipcMessage sm = null;
    	try {
    		sm = (SipcMessage)parser.parse(is);
    	} catch (java.net.SocketTimeoutException e) {
        	Debugger.error("timeout reading contact detail: " + e.getMessage());
        	notifyState(State.NETWORK_TIMEOUT, null);
        	return false;
        } catch (Exception e) {
    		Debugger.error("error receiving get contact response: " + e.getMessage());
    		return false;
    	}
        
        if (sm == null) {
    		Debugger.error("wrong sipc message format");
    		return false;
    	}
        
        SipcResponse res = null;
        
        switch(sm.getType()) {
        case TYPE_REQUEST:
        	Debugger.warn("got an incoming request:");
        	Debugger.warn(sm.toString());
        	Debugger.warn("Ignore it");
        	return false;
        case TYPE_RESPONSE:
        	res = (SipcResponse)sm;
        	break;
        case TYPE_UNKNOWN:
        	Debugger.error("wrong get-contact response format");
        	return false;
        }
        

		if (res.getResponseCode() == 200) {
			int mobilenoind = res.body.indexOf("mobile-no=\"");
			int nicknameind = res.body.indexOf("nickname=\"");
			String mobileno, nickname;
			if (mobilenoind != -1) {
				mobileno = res.body.substring(mobilenoind + 11);
				mobileno = mobileno.substring(0, mobileno.indexOf("\""));
			} else {
				mobileno = "";
			}
			fc.mobileNumber = mobileno;
			if (nicknameind != -1) {
				nickname = res.body.substring(nicknameind + 10);
				nickname = nickname.substring(0, nickname.indexOf("\""));
			} else {
				nickname = "";
			}
			fc.nickName = nickname;
			Debugger.debug( "got user detail: nickname=" + nickname + " no = " + mobileno);
			return true;
		}
		else {
			Debugger.error("incorrect get-contact response status for " + fc.sipUri);
			Debugger.error(res.toString());
    		return false;
		}
    }
    
    private void doGetContacts() {
    	notifyState(State.WAIT_GET_CONTACT, null);
        
        Iterator<String> iter = contactList.keySet().iterator();
        while (iter.hasNext())
        {
        	notifyState(State.CONTACT_GETTING, null);
        	String uri = iter.next();
        	FetionContact fc = contactList.get(uri);
        	doGetContact(fc);
        }
        
        notifyState(State.CONTACT_GET_SUCC, null);
    }
    
    private void doSendMsg(FetionMsg fm) {
        notifyState(State.WAIT_SEND_MSG, fm);
    	
    	SipcMessageParser parser = new SipcMessageParser();
    	SipcCommand sendMsgCmd = new SipcSendMsgCommand(sysConfig.sId, 
				fm.contact.sipUri, fm.msg);
    	
    	notifyState(State.SEND_MSG_SENDING, fm);
        try {
			os.write(sendMsgCmd.toString().getBytes());
			Debugger.debug( "Sent command: " + sendMsgCmd.toString());
		} catch (Exception e) {
			Debugger.error( "sending command failed: " + e.getMessage());
			notifyState(State.SEND_MSG_FAIL, fm);
			return;
		}
		
		SipcResponse m = null;
		notifyState(State.SEND_MSG_READING, fm);
		try {
			m = (SipcResponse)parser.parse(is);
		} catch (java.net.SocketTimeoutException e) {
			Debugger.error( "send online msg command timed out:" + e.getMessage());
			
			SmsDbAdapter.insertSentSms(fm.contact.getSmsNumber(),
					System.currentTimeMillis(), fm.msg);
			notifyState(State.SEND_MSG_RESPONSE_TIMEOUT, fm);
			
			return;
		} catch (Exception e) {
			Debugger.error( "send online msg command failed:" + e.getMessage());
			SmsDbAdapter.insertSentSms(fm.contact.getSmsNumber(),
					System.currentTimeMillis(), fm.msg);
			notifyState(State.SEND_MSG_RESPONSE_TIMEOUT, fm);
			
			return;
		}
		
		notifyState(State.SEND_MSG_POSTPROCESSING, fm);
		if (m == null) {
			Debugger.error( "got an mal-formated message");
			return;
		}
		else if (m.getResponseCode() == 200) {
			Debugger.debug( "succeeded sending msg: " + m.toString());
			
			long date = System.currentTimeMillis();
			String strd = m.getHeaderValue("D");
			if (strd != null) {
				date = Date.parse(strd);
				Debugger.debug( "received date is " + DateFormat.format("yyyy-MM-dd kk:mm:ss", date));
			}
    		SmsDbAdapter.insertSentSms(fm.contact.getSmsNumber(),
    						date, fm.msg);
    		notifyState(State.SEND_MSG_SUCC_ONLINE, fm);
    		return;
		}
		else if (m.getResponseCode() == 280) {
			Debugger.debug( "succeeded sending msg by SMS: " + m.toString());
			long date = System.currentTimeMillis();
			String strd = m.getHeaderValue("D");
			if (strd != null) {
				date = Date.parse(strd);
				Debugger.debug( "received date is " + DateFormat.format("yyyy-MM-dd kk:mm:ss", date));
			}
    		// write the sent message to sms database
    		SmsDbAdapter.insertSentSms(fm.contact.getSmsNumber(),
    						date, fm.msg);
    		notifyState(State.SEND_MSG_SUCC_SMS, fm);
    		return;
		}
		else {
			notifyState(State.SEND_MSG_FAIL, fm);
			Debugger.debug( "sending msg failed: errno=" + m.getResponseCode());
			return;
		}
    }
    private void doDrop(Object arg) {
    	notifyState(State.WAIT_DROP, arg);
    	SipcMessageParser parser = new SipcMessageParser();
    	SipcDropCommand cmd = new SipcDropCommand(sysConfig.sId);
        
    	notifyState(State.DROP_SENDING, arg);
    	try {
    		os.write(cmd.toString().getBytes());
    	} catch (Exception e) {
    		
    	}
    	SipcResponse res = null;
    	notifyState(State.DROP_READING, arg);
		try {
			res = (SipcResponse)parser.parse(is);
		} catch (java.net.SocketTimeoutException e) {
        	Debugger.error("timeout reading drop response: " + e.getMessage());
        	notifyState(State.DROP_SUCC, arg);
        	pendingMsg = false;
        	return;
        } catch (Exception e) {
			notifyState(State.DROP_SUCC, arg);
			pendingMsg = false;
			return;
		}
		if (res == null) {
			notifyState(State.DROP_SUCC, arg);
			pendingMsg = false;
			return;
		}
		
		notifyState(State.DROP_POSTPROCESSING, arg);
		if (res.getResponseCode() == 200) {
			// TODO: what to do?
		}
		else {
			// TODO: what to do?
		}
		notifyState(State.DROP_SUCC, arg);
		pendingMsg = false;
    }
    private void doExitAfterSend()
    {
    	while (pendingMsg == true) {
    		try  {
    			Thread.sleep(1000, 0);
    		} catch (Exception e) {
    			
    		}
    	}
    	notifyState(State.THREAD_EXIT, null);
    	//popNotify("程序完全退出");
    	Looper.myLooper().quit();
    }

    private class WorkHandler extends Handler {
        @Override
        public void handleMessage(Message msg) 
		{

    		ThreadCommand tc = (ThreadCommand)msg.obj;
    		Object arg = tc.arg;
    		switch (tc.cmd) {
    		case CONNECT_SIPC:
    			doConnectSipc(arg);
                break;
    		case DISCONNECT_SIPC:
    			doDisconnectSipc(arg);
                break;
            case REGISTER: 
            	doRegister(arg);
                break;
            case AUTHENTICATE: 
            	doAuthenticate(arg);
	            break;
            case GET_CONTACTS:
                //doGetContacts();
            	doSubContacts();
                break;
            case DROP:
                doDrop(arg);
                break;
            case SEND_MSG:
            	doSendMsg((FetionMsg)tc.arg);
            	break;
            case EXIT_AFTER_SEND:
            	doExitAfterSend();
            	break;
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
