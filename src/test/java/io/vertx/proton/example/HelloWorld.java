/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.example;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import static io.vertx.proton.ProtonHelper.message;
import static io.vertx.proton.ProtonHelper.tag;

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

        connection.open();

        // Receive messages from a queue
        connection.receiver().setSource("queue://foo")
            .handler((delivery, msg) -> {
                Section body = msg.getBody();
                if (body instanceof AmqpValue) {
                    String content = (String) ((AmqpValue) body).getValue();
                    System.out.println("Received message with content: " + content);
                }
            })
            .flow(10)  // Prefetch up to 10 messages
            .open();


        // Send messages to a queue..
        Message message = message("queue://foo", "Hello World from client");
        connection.send(tag("m1"), message, delivery -> {
            System.out.println("The message was sent");
        });

    }

}
