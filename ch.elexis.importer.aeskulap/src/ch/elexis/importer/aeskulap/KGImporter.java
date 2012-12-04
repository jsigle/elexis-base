package ch.elexis.importer.aeskulap;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Mandant;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.data.Rechnung;
import ch.elexis.data.Xid;
import ch.elexis.icpc.Encounter;
import ch.elexis.icpc.Episode;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * Importiere Aeskulap-KG aus XML
 * 
 * @author gerry
 * 
 */
public class KGImporter {
	private static final String KONSID = AeskulapImporter.IMPORT_XID + "/KonsID";
	Mandant mandant;
	int counter = 0;
	
	KGImporter(File xml, IProgressMonitor moni) throws Exception{
		moni.subTask("Importiere KG");
		Xid.localRegisterXIDDomainIfNotExists(KONSID, "Alte KG-ID", Xid.ASSIGNMENT_LOCAL);
		String rnid = new Query<Rechnung>(Rechnung.class).findSingle("RnNummer", "=", "1");
		Rechnung rn = Rechnung.load(rnid);
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(xml);
		Element root = doc.getRootElement();
		List<Element> patienten = root.getChildren("Patient");
		int perPat = Math.round(AeskulapImporter.MONITOR_PERTASK / patienten.size());
		boolean bSkip = true;
		String last = Hub.localCfg.get("importaeskulap/lastimported", null);
		if (last == null) {
			bSkip = false;
		}
		
		for (Element ePat : patienten) {
			
			String id = ePat.getAttributeValue("PatID");
			if (bSkip && (!last.equals(id))) {
				moni.worked(perPat);
				if (moni.isCanceled()) {
					break;
				}
				continue;
			}
			bSkip = false;
			Patient pat = (Patient) Xid.findObject(AeskulapImporter.PATID, id);
			if (pat != null) {
				moni.subTask(pat.getLabel());
				Element eRisiken = ePat.getChild("Risiken");
				List<Element> lKons = ePat.getChildren("Konsultation");
				if (lKons != null) {
					for (Element eKons : lKons) {
						String konsid = eKons.getAttributeValue("KonsID");
						if (Xid.findObject(KONSID, konsid) != null) {
							continue;
						}
						mandant = AeskulapImporter.getMandant(eKons.getAttributeValue("ArztID"));
						if (mandant != null) {
							Desk.syncExec(new Runnable() {
								public void run(){
									Hub.setMandant(mandant);
								}
								
							});
							
							Element eSubj = eKons.getChild("Subjektiv");
							Element eProc = eKons.getChild("Procedere");
							Element eTher = eKons.getChild("Therapie");
							List<Element> lDiag = eKons.getChildren("Diagnosen");
							List<Element> lProb = eKons.getChildren("Probleme");
							List<Element> lMedi = eKons.getChildren("Medikation");
							TimeTool ttDate = new TimeTool(eKons.getAttributeValue("Datum"));
							Fall[] faelle = pat.getFaelle();
							Fall actFall = null;
							if (faelle.length == 0) {
								actFall =
									pat.neuerFall(Fall.getDefaultCaseLabel(),
										Fall.getDefaultCaseReason(), Fall.getDefaultCaseLaw());
							} else {
								actFall = faelle[0];
							}
							Konsultation k = actFall.neueKonsultation();
							k.setMandant(mandant);
							k.setDatum(ttDate.toString(TimeTool.DATE_COMPACT), true);
							StringBuilder sb = new StringBuilder();
							if (eSubj != null) {
								sb.append("S: ").append(eSubj.getText()).append("\n");
							}
							if (eProc != null) {
								sb.append("P: ").append(eProc.getText()).append("\n");
							}
							if (eTher != null) {
								sb.append("Therapie: ").append(eTher.getText()).append("\n");
							}
							if (lMedi != null) {
								sb.append("Medikation:\n");
								for (Element eMedi : lMedi) {
									String mText = eMedi.getAttributeValue("Text");
									if (!StringTool.isNothing(mText)) {
										sb.append(mText).append("\n");
									}
								}
							}
							k.updateEintrag(sb.toString(), true);
							for (Element eDiag : lDiag) {
								String wichtig = eDiag.getAttributeValue("Wichtige_Diagnose");
								if ("Ja".equals(wichtig)) {
									String date = eDiag.getAttributeValue("Gueltig_von");
									if (StringTool.isNothing(date)) {
										date = ttDate.toString(TimeTool.DATE_GER);
									} else {
										date = new TimeTool(date).toString(TimeTool.DATE_GER);
									}
									Element eText = eDiag.getChild("Diagnosetext");
									if (eText != null) {
										String text = date + ": " + eText.getText();
										String oldDiag = pat.get("Diagnosen");
										if (!StringTool.isNothing(oldDiag)) {
											oldDiag += "\n" + text;
										} else {
											oldDiag = text;
										}
										pat.set("Diagnosen", oldDiag);
									}
								}
							}
							for (Element eProb : lProb) {
								String title = eProb.getAttributeValue("Problem");
								Episode episode = null;
								Query<Episode> qbe = new Query<Episode>(Episode.class);
								qbe.add("Title", "=", title);
								qbe.add("PatientID", "=", pat.getId());
								List<Episode> lEpi = qbe.execute();
								if (lEpi.size() > 0) {
									episode = lEpi.get(0);
								} else {
									episode = new Episode(pat, title);
									episode.setStartDate(ttDate.toString(TimeTool.DATE_COMPACT));
								}
								new Encounter(k, episode);
							}
							k.setRechnung(rn);
							k.addXid(KONSID, konsid, true);
						}
					}
				}
				Element eAnamnese = ePat.getChild("Anamnese");
				Element eImpfungen = ePat.getChild("Impfungen");
				
				if (eAnamnese != null) {
					String anamnese = eAnamnese.getText();
					pat.set("PersAnamnese", anamnese);
				}
				if (eRisiken != null) {
					StringBuilder sb = new StringBuilder();
					List<Element> lRisks = eRisiken.getChildren("Risiko");
					for (Element eRisk : lRisks) {
						Element eBem = eRisk.getChild("Bemerkung");
						if (eBem != null) {
							sb.append(eBem.getText()).append("\n");
						}
					}
					if (sb.length() > 0) {
						pat.set("Risiken", sb.toString());
					}
				}
				if (eImpfungen != null) {
					String eI = eImpfungen.getText();
					if (!StringTool.isNothing(eI)) {
						StringBuilder sb = new StringBuilder(StringTool.unNull(pat.getBemerkung()));
						if (sb.length() > 0) {
							sb.append("\n");
						}
						sb.append("Impfungen:\n");
						sb.append(eI);
						pat.setBemerkung(sb.toString());
					}
					
				}
				
			}
			Hub.localCfg.set("importaeskulap/lastimported", id);
			moni.worked(perPat);
			if (++counter > 100) {
				System.gc();
				Thread.sleep(100);
				counter = 0;
			}
			if (moni.isCanceled()) {
				break;
			}
		}
	}
}
