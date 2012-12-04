/*******************************************************************************
 * Copyright (c) 2007, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: SubstanzSelektor.java 5016 2009-01-23 16:32:22Z rgw_ch $
 *****************************************************************************/

package ch.elexis.medikamente.bag.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.medikamente.bag.data.Substance;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.DefaultContentProvider;
import ch.elexis.util.viewers.DefaultControlFieldProvider;
import ch.elexis.util.viewers.DefaultLabelProvider;
import ch.elexis.util.viewers.SimpleWidgetProvider;
import ch.elexis.util.viewers.ViewerConfigurer;

public class SubstanzSelektor extends Dialog {
	CommonViewer cv;
	Substance result;
	
	public SubstanzSelektor(Shell shell){
		super(shell);
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		cv = new CommonViewer();
		ViewerConfigurer vc =
			new ViewerConfigurer(new DefaultContentProvider(cv, Substance.class, new String[] {
				"name"
			}, false), new DefaultLabelProvider(), new DefaultControlFieldProvider(cv,
				new String[] {
					"name=Name"
				}), new ViewerConfigurer.DefaultButtonProvider(), new SimpleWidgetProvider(
				SimpleWidgetProvider.TYPE_LIST, SWT.V_SCROLL, null));
		cv.create(vc, parent, SWT.NONE, parent);
		vc.getContentProvider().startListening();
		return cv.getViewerWidget().getControl();
	}
	
	@Override
	public boolean close(){
		cv.getConfigurer().getContentProvider().stopListening();
		result = (Substance) cv.getSelection()[0];
		return super.close();
	}
	
	@Override
	public void create(){
		super.create();
		getShell().setText("Bitte Substanz auswählen");
	}
	
}
