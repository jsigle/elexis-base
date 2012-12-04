package ch.elexis.ebanking_ch.command;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.banking.ESRFile;
import ch.elexis.banking.ESRRecord;
import ch.elexis.banking.Messages;
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.elexis.util.Log;
import ch.elexis.util.ResultAdapter;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Money;
import ch.rgw.tools.Result;
import ch.rgw.tools.TimeTool;

public class LoadESRFileHandler extends AbstractHandler implements IElementUpdater {
	
	public static final String COMMAND_ID = "ch.elexis.ebanking_ch.command.loadESRFile";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException{
		FileDialog fld =
			new FileDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), SWT.OPEN);
		fld.setText(Messages.ESRView_selectESR);
		final String filename = fld.open();
		if (filename != null) {
			final ESRFile esrf = new ESRFile();
			final File file = new File(filename);
			try {
				PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new IRunnableWithProgress() {
						
						public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException{
							monitor.beginTask(Messages.ESRView_reading_ESR,
								(int) (file.length() / 25));
							Result<List<ESRRecord>> result = esrf.read(file, monitor);
							if (result.isOK()) {
								for (ESRRecord rec : result.get()) {
									monitor.worked(1);
									if (rec.getRejectCode().equals(ESRRecord.REJECT.OK)) {
										if (rec.getTyp().equals(ESRRecord.MODE.Summenrecord)) {
											Hub.log.log(
												Messages.ESRView_ESR_finished + rec.getBetrag(),
												Log.INFOS);
										} else if ((rec.getTyp().equals(ESRRecord.MODE.Storno_edv))
											|| (rec.getTyp().equals(ESRRecord.MODE.Storno_Schalter))) {
											Rechnung rn = rec.getRechnung();
											Money zahlung = rec.getBetrag().negate();
											rn.addZahlung(zahlung,
												Messages.ESRView_storno_for + rn.getNr() + " / " //$NON-NLS-1$
													+ rec.getPatient().getPatCode(), new TimeTool(
													rec.getValuta()));
											rec.setGebucht(null);
										} else {
											Rechnung rn = rec.getRechnung();
											if (rn.getStatus() == RnStatus.BEZAHLT) {
												if (SWTHelper.askYesNo(Messages.ESRView_paid,
													Messages.ESRView_rechnung + rn.getNr()
														+ Messages.ESRView_ispaid) == false) {
													continue;
												}
											}
											Money zahlung = rec.getBetrag();
											Money offen = rn.getOffenerBetrag();
											if (zahlung.isMoreThan(offen)) {
												if (SWTHelper.askYesNo(Messages.ESRView_toohigh,
													Messages.ESRView_paymentfor + rn.getNr()
														+ Messages.ESRView_morethan) == false) {
													continue;
												}
											}
											
											rn.addZahlung(zahlung,
												Messages.ESRView_vesrfor + rn.getNr() + " / " //$NON-NLS-1$
													+ rec.getPatient().getPatCode(), new TimeTool(
													rec.getValuta()));
											rec.setGebucht(null);
										}
									}
								}
								monitor.done();
							} else {
								ResultAdapter.displayResult(result, Messages.ESRView_errorESR);
							}
						}
						
					});
			} catch (InvocationTargetException e) {
				ExHandler.handle(e);
				SWTHelper.showError(Messages.ESRView_errorESR2, Messages.ESRView_errrorESR2,
					Messages.ESRView_couldnotread + e.getMessage() + e.getCause().getMessage());
			} catch (InterruptedException e) {
				ExHandler.handle(e);
				SWTHelper.showError("ESR interrupted", Messages.ESRView_interrupted, e //$NON-NLS-1$
					.getMessage());
			}
		}
		
		return null;
	}
	
	@Override
	public void updateElement(UIElement element, Map parameters){
		element.setIcon(Desk.getImageDescriptor(Desk.IMG_IMPORT));
		element.setTooltip(Messages.ESRView_read_ESR_explain);
	}
	
}
