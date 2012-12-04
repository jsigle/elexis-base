package ch.elexis.trustx;

import com4j.*;

/**
 * IAsasCollection Interface
 */
@IID("{C8A8842F-3054-11D3-823C-00A024104348}")
public interface IAsasCollection extends Com4jObject, Iterable<Com4jObject> {
	/**
	 * property Count
	 */
	@VTID(7)
	int count();
	
	/**
	 * property Item
	 */
	@VTID(8)
	@DefaultMethod
	@ReturnValue(type = NativeType.VARIANT)
	java.lang.Object item(int idx);
	
	/**
	 * property _NewEnum
	 */
	@VTID(9)
	java.util.Iterator<Com4jObject> iterator();
	
}
