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
package com.cloudera.lib.server;

import com.cloudera.lib.lang.XException;

public class ServerException extends XException {

  public static enum ERROR implements XException.ERROR {
    S01("Dir [{0}] does not exist"),
    S02("[{0}] is not a directory"),
    S03("Could not load file from classpath [{0}], {1}"),
    S04("Service [{0}] does not implement declared interface [{1}]"),
    S05("[{0}] is not a file"),
    S06("Could not load file [{0}], {1}"),
    S07("Could not instanciate service class [{0}], {1}"),
    S08("Could not load service classes, {0}"),
    S09("Could not set service [{0}] programmatically -server shutting down-, {1}"),
    S10("Service [{0}] requires service [{1}]"),
    S11("Service [{0}] exception during status change to [{1}] -server shutting down-, {2}");

    private String msg;

    private ERROR(String msg) {
      this.msg = msg;
    }

    @Override
    public String getTemplate() {
      return msg;
    }
  }

  protected ServerException(XException.ERROR error, Object... params) {
    super(error, params);
  }

  public ServerException(ERROR error, Object... params) {
    super(error, params);
  }

}
