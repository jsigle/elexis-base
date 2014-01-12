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

package ch.rgw.oowrapper;

import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.elexis.Hub;
import ch.elexis.data.Brief;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.ReplaceCallback;
import ch.elexis.util.Log;
import ch.rgw.oowrapper.OOPrinter.MyXPrintJobListener;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;

import com.sun.star.awt.FontWeight;
import com.sun.star.awt.Size;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.comp.beans.NoConnectionException;
import com.sun.star.comp.beans.OOoBean;
import com.sun.star.container.XIndexAccess;
import com.sun.star.drawing.LineStyle;
import com.sun.star.drawing.TextHorizontalAdjust;
import com.sun.star.drawing.TextVerticalAdjust;
import com.sun.star.drawing.XDrawPageSupplier;
import com.sun.star.drawing.XShape;
import com.sun.star.drawing.XShapes;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.style.ParagraphAdjust;
import com.sun.star.text.HoriOrientation;
import com.sun.star.text.RelOrientation;
import com.sun.star.text.TableColumnSeparator;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.text.VertOrientation;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;
import com.sun.star.util.URL;
import com.sun.star.util.XReplaceable;
import com.sun.star.util.XSearchDescriptor;
import com.sun.star.util.XSearchable;
import com.sun.star.view.PaperFormat;
import com.sun.star.view.PrintableState;
import com.sun.star.view.XPrintable;

public class OOText implements ITextPlugin {
	OOoBean bean = null;
	private final Log log = Log.get("OOText");
	private SaveHandler saveHandler;
	private ICallback textHandler;
	private String font;
	private float hi;
	private int stil;
	private boolean bSaveOnFocusLost;
	public static final String MIMETYPE_OO2 = "application/vnd.oasis.opendocument.text";
	
	public OOText(){
		System.setProperty("com.sun.star.officebean.Options", "-norestore");
		// Finding OpenOffice:
		// If there is a directory ooo within the elexis directory, we use this one.
		// if not, we look for the P_OOBASEDIR preference. If it is not set, we try the current
		// directory (and almost sure will fail)
		File base = new File(Hub.getBasePath());
		File fDef = new File(base.getParentFile().getParent() + "/ooo/program");
		String defaultbase;
		if (fDef.exists()) {
			defaultbase = fDef.getAbsolutePath();
		} else {
			defaultbase = Hub.localCfg.get(PreferenceConstants.P_OOBASEDIR, ".");
		}
		System.setProperty("openoffice.path.name", defaultbase);
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see ch.rgw.oowrapper.ITextPlugin#createContainer(org.eclipse.swt.widgets.Composite)
	 */
	public Composite createContainer(final Composite parent, final ICallback handler){
		parent.setLayout(new FillLayout());
		Composite awtContainer = new Composite(parent, SWT.EMBEDDED);
		awtContainer.setLayout(new FillLayout());
		java.awt.Frame frame = SWT_AWT.new_Frame(awtContainer);
		frame.setLayout(new BorderLayout());
		textHandler = handler;
		if (bean == null) {
			if (reconnect() == false) {
				return null;
			}
		}
		frame.add(bean, BorderLayout.CENTER);
		String url = "private:factory/swriter";
		try {
			bean.loadFromURL(url, null);
			showMenu(Hub.localCfg.get(OOPrefs.SHOW_MENU, true));
			showToolbar(Hub.localCfg.get(OOPrefs.SHOW_TOOLBAR, true));
			bean.aquireSystemWindow();
			saveHandler = new SaveHandler();
			bean.getFrame().registerDispatchProviderInterceptor(new Interceptor());
			
		} catch (NoConnectionException nce) {
			bean = null;
			return createContainer(parent, handler);
		} catch (Exception ex) {
			bean = null;
			awtContainer.dispose();
			Label label = new Label(parent, SWT.NONE);
			label.setText("Konnte keine Verbindung mit OpenOffice herstellen");
			ExHandler.handle(ex);
		}
		return awtContainer;
	}
	
	public void setFocus(){
		bean.requestFocus();
	}
	
	private boolean reconnect(){
		bean = new OOoBean();
		// bean.setOOoCheckCycle(100);
		bean.setOOoCallTimeOut(60000);
		
		try {
			bean.getOOoDesktop();
			bean.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(final FocusEvent e){
					if (bSaveOnFocusLost) {
						textHandler.save();
						/*
						 * Desk.theDisplay.asyncExec(new Runnable(){ public void run() {
						 * 
						 * } });
						 */
					}
				}
				
			});
		} catch (NoConnectionException ex) {
			log.log("Cannot connect OOoBean ", Log.ERRORS);
			ExHandler.handle(ex);
			return false;
		} catch (Throwable t) {
			ExHandler.handle(t);
			return false;
		}
		return true;
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see ch.rgw.oowrapper.ITextPlugin#dispose()
	 */
	public void dispose(){
		// Ausserst unbefriedigend. Wieso bleiben manchmal diese Textleichen zurück?
		if (bean != null) {
			try {
				// XDesktop desktop=bean.getOOoDesktop();
				bean.getDocument().setModified(false);
				bean.clear();
				// bean.stopOOoConnection(); // Dies scheint zu akkumulation von Docs zu führen
				bean = null;
				/*
				 * if(desktop!=null){ desktop.terminate(); // Ich weiss nicht, ob das notwendig ist
				 * }
				 */
			} catch (Throwable e) {
				ExHandler.handle(e);
			}
		}
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see ch.rgw.oowrapper.ITextPlugin#showMenu(boolean)
	 */
	public void showMenu(final boolean b){
		try {
			com.sun.star.beans.XPropertySet xPropSet =
				(com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
					com.sun.star.beans.XPropertySet.class, bean.getFrame());
			com.sun.star.frame.XLayoutManager xLayoutManager =
				(com.sun.star.frame.XLayoutManager) UnoRuntime.queryInterface(
					com.sun.star.frame.XLayoutManager.class,
					xPropSet.getPropertyValue("LayoutManager"));
			if (b == true) {
				xLayoutManager.showElement("private:resource/menubar/menubar");
			} else {
				xLayoutManager.hideElement("private:resource/menubar/menubar");
			}
		} catch (Throwable ex) {
			ExHandler.handle(ex);
		}
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see ch.rgw.oowrapper.ITextPlugin#showToolbar(boolean)
	 */
	public void showToolbar(final boolean b){
		try {
			com.sun.star.beans.XPropertySet xPropSet =
				(com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
					com.sun.star.beans.XPropertySet.class, bean.getFrame());
			com.sun.star.frame.XLayoutManager xLayoutManager =
				(com.sun.star.frame.XLayoutManager) UnoRuntime.queryInterface(
					com.sun.star.frame.XLayoutManager.class,
					xPropSet.getPropertyValue("LayoutManager"));
			if (b == true) {
				xLayoutManager.showElement("private:resource/menubar/toolbar");
			} else {
				xLayoutManager.hideElement("private:resource/menubar/toolbar");
			}
		} catch (Throwable ex) {
			ExHandler.handle(ex);
		}
	}
	
	private class Interceptor implements XDispatchProviderInterceptor {
		XDispatchProvider master, slave;
		
		public XDispatchProvider getSlaveDispatchProvider(){
			return slave;
		}
		
		public void setSlaveDispatchProvider(final XDispatchProvider arg0){
			slave = arg0;
		}
		
		public XDispatchProvider getMasterDispatchProvider(){
			return master;
		}
		
		public void setMasterDispatchProvider(final XDispatchProvider arg0){
			master = arg0;
		}
		
		public XDispatch queryDispatch(final URL arg0, final String arg1, final int arg2){
			// log.log("queryDispatch; "+arg0.Complete,Log.DEBUGMSG);
			if (arg0.Complete.equals(".uno:Save")) {
				// System.out.println("--"+arg0.Main+"\n--"+arg0.Arguments+"\n--"+arg0.Name);
				return saveHandler;
			}
			if (arg0.Complete.equals(".uno:Close")) {
				try {
					bean.getDocument().setModified(false);
					bean.clear();
					bean.stopOOoConnection();
				} catch (Exception ex) {
					ExHandler.handle(ex);
				}
			}
			return slave.queryDispatch(arg0, arg1, arg2);
		}
		
		public XDispatch[] queryDispatches(final DispatchDescriptor[] arg0){
			log.log("queryDispatches", Log.DEBUGMSG);
			/*
			 * for(DispatchDescriptor d0:arg0){ System.out.println(d0.FeatureURL.Complete); }
			 */
			XDispatch[] ret = new XDispatch[0];
			return ret;
		}
		
	}
	
	private class SaveHandler implements XDispatch {
		
		LinkedList<XStatusListener> listeners = new LinkedList<XStatusListener>();
		FeatureStateEvent event;
		
		SaveHandler(){
			event = new FeatureStateEvent();
			event.FeatureDescriptor = "Save";
			event.IsEnabled = true;
			event.Requery = false;
			event.State = new Boolean(false);
		}
		
		public void dispatch(final URL arg0, final PropertyValue[] arg1){
			/*
			 * System.out.println("dispatch: "+arg0.Complete); if(arg1!=null && arg1.length>0){
			 * System.out.println(arg1[0].Name+": "+arg1[0].State); }
			 */
			textHandler.save();
			
			try {
				bean.getDocument().setModified(false);
			} catch (Exception ex) {
				ExHandler.handle(ex);
			}
		}
		
		public void addStatusListener(final XStatusListener arg0, final URL arg1){
			log.log("addstatuslistener " + arg1.Complete, Log.DEBUGMSG);
			listeners.add(arg0);
			event.FeatureURL = arg1;
			arg0.statusChanged(event);
		}
		
		public void removeStatusListener(final XStatusListener arg0, final URL arg1){
			listeners.remove(arg0);
		}
		
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see ch.rgw.oowrapper.ITextPlugin#createEmptyLetter()
	 */
	public boolean createEmptyDocument(){
		String url = "private:factory/swriter";
		if (bean == null) {
			if (reconnect() == false) {
				return false;
			}
		}
		try {
			bean.clear();
			bean.loadFromURL(url, null);
			bean.getDocument().setModified(false);
			// bean.setEnabled(true);
			return true;
		} catch (NoConnectionException nce) {
			bean = null;
			return createEmptyDocument();
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
		
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see ch.rgw.oowrapper.ITextPlugin#loadFromByteArray(byte[])
	 */
	public boolean loadFromByteArray(final byte[] bs, final boolean asTemplate){
		if (bs == null) {
			log.log("Null-Array zum speichern!", Log.ERRORS);
			return false;
		}
		if (bean == null) {
			if (reconnect() == false) {
				return false;
			}
		}
		try {
			PropertyValue[] props = new PropertyValue[1];
			props[0] = new PropertyValue();
			props[0].Name = "AsTemplate";
			props[0].Value = new Boolean(asTemplate);
			
			bean.loadFromByteArray(bs, props);
			return true;
		} catch (NoConnectionException nce) {
			bean = null;
			return loadFromByteArray(bs, asTemplate);
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see ch.rgw.oowrapper.ITextPlugin#findOrReplace(java.lang.String,
	 * ch.rgw.oowrapper.OOText.ReplaceCallback)
	 */
	public boolean findOrReplace(final String pattern, final ReplaceCallback cb){
		if (bean == null) {
			if (reconnect() == false) {
				return false;
			}
		}
		try {
			XReplaceable xReplaceable =
				(XReplaceable) UnoRuntime.queryInterface(XReplaceable.class, bean.getDocument());
			XSearchDescriptor xSearch = xReplaceable.createSearchDescriptor();
			xSearch.setPropertyValue("SearchRegularExpression", new Boolean(true));
			xSearch.setSearchString(pattern);
			XIndexAccess res = xReplaceable.findAll(xSearch);
			int count = res.getCount();
			for (int i = 0; i < count; i++) {
				Object found = res.getByIndex(i);
				XTextRange text = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, found);
				Object out = cb.replace(text.getString());
				if (out instanceof String[][]) {
					String[][] tbl = (String[][]) out;
					XTextDocument myDoc =
						(XTextDocument) UnoRuntime.queryInterface(
							com.sun.star.text.XTextDocument.class, bean.getDocument());
					com.sun.star.text.XTextTable xTT = null;
					com.sun.star.lang.XMultiServiceFactory xDocMSF = // bean.getMultiServiceFactory();
						(com.sun.star.lang.XMultiServiceFactory) UnoRuntime.queryInterface(
							com.sun.star.lang.XMultiServiceFactory.class, myDoc);
					Object oInt = xDocMSF.createInstance("com.sun.star.text.TextTable");
					xTT =
						(com.sun.star.text.XTextTable) UnoRuntime.queryInterface(
							com.sun.star.text.XTextTable.class, oInt);
					xTT.initialize(tbl.length, tbl[0].length);
					
					XPropertySet xTableProps =
						(XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTT);
					com.sun.star.text.XText xText = myDoc.getText();
					com.sun.star.text.XTextCursor xTCursor = null;
					xTCursor = xText.createTextCursorByRange(text);
					setFormat(xTCursor);
					xText.insertTextContent(xTCursor, xTT, false);
					text.setString("");
					for (int row = 0; row < tbl.length; row++) {
						String[] zeile = tbl[row];
						String adr = Integer.toString(row + 1);
						for (int col = 0; col < zeile.length; col++) {
							StringBuilder sb = new StringBuilder();
							sb.append((char) ('A' + col));
							sb.append(adr);
							insertIntoCell(sb.toString(), zeile[col], xTT);
						}
					}
				} else {
					text.setString(out == null ? "" : ((String) out).replaceAll("\r", ""));
				}
			}
			return true;
		} catch (NoConnectionException nce) {
			bean = null;
			return findOrReplace(pattern, cb);
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see ch.rgw.oowrapper.ITextPlugin#storeToByteArray()
	 */
	public byte[] storeToByteArray(){
		if (bean == null) {
			if (reconnect() == false) {
				return null;
			}
		}
		try {
			// bean.storeToStream(new FileOutputStream("c:/test.odt"),null);
			if (!bean.getDocument().isReadonly()) {
				
				byte[] ret = bean.storeToByteArray(null, null);
				if (ret != null) {
					bean.getDocument().setModified(false);
				}
				return ret;
			}
			
			return null;
		} catch (NoConnectionException nce) {
			bean = null;
			return storeToByteArray();
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return null;
		}
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see ch.rgw.oowrapper.ITextPlugin#clear()
	 */
	public boolean clear(){
		if (bean == null) {
			if (reconnect() == false) {
				return false;
			}
		}
		// bean.clear();
		if (false) {
			try {
				bean.getDocument().setModified(false);
				bean.getDocument().dispose();
			} catch (NoConnectionException e) {
				ExHandler.handle(e);
			} catch (PropertyVetoException pe) {
				ExHandler.handle(pe);
			}
			
		}
		return true;
	}
	
	public boolean loadFromStream(final InputStream is, final boolean asTemplate){
		if (bean == null) {
			if (reconnect() == false) {
				return false;
			}
		}
		try {
			// bean.clear();
			PropertyValue[] props = new PropertyValue[1];
			props[0] = new PropertyValue();
			props[0].Name = "AsTemplate";
			props[0].Value = new Boolean(asTemplate);
			bean.loadFromStream(is, props);
			return true;
		} catch (NoConnectionException nce) {
			bean = null;
			return loadFromStream(is, asTemplate);
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
	}
	
	public void setInitializationData(final IConfigurationElement config,
		final String propertyName, final Object data) throws CoreException{
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	public boolean print(final String printer, final String tray, final boolean waitUntilFinished){
		if (bean == null) {
			if (reconnect() == false) {
				return false;
			}
		}
		try {
			XPrintable xPrintable =
				(XPrintable) UnoRuntime.queryInterface(com.sun.star.view.XPrintable.class,
					bean.getDocument());
			
			if (!StringTool.isNothing(printer)) {
				// set printer
				PropertyValue[] printerDesc = new PropertyValue[1];
				printerDesc[0] = new PropertyValue();
				printerDesc[0].Name = "Name";
				printerDesc[0].Value = printer;
				xPrintable.setPrinter(printerDesc);
			}
			
			// set pages to be printed
			PropertyValue[] printOpts = new PropertyValue[1];
			printOpts[0] = new PropertyValue();
			printOpts[0].Name = "Pages";
			printOpts[0].Value = "1-";
			
			if (!StringTool.isNothing(tray)) {
				XTextDocument myDoc =
					(XTextDocument) UnoRuntime.queryInterface(
						com.sun.star.text.XTextDocument.class, bean.getDocument());
				if (!OOPrinter.setPrinterTray(myDoc, tray)) {
					return false;
				}
			}
			
			com.sun.star.view.XPrintJobBroadcaster selection =
				(com.sun.star.view.XPrintJobBroadcaster) UnoRuntime.queryInterface(
					com.sun.star.view.XPrintJobBroadcaster.class, xPrintable);
			MyXPrintJobListener myXPrintJobListener = new MyXPrintJobListener();
			selection.addPrintJobListener(myXPrintJobListener);
			
			xPrintable.print(printOpts);
			
			long timeout = System.currentTimeMillis();
			while ((myXPrintJobListener.getStatus() == null)
				|| (myXPrintJobListener.getStatus() == PrintableState.JOB_STARTED)) {
				Thread.sleep(100);
				long to = System.currentTimeMillis();
				if ((to - timeout) > 10000) {
					break;
				}
			}
			
			return true;
		} catch (NoConnectionException nce) {
			bean = null;
			return print(printer, tray, waitUntilFinished);
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
	}
	
	/**
	 * Eine Tabelle einfügen.
	 * 
	 * @param properties
	 *            Eigenschafen der Tabelle, mit | verknüpft
	 * @param marke
	 *            Ein Text als Platzhalter innerhalb des Texts. Die Tabelle wird anstelle
	 * @param columnSizes
	 *            ein Array mit der relativen Breite der Spalten (muss zusammen 100 ergeben) dieses
	 *            Platzhalters gesetzt
	 * @param values
	 *            Inhalt der Tabelle
	 * @return true bei erfolg
	 */
	
	public boolean insertTable(final String marke, final int properties, final String[][] values,
		final int[] columnSizes){
		if (bean == null) {
			if (reconnect() == false) {
				return false;
			}
		}
		// createEmptyLetter();
		int w = values[0].length;
		int h = values.length;
		int offset = 0;
		if ((properties & ITextPlugin.FIRST_ROW_IS_HEADER) == 0) {
			offset = 1;
		}
		try {
			XTextDocument myDoc =
				(XTextDocument) UnoRuntime.queryInterface(com.sun.star.text.XTextDocument.class,
					bean.getDocument());
			com.sun.star.lang.XMultiServiceFactory xDocMSF = // bean.getMultiServiceFactory();
				(com.sun.star.lang.XMultiServiceFactory) UnoRuntime.queryInterface(
					com.sun.star.lang.XMultiServiceFactory.class, myDoc);
			
			com.sun.star.text.XTextTable xTT = null;
			
			Object oInt = xDocMSF.createInstance("com.sun.star.text.TextTable");
			xTT =
				(com.sun.star.text.XTextTable) UnoRuntime.queryInterface(
					com.sun.star.text.XTextTable.class, oInt);
			xTT.initialize(h + offset, w);
			
			XPropertySet xTableProps =
				(XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTT);
			xTableProps.setPropertyValue("RepeatHeadline", new Boolean(offset == 0));
			
			// XCell cell=xTT.getCellByName("B2");
			// XPropertySet xCellProps=(XPropertySet)UnoRuntime.queryInterface(
			// XPropertySet.class,cell);
			
			// getting the text object
			com.sun.star.text.XText xText = myDoc.getText();
			XSearchable xSearchable =
				(XSearchable) UnoRuntime.queryInterface(XSearchable.class, myDoc);
			XSearchDescriptor xSearch = xSearchable.createSearchDescriptor();
			// xSearch.setPropertyValue("SearchRegularExpression",new Boolean(true));
			xSearch.setSearchString(marke);
			XInterface found = (XInterface) xSearchable.findFirst(xSearch);
			XTextRange textRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, found);
			com.sun.star.text.XTextCursor xTCursor = null;
			if (textRange == null) {
				xTCursor = xText.createTextCursor();
			} else {
				xTCursor = xText.createTextCursorByRange(textRange);
			}
			setFormat(xTCursor);
			xText.insertTextContent(xTCursor, xTT, false);
			textRange.setString("");
			for (int row = 0; row < values.length; row++) {
				String[] zeile = values[row];
				String adr = Integer.toString(row + 1 + offset);
				for (int col = 0; col < zeile.length; col++) {
					StringBuilder sb = new StringBuilder();
					sb.append((char) ('A' + col));
					sb.append(adr);
					insertIntoCell(sb.toString(), zeile[col], xTT);
				}
			}
			if (columnSizes != null) {
				int sum = (Short) xTableProps.getPropertyValue("TableColumnRelativeSum");
				TableColumnSeparator[] sep =
					(TableColumnSeparator[]) xTableProps.getPropertyValue("TableColumnSeparators"); // */new
																									// TableColumnSeparator[columnSizes.length];
				int percent = sum / 100;
				int total = 0;
				for (int i = 0; i < columnSizes.length - 1; i++) {
					total += columnSizes[i] * percent;
					sep[i].Position = (short) total;
				}
				xTableProps.setPropertyValue("TableColumnSeparators", sep);
				
			}
			
		} catch (NoConnectionException e) {
			bean = null;
			return insertTable(marke, properties, values, columnSizes);
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		return true;
	}
	
	static void insertIntoCell(final String CellName, final String theText,
		final com.sun.star.text.XTextTable xTTbl){
		
		com.sun.star.text.XText xTableText =
			(com.sun.star.text.XText) UnoRuntime.queryInterface(com.sun.star.text.XText.class,
				xTTbl.getCellByName(CellName));
		
		// create a cursor object
		/*
		 * com.sun.star.text.XTextCursor xTC = xTableText.createTextCursor();
		 * 
		 * 
		 * com.sun.star.beans.XPropertySet xTPS = (com.sun.star.beans.XPropertySet)
		 * UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xTC);
		 * 
		 * try { xTPS.setPropertyValue("CharColor",new Integer(16777215)); } catch (Exception e) {
		 * System.err.println(" Exception " + e); e.printStackTrace(System.err); }
		 */
		// inserting some Text
		if ((xTableText != null) && !StringTool.isNothing(theText)) {
			xTableText.setString(theText);
		}
		
	}
	
	public ch.elexis.text.ITextPlugin.PageFormat getFormat(){
		if (bean == null) {
			if (reconnect() == false) {
				return null;
			}
		}
		try {
			PropertyValue[] props = bean.getDocument().getPrinter();
			for (PropertyValue p : props) {
				if (p.Name.equals("PaperFormat")) {
					com.sun.star.view.PaperFormat pf = (com.sun.star.view.PaperFormat) p.Value;
					switch (pf.getValue()) {
					case PaperFormat.A4_value:
						return PageFormat.A4;
					case PaperFormat.A5_value:
						return PageFormat.A5;
					default:
						return PageFormat.USER;
					}
				}
			}
		} catch (NoConnectionException e) {
			bean = null;
			e.printStackTrace();
			return getFormat();
		}
		return PageFormat.USER;
	}
	
	public void setFormat(final ch.elexis.text.ITextPlugin.PageFormat f){
		if (bean == null) {
			if (reconnect() == false) {
				return;
			}
		}
		try {
			PropertyValue[] props = bean.getDocument().getPrinter();
			
			for (PropertyValue p : props) {
				if (p.Name.equals("PaperFormat")) {
					switch (f) {
					case A4:
						p.Value = PaperFormat.A4;
						break;
					case A5:
						p.Value = PaperFormat.A5;
						break;
					default:
						p.Value = PaperFormat.USER;
					}
				}
			}
		} catch (NoConnectionException e) {
			bean = null;
			e.printStackTrace();
			getFormat();
		}
		
	}
	
	public Object insertTextAt(final int x, final int y, final int w, final int h,
		final String text, final int adjust){
		
		// Integer ZOrder = new Integer( 1 );
		try {
			XTextDocument myDoc =
				(XTextDocument) UnoRuntime.queryInterface(com.sun.star.text.XTextDocument.class,
					bean.getDocument());
			com.sun.star.lang.XMultiServiceFactory documentFactory = // bean.getMultiServiceFactory();
				(com.sun.star.lang.XMultiServiceFactory) UnoRuntime.queryInterface(
					com.sun.star.lang.XMultiServiceFactory.class, myDoc);
			
			Object frame = documentFactory.createInstance("com.sun.star.text.TextFrame");
			
			XText docText = myDoc.getText();
			XTextFrame xFrame = (XTextFrame) UnoRuntime.queryInterface(XTextFrame.class, frame);
			
			XShape xWriterShape = (XShape) UnoRuntime.queryInterface(XShape.class, xFrame);
			
			xWriterShape.setSize(new Size(w * 100, h * 100));
			// xWriterShape.setPosition(new Point(x*100,y*100));
			
			/*
			 * XTextContent xTextContentShape = ( XTextContent ) UnoRuntime.queryInterface(
			 * XTextContent.class, writerShape );
			 * 
			 * // does not support XFastPropertySet XPropertySet xTextContentPropertySet = (
			 * XPropertySet ) UnoRuntime.queryInterface( XPropertySet.class, xTextContentShape );
			 * xTextContentPropertySet.setPropertyValue( "FrameStyleName", "FrameStyle" );
			 * xTextContentPropertySet.setPropertyValue( "FrameIsAutomaticHeight", Boolean.TRUE );
			 * xTextContentPropertySet.setPropertyValue( "ZOrder", ZOrder );
			 * xTextContentPropertySet.setPropertyValue( "IsFollowingTextFlow", Boolean.TRUE );
			 * xTextContentPropertySet.setPropertyValue( "BackColor", new Integer( 0xffffffff ) );
			 * //$NON-NLS-1$ xTextContentPropertySet.setPropertyValue( "BackColorTransparency", new
			 * Short( ( short ) 100 ) ); //$NON-NLS-1$
			 */
			
			XPropertySet xFrameProps =
				(XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xFrame);
			
			// Setting the vertical position
			xFrameProps.setPropertyValue("AnchorPageNo", new Short((short) 1));
			xFrameProps.setPropertyValue("VertOrientRelation", RelOrientation.PAGE_FRAME);
			xFrameProps.setPropertyValue("AnchorType", TextContentAnchorType.AT_PAGE);
			xFrameProps.setPropertyValue("HoriOrient", HoriOrientation.NONE);
			xFrameProps.setPropertyValue("VertOrient", VertOrientation.NONE);
			xFrameProps.setPropertyValue("HoriOrientPosition", x * 100);
			xFrameProps.setPropertyValue("VertOrientPosition", y * 100);
			
			XTextCursor docCursor = docText.createTextCursor();
			docCursor.gotoStart(false);
			// docText.insertControlCharacter(docCursor,ControlCharacter.PARAGRAPH_BREAK,false);
			docText.insertTextContent(docCursor, xFrame, false);
			
			// get the XText from the shape
			
			// XText xShapeText = ( XText ) UnoRuntime.queryInterface( XText.class, writerShape );
			
			XText xFrameText = xFrame.getText();
			XTextCursor xtc = xFrameText.createTextCursor();
			com.sun.star.beans.XPropertySet charProps = setFormat(xtc);
			ParagraphAdjust paradj;
			switch (adjust) {
			case SWT.LEFT:
				paradj = ParagraphAdjust.LEFT;
				break;
			case SWT.RIGHT:
				paradj = ParagraphAdjust.RIGHT;
				break;
			default:
				paradj = ParagraphAdjust.CENTER;
			}
			
			charProps.setPropertyValue("ParaAdjust", paradj);
			xFrameText.insertString(xtc, text, false);
			
			return xtc;
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
		
	}
	
	/*
	 * The same as insertTextAt above. Uses TextShape instead of TextFrame. Additionally, allow
	 * vertical alignment. (That's possible with TextShape.)
	 */
	public Object insertTextAt(final int x, final int y, final int w, final int h,
		final String text, final int adjust, final int verticalAdjust){
		
		try {
			XTextDocument myDoc =
				(XTextDocument) UnoRuntime.queryInterface(com.sun.star.text.XTextDocument.class,
					bean.getDocument());
			com.sun.star.lang.XMultiServiceFactory documentFactory = // bean.getMultiServiceFactory();
				(com.sun.star.lang.XMultiServiceFactory) UnoRuntime.queryInterface(
					com.sun.star.lang.XMultiServiceFactory.class, myDoc);
			
			Object textShape = documentFactory.createInstance("com.sun.star.drawing.TextShape");
			
			XShape xShape = (XShape) UnoRuntime.queryInterface(XShape.class, textShape);
			
			// insert TextShape into document
			XDrawPageSupplier xDrawPageSupplier =
				(XDrawPageSupplier) UnoRuntime.queryInterface(XDrawPageSupplier.class, myDoc);
			XShapes xShapes =
				(XShapes) UnoRuntime.queryInterface(XShapes.class, xDrawPageSupplier.getDrawPage());
			xShapes.add(xShape);
			
			// set position
			xShape.setSize(new Size(w * 100, h * 100));
			
			XPropertySet xShapeProps =
				(XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xShape);
			
			// horizontal adjustmet
			ParagraphAdjust paradj;
			switch (adjust) {
			case SWT.LEFT:
				paradj = ParagraphAdjust.LEFT;
				break;
			case SWT.RIGHT:
				paradj = ParagraphAdjust.RIGHT;
				break;
			default:
				paradj = ParagraphAdjust.CENTER;
			}
			xShapeProps.setPropertyValue("ParaAdjust", paradj);
			
			// horizontal adjustment
			TextHorizontalAdjust textHorizontalAdjust;
			switch (adjust) {
			case SWT.LEFT:
				textHorizontalAdjust = TextHorizontalAdjust.LEFT;
				break;
			case SWT.CENTER:
				textHorizontalAdjust = TextHorizontalAdjust.CENTER;
				break;
			case SWT.RIGHT:
				textHorizontalAdjust = TextHorizontalAdjust.RIGHT;
				break;
			default:
				textHorizontalAdjust = TextHorizontalAdjust.CENTER;
			}
			xShapeProps.setPropertyValue("TextHorizontalAdjust", textHorizontalAdjust);
			
			// vertical adjustment
			TextVerticalAdjust textVerticalAdjust;
			switch (verticalAdjust) {
			case SWT.TOP:
				textVerticalAdjust = TextVerticalAdjust.TOP;
				break;
			case SWT.CENTER:
				textVerticalAdjust = TextVerticalAdjust.CENTER;
				break;
			case SWT.BOTTOM:
				textVerticalAdjust = TextVerticalAdjust.BOTTOM;
				break;
			default:
				textVerticalAdjust = TextVerticalAdjust.TOP;
			}
			xShapeProps.setPropertyValue("TextVerticalAdjust", textVerticalAdjust);
			
			// Setting the vertical position
			xShapeProps.setPropertyValue("AnchorPageNo", new Short((short) 1));
			xShapeProps.setPropertyValue("VertOrientRelation", RelOrientation.PAGE_FRAME);
			xShapeProps.setPropertyValue("AnchorType", TextContentAnchorType.AT_PAGE);
			xShapeProps.setPropertyValue("HoriOrient", HoriOrientation.NONE);
			xShapeProps.setPropertyValue("VertOrient", VertOrientation.NONE);
			xShapeProps.setPropertyValue("HoriOrientPosition", x * 100);
			xShapeProps.setPropertyValue("VertOrientPosition", y * 100);
			
			// TODO DEBUG: line
			xShapeProps.setPropertyValue("LineStyle", LineStyle.SOLID);
			xShapeProps.setPropertyValue("LineWidth", 10);
			
			// insert text into the textShape
			XText xText = (XText) UnoRuntime.queryInterface(XText.class, xShape);
			XTextCursor xTextCursor = xText.createTextCursor();
			// set font
			setFormat(xTextCursor);
			xTextCursor.gotoStart(false);
			XTextRange xTextRange =
				(XTextRange) UnoRuntime.queryInterface(XTextRange.class, xTextCursor);
			xTextRange.setString(text);
			
			return null;
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
	}
	
	private com.sun.star.beans.XPropertySet setFormat(final XTextCursor xtc)
		throws UnknownPropertyException, PropertyVetoException, IllegalArgumentException,
		WrappedTargetException{
		com.sun.star.beans.XPropertySet charProps =
			(com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
				com.sun.star.beans.XPropertySet.class, xtc);
		if (font != null) {
			charProps.setPropertyValue("CharFontName", font);
			charProps.setPropertyValue("CharHeight", new Float(hi));
			switch (stil) {
			case SWT.MIN:
				charProps.setPropertyValue("CharWeight", 15f /* FontWeight.ULTRALIGHT */);
				break;
			case SWT.NORMAL:
				charProps.setPropertyValue("CharWeight", FontWeight.LIGHT);
				break;
			case SWT.BOLD:
				charProps.setPropertyValue("CharWeight", FontWeight.BOLD);
				break;
			}
		}
		return charProps;
	}
	
	public Object insertText(final String marke, final String text, final int adjust){
		try {
			XTextDocument myDoc =
				(XTextDocument) UnoRuntime.queryInterface(com.sun.star.text.XTextDocument.class,
					bean.getDocument());
			/*
			 * com.sun.star.lang.XMultiServiceFactory documentFactory =
			 * //bean.getMultiServiceFactory(); (com.sun.star.lang.XMultiServiceFactory)
			 * UnoRuntime.queryInterface( com.sun.star.lang.XMultiServiceFactory.class, myDoc);
			 */
			XText docText = myDoc.getText();
			XTextCursor cursor = null;
			if (marke != null) {
				XSearchable xSearchable =
					(XSearchable) UnoRuntime.queryInterface(XSearchable.class, myDoc);
				XSearchDescriptor xSearch = xSearchable.createSearchDescriptor();
				// xSearch.setPropertyValue("SearchRegularExpression",new Boolean(true));
				xSearch.setSearchString(marke);
				XInterface found = (XInterface) xSearchable.findFirst(xSearch);
				XTextRange textRange =
					(XTextRange) UnoRuntime.queryInterface(XTextRange.class, found);
				if (textRange == null) {
					cursor = docText.createTextCursor();
					cursor.gotoEnd(false);
				} else {
					cursor = docText.createTextCursorByRange(textRange);
				}
			} else {
				cursor = docText.createTextCursor();
				cursor.gotoEnd(false);
			}
			setFormat(cursor);
			cursor.setString(text);
			cursor.collapseToEnd();
			
			return cursor;
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return null;
		}
	}
	
	public Object insertText(final Object pos, final String text, final int adjust){
		XTextCursor cursor = (XTextCursor) pos;
		
		try {
			setFormat(cursor);
		} catch (Exception e) {
			ExHandler.handle(e);
		}
		cursor.setString(text);
		cursor.collapseToEnd();
		return cursor;
	}
	
	public boolean setFont(final String name, final int style, final float size){
		font = name;
		hi = size;
		stil = style;
		return true;
	}
	
	public boolean setStyle(final int style){
		stil = style;
		return true;
	}
	
	public String getMimeType(){
		return MIMETYPE_OO2;
	}
	
	public void setSaveOnFocusLost(final boolean bSave){
		bSaveOnFocusLost = bSave;
	}
	
	@Override
	public boolean isDirectOutput(){
		// TODO Auto-generated method stub
		return false;
	}
	
	
	/**
	 * 201306250243js: Adding infrastructure to keep track of the ch.elexis.data.Brief serviced by this instance of NOAText
	 * This needs an updated version of ITextView, so this plugin probably becomes incompatible with all previous versions of Elexis.
	 * It is all required to be able to set the window activation to the *correct* of multiple text plugin windows;
	 * and to maintain tracking of last user modification time on a per text-document-window basis in statusMonitor etc. 
	 */	
	private Brief briefServicedByThis = null;
	
	@Override
	public void setBriefServicedByThis(Brief suppliedBriefServicedByThis) {
		//TODO: 20130625js: Would there be any use for / any advantage in returning the previous value of that variable instead?
		briefServicedByThis = suppliedBriefServicedByThis;
	}

	@Override
	public Brief getBriefServicedByThis() {
		return briefServicedByThis;
	}
}
