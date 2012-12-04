/*******************************************************************************
 * Copyright (c) 2007-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: MsgHeartListener.java 4371 2008-09-04 13:47:51Z rgw_ch $
 *******************************************************************************/

package ch.elexis.messages;

import java.util.List;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.Heartbeat.HeartListener;
import ch.elexis.data.Query;

public class MsgHeartListener implements HeartListener {
	boolean bSkip;
	
	public void heartbeat(){
		if (!bSkip) {
			if (Hub.actUser != null) {
				Query<Message> qbe = new Query<Message>(Message.class);
				qbe.add("to", "=", Hub.actUser.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				final List<Message> res = qbe.execute();
				if (res.size() > 0) {
					Desk.getDisplay().asyncExec(new Runnable() {
						public void run(){
							bSkip = true;
							new MsgDetailDialog(Hub.getActiveShell(), res.get(0)).open();
							bSkip = false;
						}
					});
					
				}
			}
		}
	}
	
}
