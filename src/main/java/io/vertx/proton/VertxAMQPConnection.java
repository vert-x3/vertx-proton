package io.vertx.proton;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.Handler;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
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
  private Handler<String> messageHandler;

  public VertxAMQPConnection(NetSocket socket) {
    this.socket = socket;

    connection = Proton.connection();
    connection.setContainer("client-id:1");
    connection.open();
    transport = Proton.transport();
    transport.bind(connection);

    socket.handler(buff -> {
      transport.getInputBuffer().put(buff.getBytes());
    });
  }

  public void sendMessage(String address, String body) {
    Session session = connection.session();
    session.open();
    Target target = new Target();
    target.setAddress(address);
    Sender sender = session.sender("link1"); // Is this just an arbitrary string?
    sender.setTarget(target);
    sender.setSenderSettleMode(SenderSettleMode.UNSETTLED);
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
    ByteBuffer outputBuffer = transport.getOutputBuffer();
    while (outputBuffer.hasRemaining() ) {
      byte buffer[] = new byte[outputBuffer.remaining()];
      outputBuffer.get(buffer);
      socket.write(Buffer.buffer(buffer));
      transport.outputConsumed();
    }
  }

  public void setHandler(String address, Handler<String> handler) {
    this.messageHandler = handler;
    // TODO - how to connect up the handler so it consumes messages, something to do with Receiver I guess?
  }

}
