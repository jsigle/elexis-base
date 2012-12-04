package ch.elexis.medikamente.bag.data;

import java.util.List;

import ch.elexis.data.IVerrechenbar;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Verrechnet;
import ch.elexis.util.IOptifier;
import ch.rgw.tools.Result;

public class BAGOptifier implements IOptifier {
	
	public Result<Object> optify(final Konsultation kons){
		return new Result<Object>(kons);
	}
	
	public Result<IVerrechenbar> add(final IVerrechenbar code, final Konsultation kons){
		if (code instanceof BAGMedi) {
			List<Verrechnet> old = kons.getLeistungen();
			for (Verrechnet v : old) {
				IVerrechenbar vv = v.getVerrechenbar();
				if (vv.getCode().equals(code.getCode())) {
					v.changeAnzahl(v.getZahl() + 1);
					// v.setZahl(v.getZahl()+1);
					return new Result<IVerrechenbar>(code);
				}
				if (vv instanceof BAGMedi) {
					BAGMedi bm = (BAGMedi) vv;
					
				}
			}
			old.add(new Verrechnet(code, kons, 1));
			
		}
		return new Result<IVerrechenbar>(code);
	}
	
	public Result<Verrechnet> remove(final Verrechnet v, final Konsultation kons){
		List<Verrechnet> old = kons.getLeistungen();
		old.remove(v);
		v.delete();
		return new Result<Verrechnet>(null);
	}
	
}
