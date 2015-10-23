/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.util.concurrent.ExecutionException;

import static io.vertx.proton.ProtonHelper.condition;
import static io.vertx.proton.ProtonHelper.message;
import static io.vertx.proton.ProtonHelper.tag;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class MockServer {

    private ProtonServer server;

    enum Addresses {
        command,
        drop,
        echo,
        two_messages
    }
    enum Commands {
        disconnect
    }


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
            receiver.handler((delivery, msg) -> {
                String address = msg.getAddress();
                if (address == null) {
                    address = receiver.getRemoteTarget().getAddress();
                }
                processMessage(connection, receiver, delivery, msg, address);
            }).flow(10).open();
        });
        connection.senderOpenHandler(sender->{
            Addresses address = null;
            if( sender.getRemoteSource()!=null ) {
                address = Addresses.valueOf(sender.getRemoteSource().getAddress());
                switch (address) {
                    case two_messages:{
                        sender.open();
                        sender.send(tag("m1"), message("Hello"));
                        sender.send(tag("m2"), message("World"), d->{
                            sender.close();
                        });
                        break;
                    }
                    default:
                        sender.setCondition(condition("Unknown address")).close();
                }
            }
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
