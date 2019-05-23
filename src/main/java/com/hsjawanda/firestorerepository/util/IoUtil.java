/**
 *
 */
package com.hsjawanda.firestorerepository.util;

import static com.hsjawanda.utilities.repackaged.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hsjawanda.firestorerepository.Firebase;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public final class IoUtil {

	private static final int INITIAL_SIZE = 512;

	private static Logger LOG;

	private IoUtil() {
	}

	public static String asString(InputStream is) {
		return asString(is, StandardCharsets.UTF_8);
	}

	public static String asString(InputStream is, Charset cs) {
		StringBuilder input = new StringBuilder(INITIAL_SIZE);
		int length;
		char[] chars = new char[INITIAL_SIZE / 4];
		try (InputStreamReader isr = new InputStreamReader(is, cs)) {
			while ((length = isr.read(chars)) != -1) {
				input.append(chars, 0, length);
			}
		} catch (IOException e) {
			log().warn("Error reading from InputStream", e);
		}
		return input.toString();
	}

	private static Logger log() {
		if (null == LOG) {
			LOG = LoggerFactory.getLogger(IoUtil.class.getName());
		}
		return LOG;
	}

	@CheckForNull
	public static InputStream getResourceInputStream(String name) {
		if (isBlank(name))
			return null;
		ClassLoader loader = Firebase.class.getClassLoader();
		return loader.getResourceAsStream(name);
	}

}
