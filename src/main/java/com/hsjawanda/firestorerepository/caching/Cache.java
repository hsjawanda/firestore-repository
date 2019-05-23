/**
 *
 */
package com.hsjawanda.firestorerepository.caching;

import static com.hsjawanda.utilities.base.Check.checkArgument;
import static com.hsjawanda.utilities.base.Check.checkState;
import static com.hsjawanda.utilities.repackaged.commons.lang3.StringUtils.EMPTY;
import static com.hsjawanda.utilities.repackaged.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.base.CaseFormat;
import com.hsjawanda.utilities.base.Holdall;
import com.hsjawanda.utilities.repackaged.commons.lang3.mutable.MutableLong;

import lombok.NonNull;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public final class Cache {

	private final static Map<Namespace, Cache> INSTANCES = new EnumMap<>(Namespace.class);

	private static Logger LOG;

	public final MemcacheService SVC;

	protected Map<Object, ValueDetails> pendingAdditions = new ConcurrentHashMap<>();

	protected Set<Object> pendingDeletions = Collections.synchronizedSet(new HashSet<>());

	private Cache(Namespace namespace) {
		this.SVC = MemcacheServiceFactory.getMemcacheService(namespace.val());
	}

	public static synchronized Cache instance(Namespace namespace) {
		if (!INSTANCES.containsKey(namespace)) {
			INSTANCES.put(namespace, new Cache(namespace));
		}
		return INSTANCES.get(namespace);
	}

	private static Logger log() {
		if (null == LOG) {
			LOG = LoggerFactory.getLogger(Cache.class.getName());
		}
		return LOG;
	}

	@Deprecated
	public long checkCounterValueAndIncrement(@NonNull String counterName, long incrementBy, long initVal,
			long maxVal) throws IllegalArgumentException, RuntimeException {
		int numTries = 1;
		for (int waitMs = 1; waitMs < 100; waitMs *= 2, numTries++) {
			IdentifiableValue val = this.SVC.getIdentifiable(counterName);
			MutableLong counter = null != val ? (MutableLong) val.getValue() : new MutableLong(initVal);
			long currVal = counter.longValue();
			checkArgument(currVal + incrementBy <= maxVal,
					"Value of '%s' (currently: %d) would increase beyond %d if incremented by %d.", counterName,
					currVal, maxVal, incrementBy);
			counter.add(incrementBy);
			if (this.SVC.putIfUntouched(counterName, val, counter))
				return counter.longValue();
			else {
				Holdall.sleep(waitMs);
			}
		}
		throw new RuntimeException("Couldn't update counter despite " + numTries + " attempts.");
	}

	public long counterValue(@NonNull MemCounter counter, String... differentiators) {
		String[] shardNames = counter.allShardNames(differentiators);
		long total = 0;
		Map<String, Object> shardMap = this.SVC.getAll(Arrays.asList(shardNames));
		for (Object _shard : shardMap.values()) {
			MutableLong shard = (MutableLong) _shard;
			total += null != shard ? shard.longValue() : 0;
		}
		return total;
	}

	public boolean delete(Object key) {
		return this.SVC.delete(key);
	}

	public Cache flushPendingOperations() {
		finishPendingDeletions().finishPendingAdditions();
		return this;
	}

	public <T> Optional<T> get(Object key) {
		try {
			finishPendingDeletions();
			@SuppressWarnings("unchecked")
			T retVal = (T) this.SVC.get(key);
			return Optional.ofNullable(retVal);
		} catch (Exception e) {
			log().warn("Error getting from cache (key " + key + "): ", e);
			throw e;
		}
	}

	@Deprecated
	public Long incrementAtomicCounter(@NonNull String counterName, long incrementBy, long initVal) {
		return this.SVC.increment(counterName, incrementBy, initVal);
	}

	public long incrementShard(@NonNull String shardName, long incrementBy, long initVal, Expiration exp)
			throws IllegalStateException, RuntimeException {
		int numTries = 1;
		for (int waitMs = 1; waitMs < 100; waitMs *= 2, numTries++) {
			IdentifiableValue val = this.SVC.getIdentifiable(shardName);
			MutableLong counter = null != val ? (MutableLong) val.getValue() : new MutableLong(initVal);
			checkState(Long.MAX_VALUE - counter.longValue() > incrementBy,
					"Incrementing '%s' by %d would exceed the maximum capacity.", shardName, incrementBy);
			counter.add(incrementBy);
			if (null == val) {
				this.SVC.put(shardName, counter, exp);
				return counter.longValue();
			} else if (this.SVC.putIfUntouched(shardName, val, counter, exp))
				return counter.longValue();
			else {
				Holdall.sleep(waitMs);
			}
		}
		throw new RuntimeException("Couldn't update counter despite " + numTries + " attempts.");
	}

	public Cache pendForAddition(@NonNull Object key, @NonNull Object value, Expiration exp, SetPolicy sp) {
		this.pendingAdditions.put(key, new ValueDetails(value, exp, sp));
		return this;
	}

	public Cache pendForDeletion(@NonNull Object key) {
		synchronized (this.pendingDeletions) {
			this.pendingDeletions.add(key);
		}
		return this;
	}

	public void put(Object key, Object value) {
		this.SVC.put(key, value, null, SetPolicy.SET_ALWAYS);
	}

	public void put(Object key, Object value, Expiration exp, SetPolicy sp) {
		this.SVC.put(key, value, exp, sp);
	}

	public void put(Object key, Object value, SetPolicy sp) {
		this.SVC.put(key, value, null, sp);
	}

	public long resetCounter(@NonNull MemCounter counter, @Nullable Expiration exp, String... differentiators) {
		String[] shardNames = counter.allShardNames(differentiators);
		long total = 0;
		for (int i = 0; i < shardNames.length; i++) {
			total += resetShard(shardNames[i], exp);
		}
		return total;
	}

	public long resetShard(@NonNull String shardName, @Nullable Expiration exp)
			throws NullPointerException, IllegalArgumentException, RuntimeException {
		checkArgument(isNotBlank(shardName), "counterName can't be blank.");
		int numTries = 1;
		for (int waitMs = 1; waitMs < 100; waitMs *= 2, numTries++) {
			IdentifiableValue val = this.SVC.getIdentifiable(shardName);
			MutableLong counter = null != val ? (MutableLong) val.getValue() : new MutableLong(0);
			long existingCount = counter.longValue();
			counter.setValue(0);
			if (null == val) {
				this.SVC.put(shardName, counter, exp);
				return existingCount;
			} else if (this.SVC.putIfUntouched(shardName, val, counter, exp))
				return existingCount;
			else {
				Holdall.sleep(waitMs);
			}
		}
		throw new RuntimeException("Couldn't update counter despite " + numTries + " attempts.");
	}

	private Cache finishPendingAdditions() {
		synchronized (this.pendingAdditions) {
			Iterator<Map.Entry<Object, ValueDetails>> keysItr = this.pendingAdditions.entrySet().iterator();
			while (keysItr.hasNext()) {
				Map.Entry<Object, ValueDetails> entry = keysItr.next();
				this.SVC.put(entry.getKey(), entry.getValue().value, entry.getValue().exp, entry.getValue().sp);
				keysItr.remove();
			}
		}
		return this;
	}

	private Cache finishPendingDeletions() {
		synchronized (this.pendingDeletions) {
			Iterator<Object> itr = this.pendingDeletions.iterator();
			while (itr.hasNext()) {
				Object key = itr.next();
				this.SVC.delete(key);
				itr.remove();
//				log().info("Deleted & removed: '" + key + "'");
			}
		}
		return this;
	}

	public enum Namespace {

		CONTESTS, DEFAULT(EMPTY), FIRESTORE;

		private String val;

		private Namespace() {
			this.val = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
		}

		private Namespace(String val) {
			this.val = val;
		}

		public String val() {
			return this.val;
		}

	}

	private static class ValueDetails {

		public final Expiration exp;

		public final SetPolicy sp;

		public final Object value;

		public ValueDetails(Object value, Expiration exp, SetPolicy sp) {
			this.value = value;
			this.exp = exp;
			this.sp = sp;
		}

	}

}
