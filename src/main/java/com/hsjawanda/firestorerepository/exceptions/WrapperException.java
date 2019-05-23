/**
 *
 */
package com.hsjawanda.firestorerepository.exceptions;

import java.util.function.Supplier;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class WrapperException extends RuntimeException implements Supplier<WrapperException> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public WrapperException(String format, Object... args) {
		super(String.format(format, args));
	}

	public WrapperException(String reason, Throwable cause) {
		super(reason, cause);
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	public WrapperException get() {
		return this;
	}

	@Override
	public WrapperException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}

}
