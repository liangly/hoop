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

/**
 *
 */
public class TestJSONException extends XTest {

  @Test
  public void JSONException() throws Exception {
    JSONException ex = new JSONException(JSONException.ERROR.JS01);
    Assert.assertEquals(ex.getError(), JSONException.ERROR.JS01);
    Assert.assertTrue(ex.getMessage().startsWith(JSONException.ERROR.JS01.toString()));
  }

}
