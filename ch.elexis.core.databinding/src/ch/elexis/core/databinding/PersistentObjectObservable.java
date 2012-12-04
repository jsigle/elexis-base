package ch.elexis.core.databinding;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.IStaleListener;
import org.eclipse.core.databinding.observable.Realm;

public class PersistentObjectObservable implements IObservable {
	
	@Override
	public Realm getRealm(){
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void addChangeListener(IChangeListener listener){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void removeChangeListener(IChangeListener listener){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addStaleListener(IStaleListener listener){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void removeStaleListener(IStaleListener listener){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean isStale(){
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void addDisposeListener(IDisposeListener listener){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void removeDisposeListener(IDisposeListener listener){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean isDisposed(){
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void dispose(){
		// TODO Auto-generated method stub
		
	}
	
}
