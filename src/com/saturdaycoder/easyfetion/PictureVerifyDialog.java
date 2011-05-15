package com.saturdaycoder.easyfetion;
//import com.saturdaycoder.easyfetion.EasyFetionThread.State;

import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.TextView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
public class PictureVerifyDialog extends Activity
{
	
	Button btnAuth;
	EditText editCode;
	ImageView imageCode;
	
	TextView textVerifyText;
	TextView textVerifyTips;
	
	
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
        textVerifyText = (TextView)findViewById(R.id.textVerifyText);
        textVerifyTips = (TextView)findViewById(R.id.textVerifyTips);
         
        byte encPicData[] = bundle.getByteArray("picture");
    	byte decPicData[] = Crypto.base64Decode(encPicData);
    	
    	String tt = bundle.getString("text");
    	String tp = bundle.getString("tips");
    	
    	if (tt != null)
    		textVerifyText.setText(tt);
    	else
    		textVerifyText.setText("");
    	
    	if (tp != null)
    		textVerifyTips.setText(tp);
    	else
    		textVerifyTips.setText("");
        
    	//decode pic
    	if (decPicData != null) {
    		Debugger.d( "start decoding pic");
    		
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
        		Intent i = new Intent();
				Bundle b = new Bundle();
				b.putString("code", editCode.getText().toString());
				
				i.putExtras(b);
				PictureVerifyDialog.this.setResult(RESULT_OK, i);
				PictureVerifyDialog.this.finish();
        	}
        });
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
        	/*Toast.makeText(PictureVerifyDialog.this, "请输入正确的验证码",
                    Toast.LENGTH_SHORT).show();
        	return true;*/
    		Intent i = new Intent();
			Bundle b = new Bundle();
			//b.putString("code", editCode.getText().toString());
			
			i.putExtras(b);
			PictureVerifyDialog.this.setResult(RESULT_CANCELED, i);
			PictureVerifyDialog.this.finish();
			return true;
        }
        else return super.onKeyDown(keyCode, event);
           
    } 
}
