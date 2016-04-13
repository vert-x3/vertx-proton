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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.impl.ProtonClientImpl;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonClient {

  /**
   * Create a ProtonClient instance with the given Vertx instance.
   *
   * @param vertx
   *          the vertx instance to use
   * @return the client instance
   */
  static ProtonClient create(Vertx vertx) {
    return new ProtonClientImpl(vertx);
  }

  /**
   * Connect to the given host and port, without credentials.
   *
   * @param host
   *          the host to connect to
   * @param port
   *          the port to connect to
   * @param connectionHandler
   *          handler that will process the result, giving either the (unopened) ProtonConnection or failure cause.
   */
  void connect(String host, int port, Handler<AsyncResult<ProtonConnection>> connectionHandler);

  /**
   * Connect to the given host and port, with credentials (if required by server peer).
   *
   * @param host
   *          the host to connect to
   * @param port
   *          the port to connect to
   * @param username
   *          the user name to use in any SASL negotiation that requires it
   * @param password
   *          the password to use in any SASL negotiation that requires it
   * @param connectionHandler
   *          handler that will process the result, giving either the (unopened) ProtonConnection or failure cause.
   */
  void connect(String host, int port, String username, String password, Handler<AsyncResult<ProtonConnection>> handler);

  /**
   * Connect to the given host and port, without credentials.
   *
   * @param options
   *          the options to apply
   * @param host
   *          the host to connect to
   * @param port
   *          the port to connect to
   * @param connectionHandler
   *          handler that will process the result, giving either the (unopened) ProtonConnection or failure cause.
   */
  void connect(ProtonClientOptions options, String host, int port,
               Handler<AsyncResult<ProtonConnection>> connectionHandler);

  /**
   * Connect to the given host and port, with credentials (if required by server peer).
   *
   * @param host
   *          the host to connect to
   * @param port
   *          the port to connect to
   * @param username
   *          the user name to use in any SASL negotiation that requires it
   * @param password
   *          the password to use in any SASL negotiation that requires it
   * @param connectionHandler
   *          handler that will process the result, giving either the (unopened) ProtonConnection or failure cause.
   */
  void connect(ProtonClientOptions options, String host, int port, String username, String password,
               Handler<AsyncResult<ProtonConnection>> handler);
}
