/*******************************************************************************
 * Copyright (c) 2013 J. Sigle; Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    J. Sigle - added a preference page to omnivore to control  a new automatic archiving functionality  
 *    G. Weirich and others - preference pages for other plugins, used as models for this one
 *    
 *******************************************************************************/

package ch.elexis.omnivore.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.omnivore.preferences.Messages;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.util.SWTHelper;

//FIXME: 20130325js: Layout needs a thorough redesign. See: http://www.eclipse.org/articles/article.php?file=Article-Understanding-Layouts/index.html
//FIXME: 20130325js: We want a layout that will use all the available space, auto re-size input fields etc., have nested elements, and still NOT result in "dialog has invalid data" error messages.

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String PREFERENCE_BRANCH = "plugins/omnivore_js/"; //$NON-NLS-1$
	public static final String PREFERENCE_SRC_PATTERN = "src_pattern"; //$NON-NLS-1$
	public static final String PREFERENCE_DEST_DIR = "dest_dir"; //$NON-NLS-1$

	//20130325js: The following setting is used in ch.elexis.omnivore.data/DocHandle.java.
	//Linux and MacOS may be able to handle longer filenames, but we observed that Windows 7 64-bit will not import files with names longer than 80 chars.
	//So I make this setting configurable. Including a safe default and limits that a user cannot exceed.
	public static final Integer Omnivore_jsMax_Filename_Length_Min=12;	
	public static final Integer Omnivore_jsMax_Filename_Length_Default=80;
	public static final Integer Omnivore_jsMax_Filename_Length_Max=255;
	public static final String PREF_MAX_FILENAME_LENGTH= PREFERENCE_BRANCH+"max_filename_length";
	//20130325js: For automatic archiving of incoming files:
	//Here is a comfortable way to specify how many rules shall be available:
	//The individual Strings in the following arrays may be left empty - they will be automatically filled.
	//But the smaller number of entries for Src and Dest determines
	//how many rule editing field pairs are provided on the actual preferences page, and processed later on.
	//The actual content of all field labels, and all preference store keys,
	//is computed from content of the messages.properties file.
	//I've tested the construction of the preferences dialog with fields for some 26 rules, worked like a charm :-)
    public static final String[] PREF_SRC_PATTERN = { "",  "", "",  "", "" };	
	public static final String[] PREF_DEST_DIR  = { "", "", "",  "", "" };				
	public static final int nPREF_SRC_PATTERN=PREF_SRC_PATTERN.length;
	public static final int nPREF_DEST_DIR=PREF_DEST_DIR.length;
	
	public PreferencePage(){
		super(GRID);

		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
		setDescription(Messages.Omnivore_jsPREF_omnivore_js);
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new StringFieldEditor(PREF_MAX_FILENAME_LENGTH, Messages.Omnivore_jsPREF_MAX_FILENAME_LENGTH, getFieldEditorParent()));
		
		//20130325js: For automatic archiving of incoming files:
		//add field groups for display or editing of rule sets.
		//First, we define a new group (that will visually appear as an outlined box) and give it a header like setText("Regel i");
		//Then, within this group, we add one StringFieldEditor for the search pattern to be matched, and a DirectoryFieldEditor for the auto archiving target to be used.
		Integer nRules=getOmnivore_jsnRulesForAutoArchiving();
		Group gAutoArchiveRules = new Group(getFieldEditorParent(), SWT.NONE);
			GridData gAutoArchiveRulesGridLayoutData = new GridData();
			gAutoArchiveRulesGridLayoutData.horizontalSpan = 3;	//damit die Gruppe der Rules so breit ist, wie oben Label und Max_Filename_Length Eingabefeld zusammen.
			gAutoArchiveRulesGridLayoutData.grabExcessHorizontalSpace = true;	//damit die Gruppe der Rules so breit ist, wie oben Label und Max_Filename_Length Eingabefeld zusammen.
			gAutoArchiveRules.setLayoutData(gAutoArchiveRulesGridLayoutData);
		
			gAutoArchiveRules.setText(Messages.Omnivore_jsPREF_automatic_archiving_of_processed_files);
			//gAutoArchiveRules.setLayoutData(SWTHelper.getFillGridData(2,true,nRules,true));	//use all available horizontal space, but not necessarily all the vertical space.
			//gAutoArchiveRules.setLayout(new FillLayout());
			GridLayout gAutoArchiveRulesGridLayout = new GridLayout();
			gAutoArchiveRulesGridLayout.numColumns= 1;		//bestimmt die Anzahl der Spalten, in denen die Regeln innerhab des AutoArchiveRules containers angeordnet werden.
			gAutoArchiveRules.setLayout(gAutoArchiveRulesGridLayout);


  			for (int i=0;i<nRules;i++) {
 
				Group gRule = new Group(gAutoArchiveRules, SWT.NONE);
				GridData gRuleGridLayoutData = new GridData();
				gRuleGridLayoutData.horizontalSpan = 2;	//damit die Gruppe der Rules so breit ist, wie oben Label und Max_Filename_Length Eingabefeld zusammen.
				gRuleGridLayoutData.grabExcessHorizontalSpace = true;	//damit die Gruppe der Rules so breit ist, wie oben Label und Max_Filename_Length Eingabefeld zusammen.
				gRule.setLayoutData(gRuleGridLayoutData);

				//Cave: The labels show 1-based rule numbers, although the actual array indizes are 0 based. 
				gRule.setText(Messages.Omnivore_jsPREF_Rule+" "+(i+1));	//The brackets are needed, or the string representations of i and 1 will both be added...
				//gRule.setLayoutData(SWTHelper.getFillGridData(2,false,2,false));	//use all available horizontal space, but not necessarily all the vertical space.
				//gRule.setLayout(new FillLayout());
				GridLayout gRuleGridLayout = new GridLayout();
				gRuleGridLayout.numColumns=2;					//bestimmt die Anzahl der Spalten für jede Regel: links label, rechts eingabefeld (ggf. mit Knopf) 2->3: no change.
				gRule.setLayout(gRuleGridLayout);
				addField(new StringFieldEditor(PREF_SRC_PATTERN[i], Messages.Omnivore_jsPREF_SRC_PATTERN, gRule));
				addField(new DirectoryFieldEditor(PREF_DEST_DIR[i], Messages.Omnivore_jsPREF_DEST_DIR, gRule));
			}
  			
	}
	
	@Override
	public void init(IWorkbench workbench){
		//20130325js: For automatic archiving of incoming files:
		//construct the keys to the elexis preference store from a fixed header plus rule number:
		for (Integer i=0;i<getOmnivore_jsnRulesForAutoArchiving();i++) {
			PREF_SRC_PATTERN[i]=PREFERENCE_BRANCH + PREFERENCE_SRC_PATTERN + i.toString().trim(); //$NON-NLS-1$	//20130325js: If this source pattern is found in the filename...
			PREF_DEST_DIR[i]= PREFERENCE_BRANCH + PREFERENCE_DEST_DIR + i.toString().trim(); //$NON-NLS-1$					//20130325js: the incoming file will be archived here after having been read
		}
		
	}
	
	@Override
	protected void performApply(){
		
		//FIXME: Funktionalität, damit man bei Apply sieht, wenn der Wert für PREF_MAX_FILENAME_LENGTH irgendwo geclampt wird. Im Moment geht das nicht, weil das zurückschreiben der Werte aus den Editorfeldern ohne meine Kontrolle abläuft, weil ich auch die Editorfelder im Moment garnicht für mich zugreifbar mache (wenn ich mich nicht irre). Somit erscheint ein geclampter oder auf default gesetzter Wert erst beim nächsten Öffnen des Dialogs. Oder: Deshalb: nur get...() liefert den geclampten Wert (weil diese Funktion das clamping macht); in den Einstellungen steht er jedoch trotzdem noch >max oder <min drin.
		super.performApply();
				
		Hub.localCfg.flush();
	}
	
	//----------------------------------------------------------------------------
	  /**
	   * Returns a currently value from the preference store, observing default settings and min/max settings for that parameter
	   * 
	   * @param  Can be called with an already available preferenceStore. If none is passed, one will be temporarily instantiated on the fly.
	   *
	   * @return The requested integer parameter
	   * 
	   * @author Joerg Sigle
	   */
	
	  public static Integer getOmnivore_jsMax_Filename_Length() {
		IPreferenceStore preferenceStore=new SettingsPreferenceStore(Hub.localCfg);
		return getOmnivore_jsMax_Filename_Length(preferenceStore);
	  }	
	  
	  public static Integer getOmnivore_jsMax_Filename_Length(IPreferenceStore preferenceStore) {
			
		Integer Omnivore_jsMax_Filename_Length=Omnivore_jsMax_Filename_Length_Default;		//Start by establishing a valid default setting
		try {
			Omnivore_jsMax_Filename_Length=Integer.parseInt(preferenceStore.getString(PREF_MAX_FILENAME_LENGTH).trim());  //20130325js max filename length before error message is shown is now configurable
		} catch (Throwable throwable) {
		    //do not consume
		}
		if (Omnivore_jsMax_Filename_Length<Omnivore_jsMax_Filename_Length_Min) {Omnivore_jsMax_Filename_Length=Omnivore_jsMax_Filename_Length_Min;};
		if (Omnivore_jsMax_Filename_Length>Omnivore_jsMax_Filename_Length_Max) {Omnivore_jsMax_Filename_Length=Omnivore_jsMax_Filename_Length_Max;};
		
		return Omnivore_jsMax_Filename_Length;
	  }

		//----------------------------------------------------------------------------
	  /**
	   * Returns the number of rules to process for automatic archiving
	   * 
	   * @author Joerg Sigle
	   */
	
	  public static Integer getOmnivore_jsnRulesForAutoArchiving() {
			//20130325js: For automatic archiving of incoming files:
			//The smaller number of entries available for Src and Dest determines
			//how many rule editing field pairs are provided on the actual preferences page, and processed later on.
			//Now: Determine the number of slots for rule defining src and target strings,
			//and compute the actual number of rules to be the larger of these two.
			//Normally, they should be identical, if the dummy arrays used for initialization above have had the same size.
			Integer nRules=nPREF_SRC_PATTERN;																												
			if (nPREF_DEST_DIR>nPREF_SRC_PATTERN)	
				{nRules=nPREF_DEST_DIR;};
				
		return nRules;
	  }	
	  
		//----------------------------------------------------------------------------
	  /**
	   * Returns configured content of rules for automatic archiving
	   * 
	   * @param Rule number whose match pattern shall be retrieved. Cave: Visible only internally to the program, this index is 0 based, whereas the preference page for the user shows 1-based "Rule n" headings. 
	   *
	   * @return Either null if the index is out of bounds, or if the respective String  is technically undefined (which should never be the case); or the respective String (which may also be "", i.e. an empty string), if the user has cleared or left clear the respective input field. 
	   * 
	   * @author Joerg Sigle
	   */
	
	  public static String getOmnivore_jsRuleForAutoArchivingSrcPattern(Integer i) {
		  if ((i<0) || (i>=getOmnivore_jsnRulesForAutoArchiving())) {
			  return null;
		  }
		  
		  //The preferences keys should already have been constructed by init  - but if not, let's do it here for the one that we need now: 
		  if (PREF_SRC_PATTERN[i].equals("")) {
			PREF_SRC_PATTERN[i]=PREFERENCE_BRANCH + PREFERENCE_SRC_PATTERN + i.toString().trim(); //$NON-NLS-1$
		  }		  
		  return Hub.localCfg.get(PREF_SRC_PATTERN[i],"").trim();
	  }	

	  //----------------------------------------------------------------------------
	  /**
	   * Returns configured content of rules for automatic archiving
	   * 
	   * @param Rule number whose destination directory shall be retrieved. Cave: Visible only internally to the program, this index is 0 based, whereas the preference page for the user shows 1-based "Rule n" headings. 
	   *
	   * @return Either null if the index is out of bounds, or if the respective String  is technically undefined (which should never be the case); or the respective String (which may also be "", i.e. an empty string), if the user has cleared or left clear the respective input field. 
	   * 
	   * @author Joerg Sigle
	   */
	
	  public static String getOmnivore_jsRuleForAutoArchivingDestDir(Integer i) {
		  if ((i<0) || (i>=getOmnivore_jsnRulesForAutoArchiving())) {
			  return null;
		  }
		  
		  //The preferences keys should already have been constructed by init  - but if not, let's do it here for the one that we need now: 
		  if (PREF_DEST_DIR[i].equals("")) {
			  PREF_DEST_DIR[i]=PREFERENCE_BRANCH + PREFERENCE_DEST_DIR + i.toString().trim(); //$NON-NLS-1$
		  }		  
		  return Hub.localCfg.get(PREF_DEST_DIR[i],"").trim();
	  }	


}
