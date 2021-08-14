package com.hsjawanda.firestorerepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.auth.oauth2.GoogleCredentials;
import com.hsjawanda.firestorerepository.util.GCredentials;

public class FirestoreRepositoryTest {

	private static final String KEY_FILE_NAME = "firestore-repository-test-firebase-adminsdk-q8fsu-9039b24aa3.json";

	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalMemcacheServiceTestConfig());

	private GoogleCredentials creds;

	@Before
	public void setUp() throws Exception {
		this.helper.setUp();
		if (null == this.creds) {
			this.creds = GCredentials.from(KEY_FILE_NAME, null);
		}
		Firebase.db(this.creds);
	}

	@After
	public void tearDown() throws Exception {
		this.helper.tearDown();
	}

	@Test
	public void testFirestoreRepositoryImmutability() {
		@SuppressWarnings("unused")
		FirestoreRepository<EverythingOkModel> rep = FirestoreRepository.builder(EverythingOkModel.class).build();
	}

}
