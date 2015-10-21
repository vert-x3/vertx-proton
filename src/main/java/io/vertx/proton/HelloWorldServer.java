/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.core.Vertx;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class HelloWorldServer {

    public static void main(String[] args) {

        // Create the Vert.x instance
        Vertx vertx = Vertx.vertx();

        // Create the Vert.x AMQP client instance
        ProtonServer server = ProtonServer.create(vertx)
                .connectHandler((connection) -> {
                    connection.setContainer("hello-world-server");
                    helloProcessConnection(vertx, connection);
                })
                .listen(5672, (res) -> {
                    if (res.succeeded()) {
                        System.out.println("Listening on: " + res.result().actualPort());
                    } else {
                        res.cause().printStackTrace();
                    }
                });


        // Just stop main() from exiting
        try {
            System.in.read();
        } catch (Exception ignore) {
        }
    }

    private static void helloProcessConnection(Vertx vertx, ProtonConnection connection) {

//        // Lets wait for the client to open first..
//        connection.openHandler(res -> {
//            System.out.println("Client connecting: " + connection.getRemoteContainer());
//            connection.open();
//        });
//
//        connection.sessionOpenHandler(session -> {
//            System.out.println("Session opening: ");
//            session.open();
//        });
//
//        connection.receiverOpenHandler(receiver -> {
//            System.out.println("Receiving from client to: " + receiver.getRemoteTarget().getAddress());
//            receiver.setTarget(receiver.getRemoteTarget());
//            receiver.open().handler((r, delivery, msg) -> {
//
//                Section body = msg.getBody();
//                if (body instanceof AmqpValue) {
//                    String content = (String) ((AmqpValue) body).getValue();
//                    System.out.println("Received message with content: " + content);
//                }
//
//                // We could nack if we need to.
//                // delivery.disposition(new Rejected());
//                delivery.settle(); // This acks the message
//                receiver.flow(1);
//
//            }).flow(10);
//        });
//
//
//        connection.senderOpenHandler(sender -> {
//            System.out.println("Sending to client from: " + sender.getRemoteSource().getAddress());
//            sender.setSource(sender.getRemoteSource()).open();
//            vertx.setPeriodic(1000, ts -> {
//                System.out.println("Sending message to client");
//                Message message = message("Hello World from Server!");
//                sender.send(tag("m:" + ts), message).handler(delivery -> {
//                    if (delivery.remotelySettled()) {
//                        System.out.println("The message was sent");
//                    }
//                });
//                connection.flush();
//            });
//        });


    }

}
