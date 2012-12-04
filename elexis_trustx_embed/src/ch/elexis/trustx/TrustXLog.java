/*******************************************************************************
 * Copyright (c) 2006, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id$
 *******************************************************************************/

package ch.elexis.trustx;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;

/**
 * Logfile analyzer for TrustX
 * 
 * @author Gerry
 * 
 */
public class TrustXLog {
	// constansts for field offsets and meanings in the TrustX-Logfile
	private static final int OFFSET_MSG_LEVEL = 1; // (0 debug 1 info 2 warning 3 error 4 fatal)
	private static final int MSG_LEVEL_DEBUG = 0;
	private static final int MSG_LEVEL_INFO = 1;
	private static final int MSG_LEVEL_WARNING = 2;
	private static final int MSG_LEVEL_ERROR = 3;
	private static final int MSG_LEVEL_FATAL = 4;
	
	private static final int OFFSET_MSG_CLASS = 2; // 0: OS, 1 ASAS, 2 TX, 3 counter
	private static final int MSG_CLASS_OS = 0;
	private static final int MSG_CLASS_ASAS = 1;
	private static final int MSG_CLASS_TRUSTX = 2;
	private static final int MSG_CLASS_COUNTER = 3;
	
	private static final int OFFSET_MSG_ORIGIN = 3;
	private static final int MSG_ORIGIN_READER = 0;
	private static final int MSG_ORIGIN_CHECKER = 1;
	private static final int MSG_ORIGIN_ANONYMIZER = 2;
	private static final int MSG_ORIGIN_ENCRYPTER = 3;
	private static final int MSG_ORIGIN_SENDER = 4;
	private static final int MSG_ORIGIN_GATE_RECEIVER = 5;
	private static final int MSG_ORIGIN_GATE_CHECKER = 6;
	
	// Constants for TrustX's Messages
	private static final int OFFSET_MSG_MESSAGE = 4;
	static final int MSG_INFO_MODULE_START = 0;
	static final int MSG_INFO_MODULE_DONE = 1;
	static final int MSG_INFO_MOVED = 2;
	static final int MSG_WARN_NOT_MOVED = 3;
	static final int MSG_WARN_NO_FILES = 4;
	static final int MSG_WARN_MODULE_USERBREAK = 5;
	static final int MSG_ERR_CONF_INPUTDIR = 6;
	static final int MSG_ERR_CONF_WORKDIR = 7;
	static final int MSG_ERR_CONF_ASASLOGIN = 8;
	static final int MSG_ERR_CONF_TRUSTCENTER = 9;
	static final int MSG_ERR_UNKNOWN_FILE_FORMAT = 10;
	static final int MSG_ERR_MISSING_DATA = 11;
	static final int MSG_ERR_DUPLICATE_INVOICE = 12;
	static final int MSG_ERR_PDF_MISSING = 13;
	static final int MSG_ERR_PDF_UNREF = 14;
	static final int MSG_ERR_ANONENC_FAILED = 15;
	static final int MSG_ERR_CONNECTION_FAILED = 16;
	static final int MSG_ERR_SEND_FAILED = 17;
	static final int MSG_FATAL_ASAS_NOT_FOUND = 18;
	
	static final int MSG_FATAL_COULDNOTREADLOG = 20;
	
	File logfile;
	private String[] lines;
	private int line;
	private int lastErrorCode;
	private String lastErrorString;
	
	TrustXLog(String filename){
		logfile = new File(filename);
		line = 0;
	}
	
	boolean init(){
		try {
			String[] lines = readLines();
			if (lines != null) {
				line = lines.length;
			}
		} catch (IOException ex) {
			ExHandler.handle(ex);
			
		}
		return false;
	}
	
	boolean read(){
		try {
			long timeout = System.currentTimeMillis() + 20000;
			while (true) { // Wait until trustx finished the job
				if (System.currentTimeMillis() > timeout) { // wait not longer than "timeout"
					SWTHelper.alert("Trustx-Fehler: Timeout",
						"Empfange keine Rückmeldung von Trustx");
					return false;
				}
				lines = readLines();
				if (lines == null) {
					Thread.sleep(100);
					continue;
				}
				if (!isFinished(lines)) {
					Thread.sleep(100);
					continue;
				}
				break;
			} // Then analyze the result
			lastErrorCode = 0;
			StringBuilder sb = new StringBuilder();
			for (int l = line; l < lines.length - 1; l++) {
				String[] fields = lines[l].split("\\|");
				if (fields.length != 7) {
					continue;
				}
				int level = Integer.parseInt(fields[OFFSET_MSG_LEVEL]);
				if (level > 1) {
					sb.append(fields[6]).append("\n");
				}
				if (level > 2) {
					lastErrorString = sb.toString();
					lastErrorCode = Integer.parseInt(fields[OFFSET_MSG_MESSAGE]);
				}
			}
			return (lastErrorCode == 0);
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
	}
	
	private String[] readLines() throws IOException{
		if (!logfile.exists()) {
			return null;
		}
		FileReader fr = new FileReader(logfile);
		char[] buf = new char[(int) logfile.length()];
		if (fr.read(buf) != buf.length) {
			// log.log("Fehler beim Einlesen des trustx-Logfiles", Log.ERRORS);
			lastErrorCode = MSG_FATAL_COULDNOTREADLOG;
			return null;
		}
		String file = new String(buf).trim();
		String[] lines = file.split("[\\r\\n]");
		return lines;
		
	}
	
	int getLastError(){
		return lastErrorCode;
	}
	
	String getLastErrorString(){
		return lastErrorString;
	}
	
	boolean isFinished(String[] lines){
		if (lines.length <= line) {
			return false;
		}
		String last = lines[lines.length - 1].trim();
		String[] fields = last.split("\\|");
		if (fields.length != 7) {
			return false;
		}
		if (fields[OFFSET_MSG_LEVEL].equals("1") && fields[OFFSET_MSG_CLASS].equals("2")
			&& fields[OFFSET_MSG_ORIGIN].equals("4") && fields[OFFSET_MSG_MESSAGE].equals("1")) {
			return true;
		}
		return false;
	}
	
}
