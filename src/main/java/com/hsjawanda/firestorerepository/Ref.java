/**
 *
 */
package com.hsjawanda.firestorerepository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
@Accessors(chain = true)
public final class Ref<T extends Firestorable> {

	private static Logger LOG;

	@Setter(value = AccessLevel.PRIVATE)
	private ApiFuture<DocumentSnapshot> apiFutureRef;

	@Setter(value = AccessLevel.PRIVATE)
	private Future<T> futureRef;

	@Setter(value = AccessLevel.PRIVATE)
	private T ref;

	@Setter(value = AccessLevel.PRIVATE)
	private FirestoreRepository<T> rep;

	private Ref() {
	}

	static <T extends Firestorable> Ref<T> to(ApiFuture<DocumentSnapshot> futureObj,
			@NonNull FirestoreRepository<T> rep) {
		return new Ref<T>().setApiFutureRef(futureObj).setRep(rep);
	}

	static <T extends Firestorable> Ref<T> to(Future<T> futureObj) {
		return new Ref<T>().setFutureRef(futureObj);
	}

	static <T extends Firestorable> Ref<T> to(T obj) {
		return new Ref<T>().setRef(obj);
	}

	private static Logger log() {
		if (null == LOG) {
			LOG = LoggerFactory.getLogger(Ref.class.getName());
		}
		return LOG;
	}

	@CheckForNull
	public Optional<T> get() throws InterruptedException, ExecutionException {
		if (null != this.ref)
			return Optional.of(this.ref);
		else if (null != this.futureRef) {
			this.ref = this.futureRef.get();
		}
		else if (null != this.apiFutureRef) {
			DocumentSnapshot ds = this.apiFutureRef.get();
			this.ref = this.rep.setId(ds);
		}
		return Optional.ofNullable(this.ref);
	}

	public T getOr(T alternateValue) throws InterruptedException, ExecutionException {
		return get().orElse(alternateValue);
	}

	@CheckForNull
	public Optional<T> tryGet() {
		return tryGet(null);
	}

	@CheckForNull
	public Optional<T> tryGet(T defaultValue) {
		try {
			return get();
		} catch (InterruptedException | ExecutionException e) {
			log().warn("Error getting from Future or ApiFuture. Stacktrace:", e);
		}
		return Optional.ofNullable(defaultValue);
	}

}
