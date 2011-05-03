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
	EditText editMobileno;
	EditText editPasswd;
	Button btnOk;
	
	Intent intent;
	Bundle bundle;
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
		
		btnOk.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle b = new Bundle();
				b.putString("mobileno", editMobileno.getText().toString());
				b.putString("passwd", editPasswd.getText().toString());
				intent.putExtras(b);
				AccountSettingDialog.this.setResult(RESULT_OK, intent);
				AccountSettingDialog.this.finish();
			}
		});
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
        	/*Bundle b = new Bundle();
			intent.putExtras(b);
			AccountSettingDialog.this.setResult(RESULT_CANCELED, intent);
			AccountSettingDialog.this.finish();*/
        	Toast.makeText(AccountSettingDialog.this, "请输入用户名和密码。若要离开请按home按钮",
                    Toast.LENGTH_SHORT).show();
        	return true;
        }
        return super.onKeyDown(keyCode, event);
           
    } 
	
}
