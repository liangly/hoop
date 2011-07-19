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
package com.cloudera.hoop;

import com.cloudera.alfredo.server.AuthenticationFilter;
import org.apache.hadoop.conf.Configuration;

import javax.servlet.FilterConfig;
import java.util.Map;
import java.util.Properties;

public class AuthFilter extends AuthenticationFilter {
  private static final String CONF_PREFIX = "hoop.authentication.";

  @Override
  protected Properties getConfiguration(String configPrefix, FilterConfig filterConfig) {
    Properties props = new Properties();
    Configuration conf = HoopServer.get().getConfig();

    props.setProperty(AuthenticationFilter.COOKIE_PATH, "/");
    for (Map.Entry<String, String> entry : conf) {
      String name = entry.getKey();
      if (name.startsWith(CONF_PREFIX)) {
        String value = conf.get(name);
        name = name.substring(CONF_PREFIX.length());
        props.setProperty(name, value);
      }
    }
    return props;
  }


}
