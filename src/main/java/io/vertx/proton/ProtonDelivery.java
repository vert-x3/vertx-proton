package io.vertx.proton;

import io.vertx.core.Handler;
import org.apache.qpid.proton.amqp.transport.DeliveryState;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonDelivery {

  void clear();

  DeliveryState getLocalState();

  boolean isSettled();

  boolean remotelySettled();

  byte[] getTag();

  void setDefaultDeliveryState(DeliveryState state);

  DeliveryState getDefaultDeliveryState();

  boolean isReadable();

  boolean isUpdated();

  boolean isWritable();

  int pending();

  boolean isPartial();

  DeliveryState getRemoteState();

  int getMessageFormat();

  boolean isBuffered();

  ProtonDelivery disposition(DeliveryState state);

  ProtonDelivery settle();

  ProtonDelivery handler(Handler<ProtonDelivery> handler);
}
