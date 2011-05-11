package com.saturdaycoder.easyfetion;

import java.util.List;
import java.util.Map;
import android.widget.ImageView;
import android.graphics.*;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

public class ContactListAdapter extends SimpleAdapter {
	
    
	//private List<Integer> backRes;
	private Context context;
    public ContactListAdapter(Context context, List<? extends Map<String, ?>> data, 
    		//List<Integer> backRes,  
            int resource, String[] from, int[] to) {  
        super(context, data, resource, from, to);
        
        this.context = context;
        //this.backRes = backRes;
          
    }  
    /* (non-Javadoc) 
     * @see android.widget.SimpleAdapter#getView(int, android.view.View, android.view.ViewGroup) 
     */  
    
    @Override  
    public void setViewImage(ImageView v, String value) {
        Bitmap bitmap = BitmapFactory.decodeFile(value);
        ((ImageView) v).setImageBitmap(bitmap);
    }  
    

}
