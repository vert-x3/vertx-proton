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
import io.vertx.proton.impl.ProtonServerImpl;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonServer {

  /**
   * Create a ProtonServer instance with the given Vertx instance.
   *
   * @param vertx
   *          the vertx instance to use
   * @return the server instance
   */
  static ProtonServer create(Vertx vertx) {
    return new ProtonServerImpl(vertx);
  }

  /**
   * Create a ProtonServer instance with the given Vertx instance and options.
   *
   * @param vertx
   *          the vertx instance to use
   * @param options
   *          the options to use
   * @return the server instance
   */
  static ProtonServer create(Vertx vertx, ProtonServerOptions options) {
    return new ProtonServerImpl(vertx, options);
  }

  /**
   * The handler called when a new client connection is accepted.
   *
   * @param handler
   *          the handler
   * @return the server
   */
  ProtonServer connectHandler(Handler<ProtonConnection> handler);

  /**
   * Returns the current connectHandler.
   *
   * @return the handler
   */
  Handler<ProtonConnection> connectHandler();

  /**
   * Gets the actual port being listened on.
   *
   * @return the port
   */
  int actualPort();

  /**
   * Start listening on the given port and host interface, with the result handler called when the operation completes.
   *
   * @param port
   *          the port to listen on (may be 0 to auto-select port)
   * @param host
   *          the host interface to listen on (may be "0.0.0.0" to indicate all interfaces).
   * @param handler
   *          the result handler
   * @return the server
   */
  ProtonServer listen(int port, String host, Handler<AsyncResult<ProtonServer>> handler);

  /**
   * Start listening on the given port and host interface "0.0.0.0", with the result handler called when the operation
   * completes.
   *
   * @param port
   *          the port to listen on (may be 0 to auto-select port)
   * @param handler
   *          the result handler
   * @return the server
   */
  ProtonServer listen(int port, Handler<AsyncResult<ProtonServer>> handler);

  /**
   * Start listening on the host and port configured in the options used when creating the server, with the result
   * handler called when the operation completes.
   *
   * @param handler
   *          the result handler
   * @return the server
   */
  ProtonServer listen(Handler<AsyncResult<ProtonServer>> handler);

  /**
   * Start listening on the given port, and host interface "0.0.0.0".
   *
   * @param port
   *          the port to listen on (may be 0 to auto-select port)
   * @return the server
   */
  ProtonServer listen(int port);

  /**
   * Start listening on the given port, and host interface.
   *
   * @param port
   *          the port to listen on (may be 0 to auto-select port)
   * @param host
   *          the host interface to listen on (may be "0.0.0.0" to indicate all interfaces).
   * @return the server
   */
  ProtonServer listen(int port, String host);

  /**
   * Start listening on the host and port configured in the options used when creating the server.
   *
   * @return the server
   */
  ProtonServer listen();

  /**
   * Closes the server and any currently open connections. May not complete until after method has returned.
   */
  void close();

  /**
   * Closes the server and any currently open connections, notifying the given handler when complete.
   *
   * @param handler
   *          the completion handler
   */
  void close(Handler<AsyncResult<Void>> handler);
}
