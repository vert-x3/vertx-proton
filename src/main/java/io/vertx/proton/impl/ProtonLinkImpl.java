/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonLink;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
abstract class ProtonLinkImpl<T extends ProtonLink> implements ProtonLink<T> {

    protected final Link link;
    private Handler<AsyncResult<T>> openHandler;
    private Handler<AsyncResult<T>> closeHandler;

    ProtonLinkImpl(Link link) {
        this.link = link;
        this.link.setContext(this);
    }

    protected abstract T self();

    @Override
    public ProtonSessionImpl getSession() {
        return (ProtonSessionImpl) this.link.getSession().getContext();
    }

    @Override
    public ErrorCondition getCondition() {
        return link.getCondition();
    }

    public int getCredit() {
        return link.getCredit();
    }

    public boolean getDrain() {
        return link.getDrain();
    }

    public EndpointState getLocalState() {
        return link.getLocalState();
    }

    public String getName() {
        return link.getName();
    }

    @Override
    public ReceiverSettleMode getReceiverSettleMode() {
        return link.getReceiverSettleMode();
    }

    @Override
    public ErrorCondition getRemoteCondition() {
        return link.getRemoteCondition();
    }

    public int getRemoteCredit() {
        return link.getRemoteCredit();
    }

    @Override
    public ReceiverSettleMode getRemoteReceiverSettleMode() {
        return link.getRemoteReceiverSettleMode();
    }

    @Override
    public SenderSettleMode getRemoteSenderSettleMode() {
        return link.getRemoteSenderSettleMode();
    }


    public EndpointState getRemoteState() {
        return link.getRemoteState();
    }

    public SenderSettleMode getSenderSettleMode() {
        return link.getSenderSettleMode();
    }


    @Override
    public Target getRemoteTarget() {
        return link.getRemoteTarget();
    }

    @Override
    public Target getTarget() {
        return link.getTarget();
    }

    @Override
    public T setTarget(Target address) {
        link.setTarget(address);
        return self();
    }

    @Override
    public Source getRemoteSource() {
        return link.getRemoteSource();
    }

    @Override
    public Source getSource() {
        return link.getSource();
    }

    @Override
    public T setSource(Source address) {
        link.setSource(address);
        return self();
    }

    public int getUnsettled() {
        return link.getUnsettled();
    }

    public int getQueued() {
        return link.getQueued();
    }

    public boolean advance() {
        return link.advance();
    }

    public int drained() {
        return link.drained();
    }

    public boolean detached() {
        return link.detached();
    }

    public Delivery delivery(byte[] tag, int offset, int length) {
        return link.delivery(tag, offset, length);
    }

    public Delivery current() {
        return link.current();
    }

    @Override
    public T setReceiverSettleMode(ReceiverSettleMode receiverSettleMode) {
        link.setReceiverSettleMode(receiverSettleMode);
        return self();
    }

    @Override
    public T setSenderSettleMode(SenderSettleMode senderSettleMode) {
        link.setSenderSettleMode(senderSettleMode);
        return self();
    }

    @Override
    public T setCondition(ErrorCondition condition) {
        link.setCondition(condition);
        return self();
    }

    public Delivery delivery(byte[] tag) {
        return link.delivery(tag);
    }

    @Override
    public T open() {
        link.open();
        getSession().getConnectionImpl().flush();
        return self();
    }


    @Override
    public T close() {
        link.close();
        getSession().getConnectionImpl().flush();
        return self();
    }

    public T detach() {
        link.detach();
        getSession().getConnectionImpl().flush();
        return self();
    }

    @Override
    public T openHandler(Handler<AsyncResult<T>> openHandler) {
        this.openHandler = openHandler;
        return self();
    }

    @Override
    public T closeHandler(Handler<AsyncResult<T>> closeHandler) {
        this.closeHandler = closeHandler;
        return self();
    }

    @Override
    public boolean isOpen() {
        return getLocalState() == EndpointState.ACTIVE;
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // Implementation details hidden from public api.
    //
    /////////////////////////////////////////////////////////////////////////////
    void fireRemoteOpen() {
        if (openHandler != null) {
            openHandler.handle(ProtonHelper.future(self(), getRemoteCondition()));
        }
        fireLinkFlow();
    }

    void fireRemoteClose() {
        if (closeHandler != null) {
            closeHandler.handle(ProtonHelper.future(self(), getRemoteCondition()));
        }
    }

    void fireLinkFlow() {
    }
}
