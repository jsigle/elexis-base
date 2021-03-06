package ch.elexis.hl7.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class HL7Helper {
	private static final String DTM_DATE_TIME_PATTERN = "yyyyMMddHHmmss"; //$NON-NLS-1$
	
	/**
	 * Transformiert einen HL7 Date/Time String in ein java.util.Date
	 * 
	 * @param dateTimeStr
	 * @return java.util.Date
	 */
	public static Date stringToDate(final String dateTimeStr) throws ParseException{
		if (dateTimeStr == null || dateTimeStr.length() == 0) {
			return null;
		}
		SimpleDateFormat sdf =
			new SimpleDateFormat(DTM_DATE_TIME_PATTERN.substring(0, dateTimeStr.length()));
		return sdf.parse(dateTimeStr);
	}
	
	/**
	 * Transformiert java.util.Date in ein HL7 String
	 * 
	 * @param date
	 * @return
	 */
	public static String dateToString(final Date date){
		if (date == null) {
			return null;
		}
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		String pattern = DTM_DATE_TIME_PATTERN;
		if (cal.get(Calendar.SECOND) == 0) {
			pattern.substring(0, pattern.length() - 2);
		}
		if (cal.get(Calendar.MINUTE) == 0) {
			pattern.substring(0, pattern.length() - 2);
		}
		if (cal.get(Calendar.HOUR) == 0) {
			pattern.substring(0, pattern.length() - 2);
		}
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(cal.getTime());
	}
}
