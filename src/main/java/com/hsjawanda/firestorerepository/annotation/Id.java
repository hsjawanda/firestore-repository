/**
 *
 */
package com.hsjawanda.firestorerepository.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Id {

}
