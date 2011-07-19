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
package com.cloudera.hoop.fs;

import com.cloudera.lib.io.IOUtils;
import com.cloudera.lib.service.Hadoop;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FSAppend implements Hadoop.FileSystemExecutor<Void> {
  private InputStream is;
  private Path path;

  public FSAppend(InputStream is, String path) {
    this.is = is;
    this.path = new Path(path);
  }

  @Override
  public Void execute(FileSystem fs) throws IOException {
    int bufferSize = fs.getConf().getInt("hoop.buffer.size", 4096);
    OutputStream os = fs.append(path, bufferSize);
    IOUtils.copy(is, os);
    os.close();
    return null;
  }

}
