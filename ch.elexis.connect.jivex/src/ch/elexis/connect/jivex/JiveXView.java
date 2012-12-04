package ch.elexis.connect.jivex;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListenerImpl;
import ch.elexis.data.Patient;

public class JiveXView extends ViewPart {
	
	public JiveXView(){
		// TODO Auto-generated constructor stub
	}
	
	ElexisEventListenerImpl eeli_pat = new ElexisEventListenerImpl(Patient.class) {
		
		@Override
		public void runInUi(ElexisEvent ev){
			if (ElexisEventDispatcher.getSelectedPatient() == null) {
				
			}
		}
		
	};
	
	@Override
	public void createPartControl(Composite parent){
		Button button = new Button(parent, SWT.PUSH);
		button.setText("Auftrag...");
		button.addSelectionListener(new SelectionAdapter() {
			Patient pat = ElexisEventDispatcher.getSelectedPatient();
			InputDialog id = new InputDialog(getSite().getShell(), "Röntgenauftrag an JiveX",
				"Bitte geben Sie das gewünschte Bild an", "", null);
			
		});
	}
	
	@Override
	public void setFocus(){
		// TODO Auto-generated method stub
		
	}
	
}
