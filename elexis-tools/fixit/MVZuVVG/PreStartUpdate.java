package ch.elexis;

import ch.elexis.data.Fall;
import ch.elexis.data.Query;

public class PreStartUpdate {
	
	public PreStartUpdate(){
		Query<Fall> qbe = new Query<Fall>(Fall.class);
		for (Fall fall : qbe.execute()) {
			if (fall.getAbrechnungsSystem().equals("MV")) {
				fall.setAbrechnungsSystem("VVG");
			}
		}
	}
}
