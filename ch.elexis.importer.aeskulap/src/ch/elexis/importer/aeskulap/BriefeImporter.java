package ch.elexis.importer.aeskulap;

import java.io.File;
import java.io.IOException;

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
public class BriefeImporter {
	public static final String CATEGORY_AESKULAP_BRIEFE = "Aeskulap-Briefe";
	
	File dir;
	IDocumentManager dm;
	ExcelWrapper hofs;
	
	public BriefeImporter(File importBaseDir, IProgressMonitor monitor){
		monitor.subTask("Importiere Briefe");
		Object os = Extensions.findBestService(GlobalServiceDescriptors.DOCUMENT_MANAGEMENT);
		dir = importBaseDir;
		if (os != null) {
			dm = (IDocumentManager) os;
			hofs = AeskulapImporter.checkImport(dir + File.separator + "briefe.xls");
			if (hofs != null) {
				dm.addCategorie(CATEGORY_AESKULAP_BRIEFE);
				importDocs(hofs, monitor);
			}
		}
	}
	
	private boolean importDocs(final ExcelWrapper hofs, final IProgressMonitor moni){
		float last = hofs.getLastRow();
		float first = hofs.getFirstRow();
		hofs.setFieldTypes(new Class[] {
			Integer.class, Integer.class, Integer.class, String.class, String.class
		});
		int perLine = Math.round(AeskulapImporter.MONITOR_PERTASK / (last - first));
		for (int line = Math.round(first + 1); line <= last; line++) {
			String[] actLine = hofs.getRow(line).toArray(new String[0]);
			String patno = StringTool.getSafe(actLine, 0);
			String briefno = StringTool.getSafe(actLine, 1);
			String date = StringTool.getSafe(actLine, 2);
			String title = StringTool.getSafe(actLine, 3);
			Patient pat = (Patient) Xid.findObject(AeskulapImporter.PATID, patno);
			if (pat != null) {
				File file =
					AeskulapImporter.findFile(new File(dir, "Briefe"), new StringBuilder("!")
						.append(patno).append("_").append(briefno).toString());
				if (file != null) {
					try {
						dm.addDocument(new GenericDocument(pat, title, CATEGORY_AESKULAP_BRIEFE,
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
