package com.cloudera.circus.test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for {@link XTest} subclasses to indicate that the test method
 * requires a Jetty servlet-container.
 * <p/>
 * The {@link XTest#getJettyServer()} returns a ready to configure Jetty
 * servlet-container. After registering contexts, servlets, filters the the Jetty
 * server must be started (<code>getJettyServer.start()</code>. The Jetty server
 * is automatically stopped at the end of the test method invocation.
 * <p/>
 * Use the {@link XTest#getJettyURL()} to obtain the base URL (schema://host:port)
 * of the Jetty server.
 * <p/>
 * Refer to the {@link XTest} class for more details.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.METHOD)
public @interface TestServlet {
}
