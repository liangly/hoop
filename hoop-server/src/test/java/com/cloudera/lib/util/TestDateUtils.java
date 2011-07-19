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

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TestDateUtils extends XTest {

  @Test
  public void RFC822() throws Exception {
    Assert.assertNull(DateUtils.toRFC822(null));
    Assert.assertNull(DateUtils.parseRFC822(null));
    String s1 = "Mon, 14 Mar 2011 10:00:00 GMT";
    Date d1 = DateUtils.parseRFC822(s1);
    Assert.assertNotNull(d1);
    String s2 = DateUtils.toRFC822(d1);
    Assert.assertNotNull(s2);
    Assert.assertEquals(s2, s1);
  }

  @Test
  public void UTC() throws Exception {
    Assert.assertNull(DateUtils.toUTC((Date) null));
    Assert.assertNull(DateUtils.parseUTC(null));
    String s1 = "2011-03-14T10:00Z";
    Date d1 = DateUtils.parseUTC(s1);
    Assert.assertNotNull(d1);
    String s2 = DateUtils.toUTC(d1);
    Assert.assertNotNull(s2);
    Assert.assertEquals(s2, s1);
  }

  @Test
  public void UTCCalendar() throws Exception {
    Assert.assertNull(DateUtils.toUTC((Calendar) null));
    String s1 = "2011-03-14T10:00Z";
    Date d1 = DateUtils.parseUTC(s1);
    Calendar cal = new GregorianCalendar(DateUtils.getTimeZone("UTC"));
    cal.setTime(d1);
    String s2 = DateUtils.toUTC(cal);
    Assert.assertNotNull(s2);
    Assert.assertEquals(s2, s1);
  }

  @Test
  public void getCalendar() throws Exception {
    String s1 = "2011-03-14T10:00Z";
    Date d1 = DateUtils.parseUTC(s1);
    Calendar cal = DateUtils.getCalendar(s1);
    Assert.assertEquals(cal.getTime(), d1);
    cal = DateUtils.getCalendar(s1, DateUtils.getTimeZone("UTC"));
    Assert.assertEquals(cal.getTime(), d1);
  }

  @Test
  public void getTimeZone() throws Exception {
    TimeZone tz = DateUtils.getTimeZone("UTC");
    Assert.assertEquals(tz.getID(), "UTC");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void getTimeZoneInvalid() throws Exception {
    DateUtils.getTimeZone("foobar");
  }

  @Test
  public void isDSTChangeDateFalse() throws Exception {
    Assert.assertNull(DateUtils.toUTC((Calendar) null));
    String s1 = "2011-03-14T10:00Z";
    Date d1 = DateUtils.parseUTC(s1);
    Calendar cal = new GregorianCalendar(DateUtils.getTimeZone("UTC"));
    cal.setTime(d1);
    Assert.assertFalse(DateUtils.isDSTChangeDay(cal));
  }

  @Test
  public void isDSTChangeDateTrue() throws Exception {
    Assert.assertNull(DateUtils.toUTC((Calendar) null));
    String s1 = "2011-03-13T20:00Z";
    Date d1 = DateUtils.parseUTC(s1);
    Calendar cal = new GregorianCalendar(DateUtils.getTimeZone("PST"));
    cal.setTime(d1);
    Assert.assertTrue(DateUtils.isDSTChangeDay(cal));
  }

  @Test
  public void Timestamp() throws Exception {
    Assert.assertNull(DateUtils.toTimestamp(null));
    Assert.assertNull(DateUtils.toDate(null));
    String s = "2011-03-14T10:00Z";
    Date d1 = DateUtils.parseUTC(s);
    Timestamp ts = DateUtils.toTimestamp(d1);
    Date d2 = DateUtils.toDate(ts);
    Assert.assertEquals(d2, d1);
  }

}
