package ch.ngiger.comm.ftp;

public class FtpSemaException extends Exception {
	private static final long serialVersionUID = -2150109019599639291L;
	
	public FtpSemaException(String arg0){
		super(arg0, null);
	}
	
	public FtpSemaException(Throwable cause){
		super(cause);
	}
	
}
