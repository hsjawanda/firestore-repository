/**
 *
 */
package com.hsjawanda.firestorerepository.util;

import static com.hsjawanda.utilities.repackaged.commons.lang3.StringUtils.isBlank;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;


/**
 * @author Harshdeep S Jawanda (hsjawanda@gmail.com)
 *
 */
public class SplitJoin {

	public static final SplitJoin DEFAULT = with(null);

	public static final String SEPARATOR = ":::";

	public final String divider;

	private final Joiner joiner;

	private final Splitter splitter;

	private SplitJoin(String divider) {
		this.divider = divider;
		this.joiner = Joiner.on(this.divider).skipNulls();
		this.splitter = Splitter.on(this.divider);
	}

	public static StringBuilder appendTo(@Nonnull StringBuilder sb, Iterable<?> parts) throws NullPointerException {
		Objects.requireNonNull(sb, "sb can't be null");
		return DEFAULT.joiner.appendTo(sb, parts);
	}

	public static StringBuilder appendTo(@Nonnull StringBuilder sb, Object[] parts) throws NullPointerException {
		Objects.requireNonNull(sb, "sb can't be null");
		return DEFAULT.joiner.appendTo(sb, parts);
	}

	public static String join(Iterable<?> parts) {
		return DEFAULT.joiner.join(parts);
	}

	public static String join(Object... inputs) {
		return DEFAULT.joiner.join(inputs);
	}

	public static List<String> split(String input) {
		return DEFAULT.splitter.splitToList(input);
	}

	public static SplitJoin with(String divider) throws IllegalArgumentException {
		divider = isBlank(divider) ? SEPARATOR : divider;
		return new SplitJoin(divider);
	}


	public StringBuilder append(@Nonnull StringBuilder sb, Iterable<?> parts) {
		Objects.requireNonNull(sb);
		return this.joiner.appendTo(sb, parts);
	}

	public StringBuilder append(@Nonnull StringBuilder sb, Object[] parts) {
		Objects.requireNonNull(sb);
		return this.joiner.appendTo(sb, parts);
	}

	public String joinAll(Iterable<?> parts) {
		return this.joiner.join(parts);
	}

	public String joinAll(Object... inputs) {
		return this.joiner.join(inputs);
	}

	public List<String> splitToList(String input) {
		return this.splitter.splitToList(input);
	}

}
