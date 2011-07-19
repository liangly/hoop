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

import com.cloudera.lib.lang.XException;

public class HadoopException extends XException {

  public enum ERROR implements XException.ERROR {
    H01("Service property [{0}] not defined"),
    H02("Kerberos initialization failed, {0}"),
    H03("FileSystemExecutor error, {0}"),
    H04("JobClientExecutor error, {0}"),
    H05("[{0}] validation failed, {1}"),
    H06("Property [{0}] not defined in configuration object"),
    H07("[{0}] not healthy, {1}"),
    H08(""),
    H09("Invalid Hadoop security mode [{0}]");

    private String template;

    ERROR(String template) {
      this.template = template;
    }

    @Override
    public String getTemplate() {
      return template;
    }
  }

  public HadoopException(ERROR error, Object... params) {
    super(error, params);
  }

}
