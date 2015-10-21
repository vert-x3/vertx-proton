/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonHelper;
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
abstract class ProtonLink<T extends ProtonLink> {

    protected final Link link;
    private Handler<AsyncResult<T>> openHandler;
    private Handler<AsyncResult<T>> closeHandler;

    ProtonLink(Link link) {
        this.link = link;
        this.link.setContext(this);
    }

    protected abstract T self();

    public ProtonSessionImpl getSession() {
        return (ProtonSessionImpl) this.link.getSession().getContext();
    }

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

    public ReceiverSettleMode getReceiverSettleMode() {
        return link.getReceiverSettleMode();
    }

    public ErrorCondition getRemoteCondition() {
        return link.getRemoteCondition();
    }

    public int getRemoteCredit() {
        return link.getRemoteCredit();
    }

    public ReceiverSettleMode getRemoteReceiverSettleMode() {
        return link.getRemoteReceiverSettleMode();
    }

    public SenderSettleMode getRemoteSenderSettleMode() {
        return link.getRemoteSenderSettleMode();
    }

    public Source getRemoteSource() {
        return link.getRemoteSource();
    }

    public EndpointState getRemoteState() {
        return link.getRemoteState();
    }

    public Target getRemoteTarget() {
        return link.getRemoteTarget();
    }

    public SenderSettleMode getSenderSettleMode() {
        return link.getSenderSettleMode();
    }

    public Source getSource() {
        return link.getSource();
    }

    public Target getTarget() {
        return link.getTarget();
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

    public T setTarget(Target address) {
        link.setTarget(address);
        return self();
    }

    public T setSource(Source address) {
        link.setSource(address);
        return self();

    }

    public T setReceiverSettleMode(ReceiverSettleMode receiverSettleMode) {
        link.setReceiverSettleMode(receiverSettleMode);
        return self();
    }

    public T setSenderSettleMode(SenderSettleMode senderSettleMode) {
        link.setSenderSettleMode(senderSettleMode);
        return self();
    }

    public T setCondition(ErrorCondition condition) {
        link.setCondition(condition);
        return self();
    }

    public Delivery delivery(byte[] tag) {
        return link.delivery(tag);
    }

    public T open() {
        link.open();
        return self();
    }


    public T close() {
        link.close();
        return self();
    }

    public T detach() {
        link.detach();
        return self();
    }

    public T openHandler(Handler<AsyncResult<T>> openHandler) {
        this.openHandler = openHandler;
        return self();
    }

    public T closeHandler(Handler<AsyncResult<T>> closeHandler) {
        this.closeHandler = closeHandler;
        return self();
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
    }

    void fireRemoteClose() {
        if (closeHandler != null) {
            closeHandler.handle(ProtonHelper.future(self(), getRemoteCondition()));
        }
    }

}
