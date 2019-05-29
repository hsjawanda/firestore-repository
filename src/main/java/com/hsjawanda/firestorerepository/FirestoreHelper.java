/**
 *
 */
package com.hsjawanda.firestorerepository;

import static com.hsjawanda.firestorerepository.Firebase.db;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.common.base.CaseFormat;
import com.hsjawanda.firestorerepository.annotation.Collection;

import lombok.NonNull;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public final class FirestoreHelper {

	private static final Map<Class<?>, String> COLL_NAMES = new HashMap<>();

	private FirestoreHelper() {
	}

	public static String collectionName(@NonNull Class<?> cls) {
		if (!FirestoreHelper.COLL_NAMES.containsKey(cls)) {
			Collection coll = cls.getAnnotation(Collection.class);
			FirestoreHelper.COLL_NAMES.put(cls, null != coll ? coll.value()
					: CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, cls.getSimpleName()));
		}
		return FirestoreHelper.COLL_NAMES.get(cls);
	}

	public static <T extends Firestorable> CollectionReference collRef(@NonNull Class<T> cls) {
		return FirestoreHelper.collRef(cls, null);
	}

	public static <T extends Firestorable> CollectionReference collRef(@NonNull Class<T> cls,
			@Nullable DocumentReference parent) {
		String collName = collectionName(cls);
		return null != parent ? parent.collection(collName) : db().collection(collName);
	}

}
