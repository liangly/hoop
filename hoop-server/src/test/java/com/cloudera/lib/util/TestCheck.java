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
package com.cloudera.lib.util;

import com.cloudera.circus.test.XTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class TestCheck extends XTest {

  @Test
  public void notNullNotNull() {
    Assert.assertEquals(Check.notNull("value", "name"), "value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notNullNull() {
    Check.notNull(null, "name");
  }

  @Test
  public void notNullElementsNotNull() {
    Check.notNullElements(new ArrayList<String>(), "name");
    Check.notNullElements(Arrays.asList("a"), "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notNullElementsNullList() {
    Check.notNullElements(null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notNullElementsNullElements() {
    Check.notNullElements(Arrays.asList("a", "", null), "name");
  }

  @Test
  public void notEmptyElementsNotNull() {
    Check.notEmptyElements(new ArrayList<String>(), "name");
    Check.notEmptyElements(Arrays.asList("a"), "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notEmptyElementsNullList() {
    Check.notEmptyElements(null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notEmptyElementsNullElements() {
    Check.notEmptyElements(Arrays.asList("a", null), "name");
  }


  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notEmptyElementsEmptyElements() {
    Check.notEmptyElements(Arrays.asList("a", ""), "name");
  }


  @Test
  public void notEmptyNotEmtpy() {
    Assert.assertEquals(Check.notEmpty("value", "name"), "value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notEmptyNull() {
    Check.notEmpty(null, "name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notEmptyEmpty() {
    Check.notEmpty("", "name");
  }

  @Test
  public void validIdentifierValid() throws Exception {
    Assert.assertEquals(Check.validIdentifier("a", "", 1), "a");
    Assert.assertEquals(Check.validIdentifier("a1", "", 2), "a1");
    Assert.assertEquals(Check.validIdentifier("a_", "", 3), "a_");
    Assert.assertEquals(Check.validIdentifier("_", "", 1), "_");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void validIdentifierInvalid1() throws Exception {
    Check.validIdentifier("!", "", 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void validIdentifierInvalid2() throws Exception {
    Check.validIdentifier("a1", "", 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void validIdentifierInvalid3() throws Exception {
    Check.validIdentifier("1", "", 1);
  }

  @Test
  public void checkGTZeroGreater() {
    Assert.assertEquals(Check.gt0(120, "test"), 120);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkGTZeroZero() {
    Check.gt0(0, "test");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkGTZeroLessThanZero() {
    Check.gt0(-1, "test");
  }

  @Test
  public void checkGEZero() {
    Assert.assertEquals(Check.ge0(120, "test"), 120);
    Assert.assertEquals(Check.ge0(0, "test"), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkGELessThanZero() {
    Check.ge0(-1, "test");
  }

}
