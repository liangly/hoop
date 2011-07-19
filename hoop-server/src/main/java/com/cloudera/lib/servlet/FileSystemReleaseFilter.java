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

import com.cloudera.lib.service.Hadoop;
import org.apache.hadoop.fs.FileSystem;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public abstract class FileSystemReleaseFilter implements Filter {
  private static final ThreadLocal<FileSystem> FILE_SYSTEM_TL = new ThreadLocal<FileSystem>();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
    throws IOException, ServletException {
    try {
      filterChain.doFilter(servletRequest, servletResponse);
    }
    finally {
      FileSystem fs = FILE_SYSTEM_TL.get();
      if (fs != null) {
        FILE_SYSTEM_TL.remove();
        getHadoop().releaseFileSystem(fs);
      }
    }
  }

  @Override
  public void destroy() {
  }

  public static void setFileSystem(FileSystem fs) {
    FILE_SYSTEM_TL.set(fs);
  }

  protected abstract Hadoop getHadoop();

}
