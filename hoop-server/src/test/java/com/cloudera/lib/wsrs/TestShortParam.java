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

public class TestShortParam extends XTest {

  @Test
  public void param() throws Exception {
    ShortParam param = new ShortParam("p", "1") {
    };
    Assert.assertEquals(param.getDomain(), "a short");
    Assert.assertEquals(param.value(), new Short((short) 1));
    Assert.assertEquals(param.toString(), "1");
    param = new ShortParam("p", null) {
    };
    Assert.assertEquals(param.value(), null);
    param = new ShortParam("p", "") {
    };
    Assert.assertEquals(param.value(), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void invalid1() throws Exception {
    new ShortParam("p", "x") {
    };
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void invalid2() throws Exception {
    new ShortParam("p", "" + Integer.MAX_VALUE) {
    };
  }
}
