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

import com.cloudera.lib.lang.StringUtils;

import java.util.Arrays;

public abstract class EnumParam<E extends Enum<E>> extends Param<E> {
  Class<E> klass;

  public EnumParam(String label, String str, Class<E> e) {
    klass = e;
    value = parseParam(label, str);
  }

  protected E parse(String str) throws Exception {
    return Enum.valueOf(klass, str.toUpperCase());
  }

  @Override
  protected String getDomain() {
    return StringUtils.toString(Arrays.asList(klass.getEnumConstants()), ",");
  }

}
