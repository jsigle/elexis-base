package com.hilotec.elexis.messwerte.data;

public class Panel {
	private String type;
	private Panel[] panels;
	private String[] fields;
	private String[] attributes;
	
	public Panel[] getPanels(){
		return panels;
	}
	
	public void setPanels(Panel[] panels){
		this.panels = panels;
	}
	
	public String[] getFields(){
		return fields;
	}
	
	public void setFields(String[] fields){
		this.fields = fields;
	}
	
	public String[] getAttributes(){
		return attributes;
	}
	
	public void setAttributes(String[] attributes){
		this.attributes = attributes;
	}
	
	public Panel(String type){
		this.type = type;
	}
	
	public String getAttribute(String name){
		for (String a : attributes) {
			if (a.startsWith(name + "=")) {
				return a.substring(name.length() + 1);
			}
		}
		return null;
	}
	
	public String getType(){
		return type;
	}
	
}
