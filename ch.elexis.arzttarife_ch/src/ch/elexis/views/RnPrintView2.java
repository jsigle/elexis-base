/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis; Portions (c) 2013 Joerg M. Sigle www.jsigle.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	  J. Sigle   - added some closeTextContainer() openTextContainer() functionality
 * 			       to get this usable at least with the current Elexis-20131029js/NOAText_jsl 1.4.17 version,
 * 				   so far that printouts are produced and at least the first document assembly can be watched
 *                 in a window. The remainders are currently generated and printed, but can't be watched.
 *                 
 *                 TODO: TOREVIEW: TOIMPROVE:
 *                 
 *                 In contrast to other TextContainer clients, RechnungsDrucker.java/RnPrintView2.java
 *                 has the peculiarity that one call from RechnungsDrucker.java to RnPrintView2.doPrint()
 *                 produces not only ONE editable or printable document, but 1..n. Namely, the cover page
 *                 of the invoice/reminder with ESR slip: based upon templates Tarmedrechnung_EZ, Tarmedrechnung_M1 etc.
 *                 the first page of the Tarmed Rückforderungsbeleg: based upon Tarmedrechnung_S1
 *                 additional pages of the Tarmed Rückforderungsbeleg: based upon Tarmedrechnung_S2
 *                 
 *                 Each product can be optional, and all of them use the same data,
 *                 so at least *for now*, I would NOT want to split the production into doPrintEZMn(),doPrintS1(),doPrintS2()
 *                 which would be a cleaner solution, because then, RechnungsDrucker could use a call to
 *                 closePreExistingViewToEnsureOfficeCanHandleNewContentProperly() between the documents.
 *                 
 *                 So in between these documents, I need to do some careful partial closing of stuff,
 *                 so that the next document can be created using some of the loade plugin ressources
 *                 from the previous one. A side effect is, that the follow up documents of the same pass
 *                 cannot be watched during creation any more - this was different in Elexis-20130605js,
 *                 but the capability was lost when improving OO/NOA/createMe(),removeMe(),noas.isEmpty()
 *                 based usage tracking and connection handling.
 *                 
 *                 TODO: The best options would be:
 *                 
 *                 Either: To understand NOA/OO further, and maybe overcome the
 *                 limitations of OO re opening one doc while another is still open -> bridge lost; then
 *                 no more need to close stuff far down between documents.
 *                 
 *                 OR: To split multipart doc generation doPrint into three sections, maybe plus one
 *                 preceeding one generating the required data stuff in advance for all three,
 *                 if just-in-time-generation is not feasible.
 *                 
 *                 TODO: Printjob names are created using current patient because content creation
 *                 involves usage of temp files in OO/LO whose names are created based upon current
 *                 patient when Einstellungen so set (originally a NOAText_jsl feature to simplify
 *                 file attachments to e-mail etc.).
 *                 
 *                 TODO: Even Rechnungsliste etc. get such temp filenames..
 *                 
 *                  
 *    G. Weirich - initial implementation
 * 
 *******************************************************************************/
package ch.elexis.views;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import ch.elexis.Hub;
import ch.elexis.StringConstants;
import ch.elexis.TarmedRechnung.TarmedACL;
import ch.elexis.TarmedRechnung.XMLExporter;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.arzttarife_schweiz.Messages;
import ch.elexis.banking.ESR;
import ch.elexis.data.Brief;
import ch.elexis.data.Fall;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Mandant;
import ch.elexis.data.Patient;
import ch.elexis.data.Rechnung;
import ch.elexis.data.Rechnungssteller;
import ch.elexis.data.RnStatus;
import ch.elexis.data.Zahlung;
import ch.elexis.tarmedprefs.TarmedRequirements;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.ReplaceCallback;
import ch.elexis.text.TextContainer;
import ch.elexis.util.IRnOutputter;
import ch.elexis.util.Log;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.SortedList;
import ch.rgw.tools.Money;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.XMLTool;

/**
 * This is a pop-in replacement for RnPrintView. To avoid several problems around OpenOffice based
 * bills we keep things easier here. Thus this approach does not optimize printer access but rather
 * waits for each page to be printed before starting the next.
 * 
 * We also corrected several problems around the TrustCenter-system. Tokens are printed only on TG
 * bills and only if the mandator has a TC contract. Tokens are computed correctly now with the TC
 * number as identifier in TG bills and left as ESR in TP bills.
 * 
 * @author Gerry
 * 
 */
public class RnPrintView2 extends ViewPart {
	public static final String ID = "ch.elexis.arzttarife_ch.printview2";
	
	private double cmAvail = 21.4; // Verfügbare Druckhöhe in cm
	private static double cmPerLine = 0.67; // Höhe pro Zeile (0.65 plus
	// Toleranz)
	private static double cmFirstPage = 13.0; // Platz auf der ersten Seite
	private static double cmMiddlePage = 21.0; // Platz auf Folgeseiten
	private static double cmFooter = 4.5; // Platz für Endabrechnung
	private final Log log = Log.get("RnPrint");
	private String paymentMode;
	private Brief actBrief;
	TextContainer text;
	private Composite textContainerParent=null;
	private Composite textContainerComposite=null;
	TarmedACL ta = TarmedACL.getInstance();
	
	public RnPrintView2(){	
	}
	
	//20131028js: Das hier ausgelagert aus createPartControl, damit ich es von mehreren Stellen aufrufen kann:
	private void openTextContainer() {
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java: openTextContainer(): begin");
		
		if (text==null) text =  new TextContainer (getViewSite());
		
		textContainerComposite = text.getPlugin().createContainer(textContainerParent, new ITextPlugin.ICallback() {		
			public void save(){
				// TODO Auto-generated method stub		
			}
			
			public boolean saveAs(){
				// TODO Auto-generated method stub
				return false;
			}
		});		
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java: openTextContainer(): end");
	}
	
	//20131028js: Und hier nun das Gegenstück zum kontrollierten SCHLIESSEN des TextContainer-Teils mit allem Zubehör:
	private void closeTextContainer() {
	  System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java: closeTextContainer(): begin");
	  
	  if (text != null && text.getPlugin() != null) {
		  if (!textContainerComposite.isDisposed()) {  textContainerComposite.dispose(); }
		  textContainerComposite=null;
		  //text.dispose();
		  //text.getPlugin().clear();
		  
	  }
	  System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java: dispose(): end");
	}
	
	@Override
	public void createPartControl(final Composite parent){
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java createPartControl(): begin");
		//openTextContainer(parent);
		//We don't do it here any more, but rather in createBrief() below, if possible - because we need that twice,
		//and *nothing* in between, so creation of both produced output Briefe will start from a plain environment.
		textContainerParent=parent;		//20131028js: Save this info; we will need it in openTextContainer twice...
		//20131026js: Open the TextContainer for the document created from the first template...
		openTextContainer();		

		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java createPartControl(): end");
	}
	
	private void createBrief(final String template, final Kontakt adressat){
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java createBrief(): begin");
		
		//if (text == null) openTextContainer();
		if (textContainerComposite==null) openTextContainer();
		
		actBrief =
			text.createFromTemplateName(null, template, Brief.RECHNUNG, adressat,
				Messages.RnPrintView_tarmedBill);
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java createBrief(): end");
	}
	
	private boolean deleteBrief(){
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java deleteBrief(): begin");
		if (actBrief != null) {
			System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java deleteBrief(): about to return actBrief.delete()");
			return actBrief.delete();
		}
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java deleteBrief(): about to return true");
		return true;
	}
	
	@Override
	public void setFocus(){
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Druckt die Rechnung auf eine Vorlage, deren Ränder alle auf 0.5cm eingestellt sein müssen,
	 * und die unterhalb von 170 mm leer ist. (Papier mit EZ-Schein wird erwartet) Zweite und
	 * Folgeseiten müssen gem Tarmedrechnung formatiert sein.
	 * 
	 * Darin gibt's eine Menge möglichkeiten für early return false (Fehler)
	 * oder early return true (Rückforderungsbeleg soll nicht gedruckt werden).
	 * Also bitte innendrin NICHTS öffnen, was am Ende auch wieder geschlossen werden muss -
	 * oder entsprechend aufmerksam die Schliessungen an diversen Stellen vorsehen.
	 * 
	 * @param rn
	 *            die Rechnung
	 * @param saveFile
	 *            Filename für eine XML-Kopie der Rechnung oder null: Keine Kopie
	 * @param withForms
	 * @param monitor
	 * 			  A progress monitor provided by the caller; or null if none has been provided - comment and null option added 20131030js
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean doPrint(final Rechnung rn, final IRnOutputter.TYPE rnType,
		final String saveFile, final boolean withESR, final boolean withForms,
		final boolean doVerify, final IProgressMonitor monitor){

		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): begin");

		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): ********************************************************************************************************************");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: If one or two printouts don't appear as expected,");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: OO/LO might have loaded another default printer with one or two document templates.");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: Even if you DON'T see a created/processed document in the window, it may still be produced and printed.");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: To fix this problem: ");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: Open the template in OO Writer while options;load/save;'load default printer from document = off'");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: Re-Save the template again into the Elexis Systemvorlagen collection.");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: BE SURE YOU HAVE A BACKUP, the current work in progress version as of Elexis-20131029js can easily");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: lose templates while storing them into Elexis; without producing any error message on the way - TODO/js");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): ********************************************************************************************************************");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: TODO: TO REVIEW: 20131029js: TODO: TO REVIEW: 20131029js: TODO: TO REVIEW: 20131029js:  TODO: TO REVIEW");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: Underlying is the problem with Elexis above 20130627js (or so) that NOA may load a document into");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: a Composite, Panel, Whatever that may NOT become visible in front of the containing frame.");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: but still see no error and also be able to work with it. That's why I (mostly for editing-viewParts,");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: rather not for print-only-dialogs) try to quite completely close/dispose of the container,");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: to trigger a completely new buildup of the NOA constructs, to ensure that following documents");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: are processed correctly, and can be seen/edited by the user. This is NOT possible in RnPrintView2,");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: because we create/control/populate/manipulate ViewParts and from the same module where we use it,");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: and still all this in a ViewPart, so there's no proper separation like RezepteView/RezepteBlatt");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: or xyz/GeneralPrintoutView which made the provision of text.getPlugin().dispose OUTSIDE the code");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: also interrupted as a consequence of that disposal possible.");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: In RnPrintView2, I need to use a less intensive approach, that will ensure that the following portions");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: can actually be processed (via text.dispose; text.getPlugin().dispose; text=... (reOpen) in between.");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: OTHERWISE, you would get intense 'find-and-replace: missing doc msgs ");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): ********************************************************************************************************************");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: It's not so much a problem in RnPrintView2, as users don't intend to check or edit the generated doc.");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: Main drawback is that you can't visually see your invoice pages beyond the first one being assembled.");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): ********************************************************************************************************************");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: To ensure the first assembled doc is visible at least, ensure that openTextContainer() is called from");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: createPartControl() already. If I don't err, later - especially anywhere in doPrint() - is TOO LATE.");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: TODO: REVIEW: WHY? (Printing will still work, but visibility not even for the first one.)");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: TODO: REVIEW: WHY? If we'd get this fixed, we might also know why almost-complete-close[x]equivalents");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): PLEASE NOTE: TODO: REVIEW: WHY? in between documents are needed for other clients of TextContainer. 20131029js");
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): ********************************************************************************************************************");
				
		Mandant mSave = Hub.actMandant;
		if (monitor!=null) monitor.subTask(rn.getLabel());			//20131030js: explicit null support added
		Fall fall = rn.getFall();
		Mandant mnd = rn.getMandant();
		if (fall == null || mnd == null) {
			System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): fall == null || mnd == null -> early return false");
			closeTextContainer();	//20131029js
			return false;
		}
		Patient pat = fall.getPatient();
		Hub.setMandant(mnd);
		Rechnungssteller rs = mnd.getRechnungssteller();
		if (pat == null || rs == null) {
			System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): pat == null || rs == null -> early return false");
			closeTextContainer();	//20131029js
			return false;
		}
		
		String printer = null;
		
		XMLExporter xmlex = new XMLExporter();
		DecimalFormat df = new DecimalFormat(StringConstants.DOUBLE_ZERO);
		Document xmlRn = xmlex.doExport(rn, saveFile, rnType, doVerify);
		if (rn.getStatus() == RnStatus.FEHLERHAFT) {
			System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): rn.getStatus() == RnStatus.FEHLERHAFT -> early return false");
			closeTextContainer();	//20131029js
			return false;
		}
		
		Element invoice =
			xmlRn.getRootElement().getChild(XMLExporter.ELEMENT_INVOICE, XMLExporter.ns);
		Element balance = invoice.getChild(XMLExporter.ELEMENT_BALANCE, XMLExporter.ns);
		paymentMode = XMLExporter.TIERS_GARANT; // fall.getPaymentMode();
		Element eTiers = invoice.getChild(XMLExporter.ELEMENT_TIERS_GARANT, XMLExporter.ns);
		if (eTiers == null) {
			eTiers = invoice.getChild(XMLExporter.ELEMENT_TIERS_PAYANT, XMLExporter.ns);
			paymentMode = XMLExporter.TIERS_PAYANT;
		}
		
		String tcCode = null;
		if (TarmedRequirements.hasTCContract(rs) && paymentMode.equals(XMLExporter.TIERS_GARANT)) {
			tcCode = TarmedRequirements.getTCCode(rs);
		} else if (paymentMode.equals(XMLExporter.TIERS_PAYANT)) {
			tcCode = "01";
		}
		
	
		ElexisEventDispatcher.fireSelectionEvents(rn, fall, pat, rs);
		
		// make sure the Textplugin can replace all fields
		fall.setInfoString("payment", paymentMode);
		fall.setInfoString("Gesetz", TarmedRequirements.getGesetz(fall));
		mnd.setInfoElement("EAN", TarmedRequirements.getEAN(mnd));
		rs.setInfoElement("EAN", TarmedRequirements.getEAN(rs));
		mnd.setInfoElement("KSK", TarmedRequirements.getKSK(mnd));
		mnd.setInfoElement("NIF", TarmedRequirements.getNIF(mnd));
		if (!mnd.equals(rs)) {
			rs.setInfoElement("EAN", TarmedRequirements.getEAN(rs));
			rs.setInfoElement("KSK", TarmedRequirements.getKSK(rs));
			rs.setInfoElement("NIF", TarmedRequirements.getNIF(rs));
		}
		
		Kontakt adressat;
		
		if (paymentMode.equals(XMLExporter.TIERS_PAYANT)) {
			adressat = fall.getRequiredContact(TarmedRequirements.INSURANCE);
		} else {
			adressat = fall.getGarant();
		}
		if ((adressat == null) || (!adressat.exists())) {
			adressat = pat;
		}
		adressat.getPostAnschrift(true); // damit sicher eine existiert
		String userdata = rn.getRnId();
		ESR esr =
			new ESR(rs.getInfoString(ta.ESRNUMBER), rs.getInfoString(ta.ESRSUB), userdata,
				ESR.ESR27);
		Money mDue =
			XMLTool.xmlDoubleToMoney(balance.getAttributeValue(XMLExporter.ATTR_AMOUNT_DUE));
		Money mPaid =
			XMLTool.xmlDoubleToMoney(balance.getAttributeValue(XMLExporter.ATTR_AMOUNT_PREPAID));
		String offenRp = mDue.getCentsAsString();
		// Money mEZDue=new Money(xmlex.mTotal);
		Money mEZDue = new Money(mDue); // XMLTool.xmlDoubleToMoney(balance.getAttributeValue("amount_obligations"));
		Money mEZBrutto = new Money(mDue);
		mEZDue.addMoney(mPaid);
		
		//----Teil 1: Tarmedrechnung_EZ / Tarmedrechnung_M1..M3---------------------------------------
		
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): withESR == "+withESR);
		if (withESR == true) {
			
			String tmpl = "Tarmedrechnung_EZ"; //$NON-NLS-1$
			if ((rn.getStatus() == RnStatus.MAHNUNG_1)
				|| (rn.getStatus() == RnStatus.MAHNUNG_1_GEDRUCKT)) {
				tmpl = "Tarmedrechnung_M1"; //$NON-NLS-1$
			} else if ((rn.getStatus() == RnStatus.MAHNUNG_2)
				|| (rn.getStatus() == RnStatus.MAHNUNG_2_GEDRUCKT)) {
				tmpl = "Tarmedrechnung_M2"; //$NON-NLS-1$
			} else if ((rn.getStatus() == RnStatus.MAHNUNG_3)
				|| (rn.getStatus() == RnStatus.MAHNUNG_3_GEDRUCKT)) {
				tmpl = "Tarmedrechnung_M3"; //$NON-NLS-1$
			}

			System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): Erzeuge Brief aus Vorlage "+tmpl);

			createBrief(tmpl, adressat);
			
			List<Zahlung> extra = rn.getZahlungen();
			Kontakt bank = Kontakt.load(rs.getInfoString(ta.RNBANK));
			final StringBuilder sb = new StringBuilder();
			String sTarmed = balance.getAttributeValue(XMLExporter.ATTR_AMOUNT_TARMED);
			String sMedikament = balance.getAttributeValue(XMLExporter.ATTR_AMOUNT_DRUG);
			String sAnalysen = balance.getAttributeValue(XMLExporter.ATTR_AMOUNT_LAB);
			String sMigel = balance.getAttributeValue(XMLExporter.ATTR_AMOUNT_MIGEL);
			String sPhysio = balance.getAttributeValue(XMLExporter.ATTR_AMOUNT_PHYSIO);
			String sOther = balance.getAttributeValue(XMLExporter.ATTR_AMOUNT_UNCLASSIFIED);
			sb.append(Messages.RnPrintView_tarmedPoints).append(sTarmed).append(StringConstants.LF);
			sb.append(Messages.RnPrintView_medicaments).append(sMedikament)
				.append(StringConstants.LF);
			sb.append(Messages.RnPrintView_labpoints).append(sAnalysen).append(StringConstants.LF);
			sb.append(Messages.RnPrintView_migelpoints).append(sMigel).append(StringConstants.LF);
			sb.append(Messages.RnPrintView_physiopoints).append(sPhysio).append(StringConstants.LF);
			sb.append(Messages.RnPrintView_otherpoints).append(sOther).append(StringConstants.LF);
			
			for (Zahlung z : extra) {
				Money betrag = new Money(z.getBetrag()).multiply(-1.0);
				if (!betrag.isNegative()) {
					sb.append(z.getBemerkung())
						.append(":\t").append(betrag.getAmountAsString()).append(StringConstants.LF); //$NON-NLS-1$ 
					mEZDue.addMoney(betrag);
				}
			}
			sb.append("--------------------------------------").append(StringConstants.LF); //$NON-NLS-1$ 
			
			sb.append(Messages.RnPrintView_sum).append(mEZDue);
			
			if (!mPaid.isZero()) {
				sb.append(Messages.RnPrintView_prepaid).append(mPaid.getAmountAsString())
					.append(StringConstants.LF);
				// sb.append("Noch zu zahlen:\t").append(xmlex.mDue.getAmountAsString()).append("\n");
				sb.append(Messages.RnPrintView_topay)
					.append(mEZDue.subtractMoney(mPaid).roundTo5().getAmountAsString())
					.append(StringConstants.LF);
			}
			
			text.getPlugin().setFont("Serif", SWT.NORMAL, 9); //$NON-NLS-1$
			text.replace("\\[Leistungen\\]", sb.toString());
			
			if (esr.printBESR(bank, adressat, rs, mEZDue.roundTo5().getCentsAsString(), text) == false) {
				// avoid dead letters
				deleteBrief();
				;
				Hub.setMandant(mSave);
				System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): esr.printBESR(...) == false -> early return false");
				closeTextContainer();	//20131029js
				return false;
			}
			
			printer = Hub.localCfg.get("Drucker/A4ESR/Name", null); //$NON-NLS-1$
			String esrTray = Hub.localCfg.get("Drucker/A4ESR/Schacht", null); //$NON-NLS-1$
			if (StringTool.isNothing(esrTray)) {
				esrTray = null;
			}
			// Das mit der Tray- Einstellung funktioniert sowieso nicht richtig.
			// OOo nimmt den Tray aus der Druckformatvorlage. Besser wir setzen
			// ihn hier auf
			// null vorläufig.
			// Alternative: Wir verwenden ihn, falls er eingestellt ist, sonst
			// nicht.
			// Dies scheint je nach Druckertreiber unterschiedlich zu
			// funktionieren.
			if (text.getPlugin().print(printer, esrTray, false) == false) {
				SWTHelper.showError("Fehler beim Drucken", "Konnte den Drucker nicht starten");
				rn.addTrace(Rechnung.REJECTED, "Druckerfehler");
				// avoid dead letters
				deleteBrief();
				;
				Hub.setMandant(mSave);
				System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): text.getPlugin().print(printer,esrTray,false) == false -> Rechnung.REJECTED: Druckerfehler-> early return false");
				closeTextContainer();	//20131029js
				return false;
			}
			
			if (monitor!=null) monitor.worked(2);

			//20131026js: Close the TextContainer for the document created from the first template... 
			closeTextContainer();	//20131029js
		}
		
		if (withForms == false) {
			// avoid dead letters
			deleteBrief();
			;
			Hub.setMandant(mSave);
			System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): withForms == false -> early return true");
		
			//20131029js: NO closeTextContainer here, because: (see below)
			//EITHER we have just finished and printed the first document, so we would need closeTextContainer(), but that has been included in if withESR...
			//OR we have NOT generated any document so far, so the open textContainer can very well be used by the following Tarmedrechnung,
			//   which will only appear in an actually watchable window, if we don't use closeTextContainer();openTextcontainer() after
			//   the initial openTextContainer() which appeared as part of createPart... - Why, I don't really know yet, but I observed it that way. js
			//TODO: TO REVIEW: research for the reasons why, maybe make it better. Especially, in Elexis-20130605js, any doc would still appear,
			//TODO: TO REVIEW: but we added some code to rather ensure that OO/LO connections were disconnected, oo/lo servers unloaded,
			//TODO: TO REVIEW: where appropriate, by correct usage of NOAText: createMe() removeMe() noas; noas.isEmpty etc. 20131029js
			return true;
		}

		//20131029js: NO closeTextContainer here either - similar reason as above.

		//----Teil 2: Tarmedrechnung_S1---------------------------------------------------------------

		printer = Hub.localCfg.get("Drucker/A4/Name", null); //$NON-NLS-1$
		String tarmedTray = Hub.localCfg.get("Drucker/A4/Schacht", null); //$NON-NLS-1$
		if (StringTool.isNothing(tarmedTray)) {
			tarmedTray = null;
		}
		
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): Erzeuge Brief aus Vorlage Tarmedrechnung_S1");

		//20131029js: NO OpenTextContainer() here. Reasons see above, PLUS createBrief() contains an if text == null -> openTextContainer or similar.
		//So we can put closeContainer() wherever we think it's appropriate, just not where it should be avoided for good reason
		//(i.e. AFTER createPart and BEFORE first document is assembled, whichever that will be), and still be sure we have one when it's needed. 
		createBrief("Tarmedrechnung_S1", adressat);
		
		StringBuilder sb = new StringBuilder();
		Element root = xmlRn.getRootElement();
		Namespace ns = root.getNamespace();
		//Element invoice=root.getChild("invoice",ns); //$NON-NLS-1$
		if (invoice.getAttributeValue("resend").equalsIgnoreCase("true")) { //$NON-NLS-1$ //$NON-NLS-2$
			text.replace("\\[F5\\]", Messages.RnPrintView_yes); //$NON-NLS-1$
		} else {
			text.replace("\\[F5\\]", Messages.RnPrintView_no); //$NON-NLS-1$
		}
		
		// Vergütungsart F17
		// replaced with Fall.payment
		
		if (fall.getAbrechnungsSystem().equals("UVG")) { //$NON-NLS-1$
			text.replace("\\[F58\\]", fall.getBeginnDatum()); //$NON-NLS-1$
		} else {
			text.replace("\\[F58\\]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		Element detail = invoice.getChild("detail", ns); //$NON-NLS-1$
		Element diagnosis = detail.getChild("diagnosis", ns); //$NON-NLS-1$
		String type = diagnosis.getAttributeValue(Messages.RnPrintView_62);
		
		// TODO Cheap workaround, fix
		if (type.equals("by_contract")) { //$NON-NLS-1$
			type = "TI-Code"; //$NON-NLS-1$
		}
		text.replace("\\[F51\\]", type); //$NON-NLS-1$
		if (type.equals("freetext")) { //$NON-NLS-1$
			text.replace("\\[F52\\]", ""); //$NON-NLS-1$ //$NON-NLS-2$
			text.replace("\\[F53\\]", diagnosis.getText()); //$NON-NLS-1$
		} else {
			text.replace("\\[F52\\]", diagnosis.getAttributeValue("code")); //$NON-NLS-1$ //$NON-NLS-2$
			text.replace("\\[F53\\]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		// lookup EAN numbers in services and set field 98
		HashSet<String> eanUniqueSet = new HashSet<String>();
		List allServices = detail.getChild(XMLExporter.ELEMENT_SERVICES, ns).getChildren(); //$NON-NLS-1$
		for (Object object : allServices) {
			if (object instanceof Element) {
				Element service = (Element) object;
				String tariftype = service.getAttributeValue("tariff_type");
				// look into all tarmed 001 and physio 311 services
				if (tariftype != null && (tariftype.equals("001") || tariftype.equals("311"))) {
					String ean_responsible = service.getAttributeValue("ean_responsible");
					if (ean_responsible != null && !ean_responsible.isEmpty()) {
						eanUniqueSet.add(ean_responsible);
					}
					String ean_provider = service.getAttributeValue("ean_provider");
					if (ean_provider != null && !ean_provider.isEmpty()) {
						eanUniqueSet.add(ean_provider);
					}
				}
			}
		}
		
		String[] eanArray = getEANArray(eanUniqueSet);
		HashMap<String, String> eanMap = getEANHashMap(eanArray);
		text.replace("\\[F98\\]", getEANList(eanArray));
		
		Kontakt zuweiser = fall.getRequiredContact("Zuweiser");
		if (zuweiser != null) {
			String ean = TarmedRequirements.getEAN(zuweiser);
			if (!ean.equals(TarmedRequirements.EAN_PSEUDO)) {
				text.replace("\\[F23\\]", ean);
			}
		}
		
		Element services = detail.getChild(XMLExporter.ELEMENT_SERVICES, ns); //$NON-NLS-1$
		SortedList<Element> ls = new SortedList(services.getChildren(), new RnComparator());
		
		Element remark = invoice.getChild(XMLExporter.ELEMENT_REMARK); //$NON-NLS-1$
		if (remark != null) {
			final String rem = remark.getText();
			text.getPlugin().findOrReplace(Messages.RnPrintView_remark, new ReplaceCallback() {
				public String replace(final String in){
					return Messages.RnPrintView_remarksp + rem;
				}
			});
		}
		
		replaceHeaderFields(text, rn);
		text.replace("\\[F.+\\]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		Object cursor = text.getPlugin().insertText("[Rechnungszeilen]", "\n", SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		TimeTool r = new TimeTool();
		int page = 1;
		double seitentotal = 0.0;
		double sumPfl = 0.0;
		double sumNpfl = 0.0;
		double sumTotal = 0.0;
		ITextPlugin tp = text.getPlugin();
		cmAvail = cmFirstPage;
		
		if (monitor!=null) monitor.worked(2);
		
		for (Element s : ls) {
			tp.setFont("Helvetica", SWT.BOLD, 7); //$NON-NLS-1$
			cursor = tp.insertText(cursor, "\t" + s.getText() + "\n", SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
			tp.setFont("Helvetica", SWT.NORMAL, 8); //$NON-NLS-1$
			sb.setLength(0);
			if (r.set(s.getAttributeValue("date_begin")) == false) { //$NON-NLS-1$
				continue;
			}
			sb.append("■ "); //$NON-NLS-1$
			sb.append(r.toString(TimeTool.DATE_GER)).append("\t"); //$NON-NLS-1$
			sb.append(getValue(s, "tariff_type")).append("\t"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(getValue(s, "code")).append("\t"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(getValue(s, "ref_code")).append("\t"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(getValue(s, "number")).append("\t"); //$NON-NLS-1$ //$NON-NLS-2$
			if (getValue(s, "body_location").startsWith("l")) //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("L\t");
			else if (getValue(s, "body_location").startsWith("r")) //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("R\t");
			else
				sb.append(" \t");
			sb.append(getValue(s, "quantity")).append("\t"); //$NON-NLS-1$ //$NON-NLS-2$
			String val = s.getAttributeValue("unit.mt"); //$NON-NLS-1$
			if (StringTool.isNothing(val)) {
				val = s.getAttributeValue("unit"); //$NON-NLS-1$
				if (StringTool.isNothing(val)) {
					val = "\t"; //$NON-NLS-1$
				}
			}
			sb.append(val).append("\t"); //$NON-NLS-1$
			sb.append(getValue(s, "scale_factor.mt")).append("\t"); //$NON-NLS-1$ //$NON-NLS-2$
			val = s.getAttributeValue("unit_factor.mt"); //$NON-NLS-1$
			if (StringTool.isNothing(val)) {
				val = s.getAttributeValue("unit_factor"); //$NON-NLS-1$
				if (StringTool.isNothing(val)) {
					val = "\t"; //$NON-NLS-1$
				}
			}
			sb.append(val).append("\t"); //$NON-NLS-1$
			sb.append(getValue(s, "unit.tt")).append("\t\t"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(getValue(s, "unit_factor.tt")).append("\t"); //$NON-NLS-1$ //$NON-NLS-2$
			
			// set responsible (field 77) and provider (field 78)
			String tariftype = s.getAttributeValue("tariff_type");
			// look into all tarmed 001 and physio 311 services
			if (tariftype != null && (tariftype.equals("001") || tariftype.equals("311"))) {
				String ean_responsible = s.getAttributeValue("ean_responsible"); //$NON-NLS-1$
				if (ean_responsible != null && !ean_responsible.isEmpty()) {
					sb.append(eanMap.get(ean_responsible) + "\t"); //$NON-NLS-1$
				}
				String ean_provider = s.getAttributeValue("ean_provider"); //$NON-NLS-1$
				if (ean_provider != null && !ean_provider.isEmpty()) {
					sb.append(eanMap.get(ean_provider) + "\t"); //$NON-NLS-1$
				}
			} else {
				sb.append("\t\t");
			}
			
			String pfl = s.getAttributeValue("obligation"); //$NON-NLS-1$
			String am = s.getAttributeValue(XMLExporter.ATTR_AMOUNT); //$NON-NLS-1$
			// double dLine=Double.parseDouble(am);
			double dLine;
			try {
				dLine = XMLTool.xmlDoubleToMoney(am).getAmount();
			} catch (NumberFormatException ex) {
				// avoid dead letters
				deleteBrief();
				;
				log.log("Fehlerhaftes Format für amount bei " + sb.toString(), Log.ERRORS);
				Hub.setMandant(mSave);
				System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): CAUGHT NumberFormatException -> early return false");
				closeTextContainer();	//20131029js
				return false;
			}
			sumTotal += dLine;
			if (pfl.equalsIgnoreCase("true")) { //$NON-NLS-1$
				sb.append("0\t"); //$NON-NLS-1$
				sumPfl += dLine;
			} else {
				sb.append("1\t"); //$NON-NLS-1$
				sumNpfl += dLine;
			}
			sb.append(Integer.toString(guessVatCode(getValue(s, "vat_rate")))).append("\t"); //$NON-NLS-1$
			
			sb.append(am);
			seitentotal += dLine;
			sb.append("\n"); //$NON-NLS-1$
			cursor = tp.insertText(cursor, sb.toString(), SWT.LEFT);
			cmAvail -= cmPerLine;
			if (cmAvail <= 0) {
				StringBuilder footer = new StringBuilder();
				cursor = tp.insertText(cursor, "\n\n", SWT.LEFT); //$NON-NLS-1$
				footer
					.append("■ Zwischentotal\t\tCHF\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t").append(df.format(seitentotal)); //$NON-NLS-1$
				tp.setFont("Helvetica", SWT.BOLD, 7); //$NON-NLS-1$
				cursor = tp.insertText(cursor, footer.toString(), SWT.LEFT);
				seitentotal = 0.0;
				if (tcCode != null) {
					esr.printESRCodeLine(text.getPlugin(), offenRp, tcCode);
				}
				
				if (text.getPlugin().print(printer, tarmedTray, false) == false) {
					// avoid dead letters
					deleteBrief();
					;
					Hub.setMandant(mSave);
					System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): text.getPlugin().print(printer,tarmedTray,false) 1 == false - Druckerfehler -> early return false");
					closeTextContainer();	//20131029js
					return false;
				}
				
				insertPage(++page, adressat, rn);
				cursor = text.getPlugin().insertText("[Rechnungszeilen]", "\n", SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
				cmAvail = cmMiddlePage;
				
				if (monitor!=null) monitor.worked(2);
			}
			
		}
		
		cursor = tp.insertText(cursor, "\n", SWT.LEFT); //$NON-NLS-1$
		if (cmAvail < cmFooter) {
			if (tcCode != null) {
				esr.printESRCodeLine(text.getPlugin(), offenRp, tcCode);
			}
			if (text.getPlugin().print(printer, tarmedTray, false) == false) {
				// avoid dead letters
				deleteBrief();
				;
				Hub.setMandant(mSave);
				System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): text.getPlugin().print(printer,tarmedTray,false) 2 == false - Druckerfehler -> early return false");
				closeTextContainer();	//20131029js
				return false;
			}
			insertPage(++page, adressat, rn);
			cursor = text.getPlugin().insertText("[Rechnungszeilen]", "\n", SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (monitor!=null) monitor.worked(2);
		}
		
		StringBuilder footer = new StringBuilder(100);
		//Element balance=invoice.getChild("balance",ns); //$NON-NLS-1$
		
		cursor = text.getPlugin().insertTextAt(0, 220, 190, 45, " ", SWT.LEFT); //$NON-NLS-1$
		cursor = print(cursor, tp, true, "\tTARMED AL \t"); //$NON-NLS-1$
		footer.append(balance.getAttributeValue("amount_tarmed.mt")) //$NON-NLS-1$
			.append("  (").append(balance.getAttributeValue("unit_tarmed.mt")).append(")\t"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		cursor = print(cursor, tp, false, footer.toString());
		cursor = print(cursor, tp, true, "Physio \t"); //$NON-NLS-1$
		cursor = print(cursor, tp, false, getValue(balance, "amount_physio")); //$NON-NLS-1$
		cursor = print(cursor, tp, true, "\tMiGeL \t"); //$NON-NLS-1$
		cursor = print(cursor, tp, false, getValue(balance, "amount_migel")); //$NON-NLS-1$
		cursor = print(cursor, tp, true, "\tÜbrige \t"); //$NON-NLS-1$
		cursor = print(cursor, tp, false, getValue(balance, "amount_unclassified")); //$NON-NLS-1$
		cursor = print(cursor, tp, true, "\n\tTARMED TL \t"); //$NON-NLS-1$
		footer.setLength(0);
		footer.append(balance.getAttributeValue("amount_tarmed.tt")) //$NON-NLS-1$
			.append("  (").append(balance.getAttributeValue("unit_tarmed.tt")).append(")\t"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		cursor = print(cursor, tp, false, footer.toString());
		cursor = print(cursor, tp, true, "Labor \t"); //$NON-NLS-1$
		cursor = print(cursor, tp, false, getValue(balance, "amount_lab")); //$NON-NLS-1$
		cursor = print(cursor, tp, true, "\tMedi \t"); //$NON-NLS-1$
		cursor = print(cursor, tp, false, getValue(balance, "amount_drug")); //$NON-NLS-1$
		cursor = print(cursor, tp, true, "\tKantonal \t"); //$NON-NLS-1$
		cursor = print(cursor, tp, false, getValue(balance, "amount_cantonal")); //$NON-NLS-1$
		
		footer.setLength(0);
		footer.append("\n\n").append("■ Gesamtbetrag\t\tCHF\t\t").append(df.format(sumTotal)) //$NON-NLS-1$ //$NON-NLS-2$
			.append("\tdavon PFL \t").append(df.format(sumPfl)).append("\tAnzahlung \t") //$NON-NLS-1$ //$NON-NLS-2$
			.append(mPaid.getAmountAsString())
			.append("\tFälliger Betrag \t").append(mDue.getAmountAsString()); //$NON-NLS-1$
		
		Element vat = balance.getChild("vat", ns);
		String vatNumber = getValue(vat, "vat_number");
		if (vatNumber.equals(" "))
			vatNumber = "keine";
		
		footer.append("\n\n■ MwSt.Nr. \t\t"); //$NON-NLS-1$
		cursor = print(cursor, tp, true, footer.toString());
		cursor = print(cursor, tp, false, vatNumber + "\n\n"); //$NON-NLS-1$
		
		Boolean isVat =
			(Boolean) mnd.getRechnungssteller().getInfoElement(XMLExporter.VAT_ISMANDANTVAT);
		if (isVat != null && isVat) {
			cursor = print(cursor, tp, true, "  Code\tSatz\t\tBetrag\t\tMwSt\n"); //$NON-NLS-1$
			tp.setFont("Helvetica", SWT.NORMAL, 9); //$NON-NLS-1$
			footer.setLength(0);
			
			List<Element> rates = vat.getChildren();
			
			// get vat lines ordered by code
			List<String> vatLines = new ArrayList<String>();
			for (Element rate : rates) {
				StringBuilder vatBuilder = new StringBuilder();
				int code = guessVatCode(getValue(rate, "vat_rate"));
				// set amount of tabs needed according, use 7
				String amount = getValue(rate, "amount");
				String tabs = "\t\t";
				if (amount.length() > 7)
					tabs = "\t";
				
				vatBuilder.append("■ ").append(Integer.toString(code)).append("\t")
					.append(getValue(rate, "vat_rate")).append("\t\t").append(amount).append(tabs)
					.append(getValue(rate, "vat")).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// insert according to code
				if (code == 0) {
					vatLines.add(0, vatBuilder.toString());
				} else if (code == 1) {
					if (vatLines.size() == 2)
						vatLines.add(1, vatBuilder.toString());
					else
						vatLines.add(vatBuilder.toString());
				} else if (code == 2) {
					vatLines.add(vatBuilder.toString());
				}
			}
			for (String string : vatLines) {
				footer.append(string);
			}
			
			cursor = print(cursor, tp, false, footer.toString());
			cursor = print(cursor, tp, true, "\n Total\t\t\t"); //$NON-NLS-1$
			// set amount of tabs needed according to amount, use 8 as font is bold
			String amount = mDue.getAmountAsString();
			String tabs = "\t\t";
			if (amount.length() > 8)
				tabs = "\t";
			
			footer.setLength(0);
			footer.append(amount).append(tabs).append(getValue(vat, "vat")); //$NON-NLS-1$
		} else {
			cursor = print(cursor, tp, true, "\n Total\t\t"); //$NON-NLS-1$
			footer.setLength(0);
			footer.append(mDue.getAmountAsString()); //$NON-NLS-1$
		}
		
		tp.setFont("Helvetica", SWT.BOLD, 9); //$NON-NLS-1$
		tp.insertText(cursor, footer.toString(), SWT.LEFT);
		if (tcCode != null) {
			esr.printESRCodeLine(text.getPlugin(), offenRp, tcCode);
		}
		
		if (text.getPlugin().print(printer, tarmedTray, false) == false) {
			// avoid dead letters
			deleteBrief();
			;
			Hub.setMandant(mSave);
			System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): text.getPlugin().print(printer,tarmedTray,false) 3 == false - Druckerfehler -> early return false");
			closeTextContainer();	//20131029js
			return false;
		}
		
		//20131026js: Close the TextContainer for the document created from the second template... 
		//closeTextContainer();
		
		if (monitor!=null) monitor.worked(2);

		// avoid dead letters
		deleteBrief();
		;
		Hub.setMandant(mSave);
		try {
			Thread.sleep(5);
			//Thread.sleep(1);	//js tried this... may be too small to achieve anything.
		} catch (InterruptedException e) {
			// never mind
		}

		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java doPrint(): about to return true");
		closeTextContainer();	//20131029js
		return true;
	}
	
	private String getEANList(String[] eans){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < eans.length; i++) {
			if (i > 0)
				sb.append("  ");
			sb.append(Integer.toString(i + 1) + "/" + eans[i]);
		}
		return sb.toString();
	}
	
	private String[] getEANArray(HashSet<String> responsibleEANs){
		String[] eans = responsibleEANs.toArray(new String[responsibleEANs.size()]);
		return eans;
	}
	
	private HashMap<String, String> getEANHashMap(String[] eans){
		HashMap<String, String> ret = new HashMap<String, String>();
		for (int i = 0; i < eans.length; i++) {
			ret.put(eans[i], Integer.toString(i + 1));
		}
		return ret;
	}
	
	private void insertPage(final int page, final Kontakt adressat, final Rechnung rn){
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java insertPage(): begin");

		//20131026js: Close the TextContainer for the previous document... 
		closeTextContainer();
		//The new one will be opened automatically  in createBrief()

		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java insertPage(): about to createBrief(\"Tarmedrechnung_S2\", adressat");

		createBrief("Tarmedrechnung_S2", adressat);
		replaceHeaderFields(text, rn);
		text.replace("\\[Seite\\]", StringTool.pad(StringTool.LEFT, '0', Integer.toString(page), 2)); //$NON-NLS-1$
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.views/RnPrintView2.java insertPage(): end");
	}
	
	/*
	 * private TextContainer insertPage(final int page, final Kontakt adressat, TextContainer text,
	 * final Rechnung rn){
	 * 
	 * if(--existing<0){ ctF=addItem("Tarmedrechnung_S2",Messages.RnPrintView_page+page,adressat);
	 * //$NON-NLS-1$ }else{ ctF=ctab.getItem(page); useItem(page,"Tarmedrechnung_S2", adressat);
	 * //$NON-NLS-1$ } text=(TextContainer) ctF.getData("text"); //$NON-NLS-1$
	 * replaceHeaderFields(text, rn); text.replace("\\[Seite\\]",StringTool.pad(SWT
	 * .LEFT,'0',Integer.toString(page),2)); //$NON-NLS-1$ return text;
	 * 
	 * }
	 */
	private Object print(final Object cur, final ITextPlugin p, final boolean small,
		final String text){
		if (small) {
			p.setFont("Helvetica", SWT.BOLD, 7); //$NON-NLS-1$
		} else {
			p.setFont("Helvetica", SWT.NORMAL, 9); //$NON-NLS-1$
		}
		return p.insertText(cur, text, SWT.LEFT);
	}
	
	private String getValue(final Element s, final String field){
		String ret = s.getAttributeValue(field);
		if (StringTool.isNothing(ret)) {
			return " "; //$NON-NLS-1$
		}
		return ret;
	}
	
	private void replaceHeaderFields(final TextContainer text, final Rechnung rn){
		Fall fall = rn.getFall();
		Mandant m = rn.getMandant();
		text.replace("\\[F1\\]", rn.getRnId()); //$NON-NLS-1$
		
		String titel;
		String titelMahnung;
		
		if (paymentMode.equals(XMLExporter.TIERS_PAYANT)) { //$NON-NLS-1$
			titel = Messages.RnPrintView_tbBill;
			
			switch (rn.getStatus()) {
			case RnStatus.MAHNUNG_1_GEDRUCKT:
			case RnStatus.MAHNUNG_1:
				titelMahnung = Messages.RnPrintView_firstM;
				break;
			case RnStatus.MAHNUNG_2:
			case RnStatus.MAHNUNG_2_GEDRUCKT:
				titelMahnung = Messages.RnPrintView_secondM;
				break;
			case RnStatus.IN_BETREIBUNG:
			case RnStatus.TEILVERLUST:
			case RnStatus.TOTALVERLUST:
			case RnStatus.MAHNUNG_3:
			case RnStatus.MAHNUNG_3_GEDRUCKT:
				titelMahnung = Messages.RnPrintView_thirdM;
				break;
			default:
				titelMahnung = ""; //$NON-NLS-1$
			}
			;
		} else {
			titel = Messages.RnPrintView_getback;
			titelMahnung = ""; //$NON-NLS-1$
		}
		
		text.replace("\\[Titel\\]", titel); //$NON-NLS-1$
		text.replace("\\[TitelMahnung\\]", titelMahnung); //$NON-NLS-1$
		
		if (fall.getAbrechnungsSystem().equals("IV")) { //$NON-NLS-1$
			text.replace("\\[NIF\\]", TarmedRequirements.getNIF(m)); //$NON-NLS-1$
			String ahv = TarmedRequirements.getAHV(fall.getPatient());
			if (StringTool.isNothing(ahv)) {
				ahv = fall.getRequiredString("AHV-Nummer");
			}
			text.replace("\\[F60\\]", ahv); //$NON-NLS-1$
		} else {
			text.replace("\\[NIF\\]", TarmedRequirements.getKSK(m)); //$NON-NLS-1$
			text.replace("\\[F60\\]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		text.replace("\\?\\?\\??[a-zA-Z0-9 \\.]+\\?\\?\\??", "");
		
	}
	
	private class RnComparator implements Comparator<Element> {
		TimeTool tt0 = new TimeTool();
		TimeTool tt1 = new TimeTool();
		
		public int compare(Element e0, Element e1){
			if (!tt0.set(e0.getAttributeValue("date_begin"))) {
				return 1;
			}
			if (!tt1.set(e1.getAttributeValue("date_begin"))) {
				return -1;
			}
			int dat = tt0.compareTo(tt1);
			if (dat != 0) {
				return dat;
			}
			String t0 = e0.getAttributeValue(XMLExporter.ATTR_TARIFF_TYPE);
			String t1 = e1.getAttributeValue(XMLExporter.ATTR_TARIFF_TYPE);
			if (t0.equals("001")) { // tarmed-tarmed: nach code sortieren
				if (t1.equals("001")) {
					String c0 = e0.getAttributeValue(XMLExporter.ATTR_CODE);
					String c1 = e1.getAttributeValue(XMLExporter.ATTR_CODE);
					return c0.compareTo(c1);
				} else {
					return -1; // tarmed immer oberhab nicht-tarmed
				}
			} else if (t1.equals("001")) {
				return 1; // nicht-tarmed immer unterhalb tarmed
			} else { // nicht-tarmed - nicht-tarmed: alphabetisch
				int diffc = t0.compareTo(t1);
				if (diffc == 0) {
					diffc = e0.getText().compareToIgnoreCase(e1.getText());
				}
				return diffc;
			}
		}
	}
	
	/**
	 * Make a guess for the correct code value for the provided vat rate. Guessing is necessary as
	 * the correct code is not part of the XML invoice.
	 * 
	 * @param vatRate
	 * @return
	 */
	private int guessVatCode(String vatRate){
		if (vatRate != null && !vatRate.isEmpty()) {
			double scale = Double.parseDouble(vatRate);
			// make a guess for the correct code
			if (scale == 0)
				return 0;
			else if (scale < 7)
				return 2;
			else
				return 1;
		}
		return 0;
	}
}
