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

import com.cloudera.circus.test.TestDir;
import com.cloudera.circus.test.XTest;
import com.cloudera.lib.io.IOUtils;
import com.cloudera.lib.lang.ClassUtils;
import com.cloudera.lib.lang.StringUtils;
import com.cloudera.lib.lang.XException;
import com.cloudera.lib.util.XConfiguration;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Test(singleThreaded = true)
public class TestServer extends XTest {

  @DataProvider(name = "constructorFail")
  public Object[][] constructorFailParams() {
    return new Object[][]{
      {null, null, null, null, null, null},
      {"", null, null, null, null, null},
      {null, null, null, null, null, null},
      {"server", null, null, null, null, null},
      {"server", "", null, null, null, null},
      {"server", "foo", null, null, null, null},
      {"server", "/tmp", null, null, null, null},
      {"server", "/tmp", "", null, null, null},
      {"server", "/tmp", "foo", null, null, null},
      {"server", "/tmp", "/tmp", null, null, null},
      {"server", "/tmp", "/tmp", "", null, null},
      {"server", "/tmp", "/tmp", "foo", null, null},
      {"server", "/tmp", "/tmp", "/tmp", null, null},
      {"server", "/tmp", "/tmp", "/tmp", "", null},
      {"server", "/tmp", "/tmp", "/tmp", "foo", null},
    };
  }

  @Test(dataProvider = "constructorFail", expectedExceptions = IllegalArgumentException.class)
  public void constructorFail(String name, String homeDir, String configDir, String logDir, String tempDir,
                              XConfiguration conf) {
    new Server(name, homeDir, configDir, logDir, tempDir, conf);
  }

  @Test
  @TestDir
  public void constructorsGetters() throws Exception {
    Server server = new Server("server", "/a", "/b", "/c", "/d", new XConfiguration());
    Assert.assertEquals(server.getHomeDir(), "/a");
    Assert.assertEquals(server.getConfigDir(), "/b");
    Assert.assertEquals(server.getLogDir(), "/c");
    Assert.assertEquals(server.getTempDir(), "/d");
    Assert.assertEquals(server.getName(), "server");
    Assert.assertEquals(server.getPrefix(), "server");
    Assert.assertEquals(server.getPrefixedName("name"), "server.name");
    Assert.assertNotNull(server.getConfig());

    server = new Server("server", "/a", "/b", "/c", "/d");
    Assert.assertEquals(server.getHomeDir(), "/a");
    Assert.assertEquals(server.getConfigDir(), "/b");
    Assert.assertEquals(server.getLogDir(), "/c");
    Assert.assertEquals(server.getTempDir(), "/d");
    Assert.assertEquals(server.getName(), "server");
    Assert.assertEquals(server.getPrefix(), "server");
    Assert.assertEquals(server.getPrefixedName("name"), "server.name");
    Assert.assertNull(server.getConfig());

    server = new Server("server", getTestDir().getAbsolutePath(), new XConfiguration());
    Assert.assertEquals(server.getHomeDir(), getTestDir().getAbsolutePath());
    Assert.assertEquals(server.getConfigDir(), getTestDir() + "/conf");
    Assert.assertEquals(server.getLogDir(), getTestDir() + "/log");
    Assert.assertEquals(server.getTempDir(), getTestDir() + "/temp");
    Assert.assertEquals(server.getName(), "server");
    Assert.assertEquals(server.getPrefix(), "server");
    Assert.assertEquals(server.getPrefixedName("name"), "server.name");
    Assert.assertNotNull(server.getConfig());

    server = new Server("server", getTestDir().getAbsolutePath());
    Assert.assertEquals(server.getHomeDir(), getTestDir().getAbsolutePath());
    Assert.assertEquals(server.getConfigDir(), getTestDir() + "/conf");
    Assert.assertEquals(server.getLogDir(), getTestDir() + "/log");
    Assert.assertEquals(server.getTempDir(), getTestDir() + "/temp");
    Assert.assertEquals(server.getName(), "server");
    Assert.assertEquals(server.getPrefix(), "server");
    Assert.assertEquals(server.getPrefixedName("name"), "server.name");
    Assert.assertNull(server.getConfig());
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S01.*")
  @TestDir
  public void initNoHomeDir() throws Exception {
    File homeDir = new File(getTestDir(), "home");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = new Server("server", homeDir.getAbsolutePath(), conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S02.*")
  @TestDir
  public void initHomeDirNotDir() throws Exception {
    File homeDir = new File(getTestDir(), "home");
    new FileOutputStream(homeDir).close();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = new Server("server", homeDir.getAbsolutePath(), conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S01.*")
  @TestDir
  public void initNoConfigDir() throws Exception {
    File homeDir = new File(getTestDir(), "home");
    Assert.assertTrue(homeDir.mkdir());
    Assert.assertTrue(new File(homeDir, "log").mkdir());
    Assert.assertTrue(new File(homeDir, "temp").mkdir());
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = new Server("server", homeDir.getAbsolutePath(), conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S02.*")
  @TestDir
  public void initConfigDirNotDir() throws Exception {
    File homeDir = new File(getTestDir(), "home");
    Assert.assertTrue(homeDir.mkdir());
    Assert.assertTrue(new File(homeDir, "log").mkdir());
    Assert.assertTrue(new File(homeDir, "temp").mkdir());
    File configDir = new File(homeDir, "conf");
    new FileOutputStream(configDir).close();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = new Server("server", homeDir.getAbsolutePath(), conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S01.*")
  @TestDir
  public void initNoLogDir() throws Exception {
    File homeDir = new File(getTestDir(), "home");
    Assert.assertTrue(homeDir.mkdir());
    Assert.assertTrue(new File(homeDir, "conf").mkdir());
    Assert.assertTrue(new File(homeDir, "temp").mkdir());
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = new Server("server", homeDir.getAbsolutePath(), conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S02.*")
  @TestDir
  public void initLogDirNotDir() throws Exception {
    File homeDir = new File(getTestDir(), "home");
    Assert.assertTrue(homeDir.mkdir());
    Assert.assertTrue(new File(homeDir, "conf").mkdir());
    Assert.assertTrue(new File(homeDir, "temp").mkdir());
    File logDir = new File(homeDir, "log");
    new FileOutputStream(logDir).close();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = new Server("server", homeDir.getAbsolutePath(), conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S01.*")
  @TestDir
  public void initNoTempDir() throws Exception {
    File homeDir = new File(getTestDir(), "home");
    Assert.assertTrue(homeDir.mkdir());
    Assert.assertTrue(new File(homeDir, "conf").mkdir());
    Assert.assertTrue(new File(homeDir, "log").mkdir());
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = new Server("server", homeDir.getAbsolutePath(), conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S02.*")
  @TestDir
  public void initTempDirNotDir() throws Exception {
    File homeDir = new File(getTestDir(), "home");
    Assert.assertTrue(homeDir.mkdir());
    Assert.assertTrue(new File(homeDir, "conf").mkdir());
    Assert.assertTrue(new File(homeDir, "log").mkdir());
    File tempDir = new File(homeDir, "temp");
    new FileOutputStream(tempDir).close();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = new Server("server", homeDir.getAbsolutePath(), conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S05.*")
  @TestDir
  public void siteFileNotAFile() throws Exception {
    String homeDir = getTestDir().getAbsolutePath();
    File siteFile = new File(homeDir, "server-site.xml");
    Assert.assertTrue(siteFile.mkdir());
    Server server = new Server("server", homeDir, homeDir, homeDir, homeDir);
    server.init();
  }

  private Server createServer(XConfiguration conf) {
    return new Server("server", getTestDir().getAbsolutePath(), getTestDir().getAbsolutePath(),
                      getTestDir().getAbsolutePath(), getTestDir().getAbsolutePath(), conf);
  }

  @Test
  @TestDir
  public void log4jFile() throws Exception {
    InputStream is = ClassUtils.getResource("default-log4j.properties");
    OutputStream os = new FileOutputStream(new File(getTestDir(), "server-log4j.properties"));
    IOUtils.copy(is, os);
    is.close();
    os.close();
    XConfiguration conf = new XConfiguration();
    Server server = createServer(conf);
    server.init();
  }

  public static class LifeCycleService extends BaseService {

    public LifeCycleService() {
      super("lifecycle");
    }

    @Override
    protected void init() throws ServiceException {
      Assert.assertEquals(getServer().getStatus(), Server.Status.BOOTING);
    }

    @Override
    public void destroy() {
      Assert.assertEquals(getServer().getStatus(), Server.Status.SHUTTING_DOWN);
      super.destroy();
    }

    @Override
    public Class getInterface() {
      return LifeCycleService.class;
    }
  }

  @Test
  @TestDir
  public void lifeCycle() throws Exception {
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", LifeCycleService.class.getName());
    Server server = createServer(conf);
    Assert.assertEquals(server.getStatus(), Server.Status.UNDEF);
    server.init();
    Assert.assertNotNull(server.get(LifeCycleService.class));
    Assert.assertEquals(server.getStatus(), Server.Status.NORMAL);
    server.destroy();
    Assert.assertEquals(server.getStatus(), Server.Status.SHUTDOWN);
  }

  @Test
  @TestDir
  public void startWithStatusNotNormal() throws Exception {
    XConfiguration conf = new XConfiguration();
    conf.set("server.startup.status", "ADMIN");
    Server server = createServer(conf);
    server.init();
    Assert.assertEquals(server.getStatus(), Server.Status.ADMIN);
    server.destroy();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  @TestDir
  public void nonSeteableStatus() throws Exception {
    XConfiguration conf = new XConfiguration();
    Server server = createServer(conf);
    server.init();
    server.setStatus(Server.Status.SHUTDOWN);
  }

  public static class TestService implements Service {
    static List<String> LIFECYCLE = new ArrayList<String>();

    @Override
    public void init(Server server) throws ServiceException {
      LIFECYCLE.add("init");
    }

    @Override
    public void postInit() throws ServiceException {
      LIFECYCLE.add("postInit");
    }

    @Override
    public void destroy() {
      LIFECYCLE.add("destroy");
    }

    @Override
    public Class[] getServiceDependencies() {
      return new Class[0];
    }

    @Override
    public Class getInterface() {
      return TestService.class;
    }

    @Override
    public void serverStatusChange(Server.Status oldStatus, Server.Status newStatus) throws ServiceException {
      LIFECYCLE.add("serverStatusChange");
    }
  }

  public static class TestServiceExceptionOnStatusChange extends TestService {

    @Override
    public void serverStatusChange(Server.Status oldStatus, Server.Status newStatus) throws ServiceException {
      throw new RuntimeException();
    }
  }

  @Test
  @TestDir
  public void changeStatus() throws Exception {
    TestService.LIFECYCLE.clear();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = createServer(conf);
    server.init();
    server.setStatus(Server.Status.ADMIN);
    Assert.assertTrue(TestService.LIFECYCLE.contains("serverStatusChange"));
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S11.*")
  @TestDir
  public void changeStatusServiceException() throws Exception {
    TestService.LIFECYCLE.clear();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestServiceExceptionOnStatusChange.class.getName());
    Server server = createServer(conf);
    server.init();
  }

  @Test
  @TestDir
  public void setSameStatus() throws Exception {
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = createServer(conf);
    server.init();
    TestService.LIFECYCLE.clear();
    server.setStatus(server.getStatus());
    Assert.assertFalse(TestService.LIFECYCLE.contains("serverStatusChange"));
  }

  @Test
  @TestDir
  public void serviceLifeCycle() throws Exception {
    TestService.LIFECYCLE.clear();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", TestService.class.getName());
    Server server = createServer(conf);
    server.init();
    Assert.assertNotNull(server.get(TestService.class));
    server.destroy();
    Assert.assertEquals(TestService.LIFECYCLE, Arrays.asList("init", "postInit", "serverStatusChange", "destroy"));
  }

  @Test
  @TestDir
  public void loadingDefaultConfig() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    Server server = new Server("testserver", dir, dir, dir, dir);
    server.init();
    Assert.assertEquals(server.getConfig().get("testserver.a"), "default");
  }

  @Test
  @TestDir
  public void loadingSiteConfig() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    File configFile = new File(dir, "testserver-site.xml");
    Writer w = new FileWriter(configFile);
    w.write("<configuration><property><name>testserver.a</name><value>site</value></property></configuration>");
    w.close();
    Server server = new Server("testserver", dir, dir, dir, dir);
    server.init();
    Assert.assertEquals(server.getConfig().get("testserver.a"), "site");
  }

  @Test
  @TestDir
  public void loadingSysPropConfig() throws Exception {
    try {
      System.setProperty("testserver.a", "sysprop");
      String dir = getTestDir().getAbsolutePath();
      File configFile = new File(dir, "testserver-site.xml");
      Writer w = new FileWriter(configFile);
      w.write("<configuration><property><name>testserver.a</name><value>site</value></property></configuration>");
      w.close();
      Server server = new Server("testserver", dir, dir, dir, dir);
      server.init();
      Assert.assertEquals(server.getConfig().get("testserver.a"), "sysprop");
    }
    finally {
      System.getProperties().remove("testserver.a");
    }
  }

  @Test(expectedExceptions = IllegalStateException.class)
  @TestDir
  public void illegalState1() throws Exception {
    Server server = new Server("server", getTestDir().getAbsolutePath(), new XConfiguration());
    server.destroy();
  }

  @Test(expectedExceptions = IllegalStateException.class)
  @TestDir
  public void illegalState2() throws Exception {
    Server server = new Server("server", getTestDir().getAbsolutePath(), new XConfiguration());
    server.get(Object.class);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  @TestDir
  public void illegalState3() throws Exception {
    Server server = new Server("server", getTestDir().getAbsolutePath(), new XConfiguration());
    server.setService(null);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  @TestDir
  public void illegalState4() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    Server server = new Server("server", dir, dir, dir, dir, new XConfiguration());
    server.init();
    server.init();
  }

  private static List<String> ORDER = new ArrayList<String>();

  public abstract static class MyService implements Service, XException.ERROR {
    private String id;
    private Class serviceInterface;
    private Class[] dependencies;
    private boolean failOnInit;
    private boolean failOnDestroy;

    protected MyService(String id, Class serviceInterface, Class[] dependencies, boolean failOnInit,
                        boolean failOnDestroy) {
      this.id = id;
      this.serviceInterface = serviceInterface;
      this.dependencies = dependencies;
      this.failOnInit = failOnInit;
      this.failOnDestroy = failOnDestroy;
    }


    @Override
    public void init(Server server) throws ServiceException {
      ORDER.add(id + ".init");
      if (failOnInit) {
        throw new ServiceException(this);
      }
    }

    @Override
    public void postInit() throws ServiceException {
      ORDER.add(id + ".postInit");
    }

    @Override
    public String getTemplate() {
      return "";
    }

    @Override
    public void destroy() {
      ORDER.add(id + ".destroy");
      if (failOnDestroy) {
        throw new RuntimeException();
      }
    }

    @Override
    public Class[] getServiceDependencies() {
      return dependencies;
    }

    @Override
    public Class getInterface() {
      return serviceInterface;
    }

    @Override
    public void serverStatusChange(Server.Status oldStatus, Server.Status newStatus) throws ServiceException {
    }
  }

  public static class MyService1 extends MyService {

    public MyService1() {
      super("s1", MyService1.class, null, false, false);
    }

    protected MyService1(String id, Class serviceInterface, Class[] dependencies, boolean failOnInit,
                         boolean failOnDestroy) {
      super(id, serviceInterface, dependencies, failOnInit, failOnDestroy);
    }
  }

  public static class MyService2 extends MyService {
    public MyService2() {
      super("s2", MyService2.class, null, true, false);
    }
  }


  public static class MyService3 extends MyService {
    public MyService3() {
      super("s3", MyService3.class, null, false, false);
    }
  }

  public static class MyService1a extends MyService1 {
    public MyService1a() {
      super("s1a", MyService1.class, null, false, false);
    }
  }

  public static class MyService4 extends MyService1 {

    public MyService4() {
      super("s4a", String.class, null, false, false);
    }
  }

  public static class MyService5 extends MyService {

    public MyService5() {
      super("s5", MyService5.class, null, false, true);
    }

    protected MyService5(String id, Class serviceInterface, Class[] dependencies, boolean failOnInit,
                         boolean failOnDestroy) {
      super(id, serviceInterface, dependencies, failOnInit, failOnDestroy);
    }
  }

  public static class MyService5a extends MyService5 {

    public MyService5a() {
      super("s5a", MyService5.class, null, false, false);
    }
  }

  public static class MyService6 extends MyService {

    public MyService6() {
      super("s6", MyService6.class, new Class[]{MyService1.class}, false, false);
    }
  }

  public static class MyService7 extends MyService {

    @SuppressWarnings({"UnusedParameters"})
    public MyService7(String foo) {
      super("s6", MyService7.class, new Class[]{MyService1.class}, false, false);
    }
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S08.*")
  @TestDir
  public void invalidSservice() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", "foo");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S07.*")
  @TestDir
  public void serviceWithNoDefaultConstructor() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", MyService7.class.getName());
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S04.*")
  @TestDir
  public void serviceNotImplementingServiceInterface() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", MyService4.class.getName());
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "S10.*")
  @TestDir
  public void serviceWithMissingDependency() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf = new XConfiguration();
    String services = StringUtils.toString(Arrays.asList(MyService3.class.getName(), MyService6.class.getName()),
                                           ",");
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test
  @TestDir
  public void services() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration conf;
    Server server;

    // no services
    ORDER.clear();
    conf = new XConfiguration();
    server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Assert.assertEquals(ORDER.size(), 0);

    // 2 services init/destroy
    ORDER.clear();
    String services = StringUtils.toString(Arrays.asList(MyService1.class.getName(), MyService3.class.getName()),
                                           ",");
    conf = new XConfiguration();
    conf.set("server.services", services);
    server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Assert.assertEquals(server.get(MyService1.class).getInterface(), MyService1.class);
    Assert.assertEquals(server.get(MyService3.class).getInterface(), MyService3.class);
    Assert.assertEquals(ORDER.size(), 4);
    Assert.assertEquals(ORDER.get(0), "s1.init");
    Assert.assertEquals(ORDER.get(1), "s3.init");
    Assert.assertEquals(ORDER.get(2), "s1.postInit");
    Assert.assertEquals(ORDER.get(3), "s3.postInit");
    server.destroy();
    Assert.assertEquals(ORDER.size(), 6);
    Assert.assertEquals(ORDER.get(4), "s3.destroy");
    Assert.assertEquals(ORDER.get(5), "s1.destroy");

    // 3 services, 2nd one fails on init
    ORDER.clear();
    services = StringUtils.toString(Arrays.asList(MyService1.class.getName(), MyService2.class.getName(),
                                                  MyService3.class.getName()), ",");
    conf = new XConfiguration();
    conf.set("server.services", services);

    server = new Server("server", dir, dir, dir, dir, conf);
    try {
      server.init();
      Assert.fail();
    }
    catch (ServerException ex) {
      Assert.assertEquals(MyService2.class, ex.getError().getClass());
    }
    catch (Exception ex) {
      Assert.fail();
    }
    Assert.assertEquals(ORDER.size(), 3);
    Assert.assertEquals(ORDER.get(0), "s1.init");
    Assert.assertEquals(ORDER.get(1), "s2.init");
    Assert.assertEquals(ORDER.get(2), "s1.destroy");

    // 2 services one fails on destroy
    ORDER.clear();
    services = StringUtils.toString(Arrays.asList(MyService1.class.getName(), MyService5.class.getName()), ",");
    conf = new XConfiguration();
    conf.set("server.services", services);
    server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Assert.assertEquals(ORDER.size(), 4);
    Assert.assertEquals(ORDER.get(0), "s1.init");
    Assert.assertEquals(ORDER.get(1), "s5.init");
    Assert.assertEquals(ORDER.get(2), "s1.postInit");
    Assert.assertEquals(ORDER.get(3), "s5.postInit");
    server.destroy();
    Assert.assertEquals(ORDER.size(), 6);
    Assert.assertEquals(ORDER.get(4), "s5.destroy");
    Assert.assertEquals(ORDER.get(5), "s1.destroy");


    // service override via ext
    ORDER.clear();
    services = StringUtils.toString(Arrays.asList(MyService1.class.getName(), MyService3.class.getName()), ",");
    String servicesExt = StringUtils.toString(Arrays.asList(MyService1a.class.getName()), ",");

    conf = new XConfiguration();
    conf.set("server.services", services);
    conf.set("server.services.ext", servicesExt);
    server = new Server("server", dir, dir, dir, dir, conf);
    server.init();

    Assert.assertEquals(server.get(MyService1.class).getClass(), MyService1a.class);
    Assert.assertEquals(ORDER.size(), 4);
    Assert.assertEquals(ORDER.get(0), "s1a.init");
    Assert.assertEquals(ORDER.get(1), "s3.init");
    Assert.assertEquals(ORDER.get(2), "s1a.postInit");
    Assert.assertEquals(ORDER.get(3), "s3.postInit");
    server.destroy();
    Assert.assertEquals(ORDER.size(), 6);
    Assert.assertEquals(ORDER.get(4), "s3.destroy");
    Assert.assertEquals(ORDER.get(5), "s1a.destroy");

    // service override via setService
    ORDER.clear();
    services = StringUtils.toString(Arrays.asList(MyService1.class.getName(), MyService3.class.getName()), ",");
    conf = new XConfiguration();
    conf.set("server.services", services);
    server = new Server("server", dir, dir, dir, dir, conf);
    server.init();

    server.setService(MyService1a.class);
    Assert.assertEquals(ORDER.size(), 6);
    Assert.assertEquals(ORDER.get(4), "s1.destroy");
    Assert.assertEquals(ORDER.get(5), "s1a.init");

    Assert.assertEquals(server.get(MyService1.class).getClass(), MyService1a.class);

    server.destroy();
    Assert.assertEquals(ORDER.size(), 8);
    Assert.assertEquals(ORDER.get(6), "s3.destroy");
    Assert.assertEquals(ORDER.get(7), "s1a.destroy");

    // service add via setService
    ORDER.clear();
    services = StringUtils.toString(Arrays.asList(MyService1.class.getName(), MyService3.class.getName()), ",");
    conf = new XConfiguration();
    conf.set("server.services", services);
    server = new Server("server", dir, dir, dir, dir, conf);
    server.init();

    server.setService(MyService5.class);
    Assert.assertEquals(ORDER.size(), 5);
    Assert.assertEquals(ORDER.get(4), "s5.init");

    Assert.assertEquals(server.get(MyService5.class).getClass(), MyService5.class);

    server.destroy();
    Assert.assertEquals(ORDER.size(), 8);
    Assert.assertEquals(ORDER.get(5), "s5.destroy");
    Assert.assertEquals(ORDER.get(6), "s3.destroy");
    Assert.assertEquals(ORDER.get(7), "s1.destroy");

    // service add via setService exception
    ORDER.clear();
    services = StringUtils.toString(Arrays.asList(MyService1.class.getName(), MyService3.class.getName()), ",");
    conf = new XConfiguration();
    conf.set("server.services", services);
    server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    try {
      server.setService(MyService7.class);
      Assert.fail();
    }
    catch (ServerException ex) {
      Assert.assertEquals(ServerException.ERROR.S09, ex.getError());
    }
    catch (Exception ex) {
      Assert.fail();
    }
    Assert.assertEquals(ORDER.size(), 6);
    Assert.assertEquals(ORDER.get(4), "s3.destroy");
    Assert.assertEquals(ORDER.get(5), "s1.destroy");

    // service with dependency
    ORDER.clear();
    services = StringUtils.toString(Arrays.asList(MyService1.class.getName(), MyService6.class.getName()), ",");
    conf = new XConfiguration();
    conf.set("server.services", services);
    server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Assert.assertEquals(server.get(MyService1.class).getInterface(), MyService1.class);
    Assert.assertEquals(server.get(MyService6.class).getInterface(), MyService6.class);
    server.destroy();
  }

}
