package com.cloudera.circus.test;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MiniMRCluster;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.mortbay.jetty.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Base TestNG class with conveninece functionaly for tests using:
 * <ul>
 *   <li>Java System properties setting</li>
 *   <li>Local directories</li>
 *   <li>A Hadoop cluster</li>
 *   <li>Servlets/Filters</li>
 * </ul>
 * <p/>
 * <b>Java System properties setting:</b>
 * <p/>
 * To help setting Java System Properties for test cases when using Maven and
 * IDEs (ie Eclipse, IntelliJ) in a simple an consistent way, XTest scans the
 * current directory and the parents for a <code>test.properties</code> file
 * and if present, loads it and sets all its properties as Java System
 * properties.
 * <p/>
 * This loading is done once per test JVM, when the XTest class is loaded.
 * <p/>
 * The Java System property <code>test.properties</code> can be used to make
 * <code>XTest</code> to look for a file name with a different name than
 * <code>test.properties</code> (this is useful for having different test
 * configurations).
 * <p/>
 * <b>Local Test directories:</b>
 * <p/>
 * The {@link TestDir} annotation must be used in the test method.
 * <p/>
 * All test methods of <code>XTest</code> sub-classes annotated with
 * {@link TestDir} will have a private local directory for its tests.
 * <p/>
 * The base directory for all the test directories is, by default,
 * <code>target/testdir</code>. The base directory can be set using the
 * Java System property <code>test.dir</code>. This property must be set to
 * an absolute directory.
 * <p/>
 * <b>Hadoop cluster:</b>
 * <p/>
 * The {@link TestHadoop} annotation must be used in the test method.
 * <p/>
 * The <code>XTest</code> class simplifies writing test cases that require a Hadoop
 * cluster (it supports both an embedded Hadoop test minicluster and an real Hadoop
 * cluster).
 * <p/>
 * If the Java System property <code>test.hadoop.minicluster</code> is set
 * to <code>true</code> a Hadoop test minicluster will be started.
 * <p/>
 * A single Hadoop test minicluster will be started per test JVM. If all
 * test run in the same JVM a single Hadoop test minicluster will be used.
 * <p/>
 * If the Java System property <code>test.hadoop.minicluster</code> is set
 * to <code>false</code> a real Hadoop cluster will be used. The URI for the
 * JobTracker and the NameNode must be specified using the corresponding Hadoop
 * configuration properties as Java System properties (use the
 * <code>test.properties</code> file describe above).
 * </p>
 * The {@link #getHadoopConf} returns a Hadoop JobConf preconfigured to connect
 * to the Hadoop test minicluster or the Hadoop cluster information.
 * </p>
 * <b>Servlets/Filters:</b>
 * <p/>
 * The {@link TestServlet} annotation must be used in the test method.
 * <p/>
 * The <code>XTest</code> class simplifies writing test cases that require Servlets
 * and Servlet Filters .
 * <p/>
 * The {@link #getJettyServer()} returns a ready to configure Jetty
 * servlet-container. After registering contexts, servlets, filters the the Jetty
 * server must be started (<code>getJettyServer.start()</code>. The Jetty server
 * is automatically stopped at the end of the test method invocation.
 * <p/>
 * Use the {@link #getJettyURL()} to obtain the base URL (schema://host:port)
 * of the Jetty server.
 */
@Listeners(XTest.TestMethodListener.class)
public abstract class XTest {
  protected static Logger LOG;

  public static final String TEST_PROPERTIES_PROP = "test.properties";
  public static final String TEST_DIR_PROP = "test.dir";
  public static final String TEST_WAITFOR_RATIO_PROP = "test.waitfor.ratio";

  //need to do this because it seems that TestNG starts concurrent tests before the @BeforeSuite run completes
  private static float WAITFOR_RATIO_DEFAULT = Float.parseFloat(System.getProperty(TEST_WAITFOR_RATIO_PROP, "1"));

  static String TEST_DIR_ROOT;

  private static void delete(File file) throws IOException {
    if (file.getAbsolutePath().length() < 5) {
      throw new IllegalArgumentException(
        MessageFormat.format("Path [{0}] is too short, not deleting", file.getAbsolutePath()));
    }
    if (file.exists()) {
      if (file.isDirectory()) {
        File[] children = file.listFiles();
        if (children != null) {
          for (File child : children) {
            delete(child);
          }
        }
      }
      if (!file.delete()) {
        throw new RuntimeException(MessageFormat.format("Could not delete path [{0}]", file.getAbsolutePath()));
      }
    }
  }

  /**
   * Initializes the XTest context. This method is automatically called by TestNG.
   */
  @BeforeSuite
  public void testsSetup() {
    try {
      String testFileName = System.getProperty(TEST_PROPERTIES_PROP, "test.properties");
      File currentDir = new File(testFileName).getAbsoluteFile().getParentFile();
      File testFile = new File(currentDir, testFileName);
      while (currentDir != null && !testFile.exists()) {
        testFile = new File(testFile.getAbsoluteFile().getParentFile().getParentFile(), testFileName);
        currentDir = currentDir.getParentFile();
        if (currentDir != null) {
          testFile = new File(currentDir, testFileName);
        }
      }

      if (testFile.exists()) {
        System.out.println();
        System.out.println(">>> " + TEST_PROPERTIES_PROP + " : " + testFile.getAbsolutePath());
        Properties testProperties = new Properties();
        testProperties.load(new FileReader(testFile));
        for (Map.Entry entry : testProperties.entrySet()) {
          if (!System.getProperties().containsKey(entry.getKey())) {
            System.setProperty((String) entry.getKey(), (String) entry.getValue());
          }
        }
      }
      else if (System.getProperty(TEST_PROPERTIES_PROP) != null) {
        System.err.println(MessageFormat.format("Specified test.properties file does not exist [{0}]",
                                                System.getProperty(TEST_PROPERTIES_PROP)));
        System.exit(-1);

      }
      else {
        System.out.println(">>> " + TEST_PROPERTIES_PROP + " : <NONE>");
      }

      TEST_DIR_ROOT = System.getProperty(TEST_DIR_PROP, new File("target").getAbsolutePath());
      if (!TEST_DIR_ROOT.startsWith("/")) {
        System.err.println(MessageFormat.format("System property [{0}]=[{1}] must be set to an absolute path",
                                                TEST_DIR_PROP, TEST_DIR_ROOT));
        System.exit(-1);
      }
      else if (TEST_DIR_ROOT.length() < 4) {
        System.err.println(MessageFormat.format("System property [{0}]=[{1}] must be at least 4 chars",
                                                TEST_DIR_PROP, TEST_DIR_ROOT));
        System.exit(-1);
      }

      TEST_DIR_ROOT = new File(TEST_DIR_ROOT, "testdir").getAbsolutePath();
      System.setProperty(TEST_DIR_PROP, TEST_DIR_ROOT);

      WAITFOR_RATIO_DEFAULT = Float.parseFloat(System.getProperty(TEST_WAITFOR_RATIO_PROP, "1"));
      // for the first testcase
      waitForRatio = WAITFOR_RATIO_DEFAULT;

      File dir = new File(TEST_DIR_ROOT);
      delete(dir);
      if (!dir.mkdirs()) {
        System.err.println(MessageFormat.format("Could not create test dir [{0}]", TEST_DIR_ROOT));
        System.exit(-1);
      }

      System.setProperty("test.circus", "true");

      System.out.println(">>> " + TEST_DIR_PROP + "        : " + System.getProperty(TEST_DIR_PROP));

      String log4jConfig = System.getProperty("test.log", "null");
      System.out.println(">>> test.log        : " + log4jConfig);
      if (log4jConfig.equals("null")) {
        log4jConfig = "test-null-log4j.properties";
      }
      else if (log4jConfig.equals("console")) {
        log4jConfig = "test-console-log4j.properties";
      }
      else if (log4jConfig.equals("file")) {
        log4jConfig = "test-file-log4j.properties";
        System.out.println(">>> test log file   : " + System.getProperty(TEST_DIR_PROP) + "/test.log");
      }
      else {
        System.out.println(">>> test log4j conf : " + log4jConfig);
      }
      URL log4jConfigURL = getClass().getClassLoader().getResource(log4jConfig);
      if (log4jConfigURL != null) {
        PropertyConfigurator.configure(log4jConfigURL);
      }
      else {
        PropertyConfigurator.configure(log4jConfig);
      }
      System.out.println();
      sleep(100);

      LOG = LoggerFactory.getLogger("test");
      LOG.info("TESTS START");
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static final Object HADOOP_LOCK = new Object();
  private static JobConf HADOOP_CONF = null;
  private static MiniDFSCluster DFS_CLUSTER = null;
  private static MiniMRCluster MR_CLUSTER = null;

  /**
   * Destroys the XTest context. This method is automatically called by TestNG.
   */
  @AfterSuite
  public void testsDestroy() {
    if (MR_CLUSTER != null) {
      MR_CLUSTER.shutdown();
    }
    if (DFS_CLUSTER != null) {
      DFS_CLUSTER.shutdown();
    }
    LogManager.shutdown();
  }

  private static ThreadLocal<File> TEST_DIR_TL = new InheritableThreadLocal<File>();

  private static ThreadLocal<JobConf> TEST_HADOOP_TL = new InheritableThreadLocal<JobConf>();

  private static ThreadLocal<Path> TEST_DIR_HADOOP_TL = new InheritableThreadLocal<Path>();

  private static ThreadLocal<Server> TEST_SERVLET_TL = new InheritableThreadLocal<Server>();

  private float waitForRatio = WAITFOR_RATIO_DEFAULT;

  /**
   * Returns the local test directory for the current test, only available when the
   * test method has been annotated with {@link TestDir}.
   *
   * @return the test directory for the current test. It is an full/absolute
   * <code>File</code>.
   */
  protected File getTestDir() {
    File testDir = TEST_DIR_TL.get();
    if (testDir == null) {
      throw new IllegalStateException("This test does not use @TestDir");
    }
    return testDir;
  }

  /**
   * Returns the HDFS test directory for the current test, only available when the
   * test method has been annotated with {@link TestHadoop}.
   *
   * @return the HDFS test directory for the current test. It is an full/absolute
   * <code>Path</code>.
   */
  protected Path getHadoopTestDir() {
    Path testDir = TEST_DIR_HADOOP_TL.get();
    if (testDir == null) {
      throw new IllegalStateException("This test does not use @TestHadoop");
    }
    return testDir;
  }

  /**
   * Returns a Hadoop <code>JobConf</code> preconfigured with the Hadoop cluster
   * settings for testing. This configuration is only available whe the test
   * method has been annotated with {@link TestHadoop}. Refer to {@link XTest}
   * header for details)
   *
   * @return the Hadoop <code>JobConf</code> preconfigured with the Hadoop cluster
   * settings for testing
   */
  protected JobConf getHadoopConf() {
    JobConf jobConf = TEST_HADOOP_TL.get();
    if (jobConf == null) {
      throw new IllegalStateException("This test does not use @TestHadoop");
    }
    return new JobConf(jobConf);
  }

  /**
   * Returns a Jetty server ready to be configured and the started. This server
   * is only available whe the test method has been annotated with
   * {@link TestServlet}. Refer to {@link XTest} header for details.
   * <p/>
   * Once configured, the Jetty server should be started. The server will be
   * automatically stopped when the test method ends.
   *
   * @return a Jetty server ready to be configured and the started.
   */
  protected Server getJettyServer() {
    Server server = TEST_SERVLET_TL.get();
    if (server == null) {
      throw new IllegalStateException("This test does not use @TestServlet");
    }
    return server;
  }

  /**
   * Returns the base URL (SCHEMA://HOST:PORT) of the test Jetty server
   * (see {@link #getJettyServer()}) once started.
   *
   * @return the base URL (SCHEMA://HOST:PORT) of the test Jetty server.
   */
  protected URL getJettyURL() {
    Server server = TEST_SERVLET_TL.get();
    if (server == null) {
      throw new IllegalStateException("This test does not use @TestServlet");
    }
    try {
      return new URL("http://" + server.getConnectors()[0].getHost() + ":" + server.getConnectors()[0].getPort());
    }
    catch (MalformedURLException ex) {
      throw new RuntimeException("It should never happen, " + ex.getMessage(), ex);
    }
  }

  /**
   * Sets the 'wait for ratio' used in the {@link #sleep(long)},
   * {@link #waitFor(int, Predicate)} and
   * {@link #waitFor(int, boolean, Predicate)} method for the current
   * test class.
   * <p/>
   * This is useful when running tests in slow machine for tests
   * that are time sensitive.
   *
   * @param ratio the 'wait for ratio' to set.
   */
  protected void setWaitForRatio(float ratio) {
    waitForRatio = ratio;
  }

  /*
   * Returns the 'wait for ratio' used in the {@link #sleep(long)},
   * {@link #waitFor(int, Predicate)} and
   * {@link #waitFor(int, boolean, Predicate)} methods for the current
   * test class.
   * <p/>
   * This is useful when running tests in slow machine for tests
   * that are time sensitive.
   * <p/>
   * The default value is obtained from the Java System property
   * <code>test.wait.for.ratio</code> which defaults to <code>1</code>.
   *
   * @return the 'wait for ratio' for the current test class.
   */
  protected float getWaitForRatio() {
    return waitForRatio;
  }

  private static String getTestUniqueName(ITestNGMethod testMethod) {
    String className = testMethod.getRealClass().getSimpleName();
    String methodName = testMethod.getMethodName();
    int testCount = testMethod.getCurrentInvocationCount();
    return className + "-" + methodName + "-" + Integer.toString(testCount);
  }

  private static File createTestCaseDir(ITestNGMethod testMethod) {
    File dir = new File(TEST_DIR_ROOT);
    dir = new File(dir, getTestUniqueName(testMethod));
    dir = dir.getAbsoluteFile();
    try {
      delete(dir);
    }
    catch (IOException ex) {
      throw new RuntimeException(MessageFormat.format("Could not delete test dir[{0}], {1}",
                                                      dir, ex.getMessage()), ex);
    }
    if (!dir.mkdirs()) {
      throw new RuntimeException(MessageFormat.format("Could not create test dir[{0}]", dir));
    }
    return dir;
  }

  /**
   * A predicate 'closure' used by the {@link #waitFor(int, Predicate)} and
   * {@link #waitFor(int, boolean, Predicate)} methods.
   */
  public static interface Predicate {

    /**
     * Perform a predicate evaluation.
     *
     * @return the boolean result of the evaluation.
     * @throws Exception thrown if the predicate evaluation could not evaluate.
     */
    public boolean evaluate() throws Exception;

  }

  /**
   * Makes the current thread sleep for the specified number of milliseconds.
   * <p/>
   * The sleep time is multiplied by the {@link #getWaitForRatio()}.
   *
   * @param time the number of milliseconds to sleep.
   */
  protected void sleep(long time) {
    try {
      Thread.sleep((long) (getWaitForRatio() * time));
    }
    catch (InterruptedException ex) {
      LOG.error("Sleep interrupted, {}", ex, ex);
    }
  }

  /**
   * Waits up to the specified timeout for the given {@link Predicate} to
   * become <code>true</code>, failing the test if the timeout is reached
   * and the Predicate is still <code>false</code>.
   * <p/>
   * The timeout time is multiplied by the {@link #getWaitForRatio()}.
   *
   * @param timeout the timeout in milliseconds to wait for the predicate.
   * @param predicate the predicate ot evaluate.
   * @return the effective wait, in milli-seconds until the predicate become
   * <code>true</code>.
   */
  protected long waitFor(int timeout, Predicate predicate) {
    return waitFor(timeout, false, predicate);
  }

  /**
   * Waits up to the specified timeout for the given {@link Predicate} to
   * become <code>true</code>.
   * <p/>
   * The timeout time is multiplied by the {@link #getWaitForRatio()}.
   *
   * @param timeout the timeout in milliseconds to wait for the predicate.
   * @boolean failIfTimeout indicates if the test should be failed if the
   * predicate times out.
   * @param predicate the predicate ot evaluate.
   * @return the effective wait, in milli-seconds until the predicate become
   * <code>true</code> or <code>-1</code> if the predicate did not evaluate
   * to <code>true</code>.
   */
   protected long waitFor(int timeout, boolean failIfTimeout, Predicate predicate) {
    long started = System.currentTimeMillis();
    long mustEnd = System.currentTimeMillis() + (long) (getWaitForRatio() * timeout);
    long lastEcho = 0;
    try {
      long waiting = mustEnd - System.currentTimeMillis();
      LOG.info("Waiting up to [{}] msec", waiting);
      boolean eval;
      while (!(eval = predicate.evaluate()) && System.currentTimeMillis() < mustEnd) {
        if ((System.currentTimeMillis() - lastEcho) > 5000) {
          waiting = mustEnd - System.currentTimeMillis();
          LOG.info("Waiting up to [{}] msec", waiting);
          lastEcho = System.currentTimeMillis();
        }
        Thread.sleep(100);
      }
      if (!eval) {
        if (failIfTimeout) {
          Assert.fail(MessageFormat.format("Waiting timed out after [{0}] msec", timeout));
        }
        else {
          LOG.info("Waiting timed out after [{}] msec", timeout);
        }
      }
      return (eval) ? System.currentTimeMillis() - started : -1;
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static final String HADOOP_MINICLUSTER = "test.hadoop.minicluster";
  public static final String HADOOP_PROXYUSER = "test.hadoop.proxyuser";
  public static final String HADOOP_PROXYUSER_HOSTS = "test.hadoop.proxyuser.hosts";
  public static final String HADOOP_PROXYUSER_GROUPS = "test.hadoop.proxyuser.groups";

  public static final String HADOOP_USER_PREFIX = "test.hadoop.user.";

  /**
   * Returns a valid Hadoop proxyuser for the Hadoop cluster.
   * <p/>
   * The user is read from the Java System property
   * <code>test.hadoop.proxyuser</code> which defaults to the current user
   * (java System property <code>user.name</code>).
   * <p/>
   * This property should be set in the <code>test.properties</code> file.
   * <p/>
   * When running Hadoop minicluster it is used to configure the Hadoop minicluster.
   * <p/>
   * When using an external Hadoop cluster, it is expected this property is set to
   * a valid proxy user.
   *
   * @return a valid Hadoop proxyuser for the Hadoop cluster.
   */
  protected static String getHadoopProxyUser() {
    return System.getProperty(HADOOP_PROXYUSER, System.getProperty("user.name"));
  }

  /**
   * Returns the hosts for the Hadoop proxyuser settings.
   * <p/>
   * The hosts are read from the Java System property
   * <code>test.hadoop.proxyuser.hosts</code> which defaults to <code>*</code>.
   * <p/>
   * This property should be set in the <code>test.properties</code> file.
   * <p/>
   * This property is ONLY used when running Hadoop minicluster, it is used to
   * configure the Hadoop minicluster.
   * <p/>
   * When using an external Hadoop cluster this property is ignored.
   *
   * @return the hosts for the Hadoop proxyuser settings.
   */
  protected static String getHadoopProxyUserHosts() {
    return System.getProperty(HADOOP_PROXYUSER_HOSTS, "*");
  }

  /**
   * Returns the groups for the Hadoop proxyuser settings.
   * <p/>
   * The hosts are read from the Java System property
   * <code>test.hadoop.proxyuser.groups</code> which defaults to <code>*</code>.
   * <p/>
   * This property should be set in the <code>test.properties</code> file.
   * <p/>
   * This property is ONLY used when running Hadoop minicluster, it is used to
   * configure the Hadoop minicluster.
   * <p/>
   * When using an external Hadoop cluster this property is ignored.
   *
   * @return the groups for the Hadoop proxyuser settings.
   */
  protected static String getHadoopProxyUserGroups() {
    return System.getProperty(HADOOP_PROXYUSER_GROUPS, "*");
  }

  /**
   * Returns the Hadoop users to be used for tests. These users are defined
   * in the <code>test.properties</code> file in properties of the form
   * <code>test.hadoop.user.#USER#=#GROUP1#,#GROUP2#,...</code>.
   * <p/>
   * These properties are used to configure the Hadoop minicluster user/group
   * information.
   * <p/>
   * When using an external Hadoop cluster these properties should match the
   * user/groups settings in the cluster.
   *
   * @return the Hadoop users used for testing.
   */
  protected static String[] getHadoopUsers() {
    List<String> users = new ArrayList<String>();
    for (String name : System.getProperties().stringPropertyNames()) {
      if (name.startsWith(HADOOP_USER_PREFIX)) {
        users.add(name.substring(HADOOP_USER_PREFIX.length()));
      }
    }
    return users.toArray(new String[users.size()]);
  }

  /**
   * Returns the groups a Hadoop user belongs to during tests. These users/groups
   * are defined in the <code>test.properties</code> file in properties of the
   * form <code>test.hadoop.user.#USER#=#GROUP1#,#GROUP2#,...</code>.
   * <p/>
   * These properties are used to configure the Hadoop minicluster user/group
   * information.
   * <p/>
   * When using an external Hadoop cluster these properties should match the
   * user/groups settings in the cluster.
   *
   * @return the groups of Hadoop users used for testing.
   */
  protected static String[] getHadoopUserGroups(String user) {
    String groups = System.getProperty(HADOOP_USER_PREFIX + user);
    return (groups != null) ? groups.split(",") : new String[0];
  }

  /**
   * Used by {@link XTest} to wire itself to the TestNG framework.
   */
  public static class TestMethodListener implements IInvokedMethodListener {
    File testDir;

    @Override
    public void beforeInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult) {
      TestDir td = iInvokedMethod.getTestMethod().getMethod().getAnnotation(TestDir.class);
      if (td != null) {
        testDir = createTestCaseDir(iInvokedMethod.getTestMethod());
      }
      else {
        testDir = null;
      }
      TEST_DIR_TL.set(testDir);

      TestHadoop th = iInvokedMethod.getTestMethod().getMethod().getAnnotation(TestHadoop.class);
      if (th != null) {
        synchronized (HADOOP_LOCK) {
          if (HADOOP_CONF == null) {
            JobConf conf = new JobConf();
            for (String name : System.getProperties().stringPropertyNames()) {
              conf.set(name, System.getProperty(name));
            }
            if (Boolean.parseBoolean(System.getProperty(HADOOP_MINICLUSTER, "true"))) {
              try {
                HADOOP_CONF = setUpEmbeddedHadoop(conf);
              }
              catch (Exception ex) {
                throw new RuntimeException(ex);
              }
            }
            else {
              HADOOP_CONF = conf;
            }
          }
          TEST_HADOOP_TL.set(HADOOP_CONF);

          Path testDir = new Path("./" + TEST_DIR_ROOT, getTestUniqueName(iInvokedMethod.getTestMethod()));
          try {
            // currentUser
            FileSystem fs = FileSystem.get(HADOOP_CONF);
            fs.delete(testDir, true);
            fs.mkdirs(testDir);

            // proxusers
            for (String user : getHadoopUsers()) {
              createHadoopTempDir(user, testDir);
            }
          }
          catch (Exception ex) {
            throw new RuntimeException(ex);
          }
          TEST_DIR_HADOOP_TL.set(testDir);
        }
      }

      TestServlet ts = iInvokedMethod.getTestMethod().getMethod().getAnnotation(TestServlet.class);
      if (ts != null) {
        TEST_SERVLET_TL.set(createServer());
      }
    }

    private void createHadoopTempDir(String user, final Path testDir) throws Exception {
      UserGroupInformation ugi = UserGroupInformation.createProxyUser(user,
                                                                      UserGroupInformation.getCurrentUser());
      ugi.doAs(new PrivilegedExceptionAction<Void>() {
        @Override
        public Void run() throws Exception {
          FileSystem fs = FileSystem.get(HADOOP_CONF);
          fs.delete(testDir, true);
          fs.mkdirs(testDir);
          return null;
        }
      });
    }

    @Override
    public void afterInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult) {
      TEST_DIR_TL.remove();
      TEST_HADOOP_TL.remove();
      TEST_DIR_HADOOP_TL.remove();
      if (TEST_SERVLET_TL.get() != null) {
        if (TEST_SERVLET_TL.get().isRunning()) {
          try {
            TEST_SERVLET_TL.get().stop();
          }
          catch (Exception ex) {
            throw new RuntimeException("Could not stop embedded servlet container, " + ex.getMessage(), ex);
          }
        }
        TEST_SERVLET_TL.remove();
      }
    }

    private static JobConf setUpEmbeddedHadoop(JobConf conf) throws Exception {
      if (System.getProperty("hadoop.log.dir") == null) {
        System.setProperty("hadoop.log.dir", new File(TEST_DIR_ROOT, "hadoop-log").getAbsolutePath());
      }
      if (System.getProperty("test.build.data") == null) {
        System.setProperty("test.build.data", new File(TEST_DIR_ROOT, "hadoop-data").getAbsolutePath());
      }

      int taskTrackers = 2;
      int dataNodes = 2;
      conf = new JobConf(conf);
      conf.set("fs.hdfs.impl.disable.cache", "true");
      conf.set("dfs.block.access.token.enable", "false");
      conf.set("dfs.permissions", "true");
      conf.set("hadoop.security.authentication", "simple");
      conf.set("hadoop.proxyuser." + getHadoopProxyUser() + ".hosts", getHadoopProxyUserHosts());
      conf.set("hadoop.proxyuser." + getHadoopProxyUser() + ".groups", getHadoopProxyUserGroups());
      conf.set("mapred.tasktracker.map.tasks.maximum", "4");
      conf.set("mapred.tasktracker.reduce.tasks.maximum", "4");

      String[] hadoopUsers = getHadoopUsers();
      if (hadoopUsers.length == 0) {
        throw new RuntimeException("No users/groups for Hadoop minicluster defined, use system property '" +
                                   HADOOP_USER_PREFIX + ".<USER>=<GROUPS>'");
      }
      for (String user : getHadoopUsers()) {
        String[] groups = getHadoopUserGroups(user);
        UserGroupInformation.createUserForTesting(user, groups);
      }

      DFS_CLUSTER = new MiniDFSCluster(conf, dataNodes, true, null);
      FileSystem fileSystem = DFS_CLUSTER.getFileSystem();
      fileSystem.mkdirs(new Path("/tmp"));
      fileSystem.mkdirs(new Path("/user"));
      fileSystem.mkdirs(new Path("/hadoop/mapred/system"));
      fileSystem.setPermission(new Path("/tmp"), FsPermission.valueOf("-rwxrwxrwx"));
      fileSystem.setPermission(new Path("/user"), FsPermission.valueOf("-rwxrwxrwx"));
      fileSystem.setPermission(new Path("/hadoop/mapred/system"), FsPermission.valueOf("-rwx------"));
      String nnURI = fileSystem.getUri().toString();
      int numDirs = 1;
      String[] racks = null;
      String[] hosts = null;
      MR_CLUSTER = new MiniMRCluster(0, 0, taskTrackers, nnURI, numDirs, racks, hosts, null, conf);
      return MR_CLUSTER.createJobConf(conf);
    }

    private Server createServer() {
      try {

        String host = InetAddress.getLocalHost().getHostName();
        ServerSocket ss = new ServerSocket(0);
        int port = ss.getLocalPort();
        ss.close();
        Server server = new Server(0);
        server.getConnectors()[0].setHost(host);
        server.getConnectors()[0].setPort(port);
        return server;
      }
      catch (Exception ex) {
        throw new RuntimeException("Could not stop embedded servlet container, " + ex.getMessage(), ex);
      }

    }


  }

}

