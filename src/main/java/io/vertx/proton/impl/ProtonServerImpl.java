/**
 * Copyright 2015 Red Hat, Inc.
 */

package io.vertx.proton.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonServerImpl implements ProtonServer {

    private final Vertx vertx;
    private final NetServer server;
    private Handler<ProtonConnection> handler;

    public ProtonServerImpl(Vertx vertx) {
        this.vertx = vertx;
        this.server = vertx.createNetServer();
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
            if( result.succeeded() ) {
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

    public ProtonServerImpl connectHandler(Handler<ProtonConnection> handler) {
        this.handler = handler;
        server.connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(NetSocket netSocket) {
                ProtonConnectionImpl connection = new ProtonConnectionImpl();
                connection.bind(netSocket);
                handler.handle(connection);
            }
        });
        return this;
    }


}
