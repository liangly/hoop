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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class TestInputStreamEntity extends XTest {

  @Test
  public void test() throws Exception {
    InputStream is = new ByteArrayInputStream("abc".getBytes());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    InputStreamEntity i = new InputStreamEntity(is);
    i.write(baos);
    baos.close();
    Assert.assertEquals(baos.toByteArray(), "abc".getBytes());

    is = new ByteArrayInputStream("abc".getBytes());
    baos = new ByteArrayOutputStream();
    i = new InputStreamEntity(is, 1, 1);
    i.write(baos);
    baos.close();
    Assert.assertEquals(baos.toByteArray(), "b".getBytes());
  }

}
