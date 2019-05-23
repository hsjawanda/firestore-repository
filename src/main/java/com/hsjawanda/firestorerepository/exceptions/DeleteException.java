/**
 *
 */
package com.hsjawanda.firestorerepository.exceptions;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public class DeleteException extends RuntimeException {

	/**
	 * 24/08/2018
	 */
	private static final long serialVersionUID = 1L;

	public DeleteException(String message, Throwable cause) {
		super(message, cause);
	}
}
