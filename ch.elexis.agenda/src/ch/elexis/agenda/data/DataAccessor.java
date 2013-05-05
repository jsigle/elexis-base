/*******************************************************************************
 * Copyright (c) 2007-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    C. Bruegger - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.agenda.data;

import java.util.ArrayList;
import java.util.List;

import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.data.PersistentObject;
import ch.elexis.util.IDataAccess;
import ch.rgw.tools.Result;

/**
 * Access data stored in Termin.
 * 
 * @see ch.elexis.util.IDataAccess
 * @author C.Bruegger
 * 
 */
public class DataAccessor implements IDataAccess {
	
	public DataAccessor(){}
	
	@Override
	public String getName(){
		String str = "Termin";
		return str;
	}
	
	@Override
	public String getDescription(){
		String str = "Daten aus dem Agenda Plugin";
		return str;
	}
	
	/**
	 * Retourniert Platzhalter für die Integration im Textsystem.
	 * 
	 * @return
	 */
	private String getPlatzhalter(final String termin){
		return "[Termin:-:-:" + termin + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Returns a list with the defined fields. Currently only "BeiWem" (Bereich) is supported.
	 */
	public List<Element> getList(){
		ArrayList<Element> ret = new ArrayList<Element>();
		ret.add(new IDataAccess.Element(IDataAccess.TYPE.STRING, "Bereich", //$NON-NLS-1$
			getPlatzhalter(Termin.FLD_BEREICH), Termin.class, 1));
		return ret;
	}
	
	/**
	 * Returns the Object denoted by the given description
	 * 
	 * @param descriptor
	 *            Description of the data
	 * @param dependentObject
	 *            not used
	 * @param dates
	 *            not used
	 * @param params
	 *            not used
	 */
	public Result<Object> getObject(String descriptor, PersistentObject dependentObject,
		String dates, String[] params){
		Result<Object> ret = null;
		
		if (descriptor.equals(Termin.FLD_BEREICH)) {
			PersistentObject selectedTermin =
				(PersistentObject) ElexisEventDispatcher.getSelected(Termin.class);
			ret = new Result<Object>(selectedTermin.get(Termin.FLD_BEREICH));
		} else {
			ret =
				new Result<Object>(Result.SEVERITY.ERROR, IDataAccess.INVALID_PARAMETERS,
					"Ungültiger Parameter", //$NON-NLS-1$
					dependentObject, true);
		}
		return ret;
	}
}