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
package com.cloudera.lib.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobClient;

import java.io.IOException;

public interface Hadoop {

  public interface FileSystemExecutor<T> {

    public T execute(FileSystem fs) throws IOException;
  }

  public interface JobClientExecutor<T> {

    public T execute(JobClient jobClient, FileSystem fs) throws IOException;
  }

  public <T> T execute(String user, Configuration conf, FileSystemExecutor<T> executor) throws HadoopException;

  public <T> T execute(String user, Configuration conf, JobClientExecutor<T> executor) throws HadoopException;

  public FileSystem createFileSystem(String user, Configuration conf) throws IOException, HadoopException;

  public void releaseFileSystem(FileSystem fs) throws IOException;

  public Configuration getDefaultConfiguration();

}
