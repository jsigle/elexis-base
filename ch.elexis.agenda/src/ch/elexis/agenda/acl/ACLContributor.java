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
package ch.elexis.agenda.acl;

import ch.elexis.Hub;
import ch.elexis.actions.Activator;
import ch.elexis.admin.ACE;
import ch.elexis.admin.AccessControl;
import ch.elexis.admin.IACLContributor;

/**
 * Define access rights needed for the various actions with the agenda
 * 
 * @author gerry
 * 
 */
public class ACLContributor implements IACLContributor {
	/** The right to use the agenda at all */
	public static final ACE ACE_AGENDA = new ACE(ACE.ACE_ROOT, Activator.PLUGIN_ID, "Agenda"); //$NON-NLS-1$
	public static final ACE USE_AGENDA = new ACE(ACE_AGENDA,
		"user", Messages.ACLContributor_acl_use); //$NON-NLS-1$
	
	/** administrative rights to the agenda */
	public static final ACE ADMIN_AGENDA = new ACE(ACE_AGENDA,
		"admin", Messages.ACLContributor_acl_administer); //$NON-NLS-1$
	
	/** The right to see appointments */
	public static final ACE DISPLAY_APPOINTMENTS = new ACE(USE_AGENDA,
		"zeigeTermine", Messages.ACLContributor_acl_showAppointments); //$NON-NLS-1$
	/** The right to modify appointments */
	public static final ACE CHANGE_APPOINTMENTS = new ACE(USE_AGENDA,
		"ändereTermine", Messages.ACLContributor_acl_changeAppointments); //$NON-NLS-1$
	/** The right to delete appointments */
	public static final ACE DELETE_APPOINTMENTS = new ACE(USE_AGENDA,
		"löscheTermine", Messages.ACLContributor_acl_deleteAppointments); //$NON-NLS-1$
	
	/** The right to modify the day limits */
	public static final ACE CHANGE_DAYSETTINGS = new ACE(ADMIN_AGENDA,
		"Tagesgrenzen", Messages.ACLContributor_acl_daylimits); //$NON-NLS-1$
	
	/** The right to lock or unlock appointments */
	public static final ACE CHANGE_APPLOCK = new ACE(ADMIN_AGENDA,
		"TerminSperren", Messages.ACLContributor_acl_lockappointments); //$NON-NLS-1$
	
	/**
	 * get the ACE's that should be managed.
	 */
	public ACE[] getACL(){
		return new ACE[] {
			ACE_AGENDA, USE_AGENDA, ADMIN_AGENDA, DISPLAY_APPOINTMENTS, CHANGE_APPOINTMENTS,
			DELETE_APPOINTMENTS, CHANGE_DAYSETTINGS, CHANGE_APPLOCK
		};
	}
	
	/**
	 * react on errors reserving rights
	 */
	public ACE[] reject(final ACE[] acl){
		return null;
	}
	
	/**
	 * On first run of the agenda, set the rights to reasonable defaults
	 */
	public static void initialize(){
		Hub.acl.grant(AccessControl.USER_GROUP, USE_AGENDA);
		Hub.acl.grant(AccessControl.ADMIN_GROUP, ADMIN_AGENDA);
		Hub.acl.flush();
	}
}
