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

import com.cloudera.lib.util.Check;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class JSON<T> {

  private static class OrderContainerFactory implements ContainerFactory {

    @Override
    public Map createObjectContainer() {
      return new LinkedHashMap();
    }

    @Override
    public List creatArrayContainer() {
      return new ArrayList();
    }
  }

  protected abstract Object toJSONObject(T obj);

  protected abstract T fromJSONObject(Object json) throws JSONException;

  public String toJSONString(T obj) {
    return JSONValue.toJSONString(toJSONObject(obj));
  }

  public void writeJSONString(T obj, Writer writer) throws IOException {
    Check.notNull(writer, "writer");
    JSONValue.writeJSONString(toJSONObject(obj), writer);
  }

  public T parseJSONString(String str) throws IOException, JSONException {
    Check.notNull(str, "str");
    try {
      return parseJSONString(new StringReader(str));
    }
    catch (IOException ex) {
      throw new RuntimeException("It should not happen" + ex.getMessage(), ex);
    }
  }

  public T parseJSONString(Reader reader) throws IOException, JSONException {
    Check.notNull(reader, "reader");
    JSONParser parser = new JSONParser();
    ContainerFactory containerFactory = new OrderContainerFactory();
    try {
      Object json = parser.parse(reader, containerFactory);
      return fromJSONObject(json);
    }
    catch (ParseException ex) {
      throw new JSONException(JSONException.ERROR.JS01, ex.getMessage(), ex);
    }

  }

}
