/**
 *
 */
package com.hsjawanda.firestorerepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Transaction;
import com.google.cloud.firestore.TransactionOptions;
import com.google.cloud.firestore.WriteResult;
import com.hsjawanda.firestorerepository.exceptions.DeleteException;
import com.hsjawanda.firestorerepository.exceptions.GetException;
import com.hsjawanda.firestorerepository.exceptions.SaveException;
import com.hsjawanda.utilities.base.UnitOfWork;

import lombok.NonNull;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public interface Repository<T extends Firestorable> {

	void cache(Iterable<T> objs);

	T cache(T obj);

	T cache(T obj, boolean alwaysSet);

	List<WriteResult> delete(Criteria criteria) throws DeleteException;

	WriteResult delete(DocumentReference ref) throws DeleteException;

	List<WriteResult> delete(@Nullable Iterable<T> objs) throws DeleteException;

	WriteResult delete(T obj) throws DeleteException;

	DocumentReference docRef(String id);

	DocumentReference docRef(T obj);

	String generateId();

	Map<DocumentReference, T> get(Iterable<DocumentReference> refs) throws GetException;

	Optional<T> get(DocumentReference ref) throws GetException;

	Optional<T> get(String id) throws GetException;

	Ref<T> getAsync(String id);

	List<T> getByCriteria(Criteria criterion) throws GetException;

	Map<String, T> getByIds(Iterable<String> ids);

//	Optional<T> getRandom(Criteria criteria) throws GetException;
//
//	List<T> getRandom(Criteria criteria, int numToFetch);
//
//	Optional<T> getRandom(Criteria criteria, String randomPoint) throws GetException;

	Transaction getTx();

	List<WriteResult> save(@NonNull Iterable<T> objs) throws SaveException;

	WriteResult save(T obj) throws SaveException;

	ApiFuture<WriteResult> saveAsync(@Nullable T obj) throws IllegalArgumentException, SaveException;

	<V> V transact(UnitOfWork<V> function)
			throws InterruptedException, ExecutionException, RuntimeException;

	<V> V transact(UnitOfWork<V> function, TransactionOptions opts)
			throws InterruptedException, ExecutionException, RuntimeException;

	boolean useCache();

	Repository<T> useCache(boolean cache);

}
