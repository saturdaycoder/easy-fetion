package com.saturdaycoder.easyfetion;
import java.util.Comparator;

public class FetionMsg implements Comparator<FetionMsg>{
	public FetionContact contact;
	public String msg;
	public long timestamp;
	public FetionMsg()
	{}
	//@Override
	public int compare(FetionMsg fm1, FetionMsg fm2){
		if (fm1.timestamp > fm2.timestamp)
			return 1;
		else if (fm1.timestamp < fm2.timestamp)
			return -1;
		else 
			return 0;
	}
}
