/**
 * Copyright 2015 Red Hat, Inc.
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
        final NetClient netClient = vertx.createNetClient();
        connectNetClient(netClient, host, port, handler);
    }

    public void connect(ProtonClientOptions options, String host, int port, Handler<AsyncResult<ProtonConnection>> handler) {
        final NetClient netClient = vertx.createNetClient(options);
        connectNetClient(netClient, host, port, handler);
    }

    private void connectNetClient(NetClient netClient, String host, int port, Handler<AsyncResult<ProtonConnection>> handler) {
        netClient.connect(port, host, res -> {
            if (res.succeeded()) {
                ProtonConnectionImpl amqpConnnection = new ProtonConnectionImpl(vertx, host);
                amqpConnnection.bind(netClient, res.result());
                handler.handle(Future.succeededFuture(amqpConnnection));
            } else {
                handler.handle(Future.failedFuture(res.cause()));
            }
        });
    }
}
