/*******************************************************************************
 * Copyright (c) 2006-2010, D. Lutz and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    D. Lutz - initial implementation
 *    G. Weirich - Adapted for API changes
 * 
 *******************************************************************************/

package ch.medshare.ebm;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import ch.elexis.Hub;
import ch.elexis.text.EnhancedTextField;
import ch.elexis.text.IRichTextDisplay;
import ch.elexis.util.IKonsExtension;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.GenericRange;
import ch.rgw.tools.StringTool;

public class SearchAction extends Action implements IKonsExtension, IHandler {
	
	public static final String ID = "ch.medshare.ebm";
	private static IRichTextDisplay textField;
	private static SearchAction instance;
	
	public String connect(IRichTextDisplay tf){
		SearchAction.textField = (EnhancedTextField) tf;
		return "ch.medshare.ebm";
	}
	
	public SearchAction(){
		super(Messages.getString("SearchAction", "Name"));
	}
	
	public boolean doLayout(StyleRange n, String provider, String id){
		return false;
	}
	
	public boolean doXRef(String refProvider, String refID){
		return false;
	}
	
	@Override
	public void run(){
		String search = "";
		if (textField != null) {
			String text = textField.getContentsPlaintext();
			GenericRange gr = textField.getSelectedRange();
			if (gr.getLength() == 0) {
				search = StringTool.getWordAtIndex(text, gr.getPos());
			} else {
				search = text.substring(gr.getPos(), gr.getPos() + gr.getLength());
			}
			search = search.trim().replace("\r\n", " ");
		}
		
		String url = new EbmLogIn().doPostLogin(search);
		
		if (url == null) {
			SWTHelper.showError("EBM-Guidelines", "URL überprüfen!");
		} else if (url.startsWith("-")) {
			if (url.contains("-1")) {
				SWTHelper.showError("EBM-Guidelines", "Fehlercode: -1\r\nKein gültiges Login");
			} else if (url.contains("-2")) {
				SWTHelper.showError("EBM-Guidelines",
					"Fehlercode: -2\r\nKein bestehendes aktives Abo");
			} else if (url.contains("-3")) {
				SWTHelper.showError("EBM-Guidelines",
					"Fehlercode: -3\r\nKein Benutzername oder Passwort");
			} else {
				SWTHelper.showError("EBM-Guidelines", "Fehlercode: " + url
					+ "\r\nUnbekannter Fehler");
			}
		} else {
			if (Hub.userCfg.get(Preferences.EXTERN, Preferences.Defaults.EXTERN)) {
				Hub.userCfg.set(Preferences.LOGGEDIN, url);
				Program.launch(url);
			} else {
				try {
					IWorkbenchPage wbPage =
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						Hub.userCfg.set(Preferences.LOGGEDIN, url);
						wbPage.showView(BrowserView.ID);
					} catch (PartInitException e2) {
						ExHandler.handle(e2);
					}
				} catch (Exception e) {}
			}
		}
	}
	
	public IAction[] getActions(){
		return new IAction[] {
			this
		};
	}
	
	public void insert(Object o, int pos){
		// TODO Auto-generated method stub
		
	}
	
	public void removeXRef(String refProvider, String refID){
		// TODO Auto-generated method stub
		
	}
	
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException{
		// TODO Auto-generated method stub
		
	}
	
	public void addHandlerListener(IHandlerListener handlerListener){
		// TODO Auto-generated method stub
		
	}
	
	public void dispose(){
		// TODO Auto-generated method stub
		
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException{
		if (instance != null)
			instance.run();
		return null;
	}
	
	public void removeHandlerListener(IHandlerListener handlerListener){
		// TODO Auto-generated method stub
	}
}
