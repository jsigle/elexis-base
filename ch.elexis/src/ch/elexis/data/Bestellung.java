/*******************************************************************************
 * Copyright (c) 2006-2012, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    M. Descher - orders are now persisted into own table 
 *    
 *    $Id$
 *******************************************************************************/

package ch.elexis.data;

import java.util.ArrayList;
import java.util.List;

import ch.elexis.core.PersistenceException;
import ch.elexis.util.Log;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class Bestellung extends PersistentObject {
	public static final String VERSION = "1.0.0";
	private static final String FLD_ITEMS = "Liste"; //$NON-NLS-1$
	private static final String TABLENAME = "BESTELLUNGEN"; //$NON-NLS-1$
	
	public static final String ISORDERED = "ch.elexis.data.Bestellung.isOrdered"; //$NON-NLS-1$
	
	private List<Item> alItems;
	
	private static Log logger = Log.get(Bestellung.class.getName());
	
	public enum ListenTyp {
		PHARMACODE, NAME, VOLL
	};
	
	static final String create = "CREATE TABLE " + TABLENAME + " ("
		+ "ID       	VARCHAR(80) primary key, " + "lastupdate BIGINT,"
		+ "deleted  	CHAR(1) default '0'," + "datum      CHAR(8)," + "Contents 	BLOB);"
		+ "INSERT INTO " + TABLENAME + " (ID,datum,deleted) VALUES ('1'," + JdbcLink.wrap(VERSION)
		+ ",'1');";
	
	public static void initialize(){
		createOrModifyTable(create);
		
		// Starting with 2.1.7.rc0 orders are excavated from the HEAP2 table into an own table,
		// the following lines move the respective entries into the new table
		Query<NamedBlob2> eoq = new Query<NamedBlob2>(NamedBlob2.class);
		List<NamedBlob2> eor = eoq.execute();
		for (NamedBlob2 nb2 : eor) {
			String[] entry = nb2.getId().split(":");
			if (entry.length == 3) {
				Bestellung b = new Bestellung();
				if (b.create(nb2.getId())) {
					b.set(FLD_ITEMS, nb2.get(NamedBlob2.FLD_CONTENTS));
					logger.log("Moved order " + nb2.getId() + " from HEAP2 to " + TABLENAME,
						Log.INFOS);
					nb2.delete();
				} else {
					logger.log("Error creating " + nb2.getId() + " in table " + TABLENAME,
						Log.INFOS);
				}
			}
		}
	}
	
	static {
		addMapping(TABLENAME, "Liste=S:C:Contents"); //$NON-NLS-1$
		
		try {
			// Starting with 2.1.7.rc0 orders are excavated from the HEAP2 table, here
			// we check whether the table is existing (new method), else we need to call
			// the merge code
			load("1");
		} catch (PersistenceException e) {
			initialize();
		}
	}
	
	public Bestellung(String name, Anwender an){
		TimeTool t = new TimeTool();
		create(name + ":" + t.toString(TimeTool.TIMESTAMP) + ":" + an.getId()); //$NON-NLS-1$ //$NON-NLS-2$
		alItems = new ArrayList<Item>();
	}
	
	@Override
	public String getLabel(){
		String[] i = getId().split(":"); //$NON-NLS-1$
		TimeTool t = new TimeTool(i[1]);
		return i[0] + ": " + t.toString(TimeTool.FULL_GER); //$NON-NLS-1$
	}
	
	public String asString(ListenTyp type){
		StringBuilder ret = new StringBuilder();
		for (Item i : alItems) {
			switch (type) {
			case PHARMACODE:
				ret.append(i.art.getPharmaCode());
				break;
			case NAME:
				ret.append(i.art.getLabel());
				break;
			case VOLL:
				ret.append(i.art.getPharmaCode()).append(StringTool.space).append(i.art.getName());
				break;
			default:
				break;
			}
			ret.append(",").append(i.num).append(StringTool.lf); //$NON-NLS-1$
		}
		return ret.toString();
	}
	
	public List<Item> asList(){
		return alItems;
	}
	
	public void addItem(Artikel art, int num){
		Item i = findItem(art);
		if (i != null) {
			i.num += num;
		} else {
			alItems.add(new Item(art, num));
		}
	}
	
	public Item findItem(Artikel art){
		for (Item i : alItems) {
			if (i.art.getId().equals(art.getId())) {
				return i;
			}
		}
		return null;
	}
	
	public void removeItem(Item art){
		alItems.remove(art);
		
	}
	
	public void save(){
		StringBuilder sb = new StringBuilder();
		for (Item i : alItems) {
			sb.append(i.art.getId()).append(",").append(i.num).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		set(FLD_ITEMS, sb.toString());
	}
	
	public void load(){
		String[] it = checkNull(get(FLD_ITEMS)).split(";"); //$NON-NLS-1$
		if (alItems == null) {
			alItems = new ArrayList<Item>();
		} else {
			alItems.clear();
		}
		for (String i : it) {
			String[] fld = i.split(","); //$NON-NLS-1$
			if (fld.length == 2) {
				Artikel art = Artikel.load(fld[0]);
				if (art.exists()) {
					alItems.add(new Item(art, Integer.parseInt(fld[1])));
				}
			}
		}
	}
	
	@Override
	protected String getTableName(){
		return TABLENAME;
	}
	
	public static Bestellung load(String id){
		Bestellung ret = new Bestellung(id);
		if (ret != null) {
			ret.load();
		}
		return ret;
	}
	
	protected Bestellung(){}
	
	protected Bestellung(String id){
		super(id);
	}
	
	public static class Item {
		public Item(Artikel a, int n){
			art = a;
			num = n;
		}
		
		public Artikel art;
		public int num;
	}
	
	public static void markAsOrdered(Item[] list){
		for (Item item : list) {
			if (item.art != null)
				item.art.setExt(Bestellung.ISORDERED, "true");
		}
	}
}
