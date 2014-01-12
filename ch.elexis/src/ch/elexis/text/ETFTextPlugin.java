/*******************************************************************************
 * Copyright (c) 2007-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/
package ch.elexis.text;

import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Composite;

import ch.elexis.data.Brief;
import ch.elexis.util.IKonsExtension;
import ch.rgw.compress.CompEx;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;

/**
 * A TextPlugin based on an EnhancedTextField
 * 
 * @author gerry
 * 
 */
public class ETFTextPlugin implements ITextPlugin {
	private static final String CHARSET = "UTF-8"; //$NON-NLS-1$
	EnhancedTextField etf;
	ICallback handler;
	boolean bSaveOnFocusLost = false;
	IKonsExtension ike;
	
	public boolean clear(){
		etf.setText(StringTool.leer);
		return true;
	}
	
	public void setSaveOnFocusLost(boolean mode){
		bSaveOnFocusLost = mode;
	}
	
	public Composite createContainer(Composite parent, ICallback h){
		handler = h;
		etf = new EnhancedTextField(parent);
		etf.text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e){
				if (bSaveOnFocusLost) {
					if (handler != null) {
						handler.save();
					}
				}
			}
			
		});
		ike = new ExternalLink();
		ike.connect(etf);
		etf.setText(StringTool.leer);
		return etf;
	}
	
	public boolean createEmptyDocument(){
		etf.setText(StringTool.leer);
		return true;
	}
	
	public void dispose(){
		etf.dispose();
	}
	
	public boolean findOrReplace(String pattern, ReplaceCallback cb){
		// TODO Auto-generated method stub
		return false;
	}
	
	public PageFormat getFormat(){
		return PageFormat.USER;
	}
	
	public String getMimeType(){
		return "text/xml"; //$NON-NLS-1$
	}
	
	public boolean insertTable(String place, int properties, String[][] contents, int[] columnSizes){
		// TODO Auto-generated method stub
		return false;
	}
	
	public Object insertText(String marke, String text, int adjust){
		int pos = 0;
		if (StringTool.isNothing(marke)) {
			etf.text.setSelection(0);
		} else {
			String tx = etf.text.getText();
			pos = tx.indexOf(marke);
			etf.text.setSelection(pos, pos + marke.length());
		}
		etf.text.insert(text);
		return new Integer(pos + text.length());
	}
	
	public Object insertText(Object pos, String text, int adjust){
		if (!(pos instanceof Integer)) {
			return null;
		}
		Integer px = (Integer) pos;
		etf.text.setSelection(px);
		etf.text.insert(text);
		return new Integer(px + text.length());
	}
	
	public Object insertTextAt(int x, int y, int w, int h, String text, int adjust){
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean loadFromByteArray(byte[] bs, boolean asTemplate){
		try {
			byte[] exp = CompEx.expand(bs);
			String cnt = StringTool.leer;
			if (exp != null) {
				cnt = new String(exp, CHARSET);
			}
			etf.setText(cnt);
			return true;
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
	}
	
	public byte[] storeToByteArray(){
		try {
			String cnt = etf.getContentsAsXML();
			byte[] exp = cnt.getBytes(CHARSET);
			return CompEx.Compress(exp, CompEx.ZIP);
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return null;
		}
		
	}
	
	public boolean loadFromStream(InputStream is, boolean asTemplate){
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean print(String toPrinter, String toTray, boolean waitUntilFinished){
		// TODO Auto-generated method stub
		return false;
	}
	
	public void setFocus(){
		etf.setFocus();
	}
	
	public boolean setFont(String name, int style, float size){
		// Font font=new Font(Desk.theDisplay,name,Math.round(size),style);
		return true;
	}
	
	public boolean setStyle(final int style){
		return false;
	}
	
	public void setFormat(PageFormat f){
		// TODO Auto-generated method stub
		
	}
	
	public void showMenu(boolean b){
		// TODO Auto-generated method stub
		
	}
	
	public void showToolbar(boolean b){
		// TODO Auto-generated method stub
		
	}
	
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean isDirectOutput(){
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
