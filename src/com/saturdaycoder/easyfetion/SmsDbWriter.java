package com.saturdaycoder.easyfetion;
import java.text.*;
import android.util.Log;
import java.lang.System;
import android.database.Cursor;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

public class SmsDbWriter
{
	private ContentResolver cr = null;
	//private Cursor cursor;
	
	private static final int _id_index = 0;
	private static final int thread_id_index = 1;
	private static final int address_index = 2;
	private static final int person_index = 3;
	private static final int date_index = 4;
	private static final int server_date_index = 5;
	private static final int protocol_index = 6;
	private static final int read_index = 7;
	private static final int status_index = 8;
	private static final int type_index = 9;
	private static final int reply_path_present_index = 10;
	private static final int subject_index = 11;
	private static final int body_index = 12;
	private static final int service_center_index = 13;
	private static final int locked_index = 14;
	private static final int error_code_index = 15;
	private static final int seen_index = 16;
	
	//private static SmsDbWriter inst = new SmsDbWriter();
	
	public SmsDbWriter(Context context)
	{
		cr = context.getContentResolver();
	}
	
	
	
	public void insertSentSms(String recvno, long millis, String msg)
	{
		//Cursor cursor = cr.query(Uri.parse("content://sms/sent"),
		//		null, null, null, null);
		ContentValues values = new ContentValues();
		values.put("address", "12520" + recvno);
		values.put("date", millis);
		values.put("server_date", millis);
		values.put("read", 1);
		values.put("status", 0);
		values.put("locked", 0);
		values.put("error_code", 0);
		values.put("type", 2);
		values.put("seen", 1);
		values.put("body", msg);
		cr.insert(Uri.parse("content://sms/sent"), values);
	}
	
	
	
	/*private static String TAG = "EasyFetion";
	private static String dbFile = "/data/data/com.android.providers.telephony/databases/mmssms.db";
	//private static SQLiteDatabase.CursorFactory
	private static String getQueryThreadIdString(String mobileno)
	{
		String s = "select _id from threads where recipient_addresses=\'12520"
					+ mobileno + "\'";
		return s;
	}
	
	private static String getInsertThreadString(String mobileno, long time, int photo_id,
			int person_id, String recipientName)
	{
		String s = "insert into threads (date, server_date, message_count, "
			+ "unread_count, photo_id, recipient_addresses, recipient_names, is_sp, person_ids, "
			+ "snippet, snippet_cs, read, type, error, hasattachment) select "
			+ time + ","// date
			+ time + ","//server_date
			+ "0, " // message_count
			+ "0, " // unread_count
			+ photo_id + "," //photo_id
			+ "12520" + mobileno + "," // recipient
			+ recipientName + "," //recipient name
			+ "0, " // is_sp
			+ person_id + ", " // person_ids
			+ "0, " // snippet
			+ "0, " // snippet_cs
			+ "1, " // read
			+ "0, " // type
			+ "0, " // error
			+ "0 " // has attachment
			+ "where not exists (select * from threads where recipient_addresses = \'12520"
			+ mobileno + "\')"; // insert only if thread not exist
		return s;
	}
	
	private static String getInsertSmsString(String mobileno, long time, String msg)
	{
		String s = "insert into sms (thread_id, address, date, server_date, read,"
				+ "status, type, body, seen) values("
				+ "(select _id from threads where recipient_addresses = \'12520" 
				+ mobileno + "\'), " // thread_id
				+ mobileno + "," // address
				+ time + "," // date
				+ time + ", " // server_date
				+ "1," // read
				+ "0," // status
				+ "2," // type
				+ "\'" + msg + "\',"// body
				+ "1)"; // seen
		return s;
	}
	
	private static String getUpdateThreadString(String mobileno, long time)
	{
		String s = "update threads set message_count= "
			+ "(select message_count from threads where "
			+ "recipient_addresses='12520" + mobileno + "')+1 where "
			+ "recipient_addresses='12520" + mobileno + "'";
		return s;
	}
	
	public SmsDbWriter()
	{
		
	}
	
	public static boolean writeNewSms(String to, int personid,
			int photoid, String personname, String msg )
	{
		boolean succ = false;
		long now = System.currentTimeMillis();
		SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile, null, SQLiteDatabase.OPEN_READWRITE);//SQLiteDatabase.CursorFactory factory, int flags);
		db.beginTransaction();//开始事务
		try { 
			db.execSQL(getInsertThreadString(to, now, photoid,
					personid, personname));
			db.execSQL(getInsertSmsString(to, now, msg));
			db.execSQL(getUpdateThreadString(to, now));

			db.setTransactionSuccessful();//调用此方法会在执行到endTransaction() 时提交当前事务，如果不调用此方法会回滚事务
			succ = true;
		} finally {
		  db.endTransaction();//由事务的标志决定是提交事务，还是回滚事务
		  
		}  
		db.close();
		return succ;	
		
	}*/
	
}
