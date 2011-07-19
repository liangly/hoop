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

import com.cloudera.circus.test.XTest;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class TestJSONMapProvider extends XTest {

  @Test
  @SuppressWarnings("unchecked")
  public void test() throws Exception {
    JSONMapProvider p = new JSONMapProvider();
    Assert.assertTrue(p.isWriteable(Map.class, null, null, null));
    Assert.assertFalse(p.isWriteable(XTest.class, null, null, null));
    Assert.assertEquals(p.getSize(null, null, null, null, null), -1);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    JSONObject json = new JSONObject();
    json.put("a", "A");
    p.writeTo(json, JSONObject.class, null, null, null, null, baos);
    baos.close();
    Assert.assertEquals(new String(baos.toByteArray()).trim(), "{\"a\":\"A\"}");
  }

}
