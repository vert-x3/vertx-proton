package io.vertx.proton;

import io.vertx.core.Vertx;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class HelloWorld {

  public static void main(String[] args) {

    // Create the Vert.x instance
    Vertx vertx = Vertx.vertx();

    // Create the Vert.x AMQP client instance
    VertxAMQPClient client  = new VertxAMQPClient(vertx, "localhost", 5672);

    // Runt the example
    helloWorldSendAndConsumeExample(client);

    // Just stop main() from exiting
    try {
      System.in.read();
    } catch (Exception ignore) {
    }
  }

  private static void helloWorldSendAndConsumeExample(VertxAMQPClient client) {

    // First connect, asynchronously
    client.connect(res -> {

      // Now we're connected!
      System.out.println("We're connected");

      if (res.succeeded()) {

        // This is our connection object
        VertxAMQPConnection conn = res.result();

        // This is the address of the queue
        String address = "queue://foo";

        // Create a consumer that will receive messages from the queue
        conn.setHandler(address, msg -> {

          // Should print out; I received message: helloworld
          System.out.println("I received message: " + msg);
        });

        // Now send a message to the queue
        conn.sendMessage("queue://foo", "helloworld");

        System.out.println("Sent a message");

        // That's it!

      } else {
        res.cause().printStackTrace();
      }
    });
  }
}
