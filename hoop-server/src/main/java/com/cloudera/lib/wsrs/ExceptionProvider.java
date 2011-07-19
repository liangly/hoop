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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExceptionProvider implements ExceptionMapper<Throwable> {
  private static Logger LOG = LoggerFactory.getLogger(ExceptionProvider.class);

  private static final String ENTER = System.getProperty("line.separator");

  protected Response createResponse(Response.Status status, Throwable throwable, boolean includeTrace) {
    Map<String, Object> json = new LinkedHashMap<String, Object>();
    json.put("statusCode", status.getStatusCode());
    json.put("reason", status.getReasonPhrase());
    json.put("message", getOneLineMessage(throwable));
    json.put("exception", throwable.getClass().getName());
    if (includeTrace) {
      StringWriter writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer);
      throwable.printStackTrace(printWriter);
      printWriter.close();
      json.put("trace", writer.toString());
    }
    log(status, throwable);
    return Response.status(status).type(MediaType.APPLICATION_JSON).entity(json).build();
  }

  protected String getOneLineMessage(Throwable throwable) {
    String message = throwable.getMessage();
    if (message != null) {
      int i = message.indexOf(ENTER);
      if (i > -1) {
        message = message.substring(0, i);
      }
    }
    return message;
  }

  protected void log(Response.Status status, Throwable throwable) {
    LOG.debug("{}", throwable.getMessage(), throwable);
  }

  @Override
  public Response toResponse(Throwable throwable) {
    return createResponse(Response.Status.BAD_REQUEST, throwable, false);
  }

}
