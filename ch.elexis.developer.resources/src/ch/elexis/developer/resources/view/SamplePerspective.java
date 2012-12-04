/*******************************************************************************
 * Copyright (c) 2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *    $Id: SamplePerspective.java 6128 2010-02-12 16:05:28Z rgw_ch $
 *******************************************************************************/
package ch.elexis.developer.resources.view;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * This is an example on how to create a perspective. In this sample plugin with only one view it is
 * no really necessary...
 * 
 * @author gerry
 * 
 */
public class SamplePerspective implements IPerspectiveFactory {
	
	public void createInitialLayout(IPageLayout layout){
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		layout.setFixed(false);
		IFolderLayout folder = layout.createFolder("folder", IPageLayout.RIGHT, 1.0f, editorArea); //$NON-NLS-1$
		folder.addView(SampleView.ID);
		
	}
	
}
