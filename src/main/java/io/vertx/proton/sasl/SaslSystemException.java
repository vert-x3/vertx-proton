/*
* Copyright 2018 the original author or authors.
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
package io.vertx.proton.sasl;

import javax.security.sasl.SaslException;

/**
 * Indicates that a SASL handshake has failed with a {@code sys}, {@code sys-perm}, or {@code sys-temp}
 * outcome code as defined by
 * <a href="http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-security-v1.0-os.html#type-sasl-code">
 * AMQP Version 1.0, Section 5.3.3.6</a>.
 */
public class SaslSystemException extends SaslException {

  private static final long serialVersionUID = 1L;
  private final boolean permanent;

  /**
   * Creates an exception indicating a system error.
   * 
   * @param permanent {@code true} if the error is permanent and requires
   *                  (manual) intervention.
   * 
   */
  public SaslSystemException(boolean permanent) {
    this(permanent, null);
  }

  /**
   * Creates an exception indicating a system error with a detail message.
   * 
   * @param permanent {@code true} if the error is permanent and requires
   *                  (manual) intervention.
   * @param detail A message providing details about the cause
   *               of the problem.
   */
  public SaslSystemException(boolean permanent, String detail) {
    super(detail);
    this.permanent = permanent;
  }

  /**
   * Checks if the condition that caused this exception is of a permanent nature.
   * 
   * @return {@code true} if the error condition is permanent.
   */
  public final boolean isPermanent() {
    return permanent;
  }
}
