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