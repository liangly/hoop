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
package com.cloudera.lib.service.security;

import com.cloudera.circus.test.TestDir;
import com.cloudera.circus.test.XTest;
import com.cloudera.lib.lang.StringUtils;
import com.cloudera.lib.server.Server;
import com.cloudera.lib.server.ServiceException;
import com.cloudera.lib.service.Groups;
import com.cloudera.lib.service.ProxyUser;
import com.cloudera.lib.util.XConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.security.AccessControlException;
import java.util.Arrays;
import java.util.List;

public class TestProxyUserService extends XTest {

  @Test
  @TestDir
  public void service() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ProxyUser proxyUser = server.get(ProxyUser.class);
    Assert.assertNotNull(proxyUser);
    server.destroy();
  }

  @Test(expectedExceptions = ServiceException.class, expectedExceptionsMessageRegExp = "PRXU02.*")
  @TestDir
  public void wrongConfigGroups() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    conf.set("server.proxyuser.foo.hosts", "*");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test(expectedExceptions = ServiceException.class, expectedExceptionsMessageRegExp = "PRXU01.*")
  @TestDir
  public void wrongHost() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    conf.set("server.proxyuser.foo.hosts", "otherhost");
    conf.set("server.proxyuser.foo.groups", "*");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test(expectedExceptions = ServiceException.class, expectedExceptionsMessageRegExp = "PRXU02.*")
  @TestDir
  public void wrongConfigHosts() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    conf.set("server.proxyuser.foo.groups", "*");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test
  @TestDir
  public void validateAnyHostAnyUser() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    conf.set("server.proxyuser.foo.hosts", "*");
    conf.set("server.proxyuser.foo.groups", "*");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ProxyUser proxyUser = server.get(ProxyUser.class);
    Assert.assertNotNull(proxyUser);
    proxyUser.validate("foo", "localhost", "bar");
    server.destroy();
  }

  @Test(expectedExceptions = AccessControlException.class)
  @TestDir
  public void invalidProxyUser() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    conf.set("server.proxyuser.foo.hosts", "*");
    conf.set("server.proxyuser.foo.groups", "*");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ProxyUser proxyUser = server.get(ProxyUser.class);
    Assert.assertNotNull(proxyUser);
    proxyUser.validate("bar", "localhost", "foo");
    server.destroy();
  }

  @Test
  @TestDir
  public void validateHost() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    conf.set("server.proxyuser.foo.hosts", "localhost");
    conf.set("server.proxyuser.foo.groups", "*");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ProxyUser proxyUser = server.get(ProxyUser.class);
    Assert.assertNotNull(proxyUser);
    proxyUser.validate("foo", "localhost", "bar");
    server.destroy();
  }

  private String getGroup() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName()), ","));
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Groups groups = server.get(Groups.class);
    List<String> g = groups.getGroups(System.getProperty("user.name"));
    server.destroy();
    return g.get(0);
  }

  @Test
  @TestDir
  public void validateGroup() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    conf.set("server.proxyuser.foo.hosts", "*");
    conf.set("server.proxyuser.foo.groups", getGroup());
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ProxyUser proxyUser = server.get(ProxyUser.class);
    Assert.assertNotNull(proxyUser);
    proxyUser.validate("foo", "localhost", System.getProperty("user.name"));
    server.destroy();
  }


  @Test(expectedExceptions = AccessControlException.class)
  @TestDir
  public void unknownHost() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    conf.set("server.proxyuser.foo.hosts", "localhost");
    conf.set("server.proxyuser.foo.groups", "*");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ProxyUser proxyUser = server.get(ProxyUser.class);
    Assert.assertNotNull(proxyUser);
    proxyUser.validate("foo", "unknownhost.bar.foo", "bar");
    server.destroy();
  }

  @Test(expectedExceptions = AccessControlException.class)
  @TestDir
  public void invalidHost() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    conf.set("server.proxyuser.foo.hosts", "localhost");
    conf.set("server.proxyuser.foo.groups", "*");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ProxyUser proxyUser = server.get(ProxyUser.class);
    Assert.assertNotNull(proxyUser);
    proxyUser.validate("foo", "www.yahoo.com", "bar");
    server.destroy();
  }

  @Test(expectedExceptions = AccessControlException.class)
  @TestDir
  public void invalidGroup() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ProxyUserService.class.getName()), ","));
    conf.set("server.proxyuser.foo.hosts", "localhost");
    conf.set("server.proxyuser.foo.groups", "nobody");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ProxyUser proxyUser = server.get(ProxyUser.class);
    Assert.assertNotNull(proxyUser);
    proxyUser.validate("foo", "localhost", System.getProperty("user.name"));
    server.destroy();
  }
}
