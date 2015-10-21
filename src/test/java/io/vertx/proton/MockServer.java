/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import java.util.concurrent.ExecutionException;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class MockServer {

    public static ProtonServer start(Vertx vertx) throws ExecutionException, InterruptedException {
        ProtonServer server = ProtonServer.create(vertx);
        server.connectHandler((connection) -> processConnection(vertx, connection));
        FutureHandler<ProtonServer, AsyncResult<ProtonServer>> handler = FutureHandler.asyncResult();
        server.listen(0, handler);
        handler.get();
        return server;
    }

    private static void processConnection(Vertx vertx, ProtonConnection connection) {
        connection.openHandler(result -> {
            connection
                .setContainer("pong: " + connection.getRemoteContainer())
                .open();
        });
        connection.sessionOpenHandler(session -> session.open());
        connection.senderOpenHandler(sender -> {

        });
        connection.receiverOpenHandler(receiver-> {
            receiver.handler((r, delivery, msg)->{
                String address = msg.getAddress();
                if( address == null ) {
                    address = receiver.getRemoteTarget().getAddress();
                }
                processMessage(connection, address, msg);
                delivery.settle();
                receiver.flow(1);
            }).flow(10).open();
        });

    }

    private static void processMessage(ProtonConnection connection, String address, Message msg) {
        if("command".equals(address)) {
            String command = (String) ((AmqpValue) msg.getBody()).getValue();
            if( "disconnect".equals(command) ) {
                connection.disconnect();
            }
        }
    }

}
