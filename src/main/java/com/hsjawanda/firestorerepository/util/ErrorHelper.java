/**
 *
 */
package com.hsjawanda.firestorerepository.util;

import static com.hsjawanda.firestorerepository.util.Constants.NL;
import static com.hsjawanda.utilities.repackaged.commons.lang3.StringUtils.isNotBlank;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.Nullable;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public final class ErrorHelper {

	private static final int INITIAL_SIZE = 200;

	private ErrorHelper() {
	}

	public static boolean causedBy(Exception e, Class<? extends Exception> checkAgainst) {
		if (null != e && null != checkAgainst) {
			Throwable cause = e.getCause();
			if (cause != null && cause.getClass().equals(checkAgainst))
				return true;
		}
		return false;
	}

	public static String logException(Throwable e, @Nullable String prefix) {
		StringWriter log = new StringWriter(INITIAL_SIZE);
		if (isNotBlank(prefix)) {
			log.append(prefix).append(NL);
		}
		e.printStackTrace(new PrintWriter(log));
		return log.toString();
	}

}
