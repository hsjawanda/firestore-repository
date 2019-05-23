/**
 *
 */
package com.hsjawanda.firestorerepository;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.common.base.Stopwatch;
import com.hsjawanda.firestorerepository.util.GCredentials;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public final class Firebase {

	public static final String AUTH_ERR_ID_TOKEN_REVOKED = "id-token-revoked";

	public static final String KEY_FILE_NAME = "firebase-adminsdk.json";

//	private static FirebaseAuth AUTH;

	private static Firestore DB;

//	private static FirebaseApp DEF_APP;

	private static Logger LOG;

	private Firebase() {
	}

//	public static FirebaseApp app() {
//		if (null == DEF_APP) {
//			init();
//		}
//		return DEF_APP;
//	}

//	public static FirebaseAuth auth() {
//		if (null == AUTH) {
//			AUTH = FirebaseAuth.getInstance(app());
//		}
//		return AUTH;
//	}

//	public static synchronized GoogleCredentials credentials() throws IOException {
//		if (null == creds) {
//			creds = GCredentials.firestoreServiceAc();
//		}
//		return creds;
//	}

	public static synchronized Firestore db() throws RuntimeException {
		if (null == DB) {
			try {
//				init();
				Stopwatch timer = Stopwatch.createStarted();

				FirestoreOptions options = FirestoreOptions.newBuilder()
						.setCredentials(GCredentials.firestoreServiceAc()).build();
				DB = options.getService();
				log().info("Time to get Firestore Service: " + timer.stop());
			} catch (FileNotFoundException e) {
				log().warn("Exception log:", e);
				throw new RuntimeException("FileNotFoundException getting Firestore DB.", e);
			} catch (IOException e) {
				log().warn("Exception log:", e);
				throw new RuntimeException("IOException getting Firestore DB.", e);
			}
		}
		return DB;
	}

//	/**
//	 * Is idempotent.
//	 *
//	 * @throws RuntimeException
//	 */
//	public static synchronized void init() throws RuntimeException {
//		if (null == DEF_APP) {
//			Stopwatch timer = Stopwatch.createStarted();
//			try {
//				FirebaseOptions options = new FirebaseOptions.Builder()
//						.setCredentials(GCredentials.firestoreServiceAc()).build();
//				DEF_APP = FirebaseApp.initializeApp(options);
//				log().info("Time to initialize FirebaseApp with Firebase credentials for project '" + DEF_APP.getName()
//						+ "': " + timer.stop());
//			} catch (IOException e) {
//				final String msg = "Exception getting application default credentials.";
//				log().info("Initializing FirebaseApp failed. Time taken: " + timer.stop());
//				log().warn(msg, e);
//				throw new RuntimeException(msg, e);
//			}
//		}
//	}

	private static Logger log() {
		if (null == LOG) {
			LOG = LoggerFactory.getLogger(Firebase.class.getName());
		}
		return LOG;
	}

}
