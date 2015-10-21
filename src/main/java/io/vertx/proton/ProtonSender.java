package io.vertx.proton;

import io.vertx.core.Handler;
import io.vertx.proton.impl.ProtonSenderImpl;
import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonSender extends ProtonLink<ProtonSender> {

  ProtonDelivery send(byte[] tag, Message message);

  boolean sendQueueFull();

  void sendQueueDrainHandler(Handler<ProtonSender> drainHandler);
}
