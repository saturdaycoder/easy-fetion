package com.saturdaycoder.easyfetion;
import java.util.List;  
import java.util.Map;  
import android.content.Context;  
import android.view.View;  
import android.view.LayoutInflater;
import android.view.ViewGroup;  
import android.widget.SimpleAdapter;  
import android.widget.TextView;
public class MsgListAdapter extends SimpleAdapter{
    
	private List<Integer> backRes;
	private Context context;
    public MsgListAdapter(Context context, List<? extends Map<String, ?>> data, 
    		List<Integer> backRes,  
            int resource, String[] from, int[] to) {  
        super(context, data, resource, from, to);
        
        this.context = context;
        this.backRes = backRes;
          
    }  
    /* (non-Javadoc) 
     * @see android.widget.SimpleAdapter#getView(int, android.view.View, android.view.ViewGroup) 
     */  
    
    @Override  
    public View getView(int position, View convertView, ViewGroup parent) {  

        View view = super.getView(position, convertView, parent);  
       
        view.setBackgroundResource(this.backRes.get(position));
        
         return view;  
    }  
    
      
}
