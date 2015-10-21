/**
 * Copyright 2015 Red Hat, Inc.
 */

package io.vertx.proton.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonSession;
import io.vertx.proton.ProtonHelper;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Session;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonSessionImpl implements ProtonSession {

    private final Session session;
    private Handler<AsyncResult<ProtonSessionImpl>> openHandler;
    private Handler<AsyncResult<ProtonSessionImpl>> closeHandler;

    ProtonSessionImpl(Session session) {
        this.session = session;
        this.session.setContext(this);
    }

    public ProtonConnectionImpl getConnectionImpl() {
                return (ProtonConnectionImpl) this.session.getConnection().getContext();
            }

    public long getOutgoingWindow() {
        return session.getOutgoingWindow();
    }

    public void setIncomingCapacity(int bytes) {
        session.setIncomingCapacity(bytes);
    }

    public int getOutgoingBytes() {
        return session.getOutgoingBytes();
    }

    public EndpointState getRemoteState() {
        return session.getRemoteState();
    }

    public int getIncomingBytes() {
        return session.getIncomingBytes();
    }

    public ErrorCondition getRemoteCondition() {
        return session.getRemoteCondition();
    }

    public int getIncomingCapacity() {
        return session.getIncomingCapacity();
    }

    public EndpointState getLocalState() {
        return session.getLocalState();
    }

    public void setCondition(ErrorCondition condition) {
        session.setCondition(condition);
    }

    public ErrorCondition getCondition() {
        return session.getCondition();
    }

    public void setOutgoingWindow(long outgoingWindowSize) {
        session.setOutgoingWindow(outgoingWindowSize);
    }


    public ProtonSessionImpl open() {
        session.open();
        getConnectionImpl().flush();
        return this;
    }

    public ProtonSessionImpl close() {
        session.close();
        getConnectionImpl().flush();
        return this;
    }

    public ProtonSessionImpl openHandler(Handler<AsyncResult<ProtonSessionImpl>> openHandler) {
        this.openHandler = openHandler;
        return this;
    }

    public ProtonSessionImpl closeHandler(Handler<AsyncResult<ProtonSessionImpl>> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public ProtonSenderImpl sender(String name) {
        return new ProtonSenderImpl(session.sender(name))
                .setSenderSettleMode(SenderSettleMode.UNSETTLED)
                .setReceiverSettleMode(ReceiverSettleMode.FIRST);

    }

    public ProtonSenderImpl sender(String name, String address) {
        Target target = new Target();
        target.setAddress(address);
        return sender(name).setTarget(target);
    }

    public ProtonReceiverImpl receiver(String name) {
        return new ProtonReceiverImpl(session.receiver(name))
                .setSenderSettleMode(SenderSettleMode.UNSETTLED)
                .setReceiverSettleMode(ReceiverSettleMode.FIRST);

    }

    public ProtonReceiverImpl receiver(String name, String address) {
        Source source = new Source();
        source.setAddress(address);
        return receiver(name).setSource(source);
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // Implementation details hidden from public api.
    //
    /////////////////////////////////////////////////////////////////////////////
    void fireRemoteOpen() {
        if (openHandler != null) {
            openHandler.handle(ProtonHelper.future(this, getRemoteCondition()));
        }
    }

    void fireRemoteClose() {
        if (closeHandler != null) {
            closeHandler.handle(ProtonHelper.future(this, getRemoteCondition()));
        }
    }


}
