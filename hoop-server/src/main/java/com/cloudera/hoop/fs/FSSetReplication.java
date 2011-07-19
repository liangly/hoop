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

import com.cloudera.lib.service.Hadoop;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.json.simple.JSONObject;

import java.io.IOException;

public class FSSetReplication implements Hadoop.FileSystemExecutor<JSONObject> {
  private Path path;
  private short replication;

  public FSSetReplication(String path, short replication) {
    this.path = new Path(path);
    this.replication = replication;
  }

  @Override
  @SuppressWarnings("unchecked")
  public JSONObject execute(FileSystem fs) throws IOException {
    boolean ret = fs.setReplication(path, replication);
    JSONObject json = new JSONObject();
    json.put("setReplication", ret);
    return json;
  }

}
