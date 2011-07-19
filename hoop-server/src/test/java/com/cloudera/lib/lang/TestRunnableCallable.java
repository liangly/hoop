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

import com.cloudera.circus.test.XTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;

public class TestRunnableCallable extends XTest {

  public static class R implements Runnable {
    boolean RUN;

    @Override
    public void run() {
      RUN = true;
    }
  }

  public static class C implements Callable {
    boolean RUN;

    @Override
    public Object call() throws Exception {
      RUN = true;
      return null;
    }
  }

  public static class CEx implements Callable {

    @Override
    public Object call() throws Exception {
      throw new Exception();
    }
  }

  @Test
  public void runnable() throws Exception {
    R r = new R();
    RunnableCallable rc = new RunnableCallable(r);
    rc.run();
    Assert.assertTrue(r.RUN);

    r = new R();
    rc = new RunnableCallable(r);
    rc.call();
    Assert.assertTrue(r.RUN);

    Assert.assertEquals(rc.toString(), "R");
  }

  @Test
  public void callable() throws Exception {
    C c = new C();
    RunnableCallable rc = new RunnableCallable(c);
    rc.run();
    Assert.assertTrue(c.RUN);

    c = new C();
    rc = new RunnableCallable(c);
    rc.call();
    Assert.assertTrue(c.RUN);

    Assert.assertEquals(rc.toString(), "C");
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void callableExRun() throws Exception {
    CEx c = new CEx();
    RunnableCallable rc = new RunnableCallable(c);
    rc.run();
  }

}
