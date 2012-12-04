package ch.elexis.trustx;

import com4j.*;

/**
 * ICode Interface
 */
@IID("{2468C9B7-297A-48B2-8BE0-A2B6549D0351}")
public interface ICode extends Com4jObject {
	/**
	 * property Patient
	 */
	@VTID(7)
	java.lang.String patient(@MarshalAs(NativeType.VARIANT) java.lang.Object id);
	
	/**
	 * property Person
	 */
	@VTID(8)
	java.lang.String person(java.lang.String firstName, java.lang.String lastName,
		java.util.Date birthDate, @MarshalAs(NativeType.VARIANT) java.lang.Object zip);
	
	/**
	 * property Soundex
	 */
	@VTID(9)
	java.lang.String soundex(java.lang.String string);
	
	@VTID(10)
	@ReturnValue(type = NativeType.VARIANT)
	java.lang.Object encrypt(com4j.Com4jObject pUnk);
	
}
