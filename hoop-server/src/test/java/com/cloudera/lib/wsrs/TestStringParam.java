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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

public class TestStringParam extends XTest {

  @Test
  public void param() throws Exception {
    StringParam param = new StringParam("p", "s") {
    };
    Assert.assertEquals(param.getDomain(), "a string");
    Assert.assertEquals(param.value(), "s");
    Assert.assertEquals(param.toString(), "s");
    param = new StringParam("p", null) {
    };
    Assert.assertEquals(param.value(), null);
    param = new StringParam("p", "") {
    };
    Assert.assertEquals(param.value(), null);

    param.setValue("S");
    Assert.assertEquals(param.value(), "S");
  }

  @Test
  public void paramRegEx() throws Exception {
    StringParam param = new StringParam("p", "Aaa", Pattern.compile("A.*")) {
    };
    Assert.assertEquals(param.getDomain(), "A.*");
    Assert.assertEquals(param.value(), "Aaa");
    Assert.assertEquals(param.toString(), "Aaa");
    param = new StringParam("p", null) {
    };
    Assert.assertEquals(param.value(), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void paramInvalidRegEx() throws Exception {
    new StringParam("p", "Baa", Pattern.compile("A.*")) {
    };
  }
}
