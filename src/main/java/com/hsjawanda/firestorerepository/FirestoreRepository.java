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
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.CheckForNull;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
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

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A repository for performing CRUD operations and querying (by {@link Criteria}) on
 * <a href="https://firebase.google.com/docs/firestore/">Cloud Firestore</a>
 * <a href="https://firebase.google.com/docs/firestore/data-model">documents</a>.
 *
 * <p>
 * All instances of {@code FirestoreRepository} are immutable and stateless (the caching &mdash; at least for now
 * &mdash; happens externally in
 * <a href="https://cloud.google.com/appengine/docs/standard/java/memcache/">Memcache</a>). Setting any of this
 * repository's values creates and returns a new instance if required (i.e., if the value that the caller wants to set
 * is different from the current value).
 *
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 */
@Accessors(chain = true, fluent = true)
public final class FirestoreRepository<T extends Firestorable> implements Repository<T> {

	public static final Cache CACHE = Cache.instance(Namespace.FIRESTORE);

	protected static final Map<Class<?>, Field> ID_FIELDS = new HashMap<>();

	protected static final Map<Class<?>, Method> ON_LOAD_METHODS = new HashMap<>();

	protected static final Map<Class<?>, Method> ON_SAVE_METHODS = new HashMap<>();

	static final MapType MAP_TYPE = TypeFactory.defaultInstance().constructMapType(TreeMap.class, String.class,
			Object.class);

	static final ObjectMapper MAPR = new ObjectMapper().setSerializationInclusion(Include.NON_EMPTY);

	private static Logger LOG;

	private static final RetryOptions OPTS = RetryOptions.builder().debug(true).maxTries(5).initialWaitMillis(100)
			.build();

	private static ThreadLocal<Transaction> TX = new ThreadLocal<Transaction>() {
		@Override
		protected Transaction initialValue() {
			return null;
		}
	};

	protected final Expiration expiry;

	@Getter
	private final SetPolicy cacheSetPolicy;

	private final Class<T> cls;

	private final CollectionReference collRef;

	private final boolean useCache;

	@Generated("SparkTools")
	private FirestoreRepository(Builder<T> builder) {
		this.cls = builder.cls;
		this.collRef = builder.collRef != null ? builder.collRef : FirestoreHelper.collRef(this.cls, builder.parent);
		this.expiry = null == builder.cacheExpiration ? Expiration.byDeltaSeconds(3600) : builder.cacheExpiration;
		this.cacheSetPolicy = null == builder.cacheSetPolicy ?  SetPolicy.ADD_ONLY_IF_NOT_PRESENT : builder.cacheSetPolicy;
		this.useCache = null == builder.useCache? true : builder.useCache.booleanValue();
		setIdField(this.cls);
		checkArgument(ID_FIELDS.containsKey(this.cls),
				"Class %s has not annotated any String field with the @%s annotation", this.cls.getName(),
				Id.class.getName());
		setOnSaveMethod(this.cls);
		setOnLoadMethod(this.cls);
	}

	/**
	 * Creates builder to build {@link FirestoreRepository}.
	 * @return created builder
	 */
	@Generated("SparkTools")
	public static <T extends Firestorable> Builder<T> builder(Class<T> cls) {
		return new Builder<T>(cls);
	}

	protected static Logger log() {
		if (null == LOG) {
			LOG = LoggerFactory.getLogger(FirestoreRepository.class.getName());
		}
		return LOG;
	}

	/**
	 * Creates a builder to build {@link FirestoreRepository} and initialize it with the given object.
	 *
	 * @param firestoreRepository
	 *            to initialize the builder with
	 * @return created builder
	 */
	@Generated("SparkTools")
	static <T extends Firestorable> Builder<T> builderFrom(FirestoreRepository<T> firestoreRepository) {
		return new Builder<>(firestoreRepository);
	}

	static <T> T fromMap(Map<String, Object> map, Class<T> cls) {
		return MAPR.convertValue(map, cls);
	}

	/**
	 * Invoke the method annotated with {@link @OnLoad} after loading {@code obj} from DS.
	 *
	 * @param obj the object to perform operations on after loading from DS
	 * @return the {@code obj} itself
	 * @throws GetException
	 */
	@CheckForNull
	static <T> T performLoadActions(@Nullable T obj) throws GetException {
		if (null != obj) {
			Method onLoadMethod = ON_LOAD_METHODS.get(obj.getClass());
			if (null != onLoadMethod) {
				try {
					onLoadMethod.setAccessible(true);
					onLoadMethod.invoke(obj);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new GetException("Failed to invoke method marked @OnLoad", e);
				}
			}
		}
		return obj;
	}

	/**
	 * Invoke the method annotated with {@link @OnSave} before saving {@code obj} to the DS.
	 *
	 * @param obj the object to perform operations on before saving to DS
	 * @return the {@code obj} itself
	 * @throws SaveException
	 */
	@CheckForNull
	static <T> T performSaveActions(T obj) throws SaveException {
		if (null != obj) {
			Method onSaveMethod = ON_SAVE_METHODS.get(obj.getClass());
			if (null != onSaveMethod) {
				try {
					onSaveMethod.setAccessible(true);
					onSaveMethod.invoke(obj);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new SaveException("Failed to invoke method marked @OnSave", e);
				}
			}
		}
		return obj;
	}

	static <T> Map<String, Object> toMap(T obj) {
		return MAPR.convertValue(obj, MAP_TYPE);
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

	private static void setOnLoadMethod(Class<?> cls) throws IllegalArgumentException {
		synchronized (ON_LOAD_METHODS) {
			if (!ON_LOAD_METHODS.containsKey(cls)) {
				Method[] methods = cls.getDeclaredMethods();
				int methodsFound = 0;
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].isAnnotationPresent(OnLoad.class) && methods[i].getParameterTypes().length == 0) {
						methods[i].setAccessible(true);
						ON_LOAD_METHODS.put(cls, methods[i]);
						methodsFound++;
					}
				}
				checkArgument(methodsFound < 2, "Found %d methods annotated with @%s.", methodsFound,
						OnLoad.class.getSimpleName());
			}
		}
	}

	private static void setOnSaveMethod(Class<?> cls) throws IllegalArgumentException {
		synchronized (ON_SAVE_METHODS) {
			if (!ON_SAVE_METHODS.containsKey(cls)) {
				Method[] methods = cls.getDeclaredMethods();
				int methodsFound = 0;
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].isAnnotationPresent(OnSave.class) && methods[i].getParameterTypes().length == 0) {
						methods[i].setAccessible(true);
						ON_SAVE_METHODS.put(cls, methods[i]);
						methodsFound++;
					}
				}
				checkArgument(methodsFound < 2, "Found %d methods annotated with @%s.", methodsFound,
						OnSave.class.getSimpleName());
			}
		}
	}

	@Override
	public void cache(Iterable<T> objs) {
		Map<String, T> map = StreamSupport.stream(objs.spliterator(), false).collect(Collectors
				.toMap(x -> docRef(x).getPath(), Function.identity(), (x, y) -> y, LinkedHashMap<String, T>::new));
		CACHE.SVC.putAll(map, this.expiry, SetPolicy.SET_ALWAYS);
	}

	@Override
	public T cache(T obj) {
		return cache(obj, false);
	}

	@Override
	public T cache(T obj, boolean alwaysSet) {
		return internalCache(obj, alwaysSet);
	}

	public Expiration cacheExpiration() {
		return this.expiry;
	}

	/**
	 * If {@code cacheExpiration} is {@code equal()} to the internal value, the current instance is returned unchanged,
	 * otherwise a new instance with its cache expiry value set to {@code cacheExpiration} is created and returned.
	 *
	 * @param cacheExpiration the cache expiration value to set
	 * @return {@code this} or a new instance with cache expiry set to {@code cacheExpiration}
	 */
	public FirestoreRepository<T> cacheExpiration(Expiration cacheExpiration) {
		return this.expiry.equals(cacheExpiration) ? this : builderFrom(this).cacheExpiration(cacheExpiration).build();
	}

	/**
	 * If {@code cacheSetPolicy} is {@code equal()} to the internal value, the current instance is returned unchanged,
	 * otherwise a new instance with its cache expiry value set to {@code cacheSetPolicy} is created and returned.
	 *
	 * @param cacheSetPolicy the cache expiration value to set
	 * @return {@code this} or a new instance with cache expiry set to {@code cacheSetPolicy}
	 */
	public FirestoreRepository<T> cacheSetPolicy(SetPolicy cacheSetPolicy) {
		return this.cacheSetPolicy.equals(cacheSetPolicy) ? this
				: builderFrom(this).cacheSetPolicy(cacheSetPolicy).build();
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
			setId(obj, docRef.getId());
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
				return Optional.ofNullable(performLoadActions(pojo));
			} else {
				String path = ref.getPath();
				Optional<T> obj = useCache() ? CACHE.get(path) : Optional.empty();
				if (obj.isPresent()) {
					log().debug("Got {} from CACHE.", path);
					performLoadActions(obj.get());
					return obj;
				}
				else {
					T pojo = setId(retry(OPTS, () -> {
						return ref.get().get();
					}));
					log().debug("Didn't get {} (useCache: {}) from CACHE, putting it in now.", path, useCache());
					internalCache(pojo, false);
					return Optional.ofNullable(performLoadActions(pojo));
				}
			}
		} catch (Exception e) {
			throw new GetException("Error getting DocumentReference: " + ref, e);
		}
	}

	@Override
	public Map<DocumentReference, T> get(Iterable<DocumentReference> refs) throws GetException {
		try {
			// Load actions already performed in getUnordered()
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

	@Override
	public List<T> getByCriteria(@Nullable Criteria criteria) throws GetException {
		Query q = constructQuery(criteria);
		try {

			QuerySnapshot qs = null != TX.get() ? TX.get().get(q).get() : q.get().get();
			List<T> rs = qs.getDocuments().stream().map(this::setId).map(FirestoreRepository::performLoadActions)
					.collect(Collectors.toCollection(ArrayList::new));
			if (null == TX.get()) {
				rs.stream().forEach(x -> internalCache(x, false));
			} else {
				if (useCache()) {
					rs.stream().forEach(x -> CACHE.pendForAddition(docRef(x).getPath(), x, this.expiry, SetPolicy.SET_ALWAYS));
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

	public boolean inTransaction() {
		return null != TX.get();
	}

	@Override
	public List<WriteResult> save(@NonNull Iterable<T> objs) throws SaveException {
		WriteBatch batch = db().batch();
		List<String> paths = new ArrayList<>();
		for (T obj : objs) {
			DocumentReference docRef = docRef(obj);
			Map<String, Object> map = toMap(performSaveActions(obj));
			if (null == TX.get()) {
				paths.add(docRef.getPath());
				batch.set(docRef, map);
			} else {
				TX.get().set(docRef, map);
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
			} else {
				paths.stream().forEach(CACHE::pendForDeletion);
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
			DocumentReference ref = docRef(obj);
			Map<String, Object> map = toMap(performSaveActions(obj));
			try {
//				if (null == obj.getId()) {
//					setId(obj, ref.getId());
//				}
				if (null != TX.get()) {
					TX.get().set(ref, map);
					CACHE.pendForDeletion(ref.getPath());
				} else {
					futureWr = retry(OPTS, () -> {
						return ref.set(map);
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
	@CheckForNull
	public Transaction transaction() {
		return TX.get();
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
		return this.useCache == cache ? this : builderFrom(this).useCache(cache).build();
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
			T obj = performLoadActions(setId(snapshot));
			interim.put(ref, obj);
			if (null == TX.get()) {
				internalCache(obj, true);
			} else {
				if (snapshot.exists() && useCache()) {
					CACHE.pendForAddition(ref.getPath(), obj, this.expiry, SetPolicy.SET_ALWAYS);
				}
			}
		}
		return interim;
	}

	private T internalCache(T obj, boolean alwaysSet) {
		if (useCache() && null != obj) {
			SetPolicy sp = alwaysSet ? SetPolicy.SET_ALWAYS : SetPolicy.ADD_ONLY_IF_NOT_PRESENT;
			CACHE.put(docRef(obj).getPath(), obj, this.expiry, sp);
		}
		return obj;
	}

	private void removeFromCache(T obj) {
		CACHE.delete(docRef(obj).getPath());
	}

	/**
	 * Builder to build {@link FirestoreRepository}.
	 */
	@Generated("SparkTools")
	public static final class Builder<T extends Firestorable> {

		private Expiration cacheExpiration;

		private SetPolicy cacheSetPolicy;

		private Class<T> cls;

		private Boolean useCache;

		private DocumentReference parent;

		private CollectionReference collRef;

		private Builder(Class<T> cls) {
			this.cls = cls;
		}

		private Builder(FirestoreRepository<T> firestoreRepository) {
			this.cacheExpiration = firestoreRepository.expiry;
			this.cacheSetPolicy = firestoreRepository.cacheSetPolicy;
			this.cls = firestoreRepository.cls;
			this.useCache = firestoreRepository.useCache;
			this.collRef = firestoreRepository.collRef;
		}

		@Nonnull
		public FirestoreRepository<T> build() {
			return new FirestoreRepository<T>(this);
		}

		@Nonnull
		public Builder<T> cacheExpiration(Expiration expiry) {
			this.cacheExpiration = expiry;
			return this;
		}

		@Nonnull
		public Builder<T> cacheSetPolicy(SetPolicy cacheSetPolicy) {
			this.cacheSetPolicy = cacheSetPolicy;
			return this;
		}

		public Builder<T> parent(DocumentReference parent) {
			this.parent = parent;
			return this;
		}

		@Nonnull
		public Builder<T> useCache(boolean useCache) {
			this.useCache = useCache;
			return this;
		}
	}

}
