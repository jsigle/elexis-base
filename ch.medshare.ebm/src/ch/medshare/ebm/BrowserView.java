/*******************************************************************************
 * Copyright (c) 2010, St. Schenk and Medshare GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    St. Schenk - initial implementation
 * 
 *******************************************************************************/

package ch.medshare.ebm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Hub;

public class BrowserView extends ViewPart {
	public static final String ID = "ebm-guidelines";
	
	private Browser browser;
	
	@Override
	public void createPartControl(Composite parent){
		browser = new Browser(parent, SWT.NONE);
	}
	
	@Override
	public void dispose(){
		// URL zurücksetzen
		Hub.userCfg.set(Preferences.LOGGEDIN, "");
		super.dispose();
	}
	
	private String getURL(){
		String url = Hub.userCfg.get(Preferences.LOGGEDIN, Preferences.Defaults.LOGGEDIN);
		if (url.length() == 2) {
			url = new EbmLogIn().doPostLogin("");
		}
		if (!url.startsWith("http")) {
			url = "https://www.ebm-guidelines.ch/";
		}
		return url;
	}
	
	@Override
	public void setFocus(){
		if (browser != null) {
			browser.setUrl(getURL());
		}
	}
	
}
