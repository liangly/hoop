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
import com.cloudera.lib.service.ACL;
import com.cloudera.lib.service.Groups;
import com.cloudera.lib.util.XConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.security.AccessControlException;
import java.util.Arrays;

public class TestACLService extends XTest {

  @Test
  @TestDir
  public void service() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ACLService.class.getName()), ","));
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ACL acl = server.get(ACL.class);
    Assert.assertNotNull(acl);
    server.destroy();
  }

  @Test
  @TestDir
  public void validateOwner() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ACLService.class.getName()), ","));
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ACL acl = server.get(ACL.class);
    String user = System.getProperty("user.name");
    acl.validate(user, user, null);
    acl.validate(user, user, user);

    server.destroy();
  }

  @Test
  @TestDir
  public void validateUserInACL() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ACLService.class.getName()), ","));
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ACL acl = server.get(ACL.class);
    String user = System.getProperty("user.name");
    acl.validate(user, "root", user);

    server.destroy();
  }

  @Test
  @TestDir
  public void validateUserGroupInACL() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ACLService.class.getName()), ","));
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ACL acl = server.get(ACL.class);
    String user = System.getProperty("user.name");
    Groups groups = server.get(Groups.class);
    String group = groups.getGroups(user).get(0);
    acl.validate(user, "root", "bar," + group);
    server.destroy();
  }

  @Test(expectedExceptions = AccessControlException.class)
  @TestDir
  public void validateUserNotInACL() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ACLService.class.getName()), ","));
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ACL acl = server.get(ACL.class);
    String user = System.getProperty("user.name");
    Groups groups = server.get(Groups.class);
    acl.validate(user, "root", "nobody");
    server.destroy();
  }

  @Test(expectedExceptions = AccessControlException.class)
  @TestDir
  public void validateUserNotACL() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName(),
                                                                   ACLService.class.getName()), ","));
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    ACL acl = server.get(ACL.class);
    String user = System.getProperty("user.name");
    Groups groups = server.get(Groups.class);
    acl.validate(user, "root", null);
    server.destroy();
  }

}
