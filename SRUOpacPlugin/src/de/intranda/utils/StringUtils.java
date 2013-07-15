package de.intranda.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Utilities for string processing and formatting
 * 
 * @author florian
 *
 */
public class StringUtils {
	
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(StringUtils.class);
	public static final String lineSeparator = System.getProperty("line.separator");
	
	/**
	 * Splits a multiline String into an array of single lines
	 * 
	 * @param string
	 * @param separator
	 *            String separating the lines, if ==null, lineSeparator will be used,
	 * @return
	 */
	public static ArrayList<String> splitIntoLines(String string, String separator) {
		if (separator == null)
			separator = lineSeparator;

		String[] lineArray = string.split(separator);
		return new ArrayList<String>(Arrays.asList(lineArray));
	}

	/**
	 * Concats a list of Strings into a single String,
	 * 
	 * @param lines
	 * @param separator
	 *            String separating the lines, if ==null, lineSeparator will be used,
	 * @return
	 */
	public static String concatLines(List<String> lines, String separator) {
		if (separator == null) {			
			separator = lineSeparator;
		}
		String result = "";

		for (String line : lines) {
			result = result.concat(line + separator);
		}

		return result.trim();
	}

	/**
	 * Returns a String containing a human readable representation of the current date, and the current time if that parameter is true
	 * 
	 * @param timeOfDay
	 * @return
	 */
	public static String getCurrentDateString(boolean timeOfDay) {
		Calendar cal = GregorianCalendar.getInstance();
		Date date = cal.getTime();

		SimpleDateFormat simpDate;
		if (timeOfDay) {
			simpDate = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");
		} else {
			simpDate = new SimpleDateFormat("dd.MM.yyyy");
		}
		return simpDate.format(date);

		// if(timeOfDay)
		// dateFormat = DateFormat.getInstance();
		// else
		// dateFormat = DateFormat.getDateInstance();
		// return dateFormat.format(date);
	}

	/**
	 * Returns a String containing a human readable representation of the current time, including milliseconds if that parameter is true
	 * 
	 * @param millis
	 * @return
	 */
	public static String getCurrentTimeString(boolean millis) {
		Calendar cal = GregorianCalendar.getInstance();
		Date date = cal.getTime();

		SimpleDateFormat simpDate;
		if (millis) {
			simpDate = new SimpleDateFormat("HH:mm:ss.SSS z");
		} else {
			simpDate = new SimpleDateFormat("HH:mm:ss z");
		}
		return simpDate.format(date);

		// DateFormat dateFormat = DateFormat.getTimeInstance();
		// if (millis) {
		// long ms = date.getTime() % 1000;
		// return dateFormat.format(date) + "." + millisNumberFormat.format(ms);
		// } else {
		// return dateFormat.format(date);
		// }
	}

}
