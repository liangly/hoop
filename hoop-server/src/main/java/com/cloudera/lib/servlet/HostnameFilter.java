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
package com.cloudera.lib.servlet;


import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.net.InetAddress;

public class HostnameFilter implements Filter {
  static final ThreadLocal<String> HOSTNAME_TL = new ThreadLocal<String>();

  @Override
  public void init(FilterConfig config) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
    try {
      String hostname = InetAddress.getByName(request.getRemoteAddr()).getCanonicalHostName();
      HOSTNAME_TL.set(hostname);
      chain.doFilter(request, response);
    }
    finally {
      HOSTNAME_TL.remove();
    }
  }

  public static String get() {
    return HOSTNAME_TL.get();
  }

  @Override
  public void destroy() {
  }
}
