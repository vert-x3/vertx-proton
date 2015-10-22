package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.proton.impl.ProtonSessionImpl;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonLink<T extends ProtonLink> {

    Target getTarget();

    T setTarget(Target address);

    T setTarget(String address);

    Target getRemoteTarget();


    Source getSource();

    T setSource(Source address);

    T setSource(String address);

    Source getRemoteSource();


    ProtonSession getSession();

    ErrorCondition getCondition();

    ReceiverSettleMode getReceiverSettleMode();

    ErrorCondition getRemoteCondition();

    ReceiverSettleMode getRemoteReceiverSettleMode();

    SenderSettleMode getRemoteSenderSettleMode();

    T setReceiverSettleMode(ReceiverSettleMode receiverSettleMode);

    SenderSettleMode getSenderSettleMode();

    T setSenderSettleMode(SenderSettleMode senderSettleMode);

    T setCondition(ErrorCondition condition);

    T open();

    T close();

    T openHandler(Handler<AsyncResult<T>> openHandler);

    T closeHandler(Handler<AsyncResult<T>> closeHandler);

    boolean isOpen();
}
