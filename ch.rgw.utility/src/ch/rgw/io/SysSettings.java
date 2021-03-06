// $Id: SysSettings.java 23 2006-03-24 15:36:01Z rgw_ch $

package ch.rgw.io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.prefs.*;

import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;

/**
 * Settings-Implementation, die ein "Systemtypisches" Verfahren zur Speicherung verwendet. Unter
 * Windows ist das die Registry, unter Linux eine .datei in XML-Format. Es wird unterschieden
 * zwischen anwendnerspezifischen Settings (USER_SETTINGS) und systemweiten Settings
 * (SYSTEM_SETTINGS)
 */

public class SysSettings extends Settings {
	private static final long serialVersionUID = -7855039763450972263L;
	
	public static final String Version(){
		return "1.0.2";
	}
	
	public static final int USER_SETTINGS = 0;
	public static final int SYSTEM_SETTINGS = 1;
	volatile int typ;
	volatile Class clazz;
	
	/**
	 * Settings neu Anlegen oder einlesen
	 * 
	 * @param type
	 *            USER_SETTINGS oder SYSTEM_SETTINGS
	 * @param cl
	 *            Basisklasse für den Settings-zweig
	 */
	public SysSettings(int type, Class cl){
		super();
		typ = type;
		clazz = cl;
		undo();
	}
	
	private Preferences getRoot(){
		Preferences pr = null;
		if (typ == USER_SETTINGS) {
			pr = Preferences.userNodeForPackage(clazz);
		} else {
			pr = Preferences.systemNodeForPackage(clazz);
		}
		String[] nodes = (getPath().split("/"));
		Preferences sub = pr;
		// Preferences[] plist=new Preferences[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			sub = sub.node(nodes[i]);
		}
		return sub;
	}
	
	/**
	 * Diese Settings als XML-Datei exportieren
	 * 
	 * @param file
	 *            Dateiname
	 * @throws Exception
	 */
	public void write_xml(String file) throws Exception{
		FileOutputStream os = new FileOutputStream(file);
		getRoot().exportSubtree(os);
		os.close();
	}
	
	/**
	 * Settings aus XML-Datei importieren
	 * 
	 * @param file
	 *            Dateiname
	 * @throws Exception
	 */
	public void read_xml(String file) throws Exception{
		FileInputStream is = new FileInputStream(file);
		Preferences.importPreferences(is);
		is.close();
	}
	
	/**
	 * @see ch.rgw.IO.Settings#flush()
	 */
	protected void flush_absolute(){
		Iterator it = iterator();
		Preferences pr = getRoot();
		while (it.hasNext()) {
			String a = (String) it.next();
			String[] nodes = a.split("/");
			String key = nodes[nodes.length - 1];
			Object value = get(a, null);
			Preferences sub = pr;
			Preferences[] plist = new Preferences[nodes.length];
			for (int i = 0; i < plist.length - 1; i++) {
				sub = sub.node(nodes[i]);
			}
			if (StringTool.isNothing(value)) {
				sub.remove(key);
			} else {
				sub.put(key, (String) value);
			}
		}
		try {
			pr.flush();
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
	}
	
	public void undo(){
		clear();
		loadTree(getRoot(), "");
		
	}
	
	private void loadTree(Preferences root, String path){
		try {
			String[] subnodes = root.childrenNames();
			path = path.replaceFirst("^/", "");
			for (int s = 0; s < subnodes.length; s++) {
				Preferences sub = root.node(subnodes[s]);
				loadTree(sub, path + "/" + subnodes[s]);
			}
			String[] keys = root.keys();
			for (int i = 0; i < keys.length; i++) {
				if (path.equals(""))
					set(keys[i], root.get(keys[i], ""));
				else
					set(path + "/" + keys[i], root.get(keys[i], ""));
			}
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		
	}
}