package io.vertx.proton;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.message.Message;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class VertxAMQPConnection {

  private final NetSocket socket;
  private final Connection connection;
  private final Transport transport;
  //private Handler<String> messageHandler;
  private InternalHandler internalHandler;

  public VertxAMQPConnection(NetSocket socket) {
    this.socket = socket;

    connection = Proton.connection();
    connection.setContainer("client-id:1");
    connection.open();
    transport = Proton.transport();
    transport.bind(connection);

    Collector collector = Proton.collector();
    connection.collect(collector);

    socket.handler(buff -> {
      pumpInbound(ByteBuffer.wrap(buff.getBytes()));

      Event protonEvent = null;
      while ((protonEvent = collector.peek()) != null) {
        protonEvent.dispatch(internalHandler);
        collector.pop();
      }

      pumpOutbound();
    });
  }

  public void sendMessage(String address, String body) {
    Session session = connection.session();
    session.open();
    Sender sender = session.sender("sender-link-1"); // Is this just an arbitrary string?
    Target target = new Target();
    target.setAddress(address);
    sender.setTarget(target);
    sender.setSource(new Source());
    sender.setSenderSettleMode(SenderSettleMode.SETTLED);
    sender.setReceiverSettleMode(ReceiverSettleMode.FIRST);
    sender.open();
    int BUFFER_SIZE = 1024;
    Message m = Proton.message();
    m.setBody(new AmqpValue(body));
    byte[] encodedMessage = new byte[BUFFER_SIZE];
    int len = m.encode(encodedMessage, 0, BUFFER_SIZE);
    String deliveryTag = "msg:1"; // Not sure the relevance of this
    byte[] tag = deliveryTag.getBytes(StandardCharsets.UTF_8);
    Delivery serverDelivery = sender.delivery(tag); // Not sure why this is necessary
    sender.send(encodedMessage, 0, len);
    sender.advance();

    // we want to send the messages pre-settled
    serverDelivery.settle();

    pumpOutbound();

  }

  private void pumpInbound(ByteBuffer bytes) {
    // Lets push bytes from vert.x to proton engine.
    ByteBuffer inputBuffer = transport.getInputBuffer();
    while (bytes.hasRemaining() && inputBuffer.hasRemaining()) {
      inputBuffer.put(bytes.get());
      transport.processInput().checkIsOk();
    }
  }

  void pumpOutbound() {
    boolean done = false;
    while (!done) {
      ByteBuffer outputBuffer = transport.getOutputBuffer();
      if (outputBuffer != null && outputBuffer.hasRemaining()) {
        byte buffer[] = new byte[outputBuffer.remaining()];
        outputBuffer.get(buffer);
        socket.write(Buffer.buffer(buffer));
        transport.outputConsumed();
      } else {
        done = true;
      }
    }
  }

  public void setHandler(String address, Handler<String> handler) {
    this.internalHandler = new InternalHandler(handler);

    Session session = connection.session();
    session.open();

    // Create a receiver link
    Receiver receiver = session.receiver("receiver-link-1");
    Source source = new Source();
    source.setAddress(address);
    receiver.setSource(source);
    receiver.setTarget(new Target());
    receiver.setSenderSettleMode(SenderSettleMode.SETTLED); // Using pre-settled transfers
    receiver.setReceiverSettleMode(ReceiverSettleMode.FIRST);

    receiver.open();

    // Flow some credit so we can receive messages
    int credits = 50;
    receiver.flow(credits);
  }

  private static class InternalHandler extends BaseHandler {

    private final Handler<String> handler;

    public InternalHandler(Handler<String> handler) {
      this.handler = handler;
    }

    @Override
    public void onDelivery(Event e)
    {
      // We have received a new transfer frame or a disposition update.
      // Since we are pre-settling the message transfers, the latter shouldn't occur.

      // Process the delivery update
      Delivery d = e.getDelivery();
      Link l = d.getLink();

      // We are using pre-settled messages, so the only events should be about
      // receiving new transfers (where a message may need multiple transfers)

      Receiver receiver = (Receiver)l;

      Delivery delivery = receiver.current();
      if (delivery.isReadable() && !delivery.isPartial()) {
        // TODO: properly account for unknown message size, ensure we recv all bytes
        int BUFFER_SIZE = 1024;
        byte[] encodedMessage = new byte[BUFFER_SIZE];
        int count = receiver.recv(encodedMessage, 0, BUFFER_SIZE);

        Message msg = Proton.message();
        msg.decode(encodedMessage, 0, count);

        Section body = msg.getBody();
        if (body instanceof AmqpValue) {
          String content = (String)((AmqpValue) body).getValue();

          System.out.println("Received message with content: " + content);

          handler.handle(content);
        }
        //TODO: Could also be a Data or AmqpSequence body.

        // We are using pre-settled transfers, so we dont need to update the peer, but we
        // but we do need to locally-settle the delivery object now so Proton can clean it up.
        delivery.settle();

        // We need to replenish the peers credit so they can send. Ideally this
        // would be batched rather than per-message
        receiver.flow(1);
      }
    }

  }


}
