/*******************************************************************************
 * Copyright (c) 2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Sponsoring:
 * 	 mediX Notfallpaxis, diepraxen Stauffacher AG, Zürich
 * 
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.agenda.ui;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;

import ch.elexis.Hub;
import ch.elexis.actions.Activator;
import ch.elexis.agenda.data.Termin;
import ch.elexis.agenda.preferences.PreferenceConstants;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.dialogs.TerminDialog;
import ch.elexis.util.PersistentObjectDropTarget;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class ProportionalSheet extends Composite implements IAgendaLayout {
	static final int LEFT_OFFSET_DEFAULT = 20;
	static final int PADDING_DEFAULT = 5;
	
	private int left_offset, padding;
	private AgendaParallel view;
	private MenuManager contextMenuManager;
	private List<TerminLabel> tlabels;
	private double ppm;
	private int sheetHeight;
	private String[] resources;
	private int textWidth;
	private double sheetWidth;
	private double widthPerColumn;
	
	private TimeTool setTerminTo(int x, int y){
		String resource = ""; //$NON-NLS-1$
		for (int i = 0; i < resources.length; i++) {
			double lower = left_offset + i * (widthPerColumn + padding);
			double upper = lower + widthPerColumn;
			if (isBetween(x, lower, upper)) {
				resource = resources[i];
				break;
			}
		}
		int minute = (int) Math.round(y / ppm);
		TimeTool tt = new TimeTool(Activator.getDefault().getActDate());
		int hour = minute / 60;
		minute = minute - (60 * hour);
		int raster = 15;
		minute = ((minute + (raster >> 1)) / raster) * raster;
		tt.set(TimeTool.AM_PM, TimeTool.AM);
		tt.set(TimeTool.HOUR, hour);
		tt.set(TimeTool.MINUTE, minute);
		if (resource.length() > 0) {
			Activator.getDefault().setActResource(resource);
		}
		return tt;
	}
	
	public ProportionalSheet(Composite parent, AgendaParallel v){
		super(parent, SWT.NO_BACKGROUND);
		view = v;
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e){
				layout();
				recalc();
			}
		});
		addPaintListener(new TimePainter());
		addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseDoubleClick(MouseEvent e){
				String startOfDayTimeInMinutes =
					Hub.globalCfg.get(PreferenceConstants.AG_DAY_PRESENTATION_STARTS_AT, "0000");
				int sodtHours = Integer.parseInt(startOfDayTimeInMinutes.substring(0, 2));
				int sodtMinutes = Integer.parseInt(startOfDayTimeInMinutes.substring(2));

				TimeTool tt = setTerminTo(e.x, e.y);
				tt.addHours(sodtHours);
				tt.addMinutes(sodtMinutes);
				TerminDialog dlg = new TerminDialog(null);
				dlg.create();
				dlg.setTime(tt);
				if (dlg.open() == Dialog.OK) {
					
					refresh();
				}
			}
			
		});
		
		// setBackground(Desk.getColor(Desk.COL_GREEN));
		left_offset = LEFT_OFFSET_DEFAULT;
		padding = PADDING_DEFAULT;
		new PersistentObjectDropTarget(this, new PersistentObjectDropTarget.IReceiver() {
			
			public boolean accept(PersistentObject o){
				return true;
			}
			
			public void dropped(PersistentObject o, DropTargetEvent e){
				Point pt = Display.getCurrent().map(null, ProportionalSheet.this, e.x, e.y);
				TimeTool tt = setTerminTo(pt.x, pt.y);
				TerminLabel tl = (TerminLabel) e.widget;
				Termin t = tl.getTermin();
				if (t != null) {
					t.setStartTime(tt);
					t.setBereich(Activator.getDefault().getActResource());
					tl.refresh();
					refresh();
				}
			}
		});
	}
	
	private boolean isBetween(int x, double lower, double upper){
		int y = (int) Math.round(lower);
		int z = (int) Math.round(upper);
		if ((x >= y) && (x <= z)) {
			return true;
		}
		return false;
	}
	
	public MenuManager getContextMenuManager(){
		return contextMenuManager;
	}
	
	public void clear(){
		while (tlabels != null && tlabels.size() > 0) {
			tlabels.remove(0).dispose();
		}
		recalc();
		
	}
	
	synchronized void refresh(){
		String[] resnames = view.getDisplayedResources();
		Query<Termin> qbe = new Query<Termin>(Termin.class);
		String day = Activator.getDefault().getActDate().toString(TimeTool.DATE_COMPACT);
		qbe.add("Tag", "=", day);
		qbe.startGroup();
		
		for (String n : resnames) {
			qbe.add("BeiWem", "=", n);
			qbe.or();
		}
		qbe.endGroup();
		List<Termin> apps = qbe.execute();
		Collections.sort(apps);
		if (tlabels == null) {
			tlabels = new LinkedList<TerminLabel>();
		}
		int s = apps.size();
		while (s < tlabels.size()) {
			tlabels.remove(0).dispose();
		}
		while (s > tlabels.size()) {
			tlabels.add(new TerminLabel(this));
		}
		Iterator<Termin> ipi = apps.iterator();
		Iterator<TerminLabel> iptl = tlabels.iterator();
		while (ipi.hasNext()) {
			TerminLabel tl = iptl.next();
			Termin t = ipi.next();
			String m = t.getBereich();
			int idx = StringTool.getIndex(resnames, m);
			if (idx == -1) {
				ipi.remove();
				iptl.remove();
			} else {
				tl.set(t, idx);
			}
		}
		recalc();
	}
	
	void recalc(){
		if (tlabels != null) {
			ppm = AgendaParallel.getPixelPerMinute();
			
			String startOfDayTimeInMinutes =
				Hub.globalCfg.get(PreferenceConstants.AG_DAY_PRESENTATION_STARTS_AT, "0000");
			int sodtHours = Integer.parseInt(startOfDayTimeInMinutes.substring(0, 2));
			int sodtMinutes = Integer.parseInt(startOfDayTimeInMinutes.substring(2));
			int sodtM = (sodtHours * 60);
			sodtM += sodtMinutes;
			
			String endOfDayTimeInMinutes =
				Hub.globalCfg.get(PreferenceConstants.AG_DAY_PRESENTATION_ENDS_AT, "2359");
			int eodtHours = Integer.parseInt(endOfDayTimeInMinutes.substring(0, 2));
			int eodtMinutes = Integer.parseInt(endOfDayTimeInMinutes.substring(2));
			int eodtM = (eodtHours * 60);
			eodtM += eodtMinutes;
			
			sheetHeight = (int) Math.round(ppm * (eodtM - sodtM));
			ScrolledComposite sc = (ScrolledComposite) getParent();
			Point mySize = getSize();
			
			if (mySize.x > 0.0) {
				if (mySize.y != sheetHeight) {
					setSize(mySize.x, sheetHeight);
					sc.setMinSize(getSize());
				}
				ScrollBar bar = sc.getVerticalBar();
				int barWidth = 14;
				if (bar != null) {
					barWidth = bar.getSize().x;
				}
				resources = view.getDisplayedResources();
				int count = resources.length;
				Point textSize = SWTHelper.getStringBounds(this, "88:88"); //$NON-NLS-1$
				textWidth = textSize.x;
				left_offset = textWidth + 2;
				sheetWidth = mySize.x - 2 * left_offset - barWidth;
				widthPerColumn = sheetWidth / count;
				ColumnHeader header = view.getHeader();
				header.recalc(widthPerColumn, left_offset, padding, textSize.y);
				
				for (TerminLabel l : tlabels) {
					l.refresh();
				}
				sc.layout();
			}
		}
	}
	
	public double getPixelPerMinute(){
		return ppm;
	}
	
	public double getWidthPerColumn(){
		return widthPerColumn;
	}
	
	public int getLeftOffset(){
		return left_offset;
	}
	
	public int getPadding(){
		return padding;
	}
	
	class TimePainter implements PaintListener {
		
		public void paintControl(PaintEvent e){
			GC gc = e.gc;
			gc.fillRectangle(e.x, e.y, e.width, e.height);
			int y = 0;
			TimeTool runner = new TimeTool();
			String dayStartsAt =
				Hub.globalCfg.get(PreferenceConstants.AG_DAY_PRESENTATION_STARTS_AT, "0000");
			runner.set(dayStartsAt); //$NON-NLS-1$
			
			String dayEndsAt =
				Hub.globalCfg.get(PreferenceConstants.AG_DAY_PRESENTATION_ENDS_AT, "2359");
			TimeTool limit = new TimeTool(dayEndsAt); //$NON-NLS-1$
			Point textSize = gc.textExtent("88:88"); //$NON-NLS-1$
			int textwidth = textSize.x;
			
			int quarter = (int) Math.round(15.0 * AgendaParallel.getPixelPerMinute());
			int w = ProportionalSheet.this.getSize().x - 5;
			int left = 0;
			int right = w - textwidth;
			while (runner.isBefore(limit)) {
				gc.drawLine(left, y, w, y); // volle Linie
				String time = runner.toString(TimeTool.TIME_SMALL);
				gc.drawText(time, 0, y + 1);
				gc.drawText(time, right, y + 1);
				y += quarter;
				gc.drawLine(textwidth - 3, y, textwidth, y);
				gc.drawLine(right, y, right + 3, y);
				y += quarter;
				gc.drawLine(textwidth - 6, y, textwidth, y);
				gc.drawLine(right, y, right + 6, y);
				y += quarter;
				gc.drawLine(textwidth - 3, y, textwidth, y);
				gc.drawLine(right, y, right + 3, y);
				y += quarter;
				runner.addHours(1);
			}
		}
		
	}
	
	public Composite getComposite(){
		return this;
	}
}
