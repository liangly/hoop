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

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class FSUtils {
  public static final String DEFAULT_PERMISSION = "default";

  public static FsPermission getPermission(String str) {
    FsPermission permission;
    if (str.equals(DEFAULT_PERMISSION)) {
      permission = FsPermission.getDefault();
    }
    else {
      //TODO: there is something funky here, it does not detect 'x'
      permission = FsPermission.valueOf(str);
    }
    return permission;
  }

  public static Path convertPathToHoop(Path path, String hoopBaseUrl) {
    URI uri = path.toUri();
    String filePath = uri.getRawPath();
    String hoopPath = hoopBaseUrl + filePath;
    return new Path(hoopPath);
  }

  private static String permissionToString(FsPermission p) {
    return (p == null) ? "default" : "-" + p.getUserAction().SYMBOL + p.getGroupAction().SYMBOL +
                                     p.getOtherAction().SYMBOL;
  }

  @SuppressWarnings("unchecked")
  public static Map fileStatusToJSON(FileStatus status, String hoopBaseUrl) {
    Map json = new LinkedHashMap();
    json.put("path", convertPathToHoop(status.getPath(), hoopBaseUrl).toString());
    json.put("isDir", status.isDir());
    json.put("len", status.getLen());
    json.put("owner", status.getOwner());
    json.put("group", status.getGroup());
    json.put("permission", permissionToString(status.getPermission()));
    json.put("accessTime", status.getAccessTime());
    json.put("modificationTime", status.getModificationTime());
    json.put("blockSize", status.getBlockSize());
    json.put("replication", status.getReplication());
    return json;
  }

  @SuppressWarnings("unchecked")
  public static JSONArray fileStatusToJSON(FileStatus[] status, String hoopBaseUrl) {
    JSONArray json = new JSONArray();
    if (status != null) {
      for (FileStatus s : status) {
        json.add(fileStatusToJSON(s, hoopBaseUrl));
      }
    }
    return json;
  }

  @SuppressWarnings("unchecked")
  public static JSONObject toJSON(String name, Object value) {
    JSONObject json = new JSONObject();
    json.put(name, value);
    return json;
  }
}
