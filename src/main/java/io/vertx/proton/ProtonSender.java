package io.vertx.proton;

import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonSender {

  ProtonDelivery send( Message message);
}
