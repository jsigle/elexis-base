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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.DisposeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.IStaleListener;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.ValueDiff;

import ch.elexis.core.data.IPersistentObject;

/**
 * This is an IObservableValue conforming to JFace Databinding. Its constructor takes
 * IPersistentObject and a field to observe as parameter
 * 
 * @author gerry
 * 
 */
public class PersistentObjectObservableValue implements IObservableValue,
		ch.elexis.core.data.IChangeListener {
	private List<IChangeListener> changeListeners = new LinkedList<IChangeListener>();
	private List<IStaleListener> staleListeners = new LinkedList<IStaleListener>();
	private List<IDisposeListener> disposeListeners = new LinkedList<IDisposeListener>();
	private List<IValueChangeListener> valueListeners = new LinkedList<IValueChangeListener>();
	private boolean bDisposed = false;
	private IPersistentObject myObject;
	private String myField;
	
	/**
	 * Create an IObservableValue
	 * 
	 * @param myObject
	 *            Object from the storage system
	 * @param myField
	 *            property to observe
	 */
	public PersistentObjectObservableValue(IPersistentObject myObject, String myField){
		this.myObject = myObject;
		this.myField = myField;
		this.myObject.addChangeListener(this, myField);
	}
	
	@Override
	public void objectDisposing(IPersistentObject object){
		dispose();
	}
	
	@Override
	public Realm getRealm(){
		return Realm.getDefault();
	}
	
	@Override
	public void addChangeListener(IChangeListener listener){
		if (!changeListeners.contains(listener)) {
			changeListeners.add(listener);
		}
	}
	
	@Override
	public void removeChangeListener(IChangeListener listener){
		if (changeListeners.contains(listener)) {
			changeListeners.remove(listener);
		}
	}
	
	@Override
	public void addStaleListener(IStaleListener listener){
		if (!staleListeners.contains(listener)) {
			staleListeners.add(listener);
		}
	}
	
	@Override
	public void removeStaleListener(IStaleListener listener){
		staleListeners.remove(listener);
	}
	
	@Override
	public boolean isStale(){
		return false;
	}
	
	@Override
	public void addDisposeListener(IDisposeListener listener){
		if (!disposeListeners.contains(listener)) {
			disposeListeners.add(listener);
		}
	}
	
	@Override
	public void removeDisposeListener(IDisposeListener listener){
		disposeListeners.remove(listener);
	}
	
	@Override
	public boolean isDisposed(){
		return bDisposed;
	}
	
	@Override
	public void dispose(){
		bDisposed = true;
		DisposeEvent de = new DisposeEvent(this);
		for (IDisposeListener l : disposeListeners) {
			l.handleDispose(de);
		}
		myObject.removeChangeListener(this, myField);
		myObject = null;
	}
	
	@Override
	public Object getValueType(){
		return String.class;
	}
	
	@Override
	public Object getValue(){
		ObservableTracker.getterCalled(this);
		return myObject.get(myField);
	}
	
	@Override
	public void setValue(final Object value){
		myObject.set(myField, value.toString());
	}
	
	@Override
	public void addValueChangeListener(IValueChangeListener listener){
		if (!valueListeners.contains(listener)) {
			valueListeners.add(listener);
		}
	}
	
	@Override
	public void removeValueChangeListener(IValueChangeListener listener){
		valueListeners.remove(listener);
	}
	
	@Override
	public void valueChanged(final IPersistentObject object, final String field,
		final Object oldValue, final Object newValue){
		ChangeEvent ce = new ChangeEvent(this);
		for (IChangeListener l : changeListeners) {
			l.handleChange(ce);
		}
		ValueDiff diff = new ValueDiff() {
			
			@Override
			public Object getNewValue(){
				return newValue;
			}
			
			@Override
			public Object getOldValue(){
				return oldValue;
			}
			
		};
		ValueChangeEvent ve = new ValueChangeEvent(this, diff);
		for (IValueChangeListener l : valueListeners) {
			l.handleValueChange(ve);
		}
		
	}
	
}
