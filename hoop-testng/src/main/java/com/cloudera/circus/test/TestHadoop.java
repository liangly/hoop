package com.cloudera.circus.test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Annotation for {@link XTest} subclasses to indicate that the test method
 * requires a Hadoop cluster.
 * <p/>
 * The {@link XTest#getHadoopConf} returns a Hadoop JobConf preconfigured to connect
 * to the Hadoop test minicluster or the Hadoop cluster information.
 * <p/>
 * A HDFS test directory for the test will be created. The HDFS test directory
 * location can be retrieve using the {@link XTest#getHadoopTestDir()} method.
 * <p/>
 * Refer to the {@link XTest} class for details on how to use and configure
 * a Hadoop test minicluster or a real Hadoop cluster for the tests.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.METHOD)
public @interface TestHadoop {
}
