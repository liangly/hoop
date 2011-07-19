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

import com.cloudera.circus.test.XTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestStringUtils extends XTest {

  @Test
  public void testToStringList() {
    Assert.assertEquals(StringUtils.toString(null, ","), "L[null]");

    List<String> list = new ArrayList<String>();
    Assert.assertEquals(StringUtils.toString(list, ","), "");

    list.add("a");
    Assert.assertEquals(StringUtils.toString(list, ","), "a");

    list.add("b");
    Assert.assertEquals(StringUtils.toString(list, ","), "a,b");
  }

}
