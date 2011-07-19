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

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

  private static final TimeZone GMT = getTimeZone("GMT");

  public static String toRFC822(Date date) {
    String str = null;
    if (date != null) {
      SimpleDateFormat dateFormater = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
      dateFormater.setTimeZone(GMT);
      str = dateFormater.format(date);
    }
    return str;
  }

  public static Date parseRFC822(String str) throws ParseException {
    Date date = null;
    if (str != null) {
      SimpleDateFormat dateFormater = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
      dateFormater.setTimeZone(GMT);
      date = dateFormater.parse(str);
    }
    return date;
  }

  private static final TimeZone UTC = getTimeZone("UTC");

  private static DateFormat getISO8601DateFormat() {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    dateFormat.setTimeZone(UTC);
    return dateFormat;
  }

  public static TimeZone getTimeZone(String tzId) {
    Check.notEmpty(tzId, "tzId");
    TimeZone tz = TimeZone.getTimeZone(tzId);
    if (!tz.getID().equals(tzId)) {
      throw new IllegalArgumentException("Invalid TimeZone: " + tzId);
    }
    return tz;
  }

  public static Date parseUTC(String str) throws ParseException {
    Date date = null;
    if (str != null) {
      return getISO8601DateFormat().parse(str);
    }
    return date;
  }

  public static String toUTC(Date date) throws Exception {
    String str = null;
    if (date != null) {
      str = getISO8601DateFormat().format(date);
    }
    return str;
  }

  public static String toUTC(Calendar calendar) throws Exception {
    String str = null;
    if (calendar != null) {
      str = toUTC(calendar.getTime());
    }
    return str;
  }

  public static int getHoursInDay(Calendar calendar) {
    Check.notNull(calendar, "calendar");
    Calendar localCal = new GregorianCalendar(calendar.getTimeZone());
    localCal.set(Calendar.MILLISECOND, 0);
    localCal.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                 calendar.get(Calendar.DAY_OF_MONTH), 0, 30, 0);
    localCal.add(Calendar.HOUR_OF_DAY, 24);
    switch (localCal.get(Calendar.HOUR_OF_DAY)) {
      case 1:
        return 23;
      case 23:
        return 25;
      case 0:
      default:
        return 24;
    }
  }

  public static boolean isDSTChangeDay(Calendar calendar) {
    return getHoursInDay(calendar) != 24;
  }

  public static Calendar getCalendar(String strDate, TimeZone tz) throws Exception {
    Check.notEmpty(strDate, "strDate");
    Date date = DateUtils.parseUTC(strDate);
    Calendar calDate = Calendar.getInstance();
    calDate.setTime(date);
    calDate.setTimeZone(tz);
    return calDate;
  }

  public static Calendar getCalendar(String strDate) throws Exception {
    return getCalendar(strDate, DateUtils.getTimeZone("UTC"));
  }

  public static Date toDate(Timestamp timestamp) {
    Date date = null;
    if (timestamp != null) {
      date = new Date(timestamp.getTime());
    }
    return date;
  }

  public static Timestamp toTimestamp(Date date) {
    Timestamp ts = null;
    if (date != null) {
      ts = new Timestamp(date.getTime());
    }
    return ts;
  }

}
