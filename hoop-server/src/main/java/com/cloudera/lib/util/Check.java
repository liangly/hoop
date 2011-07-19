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

import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Pattern;

public class Check {

  public static <T> T notNull(T obj, String name) {
    if (obj == null) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
    return obj;
  }

  public static <T> List<T> notNullElements(List<T> list, String name) {
    notNull(list, name);
    for (int i = 0; i < list.size(); i++) {
      notNull(list.get(i), MessageFormat.format("list [{0}] element [{1}]", name, i));
    }
    return list;
  }

  public static String notEmpty(String str, String name) {
    if (str == null) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
    if (str.length() == 0) {
      throw new IllegalArgumentException(name + " cannot be empty");
    }
    return str;
  }

  public static List<String> notEmptyElements(List<String> list, String name) {
    notNull(list, name);
    for (int i = 0; i < list.size(); i++) {
      notEmpty(list.get(i), MessageFormat.format("list [{0}] element [{1}]", name, i));
    }
    return list;
  }

  private static final String IDENTIFIER_PATTERN_STR = "[a-zA-z_][a-zA-Z0-9_\\-]*";

  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^" + IDENTIFIER_PATTERN_STR + "$");

  public static String validIdentifier(String value, String name, int maxLen) {
    Check.notEmpty(value, name);
    if (value.length() > maxLen) {
      throw new IllegalArgumentException(
        MessageFormat.format("[{0}] = [{1}] exceeds max len [{2}]", name, value, maxLen));
    }
    if (!IDENTIFIER_PATTERN.matcher(value).find()) {
      throw new IllegalArgumentException(
        MessageFormat.format("[{0}] = [{1}] must be '{2}'", name, value, IDENTIFIER_PATTERN_STR));
    }
    return value;
  }

  public static int gt0(int value, String name) {
    return (int) gt0((long) value, name);
  }

  public static long gt0(long value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(
        MessageFormat.format("parameter [{0}] = [{1}] must be greater than zero", name, value));
    }
    return value;
  }

  public static int ge0(int value, String name) {
    return (int) ge0((long) value, name);
  }

  public static long ge0(long value, String name) {
    if (value < 0) {
      throw new IllegalArgumentException(MessageFormat.format(
        "parameter [{0}] = [{1}] must be greater than or equals zero", name, value));
    }
    return value;
  }

}
