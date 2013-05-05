/*******************************************************************************
 * Copyright (c) 2010, St. Schenk and Medshare GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    St. Schenk - initial implementation
 * 
 *******************************************************************************/

package ch.medshare.ebm;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class HotKeyHandler extends AbstractHandler {
	
	public Object execute(ExecutionEvent event) throws ExecutionException{
		SearchAction action = new SearchAction();
		action.run();
		return null;
	}
}