/*******************************************************************************
 * Copyright (c) 2007-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/
package ch.elexis.artikel_at.views;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;

import ch.elexis.Desk;
import ch.elexis.actions.AbstractDataLoaderJob;
import ch.elexis.actions.JobPool;
import ch.elexis.actions.ListLoader;
import ch.elexis.artikel_at.data.Medikament;
import ch.elexis.data.Artikel;
import ch.elexis.data.Query;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.DefaultControlFieldProvider;
import ch.elexis.util.viewers.LazyContentProvider;
import ch.elexis.util.viewers.SimpleWidgetProvider;
import ch.elexis.util.viewers.ViewerConfigurer;
import ch.elexis.views.artikel.ArtikelContextMenu;
import ch.elexis.views.artikel.ArtikelContextMenu.ArtikelDetailDisplay;
import ch.elexis.views.codesystems.CodeSelectorFactory;

public class MedikamentSelector extends CodeSelectorFactory implements ArtikelDetailDisplay {
	public static final String JOBNAME = "Medikamente (A)";
	
	AbstractDataLoaderJob dataloader;
	
	public MedikamentSelector(){
		dataloader = (AbstractDataLoaderJob) JobPool.getJobPool().getJob(JOBNAME);
		if (dataloader == null) {
			dataloader =
				new ListLoader(JOBNAME, new Query<Medikament>(Medikament.class), new String[] {
					"Name", "Substanz", "Notiz"
				});
			JobPool.getJobPool().addJob(dataloader);
		}
		JobPool.getJobPool().activate(JOBNAME, Job.SHORT);
	}
	
	@Override
	public ViewerConfigurer createViewerConfigurer(CommonViewer cv){
		new ArtikelContextMenu(
			(Medikament) new ch.elexis.artikel_at.data.ArtikelFactory()
				.createTemplate(Medikament.class),
			cv, this);
		return new ViewerConfigurer(new LazyContentProvider(cv, dataloader, null),
			new VidalLabelProvider(), new DefaultControlFieldProvider(cv, new String[] {
				"Name"
			}), new ViewerConfigurer.DefaultButtonProvider(), new SimpleWidgetProvider(
				SimpleWidgetProvider.TYPE_LAZYLIST, SWT.NONE, null));
	}
	
	@Override
	public void dispose(){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String getCodeSystemName(){
		return Medikament.CODESYSTEMNAME;
	}
	
	@Override
	public Class getElementClass(){
		return Medikament.class;
	}
	
	public boolean show(Artikel art){
		MedikamentDetailDialog mdd =
			new MedikamentDetailDialog(Desk.getTopShell(), (Medikament) art);
		mdd.open();
		return false;
	}
	
}
