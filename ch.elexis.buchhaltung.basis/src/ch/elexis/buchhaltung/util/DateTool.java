package ch.elexis.buchhaltung.util;

import java.util.Calendar;

import ch.rgw.tools.TimeTool;

public class DateTool extends TimeTool {
	
	public DateTool(){
		super();
	}
	
	public DateTool(TimeTool other){
		super(other);
	}
	
	public DateTool(String other){
		super(other);
	}
	
	@Override
	public String toString(){
		return toString(TimeTool.DATE_SIMPLE);
	}
	
	@Override
	public int compareTo(Calendar c){
		long diff = (getTimeInMillis() - c.getTimeInMillis()) / 86400000L; // consider only
																			// day-differences
		return (int) diff;
	}
	
}
