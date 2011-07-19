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
package com.cloudera.lib.json;

import com.cloudera.lib.lang.XException;

public class JSONException extends XException {

  public static enum ERROR implements XException.ERROR {
    JS01("Parse error: {0}"),
    JS02("Invalid content: {0}");

    private String msg;

    private ERROR(String msg) {
      this.msg = msg;
    }

    @Override
    public String getTemplate() {
      return msg;
    }
  }

  public JSONException(XException ex) {
    super(ex);
  }

  public JSONException(ERROR error, Object... params) {
    super(error, params);
  }

}
