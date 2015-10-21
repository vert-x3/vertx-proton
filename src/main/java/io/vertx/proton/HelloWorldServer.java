/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.core.Vertx;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import static io.vertx.proton.ProtonHelper.message;
import static io.vertx.proton.ProtonHelper.tag;

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
        connection.open();
        connection.sessionOpenHandler(session -> session.open());

        connection.receiverOpenHandler(receiver -> {
            System.out.println("Receiving from client to: " + receiver.getRemoteTarget().getAddress());
            receiver
                .setTarget(receiver.getRemoteTarget())
                .handler((r, delivery, msg) -> {

                    Section body = msg.getBody();
                    if (body instanceof AmqpValue) {
                        String content = (String) ((AmqpValue) body).getValue();
                        System.out.println("Received message with content: " + content);
                    }

                    // We could nack if we need to.
                    // delivery.disposition(new Rejected());
                    delivery.settle(); // This acks the message
                    receiver.flow(1);

                })
                .flow(10)
                .open();
        });


        connection.senderOpenHandler(sender -> {
            System.out.println("Sending to client from: " + sender.getRemoteSource().getAddress());
            sender.setSource(sender.getRemoteSource()).open();
            vertx.setPeriodic(1000, timer -> {
                System.out.println("Sending message to client");
                Message m = message("Hello World from Server!");
                sender.send(tag("m1"), m).handler(delivery -> {
                    if (delivery.remotelySettled()) {
                        System.out.println("The message was sent");
                    }
                });
            });
        });


    }

}
