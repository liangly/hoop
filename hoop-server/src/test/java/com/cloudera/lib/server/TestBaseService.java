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
package com.cloudera.lib.server;

import com.cloudera.circus.test.XTest;
import com.cloudera.lib.util.XConfiguration;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBaseService extends XTest {

  public static class MyService extends BaseService {
    static Boolean INIT;

    public MyService() {
      super("myservice");
    }

    @Override
    protected void init() throws ServiceException {
      INIT = true;
    }

    @Override
    public Class getInterface() {
      return null;
    }
  }

  @Test
  public void baseService() throws Exception {
    BaseService service = new MyService();
    Assert.assertNull(service.getInterface());
    Assert.assertEquals(service.getPrefix(), "myservice");
    Assert.assertEquals(service.getServiceDependencies(), new Class[0]);

    Server server = Mockito.mock(Server.class);
    XConfiguration conf = new XConfiguration();
    conf.set("server.myservice.foo", "FOO");
    conf.set("server.myservice1.bar", "BAR");
    Mockito.when(server.getConfig()).thenReturn(conf);
    Mockito.when(server.getPrefixedName("myservice.foo")).thenReturn("server.myservice.foo");
    Mockito.when(server.getPrefixedName("myservice.")).thenReturn("server.myservice.");

    service.init(server);
    Assert.assertEquals(service.getPrefixedName("foo"), "server.myservice.foo");
    Assert.assertEquals(service.getServiceConfig().size(), 1);
    Assert.assertEquals(service.getServiceConfig().get("foo"), "FOO");
    Assert.assertTrue(MyService.INIT);
  }
}
