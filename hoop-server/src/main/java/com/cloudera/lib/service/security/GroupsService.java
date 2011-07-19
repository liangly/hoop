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
package com.cloudera.lib.service.security;

import com.cloudera.lib.server.BaseService;
import com.cloudera.lib.server.ServiceException;
import com.cloudera.lib.service.Groups;
import com.cloudera.lib.util.XConfiguration;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.util.List;

public class GroupsService extends BaseService implements Groups {
  private static final String PREFIX = "groups";

  private org.apache.hadoop.security.Groups hGroups;

  public GroupsService() {
    super(PREFIX);
  }

  @Override
  protected void init() throws ServiceException {
    Configuration hConf = new XConfiguration();
    XConfiguration.copy(getServiceConfig(), hConf);
    hGroups = new org.apache.hadoop.security.Groups(hConf);
  }

  @Override
  public Class getInterface() {
    return Groups.class;
  }

  @Override
  public List<String> getGroups(String user) throws IOException {
    return hGroups.getGroups(user);
  }

}
