/**
 *
 */
package com.hsjawanda.firestorerepository.caching;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;

import com.google.common.base.CaseFormat;
import com.hsjawanda.firestorerepository.util.SplitJoin;
import com.hsjawanda.utilities.collections.Lists;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public enum MemCounter {

	CONTEST_ENTRIES(4);

	private static final SecureRandom CHOOSER = new SecureRandom();

	private static final int DEF_NUM_SHARDS = 1;

	private final String []	names;

	private final int numShards;

	private final String stem;

	private MemCounter() {
		this(DEF_NUM_SHARDS);
	}

	private MemCounter(int numShards) {
		this.stem = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, name());
		this.numShards = numShards;
		this.names = new String[this.numShards];
		for (int i = 0; i < this.numShards; i++) {
			this.names[i] = SplitJoin.join(this.stem, String.valueOf(i));
		}
	}

	public String[] allShardNames(String... differentiators) {
		if (null == differentiators)
			return this.names;
		else {
			List<String> parts = Lists.newListOf(differentiators.length + 2, differentiators);
			parts.add(0, this.stem);
			parts.add(String.valueOf(0));
			String[] retVal = new String[this.numShards];
			retVal[0] = SplitJoin.join(parts);
			for (int i = 1; i < this.numShards; i++) {
				parts.set(parts.size() - 1, String.valueOf(i));
				retVal[i] = SplitJoin.join(parts);
			}
			return retVal;
		}
	}

	public int numShards() {
		return this.numShards;
	}

	public String shardName() {
		return this.names[CHOOSER.nextInt(this.numShards)];
	}

	public String shardName(String... differentiators) throws NullPointerException {
		Objects.requireNonNull(differentiators, "null differentiators not allowed. Use shardName() instead.");
		List<String> parts = Lists.newListOf(differentiators.length + 2, differentiators);
		parts.add(0, this.stem);
		parts.add(String.valueOf(this.numShards > 1 ? CHOOSER.nextInt(this.numShards) : 0));
		return SplitJoin.join(parts);
	}

}
