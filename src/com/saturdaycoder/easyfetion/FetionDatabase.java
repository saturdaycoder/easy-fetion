package com.saturdaycoder.easyfetion;
//import android.content.ContentValues;
import android.content.Context;
import java.util.ArrayList;
import android.database.Cursor;
//import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
//import android.hardware.SensorManager;
import android.util.Log;
public class FetionDatabase extends SQLiteOpenHelper  
{
	private static final String TAG="EasyFetion";
	private static final String DATABASE_NAME = "userdb";
	private static final int DB_VERSION = 1;
	private static final boolean encryptUserPasswd = true;
	public FetionDatabase(Context context) {
		super(context, DATABASE_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "FetionDatabase onCreate");
		db.execSQL("CREATE TABLE contacts (uri TEXT primary key, "
				+ " version TEXT DEFAULT '', "
				+ " sid TEXT DEFAULT '', "
				+ " mobile_no TEXT DEFAULT '', "
				+ " basic_service_status INTEGER DEFAULT -1, "
				+ " carrier TEXT DEFAULT '', "
				+ " carrier_status INTEGER DEFAULT -1, "
				+ " portrait_crc TEXT DEFAULT '', "
				+ " name TEXT DEFAULT '', "
				+ " nickname TEXT DEFAULT '', "
				+ " gender INTEGER DEFAULT -1, "
				+ " birth_date TEXT DEFAULT '', "
				+ " birthday_valid INTEGER DEFAULT -1, "
				+ " impresa TEXT DEFAULT '', "
				+ " carrier_region TEXT DEFAULT '', "
				+ " user_region TEXT DEFAULT '', "
				+ " profile TEXT DEFAULT '', "
				+ " blood_type INTEGER DEFAULT -1, "
				+ " occupation TEXT DEFAULT '', "
				+ " hobby TEXT DEFAULT '', "
				+ " score_level INTEGER DEFAULT -1)");
		db.execSQL("CREATE TABLE user (sid TEXT primary key, "
				+ " password TEXT, "
				+ " userid TEXT, "
				+ " mobile_number TEXT, "
				+ " config_servers_version TEXT DEFAULT '', "
				+ " config_parameters_version TEXT DEFAULT '', "
				+ " config_hints_version TEXT DEFAULT '', "
				+ " sipc_proxy_ip TEXT DEFAULT '', "
				+ " sipc_proxy_port INTEGER DEFAULT -1, "
				+ " portrait_servers_name TEXT DEFAULT '', "
				+ " portrait_servers_path TEXT DEFAULT '', "
				+ " personal_version TEXT DEFAULT '', "
				+ " custom_config_version TEXT DEFAULT '', "
				+ " contact_version TEXT DEFAULT '')");
		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		Log.d(TAG, "FetionDatabase onUpgrade");
		//android.util.Log.w("Constants", "Upgrading database, which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS user");
		db.execSQL("DROP TABLE IF EXISTS contacts");
		onCreate(db);
	}
	
	/*public void updateContacts(FetionContact contacts[])
	{
		SQLiteDatabase db = this.getWritableDatabase();
	}*/
	
	public void setContact(FetionContact contact)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery("select * from contacts where uri='" 
				+ contact.sipUri +"'", null);
		if (cursor.moveToFirst()) {
			Log.d(TAG, "update contact " + contact.sipUri);
			db.execSQL("update contacts set uri='" + contact.sipUri + "',"
				+ "version='"+contact.version+"',"//+ " version TEXT, "
				+ "sid='" + contact.sId + "',"//+ " sid TEXT, "
				+ "mobile_no='" + contact.mobileNumber + "',"//+ " mibile_no TEXT, "
				//+ " basic_service_status INTEGER, "
				+ "carrier='" + contact.carrier + "',"//+ " carrier TEXT, "
				+ "carrier_status=" + contact.carrierStatus + ","//+ " carrier_status INTEGER, "
				+ "portrait_crc='" + contact.portraitCrc + "',"//+ " portrait_crc TEXT, "
				+ "name='" + contact.localName + "',"//+ " name TEXT, "
				+ "nickname='" + contact.nickName + "',"//+ " nickname TEXT, "
				+ "gender=" + contact.gender + ","//+ " gender INTEGER, "
				+ "birth_date='" + contact.birthday + "',"//+ " birth_date TEXT, "
				//+ " birthday_valid INTEGER, "
				+ "impresa='" + contact.impression + "',"//+ " impresa TEXT, "
				//+ " carrier_region TEXT, "
				//+ " user_region TEXT, "
				//+ " profile TEXT, "
				//+ " blood_type INTEGER, "
				//+ " occupation TEXT, "
				//+ " hobby TEXT, "
				+ "score_level=" + contact.scoreLevel);//+ " score_level INTEGER)");
				
		}
		else {
			Log.d(TAG, "insert contact " + contact.sipUri);
			db.execSQL("insert into contacts (uri, version, sid, mobile_no," +
					"carrier, carrier_status, portrait_crc, name, nickname, gender, birth_date," +
					"impresa," +
					"score_level) values('" + contact.sipUri + "', '" + contact.version
					+ "','" + contact.sId + "','" + contact.mobileNumber + "','"
					+ contact.carrier + "'," + contact.carrierStatus + ",'"
					+ contact.portraitCrc + "','" + contact.localName + "','"
					+ contact.nickName + "'," + contact.gender + ",'"
					+ contact.birthday + "','" + contact.impression + "',"
					+ contact.scoreLevel + ")");
		}
	}
	
	public void removeContact(FetionContact contact)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("delete from contacts where sid='" + contact.sipUri + "'");
	}
	
	public boolean hasContactByUri(String sipuri)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Log.d(TAG, "query contact db for '" + sipuri + "'");
		Cursor cursor = db.rawQuery("select * from contacts where uri='" + sipuri + "'", null);
		if (cursor.moveToFirst())
			return true;
		else 
			return false;
	}
	
	public FetionContact getContactByUri(String sipuri)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("select * from contacts where uri='" + sipuri + "'", null);
		if (cursor.moveToFirst())
		{
			FetionContact c = new FetionContact();
			c.sipUri = cursor.getString(0); //uri TEXT primary key, "
			c.version = cursor.getString(1);//+ " version TEXT, "
			c.sId = cursor.getString(2);//+ " sid TEXT, "
			c.mobileNumber = cursor.getString(3);//+ " mibile_no TEXT, "
			//+ " basic_service_status INTEGER, "
			c.carrier = cursor.getString(5);//+ " carrier TEXT, "
			c.carrierStatus = cursor.getInt(6);//+ " carrier_status INTEGER, "
			c.portraitCrc = cursor.getString(7);//+ " portrait_crc INTEGER, "
			c.localName = cursor.getString(8);//+ " name TEXT, "
			c.nickName = cursor.getString(9);//+ " nickname TEXT, "
			c.gender = cursor.getInt(10);//+ " gender INTEGER, "
			c.birthday = cursor.getString(11);//+ " birth_date TIME, "
			//+ " birthday_valid INTEGER, "
			c.impression = cursor.getString(13);//+ " impresa TEXT, "
			//+ " carrier_region TEXT, "
			//+ " user_region TEXT, "
			//+ " profile TEXT, "
			//+ " blood_type INTEGER, "
			//+ " occupation TEXT, "
			//+ " hobby TEXT, "
			c.scoreLevel = cursor.getInt(20);//+ " score_level INTEGER)");
			return c;
		}
		else return null;
	}
	
	public FetionContact[] getContacts()
	{
		SQLiteDatabase db = this.getReadableDatabase();
		ArrayList<FetionContact> contactList = new ArrayList<FetionContact>();
		Cursor cursor = db.rawQuery("select * from contacts", null);
		if (cursor.moveToFirst()) 
		{
			do {
				FetionContact c = new FetionContact();
				c.sipUri = cursor.getString(0); //uri TEXT primary key, "
				c.version = cursor.getString(1);//+ " version TEXT, "
				c.sId = cursor.getString(2);//+ " sid TEXT, "
				c.mobileNumber = cursor.getString(3);//+ " mibile_no TEXT, "
				//+ " basic_service_status INTEGER, "
				c.carrier = cursor.getString(5);//+ " carrier TEXT, "
				c.carrierStatus = cursor.getInt(6);//+ " carrier_status INTEGER, "
				c.portraitCrc = cursor.getString(7);//+ " portrait_crc INTEGER, "
				c.localName = cursor.getString(8);//+ " name TEXT, "
				c.nickName = cursor.getString(9);//+ " nickname TEXT, "
				c.gender = cursor.getInt(10);//+ " gender INTEGER, "
				c.birthday = cursor.getString(11);//+ " birth_date TIME, "
				//+ " birthday_valid INTEGER, "
				c.impression = cursor.getString(13);//+ " impresa TEXT, "
				//+ " carrier_region TEXT, "
				//+ " user_region TEXT, "
				//+ " profile TEXT, "
				//+ " blood_type INTEGER, "
				//+ " occupation TEXT, "
				//+ " hobby TEXT, "
				c.scoreLevel = cursor.getInt(20);//+ " score_level INTEGER)");
				contactList.add(c);
			} while (cursor.moveToNext());
		}
		FetionContact ca[] = new FetionContact[contactList.size()];
		return contactList.toArray(ca);
	}
	
	
	public void setAccount(SystemConfig sysConfig)
	{
		Log.d(TAG, "FetionDatabase setAccount, write account info to db");
		SQLiteDatabase db = this.getWritableDatabase();
		//db.execSQL("delete from user");
		String savedPasswd;
		if (encryptUserPasswd) {
			byte enc[] = Crypto.base64Encode(sysConfig.userPassword.getBytes());
			savedPasswd = new String(enc);
		} else {
			savedPasswd = sysConfig.userPassword;
		}
		// try update
		Cursor cursor = db.rawQuery("select * from user", null);
		if (cursor.moveToFirst()) {
			cursor = db.rawQuery("select * from user where sid<>'" 
					+ sysConfig.sId + "'", null);
			if (cursor.moveToFirst()) {
				Log.d(TAG, "DB DELETE NOT MATCHING ACC");
				db.execSQL("delete from user");
			}
			db.execSQL("update user set password='" + savedPasswd + "'"
					+ ", userId='" + sysConfig.userId + "'"
					+ ", mobile_number='" + sysConfig.mobileNumber + "'"
					+ " where sid='" + sysConfig.sId + "'");
			Log.d(TAG, "DB UPDATE ACC");
		}
		// if not exist, insert new
		else {
			db.execSQL("insert into user (sid, password, userid, mobile_number) "
				+ "select '" + sysConfig.sId + "', "
				+ "'" + savedPasswd + "', "
				+ "'" + sysConfig.userId + "', "
				+ "'" + sysConfig.mobileNumber + "' " 
				+ " where not exists (select * from user where sid = '" 
				+ sysConfig.sId + "')");
			Log.d(TAG, "INSERT NEW ACC");
		}
	}
	
	public void getAccount(SystemConfig sysConfig)
	{
		Log.d(TAG, "FetionDatabase getAccount");
		SQLiteDatabase db = this.getWritableDatabase();

		Cursor cursor = db.rawQuery("select * from user", null);
		String plainPasswd = "";

		Log.d(TAG, "cursor queried");
		if (cursor.moveToFirst()) 
		{
			if (encryptUserPasswd) {
				plainPasswd = new String(Crypto.base64Decode(cursor.getString(1).getBytes()));
			} else {
				plainPasswd = cursor.getString(1);
			}
			
			Log.d(TAG, "found existing account: ");
			
			//plainPasswd = plainPasswd.substring(plainPasswd.indexOf("Fuck Fetion:") + 12);
			sysConfig.userPassword = plainPasswd;

			sysConfig.mobileNumber = cursor.getString(3);
			Log.d(TAG, "passwd=" + sysConfig.userPassword
					+ " mobileno=" + sysConfig.mobileNumber);
		}
		else 
		{
			Log.d(TAG, "found no matching account");

			sysConfig.userPassword = "";

			sysConfig.mobileNumber = "";
		}
	}
	
	public void getUserInfo (SystemConfig sysConfig)
	{
		Log.d(TAG, "EasyFetion getUserInfo");
		SQLiteDatabase db = this.getReadableDatabase();
		/*String sel[] = new String[] {
				"config_servers_version",
				"config_parameters_version",
				"config_hints_version",
				"sipc_proxy_ip",
				"sipc_proxy_port",
				"portrait_servers_name",
				"portrait_servers_path",
				"personal_version",
				"custom_config_version",
				"contact_version"
		};*/
		//Cursor cursor = db.rawQuery("select * from user where sid = '" 
		//		+ sysConfig.sId + "';", sel);
		Cursor cursor = db.rawQuery("select * from user where sid = '" 
				+ sysConfig.sId +"'", null);
		
		if (cursor.moveToFirst()) 
		{
			sysConfig.configServersVersion = cursor.getString(4);;
			sysConfig.configParametersVersion =cursor.getString(5);
			sysConfig.configHintsVersion = cursor.getString(6);
			sysConfig.sipcProxyIp = cursor.getString(7);
			sysConfig.sipcProxyPort = cursor.getInt(8);
			sysConfig.portraitServersName = cursor.getString(9);
			sysConfig.portraitServersPath = cursor.getString(10);
			sysConfig.personalVersion = cursor.getString(11);
			sysConfig.customConfigVersion = cursor.getString(12);
			sysConfig.contactVersion = cursor.getString(13);
		}
		else
		{
			sysConfig.configServersVersion = "";
			sysConfig.configParametersVersion = "";
			sysConfig.configHintsVersion = "";
			sysConfig.sipcProxyIp = "";
			sysConfig.sipcProxyPort = -1;
			sysConfig.portraitServersName = "";
			sysConfig.portraitServersPath = "";
			sysConfig.personalVersion = "";
			sysConfig.customConfigVersion = "";
			sysConfig.contactVersion = "";
		}
		
	}
	
	public void setUserInfo(SystemConfig sysConfig)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("update user set config_servers_version = '" + sysConfig.configServersVersion + "' "
				+ ", config_parameters_version = '" + sysConfig.configParametersVersion + "' "
				+ ", config_hints_version = '" + sysConfig.configHintsVersion + "' "
				+ ", sipc_proxy_ip = '" + sysConfig.sipcProxyIp + "' "
				+ ", sipc_proxy_port = " + sysConfig.sipcProxyPort + " "
				+ ", portrait_servers_name = '" + sysConfig.portraitServersName + "' "
				+ ", portrait_servers_path  = '" + sysConfig.portraitServersPath + "' "
				+ ", personal_version = '" + sysConfig.personalVersion + "' "
				+ ", custom_config_version = '" + sysConfig.customConfigVersion + "' "
				+ ", contact_version = '" + sysConfig.contactVersion + "' "
				+ " where sid = '" + sysConfig.sId + "'");
	}
}
