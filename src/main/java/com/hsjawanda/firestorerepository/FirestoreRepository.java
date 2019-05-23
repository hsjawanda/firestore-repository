/**
 *
 */
package com.hsjawanda.firestorerepository;

import static com.hsjawanda.firestorerepository.Firebase.db;
import static com.hsjawanda.utilities.base.Check.checkArgument;
import static com.hsjawanda.utilities.repackaged.commons.lang3.StringUtils.isBlank;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.UnavailableException;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.Query.Direction;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;
import com.google.cloud.firestore.TransactionOptions;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.google.common.collect.Iterables;
import com.hsjawanda.firestorerepository.Criteria.Criterion;
import com.hsjawanda.firestorerepository.annotation.Id;
import com.hsjawanda.firestorerepository.annotation.OnLoad;
import com.hsjawanda.firestorerepository.annotation.OnSave;
import com.hsjawanda.firestorerepository.caching.Cache;
import com.hsjawanda.firestorerepository.caching.Cache.Namespace;
import com.hsjawanda.firestorerepository.exceptions.DeleteException;
import com.hsjawanda.firestorerepository.exceptions.GetException;
import com.hsjawanda.firestorerepository.exceptions.SaveException;
import com.hsjawanda.firestorerepository.util.ErrorHelper;
import com.hsjawanda.utilities.base.RetryOptions;
import com.hsjawanda.utilities.base.UnitOfWork;

import lombok.NonNull;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public class FirestoreRepository<T extends Firestorable> implements Repository<T> {

	public static final Cache CACHE = Cache.instance(Namespace.FIRESTORE);

	protected static final Expiration EXP = Expiration.byDeltaSeconds(3600);

	protected static final Map<Class<?>, Field> ID_FIELDS = new HashMap<>();

	private static Logger LOG;

	protected static final Map<Class<?>, Method> ON_LOAD_METHODS = new HashMap<>();

	protected static final Map<Class<?>, Method> ON_SAVE_METHODS = new HashMap<>();

	private static final RetryOptions OPTS = RetryOptions.builder().debug(true).maxTries(5).initialWaitMillis(100)
			.build();

	private static ThreadLocal<Transaction> TX = new ThreadLocal<Transaction>() {
		@Override
		protected Transaction initialValue() {
			return null;
		}
	};

	protected final Class<T> cls;

	protected final CollectionReference collRef;

	private final boolean useCache;

	FirestoreRepository(@NonNull CollectionReference collRef, @NonNull Class<T> cls) throws IllegalArgumentException {
		this(collRef, cls, true);
	}

	FirestoreRepository(@NonNull CollectionReference collRef, @NonNull Class<T> cls, boolean useCache)
			throws IllegalArgumentException {
		this.collRef = collRef;
		this.cls = cls;
		this.useCache = useCache;
		setIdField(cls);
		checkArgument(ID_FIELDS.containsKey(cls), "Class %s has not annotated any String field with the @Id annotation",
				cls.getName());
		setOnSaveMethod(cls);
		setOnLoadMethod(cls);
	}

//	public static Map<DocumentReference, DocumentSnapshot> getSnapshots(@Nullable Iterable<DocumentReference> refs)
//			throws GetException {
//		if (null == refs)
//			return java.util.Collections.emptyMap();
//		DocumentReference[] docRefs = Iterables.toArray(refs, DocumentReference.class);
//		List<DocumentSnapshot> snapshots;
//		try {
//			snapshots = db().getAll(docRefs).get();
//			if (snapshots.size() < docRefs.length)
//				throw new GetException(String.format(
//						"Number of DocumentSnapshots returned (%d) is < number of DocumentReferences (%d)",
//						Collections.size(snapshots), docRefs.length), null);
//			Map<DocumentReference, DocumentSnapshot> unordered = snapshots.stream()
//					.collect(toMap(DocumentSnapshot::getReference, Function.identity()));
//			Map<DocumentReference, DocumentSnapshot> retVal = new LinkedHashMap<>(docRefs.length * 8 / 10);
//			for (DocumentReference ref : refs) {
//				retVal.put(ref, unordered.get(ref));
//			}
//			return retVal;
//		} catch (GetException e) {
//			throw e;
//		} catch (Exception e) {
//			log().warn("Exception log:", e);
//			throw new GetException(e.getMessage(), e);
//		}
//	}

	protected static Logger log() {
		if (null == LOG) {
			LOG = LoggerFactory.getLogger(FirestoreRepository.class.getName());
		}
		return LOG;
	}

	private static <T> T retry(RetryOptions opts, UnitOfWork<T> work)
			throws ExecutionException, InterruptedException, RuntimeException {
		opts.reset();
		int i;
		for (i = 0; i < opts.maxTries; i++) {
			try {
				return work.execute();
			} catch (ExecutionException e) {
				if (!ErrorHelper.causedBy(e, UnavailableException.class))
					throw e;
				else {
					opts.backoff();
				}
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("Exception performing a UnitOfWork", e);
			}
		}
		if (opts.debug && i > 1) {
			log().warn("It took {} tries to successfully exectue a UnitOfWork.", i);
		}
		return null;
	}

	private static void setIdField(Class<?> cls) {
		synchronized (ID_FIELDS) {
			if (!ID_FIELDS.containsKey(cls)) {
				Field[] fields = cls.getDeclaredFields();
				for (Field field : fields) {
					if (field.isAnnotationPresent(Id.class) && field.getType().equals(String.class)) {
						ID_FIELDS.put(cls, field);
						break;
					}
				}
			}
		}
	}

	private static void setOnLoadMethod(Class<?> cls) {
		synchronized (ON_LOAD_METHODS) {
			if (!ON_LOAD_METHODS.containsKey(cls)) {
				Method[] methods = cls.getDeclaredMethods();
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].isAnnotationPresent(OnLoad.class) && methods[i].getParameterTypes().length == 0) {
						ON_LOAD_METHODS.put(cls, methods[i]);
						ON_LOAD_METHODS.get(cls).setAccessible(true);
						break;
					}
				}
			}
		}
	}

	private static void setOnSaveMethod(Class<?> cls) {
		synchronized (ON_SAVE_METHODS) {
			if (!ON_SAVE_METHODS.containsKey(cls)) {
				Method[] methods = cls.getDeclaredMethods();
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].isAnnotationPresent(OnSave.class) && methods[i].getParameterTypes().length == 0) {
						ON_SAVE_METHODS.put(cls, methods[i]);
						ON_SAVE_METHODS.get(cls).setAccessible(true);
						break;
					}
				}
			}
		}
	}

	@Override
	public void cache(Iterable<T> objs) {
		Map<String, T> map = StreamSupport.stream(objs.spliterator(), false).collect(Collectors
				.toMap(x -> docRef(x).getPath(), Function.identity(), (x, y) -> y, LinkedHashMap<String, T>::new));
		CACHE.SVC.putAll(map, EXP, SetPolicy.SET_ALWAYS);
	}

	@Override
	public T cache(T obj) {
		return cache(obj, false);
	}

	@Override
	public T cache(T obj, boolean alwaysSet) {
		return internalCache(obj, alwaysSet);
	}

	@Override
	public List<WriteResult> delete(Criteria criteria) throws DeleteException {
		return delete(getByCriteria(criteria));
	}

	@CheckForNull
	@Override
	public WriteResult delete(DocumentReference ref) throws DeleteException {
		try {
			if (null == TX.get())
				return ref.delete().get();
			else {
				TX.get().delete(ref);
				return null;
			}
		} catch (Exception e) {
			throw new DeleteException("Error deleting Document: " + ref, e);
		}
	}

	@Override
	public List<WriteResult> delete(@Nullable Iterable<T> objs) throws DeleteException {
		if (null == objs)
			return java.util.Collections.emptyList();
		return deleteByReferences(StreamSupport.stream(objs.spliterator(), false).filter(Objects::nonNull)
				.map(this::docRef).filter(Objects::nonNull).collect(toList()));
	}

	@Override
	public WriteResult delete(T obj) throws DeleteException {
		return delete(docRef(obj));
	}

	public List<WriteResult> deleteByReferences(@Nullable Iterable<DocumentReference> refs) throws DeleteException {
		if (null == refs)
			return java.util.Collections.emptyList();
		Iterator<DocumentReference> itr = refs.iterator();
		List<WriteResult> wrs = new ArrayList<>();
		while (itr.hasNext()) {
			WriteBatch batch = db().batch();
			int counter = 0;
			while (itr.hasNext() && counter < Firestore.FIRESTORE_MAX_BATCH_SIZE) {
				batch.delete(itr.next());
				counter++;
			}
			try {
				wrs.addAll(batch.commit().get());
			} catch (InterruptedException | ExecutionException e) {
				log().warn("Exception log:", e);
				throw new DeleteException("Error bulk-deleting.", e);
			}
		}
		return wrs;
	}

	@Override
	public DocumentReference docRef(@Nullable String id) {
		return null != id ? this.collRef.document(id) : this.collRef.document();
	}

	@Override
	public DocumentReference docRef(@Nullable T obj) throws RuntimeException {
		DocumentReference docRef = null;
		if (null == obj || null == obj.getId()) {
			docRef = this.collRef.document();
			Field idField = ID_FIELDS.get(this.cls);
			if (null != idField && null != obj) {
				idField.setAccessible(true);
				try {
					idField.set(obj, docRef.getId());
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Failed to set ID of object", e);
				}
			}
		} else {
			docRef = this.collRef.document(obj.getId());
		}
		return docRef;
	}

	@Override
	public String generateId() {
		return this.collRef.document().getId();
	}

	@Override
	public Optional<T> get(DocumentReference ref) throws GetException {
		try {
			if (null != TX.get()) {
				T pojo = setId(TX.get().get(ref).get());
				return Optional.ofNullable(pojo);
			} else {
				String path = ref.getPath();
				Optional<T> obj = useCache() ? CACHE.get(path) : Optional.empty();
				if (obj.isPresent()) {
					log().debug("Got {} from CACHE.", path);
					return obj;
				}
				else {
					T pojo = setId(retry(OPTS, () -> {
						return ref.get().get();
					}));
					log().debug("Didn't get {} (useCache: {}) from CACHE, putting it in now.", path, useCache());
					internalCache(pojo, false);
					return Optional.ofNullable(pojo);
				}
			}
		} catch (Exception e) {
			throw new GetException("Error getting DocumentReference: " + ref, e);
		}
	}

	@Override
	public Map<DocumentReference, T> get(Iterable<DocumentReference> refs) throws GetException {
		try {
			Map<DocumentReference, T> interim = getUnordered(refs);
			Map<DocumentReference, T> ordered = new LinkedHashMap<>();

			StreamSupport.stream(refs.spliterator(), false).forEach(x -> ordered.put(x, interim.get(x)));
			return ordered;
		} catch (InterruptedException | ExecutionException | RuntimeException e) {
			throw new GetException("Error getting multiple DocumentReferences: " + refs, e);
		}
	}

	@Override
	public Optional<T> get(String id) throws GetException {
		return isBlank(id) ? Optional.empty() : get(docRef(id));
	}

	@Override
	public Ref<T> getAsync(String id) {
		return isBlank(id) ? Ref.to((T) null) : Ref.to(this.collRef.document(id).get(), this);
	}

//	@Override
//	public Optional<T> getRandom(Criteria criteria) throws GetException {
//		return getRandom(criteria, null);
//	}
//
//	@Override
//	public List<T> getRandom(Criteria criteria, int numToFetch) {
//		String rdmId = this.collRef.document().getId();
//		if (null != this.idField) {
//			try {
//				Query q = constructQuery(criteria).orderBy(this.idField.getName());
//				List<QueryDocumentSnapshot> snapshots = q.startAfter(rdmId).limit(numToFetch).get().get().getDocuments();
//				if (Collections.isEmpty(snapshots)) {
//					snapshots = q.endBefore(rdmId).limit(numToFetch).get().get().getDocuments();
//				}
//				return snapshots.stream().map(x -> setId(x)).collect(Collectors.toList());
//			} catch (InterruptedException | ExecutionException e) {
//				// 12/10/2018
//				log().warn("Unexpected error. Stacktrace:", e);
//			}
//		}
//		return java.util.Collections.emptyList();
//	}
//
//	@Override
//	public Optional<T> getRandom(Criteria criteria, String randomPoint) throws GetException {
//		String rdmId = null == randomPoint ? this.collRef.document().getId() : randomPoint;
////		log().info("rmdId: " + rdmId + " (provided: " + (null != randomPoint) + "); Class: " + this.cls.getSimpleName());
//		int limit = 20;
//		if (null != this.idField) {
//			try {
//				@SuppressWarnings("unused")
//				int searchNum = 1;
//				Query q = constructQuery(criteria).orderBy(this.idField.getName());
//				List<QueryDocumentSnapshot> snapshots = q.startAfter(rdmId).limit(limit).get().get().getDocuments();
//				if (Collections.isEmpty(snapshots)) {
//					snapshots = q.endBefore(rdmId).limit(limit).get().get().getDocuments();
//					searchNum++;
//				}
//				if (Collections.isNotEmpty(snapshots))
////					log().info("Found question in search #" + searchNum);
//					return Optional.ofNullable(setId(snapshots.get(RANDOMIZER.nextInt(snapshots.size()))));
//				else {
//					log().info("No results found.");
//				}
//			} catch (InterruptedException | ExecutionException e) {
//				throw new GetException("Unexpected error", e);
//			}
//		}
//		return Optional.empty();
//	}

	@Override
	public List<T> getByCriteria(@Nullable Criteria criteria) throws GetException {
		Query q = constructQuery(criteria);
		try {

			QuerySnapshot qs = null != TX.get() ? TX.get().get(q).get() : q.get().get();
			List<T> rs = qs.getDocuments().stream().map(x -> setId(x)).collect(Collectors.toCollection(ArrayList::new));
			if (null == TX.get()) {
				rs.stream().forEach(x -> internalCache(x, false));
			} else {
				if (useCache()) {
					rs.stream().forEach(x -> CACHE.pendForAddition(docRef(x).getPath(), x, EXP, SetPolicy.SET_ALWAYS));
				}
			}
			return rs;
		} catch (InterruptedException | ExecutionException e) {
			throw new GetException("Error getting by query: " + q, e);
		}
	}

	@Override
	@Nonnull
	public Map<String, T> getByIds(Iterable<String> ids) throws GetException {
		Map<String, DocumentReference> refMap = new HashMap<>();
		StreamSupport.stream(ids.spliterator(), false).forEach(x -> refMap.put(x, docRef(x)));
		try {
			Map<DocumentReference, T> unordered = getUnordered(refMap.values());
			Map<String, T> ordered = new LinkedHashMap<>();
			StreamSupport.stream(ids.spliterator(), false).forEach(x -> ordered.put(x, unordered.get(refMap.get(x))));
			return ordered;
		} catch (InterruptedException | ExecutionException | RuntimeException e) {
			throw new GetException("Error getting multiple String IDs: " + ids, e);
		}
	}

	@Override
	@CheckForNull
	public Transaction getTx() {
		return TX.get();
	}

	@Override
	public List<WriteResult> save(@NonNull Iterable<T> objs) throws SaveException {
		WriteBatch batch = db().batch();
		List<String> paths = new ArrayList<>();
		for (T obj : objs) {
			performSaveActions(obj);
			DocumentReference docRef = docRef(obj);
			if (null == TX.get()) {
				paths.add(docRef.getPath());
				batch.set(docRef, obj);
			} else {
				TX.get().set(docRef, obj);
			}
		}
		try {
			List<WriteResult> wrs = java.util.Collections.emptyList();
			if (null == TX.get()) {
				wrs = retry(OPTS, () -> {
					return batch.commit().get();
				});
				if (useCache()) {
					paths.stream().forEach(CACHE::delete);
				}
			}
			return wrs;
		} catch (InterruptedException | ExecutionException e) {
			throw new SaveException("Error saving a batch.", e);
		}
	}

	@CheckForNull
	@Override
	public WriteResult save(@Nullable T obj) throws IllegalArgumentException, SaveException {
		WriteResult wr = null;
		ApiFuture<WriteResult> futureWr = saveAsync(obj);
		if (null != futureWr) {
			try {
				wr = futureWr.get();
				removeFromCache(obj);
			} catch (ExecutionException e) {
				throw new SaveException("Error saving Object (ExecutionException): " + obj, e.getCause());
			} catch (Exception e) {
				throw new SaveException("Error saving Object: " + obj, e);
			}
		}
		return wr;
	}

	@CheckForNull
	@Override
	public ApiFuture<WriteResult> saveAsync(@Nullable T obj) throws IllegalArgumentException, SaveException {
		ApiFuture<WriteResult> futureWr = null;
		if (null != obj) {
			performSaveActions(obj);
			DocumentReference ref = docRef(obj);
			try {
				if (null == obj.getId()) {
					setId(obj, ref.getId());
				}
				if (null != TX.get()) {
					TX.get().set(ref, obj);
					CACHE.pendForDeletion(ref.getPath());
				} else {
					futureWr = retry(OPTS, () -> {
						return ref.set(obj);
					});
					internalCache(obj, false);
				}
			} catch (SaveException e) {
				throw e;
			} catch (Exception e) {
				throw new SaveException("Error saving Object: " + obj, e);
			}
		}
		return futureWr;
	}

	@Override
	public synchronized <V> V transact(UnitOfWork<V> work) throws InterruptedException, ExecutionException {
		return transact(work, TransactionOptions.create());
	}

	@Override
	public synchronized <V> V transact(UnitOfWork<V> work, TransactionOptions opts)
			throws InterruptedException, ExecutionException {
		V retVal = null;
		try {
			if (null != TX.get()) {
				retVal = work.execute();
			} else {
				try {
					ApiFuture<V> futureRetVal = db().runTransaction((Transaction tx) -> {
						TX.set(tx);
						return work.execute();
					}, opts);
					retVal = futureRetVal.get();
				} finally {
					TX.set(null);
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			throw e;
		} catch (Exception e) {
			throw new ExecutionException("Error executing a UnitOfWork.", e);
		} finally {
			CACHE.flushPendingOperations();
		}
		return retVal;
	}

	@Override
	public boolean useCache() {
		return this.useCache;
	}

	/**
	 * Generates a new {@code FirestoreRepository} identical to this one (except the choice of whether to use caching
	 * is set as per {@code cache}) if {@code cache} is different from the value for {@code this} repository.
	 *
	 * @return a new {@code FirestoreRepository}
	 */
	@Override
	public FirestoreRepository<T> useCache(boolean cache) {
		return this.useCache == cache ? this : new FirestoreRepository<>(this.collRef, this.cls, this.useCache);
	}

	@CheckForNull
	protected T setId(T pojo, String id) throws GetException {
		if (null == pojo)
			return null;
		Field idField = ID_FIELDS.get(this.cls);
		try {
			idField.setAccessible(true);
			idField.set(pojo, id);
			return pojo;
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			String fieldName = idField.getName();
			throw new GetException("Error setting value of '" + fieldName + "' field using reflection.", e);
		}
	}

	@CheckForNull
	T setId(DocumentSnapshot ds) throws GetException {
		return null != ds && ds.exists() ? setId(ds.toObject(this.cls), ds.getId()) : null;
	}

	private Query constructQuery(@Nullable Criteria criteria) {
		Query q = this.collRef;
		if (null != criteria) {
			for (Criterion criterion : criteria.getCriteria()) {
				switch (criterion.getOp()) {
				case ARRAY_CONTAINS:
					q = q.whereArrayContains(criterion.getField(), criterion.getValue());
					break;
				case EQUAL:
					q = q.whereEqualTo(criterion.getField(), criterion.getValue());
					break;
				case GREATER_THAN:
					q = q.whereGreaterThan(criterion.getField(), criterion.getValue());
					break;
				case GREATER_THAN_OR_EQUAL:
					q = q.whereGreaterThanOrEqualTo(criterion.getField(), criterion.getValue());
					break;
				case LESS_THAN:
					q = q.whereLessThan(criterion.getField(), criterion.getValue());
					break;
				case LESS_THAN_OR_EQUAL:
					q = q.whereLessThanOrEqualTo(criterion.getField(), criterion.getValue());
					break;
				case LIMIT:
					q = q.limit((Integer) criterion.getValue());
					break;
				case ORDER_BY:
					q = q.orderBy(criterion.getField());
					break;
				case ORDER_BY_ASC:
					q = q.orderBy(criterion.getField(), Direction.ASCENDING);
					break;
				case ORDER_BY_DSC:
					q = q.orderBy(criterion.getField(), Direction.DESCENDING);
					break;
				case START_AFTER:
					if (null != criterion.getValue()) {
						q = q.startAfter(criterion.getValue());
					}
					break;
				case START_AT:
					if (null != criterion.getValue()) {
						q = q.startAt(criterion.getValue());
					}
					break;
				default:
					break;
				}
			}
		}
		return q;
	}

	private Map<DocumentReference, T> getUnordered(Iterable<DocumentReference> refs)
			throws InterruptedException, ExecutionException, RuntimeException {
		List<DocumentSnapshot> snapshots;
		DocumentReference[] refsArr = Iterables.toArray(refs, DocumentReference.class);
		if (null != TX.get()) {
			snapshots = TX.get().getAll(refsArr).get();
		} else {
			snapshots = db().getAll(refsArr).get();
		}
		Map<DocumentReference, T> interim = new HashMap<>();
		for (DocumentSnapshot snapshot : snapshots) {
			DocumentReference ref = snapshot.getReference();
			T obj = setId(snapshot);
			interim.put(ref, obj);
			if (null == TX.get()) {
				internalCache(obj, true);
			} else {
				if (snapshot.exists() && useCache()) {
					CACHE.pendForAddition(ref.getPath(), obj, EXP, SetPolicy.SET_ALWAYS);
				}
			}
		}
		return interim;
	}

	private T internalCache(T obj, boolean alwaysSet) {
		if (useCache() && null != obj) {
			SetPolicy sp = alwaysSet ? SetPolicy.SET_ALWAYS : SetPolicy.ADD_ONLY_IF_NOT_PRESENT;
			CACHE.put(docRef(obj).getPath(), obj, EXP, sp);
		}
		return obj;
	}

	private void performLoadActions(T obj) throws GetException {
		Method onLoadMethod = ON_LOAD_METHODS.get(this.cls);
		if (null != onLoadMethod) {
			try {
				onLoadMethod.setAccessible(true);
				onLoadMethod.invoke(obj);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new GetException("Failed to invoke method marked @OnLoad", e);
			}
		}
	}

	private void performSaveActions(T obj) throws SaveException {
		Method onSaveMethod = ON_SAVE_METHODS.get(this.cls);
		if (null != onSaveMethod) {
			try {
				onSaveMethod.setAccessible(true);
				onSaveMethod.invoke(obj);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SaveException("Failed to invoke method marked @OnSave", e);
			}
		}
	}

	private void removeFromCache(T obj) {
		CACHE.delete(docRef(obj).getPath());
	}

}
