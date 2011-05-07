package com.saturdaycoder.easyfetion;
import java.text.*;
import android.util.Log;
import java.lang.System;
import android.database.Cursor;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import java.util.ArrayList;
public class SmsDbAdapter
{
	private static ContentResolver cr = null;
	//private Cursor cursor;
	
	/*private static final int _id_index = 0;
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
	private static final int seen_index = 16;*/
	
	//private static SmsDbWriter inst = new SmsDbWriter();
	
	public static void setContext(Context context)
	{
		cr = context.getContentResolver();
	}
	
	public static ArrayList<AndroidSms> getSmsList(String mobileno) {
		ArrayList<AndroidSms> smsList = new ArrayList<AndroidSms>();
		String projection[] = new String[]{
				"address",
				"date",
				"read",
				"status",
				"type",
				"body"
		};
		String selection = "address like ?";
		String selectionArgs[] = new String[] {
				"%" + mobileno
		};
		String sortOrder = "date asc";
		Cursor cursor = cr.query(Uri.parse("content://sms"), 
				projection, selection, selectionArgs, sortOrder);
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
		//Log.d(TAG, "write sms into " + recvno);
		ContentValues values = new ContentValues();
		values.put("address", recvno);
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
	public static void insertReceivedSms(String fromno, long millis, String msg)
	{
		//Log.d(TAG, "write sms into " + recvno);
		ContentValues values = new ContentValues();
		values.put("address", fromno);
		values.put("date", millis);
		values.put("server_date", millis);
		values.put("read", 1);
		values.put("status", 0);
		values.put("locked", 0);
		values.put("error_code", 0);
		values.put("type", 1);
		values.put("seen", 1);
		values.put("body", msg);
		cr.insert(Uri.parse("content://sms/inbox"), values);
	}
	
}
