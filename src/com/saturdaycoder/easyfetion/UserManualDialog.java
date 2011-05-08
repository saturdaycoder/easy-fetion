package com.saturdaycoder.easyfetion;
import android.content.*;
import android.app.*;
import android.os.*;
import android.view.View;
import android.widget.Button;
public class UserManualDialog extends Activity {
	Button btnOk;
	Bundle bundle;
	Intent intent;
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.usermanualdialog);
		
		btnOk = (Button)findViewById(R.id.button1);
		
		intent = this.getIntent();
		bundle = intent.getExtras();
		
		btnOk.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle b = new Bundle();

				intent.putExtras(b);
				UserManualDialog.this.setResult(RESULT_OK, intent);
				UserManualDialog.this.finish();
			}
		});
	}
}
