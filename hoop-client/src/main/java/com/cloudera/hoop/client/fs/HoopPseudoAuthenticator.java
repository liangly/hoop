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
package com.cloudera.hoop.client.fs;

import com.cloudera.alfredo.client.PseudoAuthenticator;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;

/**
 * A <code>PseudoAuthenticator</code> subclass that uses Hadoop's
 * <code>UserGroupInformation</code> to obtain the client user name (the UGI's login user).
 */
public class HoopPseudoAuthenticator extends PseudoAuthenticator {

  /**
   * Return the client user name.
   *
   * @return the client user name.
   */
  @Override
  protected String getUserName() {
    try {
      return UserGroupInformation.getLoginUser().getUserName();
    }
    catch (IOException ex) {
      throw new SecurityException("Could not obtain current user, " + ex.getMessage(), ex);
    }
  }
}
