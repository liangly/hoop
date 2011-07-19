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
package com.cloudera.lib.service;

import java.util.Map;

public interface Instrumentation {

  public interface Cron {

    public Cron start();

    public Cron stop();
  }

  public interface Variable<T> {

    T getValue();
  }

  public Cron createCron();

  public void incr(String group, String name, long count);

  public void addCron(String group, String name, Cron cron);

  public void addVariable(String group, String name, Variable<?> variable);

  //sampling happens once a second
  public void addSampler(String group, String name, int samplingSize, Variable<Long> variable);

  public Map<String, Map<String, ?>> getSnapshot();

}
