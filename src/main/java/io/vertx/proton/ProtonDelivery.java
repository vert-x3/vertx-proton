package io.vertx.proton;

import org.apache.qpid.proton.amqp.transport.DeliveryState;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonDelivery {

  byte[] getTag();

  int getMessageFormat();

  DeliveryState getLocalState();
  ProtonDelivery disposition(DeliveryState state);
  DeliveryState getRemoteState();

}
