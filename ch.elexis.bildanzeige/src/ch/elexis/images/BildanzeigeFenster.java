/*******************************************************************************
 * Copyright (c) 2006-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *    $Id: BildanzeigeFenster.java 6014 2010-01-31 19:17:37Z rgw_ch $
 *******************************************************************************/

package ch.elexis.images;

import java.io.File;
import java.io.FileOutputStream;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.Desk;
import ch.elexis.util.SWTHelper;

public class BildanzeigeFenster extends TitleAreaDialog {
	Bild bild;
	Image img;
	
	public BildanzeigeFenster(Shell shell, Bild bild){
		super(shell);
		this.bild = bild;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		// parent.setLayout(new FillLayout());
		ScrolledComposite ret =
			new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		Composite canvas = new Composite(ret, SWT.NONE);
		ret.setContent(canvas);
		img = bild.createImage();
		Rectangle r = img.getBounds();
		// GridData gd=new GridData(r.width,r.height);
		// canvas.setLayoutData(gd);
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e){
				GC gc = e.gc;
				gc.drawImage(img, 0, 0);
			}
		});
		canvas.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e){
				img.dispose();
			}
			
		});
		canvas.setSize(r.width, r.height);
		return ret;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		Button bClose =
			createButton(parent, org.eclipse.jface.dialogs.Dialog.OK,
				Messages.BildanzeigeFenster_Close, true);
		bClose.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e){
				BildanzeigeFenster.this.okPressed();
			}
			
		});
		Button bExport = createButton(parent, 4, Messages.BildanzeigeFenster_Export, false);
		bExport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				FileDialog fd = new FileDialog(BildanzeigeFenster.this.getShell(), SWT.SAVE);
				String fname = fd.open();
				if (fname != null) {
					File file = new File(fname);
					try {
						if (!file.createNewFile() || !file.canWrite()) {
							SWTHelper.showError(Messages.BildanzeigeFenster_Error,
								Messages.BildanzeigeFenster_Cannot + fname
									+ Messages.BildanzeigeFenster_Create);
						} else {
							byte[] arr = bild.getData();
							FileOutputStream fout = new FileOutputStream(file);
							fout.write(arr);
							fout.close();
						}
					} catch (Exception ex) {
						SWTHelper.showError(Messages.BildanzeigeFenster_Error,
							Messages.BildanzeigeFenster_ErrorWriting + fname);
					}
				}
			}
		});
		// super.createButtonsForButtonBar(parent);
	}
	
	@Override
	public void create(){
		super.create();
		getShell().setText(bild.getPatient().getLabel());
		setTitle(bild.getLabel());
		setMessage(bild.get("Info")); //$NON-NLS-1$
		setTitleImage(Desk.getImage(Desk.IMG_LOGO48));
	}
	
}
