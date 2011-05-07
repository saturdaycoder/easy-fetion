package com.saturdaycoder.easyfetion;
import android.app.*;
import android.util.Log;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.content.*;
import android.os.*;
import android.view.*;
public class AccountSettingDialog extends Activity
{
	private EditText editMobileno;
	private EditText editPasswd;
	private Button btnOk;
	private Button btnCancel;
	private static final String TAG="EasyFetion";
	private Intent intent;
	private Bundle bundle;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accountsettingdialog);
		editMobileno = (EditText)findViewById(R.id.editAcc);
		editPasswd = (EditText)findViewById(R.id.editAccPasswd);
		btnOk = (Button)findViewById(R.id.btnConfirmAccountSetting);
		btnCancel = (Button)findViewById(R.id.btnCancelAccountSetting);
		intent = this.getIntent();
		bundle = intent.getExtras();
		
		btnOk.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Intent i = new Intent();
				Bundle b = new Bundle();
				b.putString("mobileno", editMobileno.getText().toString());
				b.putString("passwd", editPasswd.getText().toString());
				intent.putExtras(b);
				AccountSettingDialog.this.setResult(RESULT_OK, intent);
				AccountSettingDialog.this.finish();
			}
		});
		btnCancel.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle b = new Bundle();

				intent.putExtras(b);
				AccountSettingDialog.this.setResult(RESULT_CANCELED, intent);
				AccountSettingDialog.this.finish();
			}
		});
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		//Log.v(TAG, "ACCSET onDestroy");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Bundle b = new Bundle();
	
			intent.putExtras(b);
			AccountSettingDialog.this.setResult(RESULT_CANCELED, intent);
			AccountSettingDialog.this.finish();
			//super.onDestroy();
			return true;
		}
		else {
			return false;
		}
	}
}
