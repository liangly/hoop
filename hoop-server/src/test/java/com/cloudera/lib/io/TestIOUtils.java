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
package com.cloudera.lib.io;

import com.cloudera.circus.test.TestDir;
import com.cloudera.circus.test.XTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class TestIOUtils extends XTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void deleteTooShort() throws IOException {
    IOUtils.delete(new File("/xxx"));
  }

  @Test
  @TestDir
  public void delete() throws IOException {
    File file = new File(getTestDir(), "xxx");
    new FileOutputStream(file).close();
    IOUtils.delete(file);
    Assert.assertFalse(file.exists());

    File dir = new File(getTestDir(), "foo");
    File subdir = new File(dir, "bar");
    File subsubdir = new File(subdir, "woo");
    file = new File(subdir, "file");
    Assert.assertTrue(subsubdir.mkdirs());
    new FileOutputStream(file).close();
    IOUtils.delete(dir);
    Assert.assertFalse(dir.exists());
  }

  private byte[] createByteArray(int len, int offset, int trim) {
    byte[] arr = new byte[len];
    for (int i = 0; i < len; i++) {
      arr[i] = (byte) (i % 255);
    }
    byte[] trimmed = new byte[trim];
    System.arraycopy(arr, offset, trimmed, 0, trim);
    return trimmed;
  }

  @DataProvider(name = "copyByteStream")
  public Object[][] copyByteStreamParams() {
    return new Object[][]{
      {createByteArray(10, 0, 10), -1L, -1L, createByteArray(10, 0, 10)},
      {createByteArray(10, 0, 10), 2L, 5L, createByteArray(10, 2, 5)},
      {createByteArray(1100, 0, 1100), -1L, -1L, createByteArray(1100, 0, 1100)},
      {createByteArray(1100, 0, 1100), 0L, 1100L, createByteArray(1100, 0, 1100)},
      {createByteArray(1100, 0, 1100), 0L, 1099L, createByteArray(1100, 0, 1099)},
      {createByteArray(1100, 0, 1100), 0L, 1101L, createByteArray(1100, 0, 1100)},
    };
  }

  @Test(dataProvider = "copyByteStream")
  public void copyByteStream(byte[] data, long offset, long len, byte[] expected) throws IOException {

    ByteArrayInputStream is = new ByteArrayInputStream(data);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    if (offset == -1) {
      IOUtils.copy(is, os);
    }
    else {
      IOUtils.copy(is, os, offset, len);
    }
    os.close();
    Assert.assertEquals(os.toByteArray(), expected);
  }

  @Test(expectedExceptions = IOException.class)
  public void copyByteStreamPastEOF() throws IOException {
    byte[] data = "0123456789".getBytes();
    ByteArrayInputStream is = new ByteArrayInputStream(data);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    IOUtils.copy(is, os, 20, 5);
  }


  private char[] createCharArray(int len, int offset, int trim) {
    char[] arr = new char[len];
    for (int i = 0; i < len; i++) {
      arr[i] = (char) (i % 255);
    }
    char[] trimmed = new char[trim];
    System.arraycopy(arr, offset, trimmed, 0, trim);
    return trimmed;
  }

  @DataProvider(name = "copyCharStream")
  public Object[][] copyCharStreamParams() {
    return new Object[][]{
      {createCharArray(10, 0, 10), -1L, -1L, createCharArray(10, 0, 10)},
      {createCharArray(10, 0, 10), 2L, 5L, createCharArray(10, 2, 5)},
      {createCharArray(1100, 0, 1100), -1L, -1L, createCharArray(1100, 0, 1100)},
      {createCharArray(1100, 0, 1100), 0L, 1100L, createCharArray(1100, 0, 1100)},
      {createCharArray(1100, 0, 1100), 0L, 1099L, createCharArray(1100, 0, 1099)},
      {createCharArray(1100, 0, 1100), 0L, 1101L, createCharArray(1100, 0, 1100)},
    };
  }

  @Test(dataProvider = "copyCharStream")
  public void copyCharStream(char[] data, long offset, long len, char[] expected) throws IOException {
    StringReader reader = new StringReader(new String(data));
    StringWriter writer = new StringWriter();
    if (offset == -1) {
      IOUtils.copy(reader, writer);
    }
    else {
      IOUtils.copy(reader, writer, offset, len);
    }
    writer.close();
    Assert.assertEquals(writer.toString(), new String(expected));
  }

  @Test(expectedExceptions = IOException.class)
  public void copyCharStreamPastEOF() throws IOException {
    String data = "0123456789";
    StringReader reader = new StringReader(data);
    StringWriter writer = new StringWriter();
    IOUtils.copy(reader, writer, 20, 5);
  }

  @Test
  public void ListToString() throws IOException {
    String data = "0123456789";
    StringReader is = new StringReader(data);
    String str = IOUtils.toString(is);
    Assert.assertEquals(str, data);
  }

  @DataProvider(name = "zip")
  public Object[][] zipParams() {
    return new Object[][]{{""}, {"foo/"}};
  }

  @Test(dataProvider = "zip")
  @TestDir
  public void zip(String base) throws IOException {

    File dir = getTestDir();
    File subdir = new File(dir, "bar");
    File subsubdir = new File(subdir, "woo");
    File file = new File(subdir, "file");
    Assert.assertTrue(subsubdir.mkdirs());
    OutputStream os = new FileOutputStream(file);
    os.write('a');
    os.close();

    Set<String> contents = new HashSet<String>();
    contents.add(base + "bar/woo/");
    contents.add(base + "bar/file");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    IOUtils.zipDir(dir, base, zos);
    ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()));
    Set<String> zipContents = new HashSet<String>();
    ZipEntry entry = zis.getNextEntry();
    while (entry != null) {
      zipContents.add(entry.getName());
      entry = zis.getNextEntry();
    }
    Assert.assertEquals(zipContents, contents);
  }

}
