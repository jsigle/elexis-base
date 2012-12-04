package ch.elexis.agenda.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

public class ExportCommand extends AbstractHandler {
	public static final String ID = "ch.elexis.agenda.commands.export"; //$NON-NLS-1$
	
	public Object execute(ExecutionEvent event) throws ExecutionException{
		// TODO Auto-generated method stub
		return null;
	}
	
	public static Object ExecuteWithParams(IViewSite origin, String bereich, String from,
		String until){
		IHandlerService handlerService = (IHandlerService) origin.getService(IHandlerService.class);
		ICommandService cmdService = (ICommandService) origin.getService(ICommandService.class);
		try {
			Command command = cmdService.getCommand(ID);
			Parameterization px1 =
				new Parameterization(
					command.getParameter("ch.elexis.agenda.param.resource"), bereich); //$NON-NLS-1$
			
			Parameterization px2 =
				new Parameterization(command.getParameter("ch.elexis.agenda.param.from"), from); //$NON-NLS-1$
			Parameterization px3 =
				new Parameterization(command.getParameter("ch.elexis.agenda.param.until"), until); //$NON-NLS-1$
			ParameterizedCommand parmCommand =
				new ParameterizedCommand(command, new Parameterization[] {
					px1, px2, px3
				});
			
			return handlerService.executeCommand(parmCommand, null);
			
		} catch (Exception ex) {
			throw new RuntimeException(" export command not found"); //$NON-NLS-1$
		}
	}
}
