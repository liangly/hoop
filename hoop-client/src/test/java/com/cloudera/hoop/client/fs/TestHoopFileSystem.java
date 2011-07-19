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
package com.cloudera.hoop.client.fs;

import com.cloudera.circus.test.TestDir;
import com.cloudera.circus.test.TestHadoop;
import com.cloudera.circus.test.TestServlet;
import com.cloudera.circus.test.XTest;
import com.cloudera.hoop.HoopServer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedExceptionAction;

@Test(singleThreaded = true)
public class TestHoopFileSystem extends XTest {

  private void createHoopServer() throws Exception {
    File homeDir = getTestDir();
    Assert.assertTrue(new File(homeDir, "conf").mkdir());
    Assert.assertTrue(new File(homeDir, "log").mkdir());
    Assert.assertTrue(new File(homeDir, "temp").mkdir());
    HoopServer.setHomeDirForCurrentThread(homeDir.getAbsolutePath());

    String fsDefaultName = getHadoopConf().get("fs.default.name");
    Configuration conf = new Configuration(false);
    conf.set("hoop.hadoop.conf:fs.default.name", fsDefaultName);
    conf.set("hoop.base.url", getJettyURL().toExternalForm());
    conf.set("hoop.proxyuser." + getHadoopProxyUser() + ".groups", getHadoopProxyUserGroups());
    conf.set("hoop.proxyuser." + getHadoopProxyUser() + ".hosts", getHadoopProxyUserHosts());
    File hoopSite = new File(new File(homeDir, "conf"), "hoop-site.xml");
    OutputStream os = new FileOutputStream(hoopSite);
    conf.writeXml(os);
    os.close();

    File currentDir = new File("foo").getAbsoluteFile().getParentFile();
    if (currentDir.getName().equals("target")) {
      currentDir = currentDir.getParentFile();
    }
    if (currentDir.getName().equals("hoop-client")) {
      currentDir = currentDir.getParentFile();
    }
    File hoopDir = new File(currentDir, "hoop-webapp");
    Assert.assertTrue(hoopDir.exists(), "Could not locate hoop-webapp source dir: " + hoopDir.getAbsolutePath());
    String hoopWebAppDir = new File(new File(new File(hoopDir, "src"), "main"), "webapp").getAbsolutePath();
    WebAppContext context = new WebAppContext(hoopWebAppDir, "/");

    Server server = getJettyServer();
    server.addHandler(context);
    server.start();
  }

  private void testGet() throws Exception {
    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    FileSystem fs = FileSystem.get(getJettyURL().toURI(), conf);
    Assert.assertNotNull(fs);
    Assert.assertEquals(fs.getUri(), getJettyURL().toURI());
    fs.close();
  }

  private void testOpen() throws Exception {
    FileSystem fs = FileSystem.get(getHadoopConf());
    Path path = new Path(getHadoopTestDir(), "foo.txt");
    OutputStream os = fs.create(path);
    os.write(1);
    os.close();
    fs.close();
    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    fs = FileSystem.get(getJettyURL().toURI(), conf);
    InputStream is = fs.open(new Path(path.toUri().getPath()));
    Assert.assertEquals(is.read(), 1);
    is.close();
    fs.close();
  }

  private void testCreate(Path path, boolean override) throws Exception {
    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    FileSystem fs = FileSystem.get(getJettyURL().toURI(), conf);
    FsPermission permission = new FsPermission(FsAction.READ_WRITE, FsAction.NONE, FsAction.NONE);
    OutputStream os = fs.create(new Path(path.toUri().getPath()), permission, override, 1024,
                                (short) 2, 100 * 1024 * 1024, null);
    os.write(1);
    os.close();
    fs.close();

    fs = FileSystem.get(getHadoopConf());
    FileStatus status = fs.getFileStatus(path);
    Assert.assertEquals(status.getReplication(), 2);
    Assert.assertEquals(status.getBlockSize(), 100 * 1024 * 1024);
    Assert.assertEquals(status.getPermission(), permission);
    InputStream is = fs.open(path);
    Assert.assertEquals(is.read(), 1);
    is.close();
    fs.close();
  }

  private void testCreate() throws Exception {
    Path path = new Path(getHadoopTestDir(), "foo.txt");
    testCreate(path, false);
    testCreate(path, true);
    try {
      testCreate(path, false);
      Assert.fail();
    }
    catch (IOException ex) {

    }
    catch (Exception ex) {
      Assert.fail();
    }
  }

  private void testAppend() throws Exception {
    FileSystem fs = FileSystem.get(getHadoopConf());
    Path path = new Path(getHadoopTestDir(), "foo.txt");
    OutputStream os = fs.create(path);
    os.write(1);
    os.close();
    fs.close();
    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    fs = FileSystem.get(getJettyURL().toURI(), conf);
    os = fs.append(new Path(path.toUri().getPath()));
    os.write(2);
    os.close();
    fs.close();
    fs = FileSystem.get(getHadoopConf());
    InputStream is = fs.open(path);
    Assert.assertEquals(is.read(), 1);
    Assert.assertEquals(is.read(), 2);
    Assert.assertEquals(is.read(), -1);
    is.close();
    fs.close();
  }

  private void testRename() throws Exception {
    FileSystem fs = FileSystem.get(getHadoopConf());
    Path path = new Path(getHadoopTestDir(), "foo");
    fs.mkdirs(path);
    fs.close();
    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    fs = FileSystem.get(getJettyURL().toURI(), conf);
    Path oldPath = new Path(path.toUri().getPath());
    Path newPath = new Path(path.getParent(), "bar");
    fs.rename(oldPath, newPath);
    fs.close();
    fs = FileSystem.get(getHadoopConf());
    Assert.assertFalse(fs.exists(oldPath));
    Assert.assertTrue(fs.exists(newPath));
    fs.close();
  }

  private void testDelete() throws Exception {
    Path foo = new Path(getHadoopTestDir(), "foo");
    Path bar = new Path(getHadoopTestDir(), "bar");
    Path foe = new Path(getHadoopTestDir(), "foe");
    FileSystem fs = FileSystem.get(getHadoopConf());
    fs.mkdirs(foo);
    fs.mkdirs(new Path(bar, "a"));
    fs.mkdirs(foe);

    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    FileSystem hoopFs = FileSystem.get(getJettyURL().toURI(), conf);
    Assert.assertTrue(hoopFs.delete(new Path(foo.toUri().getPath()), false));
    Assert.assertFalse(fs.exists(foo));
    try {
      hoopFs.delete(new Path(bar.toUri().getPath()), false);
      Assert.fail();
    }
    catch (IOException ex) {
    }
    catch (Exception ex) {
      Assert.fail();
    }
    Assert.assertTrue(fs.exists(bar));
    Assert.assertTrue(hoopFs.delete(new Path(bar.toUri().getPath()), true));
    Assert.assertFalse(fs.exists(bar));

    Assert.assertTrue(fs.exists(foe));
    Assert.assertTrue(hoopFs.delete(foe));
    Assert.assertFalse(fs.exists(foe));

    hoopFs.close();
    fs.close();
  }

  private void testListStatus() throws Exception {
    FileSystem fs = FileSystem.get(getHadoopConf());
    Path path = new Path(getHadoopTestDir(), "foo.txt");
    OutputStream os = fs.create(path);
    os.write(1);
    os.close();
    FileStatus status1 = fs.getFileStatus(path);
    fs.close();

    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    fs = FileSystem.get(getJettyURL().toURI(), conf);
    FileStatus status2 = fs.getFileStatus(new Path(path.toUri().getPath()));
    fs.close();

    Assert.assertEquals(status2.getPermission(), status1.getPermission());
    Assert.assertEquals(status2.getPath().toUri().getPath(), status1.getPath().toUri().getPath());
    Assert.assertEquals(status2.getReplication(), status1.getReplication());
    Assert.assertEquals(status2.getBlockSize(), status1.getBlockSize());
    Assert.assertEquals(status2.getAccessTime(), status1.getAccessTime());
    Assert.assertEquals(status2.getModificationTime(), status1.getModificationTime());
    Assert.assertEquals(status2.getOwner(), status1.getOwner());
    Assert.assertEquals(status2.getGroup(), status1.getGroup());
    Assert.assertEquals(status2.getLen(), status1.getLen());

    FileStatus[] stati = fs.listStatus(path.getParent());
    Assert.assertEquals(stati.length, 1);
    Assert.assertEquals(stati[0].getPath().getName(), path.getName());
  }

  private void testWorkingdirectory() throws Exception {
    FileSystem fs = FileSystem.get(getHadoopConf());
    Path workingDir = fs.getWorkingDirectory();
    fs.close();

    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    fs = FileSystem.get(getJettyURL().toURI(), conf);
    Path hoopWorkingDir = fs.getWorkingDirectory();
    fs.close();
    Assert.assertEquals(hoopWorkingDir.toUri().getPath(), workingDir.toUri().getPath());

    conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    fs = FileSystem.get(getJettyURL().toURI(), conf);
    fs.setWorkingDirectory(new Path("/tmp"));
    workingDir = fs.getWorkingDirectory();
    fs.close();
    Assert.assertEquals(workingDir.toUri().getPath(), new Path("/tmp").toUri().getPath());
  }

  private void testMkdirs() throws Exception {
    Path path = new Path(getHadoopTestDir(), "foo");
    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    FileSystem fs = FileSystem.get(getJettyURL().toURI(), conf);
    fs.mkdirs(path);
    fs.close();
    fs = FileSystem.get(getHadoopConf());
    Assert.assertTrue(fs.exists(path));
    fs.close();
  }

  private void testSetTimes() throws Exception {
    FileSystem fs = FileSystem.get(getHadoopConf());
    Path path = new Path(getHadoopTestDir(), "foo.txt");
    OutputStream os = fs.create(path);
    os.write(1);
    os.close();
    FileStatus status1 = fs.getFileStatus(path);
    fs.close();
    long at = status1.getAccessTime();
    long mt = status1.getModificationTime();

    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    fs = FileSystem.get(getJettyURL().toURI(), conf);
    fs.setTimes(path, mt + 10, at + 20);
    fs.close();

    fs = FileSystem.get(getHadoopConf());
    status1 = fs.getFileStatus(path);
    fs.close();
    long atNew = status1.getAccessTime();
    long mtNew = status1.getModificationTime();
    Assert.assertEquals(mtNew, mt + 10);
    Assert.assertEquals(atNew, at + 20);
  }

  private void testSetPermission() throws Exception {
    FileSystem fs = FileSystem.get(getHadoopConf());
    Path path = new Path(getHadoopTestDir(), "foo.txt");
    OutputStream os = fs.create(path);
    os.write(1);
    os.close();
    fs.close();

    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    fs = FileSystem.get(getJettyURL().toURI(), conf);
    FsPermission permission1 = new FsPermission(FsAction.READ_WRITE, FsAction.NONE, FsAction.NONE);
    fs.setPermission(path, permission1);
    fs.close();

    fs = FileSystem.get(getHadoopConf());
    FileStatus status1 = fs.getFileStatus(path);
    fs.close();
    FsPermission permission2 = status1.getPermission();
    Assert.assertEquals(permission2, permission1);
  }

  private void testSetOwner() throws Exception {
    FileSystem fs = FileSystem.get(getHadoopConf());
    Path path = new Path(getHadoopTestDir(), "foo.txt");
    OutputStream os = fs.create(path);
    os.write(1);
    os.close();
    fs.close();

    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    fs = FileSystem.get(getJettyURL().toURI(), conf);
    String user = getHadoopUsers()[1];
    String group = getHadoopUserGroups(user)[0];
    fs.setOwner(path, user, group);
    fs.close();

    fs = FileSystem.get(getHadoopConf());
    FileStatus status1 = fs.getFileStatus(path);
    fs.close();
    Assert.assertEquals(status1.getOwner(), user);
    Assert.assertEquals(status1.getGroup(), group);
  }

  private void testSetReplication() throws Exception {
    FileSystem fs = FileSystem.get(getHadoopConf());
    Path path = new Path(getHadoopTestDir(), "foo.txt");
    OutputStream os = fs.create(path);
    os.write(1);
    os.close();
    fs.close();
    fs.setReplication(path, (short) 2);

    Configuration conf = new Configuration();
    conf.set("fs.http.impl", HoopFileSystem.class.getName());
    fs = FileSystem.get(getJettyURL().toURI(), conf);
    fs.setReplication(path, (short) 1);
    fs.close();

    fs = FileSystem.get(getHadoopConf());
    FileStatus status1 = fs.getFileStatus(path);
    fs.close();
    Assert.assertEquals(status1.getReplication(), (short) 1);
  }

  private enum Operation {
    GET, OPEN, CREATE, APPEND, RENAME, DELETE, LIST_STATUS, WORKING_DIRECTORY, MKDIRS,
    SET_TIMES, SET_PERMISSION, SET_OWNER, SET_REPLICATION
  }

  @DataProvider
  public Object[][] Operations() {
    Object[][] ops = new Object[Operation.values().length][];
    for (int i = 0; i < Operation.values().length; i++) {
      ops[i] = new Object[]{Operation.values()[i]};
    }
    return ops;
  }

  private void operation(Operation op) throws Exception {
    switch (op) {
      case GET:
        testGet();
        break;
      case OPEN:
        testOpen();
        break;
      case CREATE:
        testCreate();
        break;
      case APPEND:
        testAppend();
        break;
      case RENAME:
        testRename();
        break;
      case DELETE:
        testDelete();
        break;
      case LIST_STATUS:
        testListStatus();
        break;
      case WORKING_DIRECTORY:
        testWorkingdirectory();
        break;
      case MKDIRS:
        testMkdirs();
        break;
      case SET_TIMES:
        testSetTimes();
        break;
      case SET_PERMISSION:
        testSetPermission();
        break;
      case SET_OWNER:
        testSetOwner();
        break;
      case SET_REPLICATION:
        testSetReplication();
        break;
    }
  }

  @Test(dataProvider = "Operations")
  @TestDir
  @TestServlet
  @TestHadoop
  public void testOperation(final Operation op) throws Exception {
    createHoopServer();
    operation(op);
  }

  @Test(dataProvider = "Operations")
  @TestDir
  @TestServlet
  @TestHadoop
  public void testOperationDoAs(final Operation op) throws Exception {
    createHoopServer();
    UserGroupInformation ugi = UserGroupInformation.createProxyUser(getHadoopUsers()[0],
                                                                    UserGroupInformation.getCurrentUser());
    ugi.doAs(new PrivilegedExceptionAction<Void>() {
      @Override
      public Void run() throws Exception {
        operation(op);
        return null;
      }
    });
  }

}
