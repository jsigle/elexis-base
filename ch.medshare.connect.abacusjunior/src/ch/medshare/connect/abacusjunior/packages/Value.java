package ch.medshare.connect.abacusjunior.packages;

import java.util.List;
import java.util.ResourceBundle;

import ch.elexis.data.LabItem;
import ch.elexis.data.LabResult;
import ch.elexis.data.Labor;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.rgw.tools.TimeTool;

public class Value {
	private static final String BUNDLE_NAME =
		"ch.medshare.connect.abacusjunior.packages.valuetexts";
	
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	
	private static String getString(String paramName, String key){
		return RESOURCE_BUNDLE.getString(paramName + "." + key);
	}
	
	public static Value getValue(String paramName){
		return new Value(paramName);
	}
	
	String _shortName;
	String _longName;
	String _unit;
	LabItem _labItem;
	String _refMann;
	String _refFrau;
	
	public String get_shortName(){
		return _shortName;
	}
	
	public String get_longName(){
		return _longName;
	}
	
	Value(String paramName){
		_shortName = getString(paramName, "kuerzel");
		_longName = getString(paramName, "text");
		_unit = getString(paramName, "unit");
		_refMann = getString(paramName, "refM");
		_refFrau = getString(paramName, "refF");
	}
	
	private void initialize(){
		Labor myLab;
		
		Query<Labor> qbe = new Query<Labor>(Labor.class);
		qbe.add("Kuerzel", "LIKE", "%" + Messages.getString("Value.LabKuerzel") + "%");
		List<Labor> list = qbe.execute();
		
		if (list.size() < 1) {
			myLab =
				new Labor(Messages.getString("Value.LabKuerzel"),
					Messages.getString("Value.LabName"));
		} else {
			myLab = list.get(0);
		}
		
		Query<LabItem> qli = new Query<LabItem>(LabItem.class);
		qli.add("kuerzel", "=", _shortName);
		qli.and();
		qli.add("LaborID", "=", myLab.get("ID"));
		
		List<LabItem> itemList = qli.execute();
		if (itemList.size() < 1) {
			_labItem =
				new LabItem(_shortName, _longName, myLab, _refMann, _refFrau, _unit,
					LabItem.typ.NUMERIC, Messages.getString("Value.LabName"), "50");
		} else {
			_labItem = itemList.get(0);
		}
	}
	
	public void fetchValue(Patient patient, String value, String flags, TimeTool date){
		if (_labItem == null) {
			initialize();
		}
		
		String comment = "";
		int resultFlags = 0;
		if (flags.equals("1")) {
			// comment = Messages.getString("Value.High");
			resultFlags |= LabResult.PATHOLOGIC;
		}
		if (flags.equals("2")) {
			// comment = Messages.getString("Value.Low");
			resultFlags |= LabResult.PATHOLOGIC;
		}
		if (flags.equals("*") || flags.equals("E")) {
			comment = Messages.getString("Value.Error");
		}
		
		LabResult lr = new LabResult(patient, date, _labItem, value, comment);
		lr.set("Quelle", Messages.getString("Value.LabKuerzel"));
		lr.setFlag(resultFlags, true);
	}
}