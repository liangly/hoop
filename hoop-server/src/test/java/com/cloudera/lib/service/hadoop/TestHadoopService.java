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
package com.cloudera.lib.service.hadoop;

import com.cloudera.circus.test.TestDir;
import com.cloudera.circus.test.TestHadoop;
import com.cloudera.circus.test.XTest;
import com.cloudera.lib.lang.StringUtils;
import com.cloudera.lib.server.Server;
import com.cloudera.lib.server.ServiceException;
import com.cloudera.lib.service.Hadoop;
import com.cloudera.lib.service.HadoopException;
import com.cloudera.lib.service.instrumentation.InstrumentationService;
import com.cloudera.lib.util.XConfiguration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;

@Test(singleThreaded = true)
public class TestHadoopService extends XTest {

  @Test
  @TestDir
  public void simpleSecurity() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Assert.assertNotNull(server.get(Hadoop.class));
    server.destroy();
  }

  @Test(expectedExceptions = ServiceException.class, expectedExceptionsMessageRegExp = "H01.*")
  @TestDir
  public void noKerberosKeytabProperty() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    conf.set("server.hadoop.authentication.type", "kerberos");
    conf.set("server.hadoop.authentication.kerberos.keytab", " ");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test(expectedExceptions = ServiceException.class, expectedExceptionsMessageRegExp = "H01.*")
  @TestDir
  public void noKerberosPrincipalProperty() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    conf.set("server.hadoop.authentication.type", "kerberos");
    conf.set("server.hadoop.authentication.kerberos.keytab", "/tmp/foo");
    conf.set("server.hadoop.authentication.kerberos.principal", " ");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test(expectedExceptions = ServiceException.class, expectedExceptionsMessageRegExp = "H02.*")
  @TestDir
  public void kerberosInitializationFailure() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    conf.set("server.hadoop.authentication.type", "kerberos");
    conf.set("server.hadoop.authentication.kerberos.keytab", "/tmp/foo");
    conf.set("server.hadoop.authentication.kerberos.principal", "foo@FOO");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test(expectedExceptions = ServiceException.class, expectedExceptionsMessageRegExp = "H09.*")
  @TestDir
  public void invalidSecurity() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    conf.set("server.hadoop.authentication.type", "foo");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
  }

  @Test
  @TestDir
  public void serviceHadoopConf() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    conf.set("server.hadoop.conf:foo", "FOO");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    HadoopService hadoop = (HadoopService) server.get(Hadoop.class);
    Assert.assertEquals(hadoop.serviceHadoopConf.get("foo"), "FOO");
    server.destroy();
  }

  @Test
  @TestDir
  public void inWhitelists() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    HadoopService hadoop = (HadoopService) server.get(Hadoop.class);
    hadoop.validateJobtracker("JT");
    hadoop.validateNamenode("NN");
    server.destroy();

    conf = new XConfiguration();
    conf.set("server.services", services);
    conf.set("server.hadoop.job.tracker.whitelist", "*");
    conf.set("server.hadoop.name.node.whitelist", "*");
    server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    hadoop = (HadoopService) server.get(Hadoop.class);
    hadoop.validateJobtracker("JT");
    hadoop.validateNamenode("NN");
    server.destroy();

    conf = new XConfiguration();
    conf.set("server.services", services);
    conf.set("server.hadoop.job.tracker.whitelist", "JT");
    conf.set("server.hadoop.name.node.whitelist", "NN");
    server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    hadoop = (HadoopService) server.get(Hadoop.class);
    hadoop.validateJobtracker("JT");
    hadoop.validateNamenode("NN");
    server.destroy();
  }

  @Test(expectedExceptions = HadoopException.class, expectedExceptionsMessageRegExp = "H05.*")
  @TestDir
  public void JobTrackerNotinWhitelist() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    conf.set("server.hadoop.job.tracker.whitelist", "JT");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    HadoopService hadoop = (HadoopService) server.get(Hadoop.class);
    hadoop.validateJobtracker("JTT");
  }

  @Test(expectedExceptions = HadoopException.class, expectedExceptionsMessageRegExp = "H05.*")
  @TestDir
  public void NameNodeNotinWhitelists() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    conf.set("server.hadoop.name.node.whitelist", "NN");
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    HadoopService hadoop = (HadoopService) server.get(Hadoop.class);
    hadoop.validateNamenode("NNx");
  }

  @Test
  @TestDir
  @TestHadoop
  public void createFileSystem() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Hadoop hadoop = server.get(Hadoop.class);
    FileSystem fs = hadoop.createFileSystem("u", getHadoopConf());
    Assert.assertNotNull(fs);
    fs.mkdirs(new Path("/tmp/foo"));
    hadoop.releaseFileSystem(fs);
    try {
      fs.mkdirs(new Path("/tmp/foo"));
      Assert.fail();
    }
    catch (IOException ex) {
    }
    catch (Exception ex) {
      Assert.fail();
    }
    server.destroy();
  }

  @Test
  @TestDir
  @TestHadoop
  public void fileSystemExecutor() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Hadoop hadoop = server.get(Hadoop.class);

    final FileSystem fsa[] = new FileSystem[1];

    hadoop.execute("u", getHadoopConf(), new Hadoop.FileSystemExecutor<Void>() {
      @Override
      public Void execute(FileSystem fs) throws IOException {
        fs.mkdirs(new Path("/tmp/foo"));
        fsa[0] = fs;
        return null;
      }
    });
    try {
      fsa[0].mkdirs(new Path("/tmp/foo"));
      Assert.fail();
    }
    catch (IOException ex) {
    }
    catch (Exception ex) {
      Assert.fail();
    }
    server.destroy();
  }

  @Test(expectedExceptions = HadoopException.class, expectedExceptionsMessageRegExp = "H06.*")
  @TestDir
  @TestHadoop
  public void fileSystemExecutorNoNameNode() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Hadoop hadoop = server.get(Hadoop.class);

    JobConf jobConf = getHadoopConf();
    jobConf.set("fs.default.name", "");
    hadoop.execute("u", jobConf, new Hadoop.FileSystemExecutor<Void>() {
      @Override
      public Void execute(FileSystem fs) throws IOException {
        return null;
      }
    });
  }

  @Test
  @TestDir
  @TestHadoop
  public void fileSystemExecutorException() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Hadoop hadoop = server.get(Hadoop.class);

    final FileSystem fsa[] = new FileSystem[1];
    try {
      hadoop.execute("u", getHadoopConf(), new Hadoop.FileSystemExecutor<Void>() {
        @Override
        public Void execute(FileSystem fs) throws IOException {
          fsa[0] = fs;
          throw new IOException();
        }
      });
      Assert.fail();
    }
    catch (HadoopException ex) {
      Assert.assertEquals(ex.getError(), HadoopException.ERROR.H03);
    }
    catch (Exception ex) {
      Assert.fail();
    }

    try {
      fsa[0].mkdirs(new Path("/tmp/foo"));
      Assert.fail();
    }
    catch (IOException ex) {
    }
    catch (Exception ex) {
      Assert.fail();
    }
    server.destroy();
  }

  @Test(expectedExceptions = HadoopException.class, expectedExceptionsMessageRegExp = "H06.*")
  @TestDir
  @TestHadoop
  public void jobClientExecutorNoNameNode() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Hadoop hadoop = server.get(Hadoop.class);

    JobConf jobConf = getHadoopConf();
    jobConf.set("fs.default.name", "");
    hadoop.execute("u", jobConf, new Hadoop.JobClientExecutor<Void>() {
      @Override
      public Void execute(JobClient jobClient, FileSystem fs) throws IOException {
        return null;
      }
    });
  }

  @Test(expectedExceptions = HadoopException.class, expectedExceptionsMessageRegExp = "H06.*")
  @TestDir
  @TestHadoop
  public void jobClientExecutorNoJobTracker() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Hadoop hadoop = server.get(Hadoop.class);

    JobConf jobConf = getHadoopConf();
    jobConf.set("mapred.job.tracker", "");
    hadoop.execute("u", jobConf, new Hadoop.JobClientExecutor<Void>() {
      @Override
      public Void execute(JobClient jobClient, FileSystem fs) throws IOException {
        return null;
      }
    });
  }

  @Test
  @TestDir
  @TestHadoop
  public void jobClientExecutor() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Hadoop hadoop = server.get(Hadoop.class);

    final JobClient jca[] = new JobClient[1];
    final FileSystem fsa[] = new FileSystem[1];

    hadoop.execute("u", getHadoopConf(), new Hadoop.JobClientExecutor<Void>() {
      @Override
      public Void execute(JobClient jc, FileSystem fs) throws IOException {
        fs.mkdirs(new Path("/tmp/foo"));
        jc.getQueues();
        jca[0] = jc;
        fsa[0] = fs;
        return null;
      }
    });
    // NOT testing JobClient as the closed one still connects to the JobTracker successfully
    //        try {
    //            jca[0].submitJob(jobConf);
    //            Assert.fail();
    //        }
    //        catch (IOException ex) {
    //        }
    //        catch (Exception ex) {
    //            Assert.fail();
    //        }
    try {
      fsa[0].mkdirs(new Path("/tmp/foo"));
      Assert.fail();
    }
    catch (IOException ex) {
    }
    catch (Exception ex) {
      Assert.fail();
    }
    server.destroy();
  }

  @Test
  @TestDir
  @TestHadoop
  public void jobClientExecutorException() throws Exception {
    String dir = getTestDir().getAbsolutePath();
    String services = StringUtils.toString(Arrays.asList(InstrumentationService.class.getName(),
                                                         HadoopService.class.getName()), ",");
    XConfiguration conf = new XConfiguration();
    conf.set("server.services", services);
    Server server = new Server("server", dir, dir, dir, dir, conf);
    server.init();
    Hadoop hadoop = server.get(Hadoop.class);

    final JobClient jca[] = new JobClient[1];
    final FileSystem fsa[] = new FileSystem[1];

    try {
      hadoop.execute("u", getHadoopConf(), new Hadoop.JobClientExecutor<Void>() {
        @Override
        public Void execute(JobClient jc, FileSystem fs) throws IOException {
          jca[0] = jc;
          fsa[0] = fs;
          throw new IOException();
        }
      });
      Assert.fail();
    }
    catch (HadoopException ex) {
      Assert.assertEquals(ex.getError(), HadoopException.ERROR.H04);
    }
    catch (Exception ex) {
      Assert.fail();
    }

    // NOT testing JobClient as the closed one still connects to the JobTracker successfully
    //        try {
    //            jca[0].submitJob(jobConf);
    //            Assert.fail();
    //        }
    //        catch (IOException ex) {
    //        }
    //        catch (Exception ex) {
    //            Assert.fail();
    //        }
    try {
      fsa[0].mkdirs(new Path("/tmp/foo"));
      Assert.fail();
    }
    catch (IOException ex) {
    }
    catch (Exception ex) {
      Assert.fail();
    }
    server.destroy();
  }

}
