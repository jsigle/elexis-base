/*******************************************************************************
 * Copyright (c) 2007-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    M. Descher - implemented cache
 *    
 *  $Id: VidalLabelProvider.java 6442 2010-06-30 09:49:08Z marcode79 $
 *******************************************************************************/
package ch.elexis.artikel_at.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.artikel_at.data.Artikel_AT_Cache;
import ch.elexis.artikel_at.data.Medikament;
import ch.elexis.util.Log;
import ch.elexis.util.viewers.DefaultLabelProvider;

public class VidalLabelProvider extends DefaultLabelProvider implements ITableColorProvider {
	
	public VidalLabelProvider(){
		if (Desk.getImage("VidalRed") == null) {
			Desk.getImageRegistry().put("VidalRed", getImageDescriptor("rsc/redbox.ico"));
		}
		if (Desk.getImage("VidalGreen") == null) {
			Desk.getImageRegistry().put("VidalGreen", getImageDescriptor("rsc/greenbox.ico"));
		}
		if (Desk.getImage("VidalYellow") == null) {
			Desk.getImageRegistry().put("VidalYellow", getImageDescriptor("rsc/yellowbox.ico"));
		}
		if (Desk.getImage("VidalBlack") == null) {
			Desk.getImageRegistry().put("VidalBlack", getImageDescriptor("rsc/blackbox.ico"));
		}
	}
	
	public static ImageDescriptor getImageDescriptor(String path){
		return AbstractUIPlugin.imageDescriptorFromPlugin("ch.elexis.artikel_at", path); //$NON-NLS-1$
	}
	
	@Override
	public Image getColumnImage(Object element, int columnIndex){
		String box;
		
		if (!(element instanceof Medikament)) {
			return Desk.getImage(Desk.IMG_ACHTUNG);
		}
		Medikament art = (Medikament) element;
		box = Artikel_AT_Cache.get(art.getId(), Artikel_AT_Cache.MEDIKAMENT_AT_CACHE_ELEMENT_BOX);
		
		if (box != null) {
			if (box.startsWith("N")) {
				return null;
			} else if (box.startsWith("R")) {
				return Desk.getImage("VidalRed");
			} else if (box.startsWith("G")) {
				return Desk.getImage("VidalGreen");
			} else if (box.startsWith("Y")) {
				return Desk.getImage("VidalYellow");
			} else if (box.startsWith("B")) {
				return Desk.getImage("VidalBlack");
			}
		} else {
			Hub.log.log("Box is Null!", Log.ERRORS);
		}
		return null;
	}
	
	@Override
	public String getColumnText(Object element, int columnIndex){
		
		if (element instanceof Medikament) {
			Medikament art = (Medikament) element;
			return Artikel_AT_Cache.get(art.getId(),
				Artikel_AT_Cache.MEDIKAMENT_AT_CACHE_ELEMENT_LABEL);
		}
		return super.getColumnText(element, columnIndex);
	}
	
	public Color getBackground(Object element, int columnIndex){
		// TODO Auto-generated method stub
		return null;
	}
	
	public Color getForeground(Object element, int columnIndex){
		// Extremely slow function
		// if (element instanceof Artikel) {
		// if (((Artikel) element).isLagerartikel()) {
		// return Desk.getColor(Desk.COL_BLUE);
		// }
		// }
		return null;
	}
	
}
