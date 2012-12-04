package ch.elexis.views;

import org.eclipse.jface.viewers.StructuredViewer;

import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListenerImpl;
import ch.elexis.data.Konsultation;
import ch.elexis.data.PersistentObject;
import ch.elexis.selectors.FieldDescriptor;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.SelectorPanelProvider;
import ch.rgw.tools.TimeTool;

public class TarmedSelectorPanelProvider extends SelectorPanelProvider {
	private CommonViewer commonViewer;
	private StructuredViewer viewer;
	
	private TarmedValidDateFilter validDateFilter = new TarmedValidDateFilter();
	private FilterKonsultationListener konsFilter = new FilterKonsultationListener(
		Konsultation.class);
	
	public TarmedSelectorPanelProvider(CommonViewer cv,
		FieldDescriptor<? extends PersistentObject>[] fields, boolean bExlusive){
		super(fields, bExlusive);
		commonViewer = cv;
	}
	
	@Override
	public void setFocus(){
		super.setFocus();
		if (viewer == null) {
			viewer = commonViewer.getViewerWidget();
			viewer.addFilter(validDateFilter);
			ElexisEventDispatcher.getInstance().addListeners(konsFilter);
			// call with null, event is not used in listener impl.
			konsFilter.catchElexisEvent(null);
		}
	}
	
	private class FilterKonsultationListener extends ElexisEventListenerImpl {
		
		public FilterKonsultationListener(Class<?> clazz){
			super(clazz);
		}
		
		@Override
		public void runInUi(ElexisEvent ev){
			Konsultation selectedKons =
				(Konsultation) ElexisEventDispatcher.getSelected(Konsultation.class);
			// apply the filter
			if (selectedKons != null) {
				validDateFilter.setValidDate(new TimeTool(selectedKons.getDatum()));
				viewer.getControl().setRedraw(false);
				viewer.refresh();
				viewer.getControl().setRedraw(true);
			} else {
				validDateFilter.setValidDate(null);
				viewer.getControl().setRedraw(false);
				viewer.refresh();
				viewer.getControl().setRedraw(true);
			}
		}
	}
}
