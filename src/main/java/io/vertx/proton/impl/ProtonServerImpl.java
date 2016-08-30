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

import io.vertx.core.*;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import io.vertx.proton.sasl.ProtonSaslAuthenticatorFactory;

import org.apache.qpid.proton.amqp.Symbol;

import io.vertx.core.net.NetServer;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;
import org.apache.qpid.proton.engine.Transport;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonServerImpl implements ProtonServer {

  private final Vertx vertx;
  private final NetServer server;
  private Handler<ProtonConnection> handler;
  // default authenticator, anonymous
  private ProtonSaslAuthenticatorFactory authenticatorFactory = new DefaultAuthenticatorFactory();
  private boolean advertiseAnonymousRelayCapability = true;

  public ProtonServerImpl(Vertx vertx) {
    this.vertx = vertx;
    this.server = this.vertx.createNetServer();
  }

  public ProtonServerImpl(Vertx vertx, ProtonServerOptions options) {
    this.vertx = vertx;
    this.server = this.vertx.createNetServer(options);
  }

  @Override
  public int actualPort() {
    return server.actualPort();
  }

  @Override
  public ProtonServerImpl listen(int i) {
    server.listen(i);
    return this;
  }

  @Override
  public ProtonServerImpl listen() {
    server.listen();
    return this;
  }

  public boolean isMetricsEnabled() {
    return server.isMetricsEnabled();
  }

  @Override
  public ProtonServerImpl listen(int port, String host, Handler<AsyncResult<ProtonServer>> handler) {
    server.listen(port, host, convertHandler(handler));
    return this;
  }

  @Override
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

  @Override
  public ProtonServerImpl listen(int i, String s) {
    server.listen(i, s);
    return this;
  }

  @Override
  public ProtonServerImpl listen(int i, Handler<AsyncResult<ProtonServer>> handler) {
    server.listen(i, convertHandler(handler));
    return this;
  }

  @Override
  public void close() {
    server.close();
  }

  @Override
  public void close(Handler<AsyncResult<Void>> handler) {
    server.close(handler);
  }

  @Override
  public Handler<ProtonConnection> connectHandler() {
    return handler;
  }

  @Override
  public ProtonServer saslAuthenticatorFactory(ProtonSaslAuthenticatorFactory authenticatorFactory) {
    if (authenticatorFactory == null) {
      // restore the default
      this.authenticatorFactory = new DefaultAuthenticatorFactory();
    } else {
      this.authenticatorFactory = authenticatorFactory;
    }
    return this;
  }

  @Override
  public ProtonServerImpl connectHandler(Handler<ProtonConnection> handler) {
    this.handler = handler;
    server.connectHandler(netSocket -> {
      String hostname = null;
      try {
        hostname = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        // ignore
      }

      final ProtonConnectionImpl connection = new ProtonConnectionImpl(vertx, hostname);
      if (advertiseAnonymousRelayCapability) {
        connection.setOfferedCapabilities(new Symbol[] { ProtonConnectionImpl.ANONYMOUS_RELAY });
      }

      final ProtonSaslAuthenticator authenticator = authenticatorFactory.create();

      connection.bindServer(netSocket, new ProtonSaslAuthenticator() {

        @Override
        public void init(NetSocket socket, ProtonConnection protonConnection, Transport transport) {
          authenticator.init(socket, protonConnection, transport);
        }

        @Override
        public void process(Handler<Boolean> completionHandler) {
          final Context context = Vertx.currentContext();

          authenticator.process(complete -> {
            final Context callbackContext = vertx.getOrCreateContext();
            if(context != callbackContext) {
              throw new IllegalStateException("Callback was not made on the original context");
            }

            if (complete) {
              // The authenticator completed, now check success, do required post processing
              if (succeeded()) {
                handler.handle(connection);
                connection.flush();
              } else {
                // auth failed, flush any pending data and disconnect client
                connection.flush();
                connection.disconnect();
              }
            }

            completionHandler.handle(complete);
          });
        }

        @Override
        public boolean succeeded() {
          return authenticator.succeeded();
        }
      });
    });
    return this;
  }

  public void setAdvertiseAnonymousRelayCapability(boolean advertiseAnonymousRelayCapability) {
    this.advertiseAnonymousRelayCapability = advertiseAnonymousRelayCapability;
  }

  private static class DefaultAuthenticatorFactory implements ProtonSaslAuthenticatorFactory {
    @Override
    public ProtonSaslAuthenticator create() {
      return new ProtonSaslServerAuthenticatorImpl();
    }
  }
}
