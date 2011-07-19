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
import com.cloudera.lib.service.ACL;
import com.cloudera.lib.service.Groups;
import com.cloudera.lib.util.Check;

import java.io.IOException;
import java.security.AccessControlException;
import java.text.MessageFormat;
import java.util.List;

public class ACLService extends BaseService implements ACL {
  private static final String PREFIX = "acl";

  public ACLService() {
    super(PREFIX);
  }

  @Override
  protected void init() throws ServiceException {
  }

  @Override
  public Class getInterface() {
    return ACL.class;
  }

  @Override
  public Class[] getServiceDependencies() {
    return new Class[]{Groups.class};
  }

  @Override
  public void validate(String user, String owner, String acl) throws AccessControlException {
    Check.notEmpty(user, "user");
    Check.notEmpty(owner, "owner");
    if (!user.equals(owner)) {
      if (acl != null) {
        String values[] = acl.split(",");
        for (String value : values) {
          value = value.trim();
          if (value.equals(user)) {
            return;
          }
        }
        try {
          List<String> groups = getServer().get(Groups.class).getGroups(user);
          for (String value : values) {
            if (groups.contains(value)) {
              return;
            }
          }
          throw new AccessControlException(MessageFormat.format("User [{0}] does not satisfy ACL [{1}]",
                                                                user, acl));
        }
        catch (IOException ex) {
          throw new AccessControlException(ex.getMessage());
        }
      }
      else {
        throw new AccessControlException(MessageFormat.format("No ACL, user [{0}] not owner [{1}]",
                                                              user, owner));
      }
    }
  }

}
