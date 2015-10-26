/**
 * Copyright 2015 Red Hat, Inc.
 */

package io.vertx.proton.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonSession;
import io.vertx.proton.ProtonHelper;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonSessionImpl implements ProtonSession {

    private final Session session;
    private int autoLinkCounter =0;
    private Handler<AsyncResult<ProtonSession>> openHandler;
    private Handler<AsyncResult<ProtonSession>> closeHandler;

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

    @Override
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

    @Override
    public ErrorCondition getRemoteCondition() {
        return session.getRemoteCondition();
    }

    @Override
    public int getIncomingCapacity() {
        return session.getIncomingCapacity();
    }

    public EndpointState getLocalState() {
        return session.getLocalState();
    }

    @Override
    public void setCondition(ErrorCondition condition) {
        session.setCondition(condition);
    }

    @Override
    public ErrorCondition getCondition() {
        return session.getCondition();
    }

    public void setOutgoingWindow(long outgoingWindowSize) {
        session.setOutgoingWindow(outgoingWindowSize);
    }


    @Override
    public ProtonSessionImpl open() {
        session.open();
        getConnectionImpl().flush();
        return this;
    }

    @Override
    public ProtonSessionImpl close() {
        session.close();
        getConnectionImpl().flush();
        return this;
    }

    @Override
    public ProtonSessionImpl openHandler(Handler<AsyncResult<ProtonSession>> openHandler) {
        this.openHandler = openHandler;
        return this;
    }

    @Override
    public ProtonSessionImpl closeHandler(Handler<AsyncResult<ProtonSession>> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    @Override
    public ProtonSender sender() {
        return sender("auto-"+(autoLinkCounter++));
    }

    @Override
    public ProtonSender sender(String name) {
        Sender sender = session.sender(name);
        if (sender.getContext() != null) {
            return (ProtonSender) sender.getContext();
        } else {
            return new ProtonSenderImpl(sender);
        }
    }

    @Override
    public ProtonReceiver receiver() {
        return receiver("auto-" + (autoLinkCounter++));
    }

    @Override
    public ProtonReceiver receiver(String name) {
        Receiver receiver = session.receiver(name);
        if (receiver.getContext() != null) {
            return (ProtonReceiver) receiver.getContext();
        } else {
            return new ProtonReceiverImpl(receiver);
        }
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
