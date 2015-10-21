package io.vertx.proton;

import io.vertx.proton.impl.ProtonDeliveryImpl;
import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonSender extends ProtonLink<ProtonSender> {

  ProtonDeliveryImpl send(byte[] tag, Message message);

}
