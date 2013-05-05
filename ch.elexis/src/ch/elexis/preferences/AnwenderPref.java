/*******************************************************************************
 * Copyright (c) 2006-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/
package ch.elexis.preferences;

import java.util.Hashtable;
import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.data.Anwender;
import ch.elexis.data.Mandant;
import ch.elexis.data.Query;
import ch.elexis.preferences.inputs.PrefAccessDenied;
import ch.elexis.util.LabeledInputField;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.LabeledInputField.InputData;
import ch.elexis.util.LabeledInputField.InputData.Typ;

public class AnwenderPref extends PreferencePage implements IWorkbenchPreferencePage {
	private static final String EXT_INFO = "ExtInfo"; //$NON-NLS-1$
	
	public static final String ID = "ch.elexis.anwenderprefs"; //$NON-NLS-1$
	
	private LabeledInputField.AutoForm lfa;
	private InputData[] def;
	
	private Hashtable<String, Anwender> hAnwender = new Hashtable<String, Anwender>();
	
	@Override
	protected Control createContents(Composite parent){
		if (Hub.acl.request(AccessControlDefaults.ACL_USERS)) {
			FormToolkit tk = new FormToolkit(Desk.getDisplay());
			Form form = tk.createForm(parent);
			Composite body = form.getBody();
			body.setLayout(new GridLayout(1, false));
			Combo cbAnwender = new Combo(body, SWT.DROP_DOWN | SWT.READ_ONLY);
			Query<Anwender> qbe = new Query<Anwender>(Anwender.class);
			List<Anwender> list = qbe.execute();
			for (Anwender m : list) {
				cbAnwender.add(m.getLabel());
				hAnwender.put(m.getLabel(), m);
			}
			cbAnwender.addSelectionListener(new SelectionAdapter() {
				
				@Override
				public void widgetSelected(SelectionEvent e){
					Combo source = (Combo) e.getSource();
					String m = (source.getItem(source.getSelectionIndex()));
					Anwender anw = hAnwender.get(m);
					lfa.reload(anw);
				}
				
			});
			GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			// gd.horizontalSpan=2;
			cbAnwender.setLayoutData(gd);
			tk.adapt(cbAnwender);
			lfa = new LabeledInputField.AutoForm(body, def);
			lfa.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
			tk.paintBordersFor(body);
			return form;
		} else {
			return new PrefAccessDenied(parent);
		}
	}
	
	public void init(IWorkbench workbench){
		List<Mandant> ml = Hub.getMandantenList();
		String[] mands = new String[ml.size()];
		for (int i = 0; i < mands.length; i++) {
			mands[i] = ml.get(i).getLabel();
		}
		String grp = Hub.globalCfg.get(PreferenceConstants.ACC_GROUPS, "Admin"); //$NON-NLS-1$
		def =
			new InputData[] {
				new InputData(Messages.AnwenderPref_kuerzel, "Label", Typ.STRING, null), //$NON-NLS-1$
				new InputData(Messages.AnwenderPref_passwort, EXT_INFO, Typ.STRING, "UsrPwd"), //$NON-NLS-1$
				new InputData(Messages.AnwenderPref_gruppe, EXT_INFO, "Groups", grp.split(",")), //$NON-NLS-1$ //$NON-NLS-2$
				new InputData(Messages.AnwenderPref_fuerMandant, Messages.AnwenderPref_12,
					"Mandant", mands) //$NON-NLS-1$
			};
	}
	
}
