package ch.elexis.extdoc.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import ch.elexis.extdoc.preferences.PreferenceConstants;

import ch.rgw.tools.StringTool;

public class ListFiles {
	
	/**
	 * Return all external files
	 * 
	 * @param paths
	 *            the path from where to load files; may be null
	 * @param name
	 *            family name of the concerned patient
	 * @param vorname
	 *            name of the concerned patient
	 * @param geburtsDatum
	 *            geburtsDatum of the concerned patient
	 * 
	 * @return a list of files (maybe empty)
	 */
	public static List<File> getList(String[] paths, String name, String vorname,
		String geburtsDatum, FilenameFilter filter){
		{
			List<File> list = new ArrayList<File>();
			/*
			 * Here we load all files in the selected paths and all their sub directories
			 */
			FileFiltersConvention convention = new FileFiltersConvention(name, vorname);
			
			for (String path : paths) {
				if (!StringTool.isNothing(path)) {
					File mainDirectory = new File(path);
					if (mainDirectory.isDirectory()) {
						File[] files = mainDirectory.listFiles(filter);
						if (files != null) {
							for (File file : files) {
								if (file.isFile())
									list.add(file);
							}
						}
						String subDir =
							new String(mainDirectory + File.separator + convention.getShortName())
								+ " " + MatchPatientToPath.geburtsDatumToCanonical(geburtsDatum); //$NON-NLS-1$
						File subDirectory = new File(subDir);
						files = subDirectory.listFiles();
						if (files != null) {
							for (File file : files) {
								list.add(file);
							}
						}
					}
				}
			}
			return list;
		}
	}
}
