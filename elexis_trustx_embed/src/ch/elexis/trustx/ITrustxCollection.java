package ch.elexis.trustx;

import com4j.*;

/**
 * ITrustxCollection Interface
 */
@IID("{8CE4FC1F-BA07-4CF6-AF6B-FE1F9B527456}")
public interface ITrustxCollection extends Com4jObject, Iterable<Com4jObject> {
	@VTID(7)
	java.util.Iterator<Com4jObject> iterator();
	
	@VTID(8)
	@DefaultMethod
	java.lang.String item(int index);
	
	@VTID(9)
	int count();
	
}
