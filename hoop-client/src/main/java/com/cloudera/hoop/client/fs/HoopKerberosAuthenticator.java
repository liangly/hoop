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

import com.cloudera.alfredo.client.Authenticator;
import com.cloudera.alfredo.client.KerberosAuthenticator;

/**
 * A <code>KerberosAuthenticator</code> subclass that fallback to
 * {@link HoopPseudoAuthenticator}.
 */
public class HoopKerberosAuthenticator extends KerberosAuthenticator {

  /**
   * Returns the fallback authenticator if the server does not use
   * Kerberos SPNEGO HTTP authentication.
   *
   * @return a {@link HoopPseudoAuthenticator} instance.
   */
  @Override
  protected Authenticator getFallBackAuthenticator() {
    return new HoopPseudoAuthenticator();
  }
}
