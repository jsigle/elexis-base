package ch.elexis.db;

import java.util.Date;

public class Patient {
	public final static int MALE = 1;
	public final static int FEMALE = 2;
	
	private String id = null;
	private String patId = null;
	private Long lastUpdate = null;
	private Integer persFunktion = null;
	private String nachname = null;
	private String vorname = null;
	private String strasse = null;
	private String other = null;
	private String plz = null;
	private String ort = null;
	private String telefon1 = null;
	private String telefon2 = null;
	private String natelnr = null;
	private String fax = null;
	private String email = null;
	private Integer sex = null;
	private Date gebDatum = null;
	private String ahv = null;
	private String titel = null;
	private String land = null;
	
	public String getId(){
		return id;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public String getPatId(){
		return patId;
	}
	
	public void setPatId(String patId){
		this.patId = patId;
	}
	
	public Long getLastUpdate(){
		return lastUpdate;
	}
	
	public void setLastUpdate(Long lastUpdate){
		this.lastUpdate = lastUpdate;
	}
	
	public Integer getPersFunktion(){
		return persFunktion;
	}
	
	public void setPersFunktion(Integer persFunktion){
		this.persFunktion = persFunktion;
	}
	
	public String getNachname(){
		return nachname;
	}
	
	public void setNachname(String nachname){
		this.nachname = nachname;
	}
	
	public String getVorname(){
		return vorname;
	}
	
	public void setVorname(String vorname){
		this.vorname = vorname;
	}
	
	public String getStrasse(){
		return strasse;
	}
	
	public void setStrasse(String strasse){
		this.strasse = strasse;
	}
	
	public String getOther(){
		return other;
	}
	
	public void setOther(String other){
		this.other = other;
	}
	
	public String getPlz(){
		return plz;
	}
	
	public void setPlz(String plz){
		this.plz = plz;
	}
	
	public String getOrt(){
		return ort;
	}
	
	public void setOrt(String ort){
		this.ort = ort;
	}
	
	public String getTelefon1(){
		return telefon1;
	}
	
	public void setTelefon1(String telefon1){
		this.telefon1 = telefon1;
	}
	
	public String getTelefon2(){
		return telefon2;
	}
	
	public void setTelefon2(String telefon2){
		this.telefon2 = telefon2;
	}
	
	public String getNatelnr(){
		return natelnr;
	}
	
	public void setNatelnr(String natelnr){
		this.natelnr = natelnr;
	}
	
	public String getFax(){
		return fax;
	}
	
	public void setFax(String fax){
		this.fax = fax;
	}
	
	public String getEmail(){
		return email;
	}
	
	public void setEmail(String email){
		this.email = email;
	}
	
	public Integer getSex(){
		return sex;
	}
	
	public void setSex(Integer sex){
		this.sex = sex;
	}
	
	public Date getGebDatum(){
		return gebDatum;
	}
	
	public void setGebDatum(Date gebDatum){
		this.gebDatum = gebDatum;
	}
	
	public String getAhv(){
		return ahv;
	}
	
	public void setAhv(String ahv){
		this.ahv = ahv;
	}
	
	public String getTitel(){
		return titel;
	}
	
	public void setTitel(String titel){
		this.titel = titel;
	}
	
	public String getLand(){
		return land;
	}
	
	public void setLand(String land){
		this.land = land;
	}
	
	public String toString(){
		return this.id + ": " + this.nachname + " " + this.vorname; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
