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
package com.cloudera.circus.test;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestXTest extends XTest {

  @Test(expectedExceptions = IllegalStateException.class)
  public void testDirNoAnnotation() throws Exception {
    getTestDir();
  }

  @Test
  @TestDir
  public void testDirAnnotation() throws Exception {
    Assert.assertNotNull(getTestDir());
  }

  @Test
  public void waitFor() {
    long start = System.currentTimeMillis();
    long waited = waitFor(1000, new Predicate() {
      public boolean evaluate() throws Exception {
        return true;
      }
    });
    long end = System.currentTimeMillis();
    Assert.assertEquals(waited, 0, 50);
    Assert.assertEquals(end - start - waited, 0, 50);
  }

  @Test
  public void waitForTimeOutRatio1() {
    setWaitForRatio(1);
    long start = System.currentTimeMillis();
    long waited = waitFor(200, new Predicate() {
      public boolean evaluate() throws Exception {
        return false;
      }
    });
    long end = System.currentTimeMillis();
    Assert.assertEquals(waited, -1);
    Assert.assertEquals(end - start, 200, 50);
  }

  @Test
  public void waitForTimeOutRatio2() {
    setWaitForRatio(2);
    long start = System.currentTimeMillis();
    long waited = waitFor(200, new Predicate() {
      public boolean evaluate() throws Exception {
        return false;
      }
    });
    long end = System.currentTimeMillis();
    Assert.assertEquals(waited, -1);
    Assert.assertEquals(end - start, 200 * getWaitForRatio(), 50 * getWaitForRatio());
  }

  @Test
  public void sleepRatio1() {
    setWaitForRatio(1);
    long start = System.currentTimeMillis();
    sleep(100);
    long end = System.currentTimeMillis();
    Assert.assertEquals(end - start, 100, 50);
  }

  @Test
  public void sleepRatio2() {
    setWaitForRatio(1);
    long start = System.currentTimeMillis();
    sleep(100);
    long end = System.currentTimeMillis();
    Assert.assertEquals(end - start, 100 * getWaitForRatio(), 50 * getWaitForRatio());
  }

  @Test
  @TestHadoop
  public void testHadoopMinicluster() throws Exception {
    JobConf conf = getHadoopConf();
    Assert.assertNotNull(conf);
    FileSystem fs = FileSystem.get(conf);
    Assert.assertNotNull(fs);
    Assert.assertEquals(fs.getUri().getScheme(), "hdfs");
    Assert.assertTrue(fs.exists(getHadoopTestDir()));
    fs.close();
    JobClient jobClient = new JobClient(conf);
    Assert.assertNotNull(jobClient);
    jobClient.close();
  }

  @Test
  @TestHadoop
  public void testHadoopFileSystem() throws Exception {
    JobConf conf = getHadoopConf();
    FileSystem fs = FileSystem.get(conf);
    try {
      OutputStream os = fs.create(new Path(getHadoopTestDir(), "foo"));
      os.write(new byte[]{1});
      os.close();
      InputStream is = fs.open(new Path(getHadoopTestDir(), "foo"));
      Assert.assertEquals(is.read(), 1);
      Assert.assertEquals(is.read(), -1);
      is.close();
    }
    finally {
      fs.close();
    }
  }

  @Test
  @TestHadoop
  public void testHadoopMapReduce() throws Exception {
    JobConf conf = getHadoopConf();
    FileSystem fs = FileSystem.get(conf);
    JobClient jobClient = new JobClient(conf);
    try {
      Path inputDir = new Path(getHadoopTestDir(), "input");
      Path outputDir = new Path(getHadoopTestDir(), "output");

      fs.mkdirs(inputDir);
      Writer writer = new OutputStreamWriter(fs.create(new Path(inputDir, "data.txt")));
      writer.write("a\n");
      writer.write("b\n");
      writer.write("c\n");
      writer.close();

      JobConf jobConf = getHadoopConf();
      jobConf.setInt("mapred.map.tasks", 1);
      jobConf.setInt("mapred.map.max.attempts", 1);
      jobConf.setInt("mapred.reduce.max.attempts", 1);
      jobConf.set("mapred.input.dir", inputDir.toString());
      jobConf.set("mapred.output.dir", outputDir.toString());
      final RunningJob runningJob = jobClient.submitJob(jobConf);
      waitFor(60 * 1000, true, new Predicate() {
        @Override
        public boolean evaluate() throws Exception {
          return runningJob.isComplete();
        }
      });
      Assert.assertTrue(runningJob.isSuccessful());
      Assert.assertTrue(fs.exists(new Path(outputDir, "part-00000")));
      BufferedReader reader =
        new BufferedReader(new InputStreamReader(fs.open(new Path(outputDir, "part-00000"))));
      Assert.assertTrue(reader.readLine().trim().endsWith("a"));
      Assert.assertTrue(reader.readLine().trim().endsWith("b"));
      Assert.assertTrue(reader.readLine().trim().endsWith("c"));
      Assert.assertNull(reader.readLine());
      reader.close();
    }
    finally {
      fs.close();
      jobClient.close();
    }
  }

  public static class MyServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      resp.getWriter().write("foo");
    }
  }

  @Test
  @TestServlet
  public void testJetty() throws Exception {
    Context context = new Context();
    context.setContextPath("/");
    context.addServlet(MyServlet.class, "/bar");
    Server server = getJettyServer();
    server.addHandler(context);
    server.start();
    URL url = new URL(getJettyURL(), "/bar");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    Assert.assertEquals(conn.getResponseCode(), HttpURLConnection.HTTP_OK);
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    Assert.assertEquals(reader.readLine(), "foo");
    reader.close();
  }

}
