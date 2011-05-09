package com.saturdaycoder.easyfetion;

import android.database.Cursor;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import java.util.ArrayList;
public class SmsDbAdapter
{
	private static ContentResolver cr = null;
	private static final String TAG = "EasyFetion";
	//private Cursor cursor;
	
	private static int _id_index = -1;
	private static int thread_id_index = -1;
	private static int address_index = -1;
	private static int person_index = -1;
	private static int date_index = -1;
	private static int server_date_index = -1;
	private static int protocol_index = -1;
	private static int read_index = -1;
	private static int status_index = -1;
	private static int type_index = -1;
	private static int reply_path_present_index = -1;
	private static int subject_index = -1;
	private static int body_index = -1;
	private static int service_center_index = -1;
	private static int locked_index = -1;
	private static int error_code_index = -1;
	private static int seen_index = -1;
	
	private static final String _id_fieldname = "_id";
	private static final String thread_id_fieldname = "thread_id";
	private static final String address_fieldname = "address";
	private static final String person_fieldname = "person";
	private static final String date_fieldname = "date";
	private static final String server_date_fieldname = "server_date";
	private static final String protocol_fieldname = "protocol";
	private static final String read_fieldname = "read";
	private static final String status_fieldname = "status";
	private static final String type_fieldname = "type";
	private static final String reply_path_present_fieldname = "replay_path_present";
	private static final String subject_fieldname = "subject";
	private static final String body_fieldname = "body";
	private static final String service_center_fieldname = "service_center";
	private static final String locked_fieldname = "locked";
	private static final String error_code_fieldname = "error_code";
	private static final String seen_fieldname = "seen";
	
	
	static {

	}
	
	public static void setContext(Context context)
	{
		cr = context.getContentResolver();
		Cursor cursor = cr.query(Uri.parse("content://sms"), 
				null, null, null, null);
		
		_id_index = cursor.getColumnIndex(_id_fieldname);
		thread_id_index = cursor.getColumnIndex(thread_id_fieldname);
		address_index = cursor.getColumnIndex(address_fieldname);
		person_index = cursor.getColumnIndex(person_fieldname);
		date_index = cursor.getColumnIndex(date_fieldname);
		server_date_index = cursor.getColumnIndex(server_date_fieldname);
		protocol_index = cursor.getColumnIndex(protocol_fieldname);
		read_index = cursor.getColumnIndex(read_fieldname);
		status_index = cursor.getColumnIndex(status_fieldname);
		type_index = cursor.getColumnIndex(type_fieldname);
		reply_path_present_index = cursor.getColumnIndex(reply_path_present_fieldname);
		subject_index = cursor.getColumnIndex(subject_fieldname);
		body_index = cursor.getColumnIndex(body_fieldname);
		service_center_index = cursor.getColumnIndex(service_center_fieldname);
		locked_index = cursor.getColumnIndex(locked_fieldname);
		error_code_index = cursor.getColumnIndex(error_code_fieldname);
		seen_index = cursor.getColumnIndex(seen_fieldname);
	}
	
	public static ArrayList<AndroidSms> getSmsList(String mobileno) {
		ArrayList<AndroidSms> smsList = new ArrayList<AndroidSms>();
		String projection[] = new String[]{
				address_fieldname,
				date_fieldname,
				read_fieldname,
				status_fieldname,
				type_fieldname,
				body_fieldname
		};
		String selection = "address like ?";
		String selectionArgs[] = new String[] {
				"%" + mobileno
		};
		String sortOrder = "date asc";
		Cursor cursor = cr.query(Uri.parse("content://sms"), 
				projection, selection, selectionArgs, sortOrder);
		
		//debug
		//int count = cursor.getColumnCount();
		//for (int i = 0; i < count; ++i) {
		//	Debugger.v( "column[" + i + "]=" + cursor.getColumnName(i));
		//}
		//debug
		
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				AndroidSms sms = new AndroidSms();
				sms.mobileno = cursor.getString(0);
				sms.date = cursor.getLong(1);
				sms.read = cursor.getInt(2);
				sms.status = cursor.getInt(3);
				sms.type = cursor.getInt(4);
				sms.body = cursor.getString(5);
				smsList.add(sms);
				cursor.moveToNext();
			}
		}
		return smsList;
	}
	
	public static void insertSentSms(String recvno, long millis, String msg)
	{
		
		ContentValues values = new ContentValues();
		values.put(address_fieldname, recvno);
		values.put(date_fieldname, millis);
		if (server_date_index !=-1)
			values.put(server_date_fieldname, millis);
		values.put(read_fieldname, 1);
		values.put(status_fieldname, 0);
		if (locked_index != -1)
			values.put(locked_fieldname, 0);
		if (error_code_index != -1)
			values.put(error_code_fieldname, 0);
		values.put(type_fieldname, 2);
		if (seen_index != -1)
			values.put(seen_fieldname, 1);
		values.put(body_fieldname, msg);
		
		
		Uri sent = Uri.parse("content://sms/sent");
		//Uri allsms = Uri.parse("content://sms");
		
		Uri inserted = cr.insert(sent, values);
		Debugger.d( "write sms into " + recvno + ": " + inserted.toString());
	}
	public static void insertReceivedSms(String fromno, long millis, String msg)
	{	
		ContentValues values = new ContentValues();
		values.put(address_fieldname, fromno);
		values.put(date_fieldname, millis);
		if (server_date_index !=-1)
			values.put(server_date_fieldname, millis);
		values.put(read_fieldname, 1);
		values.put(status_fieldname, -1);
		if (locked_index != -1)
			values.put(locked_fieldname, 0);
		if (error_code_index != -1)
			values.put(error_code_fieldname, 0);
		values.put(type_fieldname, 1);
		if (seen_index != -1)
			values.put(seen_fieldname, 1);
		values.put(body_fieldname, msg);
		
		Uri inbox = Uri.parse("content://sms/inbox");
		//Uri allsms = Uri.parse("content://sms");
		
		Uri inserted = cr.insert(inbox, values);
		Debugger.d( "write sms into " + fromno + ": " + inserted.toString());
	}
	
}
