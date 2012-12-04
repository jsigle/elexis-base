package ch.elexis.notes;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.notes.messages"; //$NON-NLS-1$
	public static String AddLinkDialog_addLinkDialogMessage;
	public static String AddLinkDialog_addLinkDialogTitle;
	public static String AddLinkDialog_searchCaption;
	public static String NotesDetail_couldNotLaunch;
	public static String NotesDetail_deleteActionCaption;
	public static String NotesDetail_deleteActionToolTip;
	public static String NotesDetail_deleteConfirmCaption;
	public static String NotesDetail_deleteConfirmMessage;
	public static String NotesDetail_newActionCaption;
	public static String NotesDetail_newActionToolTip;
	public static String NotesDetail_xrefs;
	public static String NotesList_searchLabel;
	public static String NotesView_badBaseDirectoryMessage;
	public static String NotesView_badBaseDirectoryTitle;
	public static String NotesView_categories;
	public static String NotesView_createMainCategoryDlgMessage;
	public static String NotesView_createMainCategoryDlgTitle;
	public static String NotesView_createMainCategoryTootltip;
	public static String NotesView_deleteActionCaption;
	public static String NotesView_deleteActionTooltip;
	public static String NotesView_deleteConfirmDlgMessage;
	public static String NotesView_deleteConfirmDlgTitle;
	public static String NotesView_importDocDlgMessage;
	public static String NotesView_importDocuDlgTitle;
	public static String NotesView_importErrorDlgTitle;
	public static String NotesView_importErrorTitle;
	public static String NotesView_newCategory;
	public static String NotesView_newNoteCaption;
	public static String NotesView_newNoteDlgMessage;
	public static String NotesView_newNoteDlgTitle;
	public static String NotesView_newNoteTooltip;
	public static String NotesView_scabCaption;
	public static String NotesView_scanTooltip;
	public static String Preferences_basedir;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
