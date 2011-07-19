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

import com.cloudera.lib.server.ServerException;
import com.cloudera.lib.service.Hadoop;
import com.cloudera.lib.servlet.ServerWebApp;
import com.cloudera.lib.util.XConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HoopServer extends ServerWebApp {
  private static final Logger LOG = LoggerFactory.getLogger(HoopServer.class);

  public static final String NAME = "hoop";
  public static final String CONF_ADMIN_GROUP = "admin.group";
  public static final String CONF_BASE_URL = "base.url";

  private static HoopServer HOOP_SERVER;

  private String adminGroup;
  private String baseUrl;

  public HoopServer() throws IOException {
    super("hoop");
  }

  //for testing
  protected HoopServer(String homeDir, String configDir, String logDir, String tempDir,
                       XConfiguration config) {
    super(NAME, homeDir, configDir, logDir, tempDir, config);
  }

  //for testing
  public HoopServer(String homeDir, XConfiguration config) {
    super(NAME, homeDir, config);
  }

  @Override
  public void init() throws ServerException {
    super.init();
    if (HOOP_SERVER != null) {
      throw new RuntimeException("Hoop server already initialized");
    }
    HOOP_SERVER = this;
    adminGroup = getConfig().get(getPrefixedName(CONF_ADMIN_GROUP), "admin");
    baseUrl = getConfig().get(getPrefixedName(CONF_BASE_URL), "http://localhost:14000");
    LOG.info("Connects to Namenode [{}]",
             HoopServer.get().get(Hadoop.class).getDefaultConfiguration().get("fs.default.name"));
  }

  @Override
  public void destroy() {
    HOOP_SERVER = null;
    super.destroy();
  }

  public static HoopServer get() {
    return HOOP_SERVER;
  }

  public String getAdminGroup() {
    return adminGroup;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

}
