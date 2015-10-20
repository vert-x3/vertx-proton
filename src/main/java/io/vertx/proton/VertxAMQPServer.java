/**
 * Copyright 2015 Red Hat, Inc.
 */

package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class VertxAMQPServer {

    private final Vertx vertx;
    private final NetServer server;
    private Handler<VertxAMQPConnnection> handler;

    public VertxAMQPServer(Vertx vertx) {
        this.vertx = vertx;
        this.server = vertx.createNetServer();
    }

    public int actualPort() {
        return server.actualPort();
    }

    public VertxAMQPServer listen(int i) {
        server.listen(i);
        return this;
    }

    public VertxAMQPServer listen() {
        server.listen();
        return this;
    }

    public boolean isMetricsEnabled() {
        return server.isMetricsEnabled();
    }

    public VertxAMQPServer listen(int i, String s, Handler<AsyncResult<VertxAMQPServer>> handler) {
        server.listen(i, s, convertHandler(handler));
        return this;
    }


    public VertxAMQPServer listen(Handler<AsyncResult<VertxAMQPServer>> handler) {
        server.listen(convertHandler(handler));
        return this;
    }

    private Handler<AsyncResult<NetServer>> convertHandler(final Handler<AsyncResult<VertxAMQPServer>> handler) {
        return new Handler<AsyncResult<NetServer>>() {
            @Override
            public void handle(AsyncResult<NetServer> result) {
                if( result.succeeded() ) {
                    handler.handle(Future.succeededFuture(VertxAMQPServer.this));
                } else {
                    handler.handle(Future.failedFuture(result.cause()));
                }
            }
        };
    }

    public VertxAMQPServer listen(int i, String s) {
        server.listen(i, s);
        return this;
    }

    public VertxAMQPServer listen(int i, Handler<AsyncResult<VertxAMQPServer>> handler) {
        server.listen(i, convertHandler(handler));
        return this;
    }

    public void close() {
        server.close();
    }

    public void close(Handler<AsyncResult<Void>> handler) {
        server.close(handler);
    }

    public Handler<VertxAMQPConnnection> connectHandler() {
        return handler;
    }

    public VertxAMQPServer connectHandler(Handler<VertxAMQPConnnection> handler) {
        this.handler = handler;
        server.connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(NetSocket netSocket) {
                VertxAMQPConnnection connection = new VertxAMQPConnnection();
                connection.bind(netSocket);
                handler.handle(connection);
            }
        });
        return this;
    }

}
