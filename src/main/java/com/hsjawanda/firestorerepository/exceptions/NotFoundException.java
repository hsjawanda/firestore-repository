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
public class NotFoundException extends RuntimeException implements Supplier<NotFoundException> {

	/**
	 * 30/05/2019
	 */
	private static final long serialVersionUID = 1L;

	public NotFoundException(String mesg) {
		super(mesg);
	}

	public NotFoundException(String mesg, Object... args) {
		super(String.format(mesg, args));
	}

	@Override
	public NotFoundException get() {
		return this;
	}

}
