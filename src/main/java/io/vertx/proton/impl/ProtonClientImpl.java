/*
* Copyright 2016, 2017 the original author or authors.
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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonTransportOptions;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonClientImpl implements ProtonClient {

  private static final Logger LOG = LoggerFactory.getLogger(ProtonClientImpl.class);
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
    connectNetClient(netClient, host, port, username, password, new ConnectCompletionHandler(handler, netClient), options);
  }

  private void connectNetClient(NetClient netClient, String host, int port, String username, String password,
                                ConnectCompletionHandler connectHandler, ProtonClientOptions options) {

    String serverName = options.getSniServerName() != null ? options.getSniServerName() :
      (options.getVirtualHost() != null ? options.getVirtualHost() : null);

    netClient.connect(port, host, serverName, res -> {
      if (res.succeeded()) {
        String virtualHost = options.getVirtualHost() != null ? options.getVirtualHost() : host;
        ProtonConnectionImpl conn = new ProtonConnectionImpl(vertx, virtualHost);
        conn.disconnectHandler(h -> {
          LOG.trace("Connection disconnected");
          if(!connectHandler.isComplete()) {
            connectHandler.handle(Future.failedFuture(new VertxException("Disconnected")));
          }
        });

        ProtonSaslClientAuthenticatorImpl authenticator = new ProtonSaslClientAuthenticatorImpl(username, password,
                options.getEnabledSaslMechanisms(), connectHandler);

        ProtonTransportOptions transportOptions = new ProtonTransportOptions();
        transportOptions.setHeartbeat(options.getHeartbeat());
        transportOptions.setMaxFrameSize(options.getMaxFrameSize());

        conn.bindClient(netClient, res.result(), authenticator, transportOptions);

        // Need to flush here to get the SASL process going, or it will wait until calls on the connection are processed
        // later (e.g open()).
        conn.flush();
      } else {
        connectHandler.handle(Future.failedFuture(res.cause()));
      }
    });
  }

  static class ConnectCompletionHandler implements Handler<AsyncResult<ProtonConnection>> {
    private AtomicBoolean completed = new AtomicBoolean();
    private Handler<AsyncResult<ProtonConnection>> applicationConnectHandler;
    private NetClient netClient;

    ConnectCompletionHandler(Handler<AsyncResult<ProtonConnection>> applicationConnectHandler, NetClient netClient) {
      this.applicationConnectHandler = Objects.requireNonNull(applicationConnectHandler);
      this.netClient = Objects.requireNonNull(netClient);
    }

    public boolean isComplete() {
      return completed.get();
    }

    @Override
    public void handle(AsyncResult<ProtonConnection> event) {
      if (completed.compareAndSet(false, true)) {
        if (event.failed()) {
          netClient.close();
        }
        applicationConnectHandler.handle(event);
      }
    }
  }
}
