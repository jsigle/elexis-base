/*******************************************************************************
 * Copyright (c) 2007-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.medikamente.bag.views;

import java.util.List;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.data.Artikel;
import ch.elexis.medikamente.bag.data.BAGMedi;
import ch.elexis.medikamente.bag.data.Substance;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.util.viewers.DefaultLabelProvider;
import ch.rgw.tools.StringTool;

public class BAGMediLabelProvider extends DefaultLabelProvider implements ITableColorProvider {
	
	@Override
	public String getColumnText(final Object element, final int columnIndex){
		if (element instanceof BAGMedi) {
			BAGMedi bm = (BAGMedi) element;
			StringBuilder sb = new StringBuilder();
			sb.append(bm.getLabel()).append(" <").append(bm.getVKPreis().getAmountAsString())
				.append(">");
			
			List<Substance> conts = bm.getSubstances();
			if (conts.size() > 0) {
				sb.append("[");
				for (Substance s : conts) {
					sb.append(s.getLabel()).append("; ");
				}
				sb.append("]");
			}
			if (bm.isLagerartikel()) {
				sb.append(" (").append(bm.getTotalCount()).append(")");
			}
			
			return sb.toString();
		}
		return super.getColumnText(element, columnIndex);
	}
	
	public Color getBackground(final Object element, final int columnIndex){
		// TODO Auto-generated method stub
		return null;
	}
	
	public Color getForeground(final Object element, final int columnIndex){
		if (element instanceof Artikel) {
			Artikel art = (Artikel) element;
			
			if (art.isLagerartikel()) {
				int trigger =
					Hub.globalCfg.get(PreferenceConstants.INVENTORY_ORDER_TRIGGER,
						PreferenceConstants.INVENTORY_ORDER_TRIGGER_DEFAULT);
				
				int ist = art.getIstbestand();
				int min = art.getMinbestand();
				
				boolean order = false;
				switch (trigger) {
				case PreferenceConstants.INVENTORY_ORDER_TRIGGER_BELOW:
					order = (ist < min);
					break;
				case PreferenceConstants.INVENTORY_ORDER_TRIGGER_EQUAL:
					order = (ist <= min);
					break;
				default:
					order = (ist < min);
				}
				
				if (order) {
					return Desk.getColor(Desk.COL_RED);
				} else {
					return Desk.getColor(Desk.COL_BLUE);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public Image getColumnImage(final Object element, final int columnIndex){
		if (element instanceof BAGMedi) {
			BAGMedi bm = (BAGMedi) element;
			String g = StringTool.unNull(bm.get("Generikum"));
			/*
			 * if(g.equals("")){ return Desk.getImage(Desk.IMG_ACHTUNG); }
			 */
			if (g.startsWith("G")) {
				return Desk.getImage(BAGMedi.IMG_GENERIKUM);
			} else if (g.startsWith("O")) {
				return Desk.getImage(BAGMedi.IMG_HAS_GENERIKA);
			} else {
				return Desk.getImage(BAGMedi.IMG_ORIGINAL);
			}
		}
		
		return super.getColumnImage(element, columnIndex);
	}
	
}
