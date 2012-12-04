package ch.elexis.trustx;

import com4j.*;

/**
 * Defines methods to create COM objects
 */
public abstract class ClassFactory {
	private ClassFactory(){} // instanciation is not allowed
	
	/**
	 * Trustx Class
	 */
	public static ch.elexis.trustx.ITrustx createCTrustx(){
		return COM4J.createInstance(ch.elexis.trustx.ITrustx.class,
			"{A40A4257-972B-4D62-80EB-ABA22D9283CD}");
	}
	
	/**
	 * Code Class
	 */
	public static ch.elexis.trustx.ICode createCCode(){
		return COM4J.createInstance(ch.elexis.trustx.ICode.class,
			"{DB574E81-F728-4853-8E9E-291FC4BD22A6}");
	}
}
