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
	private String mobileno;
	private String passwd;
	
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
		String lastLogin = bundle.getString("lastlogin");
		editMobileno.setText(lastLogin);
		
		btnOk.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (editMobileno.getText().toString().equals("")
						|| editPasswd.getText().toString().equals("")) {
					popNotify("请输入正确的用户名和密码。");
					return;
				}
					
				mobileno = editMobileno.getText().toString();
				passwd = editPasswd.getText().toString();
				
				String localMobileno = "";
				try {
					localMobileno = Network.getPhoneNumber();
				} catch (Exception e) {
					Debugger.e( "error getting local phone number: " + e.getMessage());
				}
				
				if (!localMobileno.equals(mobileno)) {
					Intent intent = new Intent();
					intent.setClass(AccountSettingDialog.this, UserManualDialog.class);
					Bundle bundle = new Bundle();
					
					intent.putExtras(bundle);
					
					startActivityForResult(intent, 0);
					
				}
				
				else {
					Bundle b = new Bundle();
					b.putString("mobileno", mobileno);
					b.putString("passwd", passwd);
					intent.putExtras(b);
					AccountSettingDialog.this.setResult(RESULT_OK, intent);
					AccountSettingDialog.this.finish();
				}
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	Debugger.d( "onActivityResult: " + requestCode + ", " + resultCode );
    	switch (requestCode) {
    	case 0: {
    		Bundle b = new Bundle();
			b.putString("mobileno", mobileno);
			b.putString("passwd", passwd);
			intent.putExtras(b);
			AccountSettingDialog.this.setResult(RESULT_OK, intent);
			AccountSettingDialog.this.finish();
    	}
    	default:
    		break;
    	}
    }
	
    private void popNotify(String msg)
    {
        Toast.makeText(AccountSettingDialog.this, msg,
                Toast.LENGTH_LONG).show();
    }
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		//Debugger.v( "ACCSET onDestroy");
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
