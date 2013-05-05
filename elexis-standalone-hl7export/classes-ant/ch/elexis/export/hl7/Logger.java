package ch.elexis.export.hl7;

public class Logger {
	private static boolean isDebug = Settings.getCurrent().isDebug();
	
	public static void logInfoLn(){
		System.out.println();
	}
	
	public static void logInfo(final String message){
		System.out.println(message);
	}
	
	public static void logDebug(final String message){
		if (isDebug) {
			System.out.println(message);
		}
	}
	
	public static void logError(final String message, final Throwable t){
		System.err.println(message);
		t.printStackTrace();
	}
}
