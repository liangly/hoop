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

import com.cloudera.lib.server.Server;
import com.cloudera.lib.server.ServerException;
import com.cloudera.lib.util.XConfiguration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.text.MessageFormat;

public abstract class ServerWebApp extends Server implements ServletContextListener {

  private static final String HOME_DIR = ".home.dir";
  private static final String CONFIG_DIR = ".config.dir";
  private static final String LOG_DIR = ".log.dir";
  private static final String TEMP_DIR = ".temp.dir";

  private static ThreadLocal<String> HOME_DIR_TL = new ThreadLocal<String>();

  //for testing, used only by constructor
  public static void setHomeDirForCurrentThread(String homeDir) {
    HOME_DIR_TL.set(homeDir);
  }

  //for testing
  protected ServerWebApp(String name, String homeDir, String configDir, String logDir, String tempDir,
                         XConfiguration config) {
    super(name, homeDir, configDir, logDir, tempDir, config);
  }

  //for testing
  protected ServerWebApp(String name, String homeDir, XConfiguration config) {
    super(name, homeDir, config);
  }

  public ServerWebApp(String name) {
    super(name, getHomeDir(name),
          getDir(name, CONFIG_DIR, getHomeDir(name) + "/conf"),
          getDir(name, LOG_DIR, getHomeDir(name) + "/log"),
          getDir(name, TEMP_DIR, getHomeDir(name) + "/temp"), null);
  }

  static String getHomeDir(String name) {
    String homeDir = HOME_DIR_TL.get();
    if (homeDir == null) {
      String sysProp = name + HOME_DIR;
      homeDir = System.getProperty(sysProp);
      if (homeDir == null) {
        throw new IllegalArgumentException(MessageFormat.format("System property [{0}] not defined", sysProp));
      }
    }
    return homeDir;
  }

  static String getDir(String name, String dirType, String defaultDir) {
    String sysProp = name + dirType;
    return System.getProperty(sysProp, defaultDir);
  }


  public void contextInitialized(ServletContextEvent event) {
    try {
      init();
    }
    catch (ServerException ex) {
      event.getServletContext().log("ERROR: " + ex.getMessage());
      throw new RuntimeException(ex);
    }
  }

  public void contextDestroyed(ServletContextEvent event) {
    destroy();
  }

}
