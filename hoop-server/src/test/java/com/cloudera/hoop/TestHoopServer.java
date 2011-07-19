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
package com.cloudera.hoop;

import com.cloudera.circus.test.TestDir;
import com.cloudera.circus.test.TestHadoop;
import com.cloudera.circus.test.TestServlet;
import com.cloudera.circus.test.XTest;
import com.cloudera.lib.util.XConfiguration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;

@Test(singleThreaded = true)
public class TestHoopServer extends XTest {

  @Test
  @TestDir
  @TestServlet
  public void server() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    XConfiguration hoopConf = new XConfiguration();
    HoopServer server = new HoopServer(dir, dir, dir, dir, hoopConf);
    server.init();
    server.destroy();
  }

  private void createHoopServer() throws Exception {
    File homeDir = getTestDir();
    Assert.assertTrue(new File(homeDir, "conf").mkdir());
    Assert.assertTrue(new File(homeDir, "log").mkdir());
    Assert.assertTrue(new File(homeDir, "temp").mkdir());
    HoopServer.setHomeDirForCurrentThread(homeDir.getAbsolutePath());

    String fsDefaultName = getHadoopConf().get("fs.default.name");
    XConfiguration conf = new XConfiguration();
    conf.set("hoop.hadoop.conf:fs.default.name", fsDefaultName);
    conf.set("hoop.base.url", getJettyURL().toExternalForm());
    File hoopSite = new File(new File(homeDir, "conf"), "hoop-site.xml");
    OutputStream os = new FileOutputStream(hoopSite);
    conf.writeXml(os);
    os.close();

    File dir = new File("foo").getAbsoluteFile().getParentFile();
    if (!new File(dir, "hoop-webapp").exists()) {
      dir = dir.getParentFile();
      if (!new File(dir, "hoop-webapp").exists()) {
        Assert.fail("Could not locate hoop-webapp source dir");
      }
    }
    String hoopWebAppDir =
      new File(new File(new File(new File(dir, "hoop-webapp"), "src"), "main"), "webapp").getAbsolutePath();
    WebAppContext context = new WebAppContext(hoopWebAppDir, "/");

    Server server = getJettyServer();
    server.addHandler(context);
    server.start();
  }

  @Test
  @TestDir
  @TestServlet
  @TestHadoop
  public void instrumentation() throws Exception {
    createHoopServer();

    URL url = new URL(getJettyURL(), MessageFormat.format("?user.name={0}&op=instrumentation", "nobody"));
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    Assert.assertEquals(conn.getResponseCode(), HttpURLConnection.HTTP_UNAUTHORIZED);

    url = new URL(getJettyURL(), MessageFormat.format("?user.name={0}&op=instrumentation", "root"));
    conn = (HttpURLConnection) url.openConnection();
    Assert.assertEquals(conn.getResponseCode(), HttpURLConnection.HTTP_OK);
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String line = reader.readLine();
    reader.close();
    Assert.assertTrue(line.contains("\"counters\":{"));

    url = new URL(getJettyURL(), MessageFormat.format("/foo?user.name={0}&op=instrumentation", "root"));
    conn = (HttpURLConnection) url.openConnection();
    Assert.assertEquals(conn.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
  }

  @Test
  @TestDir
  @TestServlet
  @TestHadoop
  public void testHdfsAccess() throws Exception {
    createHoopServer();

    String user = XTest.getHadoopUsers()[0];
    URL url = new URL(getJettyURL(), MessageFormat.format("/?user.name={0}&op=list", user));
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    Assert.assertEquals(conn.getResponseCode(), HttpURLConnection.HTTP_OK);
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    reader.readLine();
    reader.close();
  }

  @Test
  @TestDir
  @TestServlet
  @TestHadoop
  public void testGlobFilter() throws Exception {
    createHoopServer();

    FileSystem fs = FileSystem.get(getHadoopConf());
    fs.create(new Path("foo.txt")).close();

    String user = XTest.getHadoopUsers()[0];
    URL url = new URL(getJettyURL(), MessageFormat.format("/?user.name={0}&op=list&filter=f*", user));
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    Assert.assertEquals(conn.getResponseCode(), HttpURLConnection.HTTP_OK);
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    reader.readLine();
    reader.close();
  }

  @Test
  @TestDir
  @TestServlet
  @TestHadoop
  public void testFavicon() throws Exception {
    createHoopServer();

    String user = XTest.getHadoopUsers()[0];
    URL url = new URL(getJettyURL(), MessageFormat.format("/favicon.ico?user.name={0}", user));
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    Assert.assertEquals(conn.getResponseCode(), HttpURLConnection.HTTP_OK);
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    reader.readLine();
    reader.close();
  }

  @Test
  @TestDir
  @TestServlet
  @TestHadoop
  public void testPutNoOperation() throws Exception {
    createHoopServer();

    String user = XTest.getHadoopUsers()[0];
    URL url = new URL(getJettyURL(), MessageFormat.format("/foo?user.name={0}", user));
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoInput(true);
    conn.setDoOutput(true);
    conn.setRequestMethod("PUT");
    Assert.assertEquals(conn.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
  }

}
