/*******************************************************************************
 * Copyright (c) 2007-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich   - initial implementation
 *    H. Marlovits - added support for placeholders inside controls
 *    
 *  $Id$
 *******************************************************************************/
package ch.elexis.noa;

import com.sun.star.uno.XComponentContext;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.bridge.UnoUrlResolver;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.beans.XPropertySet;
import com.sun.star.uno.UnoRuntime;

import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jdom.Document;
import org.osgi.framework.Bundle;

import ag.ion.bion.officelayer.application.IOfficeApplication;
import ag.ion.bion.officelayer.application.OfficeApplicationException;
import ag.ion.bion.officelayer.desktop.IDesktopService;
import ag.ion.bion.officelayer.document.DocumentDescriptor;
import ag.ion.bion.officelayer.document.DocumentException;
import ag.ion.bion.officelayer.event.ICloseEvent;
import ag.ion.bion.officelayer.event.ICloseListener;
import ag.ion.bion.officelayer.event.IEvent;
import ag.ion.bion.officelayer.form.IFormComponent;
import ag.ion.bion.officelayer.form.IFormService;
import ag.ion.bion.officelayer.internal.application.connection.LocalOfficeConnectionGhost;
import ag.ion.bion.officelayer.text.ITextDocument;
import ag.ion.bion.officelayer.text.ITextRange;
import ag.ion.bion.officelayer.text.ITextTable;
import ag.ion.bion.officelayer.text.table.ITextTablePropertyStore;
import ag.ion.bion.workbench.office.editor.core.EditorCorePlugin;
import ag.ion.noa.NOAException;
import ag.ion.noa.search.ISearchResult;
import ag.ion.noa.search.SearchDescriptor;
import ag.ion.noa.service.IServiceProvider;
import ag.ion.noa4e.ui.widgets.OfficePanel;
import ch.elexis.Hub;
import ch.elexis.noa.OOPrinter.MyXPrintJobListener;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.ReplaceCallback;
import ch.elexis.util.Log;
import ch.elexis.util.SWTHelper;
import ch.rgw.io.FileTool;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

import com.sun.star.awt.FontWeight;
import com.sun.star.awt.Size;
import com.sun.star.awt.XGraphics;
import com.sun.star.awt.XTextComponent;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.bridge.UnoUrlResolver;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.drawing.XShape;
import com.sun.star.form.FormComponentType;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.graphic.XGraphic;
import com.sun.star.graphic.XGraphicProvider;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.style.ParagraphAdjust;
import com.sun.star.text.HoriOrientation;
import com.sun.star.text.RelOrientation;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.text.VertOrientation;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextGraphicObjectsSupplier;
import com.sun.star.ucb.XFileIdentifierConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XRefreshable;
import com.sun.star.view.PrintableState;
import com.sun.star.view.XPrintable;

public class NOAText implements ITextPlugin {
	public static final String MIMETYPE_OO2 = "application/vnd.oasis.opendocument.text";
	public static LinkedList<NOAText> noas = new LinkedList<NOAText>();
	OfficePanel panel;
	ITextDocument doc;
	ICallback textHandler;
	File myFile;
	private final Log log = Log.get("NOAText");
	IOfficeApplication office;
	private String font;
	private float hi = 0;
	private int stil = -1;
	
	public NOAText(){
		System.out.println("noa loaded");
		File base = new File(Hub.getBasePath());
		File fDef = new File(base.getParentFile().getParent() + "/ooo");
		String defaultbase;
		if (fDef.exists()) {
			defaultbase = fDef.getAbsolutePath();
			Hub.localCfg.set(PreferenceConstants.P_OOBASEDIR, defaultbase);
		} else {
			defaultbase = Hub.localCfg.get(PreferenceConstants.P_OOBASEDIR, ".");
		}
		System.setProperty("openoffice.path.name", defaultbase);
	}
	
	/*
	 * We keep track on opened office windows
	 */
	private void createMe(){
		if (office == null) {
			office = EditorCorePlugin.getDefault().getManagedLocalOfficeApplication();
		}
		doc = (ITextDocument) panel.getDocument();
		if (doc != null) {
			doc.addCloseListener(new closeListener(office));
			noas.add(this);
		}
	}
	
	/*
	 * We deactivate the office application as the user closes the last office window
	 */
	private void removeMe(){
		
		try {
			if (textHandler != null) {
				textHandler.save();
				noas.remove(this);
				if (doc != null) {
					doc.setModified(false);
					doc.close();
				}
			}
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		if (noas.isEmpty()) {
			try {
				office.deactivate();
				log.log("Office deactivated", Log.INFOS);
			} catch (OfficeApplicationException e) {
				ExHandler.handle(e);
				log.log("Office deactivation failed", Log.ERRORS);
			}
		}
	}
	
	public boolean clear(){
		if (textHandler != null) {
			try {
				textHandler.save();
				doc.setModified(false);
				return true;
			} catch (DocumentException e) {
				ExHandler.handle(e);
			}
		}
		return false;
	}
	
	/**
	 * Create the OOo-Container that will appear inside the view or dialog for Text-Display. Here we
	 * use a slightly adapted OfficePanel from NOA4e (www.ubion.org)
	 */
	public Composite createContainer(final Composite parent, final ICallback handler){
		new Frame();
		panel = new OfficePanel(parent, SWT.NONE);
		panel.setBuildAlwaysNewFrames(false);
		office = EditorCorePlugin.getDefault().getManagedLocalOfficeApplication();
		return panel;
	}
	
	/**
	 * Create an empty text document. We simply use an empty template and save it immediately into a
	 * temporary file to avoid OOo's complaints when we close the Container or overwrite its
	 * contents.
	 */
	public boolean createEmptyDocument(){
		try {
			clean();
			Bundle bundle = Platform.getBundle("ch.elexis.noatext");
			Path path = new Path("rsc/empty.odt");
			InputStream is = FileLocator.openStream(bundle, path, false);
			FileOutputStream fos = new FileOutputStream(myFile);
			FileTool.copyStreams(is, fos);
			is.close();
			fos.close();
			panel.loadDocument(false, myFile.getAbsolutePath(), DocumentDescriptor.DEFAULT);
			createMe();
			return true;
			/*
			 * doc=(ITextDocument)office.getDocumentService().constructNewDocument(IDocument.WRITER,
			 * DocumentDescriptor.DEFAULT); if(doc!=null){
			 * doc.getPersistenceService().store(myFile.getAbsolutePath()); doc.close();
			 * panel.loadDocument(false, myFile.getAbsolutePath(), DocumentDescriptor.DEFAULT);
			 * doc=(ITextDocument)panel.getDocument(); return true; }
			 */

		} catch (Exception e) {
			ExHandler.handle(e);
			
		}
		return false;
	}
	
	/**
	 * Load a file from a byte array. Again, wie store it first into a temporary disk file because
	 * OOo does not like documents that have no representation on disk.
	 */
	public boolean loadFromByteArray(final byte[] bs, final boolean asTemplate){
		if (bs == null) {
			log.log("Null-Array zum speichern!", Log.ERRORS);
			return false;
		}
		try {
			clean();
			FileOutputStream fout = new FileOutputStream(myFile);
			fout.write(bs);
			fout.close();
			panel.loadDocument(false, myFile.getAbsolutePath(), DocumentDescriptor.DEFAULT);
			createMe();
			return true;
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
	}
	
	/**
	 * Load a file from an input stream. Explanations
	 * 
	 * @see loadFromByteArray()
	 */
	public boolean loadFromStream(final InputStream is, final boolean asTemplate){
		try {
			clean();
			doc =
				(ITextDocument) office.getDocumentService().loadDocument(is,
					DocumentDescriptor.DEFAULT_HIDDEN);
			if (doc != null) {
				doc.getPersistenceService().store(myFile.getAbsolutePath());
				doc.close();
				panel.loadDocument(false, myFile.getAbsolutePath(), DocumentDescriptor.DEFAULT);
				createMe();
			}
		} catch (Exception e) {
			ExHandler.handle(e);
			
		}
		return false;
	}
	
	/**
	 * Store the contents of the OOo-Frame into a byte array. We save it into a temporary disk file
	 * first to ensure OOo, that the file ist really saved. That way OOo will not complain about
	 * corrupted or lost files.
	 */
	public byte[] storeToByteArray(){
		if (doc == null) {
			return null;
		}
		try {
			doc.getPersistenceService().store(myFile.getAbsolutePath());
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
			byte[] ret = new byte[(int) myFile.length()];
			int pos = 0, len = 0;
			while (pos + (len = bis.read(ret)) != ret.length) {
				pos += len;
			}
			return ret;
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return null;
		}
	}
	
	/**
	 * Destroy the Panel with the OOo frame
	 */
	public void dispose(){
		if (doc != null) {
			doc.close();
			doc = null;
		}
		if (panel != null) {
			panel.dispose();
		}
		
	}
	
	public boolean findOrReplace(final String pattern, final ReplaceCallback cb){
		SearchDescriptor search = new SearchDescriptor(pattern);
		search.setUseRegularExpression(true);
		if (doc == null) {
			SWTHelper.showError("No doc in bill", "Fehler:",
				"Es ist keine Rechnungsvorlage definiert");
			return false;
		}
		
		
		// *** START support for replacement of placeholders inside Forms/Controls
		String cWrongNumOfArgs       = "*** Wrong number of arguments: Allowed number of arguments for this type of control: ";
		String cWrongNumOfArgs_2     = " ***";
		IFormService formService = doc.getFormService();
		IFormComponent[] formComponents;
		try {
			formComponents = formService.getFormComponents();
			for (int i = 0; i < formComponents.length; i++){	
				try	{
					IFormComponent formComponent = formComponents[i];
					// *** read control name - this may contain a replacement instruction
					XPropertySet xPSet = formComponent.getXPropertySet();
					int componentType = getFormComponentType(xPSet);
					try {
						String controlName = (String) xPSet.getPropertyValue("Name");
					} catch (UnknownPropertyException e) {
						break; // don't process if this can't be found
					} catch (WrappedTargetException e) {
						break; // don't process if this can't be found
					}
					
					// *** get the replacement specification
					String replacement = (String) xPSet.getPropertyValue("Tag");
					
					// *** do the replacement
					if (cb != null) {
						Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
						Matcher m = p.matcher(replacement);
						StringBuffer sb = new StringBuffer(replacement.length() * 4);
						while (m.find()) {
							int start = m.start();
							int end   = m.end();
							String orig = replacement.substring(start, end);
							Object replace = cb.replace(orig);
							if (replace == null) {
								m.appendReplacement(sb, "??Auswahl??");
							} else if (replace instanceof String) {
								String repl = ((String) replace).replaceAll("\\r", "\n");
								repl = repl.replaceAll("\\n\\n+", "\n");
								m.appendReplacement(sb, repl);
							} else {
								m.appendReplacement(sb, "Not a String");
							}
						}
						m.appendTail(sb);
						replacement = sb.toString();
					}
					
					// *** must save into Tag field because called repeatedly with different replacements
					xPSet.setPropertyValue("Tag", replacement);
					
					// *** split into parts
					String[] replacementParts = replacement.split("@@@");
					String replacement1 = replacementParts[0];
					String replacement2 = replacementParts.length >=2 ? replacementParts[1] : null;
					String replacement3 = replacementParts.length >=3 ? replacementParts[2] : null;
					String replacement4 = replacementParts.length >=4 ? replacementParts[3] : null;
					String replacement5 = replacementParts.length >=5 ? replacementParts[4] : null;
					
					if (StringTool.isNothing(replacement1)) replacement1 = null;
					if (StringTool.isNothing(replacement2)) replacement2 = null;
					if (StringTool.isNothing(replacement3)) replacement3 = null;
					if (StringTool.isNothing(replacement4)) replacement4 = null;
					if (StringTool.isNothing(replacement5)) replacement5 = null;
					
					//test if number of params ok,
					//if error break, show error info in tag field for debugging purposes
					String[] argumentsMapping = {
							FormComponentType.PATTERNFIELD  + ":" + 1,
							FormComponentType.FILECONTROL   + ":" + 1,
							FormComponentType.RADIOBUTTON   + ":" + 1,
							FormComponentType.CHECKBOX      + ":" + 1,
							FormComponentType.COMMANDBUTTON + ":" + 1,
							FormComponentType.FIXEDTEXT     + ":" + 1,
							FormComponentType.GROUPBOX      + ":" + 1,
							FormComponentType.IMAGEBUTTON   + ":" + 1,
							FormComponentType.IMAGECONTROL  + ":" + 1,
							FormComponentType.COMBOBOX      + ":" + 2,
							FormComponentType.LISTBOX       + ":" + 2,
							FormComponentType.DATEFIELD     + ":" + 3,
							FormComponentType.TIMEFIELD     + ":" + 3,
							FormComponentType.NUMERICFIELD  + ":" + 4,
							FormComponentType.SPINBUTTON    + ":" + 4,
							FormComponentType.CURRENCYFIELD + ":" + 4,
							FormComponentType.SCROLLBAR     + ":" + 5
					};
					for (int argi = 0; argi < argumentsMapping.length; argi++)	{
						String argMap = argumentsMapping[argi];
						int argType      = Integer.parseInt(argMap.split(":")[0]);
						int argNumOfArgs = Integer.parseInt(argMap.split(":")[1]);
						if (componentType == argType)	{
							String controlDefaultControl = (String) xPSet.getPropertyValue("DefaultControl");
							if (controlDefaultControl.equalsIgnoreCase("com.sun.star.form.control.FormattedField"))	{
								// *** special case FormattedField which is a text field
								if (replacementParts.length > argNumOfArgs) xPSet.setPropertyValue("Tag", cWrongNumOfArgs + 3 + cWrongNumOfArgs_2);
							} else	{
								// *** "normal" fields
								if (replacementParts.length > argNumOfArgs) xPSet.setPropertyValue("Tag", cWrongNumOfArgs + argNumOfArgs + cWrongNumOfArgs_2);
							}
							break;
						}
					}
					
					// if ComboBox or ListBox, then set list items if specified
					if ((componentType == FormComponentType.COMBOBOX) || (componentType == FormComponentType.LISTBOX))	{
						// *** if delimited by returns (coming from SQL-Select)
						replacement2 = replacement2.replaceAll("\\n", ";");
						if (replacement2 != null) xPSet.setPropertyValue("StringItemList",  replacement2.split(";"));
					}
					
					switch (componentType)	{
						case (FormComponentType.TEXTFIELD):
						case (FormComponentType.COMBOBOX):
						case (FormComponentType.PATTERNFIELD):
						case (FormComponentType.FILECONTROL):
							String controlDefaultControl = (String) xPSet.getPropertyValue("DefaultControl");
							if (controlDefaultControl.equalsIgnoreCase("com.sun.star.form.control.FormattedField"))	{
								// *** FormattedField
								if (isInteger(replacement1)) xPSet.setPropertyValue("EffectiveValue", new Short((short) Integer.parseInt(replacement1)));
								if (isInteger(replacement2)) xPSet.setPropertyValue("EffectiveMin",   new Short((short) Integer.parseInt(replacement2)));
								if (isInteger(replacement3)) xPSet.setPropertyValue("EffectiveMax",   new Short((short) Integer.parseInt(replacement3)));
							} else	{
								// *** simple text field
								XTextComponent xTextComponent = formComponent.getXTextComponent();
								if (replacement1 != null) xTextComponent.setText(replacement1);
							}
							break;
						case (FormComponentType.DATEFIELD):
							TimeTool timeTool = new TimeTool();
							// *** set date
							if (timeTool.set(replacement1))	{
								String yyyymmddDate = timeTool.toString(TimeTool.DATE_COMPACT);
								if (!StringTool.isNothing(yyyymmddDate)) xPSet.setPropertyValue("Date", Integer.parseInt(yyyymmddDate));
							}
							// *** set DateMin
							if (timeTool.set(replacement2))	{
								String yyyymmddDate = timeTool.toString(TimeTool.DATE_COMPACT);
								if (!StringTool.isNothing(yyyymmddDate)) xPSet.setPropertyValue("DateMin", Integer.parseInt(yyyymmddDate));
							}
							// *** set DateMax
							if (timeTool.set(replacement3))	{
								String yyyymmddDate = timeTool.toString(TimeTool.DATE_COMPACT);
								if (!StringTool.isNothing(yyyymmddDate)) xPSet.setPropertyValue("DateMax", Integer.parseInt(yyyymmddDate));
							}
							break;
						case (FormComponentType.TIMEFIELD):
							TimeTool timeTool2 = new TimeTool();
							// *** set time
							if (timeTool2.set(replacement1))	{
								String hhmmssTime = timeTool2.toString(TimeTool.TIME_FULL);
								hhmmssTime = hhmmssTime.replaceAll(":", "") + "00";
								if (!StringTool.isNothing(hhmmssTime)) xPSet.setPropertyValue("Time", Integer.parseInt(hhmmssTime));
							}
							// *** set TimeMin
							if (timeTool2.set(replacement2))	{
								String hhmmssTime = timeTool2.toString(TimeTool.TIME_FULL);
								hhmmssTime = hhmmssTime.replaceAll(":", "") + "00";
								if (!StringTool.isNothing(hhmmssTime))xPSet.setPropertyValue("TimeMin", Integer.parseInt(hhmmssTime));
							}
							// *** set TimeMax
							if (timeTool2.set(replacement3))	{
								String hhmmssTime = timeTool2.toString(TimeTool.TIME_FULL);
								hhmmssTime = hhmmssTime.replaceAll(":", "") + "00";
								if (!StringTool.isNothing(hhmmssTime)) xPSet.setPropertyValue("TimeMax", Integer.parseInt(hhmmssTime));
							}
							break;
						case (FormComponentType.NUMERICFIELD):
						case (FormComponentType.CURRENCYFIELD):
							if (isInteger(replacement1)) xPSet.setPropertyValue("Value",     new Short((short) Integer.parseInt(replacement1)));
							if (isInteger(replacement2)) xPSet.setPropertyValue("ValueMin",  new Short((short) Integer.parseInt(replacement2)));
							if (isInteger(replacement3)) xPSet.setPropertyValue("ValueMax",  new Short((short) Integer.parseInt(replacement3)));
							if (isInteger(replacement4)) xPSet.setPropertyValue("ValueStep", new Short((short) Integer.parseInt(replacement4)));
							break;
						case (FormComponentType.RADIOBUTTON):
						case (FormComponentType.CHECKBOX):
							if (isInteger(replacement1))  xPSet.setPropertyValue("State", new Short((short) Integer.parseInt(replacement1)));
							break;
						case (FormComponentType.COMMANDBUTTON):
						case (FormComponentType.FIXEDTEXT):
						case (FormComponentType.GROUPBOX):
							if (replacement1 != null) xPSet.setPropertyValue("Label", replacement1);
							break;
						case (FormComponentType.LISTBOX):
							// *** if delimited by returns (coming from SQL-Select)
							replacement1 = replacement1.replaceAll("\\n", ";");
							// *** create short[] from replacement1
							String[] splittedArgs = replacement1.split(";");
							short[] shortList = new short[splittedArgs.length];
							for (int argsi = 0; argsi < splittedArgs.length; argsi++)	{
								String argStr = splittedArgs[argsi];
								if (isInteger(argStr))	{
									short arg = (short) Integer.parseInt(argStr);
									shortList[argsi] = arg;
								}
							}
							if (replacement1 != null) xPSet.setPropertyValue("SelectedItems", shortList);
							break;
						case (FormComponentType.SPINBUTTON):
							if (isInteger(replacement3)) xPSet.setPropertyValue("SpinValueMax",  new Short((short) Integer.parseInt(replacement3)));
							if (isInteger(replacement2)) xPSet.setPropertyValue("SpinValueMin",  new Short((short) Integer.parseInt(replacement2)));
							if (isInteger(replacement4)) xPSet.setPropertyValue("SpinIncrement", new Short((short) Integer.parseInt(replacement4)));
							if (isInteger(replacement1)) xPSet.setPropertyValue("SpinValue",     new Short((short) Integer.parseInt(replacement1)));
							break;
						case (FormComponentType.SCROLLBAR):
							if (isInteger(replacement1)) xPSet.setPropertyValue("ScrollValue",    new Short((short) Integer.parseInt(replacement1)));
							if (isInteger(replacement2)) xPSet.setPropertyValue("ScrollValueMin", new Short((short) Integer.parseInt(replacement2)));
							if (isInteger(replacement3)) xPSet.setPropertyValue("ScrollValueMax", new Short((short) Integer.parseInt(replacement3)));
							if (isInteger(replacement4)) xPSet.setPropertyValue("LineIncrement",  new Short((short) Integer.parseInt(replacement4)));
							if (isInteger(replacement5)) xPSet.setPropertyValue("BlockIncrement", new Short((short) Integer.parseInt(replacement5)));
							break;
						case (FormComponentType.IMAGEBUTTON):
						case (FormComponentType.IMAGECONTROL):
							// *** doesn't work correctly... hmmmm... can anyone tell me how to get this to work???
							//     anyway: embedding into doc doesn't work in OO < 3.1
							//     so: more or less useless this way - and waiting for new OO in Elexis
 							if (replacement1 != null) xPSet.setPropertyValue("ImageURL", replacement1);
							break;
					}
				} catch (NOAException e) {
					e.printStackTrace();
				} catch (UnknownPropertyException e) {
					e.printStackTrace();
				} catch (PropertyVetoException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (WrappedTargetException e) {
					e.printStackTrace();
				} catch (Exception e)	{
					// *** catch just everything so that the proc is going on...
				}
			}
		} catch (NOAException e1) {
			e1.printStackTrace();
		} catch (Exception e1) {
			// *** catch just everything so that the proc is going on...
			e1.printStackTrace();
		}
		// *** END support for replacement of placeholders inside Forms/Controls
		
		ISearchResult searchResult = doc.getSearchService().findAll(search);
		if (!searchResult.isEmpty()) {
			ITextRange[] textRanges = searchResult.getTextRanges();
			if (cb != null) {
				for (ITextRange r : textRanges) {
					String orig = r.getXTextRange().getString();
					Object replace = cb.replace(orig);
					if (replace == null) {
						r.setText("??Auswahl??");
					} else if (replace instanceof String) {
						// String repl=((String)replace).replaceAll("\\r\\n[\\r\\n]*", "\n")
						String repl = ((String) replace).replaceAll("\\r", "\n");
						repl = repl.replaceAll("\\n\\n+", "\n");
						r.setText(repl);
					} else if (replace instanceof String[][]) {
						String[][] contents = (String[][]) replace;
						try {
							ITextTable textTable =
								doc.getTextTableService().constructTextTable(contents.length,
									contents[0].length);
							doc.getTextService().getTextContentService().insertTextContent(r,
								textTable);
							r.setText("");
							ITextTablePropertyStore props = textTable.getPropertyStore();
							// long w=props.getWidth();
							// long percent=w/100;
							for (int row = 0; row < contents.length; row++) {
								String[] zeile = contents[row];
								for (int col = 0; col < zeile.length; col++) {
									textTable.getCell(col, row).getTextService().getText().setText(
										zeile[col]);
								}
							}
							textTable.spreadColumnsEvenly();
							
						} catch (Exception ex) {
							ExHandler.handle(ex);
							r.setText("Fehler beim Ersetzen");
						}
						
					} else {
						r.setText("Not a String");
					}
				}
			}
			return true;
		}
		return false;
	}
	
	public boolean isInteger(String input)	{  
		try	{  
			Integer.parseInt(input);
			return true;  
		} catch(Exception e) {
			return false;
			}
		}  
	
	/** retrieves the type of a form component.
	*/
	static public int getFormComponentType(XPropertySet xComponent)	{
	    XPropertySetInfo xPSI = null;
	    if (null != xComponent)
	        xPSI = xComponent.getPropertySetInfo();
	    
	    if ((null != xPSI) && xPSI.hasPropertyByName("ClassId")) {
	        // get the ClassId property
	        XPropertySet xCompProps = (XPropertySet)UnoRuntime.queryInterface(XPropertySet.class, xComponent);
			try {
				return (Short)xCompProps.getPropertyValue("ClassId");
			} catch (UnknownPropertyException e) {
				e.printStackTrace();
			} catch (WrappedTargetException e) {
				e.printStackTrace();
			}
	     }
	    return 0;
		}	
	
	public PageFormat getFormat(){
		return ITextPlugin.PageFormat.USER;
	}
	
	public String getMimeType(){
		return MIMETYPE_OO2;
	}
	
	/**
	 * Insert a table.
	 * 
	 * @param place
	 *            A string to search for and replace with the table
	 * @param properties
	 *            properties for the table
	 * @param contents
	 *            An Array of String[]s describing each line of the table
	 * @param columnsizes
	 *            int-array describing the relative width of each column (all columns together are
	 *            taken as 100%). May be null, in that case the columns will bhe spread evenly
	 */
	public boolean insertTable(final String place, final int properties, final String[][] contents,
		final int[] columnSizes){
		int offset = 0;
		if ((properties & ITextPlugin.FIRST_ROW_IS_HEADER) == 0) {
			offset = 1;
		}
		SearchDescriptor search = new SearchDescriptor(place);
		search.setIsCaseSensitive(true);
		ISearchResult searchResult = doc.getSearchService().findFirst(search);
		if (!searchResult.isEmpty()) {
			ITextRange r = searchResult.getTextRanges()[0];
			
			try {
				ITextTable textTable =
					doc.getTextTableService().constructTextTable(contents.length + offset,
						contents[0].length);
				doc.getTextService().getTextContentService().insertTextContent(r, textTable);
				r.setText("");
				ITextTablePropertyStore props = textTable.getPropertyStore();
				long w = props.getWidth();
				long percent = w / 100;
				for (int row = 0; row < contents.length; row++) {
					String[] zeile = contents[row];
					for (int col = 0; col < zeile.length; col++) {
						textTable.getCell(col, row + offset).getTextService().getText().setText(
							zeile[col]);
					}
				}
				if (columnSizes == null) {
					textTable.spreadColumnsEvenly();
				} else {
					for (int col = 0; col < contents[0].length; col++) {
						textTable.getColumn(col).setWidth((short) (columnSizes[col] * percent));
					}
					
				}
				
				return true;
			} catch (Exception ex) {
				ExHandler.handle(ex);
			}
		}
		return false;
		
	}
	
	/**
	 * Insert Text and return a cursor describing the position We can not avoid using UNO here,
	 * because NOA does not give us enough control over the text cursor
	 */
	public Object insertText(final String marke, final String text, final int adjust){
		SearchDescriptor search = new SearchDescriptor(marke);
		search.setIsCaseSensitive(true);
		ISearchResult searchResult = doc.getSearchService().findFirst(search);
		XText myText = doc.getXTextDocument().getText();
		XTextCursor cur = myText.createTextCursor();
		// ITextCursor cur=doc.getTextService().getCursorService().getTextCursor();
		if (!searchResult.isEmpty()) {
			ITextRange r = searchResult.getTextRanges()[0];
			cur = myText.createTextCursorByRange(r.getXTextRange());
			cur.setString(text);
			try {
				setFormat(cur);
			} catch (Exception e) {
				ExHandler.handle(e);
			}
			
			cur.collapseToEnd();
		}
		return cur;
	}
	
	/**
	 * Insert text at a position returned by insertText(String,text,adjust)
	 */
	public Object insertText(final Object pos, final String text, final int adjust){
		XTextCursor cur = (XTextCursor) pos;
		if (cur != null) {
			cur.setString(text);
			try {
				setFormat(cur);
			} catch (Exception e) {
				ExHandler.handle(e);
			}
			cur.collapseToEnd();
		}
		return cur;
	}
	
	/**
	 * Insert Text inside a rectangular area. Again we need UNO to get access to a Text frame.
	 */
	public Object insertTextAt(final int x, final int y, final int w, final int h,
		final String text, final int adjust){
		
		try {
			XTextDocument myDoc = doc.getXTextDocument();
			com.sun.star.lang.XMultiServiceFactory documentFactory =
				(com.sun.star.lang.XMultiServiceFactory) UnoRuntime.queryInterface(
					com.sun.star.lang.XMultiServiceFactory.class, myDoc);
			
			Object frame = documentFactory.createInstance("com.sun.star.text.TextFrame");
			
			XText docText = myDoc.getText();
			XTextFrame xFrame = (XTextFrame) UnoRuntime.queryInterface(XTextFrame.class, frame);
			
			XShape xWriterShape = (XShape) UnoRuntime.queryInterface(XShape.class, xFrame);
			
			xWriterShape.setSize(new Size(w * 100, h * 100));
			
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
	
	/**
	 * Print the contents of the panel. NOA does no allow us to select printer and tray, so we do it
	 * with UNO again.
	 */
	public boolean print(final String toPrinter, final String toTray,
		final boolean waitUntilFinished){
		try {
			PropertyValue[] pprops;
			if (StringTool.isNothing(toPrinter)) {
				pprops = new PropertyValue[1];
				pprops[0] = new PropertyValue();
				pprops[0].Name = "Pages";
				pprops[0].Value = "1-";
			} else {
				pprops = new PropertyValue[2];
				pprops[0] = new PropertyValue();
				pprops[0].Name = "Pages";
				pprops[0].Value = "1-";
				pprops[1] = new PropertyValue();
				pprops[1].Name = "Name";
				pprops[1].Value = toPrinter;
			}
			if (!StringTool.isNothing(toTray)) {
				XTextDocument myDoc = doc.getXTextDocument();
				// XTextDocument myDoc=(XTextDocument)
				// UnoRuntime.queryInterface(com.sun.star.text.XTextDocument.class,
				// doc);
				if (!OOPrinter.setPrinterTray(myDoc, toTray)) {
					return false;
				}
			}
			XPrintable xPrintable =
				(XPrintable) UnoRuntime.queryInterface(com.sun.star.view.XPrintable.class, doc
					.getXTextDocument());
			
			com.sun.star.view.XPrintJobBroadcaster selection =
				(com.sun.star.view.XPrintJobBroadcaster) UnoRuntime.queryInterface(
					com.sun.star.view.XPrintJobBroadcaster.class, xPrintable);
			
			MyXPrintJobListener myXPrintJobListener = new MyXPrintJobListener();
			selection.addPrintJobListener(myXPrintJobListener);
			
			// bean.getDocument().print(pprops);
			xPrintable.print(pprops);
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
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
	}
	
	public void setFocus(){
	// TODO Auto-generated method stub
	
	}
	
	public void setFormat(final PageFormat f){
	// TODO Auto-generated method stub
	
	}
	
	public void setSaveOnFocusLost(final boolean bSave){
	// TODO Auto-generated method stub
	
	}
	
	public void showMenu(final boolean b){
	// TODO Auto-generated method stub
	
	}
	
	public void showToolbar(final boolean b){
	// TODO Auto-generated method stub
	
	}
	
	public void setInitializationData(final IConfigurationElement config,
		final String propertyName, final Object data) throws CoreException{
	// TODO Auto-generated method stub
	
	}
	
	/**
	 * basically: ensure that OpenOffice is happy closing the document and create a new temporary
	 * file
	 * 
	 */
	private void clean(){
		try {
			if (doc != null) {
				doc.getPersistenceService().store(myFile.getAbsolutePath());
				// doc.close();
				myFile.delete();
			}
			myFile = File.createTempFile("noa", ".odt");
			myFile.deleteOnExit();
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
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
	
	private com.sun.star.beans.XPropertySet setFormat(final XTextCursor xtc)
		throws UnknownPropertyException, PropertyVetoException, IllegalArgumentException,
		WrappedTargetException{
		com.sun.star.beans.XPropertySet charProps =
			(com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
				com.sun.star.beans.XPropertySet.class, xtc);
		if (font != null) {
			charProps.setPropertyValue("CharFontName", font);
		}
		if (hi > 0)
		{
			charProps.setPropertyValue("CharHeight", new Float(hi));
		}
		if (stil > -1)
		{
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

	class closeListener implements ICloseListener {
		
		private IOfficeApplication officeAplication = null;
		
		// ----------------------------------------------------------------------------
		/**
		 * Constructs a new SnippetDocumentCloseListener
		 * 
		 * @author Sebastian Rösgen
		 * @date 17.03.2006
		 */
		public closeListener(final IOfficeApplication officeAplication){
			this.officeAplication = officeAplication;
		}
		
		// ----------------------------------------------------------------------------
		/**
		 * Is called when someone tries to close a listened object. Not needed in here.
		 * 
		 * @param closeEvent
		 *            close event
		 * @param getsOwnership
		 *            information about the ownership
		 * 
		 * @author Sebastian Rösgen
		 * @date 17.03.2006
		 */
		public void queryClosing(final ICloseEvent closeEvent, final boolean getsOwnership){
		// nothing to do in here
		}
		
		// ----------------------------------------------------------------------------
		/**
		 * Is called when the listened object is closed really.
		 * 
		 * @param closeEvent
		 *            close event
		 * 
		 * @author Sebastian Rösgen
		 * @date 17.03.2006
		 */
		public void notifyClosing(final ICloseEvent closeEvent){
			try {
				removeMe();
			} catch (Exception exception) {
				System.err.println("Error closing office application!");
				exception.printStackTrace();
			}
		}
		
		// ----------------------------------------------------------------------------
		/**
		 * Is called when the broadcaster is about to be disposed.
		 * 
		 * @param event
		 *            source event
		 * 
		 * @author Sebastian Rösgen
		 * @date 17.03.2006
		 */
		public void disposing(final IEvent event){
		// nothing to do in here
		}
		// ----------------------------------------------------------------------------
		
	}

	@Override
	public boolean isDirectOutput() {
		return false;
	}
}
