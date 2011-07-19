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
package com.cloudera.lib.servlet;

import com.cloudera.circus.test.TestDir;
import com.cloudera.circus.test.XTest;
import com.cloudera.lib.server.Server;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class TestServerWebApp extends XTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void getHomeDirNotDef() {
    ServerWebApp.getHomeDir("TestServerWebApp00");
  }

  @Test
  public void getHomeDir() {
    System.setProperty("TestServerWebApp0.home.dir", "/tmp");
    Assert.assertEquals(ServerWebApp.getHomeDir("TestServerWebApp0"), "/tmp");
    Assert.assertEquals(ServerWebApp.getDir("TestServerWebApp0", ".log.dir", "/tmp/log"), "/tmp/log");
    System.setProperty("TestServerWebApp0.log.dir", "/tmplog");
    Assert.assertEquals(ServerWebApp.getDir("TestServerWebApp0", ".log.dir", "/tmp/log"), "/tmplog");
  }

  @Test
  @TestDir
  public void lifecycle() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    System.setProperty("TestServerWebApp1.home.dir", dir);
    System.setProperty("TestServerWebApp1.config.dir", dir);
    System.setProperty("TestServerWebApp1.log.dir", dir);
    System.setProperty("TestServerWebApp1.temp.dir", dir);
    ServerWebApp server = new ServerWebApp("TestServerWebApp1") {
    };

    Assert.assertEquals(server.getStatus(), Server.Status.UNDEF);
    server.contextInitialized(null);
    Assert.assertEquals(server.getStatus(), Server.Status.NORMAL);
    server.contextDestroyed(null);
    Assert.assertEquals(server.getStatus(), Server.Status.SHUTDOWN);
  }

  @Test(expectedExceptions = RuntimeException.class)
  @TestDir
  public void failedInit() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    System.setProperty("TestServerWebApp2.home.dir", dir);
    System.setProperty("TestServerWebApp2.config.dir", dir);
    System.setProperty("TestServerWebApp2.log.dir", dir);
    System.setProperty("TestServerWebApp2.temp.dir", dir);
    System.setProperty("testserverwebapp2.services", "FOO");
    ServerWebApp server = new ServerWebApp("TestServerWebApp2") {
    };

    server.contextInitialized(null);
  }
}
