/********************************************************************************
 * Copyright (c) 2011, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *
 *    $Id$
 *******************************************************************************/

package ch.elexis.core.databinding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.Realm;
import org.junit.Test;

import ch.elexis.core.data.IPartner;
import ch.elexis.core.data.IPersistentObjectManager;

/** DOESNT WORK */
public class Test_observables {
	
	@Test
	public void simplePattern() throws Exception{
		
		// create two simple IPersistentObjects
		IPartner p1 = null; // (IPartner) pom.create(IPartner.class, null, true);
		IPartner p2 = null; // (IPartner) pom.create(IPartner.class, null, true);
		
		p1.set(IPartner.FLD_NAME, "Rumpelstilzchen");
		p2.set(IPartner.FLD_NAME, "Schulz");
		p1.set(IPartner.FLD_FIRSTNAME, "Franz");
		p2.set(IPartner.FLD_FIRSTNAME, "Schneewittchen");
		
		// create an IObservable on the property FLD_NAME of each
		// IPersistentIObject
		PersistentObjectObservableValue po1 =
			new PersistentObjectObservableValue(p1, IPartner.FLD_NAME);
		PersistentObjectObservableValue po2 =
			new PersistentObjectObservableValue(p2, IPartner.FLD_NAME);
		
		// Create a DataBindingContext to control information trabsfer
		DataBindingContext dbc = new DataBindingContext(new myRealm());
		
		// Bind the first IObservable to the second. Note that they will
		// synchronize immediately. The second Object will take the
		// observed value from the first
		dbc.bindValue(po1, po2);
		assertEquals(p1.get(IPartner.FLD_NAME), p2.get(IPartner.FLD_NAME));
		assertFalse(p1.get(IPartner.FLD_FIRSTNAME).equals(p2.get(IPartner.FLD_FIRSTNAME)));
		
		// If we change the observed value on the first object, the change is
		// reflected immediately to the second (only the observed field)
		p1.set(IPartner.FLD_NAME, "Meier");
		assertEquals("Meier", p2.get(IPartner.FLD_NAME));
		assertFalse(p1.get(IPartner.FLD_FIRSTNAME).equals(p2.get(IPartner.FLD_FIRSTNAME)));
		
	}
	
	/**
	 * Mock that returns always true, because we let the test run in a single thread.
	 * 
	 * @author gerry
	 * 
	 */
	static final class myRealm extends Realm {
		
		myRealm(){
			setDefault(this);
		}
		
		@Override
		public boolean isCurrent(){
			return true;
		}
		
	}
	
}
