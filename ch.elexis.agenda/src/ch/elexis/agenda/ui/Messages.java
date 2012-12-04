package ch.elexis.agenda.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.agenda.ui.messages"; //$NON-NLS-1$
	public static String AgendaParallel_dayBack;
	public static String AgendaParallel_dayForward;
	public static String AgendaParallel_selectDay;
	public static String AgendaParallel_setZoomFactor;
	public static String AgendaParallel_showCalendarForSelcetion;
	public static String AgendaParallel_showNextDay;
	public static String AgendaParallel_showPreviousDay;
	public static String AgendaParallel_zoom;
	public static String ColumnHeader_Mandantors;
	public static String ColumnHeader_mandatorsForParallelView;
	public static String ColumnHeader_selectMandators;
	public static String ColumnHeader_selectMandatorToShow;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
