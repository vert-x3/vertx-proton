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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.qpid.proton.amqp.Symbol;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonServerImpl implements ProtonServer {

  private final Vertx vertx;
  private final NetServer server;
  private Handler<ProtonConnection> handler;
  private ProtonSaslAuthenticator authenticator;
  private boolean advertiseAnonymousRelayCapability = true;

  public ProtonServerImpl(Vertx vertx) {
    this.vertx = vertx;
    this.server = this.vertx.createNetServer();
  }

  public ProtonServerImpl(Vertx vertx, ProtonServerOptions options) {
    this.vertx = vertx;
    this.server = this.vertx.createNetServer(options);
  }

  public int actualPort() {
    return server.actualPort();
  }

  public ProtonServerImpl listen(int i) {
    server.listen(i);
    return this;
  }

  public ProtonServerImpl listen() {
    server.listen();
    return this;
  }

  public boolean isMetricsEnabled() {
    return server.isMetricsEnabled();
  }

  public ProtonServerImpl listen(int port, String host, Handler<AsyncResult<ProtonServer>> handler) {
    server.listen(port, host, convertHandler(handler));
    return this;
  }

  public ProtonServerImpl listen(Handler<AsyncResult<ProtonServer>> handler) {
    server.listen(convertHandler(handler));
    return this;
  }

  private Handler<AsyncResult<NetServer>> convertHandler(final Handler<AsyncResult<ProtonServer>> handler) {
    return result -> {
      if (result.succeeded()) {
        handler.handle(Future.succeededFuture(ProtonServerImpl.this));
      } else {
        handler.handle(Future.failedFuture(result.cause()));
      }
    };
  }

  public ProtonServerImpl listen(int i, String s) {
    server.listen(i, s);
    return this;
  }

  public ProtonServerImpl listen(int i, Handler<AsyncResult<ProtonServer>> handler) {
    server.listen(i, convertHandler(handler));
    return this;
  }

  public void close() {
    server.close();
  }

  public void close(Handler<AsyncResult<Void>> handler) {
    server.close(handler);
  }

  public Handler<ProtonConnection> connectHandler() {
    return handler;
  }

  public ProtonServer saslAuthenticator(ProtonSaslAuthenticator authenticator) {
    this.authenticator = authenticator;
    return this;
  }

  public ProtonServerImpl connectHandler(Handler<ProtonConnection> handler) {
    this.handler = handler;
    server.connectHandler(new Handler<NetSocket>() {
      @Override
      public void handle(NetSocket netSocket) {
        String hostname = null;
        try {
          hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
        }

        ProtonConnectionImpl connection = new ProtonConnectionImpl(vertx, hostname);
        if (advertiseAnonymousRelayCapability) {
          connection.setOfferedCapabilities(new Symbol[] { ProtonConnectionImpl.ANONYMOUS_RELAY });
        }

        ProtonSaslAuthenticator connectionAuthenticator = authenticator;
        if(connectionAuthenticator == null) {
          connectionAuthenticator = new ProtonSaslServerAuthenticatorImpl(handler, connection);
        }
        connection.bindServer(netSocket, connectionAuthenticator);
      }
    });
    return this;
  }

  public void setAdvertiseAnonymousRelayCapability(boolean advertiseAnonymousRelayCapability) {
    this.advertiseAnonymousRelayCapability = advertiseAnonymousRelayCapability;
  }

}
