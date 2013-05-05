package ch.elexis.db;

public class Laboritem {
	private String id = null;
	private Long lastUpdate = null;
	private String kuerzel = null;
	private String titel = null;
	private String refMann = null;
	private String refFrau = null;
	private String einheit = null;
	private String gruppe = null;
	private String prio = null;
	private Typ typ = null;
	
	public enum Typ {
		NUMERIC, TEXT, ABSOLUTE, FORMULA, DOCUMENT
	};
	
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
	
	public String getKuerzel(){
		return kuerzel;
	}
	
	public void setKuerzel(String kuerzel){
		this.kuerzel = kuerzel;
	}
	
	public String getTitel(){
		return titel;
	}
	
	public void setTitel(String titel){
		this.titel = titel;
	}
	
	public String getRefMann(){
		return refMann;
	}
	
	public void setRefMann(String refMann){
		this.refMann = refMann;
	}
	
	public String getRefFrau(){
		return refFrau;
	}
	
	public void setRefFrau(String refFrau){
		this.refFrau = refFrau;
	}
	
	public String getEinheit(){
		return einheit;
	}
	
	public void setEinheit(String einheit){
		this.einheit = einheit;
	}
	
	public String getGruppe(){
		return gruppe;
	}
	
	public void setGruppe(String gruppe){
		this.gruppe = gruppe;
	}
	
	public String getPrio(){
		return prio;
	}
	
	public void setPrio(String prio){
		this.prio = prio;
	}
	
	public Typ getTyp(){
		return typ;
	}
	
	public void setTyp(Typ typ){
		this.typ = typ;
	}
	
	public String toString(){
		return this.id + ": " + this.titel; //$NON-NLS-1$
	}
}