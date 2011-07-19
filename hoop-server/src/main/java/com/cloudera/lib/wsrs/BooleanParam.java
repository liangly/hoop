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
package com.cloudera.lib.wsrs;

import java.text.MessageFormat;

public abstract class BooleanParam extends Param<Boolean> {

  public BooleanParam(String name, String str) {
    value = parseParam(name, str);
  }

  protected Boolean parse(String str) throws Exception {
    if (str.equalsIgnoreCase("true")) {
      return true;
    }
    if (str.equalsIgnoreCase("false")) {
      return false;
    }
    throw new IllegalArgumentException(MessageFormat.format("Invalid value [{0}], must be a boolean", str));
  }

  @Override
  protected String getDomain() {
    return "a boolean";
  }
}
