/**
 *
 */
package com.hsjawanda.firestorerepository.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
/**
 * @author Harshdeep S Jawanda <hsjawanda@gmail.com>
 *
 */
public @interface OnLoad {
}
