/*******************************************************************************
 * Copyright (c) 2005-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: ODDBView.java 3681 2008-02-15 17:47:31Z rgw_ch $
 *******************************************************************************/
package ch.elexis.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.actions.GlobalActions;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;

/**
 * Diese View reichtet einen Browser aufs Arzneimittel-Kompendium ein.
 */
public class ODDBView extends ViewPart implements ISaveablePart2 {
	public static final String ID = "ch.elexis.ODDBView";
	Browser browser;
	
	@Override
	public void createPartControl(Composite parent){
		browser = new Browser(parent, SWT.NONE);
		browser.addLocationListener(new LocationAdapter() {
			
			@Override
			public void changed(LocationEvent arg0){
				String text = getText(arg0.location);
				System.out.println(text);
			}
			
		});
		// browser.setUrl("http://ch.oddb.org");
		browser.setUrl("http://santesuisse.oddb.org/");
		
	}
	
	@Override
	public void setFocus(){
		// TODO Auto-generated method stub
		
	}
	
	public String getText(String loc){
		try {
			if (StringTool.isNothing(loc)) {
				loc = browser.getUrl();
			}
			URLConnection url = new URL(loc).openConnection();
			url.setDoInput(true);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.getInputStream()));
			StringBuilder ret = new StringBuilder();
			String line;
			;
			while ((line = in.readLine()) != null) {
				ret.append(line);
			}
			// Programm beenden
			in.close();
			return ret.toString();
		} catch (IOException e) {
			ExHandler.handle(e);
			return "";
		}
	}
	
	/* ******
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir benötigen das
	 * Interface nur, um das Schliessen einer View zu verhindern, wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */
	public int promptToSaveOnClose(){
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL
				: ISaveablePart2.NO;
	}
	
	public void doSave(IProgressMonitor monitor){ /* leer */}
	
	public void doSaveAs(){ /* leer */}
	
	public boolean isDirty(){
		return true;
	}
	
	public boolean isSaveAsAllowed(){
		return false;
	}
	
	public boolean isSaveOnCloseNeeded(){
		return true;
	}
	
}
