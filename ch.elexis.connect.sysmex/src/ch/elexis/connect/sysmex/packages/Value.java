package ch.elexis.connect.sysmex.packages;

import java.util.List;
import java.util.ResourceBundle;

import ch.elexis.data.LabItem;
import ch.elexis.data.LabResult;
import ch.elexis.data.Labor;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.rgw.tools.TimeTool;

public class Value {
	private static final String KX21_BUNDLE_NAME =
		"ch.elexis.connect.sysmex.packages.valuetexts_KX21"; //$NON-NLS-1$
	private static final String KX21N_BUNDLE_NAME =
		"ch.elexis.connect.sysmex.packages.valuetexts_KX21N"; //$NON-NLS-1$
	private static final String POCH_BUNDLE_NAME =
		"ch.elexis.connect.sysmex.packages.valuetexts_pocH"; //$NON-NLS-1$
	
	private final ResourceBundle _bundle;
	String _shortName;
	String _longName;
	String _unit;
	LabItem _labItem;
	String _refMann;
	String _refFrau;
	
	public static Value getValueKX21(final String paramName) throws PackageException{
		return new Value(paramName, KX21_BUNDLE_NAME);
	}
	
	public static Value getValueKX21N(final String paramName) throws PackageException{
		return new Value(paramName, KX21N_BUNDLE_NAME);
	}
	
	public static Value getValuePOCH(final String paramName) throws PackageException{
		return new Value(paramName, POCH_BUNDLE_NAME);
	}
	
	private Value(final String paramName, final String bundleName) throws PackageException{
		_bundle = ResourceBundle.getBundle(bundleName);
		_shortName = getString(paramName, "kuerzel"); //$NON-NLS-1$
		_longName = getString(paramName, "text"); //$NON-NLS-1$
		_unit = getString(paramName, "unit"); //$NON-NLS-1$
		_refMann = getString(paramName, "refM");//$NON-NLS-1$
		_refFrau = getString(paramName, "refF");//$NON-NLS-1$
	}
	
	private void initialize(){
		Labor myLab;
		
		Query<Labor> qbe = new Query<Labor>(Labor.class);
		qbe.add("Kuerzel", "LIKE", "%" + Messages.getString("Value.LabKuerzel") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			+ "%"); //$NON-NLS-1$
		List<Labor> list = qbe.execute();
		
		if (list.size() < 1) {
			myLab = new Labor(Messages.getString("Value.LabKuerzel"), Messages //$NON-NLS-1$
				.getString("Value.LabName")); //$NON-NLS-1$
		} else {
			myLab = list.get(0);
		}
		
		Query<LabItem> qli = new Query<LabItem>(LabItem.class);
		qli.add("kuerzel", "=", _shortName); //$NON-NLS-1$ //$NON-NLS-2$
		qli.and();
		qli.add("LaborID", "=", myLab.get("ID")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		List<LabItem> itemList = qli.execute();
		if (itemList.size() < 1) {
			_labItem =
				new LabItem(_shortName, _longName, myLab, _refMann, _refFrau, _unit,
					LabItem.typ.NUMERIC, Messages.getString("Value.LabName"), "50"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			_labItem = itemList.get(0);
		}
	}
	
	public void fetchValue(Patient patient, String value, String flags, TimeTool date){
		if (_labItem == null) {
			initialize();
		}
		
		String comment = ""; //$NON-NLS-1$
		int resultFlags = 0;
		if (flags.equals("1")) { //$NON-NLS-1$
			// comment = Messages.getString("Value.High");
			resultFlags |= LabResult.PATHOLOGIC;
		}
		if (flags.equals("2")) { //$NON-NLS-1$
			// comment = Messages.getString("Value.Low");
			resultFlags |= LabResult.PATHOLOGIC;
		}
		if (flags.equals("*") || flags.equals("E")) { //$NON-NLS-1$ //$NON-NLS-2$
			comment = Messages.getString("Value.Error"); //$NON-NLS-1$
		}
		
		LabResult lr = new LabResult(patient, date, _labItem, value, comment);
		lr.set("Quelle", Messages.getString("Value.LabKuerzel")); //$NON-NLS-1$ //$NON-NLS-2$
		lr.setFlag(resultFlags, true);
	}
	
	public String get_shortName(){
		return _shortName;
	}
	
	public String get_longName(){
		return _longName;
	}
	
	private String getString(String paramName, String key){
		return _bundle.getString(paramName + "." + key); //$NON-NLS-1$
	}
}