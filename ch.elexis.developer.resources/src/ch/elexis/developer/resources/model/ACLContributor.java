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
 *    $Id: ACLContributor.java 6116 2010-02-11 21:40:19Z rgw_ch $
 *******************************************************************************/
package ch.elexis.developer.resources.model;

import ch.elexis.admin.ACE;
import ch.elexis.admin.IACLContributor;

/**
 * Add this Plugin to the Elexis access rights system.
 * 
 * Note: This class must be declared at the Extension point ch.elexis.ACLContribution
 * 
 * @author gerry
 * 
 */
public class ACLContributor implements IACLContributor {
	private static final String SAMPLE_ACE_TRANSLATABLE_NODE_NAME =
		Messages.ACLContributor_SampleDataTypeAccess;
	private static final String SAMPLE_ACE_READ = Messages.ACLContributor_ReadSampleDataTypes;
	private static final String SAMPLE_ACE_CREATE = Messages.ACLContributor_CreateSampleDataTypes;
	private static final String SAMPLE_ACE_MODIFY = Messages.ACLContributor_ModifySampleDataTypes;
	private static final String SAMPLE_ACE_DELETE = Messages.ACLContributor_DeleteSampleDataTypes;
	
	/** The Node for our ACE's */
	public static final ACE MY_NODE = new ACE(ACE.ACE_ROOT, Activator.PLUGIN_ID,
		SAMPLE_ACE_TRANSLATABLE_NODE_NAME);
	/**
	 * The right to read SampleDataTypes
	 */
	public static final ACE ReadSDT = new ACE(MY_NODE,
		Activator.PLUGIN_ID + "_readSDT", SAMPLE_ACE_READ); //$NON-NLS-1$
	
	public static final ACE ModifySDT = new ACE(MY_NODE,
		Activator.PLUGIN_ID + "_modifySDT", SAMPLE_ACE_MODIFY); //$NON-NLS-1$
	
	public static final ACE CreateSDT = new ACE(MY_NODE,
		Activator.PLUGIN_ID + "_createSDT", SAMPLE_ACE_CREATE); //$NON-NLS-1$
	
	public static final ACE DeleteSDT = new ACE(MY_NODE,
		Activator.PLUGIN_ID + "_deleteSDT", SAMPLE_ACE_DELETE); //$NON-NLS-1$
	
	/**
	 * We insert our ACEs to the Elexis AccessControl System
	 */
	public ACE[] getACL(){
		return new ACE[] {
			MY_NODE, ReadSDT, ModifySDT, CreateSDT, DeleteSDT
		};
	}
	
	/**
	 * Here we do not react on rejects of our ACL. There should not be rejection anyway, since we
	 * used our plugin ID as prefix of our ACE Names, and we did not make any fault in defining our
	 * ACE's. If the framework would reject our ACL, since we do not return an alternative,our ACL
	 * would not be switched on at all.
	 */
	public ACE[] reject(ACE[] acl){
		return null;
	}
	
}
