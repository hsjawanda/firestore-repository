/**
 *
 */
package com.hsjawanda.firestorerepository;

import static com.hsjawanda.utilities.base.Check.checkArgument;
import static com.hsjawanda.utilities.base.Check.checkState;
import static com.hsjawanda.utilities.repackaged.commons.lang3.StringUtils.trimToNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
@ToString(exclude = {"current"})
public class Criteria {

	private static Logger LOG;

	private List<Criterion> criteria = new ArrayList<>();

	private Criterion current;

	private Criteria() {
	}

//	public static boolean addDateCriteria(Builder bildr, String dateStr, boolean lessThan) {
//		if (isNotBlank(dateStr)) {
//			try {
//				Instant instant = OffsetDateTime.parse(dateStr).toInstant();
//				if (lessThan) {
//					bildr.property(DATE_CREATED).lessThan(Date.from(instant));
//				} else {
//					bildr.property(DATE_CREATED).greaterThan(Date.from(instant));
//				}
//				return true;
//			} catch (DateTimeParseException e) {
//				log().warn("Error parsing dateStr '{}' (lessThan: {}). Exception message: {}", dateStr, lessThan,
//						e.getMessage());
//			}
//		}
//		return false;
//	}

	public static Criteria.Builder builder() {
		return new Criteria.Builder();
	}

	public static Criteria empty() {
		return builder().build();
	}

	@SuppressWarnings("unused")
	private static Logger log() {
		if (null == LOG) {
			LOG = LoggerFactory.getLogger(Criteria.class.getName());
		}
		return LOG;
	}

	ImmutableList<Criterion> getCriteria() {
		return ImmutableList.copyOf(this.criteria);
	}

	public static class Builder {

		private Criteria c = new Criteria();

		public Criteria.Builder arrayContains(Object value) throws IllegalStateException {
			checkState(null != this.c.current && null != this.c.current.getField(),
					"No field has been set on which to operate.");
			this.c.current.setOp(Operator.ARRAY_CONTAINS).setValue(value);
			return this;
		}

		public Criteria build() {
			return this.c;
		}

		public Criteria.Builder equal(Object value) throws IllegalStateException {
			checkState(null != this.c.current && null != this.c.current.getField(),
					"No field has been set on which to operate.");
			this.c.current.setOp(Operator.EQUAL).setValue(value);
			return this;
		}

		public Criteria.Builder greaterThan(Object value) throws IllegalStateException {
			checkState(null != this.c.current && null != this.c.current.getField(),
					"No field has been set on which to operate.");
			this.c.current.setOp(Operator.GREATER_THAN).setValue(value);
			return this;
		}

		public Criteria.Builder greaterThanOrEqual(Object value) throws IllegalStateException {
			checkState(null != this.c.current && null != this.c.current.getField(),
					"No field has been set on which to operate.");
			this.c.current.setOp(Operator.GREATER_THAN_OR_EQUAL).setValue(value);
			return this;
		}

		public Criteria.Builder lessThan(Object value) throws IllegalStateException {
			checkState(null != this.c.current && null != this.c.current.getField(),
					"No field has been set on which to operate.");
			this.c.current.setOp(Operator.LESS_THAN).setValue(value);
			return this;
		}

		public Criteria.Builder lessThanOrEqual(Object value) throws IllegalStateException {
			checkState(null != this.c.current && null != this.c.current.getField(),
					"No field has been set on which to operate.");
			this.c.current.setOp(Operator.LESS_THAN_OR_EQUAL).setValue(value);
			return this;
		}

		public Criteria.Builder limit(int value) {
			this.c.current = new Criterion().setOp(Operator.LIMIT).setValue(value);
			this.c.criteria.add(this.c.current);
			return this;
		}

		public Criteria.Builder orderBy(@NonNull String property) {
			this.c.current = new Criterion().setField(property).setOp(Operator.ORDER_BY);
			this.c.criteria.add(this.c.current);
			return this;
		}

		public Criteria.Builder orderByAscending(@NonNull String property) {
			this.c.current = new Criterion().setField(property).setOp(Operator.ORDER_BY_ASC);
			this.c.criteria.add(this.c.current);
			return this;
		}

		public Criteria.Builder orderByDescending(@NonNull String property) {
			this.c.current = new Criterion().setField(property).setOp(Operator.ORDER_BY_DSC);
			this.c.criteria.add(this.c.current);
			return this;
		}

		public Criteria.Builder property(@Nonnull String propertyName) throws IllegalArgumentException {
			propertyName = trimToNull(propertyName);
			checkArgument(null != propertyName, "propertyName can't be null/empty");
			this.c.current = new Criterion().setField(propertyName);
			this.c.criteria.add(this.c.current);
			return this;
		}

		public Criteria.Builder startAfter(Object cursor) {
			if (null != cursor) {
				this.c.current = new Criterion().setOp(Operator.START_AFTER).setValue(cursor);
				this.c.criteria.add(this.c.current);
			}
			return this;
		}

		public Criteria.Builder startAt(Object cursor) {
			if (null != cursor) {
				this.c.current = new Criterion().setOp(Operator.START_AT).setValue(cursor);
				this.c.criteria.add(this.c.current);
			}
			return this;
		}

	}

	@Data
	@Accessors(chain = true)
	public static class Criterion {

		@Getter(value = AccessLevel.PACKAGE)
		@Setter(value = AccessLevel.PRIVATE)
		private String field;

		@Getter(value = AccessLevel.PACKAGE)
		@Setter(value = AccessLevel.PRIVATE)
		private Operator op;

		@Getter(value = AccessLevel.PACKAGE)
		@Setter(value = AccessLevel.PRIVATE)
		private Object value;

		private Criterion() {
		}

	}

	public enum Operator {

		ARRAY_CONTAINS, EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, LIMIT, ORDER_BY,
		ORDER_BY_ASC, ORDER_BY_DSC, START_AFTER, START_AT;

	}

}
