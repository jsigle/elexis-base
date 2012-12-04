package ch.elexis.importer.aeskulap;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;

import ch.elexis.ElexisException;
import ch.elexis.data.Patient;
import ch.elexis.data.Xid;
import ch.elexis.importers.ExcelWrapper;
import ch.elexis.services.GlobalServiceDescriptors;
import ch.elexis.services.IDocumentManager;
import ch.elexis.text.GenericDocument;
import ch.elexis.util.Extensions;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;

/** Import Documents from Aeskulap into Omnivore */
public class DokuImporter {
	public static final String CATEGORY_AESKULAP_DOKUMENTE = "Aeskulap-Dokumente";
	
	File dir;
	IDocumentManager dm;
	ExcelWrapper hofs;
	
	public DokuImporter(File importBaseDir, IProgressMonitor monitor){
		monitor.subTask("Importiere Dokumente");
		Object os = Extensions.findBestService(GlobalServiceDescriptors.DOCUMENT_MANAGEMENT);
		dir = importBaseDir;
		if (os != null) {
			dm = (IDocumentManager) os;
			hofs = AeskulapImporter.checkImport(dir + File.separator + "dokumente.xls");
			if (hofs != null) {
				dm.addCategorie(CATEGORY_AESKULAP_DOKUMENTE);
				importDocs(hofs, monitor);
			}
		}
	}
	
	private boolean importDocs(final ExcelWrapper hofs, final IProgressMonitor moni){
		float last = hofs.getLastRow();
		float first = hofs.getFirstRow();
		hofs.setFieldTypes(new Class[] {
			Integer.class, String.class, Integer.class, String.class, String.class
		});
		int perLine = Math.round(AeskulapImporter.MONITOR_PERTASK / (last - first));
		for (int line = Math.round(first + 1); line <= last; line++) {
			String[] actLine = hofs.getRow(line).toArray(new String[0]);
			String patno = StringTool.getSafe(actLine, 0);
			String konsid = StringTool.getSafe(actLine, 1);
			String docno = StringTool.getSafe(actLine, 2);
			String date = StringTool.getSafe(actLine, 3);
			String title = StringTool.getSafe(actLine, 4);
			Patient pat = (Patient) Xid.findObject(AeskulapImporter.PATID, patno);
			if (pat != null) {
				File file =
					AeskulapImporter.findFile(new File(dir, "Dokumente"), new StringBuilder("Doc_")
						.append(patno).append("_").append(docno).toString());
				if (file != null) {
					try {
						dm.addDocument(new GenericDocument(pat, title, CATEGORY_AESKULAP_DOKUMENTE,
							file, date, "", null));
					} catch (Exception e) {
						ExHandler.handle(e);
					}
				}
			}
			
			moni.worked(perLine);
			if (moni.isCanceled()) {
				return false;
			}
		}
		return true;
	}
	
}
