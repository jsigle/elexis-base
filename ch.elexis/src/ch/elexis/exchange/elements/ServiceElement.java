/*******************************************************************************
 * Copyright (c) 2010 by G. Weirich
 * This program is based on the Sgam-Exchange project,
 * (c) SGAM-Informatics
 * All rights resevred
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *  $Id: ServiceElement.java 5880 2009-12-19 19:25:22Z rgw_ch $
 *******************************************************************************/
package ch.elexis.exchange.elements;

import java.util.List;

import org.jdom.Element;

import ch.elexis.data.Eigenleistung;
import ch.elexis.data.IVerrechenbar;
import ch.elexis.data.PersistentObject;
import ch.elexis.exchange.XChangeContainer;
import ch.elexis.exchange.XChangeExporter;
import ch.rgw.tools.TimeTool;

public class ServiceElement extends XChangeElement {
	
	private static final long serialVersionUID = 6382517263003793221L;
	public static final String XMLNAME = "service";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_CONTRACT_NAME = "contractName";
	public static final String ATTR_CONTRACT_CODE = "contractCode";
	public static final String ATTR_MINUTES = "minutes";
	public static final String ATTR_COST = "cost";
	public static final String ATTR_PRICE = "price";
	public static final String ELEMENT_XID = XidElement.XMLNAME;
	
	public ServiceElement asExporter(XChangeExporter p, IVerrechenbar iv){
		asExporter(p);
		setAttribute(ATTR_NAME, iv.getText());
		setAttribute(ATTR_CONTRACT_CODE, iv.getCode());
		setAttribute(ATTR_CONTRACT_NAME, iv.getCodeSystemName());
		setAttribute(ATTR_MINUTES, Integer.toString(iv.getMinutes()));
		setAttribute(ATTR_COST, iv.getKosten(new TimeTool()).getCentsAsString());
		setAttribute(ATTR_PRICE, Integer.toString(iv.getTP(new TimeTool(), null)));
		add(new XidElement().asExporter(p, iv));
		return this;
	}
	
	public IVerrechenbar createObject(XChangeContainer home, Element el){
		XidElement xide = (XidElement) getChild(XidElement.XMLNAME, XidElement.class);
		List<PersistentObject> objs = xide.findObject();
		for (PersistentObject po : objs) {
			if (po instanceof IVerrechenbar) {
				return (IVerrechenbar) po;
			}
		}
		Eigenleistung egl =
			new Eigenleistung(el.getAttributeValue(ATTR_CONTRACT_CODE),
				el.getAttributeValue(ATTR_NAME), el.getAttributeValue(ATTR_COST),
				el.getAttributeValue(ATTR_PRICE));
		return egl;
	}
	
	@Override
	public String getXMLName(){
		return XMLNAME;
	}
	
}
