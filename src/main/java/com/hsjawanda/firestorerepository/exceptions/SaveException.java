/**
 *
 */
package com.hsjawanda.firestorerepository.exceptions;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public class SaveException extends RuntimeException {

	/**
	 * 27/08/2018
	 */
	private static final long serialVersionUID = 1L;

	public SaveException(String message, Throwable cause) {
		super(message, cause);
	}

}
