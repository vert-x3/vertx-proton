package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.proton.impl.ProtonDeliveryImpl;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface ProtonConnection {

  ProtonConnection setHostname(String hostname);

  ProtonConnection setContainer(String container);

  ProtonConnection setCondition(ErrorCondition condition);

  ErrorCondition getCondition();

  String getContainer();

  String getHostname();

  ErrorCondition getRemoteCondition();

  String getRemoteContainer();

  String getRemoteHostname();

  ProtonConnection open();

  ProtonConnection close();

  ProtonSession session();

  ProtonDeliveryImpl send(byte[] tag, Message message);

  ProtonReceiver receiver(String name);

  ProtonReceiver receiver(String name, String address);

  void disconnect();

  boolean isDisconnected();

  ProtonConnection openHandler(Handler<AsyncResult<ProtonConnection>> openHandler);

  ProtonConnection closeHandler(Handler<AsyncResult<ProtonConnection>> closeHandler);

  ProtonConnection disconnectHandler(Handler<ProtonConnection> disconnectHandler);

  ProtonConnection sessionOpenHandler(Handler<ProtonSession> remoteSessionOpenHandler);

  ProtonConnection senderOpenHandler(Handler<ProtonSender> remoteSenderOpenHandler);

  ProtonConnection receiverOpenHandler(Handler<ProtonReceiver> remoteReceiverOpenHandler);
}
