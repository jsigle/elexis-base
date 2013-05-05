/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.privatnotizen;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.text.IRichTextDisplay;
import ch.elexis.util.IKonsExtension;

public class KonsExtension implements IKonsExtension {
	IRichTextDisplay mine = null;
	
	/**
	 * Die Darstellung unserer Hyperlinks. Da wir alle Hyperlinks gleich darstellen, interessieren
	 * uns hier provider und id nicht. Wir formatieren unsere Links einfach kursiv, und wir geben
	 * "true" zurück um anzuzeigen, dass wir auf Mausklicks reagieren wollen.
	 */
	public boolean doLayout(StyleRange n, String provider, String id){
		n.fontStyle = SWT.ITALIC;
		return true;
	}
	
	/**
	 * Der Anwender hat auf einen unserer Links geklickt. Wir müssen jetzt anhand der id
	 * herausfinden, welcher Link das war. Wenn der Anwender derjenige Mandant ist, dem der Eintrag
	 * "gehört", dann zeigen wir den Inhalt der Notiz an.
	 */
	public boolean doXRef(String refProvider, String refID){
		Privatnotiz clicked = Privatnotiz.load(refID);
		if (clicked.getMandantID().equals(Hub.actUser.getId())) {
			new NotizInputDialog(Desk.getTopShell(), clicked).open();
			return true;
		}
		return false;
	}
	
	/**
	 * Wir möchten gern ein Kontextmenu in der KonsDetailView einbringen. Dieses dient dazu, eine
	 * neue Notiz zu erstellen.
	 */
	public IAction[] getActions(){
		IAction[] ret = new IAction[1];
		ret[0] = new Action(Messages.KonsExtension_noteActionLabel) {
			
			@Override
			public void run(){
				Privatnotiz np = new Privatnotiz(Hub.actMandant);
				if (new NotizInputDialog(Desk.getTopShell(), np).open() == Dialog.OK) {
					mine.insertXRef(-1, Messages.KonsExtension_noteActionXREFText,
						"privatnotizen", np.getId()); //$NON-NLS-2$
				} else {
					np.delete();
				}
			}
			
		};
		return ret;
	}
	
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException{
		// TODO Auto-generated method stub
		
	}
	
	public String connect(IRichTextDisplay tf){
		mine = tf;
		return "privatnotizen"; //$NON-NLS-1$
	}
	
	/**
	 * Der Anwender möchte den Querverweis wieder entfernen
	 */
	public void removeXRef(String refProvider, String refID){
		Privatnotiz n = Privatnotiz.load(refID);
		n.delete();
	}
	
	public void insert(Object o, int pos){
		// TODO Auto-generated method stub
		
	}
	
}
