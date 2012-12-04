/*******************************************************************************
 * Copyright (c) 2006, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: OOWrapper.java 1047 2006-10-05 21:05:17Z rgw_ch $
 *******************************************************************************/

package ch.rgw.oowrapper;

import java.io.IOException;

import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class OOWrapper {
	private static OOWrapper theInstance = null;
	
	private XComponentContext xContext = null;
	// private XMultiComponentFactory xRemoteServiceManager = null;
	/*
	 * private String unoUrl = "uno:socket,host=localhost,port=8100;urp;" +
	 * "StarOffice.ServiceManager";
	 */
	private boolean connected = false;
	
	private OOWrapper(){}
	
	public static OOWrapper getInstance(){
		if (theInstance == null) {
			theInstance = new OOWrapper();
		}
		return theInstance;
	}
	
	public boolean isConnected(){
		return connected;
	}
	
	public boolean connect(){
		try {
			xContext = com.sun.star.comp.helper.Bootstrap.bootstrap();
		} catch (Exception ex) {
			try {
				useConnection();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Lädt ein template,speichert es am angegebenen Ziel unter neuem Namen und öffnet ein
	 * OpenOffice-Fenster mit diesem File
	 * 
	 * @param template
	 *            Pfadname eines existierenden Files als Schablone
	 * @param Pfadname
	 *            des mit der Schablone zu erstellenden Zielfiles
	 * @return true bei Erfolg
	 */
	public XTextDocument createTextFile(String template, String dest){
		if (xContext == null) {
			if (connect() == false) {
				return null;
			}
		}
		try {
			
			// get the remote office service manager
			com.sun.star.lang.XMultiComponentFactory xMCF = xContext.getServiceManager();
			
			Object oDesktop =
				xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
			
			com.sun.star.frame.XComponentLoader xCompLoader =
				(com.sun.star.frame.XComponentLoader) UnoRuntime.queryInterface(
					com.sun.star.frame.XComponentLoader.class, oDesktop);
			
			String sLoadUrl;
			if ((template == null) || (template.equals(""))) {
				sLoadUrl = "private:factory/swriter";
			} else {
				sLoadUrl = makeURL(template);
			}
			String sSaveUrl = makeURL(dest);
			
			com.sun.star.beans.PropertyValue[] propertyValue =
				new com.sun.star.beans.PropertyValue[1];
			propertyValue[0] = new com.sun.star.beans.PropertyValue();
			propertyValue[0].Name = "Hidden";
			propertyValue[0].Value = new Boolean(true);
			
			Object oDocToStore =
				xCompLoader.loadComponentFromURL(sLoadUrl, "_blank", 0, propertyValue);
			com.sun.star.frame.XStorable xStorable =
				(com.sun.star.frame.XStorable) UnoRuntime.queryInterface(
					com.sun.star.frame.XStorable.class, oDocToStore);
			
			propertyValue = new com.sun.star.beans.PropertyValue[1];
			propertyValue[0] = new com.sun.star.beans.PropertyValue();
			propertyValue[0].Name = "Overwrite";
			propertyValue[0].Value = new Boolean(true);
			// propertyValue[1] = new com.sun.star.beans.PropertyValue();
			
			// propertyValue[1].Name = "FilterName";
			// propertyValue[1].Value = "OpenDocument (Writer)"; //"StarOffice XML (Writer)";
			xStorable.storeAsURL(sSaveUrl, propertyValue);
			
			/*
			 * com.sun.star.util.XCloseable xCloseable = (com.sun.star.util.XCloseable)
			 * UnoRuntime.queryInterface(com.sun.star.util.XCloseable.class, oDocToStore );
			 * 
			 * if (xCloseable != null ) { xCloseable.close(false); } else {
			 * com.sun.star.lang.XComponent xComp = (com.sun.star.lang.XComponent)
			 * UnoRuntime.queryInterface( com.sun.star.lang.XComponent.class, oDocToStore );
			 * xComp.dispose(); }
			 */
			// Load a Writer document, which will be automaticly displayed
			com.sun.star.lang.XComponent xComp =
				xCompLoader.loadComponentFromURL(sSaveUrl, "_blank", 0,
					new com.sun.star.beans.PropertyValue[0]);
			
			XTextDocument xDoc =
				(com.sun.star.text.XTextDocument) UnoRuntime.queryInterface(
					com.sun.star.text.XTextDocument.class, xComp);
			return xDoc;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	private void useConnection() throws java.lang.Exception{
		
		// xRemoteServiceManager = getRemoteServiceManager(unoUrl);
		/*
		 * Object desktop = xRemoteServiceManager.createInstanceWithContext(
		 * "com.sun.star.frame.Desktop", xContext);
		 */
		/*
		 * XComponentLoader xComponentLoader = (XComponentLoader)UnoRuntime.queryInterface(
		 * XComponentLoader.class, desktop);
		 */
	}
	
	private String makeURL(String f) throws IOException{
		java.io.File file = new java.io.File(f);
		StringBuffer sUrl = new StringBuffer("file:///");
		sUrl.append(file.getCanonicalPath().replace('\\', '/'));
		return sUrl.toString();
	}
	
	protected XMultiComponentFactory getRemoteServiceManager(String unoUrl)
		throws java.lang.Exception{
		
		if (xContext == null) {
			
			// First step: create local component
			// context, get local service manager
			// and ask it to create a UnoUrlResolver
			// object with an XUnoUrlResolver interface
			
			XComponentContext xLocalContext =
				com.sun.star.comp.helper.Bootstrap.createInitialComponentContext(null);
			
			XMultiComponentFactory xLocalServiceManager = xLocalContext.getServiceManager();
			
			Object urlResolver =
				xLocalServiceManager.createInstanceWithContext(
					"com.sun.star.bridge.UnoUrlResolver", xLocalContext);
			
			// query XUnoUrlResolver interface from
			// urlResolver object
			
			XUnoUrlResolver xUnoUrlResolver =
				(XUnoUrlResolver) UnoRuntime.queryInterface(XUnoUrlResolver.class, urlResolver);
			
			// Second step: use xUrlResolver interface
			// to import the remote
			// StarOffice.ServiceManager, retrieve its
			// property DefaultContext and get the
			// remote service manager
			
			Object initialObject = xUnoUrlResolver.resolve(unoUrl);
			
			XPropertySet xPropertySet =
				(XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, initialObject);
			
			Object context = xPropertySet.getPropertyValue("DefaultContext");
			
			xContext =
				(XComponentContext) UnoRuntime.queryInterface(XComponentContext.class, context);
		}
		
		return xContext.getServiceManager();
		
	}
}
