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
import com.cloudera.lib.service.Groups;
import com.cloudera.lib.util.XConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public class TestGroupsService extends XTest {

  @Test
  @TestDir
  public void service() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName()), ","));
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Groups groups = server.get(Groups.class);
    Assert.assertNotNull(groups);
    List<String> g = groups.getGroups(System.getProperty("user.name"));
    Assert.assertNotSame(g.size(), 0);
    server.destroy();
  }

  @Test(expectedExceptions = RuntimeException.class)
  @TestDir
  public void invalidGroupsMapping() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", StringUtils.toString(Arrays.asList(GroupsService.class.getName()), ","));
    conf.set("server.groups.hadoop.security.group.mapping", String.class.getName());
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

}
