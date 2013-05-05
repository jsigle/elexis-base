/*******************************************************************************
 * Copyright (c) 2007-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    D. Lutz    - Adapted from elexis-arzttarife-schweiz to elexis-privatrechnung2
 *    
 *******************************************************************************/

package ch.elexis.privatrechnung2.prefs;

import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.banking.ESR;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Mandant;
import ch.elexis.data.Messages;
import ch.elexis.data.Organisation;
import ch.elexis.data.Query;
import ch.elexis.dialogs.KontaktExtDialog;
import ch.elexis.dialogs.KontaktSelektor;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.StringTool;

public class Preferences extends PreferencePage implements IWorkbenchPreferencePage {
	
	// indices for cbESROCRFontWeight
	private static final int FONT_MIN_INDEX = 0;
	private static final int FONT_NORMAL_INDEX = 1;
	private static final int FONT_BOLD_INDEX = 2;
	
	Combo cbMands;
	HashMap<String, Mandant> hMandanten;
	Hyperlink hRechng, hTreat, hPost, hBank;
	IHyperlinkListener hDetailListener;
	FocusListener focusListener;
	Text tRechng, tTreat, tPost, tBank;
	Text tESRNormalFontName;
	Text tESRNormalFontSize;
	Text tESROCRFontName;
	Text tESROCRFontSize;
	Text tESRPrinterCorrectionX;
	Text tESRPrinterCorrectionY;
	Text tESRPrintCorrectionBaseX;
	Combo cbESROCRFontWeight;
	Button bPost;
	Button bBank;
	Mandant actMandant;
	Kontakt actBank;
	Button bUseTC;
	Combo cbTC;
	Button bUseEDA;
	Button bWithImage;
	
	private static final String PREFIX = "Privatrechnung2"; //$NON-NLS-1$
	
	public static final String RNBANK = PREFIX + "RnBank"; //$NON-NLS-1$
	public static final String ESR5OR9 = PREFIX + "ESR5OrEsr9"; //$NON-NLS-1$
	public static final String ESRPLUS = PREFIX + "ESRPlus"; //$NON-NLS-1$
	public static final String ESRNUMBER = PREFIX + "ESRParticipantNumber"; //$NON-NLS-1$
	public static final String ESRSUB = PREFIX + "ESRIdentity"; //$NON-NLS-1$
	public static final String BANK_NAME = PREFIX + "BankName"; //$NON-NLS-1$
	public static final String BANK_LOCATION = PREFIX + "BankLocation"; //$NON-NLS-1$
	
	private static final String[] ExtFlds = {
		ESR5OR9, ESRPLUS
	}; //$NON-NLS-1$ //$NON-NLS-2$
	
	public Preferences(){
		super("Privatrechnung2");
	}
	
	@Override
	public void dispose(){
		// nothing to do
	}
	
	@Override
	protected Control createContents(Composite parent){
		Color blau = Desk.getColor(Desk.COL_BLUE);
		hDetailListener = new DetailListener();
		focusListener = new TextListener();
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());
		hMandanten = new HashMap<String, Mandant>();
		cbMands = new Combo(ret, SWT.READ_ONLY);
		cbMands.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		cbMands.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				int i = cbMands.getSelectionIndex();
				if (i == -1) {
					return;
				}
				setMandant((Mandant) hMandanten.get(cbMands.getItem(i)));
				
			}
			
		});
		Query<Mandant> qbe = new Query<Mandant>(Mandant.class);
		List<Mandant> list = qbe.execute();
		for (Mandant m : list) {
			cbMands.add(m.getLabel());
			hMandanten.put(m.getLabel(), m);
		}
		Group adrs = new Group(ret, SWT.NONE);
		adrs.setLayout(new GridLayout(2, false));
		adrs.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		adrs.setText(Messages.getString("RechnungsPrefs.BillDetails")); //$NON-NLS-1$
		hTreat = new Hyperlink(adrs, SWT.NONE);
		hTreat.setText(Messages.getString("RechnungsPrefs.Treator")); //$NON-NLS-1$
		hTreat.setForeground(blau);
		hTreat.addHyperlinkListener(hDetailListener);
		tTreat = new Text(adrs, SWT.BORDER | SWT.READ_ONLY);
		tTreat.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// Finanzinstitut
		// TODO better layout
		
		GridData gd;
		
		Composite cFinanzinstitut = new Composite(adrs, SWT.NONE);
		cFinanzinstitut.setLayoutData(SWTHelper.getFillGridData(2, true, 1, false));
		cFinanzinstitut.setLayout(new GridLayout(2, false));
		
		Label lFinanzinstitut = new Label(cFinanzinstitut, SWT.NONE);
		lFinanzinstitut.setText(Messages.getString("RechnungsPrefs.Financeinst")); //$NON-NLS-1$
		lFinanzinstitut.setLayoutData(SWTHelper.getFillGridData(2, true, 1, false));
		
		bPost = new Button(cFinanzinstitut, SWT.RADIO);
		gd = SWTHelper.getFillGridData(1, false, 1, false);
		gd.verticalAlignment = SWT.TOP;
		bPost.setLayoutData(gd);
		bPost.setText(Messages.getString("RechnungsPrefs.post")); //$NON-NLS-1$
		bPost.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e){
				if (bPost.getSelection()) {
					// selection changed
					
					// TODO DEBUG
					System.err.println("DEBUG: P Post: " + bPost.getSelection());
					System.err.println("DEBUG: P Bank: " + bBank.getSelection());
					
					// check if Bank has been chosen
					if (actBank != null && actBank.isValid()) {
						// clear all bank data
						actBank = null;
						actMandant.setInfoElement(RNBANK, ""); //$NON-NLS-1$
						// clear data set with BankLister dialog
						actMandant.setInfoElement(ESRNUMBER, ""); //$NON-NLS-1$
						actMandant.setInfoElement(ESRSUB, ""); //$NON-NLS-1$
						actMandant.setInfoElement(BANK_NAME, ""); //$NON-NLS-1$ //$NON-NLS-2$
						actMandant.setInfoElement(BANK_LOCATION, ""); //$NON-NLS-1$ //$NON-NLS-2$
					}
					
					// check if Post account is already available
					if (StringTool.isNothing(actMandant.getInfoElement(ESRNUMBER))) {
						new PostDialog(getShell()).open();
					}
					
					// update widgets
					setMandant(actMandant);
				}
			}
		});
		
		Composite cPost = new Composite(cFinanzinstitut, SWT.NONE);
		gd = SWTHelper.getFillGridData(1, true, 1, false);
		gd.verticalAlignment = SWT.TOP;
		cPost.setLayoutData(gd);
		cPost.setLayout(new GridLayout(1, false));
		hPost = new Hyperlink(cPost, SWT.NONE);
		hPost.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		hPost.setText(Messages.getString("RechnungsPrefs.POAccount")); //$NON-NLS-1$
		hPost.setForeground(blau);
		hPost.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e){
				new PostDialog(getShell()).open();
				// update widgets
				setMandant(actMandant);
			}
			
		});
		tPost = new Text(cPost, SWT.BORDER | SWT.READ_ONLY);
		tPost.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		bBank = new Button(cFinanzinstitut, SWT.RADIO);
		gd = SWTHelper.getFillGridData(1, false, 1, false);
		gd.verticalAlignment = SWT.TOP;
		bBank.setLayoutData(gd);
		bBank.setText(Messages.getString("RechnungsPrefs.bank")); //$NON-NLS-1$
		bBank.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e){
				if (bBank.getSelection()) {
					// TODO DEBUG
					System.err.println("DEBUG: B Post: " + bPost.getSelection());
					System.err.println("DEBUG: B Bank: " + bBank.getSelection());
					
					if (actBank == null) {
						// invalidate available post data
						actMandant.setInfoElement(ESRNUMBER, ""); //$NON-NLS-1$
						new BankLister(getShell()).open();
					}
				}
			}
		});
		
		Composite cBank = new Composite(cFinanzinstitut, SWT.NONE);
		gd = SWTHelper.getFillGridData(1, true, 1, false);
		gd.verticalAlignment = SWT.TOP;
		cBank.setLayoutData(gd);
		cBank.setLayout(new GridLayout(1, false));
		hBank = new Hyperlink(cBank, SWT.NONE);
		hBank.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		hBank.setText(Messages.getString("RechnungsPrefs.bankconnection")); //$NON-NLS-1$
		hBank.setForeground(blau);
		hBank.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e){
				// KontaktSelektor dlg=new
				// KontaktSelektor(getShell(),Organisation.class,"Bankverbindung auswählen","Bitte
				// geben Sie an, auf welches Finanzinstitut\nIhre Einuahlungsscheine lauten
				// sollen");
				BankLister dlg = new BankLister(getShell());
				dlg.open();
			}
			
		});
		tBank = new Text(cBank, SWT.BORDER | SWT.READ_ONLY);
		tBank.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// OCR font
		
		Group fonts = new Group(ret, SWT.NONE);
		fonts.setText(Messages.getString("RechnungsPrefs.FontSlip")); //$NON-NLS-1$
		fonts.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		fonts.setLayout(new GridLayout(2, false));
		
		FontsTextListener fontsTextListener = new FontsTextListener();
		
		String warning = Messages.getString("RechnungsPrefs.FontWarning") //$NON-NLS-1$
			+ Messages.getString("RechnungsPrefs.FontWarning2") //$NON-NLS-1$
			+ Messages.getString("RechnungsPrefs.FontWarning3"); //$NON-NLS-1$
		Label lWarning = new Label(fonts, SWT.NONE);
		lWarning.setText(warning);
		lWarning.setLayoutData(SWTHelper.getFillGridData(2, true, 1, false));
		
		new Label(fonts, SWT.NONE).setText(Messages.getString("RechnungsPrefs.Font")); //$NON-NLS-1$
		tESRNormalFontName = new Text(fonts, SWT.BORDER | SWT.SINGLE);
		tESRNormalFontName.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tESRNormalFontName.addFocusListener(fontsTextListener);
		
		new Label(fonts, SWT.NONE).setText(Messages.getString("RechnungsPrefs.Size")); //$NON-NLS-1$
		tESRNormalFontSize = new Text(fonts, SWT.BORDER | SWT.SINGLE);
		tESRNormalFontSize.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tESRNormalFontSize.addFocusListener(fontsTextListener);
		
		new Label(fonts, SWT.NONE).setText(Messages.getString("RechnungsPrefs.fontCodingLine")); //$NON-NLS-1$
		tESROCRFontName = new Text(fonts, SWT.BORDER | SWT.SINGLE);
		tESROCRFontName.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tESROCRFontName.addFocusListener(fontsTextListener);
		
		new Label(fonts, SWT.NONE).setText(Messages.getString("RechnungsPrefs.SizeCondingLine")); //$NON-NLS-1$
		tESROCRFontSize = new Text(fonts, SWT.BORDER | SWT.SINGLE);
		tESROCRFontSize.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tESROCRFontSize.addFocusListener(fontsTextListener);
		
		new Label(fonts, SWT.NONE).setText(Messages.getString("RechnungsPrefs.Weight")); //$NON-NLS-1$
		cbESROCRFontWeight = new Combo(fonts, SWT.READ_ONLY);
		cbESROCRFontWeight.setLayoutData(SWTHelper.getFillGridData(1, false, 1, false));
		
		cbESROCRFontWeight.add(Messages.getString("RechnungsPrefs.light")); // FONT_MIN_INDEX
		// //$NON-NLS-1$
		cbESROCRFontWeight.add(Messages.getString("RechnungsPrefs.normal")); // FONT_NORMAL_INDEX
		// //$NON-NLS-1$
		cbESROCRFontWeight.add(Messages.getString("RechnungsPrefs.bold")); // FONT_BOLD_INDEX
		// //$NON-NLS-1$
		
		cbESROCRFontWeight.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				storeFontsTextValues();
			}
		});
		
		new Label(fonts, SWT.NONE).setText(Messages.getString("RechnungsPrefs.horzCorrCodingLine")); //$NON-NLS-1$
		tESRPrinterCorrectionX = new Text(fonts, SWT.BORDER | SWT.SINGLE);
		tESRPrinterCorrectionX.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tESRPrinterCorrectionX.addFocusListener(fontsTextListener);
		
		new Label(fonts, SWT.NONE).setText(Messages.getString("RechnungsPrefs.vertCorrCodingLine")); //$NON-NLS-1$
		tESRPrinterCorrectionY = new Text(fonts, SWT.BORDER | SWT.SINGLE);
		tESRPrinterCorrectionY.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tESRPrinterCorrectionY.addFocusListener(fontsTextListener);
		
		new Label(fonts, SWT.NONE).setText(Messages.getString("RechnungsPrefs.horrzBaseOffset"));
		tESRPrintCorrectionBaseX = new Text(fonts, SWT.BORDER | SWT.SINGLE);
		tESRPrintCorrectionBaseX.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tESRPrintCorrectionBaseX.addFocusListener(fontsTextListener);
		
		// set the initial font values
		setFontsTextValues();
		
		cbMands.select(0);
		setMandant((Mandant) hMandanten.get(cbMands.getItem(0)));
		return ret;
	}
	
	public void init(IWorkbench workbench){
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	class TextListener extends FocusAdapter {
		
		@Override
		public void focusLost(FocusEvent e){
			Text t = (Text) e.getSource();
			String fld = (String) t.getData();
			actMandant.setInfoElement(fld, t.getText());
		}
		
	}
	
	class DetailListener extends HyperlinkAdapter {
		
		@Override
		public void linkActivated(HyperlinkEvent e){
			if (actMandant == null) {
				return;
			}
			KontaktExtDialog dlg = new KontaktExtDialog(getShell(), actMandant, ExtFlds);
			dlg.create();
			dlg.setTitle(Messages.getString("RechnungsPrefs.MandatorDetails")); //$NON-NLS-1$
			
			dlg.open();
		}
		
	}
	
	class BankLister extends TitleAreaDialog {
		final String[] flds = {
			BANK_NAME, BANK_LOCATION, ESRNUMBER, ESRSUB
		}; //$NON-NLS-1$ //$NON-NLS-2$
		Label banklabel;
		KontaktExtDialog.ExtInfoTable exTable;
		
		BankLister(Shell shell){
			super(shell);
		}
		
		@Override
		protected Control createDialogArea(Composite parent){
			Composite ret = new Composite(parent, SWT.NONE);
			ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
			ret.setLayout(new GridLayout(2, false));
			Hyperlink hb =
				Desk.getToolkit().createHyperlink(ret,
					Messages.getString("RechnungsPrefs.FinanceInst"), SWT.NONE); //$NON-NLS-1$
			hb.addHyperlinkListener(new HyperlinkAdapter() {
				
				@Override
				public void linkActivated(HyperlinkEvent e){
					KontaktSelektor ksl =
						new KontaktSelektor(
							getShell(),
							Organisation.class,
							Messages.getString("RechnungsPrefs.paymentinst"), Messages.getString("RechnungsPrefs.PleseChooseBank"), Organisation.DEFAULT_SORT); //$NON-NLS-1$ //$NON-NLS-2$
					if (ksl.open() == Dialog.OK) {
						actBank = (Kontakt) ksl.getSelection();
						actMandant.setInfoElement(RNBANK, actBank.getId());
						setMandant(actMandant);
						exTable.setKontakt(actMandant);
					}
				}
				
			});
			banklabel = new Label(ret, SWT.NONE);
			banklabel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			exTable = new KontaktExtDialog.ExtInfoTable(parent, flds);
			exTable.setLayoutData(SWTHelper.getFillGridData(2, true, 1, true));
			exTable.setKontakt(actMandant);
			return ret;
		}
		
		@Override
		public void create(){
			super.create();
			getShell().setText(Messages.getString("RechnungsPrefs.ChooseBank")); //$NON-NLS-1$
			setTitle(actMandant.getLabel());
			setMessage(Messages.getString("RechnungsPrefs.ChosseInst")); //$NON-NLS-1$
		}
		
		@Override
		protected void okPressed(){
			super.okPressed();
		}
		
	}
	
	class PostDialog extends TitleAreaDialog {
		final String[] flds = {
			ESRNUMBER
		};
		KontaktExtDialog.ExtInfoTable exTable;
		
		PostDialog(Shell shell){
			super(shell);
		}
		
		@Override
		protected Control createDialogArea(Composite parent){
			Composite ret = new Composite(parent, SWT.NONE);
			ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
			ret.setLayout(new GridLayout(2, false));
			exTable = new KontaktExtDialog.ExtInfoTable(parent, flds);
			exTable.setLayoutData(SWTHelper.getFillGridData(2, true, 1, true));
			exTable.setKontakt(actMandant);
			return ret;
		}
		
		@Override
		public void create(){
			super.create();
			getShell().setText(Messages.getString("RechnungsPrefs.postAccount")); //$NON-NLS-1$
			setTitle(actMandant.getLabel());
			setMessage(Messages.getString("RechnungsPrefs.InfoPostAccount")); //$NON-NLS-1$
		}
		
		@Override
		protected void okPressed(){
			// TODO Automatisch erstellter Methoden-Stub
			super.okPressed();
		}
	}
	
	void setMandant(Mandant m){
		actMandant = m;
		
		tTreat.setText(actMandant.getLabel());
		
		actBank = Kontakt.load(actMandant.getInfoString(RNBANK));
		if (actBank != null && actBank.isValid()) {
			tPost.setText(""); //$NON-NLS-1$
			tBank.setText(actBank.getLabel());
		} else {
			tPost.setText(actMandant.getInfoString(ESRNUMBER));
			tBank.setText(""); //$NON-NLS-1$
			
			actBank = null;
		}
		bPost.setSelection(actBank == null);
		bBank.setSelection(actBank != null);
	}
	
	/**
	 * set fonts text values from configuration
	 */
	private void setFontsTextValues(){
		String normalFontName =
			Hub.localCfg.get(ESR.ESR_NORMAL_FONT_NAME, ESR.ESR_NORMAL_FONT_NAME_DEFAULT);
		int normalFontSize =
			Hub.localCfg.get(ESR.ESR_NORMAL_FONT_SIZE, ESR.ESR_NORMAL_FONT_SIZE_DEFAULT);
		String ocrFontName = Hub.localCfg.get(ESR.ESR_OCR_FONT_NAME, ESR.ESR_OCR_FONT_NAME_DEFAULT);
		int ocrFontSize = Hub.localCfg.get(ESR.ESR_OCR_FONT_SIZE, ESR.ESR_OCR_FONT_SIZE_DEFAULT);
		
		tESRNormalFontName.setText(normalFontName);
		tESRNormalFontSize.setText(new Integer(normalFontSize).toString());
		tESROCRFontName.setText(ocrFontName);
		tESROCRFontSize.setText(new Integer(ocrFontSize).toString());
		
		int ocrFontWeight =
			Hub.localCfg.get(ESR.ESR_OCR_FONT_WEIGHT, ESR.ESR_OCR_FONT_WEIGHT_DEFAULT);
		int index;
		switch (ocrFontWeight) {
		case SWT.MIN:
			index = FONT_MIN_INDEX;
			break;
		case SWT.NORMAL:
			index = FONT_NORMAL_INDEX;
			break;
		case SWT.BOLD:
			index = FONT_BOLD_INDEX;
			break;
		default:
			index = FONT_NORMAL_INDEX;
		}
		cbESROCRFontWeight.select(index);
		
		int printerCorrectionX =
			Hub.localCfg.get(ESR.ESR_PRINTER_CORRECTION_X, ESR.ESR_PRINTER_CORRECTION_X_DEFAULT);
		int printerCorrectionY =
			Hub.localCfg.get(ESR.ESR_PRINTER_CORRECTION_Y, ESR.ESR_PRINTER_CORRECTION_Y_DEFAULT);
		int printerBaseCorrectionX =
			Hub.localCfg.get(ESR.ESR_PRINTER_BASE_OFFSET_X, ESR.ESR_PRINTER_BASE_OFFSET_X_DEFAULT);
		
		tESRPrinterCorrectionX.setText(new Integer(printerCorrectionX).toString());
		tESRPrinterCorrectionY.setText(new Integer(printerCorrectionY).toString());
		tESRPrintCorrectionBaseX.setText(Integer.toString(printerBaseCorrectionX));
	}
	
	/**
	 * store fonts text values to configuration
	 */
	void storeFontsTextValues(){
		String normalFontName;
		int normalFontSize;
		String ocrFontName;
		int ocrFontSize;
		int ocrFontWeight;
		
		int printerCorrectionX;
		int printerCorrectionY;
		int printerBaseCorrectionX;
		
		normalFontName = tESRNormalFontName.getText().trim();
		if (StringTool.isNothing(normalFontName)) {
			normalFontName = ESR.ESR_NORMAL_FONT_NAME_DEFAULT;
		}
		// parse entered font size
		try {
			normalFontSize = Integer.parseInt(tESRNormalFontSize.getText().trim());
		} catch (NumberFormatException ex) {
			normalFontSize = ESR.ESR_NORMAL_FONT_SIZE_DEFAULT;
		}
		
		ocrFontName = tESROCRFontName.getText().trim();
		if (StringTool.isNothing(ocrFontName)) {
			ocrFontName = ESR.ESR_OCR_FONT_NAME_DEFAULT;
		}
		// parse entered font size
		try {
			ocrFontSize = Integer.parseInt(tESROCRFontSize.getText().trim());
		} catch (NumberFormatException ex) {
			ocrFontSize = ESR.ESR_OCR_FONT_SIZE_DEFAULT;
		}
		
		// get font weight selection
		int index = cbESROCRFontWeight.getSelectionIndex();
		switch (index) {
		case FONT_MIN_INDEX:
			ocrFontWeight = SWT.MIN;
			break;
		case FONT_NORMAL_INDEX:
			ocrFontWeight = SWT.NORMAL;
			break;
		case FONT_BOLD_INDEX:
			ocrFontWeight = SWT.BOLD;
			break;
		default:
			ocrFontWeight = SWT.NORMAL;
		}
		
		// parse entered corrections
		try {
			printerCorrectionX = Integer.parseInt(tESRPrinterCorrectionX.getText().trim());
		} catch (NumberFormatException ex) {
			printerCorrectionX = ESR.ESR_PRINTER_CORRECTION_X_DEFAULT;
		}
		try {
			printerCorrectionY = Integer.parseInt(tESRPrinterCorrectionY.getText().trim());
		} catch (NumberFormatException ex) {
			printerCorrectionY = ESR.ESR_PRINTER_CORRECTION_Y_DEFAULT;
		}
		try {
			printerBaseCorrectionX = Integer.parseInt(tESRPrintCorrectionBaseX.getText().trim());
		} catch (NumberFormatException ex) {
			printerBaseCorrectionX = ESR.ESR_PRINTER_BASE_OFFSET_X_DEFAULT;
		}
		
		Hub.localCfg.set(ESR.ESR_NORMAL_FONT_NAME, normalFontName);
		Hub.localCfg.set(ESR.ESR_NORMAL_FONT_SIZE, normalFontSize);
		Hub.localCfg.set(ESR.ESR_OCR_FONT_NAME, ocrFontName);
		Hub.localCfg.set(ESR.ESR_OCR_FONT_SIZE, ocrFontSize);
		Hub.localCfg.set(ESR.ESR_OCR_FONT_WEIGHT, ocrFontWeight);
		Hub.localCfg.set(ESR.ESR_PRINTER_CORRECTION_X, printerCorrectionX);
		Hub.localCfg.set(ESR.ESR_PRINTER_CORRECTION_Y, printerCorrectionY);
		Hub.localCfg.set(ESR.ESR_PRINTER_BASE_OFFSET_X, printerBaseCorrectionX);
	}
	
	/*
	 * Store the values from the font text fields
	 */
	class FontsTextListener extends FocusAdapter {
		@Override
		public void focusLost(FocusEvent e){
			storeFontsTextValues();
		}
		
	}
}
