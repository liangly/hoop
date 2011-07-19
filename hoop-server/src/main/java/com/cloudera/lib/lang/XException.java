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
package com.cloudera.lib.lang;

import com.cloudera.lib.util.Check;

import java.text.MessageFormat;

public class XException extends Exception {

  public static interface ERROR {

    public String getTemplate();

  }

  private ERROR error;

  private XException(ERROR error, String message, Throwable cause) {
    super(message, cause);
    this.error = error;
  }

  public XException(XException cause) {
    this(cause.getError(), cause.getMessage(), cause);
  }

  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
  public XException(ERROR error, Object... params) {
    this(Check.notNull(error, "error"), format(error, params), getCause(params));
  }

  public ERROR getError() {
    return error;
  }

  private static String format(ERROR error, Object... args) {
    String template = error.getTemplate();
    if (template == null) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < args.length; i++) {
        sb.append(" {").append(i).append("}");
      }
      template = sb.deleteCharAt(0).toString();
    }
    return error + ": " + MessageFormat.format(error.getTemplate(), args);
  }

  private static Throwable getCause(Object... params) {
    Throwable throwable = null;
    if (params != null && params.length > 0 && params[params.length - 1] instanceof Throwable) {
      throwable = (Throwable) params[params.length - 1];
    }
    return throwable;
  }

}
