package ch.elexis.notes;

import org.eclipse.swt.SWT;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class NotesPerspective implements IPerspectiveFactory {
	
	public void createInitialLayout(IPageLayout layout){
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		layout.setFixed(true);
		layout.addView(NotesView.ID, SWT.RIGHT, 0.9f, editorArea);
		
	}
	
}
