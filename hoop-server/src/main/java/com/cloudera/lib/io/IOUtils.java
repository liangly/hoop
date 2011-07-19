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

import com.cloudera.lib.util.Check;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class IOUtils {

  public static void delete(File file) throws IOException {
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

  public static void copy(InputStream is, OutputStream os) throws IOException {
    Check.notNull(is, "is");
    Check.notNull(os, "os");
    copy(is, os, 0, -1);
  }

  public static void copy(InputStream is, OutputStream os, long offset, long len) throws IOException {
    Check.notNull(is, "is");
    Check.notNull(os, "os");
    byte[] buffer = new byte[1024];
    long skipped = is.skip(offset);
    if (skipped == offset) {
      if (len == -1) {
        int read = is.read(buffer);
        while (read > -1) {
          os.write(buffer, 0, read);
          read = is.read(buffer);
        }
        is.close();
      }
      else {
        long count = 0;
        int read = is.read(buffer);
        while (read > -1 && count < len) {
          count += read;
          if (count < len) {
            os.write(buffer, 0, read);
            read = is.read(buffer);
          }
          else if (count == len) {
            os.write(buffer, 0, read);
          }
          else {
            int leftToWrite = read - (int) (count - len);
            os.write(buffer, 0, leftToWrite);
          }
        }
      }
      os.flush();
    }
    else {
      throw new IOException(MessageFormat.format("InputStream ended before offset [{0}]", offset));
    }
  }

  public static void copy(Reader reader, Writer writer) throws IOException {
    Check.notNull(reader, "reader");
    Check.notNull(writer, "writer");
    copy(reader, writer, 0, -1);
  }

  public static void copy(Reader reader, Writer writer, long offset, long len) throws IOException {
    Check.notNull(reader, "reader");
    Check.notNull(writer, "writer");
    Check.ge0(offset, "offset");
    char[] buffer = new char[1024];
    long skipped = reader.skip(offset);
    if (skipped == offset) {
      if (len == -1) {
        int read = reader.read(buffer);
        while (read > -1) {
          writer.write(buffer, 0, read);
          read = reader.read(buffer);
        }
        reader.close();
      }
      else {
        long count = 0;
        int read = reader.read(buffer);
        while (read > -1 && count < len) {
          count += read;
          if (count < len) {
            writer.write(buffer, 0, read);
            read = reader.read(buffer);
          }
          else if (count == len) {
            writer.write(buffer, 0, read);
          }
          else {
            int leftToWrite = read - (int) (count - len);
            writer.write(buffer, 0, leftToWrite);
          }
        }
      }
      writer.flush();
    }
    else {
      throw new IOException(MessageFormat.format("Reader ended before offset [{0}]", offset));
    }
  }

  public static String toString(Reader reader) throws IOException {
    Check.notNull(reader, "reader");
    StringWriter writer = new StringWriter();
    copy(reader, writer);
    return writer.toString();
  }


  public static void zipDir(File dir, String relativePath, ZipOutputStream zos) throws IOException {
    Check.notNull(dir, "dir");
    Check.notNull(relativePath, "relativePath");
    Check.notNull(zos, "zos");
    zipDir(dir, relativePath, zos, true);
    zos.close();
  }

  private static void zipDir(File dir, String relativePath, ZipOutputStream zos, boolean start) throws IOException {
    String[] dirList = dir.list();
    for (String aDirList : dirList) {
      File f = new File(dir, aDirList);
      if (!f.isHidden()) {
        if (f.isDirectory()) {
          if (!start) {
            ZipEntry dirEntry = new ZipEntry(relativePath + f.getName() + "/");
            zos.putNextEntry(dirEntry);
            zos.closeEntry();
          }
          String filePath = f.getPath();
          File file = new File(filePath);
          zipDir(file, relativePath + f.getName() + "/", zos, false);
        }
        else {
          ZipEntry anEntry = new ZipEntry(relativePath + f.getName());
          zos.putNextEntry(anEntry);
          InputStream is = new FileInputStream(f);
          byte[] arr = new byte[4096];
          int read = is.read(arr);
          while (read > -1) {
            zos.write(arr, 0, read);
            read = is.read(arr);
          }
          is.close();
          zos.closeEntry();
        }
      }
    }
  }

}
