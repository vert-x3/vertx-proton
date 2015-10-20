/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class VertxAMQPClient extends VertxAMQPConnnection {

    public void connect(Vertx vertx, String host, int port, Handler<AsyncResult<Void>> handler) {
        final NetClient netClient = vertx.createNetClient();
        netClient.connect(port, host, res -> {
            if (res.succeeded()) {
                transport = new VertxAMQPTransport(connection, netClient, res.result());
                handler.handle(Future.succeededFuture(null));
            } else {
                handler.handle(Future.failedFuture(res.cause()));
            }
            flush();
        });
    }
}
