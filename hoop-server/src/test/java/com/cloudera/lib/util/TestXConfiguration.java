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
import org.apache.hadoop.conf.Configuration;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

public class TestXConfiguration extends XTest {

  @Test
  public void constructors() throws Exception {
    XConfiguration conf = new XConfiguration();
    Assert.assertEquals(conf.size(), 0);

    byte[] bytes = "<configuration><property><name>a</name><value>A</value></property></configuration>".getBytes();
    InputStream is = new ByteArrayInputStream(bytes);
    conf = new XConfiguration(is);
    Assert.assertEquals(conf.size(), 1);
    Assert.assertEquals(conf.get("a"), "A");

    String str = "<configuration><property><name>a</name><value>A</value></property></configuration>";
    Reader reader = new StringReader(str);
    conf = new XConfiguration(reader);
    Assert.assertEquals(conf.size(), 1);
    Assert.assertEquals(conf.get("a"), "A");

    Properties props = new Properties();
    props.setProperty("a", "A");
    conf = new XConfiguration(props);
    Assert.assertEquals(conf.size(), 1);
    Assert.assertEquals(conf.get("a"), "A");
  }

  @Test(expectedExceptions = IOException.class)
  public void constructorsFail1() throws Exception {
    new XConfiguration(new StringReader("<xonfiguration>"));
  }


  @Test(expectedExceptions = IOException.class)
  public void constructorsFail2() throws Exception {
    new XConfiguration(new StringReader("<xonfiguration></xonfiguration>"));
  }

  @Test(expectedExceptions = IOException.class)
  public void constructorsFail3() throws Exception {
    InputStream is = new ByteArrayInputStream("<xonfiguration></xonfiguration>".getBytes());
    new XConfiguration(is);
  }

  @Test(expectedExceptions = IOException.class)
  public void constructorsFail4() throws Exception {
    Reader reader = new StringReader("<configuration><qroperty></qroperty></configuration>");
    new XConfiguration(reader);
  }

  @Test(expectedExceptions = IOException.class)
  public void constructorsFail5() throws Exception {
    String str = "<configuration><property><oame>a</oname><value>A</value></property></configuration>";
    Reader reader = new StringReader(str);
    new XConfiguration(reader);
  }

  @Test(expectedExceptions = IOException.class)
  public void constructorsFail6() throws Exception {
    String str = "<configuration><property><name>a</nname><walue>A</walue></property></configuration>";
    Reader reader = new StringReader(str);
    new XConfiguration(reader);
  }

  @Test
  public void getKlass() throws Exception {
    Properties props = new Properties();
    XConfiguration conf = new XConfiguration(props);
    Assert.assertEquals(conf.getClassByName("java.lang.String "), String.class);
  }

  @Test
  public void copy() throws Exception {
    Configuration srcConf = new Configuration(false);
    Configuration targetConf = new Configuration(false);

    srcConf.set("testParameter1", "valueFromSource");
    srcConf.set("testParameter2", "valueFromSource");

    targetConf.set("testParameter2", "valueFromTarget");
    targetConf.set("testParameter3", "valueFromTarget");

    XConfiguration.copy(srcConf, targetConf);

    Assert.assertEquals("valueFromSource", targetConf.get("testParameter1"));
    Assert.assertEquals("valueFromSource", targetConf.get("testParameter2"));
    Assert.assertEquals("valueFromTarget", targetConf.get("testParameter3"));
  }

  @Test
  public void injectDefaults() throws Exception {
    Configuration srcConf = new Configuration(false);
    Configuration targetConf = new Configuration(false);

    srcConf.set("testParameter1", "valueFromSource");
    srcConf.set("testParameter2", "valueFromSource");

    targetConf.set("testParameter2", "originalValueFromTarget");
    targetConf.set("testParameter3", "originalValueFromTarget");

    XConfiguration.injectDefaults(srcConf, targetConf);

    Assert.assertEquals("valueFromSource", targetConf.get("testParameter1"));
    Assert.assertEquals("originalValueFromTarget", targetConf.get("testParameter2"));
    Assert.assertEquals("originalValueFromTarget", targetConf.get("testParameter3"));

    Assert.assertEquals("valueFromSource", srcConf.get("testParameter1"));
    Assert.assertEquals("valueFromSource", srcConf.get("testParameter2"));
    Assert.assertNull(srcConf.get("testParameter3"));
  }

  @Test
  public void trim() {
    XConfiguration conf = new XConfiguration();
    conf.set("a", " A ");
    conf.set("b", "B");
    conf = conf.trim();
    Assert.assertEquals(conf.get("a"), "A");
    Assert.assertEquals(conf.get("b"), "B");
  }

  @Test
  public void resolve() {
    XConfiguration conf = new XConfiguration();
    conf.set("a", "A");
    conf.set("b", "${a}");
    Assert.assertEquals(conf.getRaw("a"), "A");
    Assert.assertEquals(conf.getRaw("b"), "${a}");
    conf = conf.resolve();
    Assert.assertEquals(conf.getRaw("a"), "A");
    Assert.assertEquals(conf.getRaw("b"), "A");
  }

  @Test
  public void testVarResolutionAndSysProps() {
    String userName = System.getProperty("user.name");
    XConfiguration conf = new XConfiguration();
    conf.set("a", "A");
    conf.set("b", "${a}");
    conf.set("c", "${user.name}");
    conf.set("d", "${aaa}");
    Assert.assertEquals(conf.getRaw("a"), "A");
    Assert.assertEquals(conf.getRaw("b"), "${a}");
    Assert.assertEquals(conf.getRaw("c"), "${user.name}");
    Assert.assertEquals(conf.get("a"), "A");
    Assert.assertEquals(conf.get("b"), "A");
    Assert.assertEquals(conf.get("c"), userName);
    Assert.assertEquals(conf.get("d"), "${aaa}");

    conf.set("user.name", "foo");
    Assert.assertEquals(conf.get("user.name"), "foo");
  }

  @Test
  public void toXMLString() throws Exception {
    XConfiguration conf = new XConfiguration();
    conf.set("a", "A");
    String str = conf.toXmlString();
    Assert.assertTrue(str.startsWith("<?xml"));
    conf = new XConfiguration(new StringReader(str));
    Assert.assertEquals(conf.get("a"), "A");

    str = conf.toXmlString(false);
    Assert.assertFalse(str.startsWith("<?xml"));
    conf = new XConfiguration(new StringReader(str));
    Assert.assertEquals(conf.get("a"), "A");
  }

  @Test
  public void toProperties() throws Exception {
    XConfiguration conf = new XConfiguration();
    conf.set("a", "A");
    Properties props = conf.toProperties();
    Assert.assertEquals(props.size(), 1);
    Assert.assertEquals(props.getProperty("a"), "A");
  }
}
