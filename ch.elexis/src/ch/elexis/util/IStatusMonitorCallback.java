/*******************************************************************************
 * Copyright (c) 2013, Joerg Sigle, www.jsigle.com
 * All rights reserved.
 *
 * Contributors:
 *    J. Sigle - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.util;

/*
 * 201306170920js: Interface to enable StatusMonitor to call additional methods,
 * especially one ShowViewHandler() to activate the view with a text document plugin.
 * 
 * The status monitor also uses the SaveHandler() defined elsewhere and previously.
 */
public interface IStatusMonitorCallback {
	public void showView();
}
