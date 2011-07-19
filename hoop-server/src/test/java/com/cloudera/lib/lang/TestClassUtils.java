/*
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.lib.lang;

import com.cloudera.circus.test.TestDir;
import com.cloudera.circus.test.XTest;
import org.apache.log4j.spi.LoggerFactory;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;

public class TestClassUtils extends XTest {

  public static void foo() {
  }

  @SuppressWarnings({"UnusedDeclaration"})
  private static void fooPrivate() {
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public void fooInstance() {
  }

  public static final Object FOO = new Object();

  @SuppressWarnings({"UnusedDeclaration"})
  private static final Object FOO_PRIVATE = new Object();

  @SuppressWarnings({"UnusedDeclaration"})
  public final Object FOO_INSTANCE = new Object();

  @Test
  @TestDir
  public void getJar() throws Exception {
    String jar = ClassUtils.getJar(Logger.class);
    Assert.assertNotNull(jar);
    Assert.assertTrue(jar.contains("slf4j-api"));

    jar = ClassUtils.getJar(String.class);
    Assert.assertNull(jar);
  }

  @Test
  public void getClasspathResource() throws Exception {
    Assert.assertNotNull(ClassUtils.getResource("classutils.txt"));
    Assert.assertNull(ClassUtils.getResource("classutils.txt.foo"));
  }

  @Test
  public void createJar() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ClassUtils.createJar(baos, Logger.class, LoggerFactory.class);
    baos.close();
  }

  @Test
  @TestDir
  public void createJarFile() throws Exception {
    File file = new File(getTestDir(), "foo.jar");
    ClassUtils.createJar(file, Logger.class, LoggerFactory.class);
    Assert.assertTrue(file.exists());
  }

  @Test
  public void findMethod() throws Exception {
    Method method = ClassUtils.findMethod(TestClassUtils.class.getName(), "foo");
    Assert.assertNotNull(method);
    Assert.assertEquals(method.getName(), "foo");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void findMethodNoMethod() throws Exception {
    ClassUtils.findMethod(TestClassUtils.class.getName(), "foobar");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void findMethodNoStatic() throws Exception {
    ClassUtils.findMethod(TestClassUtils.class.getName(), "fooInstance");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void findMethodNoPublic() throws Exception {
    ClassUtils.findMethod(TestClassUtils.class.getName(), "fooPrivate");
  }

  @Test
  public void findConstant() throws Exception {
    Object constant = ClassUtils.findConstant(TestClassUtils.class.getName(), "FOO");
    Assert.assertEquals(constant, FOO);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void findConstantNoConstant() throws Exception {
    ClassUtils.findConstant(TestClassUtils.class.getName(), "FOOBAR");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void findConstantNoStatic() throws Exception {
    ClassUtils.findConstant(TestClassUtils.class.getName(), "FOO_INSTANCE");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void findConstantNoPublic() throws Exception {
    ClassUtils.findConstant(TestClassUtils.class.getName(), "FOO_PRIVATE");
  }
}
