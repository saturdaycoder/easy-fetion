package com.saturdaycoder.easyfetion;
import android.app.*;
import android.widget.TextView;
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
	
	private Intent intent;
	private Bundle bundle;
	private String mobileno;
	private String passwd;
	
	private int[] reasons = new int[] {
			R.string.reason_first_login_instruction,
			R.string.reason_reset_account,
			R.string.reason_login_passwd_error,
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accountsettingdialog);
		editMobileno = (EditText)findViewById(R.id.editAcc);
		editPasswd = (EditText)findViewById(R.id.editAccPasswd);
		btnOk = (Button)findViewById(R.id.btnConfirmAccountSetting);
		intent = this.getIntent();
		bundle = intent.getExtras();
		String lastLogin = bundle.getString("lastlogin");
		int reason = bundle.getInt("reason");
		TextView tv = (TextView)findViewById(R.id.textAccSetText1);
		tv.setText(reasons[reason]);
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
					Debugger.error( "error getting local phone number: " + e.getMessage());
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

	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	Debugger.debug( "onActivityResult: " + requestCode + ", " + resultCode );
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

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Bundle b = new Bundle();
	
			intent.putExtras(b);
			AccountSettingDialog.this.setResult(RESULT_CANCELED, intent);
			AccountSettingDialog.this.finish();

			return true;
		}
		else {
			return super.onKeyDown(keyCode, msg);
		}
	}
}
