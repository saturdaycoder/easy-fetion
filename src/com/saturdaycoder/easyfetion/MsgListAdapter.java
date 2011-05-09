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
    //private int[] colors = new int[]{0x00FFFFFF, 0x008080FF}; 
    //private int[] colorSel;
	private List<Integer> fontColor;
	private List<Integer> backColor;
	private Context context;
    public MsgListAdapter(Context context, List<? extends Map<String, ?>> data, 
    		List<Integer> fontColor, List<Integer> backColor,  
            int resource, String[] from, int[] to) {  
        super(context, data, resource, from, to);
        //this.colorSel = colorSel;
        this.context = context;
        this.fontColor = fontColor;
        this.backColor = backColor;
        // TODO Auto-generated constructor stub  
    }  
    /* (non-Javadoc) 
     * @see android.widget.SimpleAdapter#getView(int, android.view.View, android.view.ViewGroup) 
     */  
    @Override
    public Object getItem(int position) {
    	View v = (View)super.getItem(position);
    	v.setBackgroundColor(this.backColor.get(position));
    	return v;
    }
    @Override  
    public View getView(int position, View convertView, ViewGroup parent) {  
        // TODO Auto-generated method stub  
        View view = super.getView(position, convertView, parent);  
        //int colorPos = colorSel[position]%colors.length;  
        Debugger.d("row " + position + " color is " + this.backColor.get(position));
        
        LayoutInflater vi=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.msglistitem, null);
        
        TextView msgv = (TextView)v.findViewById(R.id.msgListItemBody);
        
        msgv.setBackgroundColor(this.backColor.get(position));
        msgv.setBackgroundResource(R.drawable.icon);
        //msgv.setText(this.backColor.get(position).toString());
        Debugger.d("row " + position + " text is " + msgv.getText().toString());
        //view.set
        return view;  
    }  
    
    @Override
    public void setViewText(TextView v, String text) {
    	String color = text.substring(0, 3);
    	if (color.equals("<0>")) {
    		Debugger.d("set back color to blue");
    		v.setBackgroundColor(0x00A0A0FF);
    	}
    	if (color.equals("<1>")) {
    		Debugger.d("set back color to white");
    		v.setBackgroundColor(0x00FFFFFF);
    	}
    	String t = text.substring(3);
    	super.setViewText(v,t);
    	if (color.equals("<0>")) {
    		Debugger.d("set back color to blue");
    		v.setBackgroundColor(0x00A0A0FF);
    	}
    	if (color.equals("<1>")) {
    		Debugger.d("set back color to white");
    		v.setBackgroundColor(0x00FFFFFF);
    	}
    }
  
}
