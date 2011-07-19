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

public class TestEnumParam extends XTest {

  public static enum ENUM {
    FOO, BAR
  }

  @Test
  public void param() throws Exception {
    EnumParam<ENUM> param = new EnumParam<ENUM>("p", "FOO", ENUM.class) {
    };
    Assert.assertEquals(param.getDomain(), "FOO,BAR");
    Assert.assertEquals(param.value(), ENUM.FOO);
    Assert.assertEquals(param.toString(), "FOO");
    param = new EnumParam<ENUM>("p", null, ENUM.class) {
    };
    Assert.assertEquals(param.value(), null);
    param = new EnumParam<ENUM>("p", "", ENUM.class) {
    };
    Assert.assertEquals(param.value(), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void invalid1() throws Exception {
    new EnumParam<ENUM>("p", "x", ENUM.class) {
    };
  }

}
