/*
* Copyright 2016 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.vertx.proton;

import io.vertx.core.net.NetClientOptions;

/**
 * Options for configuring {@link io.vertx.proton.ProtonClient} connect operations.
 */
public class ProtonClientOptions extends NetClientOptions {

  private String[] allowedSaslMechanisms = null;

  /**
   * Get the mechanisms the client is currently restricted to use.
   *
   * @return the mechanisms, or null if there is no restriction in place
   */
  public String[] getAllowedSaslMechanisms() {
    return allowedSaslMechanisms;
  }

  /**
   * Set a restricted mechanism(s) that the client may use during the SASL negotiation. If null or empty argument is
   * given, no restriction is applied and any supported mechanism can be used.
   *
   * @param mechanisms
   *          the restricted mechanism(s) or null to clear the restriction.
   * @return a reference to this, so the API can be used fluently
   */
  public ProtonClientOptions setAllowedSaslMechanisms(final String... saslMechanisms) {
    if (saslMechanisms == null || saslMechanisms.length == 0) {
      this.allowedSaslMechanisms = null;
    } else {
      this.allowedSaslMechanisms = saslMechanisms;
    }

    return this;
  }

  // TODO: Use a delegate? Override methods to change return type?
}
