package ch.elexis.db;

import java.util.Date;

public class Laborwert {
	private String id = null;
	private Long lastUpdate = null;
	private Patient patient = null;
	private Laboritem labItem = null;
	private Date datum = null;
	private String resultat = null;
	private String origin = null;
	private int flags;
	private String kommentar = null;
	
	public String getId(){
		return id;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public Long getLastUpdate(){
		return lastUpdate;
	}
	
	public void setLastUpdate(Long lastUpdate){
		this.lastUpdate = lastUpdate;
	}
	
	public Patient getPatient(){
		return patient;
	}
	
	public void setLaborItem(Laboritem labItem){
		this.labItem = labItem;
	}
	
	public Laboritem getLaborItem(){
		return labItem;
	}
	
	public void setPatient(Patient patient){
		this.patient = patient;
	}
	
	public Date getDatum(){
		return this.datum;
	}
	
	public void setDatum(Date datum){
		this.datum = datum;
	}
	
	public String getResultat(){
		return resultat;
	}
	
	public void setResultat(String resultat){
		this.resultat = resultat;
	}
	
	public String getOrigin(){
		return origin;
	}
	
	public void setOrigin(String origin){
		this.origin = origin;
	}
	
	public void setFlags(int flags){
		this.flags = flags;
	}
	
	public int getFlags(){
		return this.flags;
	}
	
	public String getKommentar(){
		return kommentar;
	}
	
	public void setKommentar(String kommentar){
		this.kommentar = kommentar;
	}
	
	public String toString(){
		return this.id + ": " + this.kommentar + " " + this.resultat; //$NON-NLS-1$ //$NON-NLS-2$
	}
}