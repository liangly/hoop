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

import com.cloudera.circus.test.XTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestJSON extends XTest {

  public static class TestBean {
    String name;
  }

  public static class TestMapJSON extends JSON<TestBean> {

    @Override
    @SuppressWarnings("unchecked")
    protected Object toJSONObject(TestBean obj) {
      Map map = new HashMap();
      map.put("name", obj.name);
      return map;
    }

    @Override
    protected TestBean fromJSONObject(Object json) throws JSONException {
      Map map = (Map) json;
      TestBean bean = new TestBean();
      bean.name = (String) map.get("name");
      return bean;
    }
  }

  public static class TestArrayJSON extends JSON<List<TestBean>> {

    @Override
    @SuppressWarnings("unchecked")
    protected Object toJSONObject(List<TestBean> obj) {
      TestMapJSON testMapJSON = new TestMapJSON();
      List list = new ArrayList();
      for (TestBean bean : obj) {
        list.add(testMapJSON.toJSONObject(bean));
      }
      return list;
    }

    @Override
    protected List<TestBean> fromJSONObject(Object json) throws JSONException {
      TestMapJSON testMapJSON = new TestMapJSON();
      List<TestBean> list = new ArrayList<TestBean>();
      for (Object obj : (List) json) {
        list.add(testMapJSON.fromJSONObject(obj));
      }
      return list;
    }
  }

  @Test
  public void mapToJSONString() throws Exception {
    TestBean bean = new TestBean();
    bean.name = "NAME";

    TestMapJSON testMapJSON = new TestMapJSON();

    String str = testMapJSON.toJSONString(bean);
    Assert.assertEquals(str, "{\"name\":\"NAME\"}");

    StringWriter writer = new StringWriter();
    testMapJSON.writeJSONString(bean, writer);
    Assert.assertEquals(writer.toString(), "{\"name\":\"NAME\"}");
  }

  @Test
  public void mapParseJSONString() throws Exception {
    String json = "{\"name\":\"NAME\"}";

    TestMapJSON testMapJSON = new TestMapJSON();

    TestBean testBean = testMapJSON.parseJSONString(json);
    Assert.assertEquals(testBean.name, "NAME");

    testBean = testMapJSON.parseJSONString(new StringReader(json));
    Assert.assertEquals(testBean.name, "NAME");
  }

  @Test(expectedExceptions = JSONException.class, expectedExceptionsMessageRegExp = "JS01.*")
  public void parseException() throws Exception {
    String json = "{\"name\" - \"NAME\"}";

    TestMapJSON testMapJSON = new TestMapJSON();
    testMapJSON.parseJSONString(json);
  }

  @Test
  public void listToJSONString() throws Exception {
    TestBean bean = new TestBean();
    bean.name = "NAME";
    List<TestBean> list = new ArrayList<TestBean>();
    list.add(bean);
    TestArrayJSON testArrayJSON = new TestArrayJSON();

    String str = testArrayJSON.toJSONString(list);
    Assert.assertEquals(str, "[{\"name\":\"NAME\"}]");

    StringWriter writer = new StringWriter();
    testArrayJSON.writeJSONString(list, writer);
    Assert.assertEquals(writer.toString(), "[{\"name\":\"NAME\"}]");
  }

  @Test
  public void listParseJSONString() throws Exception {
    String json = "[{\"name\":\"NAME\"}]";

    TestArrayJSON testArrayJSON = new TestArrayJSON();

    List<TestBean> list = testArrayJSON.parseJSONString(json);
    Assert.assertEquals(1, list.size());
    Assert.assertEquals(list.get(0).name, "NAME");

    list = testArrayJSON.parseJSONString(new StringReader(json));
    Assert.assertEquals(1, list.size());
    Assert.assertEquals(list.get(0).name, "NAME");
  }

}

