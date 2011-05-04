package com.saturdaycoder.easyfetion;
import com.saturdaycoder.easyfetion.EasyFetionThread.State;

import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
public class PictureVerifyDialog extends Activity
{
	private static final String TAG="EasyFetion";
	
	Button btnAuth;
	EditText editCode;
	ImageView imageCode;
	
	Intent intent;
	Bundle bundle;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pictureverifydialog);
        
		intent = this.getIntent();
		bundle = intent.getExtras();
		
		btnAuth = (Button)findViewById(R.id.buttonAuth);
        editCode = (EditText)findViewById(R.id.editCode);
        imageCode = (ImageView)findViewById(R.id.imageCode);
        
        //editCode.setVisibility(EditText.INVISIBLE);
        //imageCode.setVisibility(ImageView.INVISIBLE);
        //btnAuth.setVisibility(Button.INVISIBLE);
        byte encPicData[] = bundle.getByteArray("picture");
		byte decPicData[] = Crypto.base64Decode(encPicData);
        
    	//decode pic
    	if (decPicData != null) {
    		Log.d(TAG, "start decoding pic");
    		
    		Bitmap bmp = BitmapFactory.decodeByteArray(decPicData, 0, decPicData.length);
    		/*if (bmp == null) {
    			showerr(TAG, "error decoding image");    			
    		}
    		else {*/
    		imageCode.setImageBitmap(bmp);
    		//imageCode.setVisibility(ImageView.VISIBLE);
    		//}
    	}
        //editCode.setVisibility(EditText.VISIBLE);
        //imageCode.setVisibility(ImageView.VISIBLE);
        //btnAuth.setVisibility(Button.VISIBLE);
        
        
        btnAuth.setOnClickListener(new Button.OnClickListener()
        {
        	//@Override
        	public void onClick(View v) {
				Bundle b = new Bundle();
				b.putString("code", editCode.getText().toString());
				
				intent.putExtras(b);
				PictureVerifyDialog.this.setResult(RESULT_OK, intent);
				PictureVerifyDialog.this.finish();
        	}
        });
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
        	/*Bundle b = new Bundle();
			intent.putExtras(b);
			AccountSettingDialog.this.setResult(RESULT_CANCELED, intent);
			AccountSettingDialog.this.finish();*/
        	Toast.makeText(PictureVerifyDialog.this, "请输入正确的验证码",
                    Toast.LENGTH_SHORT).show();
        	return true;
        }
        return super.onKeyDown(keyCode, event);
           
    } 
}
