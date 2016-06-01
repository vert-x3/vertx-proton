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
package io.vertx.proton.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonClientImpl implements ProtonClient {

  private final Vertx vertx;

  public ProtonClientImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  public void connect(String host, int port, Handler<AsyncResult<ProtonConnection>> handler) {
    connect(host, port, null, null, handler);
  }

  public void connect(String host, int port, String username, String password,
                      Handler<AsyncResult<ProtonConnection>> handler) {
    connect(new ProtonClientOptions(), host, port, username, password, handler);
  }

  public void connect(ProtonClientOptions options, String host, int port,
                      Handler<AsyncResult<ProtonConnection>> handler) {
    connect(options, host, port, null, null, handler);
  }

  public void connect(ProtonClientOptions options, String host, int port, String username, String password,
                      Handler<AsyncResult<ProtonConnection>> handler) {
    final NetClient netClient = vertx.createNetClient(options);
    connectNetClient(netClient, host, port, username, password, handler, options);
  }

  private void connectNetClient(NetClient netClient, String host, int port, String username, String password,
                                Handler<AsyncResult<ProtonConnection>> connectHandler, ProtonClientOptions options) {
    netClient.connect(port, host, res -> {
      if (res.succeeded()) {
        ProtonConnectionImpl amqpConnnection = new ProtonConnectionImpl(vertx, host);

        ProtonSaslClientAuthenticatorImpl authenticator = new ProtonSaslClientAuthenticatorImpl(username, password,
                options.getEnabledSaslMechanisms(), connectHandler);
        amqpConnnection.bindClient(netClient, res.result(), authenticator);

        // Need to flush here to get the SASL process going, or it will wait until calls on the connection are processed
        // later (e.g open()).
        amqpConnnection.flush();
      } else {
        connectHandler.handle(Future.failedFuture(res.cause()));
      }
    });
  }
}
