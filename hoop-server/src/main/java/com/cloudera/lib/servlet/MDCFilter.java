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

import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;

public class MDCFilter implements Filter {

  @Override
  public void init(FilterConfig config) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
    try {
      MDC.clear();
      String hostname = HostnameFilter.get();
      if (hostname != null) {
        MDC.put("hostname", HostnameFilter.get());
      }
      Principal principal = ((HttpServletRequest) request).getUserPrincipal();
      String user = (principal != null) ? principal.getName() : null;
      if (user != null) {
        MDC.put("user", user);
      }
      MDC.put("method", ((HttpServletRequest) request).getMethod());
      MDC.put("path", ((HttpServletRequest) request).getPathInfo());
      chain.doFilter(request, response);
    }
    finally {
      MDC.clear();
    }
  }

  @Override
  public void destroy() {
  }
}

