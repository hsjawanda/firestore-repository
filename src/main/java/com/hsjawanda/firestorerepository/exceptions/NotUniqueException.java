/**
 *
 */
package com.hsjawanda.firestorerepository.exceptions;

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
public class NotUniqueException extends RuntimeException {

	/**
	 * 30/05/2019
	 */
	private static final long serialVersionUID = 1L;

	public NotUniqueException(String message) {
		super(message);
	}

	public NotUniqueException(String mesg, Object... args) {
		super(String.format(mesg, args));
	}

}
