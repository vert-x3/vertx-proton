/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.core.Vertx;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import static io.vertx.proton.ProtonHelper.message;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class HelloWorld {

    public static void main(String[] args) {

        // Create the Vert.x instance
        Vertx vertx = Vertx.vertx();

        // Create the Vert.x AMQP client instance
        ProtonClient client = ProtonClient.create(vertx);

        client.connect("localhost", 5672, res -> {
            if (res.succeeded()) {
                System.out.println("We're connected");
                helloWorldSendAndConsumeExample(res.result());
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

    private static void helloWorldSendAndConsumeExample(ProtonConnection connection) {

        ProtonSession session = connection.session();

        // Receive messages from a queue
        session.receiver("receiver-link-1", "queue://foo")
                .handler((receiver, delivery, msg) -> {

                    Section body = msg.getBody();
                    if (body instanceof AmqpValue) {
                        String content = (String) ((AmqpValue) body).getValue();
                        System.out.println("Received message with content: " + content);
                    }

                    // We could nack if we need to.
                    // delivery.disposition(new Rejected());
                    delivery.settle(); // This acks the message
                    receiver.flow(1);

                }).flow(10); // Prefetch up to 10 messages


        // Send messages to a queue..
        ProtonSender sender = session.sender("sender-link-1", "queue://foo");

        Message message = message("Hello World from client");
        sender.send(message).handler(delivery -> {
            if (delivery.remotelySettled()) {
                System.out.println("The message was sent");
            }
        });

    }

}
