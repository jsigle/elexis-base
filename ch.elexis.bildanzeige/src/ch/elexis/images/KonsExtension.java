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
 *    $Id: KonsExtension.java 6194 2010-03-14 12:13:27Z rgw_ch $
 *******************************************************************************/

package ch.elexis.images;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.FileDialog;

import ch.elexis.Desk;
import ch.elexis.text.IRichTextDisplay;
import ch.elexis.util.IKonsExtension;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;

public class KonsExtension implements IKonsExtension {
	IRichTextDisplay mine;
	
	public String connect(IRichTextDisplay tf){
		mine = tf;
		return "bildanzeige"; //$NON-NLS-1$
	}
	
	public boolean doLayout(StyleRange n, String provider, String id){
		n.background = Desk.getColor(Desk.COL_GREEN);
		return true;
	}
	
	public boolean doXRef(String refProvider, String refID){
		Bild bild = Bild.load(refID);
		new BildanzeigeFenster(Desk.getTopShell(), bild).open();
		return true;
	}
	
	public IAction[] getActions(){
		IAction[] ret = new IAction[1];
		ret[0] = new Action(Messages.KonsExtension_InsertImage) {
			@Override
			public void run(){
				FileDialog fd = new FileDialog(Desk.getTopShell());
				String iName = fd.open();
				if (iName != null) {
					try {
						ImageLoader iml = new ImageLoader();
						iml.load(iName);
						BildImportDialog bid = new BildImportDialog(Desk.getTopShell(), iml);
						if (bid.open() == Dialog.OK) {
							Bild bild = bid.result;
							mine.insertXRef(
								-1,
								Messages.KonsExtension_Image + bild.get("Titel"), "bildanzeige", bild.getId()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						}
						
					} catch (Throwable t) {
						ExHandler.handle(t);
						SWTHelper.showError(Messages.KonsExtension_ErrorLoading,
							Messages.KonsExtension_ImageCouldnotBeLoaded + t.getMessage());
					}
				}
			}
			
		};
		return ret;
	}
	
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException{
		// TODO Auto-generated method stub
		
	}
	
	public void removeXRef(String refProvider, String refID){
		Bild bild = Bild.load(refID);
		bild.delete();
	}
	
	public void insert(Object o, int pos){
		// TODO Auto-generated method stub
		
	}
	
}
