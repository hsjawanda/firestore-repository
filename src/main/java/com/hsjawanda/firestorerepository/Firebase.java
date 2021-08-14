/**
 *
 */
package com.hsjawanda.firestorerepository;

import static com.hsjawanda.utilities.base.Check.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.common.base.Stopwatch;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public final class Firebase {

	public static final String AUTH_ERR_ID_TOKEN_REVOKED = "id-token-revoked";

	public static final String KEY_FILE_NAME = "firebase-adminsdk.json";

	private static Firestore DB;

	private static Logger LOG;

	private Firebase() {
	}

	public static synchronized Firestore db() throws IllegalStateException {
		checkState(null != DB, "Call db(GoogleCredentials credsToUse) once before calling this method.");
		return DB;
	}

	public static synchronized Firestore db(GoogleCredentials credsToUse) throws RuntimeException {
		if (null == DB) {
			try {
//				init();
				Stopwatch timer = Stopwatch.createStarted();

				FirestoreOptions options = FirestoreOptions.newBuilder()
						.setCredentials(credsToUse).build();
				DB = options.getService();
				log().info("Time to get Firestore Service: " + timer.stop());
//			} catch (FileNotFoundException e) {
//				log().warn("Exception log:", e);
//				throw new RuntimeException("FileNotFoundException getting Firestore DB.", e);
//			} catch (IOException e) {
//				log().warn("Exception log:", e);
//				throw new RuntimeException("IOException getting Firestore DB.", e);
			} catch (Exception e) {
				throw new RuntimeException("Error creating Firestore service.", e);
			}
		}
		return DB;
	}

	private static Logger log() {
		if (null == LOG) {
			LOG = LoggerFactory.getLogger(Firebase.class.getName());
		}
		return LOG;
	}

}
