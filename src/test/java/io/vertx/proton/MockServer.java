/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.message.Message;

import java.util.concurrent.ExecutionException;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class MockServer {

    private ProtonServer server;

    public MockServer(Vertx vertx) throws ExecutionException, InterruptedException {
        server = ProtonServer.create(vertx);
        server.connectHandler((connection) -> processConnection(vertx, connection));
        FutureHandler<ProtonServer, AsyncResult<ProtonServer>> handler = FutureHandler.asyncResult();
        server.listen(0, handler);
        handler.get();
    }

    private void processConnection(Vertx vertx, ProtonConnection connection) {
        connection.sessionOpenHandler(session -> session.open());
        connection.receiverOpenHandler(receiver -> {

            receiver.handler((r, delivery, msg) -> {
                String address = msg.getAddress();
                if (address == null) {
                    address = receiver.getRemoteTarget().getAddress();
                }
                processMessage(connection, receiver, delivery, msg, address);
                delivery.settle();
                receiver.flow(1);
            }).flow(10).open();
        });
        connection.openHandler(result -> {
            connection
                .setContainer("pong: " + connection.getRemoteContainer())
                .open();
        });

    }


    public void close() {
        server.close();
    }

    public int actualPort() {
        return server.actualPort();
    }

    enum Addresses {
        command,
        drop,
        echo,
    }
    enum Commands {
        disconnect
    }

    private void processMessage(ProtonConnection connection, ProtonReceiver receiver, ProtonDelivery delivery, Message msg, String to) {
        switch (Addresses.valueOf(to)) {

            case drop: {
                break;
            }

            case echo: {
                ProtonSender sender = receiver.getSession().sender("echo");
                if( !sender.isOpen() ) {
                    sender.open();
                }
                sender.send(delivery.getTag(), msg);
                break;
            }

            case command: {
                String command = (String) ((AmqpValue) msg.getBody()).getValue();
                switch (Commands.valueOf(command)) {
                    case disconnect:
                        connection.disconnect();
                        break;
                }
                break;
            }
        }
    }

}
