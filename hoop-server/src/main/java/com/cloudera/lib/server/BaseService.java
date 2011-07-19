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
package com.cloudera.lib.server;

import com.cloudera.lib.util.XConfiguration;

import java.util.Map;

public abstract class BaseService implements Service {
  private String prefix;
  private Server server;
  private XConfiguration serviceConfig;

  public BaseService(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public final void init(Server server) throws ServiceException {
    this.server = server;
    String servicePrefix = getPrefixedName("");
    serviceConfig = new XConfiguration();
    for (Map.Entry<String, String> entry : server.getConfig().resolve()) {
      String key = entry.getKey();
      if (key.startsWith(servicePrefix)) {
        serviceConfig.set(key.substring(servicePrefix.length()), entry.getValue());
      }
    }
    init();
  }


  @Override
  public void postInit() throws ServiceException {
  }

  @Override
  public void destroy() {
  }

  @Override
  public Class[] getServiceDependencies() {
    return new Class[0];
  }

  @Override
  public void serverStatusChange(Server.Status oldStatus, Server.Status newStatus) throws ServiceException {
  }

  protected String getPrefix() {
    return prefix;
  }

  protected Server getServer() {
    return server;
  }

  protected String getPrefixedName(String name) {
    return server.getPrefixedName(prefix + "." + name);
  }

  protected XConfiguration getServiceConfig() {
    return serviceConfig;
  }

  protected abstract void init() throws ServiceException;

}
