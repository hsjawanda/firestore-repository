/**
 *
 */
package com.hsjawanda.firestorerepository.util;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.hsjawanda.firestorerepository.util.Constants.Time;
import com.hsjawanda.utilities.base.Config;
import com.hsjawanda.utilities.collections.Collections;

import lombok.NonNull;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public final class GCredentials {

	public static final String FIREBASE_SVC_AC_KEY_FILE_NAME = Config.getOrEmpty("project.id")
			+ "-firebase-adminsdk.json";

	public static final String GAE_DEF_SVC_AC_KEY_FILE_NAME = Config.getOrEmpty("project.id") + "-def-app-svc-ac.json";

	private static GoogleCredentials defSvcAc;

	private static GoogleCredentials firestoreServiceAc;

	private static Logger LOG;

	private GCredentials() {
	}

	/**
	 * Creates and/or returns a single instance of a {@link GoogleCredentials} scoped to
	 * {@link GCloud.Scopes.CLOUD_PLATFORM}.
	 *
	 * @return the single instance of the {@code GoogleCredentials} object
	 * @throws IOException
	 */
	public static synchronized GoogleCredentials defaultServiceAc() throws IOException {
		if (null == defSvcAc) {
			defSvcAc = from(IoUtil.getResourceInputStream(GAE_DEF_SVC_AC_KEY_FILE_NAME),
					Arrays.asList(GCloud.Scopes.CLOUD_PLATFORM));
		}
		return defSvcAc;
	}

	/**
	 * Creates a new {@link GoogleCredentials} with the specified scope(s) every time.
	 *
	 * @param scopes the scope(s) to associate with the credential
	 * @return a newly-created {@code GoogleCredentials} object
	 * @throws IOException
	 */
	public static synchronized GoogleCredentials defaultServiceAc(String... scopes) throws IOException {
		return from(IoUtil.getResourceInputStream(GAE_DEF_SVC_AC_KEY_FILE_NAME), Arrays.asList(scopes));
	}

	public static synchronized GoogleCredentials firestoreServiceAc() throws FileNotFoundException, IOException {
		if (null == firestoreServiceAc) {
			firestoreServiceAc = from(IoUtil.getResourceInputStream(FIREBASE_SVC_AC_KEY_FILE_NAME),
					Arrays.asList(GCloud.Scopes.CLOUD_PLATFORM));
		}
		return firestoreServiceAc;
	}

	public static GoogleCredentials from(@NonNull InputStream keyFile, @Nullable Collection<String> scopes)
			throws IOException {
		GoogleCredentials creds = GoogleCredentials.fromStream(keyFile);
		keyFile.close();
		return Collections.isNotEmpty(scopes) ? creds.createScoped(scopes) : creds;
	}

	public static GoogleCredentials from(@NonNull String keyFileName, @Nullable Collection<String> scopes)
			throws IOException {
		return from(IoUtil.getResourceInputStream(keyFileName), scopes);
	}

	/**
	 * Get an {@link AccessToken} from {@code creds}, refreshing the token if necessary.
	 *
	 * @param creds the {@link GoogleCredentials} to get the {@code AccessToken} from
	 * @param debug whether to log debugging information
	 * @return a valid {@code AccessToken}
	 * @throws IOException
	 */
	public static AccessToken token(GoogleCredentials creds, final boolean debug) throws IOException {
		creds.refreshIfExpired();
		AccessToken token = creds.getAccessToken();
		if (debug) {
			log().info("Token valid upto: " + Time.TIMESTAMP.format(token.getExpirationTime().toInstant()));
		}
		return token;
	}

	private static Logger log() {
		if (null == LOG) {
			LOG = LoggerFactory.getLogger(GCredentials.class.getName());
		}
		return LOG;
	}

}
