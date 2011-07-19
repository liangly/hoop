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

import org.json.simple.JSONStreamAware;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONProvider implements MessageBodyWriter<JSONStreamAware> {
  private static final String ENTER = System.getProperty("line.separator");

  @Override
  public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
    return JSONStreamAware.class.isAssignableFrom(aClass);
  }

  @Override
  public long getSize(JSONStreamAware jsonStreamAware, Class<?> aClass, Type type, Annotation[] annotations,
                      MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(JSONStreamAware jsonStreamAware, Class<?> aClass, Type type, Annotation[] annotations,
                      MediaType mediaType, MultivaluedMap<String, Object> stringObjectMultivaluedMap,
                      OutputStream outputStream) throws IOException, WebApplicationException {
    Writer writer = new OutputStreamWriter(outputStream);
    jsonStreamAware.writeJSONString(writer);
    writer.write(ENTER);
    writer.flush();
  }

}
