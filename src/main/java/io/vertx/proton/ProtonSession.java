package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonSession {

  void setIncomingCapacity(int bytes);

  abstract ErrorCondition getRemoteCondition();

  int getIncomingCapacity();

  abstract void setCondition(ErrorCondition condition);

  abstract ErrorCondition getCondition();

  ProtonSession open();

  ProtonSession close();

  ProtonSession openHandler(Handler<AsyncResult<ProtonSession>> openHandler);

  ProtonSession closeHandler(Handler<AsyncResult<ProtonSession>> closeHandler);

  abstract ProtonSender sender(String name);

  abstract ProtonSender sender(String name, String address);

  abstract ProtonReceiver receiver(String name);

  abstract ProtonReceiver receiver(String name, String address);


}
