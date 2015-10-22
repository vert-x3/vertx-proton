package io.vertx.proton;

import io.vertx.core.Handler;
import org.apache.qpid.proton.amqp.transport.DeliveryState;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonDelivery {

  DeliveryState getLocalState();

  boolean isSettled();

  boolean remotelySettled();

  byte[] getTag();

  void setDefaultDeliveryState(DeliveryState state);

  DeliveryState getDefaultDeliveryState();

  boolean isReadable();

  boolean isUpdated();

  boolean isWritable();

  boolean isPartial();

  DeliveryState getRemoteState();

  int getMessageFormat();

  ProtonDelivery disposition(DeliveryState state);

  ProtonDelivery settle();

  ProtonDelivery handler(Handler<ProtonDelivery> handler);
}
