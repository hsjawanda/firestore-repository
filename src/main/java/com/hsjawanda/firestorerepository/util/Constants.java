/**
 *
 */
package com.hsjawanda.firestorerepository.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public final class Constants {

	private Constants() {
	}

	public static final String NL = System.lineSeparator();

	public static final class Time {

		public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

		public static final DateTimeFormatter DATE_ONLY = new DateTimeFormatterBuilder().parseCaseInsensitive()
				.appendPattern("dd MMM yyyy").toFormatter().withZone(Time.IST);

		public static final DateTimeFormatter DAY = new DateTimeFormatterBuilder().parseCaseInsensitive()
				.appendPattern("EEE, dd MMM yyyy").toFormatter().withZone(Time.IST);

		public static final DateTimeFormatter INDIAN_DATETIME = new DateTimeFormatterBuilder().parseCaseInsensitive()
				.appendPattern("yyyy-MM-dd HH:mm:ss").toFormatter().withZone(Time.IST);

		public static final DateTimeFormatter PAYTM_DATETIME = new DateTimeFormatterBuilder().parseCaseInsensitive()
				.appendPattern("yyyy-MM-dd HH:mm:ss.S").toFormatter().withZone(Time.IST);

		public static final DateTimeFormatter SHORT_TIME = new DateTimeFormatterBuilder().parseCaseInsensitive()
				.appendPattern("HH:mm:ss").toFormatter().withZone(Time.IST);

		public static final DateTimeFormatter TIME_ONLY = new DateTimeFormatterBuilder().parseCaseInsensitive()
				.appendPattern("HH:mm:ss.SSS").toFormatter().withZone(Time.IST);

		public static final DateTimeFormatter TIMESTAMP = new DateTimeFormatterBuilder().parseCaseInsensitive()
				.appendPattern("dd MMM yyyy HH:mm:ss.SSS").toFormatter().withZone(Time.IST);

		private Time() {
		}

	}

}
