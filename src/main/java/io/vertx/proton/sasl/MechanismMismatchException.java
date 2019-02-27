/*
* Copyright 2019 the original author or authors.
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
 * Indicates that a SASL handshake has failed because the client does not support any of
 * the mechanisms offered by the server.
 */
public class MechanismMismatchException extends SaslException {

  private static final long serialVersionUID = 1L;

  private final String[] offeredMechanisms;

  /**
   * Creates an exception with a detail message.
   * 
   * @param detail A message providing details about the cause
   *               of the problem.
   */
  public MechanismMismatchException(String detail) {
    this(detail, new String[0]);
  }

  /**
   * Creates an exception with a detail message for offered mechanisms.
   * 
   * @param detail A message providing details about the cause
   *               of the problem.
   * @param mechanisms The names of the SASL mechanisms offered by the server.
   */
  public MechanismMismatchException(String detail, String[] mechanisms) {
    super(detail);
    this.offeredMechanisms = mechanisms;
  }

  /**
   * Gets the names of the SASL mechanisms offered by the server.
   * 
   * @return The mechanisms.
   */
  public String[] getOfferedMechanisms() {
    return offeredMechanisms;
  }
}
