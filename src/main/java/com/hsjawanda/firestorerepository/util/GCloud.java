/**
 *
 */
package com.hsjawanda.firestorerepository.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public final class GCloud {

	public static final String BEARER = "Bearer ";

//	public static final ObjectReader GCS_RESPONSE_READER = mapper().mapper().readerFor(GcsResponse.class);
//
//	public static final ObjectReader OPERATION_READER = mapper().mapper().readerFor(Operation.class);
//
//	private static BigQuery bq;

	private static Logger LOG;

//	private static final String OPERATION_STATUS = "https://firestore.googleapis.com/v1/%s";

//	private static Storage storage;

	private GCloud() {
	}

//	public static synchronized BigQuery bigQuery() throws RuntimeException {
//		if (null == bq) {
//			try {
//				bq = BigQueryOptions.newBuilder().setProjectId(PROJECT_ID)
//						.setCredentials(GCredentials.firestoreServiceAc()).build().getService();
//			} catch (IOException e) {
//				log().warn("Exception log:", e);
//				throw new RuntimeException("Couldn't get BigQuery service.", e);
//			}
//		}
//		return bq;
//	}
//
//	public static synchronized Storage gcs() throws RuntimeException {
//		if (null == storage) {
//			try {
//				storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID)
//						.setCredentials(GCredentials.firestoreServiceAc()).build().getService();
//			} catch (IOException e) {
//				log().warn("Exception log:", e);
//				throw new RuntimeException("Couldn't get Cloud Storage service.", e);
//			}
//		}
//		return storage;
//	}
//
//	public static boolean operationIsDone(final Operation op) throws IOException {
//		Operation retVal = operationStatus(op);
//		return null != retVal && retVal.isDone();
//	}
//
//	public static Operation operationStatus(final Operation op) throws IOException {
//		GenericUrl url = new GenericUrl(String.format(OPERATION_STATUS, op.getName()));
//		HttpRequest req = Network.generalRequestFactory().buildGetRequest(url);
//		AccessToken token = GCredentials.token(GCredentials.firestoreServiceAc(), false);
//		req.getHeaders().setAuthorization(BEARER + token.getTokenValue());
//		Operation retVal = null;
//		try {
//			HttpResponse res = req.execute();
//			retVal = OPERATION_READER.readValue(res.parseAsString());
//		} catch (HttpResponseException e) {
//			log().warn("Exception log:", e);
//		}
//		return retVal;
//	}

	@SuppressWarnings("unused")
	private static Logger log() {
		if (null == LOG) {
			LOG = LoggerFactory.getLogger(GCloud.class.getName());
		}
		return LOG;
	}

//	public static final class GStorage {
//
//		private static Logger LOG;
//
//		private GStorage() {
//		}
//
//		public static void deleteDirectory(Bucket bucket, String blobPathPrefix) {
//			if (Setting.debug()) {
//				log().info("blobPathPrefix: " + blobPathPrefix);
//			}
//			Page<Blob> page = bucket.list(BlobListOption.prefix(blobPathPrefix));
//			List<BlobId> blobIds = new ArrayList<>(20);
//			for (Blob blob : page.iterateAll()) {
//				blobIds.add(blob.getBlobId());
//			}
//			if (Setting.debug()) {
//				log().info("Have {} BlobIds to delete.", blobIds.size());
//			}
//			if (blobIds.size() > 0) {
//				try {
//					GCloud.gcs().delete(blobIds);
//				} catch (RuntimeException e) {
//					log().warn("Exception log:", e);
//				}
//			}
//		}
//
//		public static String makePublic(@NonNull Blob blob) {
//			blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
//			return blob.getMediaLink();
//		}
//
//		public static boolean removePublic(@NonNull Blob blob) {
//			return blob.deleteAcl(Acl.User.ofAllUsers());
//		}
//
//		private static Logger log() {
//			if (null == LOG) {
//				LOG = LoggerFactory.getLogger(GStorage.class.getName());
//			}
//			return LOG;
//		}
//
//	}

	public static final class Scopes {

		public static final String CLOUD_PLATFORM = "https://www.googleapis.com/auth/cloud-platform";

		private Scopes() {
		}

	}

}
