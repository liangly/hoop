package com.cloudera.circus.test;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for {@link XTest} subclasses to indicate that the test method
 * requires a test directory in the local file system.
 * <p/>
 * The test directory location can be retrieve using the
 * {@link XTest#getTestDir()} method.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.METHOD)
public @interface TestDir {
}
