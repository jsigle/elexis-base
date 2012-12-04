/*******************************************************************************
 * Copyright (c) 2008-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *  $Id: PrintFindingsDialog.java 2516 2007-06-12 15:56:07Z rgw_ch $
 *******************************************************************************/
package ch.elexis.befunde.xchange;

import ch.elexis.befunde.Messwert;
import ch.elexis.data.Xid;
import ch.elexis.exchange.XChangeExporter;
import ch.elexis.exchange.elements.FindingElement;
import ch.elexis.exchange.elements.MetaElement;
import ch.elexis.exchange.elements.ResultElement;
import ch.elexis.exchange.elements.XidElement;

public class BefundeItem extends FindingElement {
	
	BefundeItem asExporter(XChangeExporter home, Messwert mw, String field){
		super.asExporter(home);
		setAttribute(ATTR_NAME, mw.getLabel() + ":" + field); //$NON-NLS-1$
		setAttribute(ATTR_GROUP, "Messwert"); //$NON-NLS-1$
		XidElement eXid = new XidElement();
		eXid.addIdentity(Xid.DOMAIN_ELEXIS, mw.getId() + field, Xid.ASSIGNMENT_LOCAL, true);
		eXid.setMainID(null);
		add(eXid);
		add(new MetaElement().asExporter(home, ResultElement.ATTRIB_CREATOR, Messwert.PLUGIN_ID));
		return this;
	}
	
}
